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
 * The analysis size is about 640 x 480: enough for the light level and the frame gate, and the
 * OCR path picks its own size in P2.
 *
 * The camera's own state (opened, in use by another app, disabled by policy) is reported as the
 * mode machine's CameraState, so "another app is using the camera" is spoken rather than shown as
 * a black preview.
 */
class CameraDoor(
    private val context: Context,
    private val onCamera: (CameraState) -> Unit,
    private val onLuma: (Double) -> Unit
) {
    private val analysisThread = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    /** Light level is spoken twice a second at most; analysing more often only warms the phone. */
    private var lastAnalysis = 0L

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
                            ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                        ).build()
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisThread) { image ->
                    try {
                        val now = System.currentTimeMillis()
                        if (now - lastAnalysis >= 500) {
                            lastAnalysis = now
                            val plane = image.planes[0]
                            val buf = plane.buffer
                            val bytes = ByteArray(buf.remaining())
                            buf.get(bytes)
                            // pixelStride is 1 for the Y plane of YUV_420_888 by the format's definition.
                            onLuma(LightLevel.meanLuma(bytes, image.width, image.height, plane.rowStride))
                        }
                    } finally {
                        image.close()
                    }
                }
                p.unbindAll()
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

    fun unbind() {
        provider?.unbindAll()
    }

    fun close() {
        unbind()
        analysisThread.shutdown()
    }
}
