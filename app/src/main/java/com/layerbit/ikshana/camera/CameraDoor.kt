package com.layerbit.ikshana.camera

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.layerbit.ikshana.engine.light.LightLevel
import com.layerbit.ikshana.engine.mode.CameraState
import java.util.concurrent.Executors

/**
 * The camera: a preview for the sighted half and an analysis stream for the modes.
 *
 * Analysis runs on its own thread with KEEP_ONLY_LATEST, so a slow mode drops frames rather than
 * queueing them; the UI thread never touches a frame (P2, item 11, measures this on the moto).
 * The analysis size is about 1280 x 960: small print on a medicine strip needs the pixels, and
 * the light level samples a grid of it anyway. Each settled frame goes to [onFrame] on the
 * analysis thread, which may take as long as the mode needs (the recogniser takes most of a
 * second); frames that arrive meanwhile are dropped, never queued.
 *
 * The camera's own state (opened, in use by another app, disabled by policy) is reported as the
 * mode machine's CameraState, so "another app is using the camera" is spoken rather than shown as
 * a black preview.
 */
class CameraDoor(
    private val context: Context,
    private val onCamera: (CameraState) -> Unit,
    /** Whether the mode now running needs colour (the vision model does; the reader and the
     *  light level do not, and converting every frame would cost the phone for nothing). */
    private val wantsColour: () -> Boolean = { false },
    private val onFrame: (Frame) -> Unit
) {
    /** One settled luma plane, as the sensor gives it (not yet rotated upright), and when asked
     *  for, the same frame upright in colour at most [COLOUR_SIDE] on its long side. */
    class Frame(val y: ByteArray, val width: Int, val height: Int, val rowStride: Int, val rotation: Int, val colour: Rgb? = null)

    /** Tightly packed RGB, three bytes a pixel, upright. */
    class Rgb(val bytes: ByteArray, val width: Int, val height: Int)

    private val analysisThread = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    /** When this binding's first frame arrived; auto-exposure is still moving until SETTLE_MS after it. */
    private var firstFrame = 0L

    fun bind(owner: LifecycleOwner, view: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder().setResolutionStrategy(
                            ResolutionStrategy(Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                        ).build()
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisThread) { image ->
                    try {
                        val now = System.currentTimeMillis()
                        if (firstFrame == 0L) firstFrame = now
                        if (now - firstFrame >= LightLevel.SETTLE_MS) {
                            val plane = image.planes[0]
                            val buf = plane.buffer
                            val bytes = ByteArray(buf.remaining())
                            buf.get(bytes)
                            // pixelStride is 1 for the Y plane of YUV_420_888 by the format's definition.
                            val colour = if (wantsColour()) rgbOf(image) else null
                            onFrame(Frame(bytes, image.width, image.height, plane.rowStride, image.imageInfo.rotationDegrees, colour))
                        }
                    } finally {
                        image.close()
                    }
                }
                p.unbindAll()
                firstFrame = 0L
                val camera = p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                camera.cameraInfo.cameraState.observe(owner) { s ->
                    val err = s.error
                    when {
                        err != null && (err.code == androidx.camera.core.CameraState.ERROR_CAMERA_IN_USE ||
                            err.code == androidx.camera.core.CameraState.ERROR_MAX_CAMERAS_IN_USE) -> onCamera(CameraState.BUSY)
                        err != null && err.code == androidx.camera.core.CameraState.ERROR_CAMERA_DISABLED -> onCamera(CameraState.DENIED)
                        s.type == androidx.camera.core.CameraState.Type.OPEN -> onCamera(CameraState.GRANTED)
                    }
                }
            } catch (t: Throwable) {
                // No back camera, or the provider failed: the machine lands in Help and says so.
                Log.w("Ikshana", "camera bind failed", t)
                onCamera(CameraState.DENIED)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** The frame upright, in colour, scaled so its long side is at most COLOUR_SIDE. Null if the
     *  conversion fails (logged): the mode then treats it as a frame it could not see. */
    private fun rgbOf(image: androidx.camera.core.ImageProxy): Rgb? = try {
        val src = image.toBitmap()
        val scale = minOf(1f, COLOUR_SIDE.toFloat() / maxOf(src.width, src.height))
        val m = android.graphics.Matrix().apply {
            postScale(scale, scale)
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }
        val up = android.graphics.Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        val px = IntArray(up.width * up.height)
        up.getPixels(px, 0, up.width, 0, 0, up.width, up.height)
        val out = ByteArray(px.size * 3)
        for (i in px.indices) {
            val p = px[i]
            out[3 * i] = (p shr 16).toByte(); out[3 * i + 1] = (p shr 8).toByte(); out[3 * i + 2] = p.toByte()
        }
        val rgb = Rgb(out, up.width, up.height)
        if (up !== src) up.recycle()
        src.recycle()
        rgb
    } catch (t: Throwable) {
        Log.w("Ikshana", "colour frame failed", t); null
    }

    fun unbind() {
        provider?.unbindAll()
    }

    fun close() {
        unbind()
        analysisThread.shutdown()
    }

    companion object {
        /** The vision models resize to about this anyway; sending more only costs time. */
        const val COLOUR_SIDE = 768
    }
}
