package com.layerbit.ikshana

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.layerbit.ikshana.camera.CameraDoor
import com.layerbit.ikshana.engine.light.LightLevel
import com.layerbit.ikshana.engine.mode.CameraState
import com.layerbit.ikshana.engine.mode.Event
import com.layerbit.ikshana.ui.HomeScreen
import com.layerbit.ikshana.ui.IkshanaTheme

/**
 * One activity, the camera as its home. It owns the doors the mode machine cannot: the camera
 * permission, the photo picker, the thermal listener. Each reports to the Session as an event,
 * and the machine decides what is said.
 */
class MainActivity : ComponentActivity() {
    private lateinit var session: Session
    private lateinit var camera: CameraDoor
    private var started = false
    private var crashed = false
    private var thermal: PowerManager.OnThermalStatusChangedListener? = null

    private val askCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val state = if (granted) CameraState.GRANTED else CameraState.DENIED
        if (!started) { started = true; session.start(state, crashed) } else session.send(Event.Camera(state))
    }

    private val pickPicture = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) readPicture(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(this)
        crashed = prefs.takeCrash()
        session = Session(applicationContext, prefs)
        camera = CameraDoor(applicationContext, { session.send(Event.Camera(it)) }, { session.wantsColour }, { session.onFrame(it) })

        setContent {
            IkshanaTheme {
                HomeScreen(
                    session = session,
                    camera = { modifier ->
                        if (session.state.camera == CameraState.GRANTED || session.state.camera == CameraState.BUSY) {
                            val owner = LocalLifecycleOwner.current
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).also {
                                        // The preview is decoration for the sighted half; TalkBack
                                        // reads the surface it sits in, which has the name.
                                        it.importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                                        camera.bind(owner, it)
                                    }
                                },
                                modifier = modifier
                            )
                            DisposableEffect(Unit) { onDispose { camera.unbind() } }
                        }
                    },
                    onAllowCamera = ::allowCamera,
                    onPickPicture = {
                        pickPicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        }

        if (hasCamera()) { started = true; session.start(CameraState.GRANTED, crashed) }
        else askCamera.launch(Manifest.permission.CAMERA)
        listenThermal()
    }

    override fun onResume() {
        super.onResume()
        // Back from the system's Settings with the permission given: the machine climbs back.
        if (started && hasCamera() && session.state.camera == CameraState.DENIED) session.send(Event.Camera(CameraState.GRANTED))
    }

    private fun hasCamera() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    /**
     * The Help screen's first door. Once Android has stopped showing the permission dialog (the
     * user said no twice, or "don't ask again"), asking again does nothing at all, which a blind
     * user would hear as nothing: so it opens the app's own page in Settings instead.
     */
    private fun allowCamera() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) || !session.prefs.askedCamera) {
            session.prefs.askedCamera = true
            askCamera.launch(Manifest.permission.CAMERA)
        } else {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }
    }

    /** A picture stands in for the camera: P0 reads its light level; P2 reads its text. */
    private fun readPicture(uri: Uri) {
        try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bmp: Bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: throw IllegalStateException("could not decode $uri")
            val px = IntArray(bmp.width * bmp.height)
            bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            bmp.recycle()
            session.send(Event.Camera(CameraState.PICTURE))
            session.onLuma(LightLevel.meanLumaArgb(px))
        } catch (t: Throwable) {
            Log.w("Ikshana", "picture unreadable", t)
            session.send(Event.NothingFound(com.layerbit.ikshana.engine.mode.Hint.NONE))
        }
    }

    private fun listenThermal() {
        if (Build.VERSION.SDK_INT < 29) return
        val pm = getSystemService(PowerManager::class.java) ?: return
        val l = PowerManager.OnThermalStatusChangedListener { status ->
            session.send(Event.Thermal(status >= PowerManager.THERMAL_STATUS_MODERATE))
        }
        thermal = l
        pm.addThermalStatusListener(ContextCompat.getMainExecutor(this), l)
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= 29) thermal?.let { getSystemService(PowerManager::class.java)?.removeThermalStatusListener(it) }
        camera.close()
        session.close()
        super.onDestroy()
    }
}
