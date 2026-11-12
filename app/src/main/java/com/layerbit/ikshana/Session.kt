package com.layerbit.ikshana

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.layerbit.ikshana.engine.light.LightBand
import com.layerbit.ikshana.engine.light.LightLevel
import com.layerbit.ikshana.engine.mode.CameraState
import com.layerbit.ikshana.engine.mode.Earcon
import com.layerbit.ikshana.engine.mode.Effect
import com.layerbit.ikshana.engine.mode.Event
import com.layerbit.ikshana.engine.mode.Mode
import com.layerbit.ikshana.engine.mode.ModeMachine
import com.layerbit.ikshana.engine.mode.Models
import com.layerbit.ikshana.engine.mode.State
import com.layerbit.ikshana.feedback.FeedbackPlayer
import com.layerbit.ikshana.text.Labels
import com.layerbit.ikshana.voice.Voice

/**
 * The one place the mode machine's effects are carried out. Every event, from the screen, the
 * camera, the thermal listener or a model runner, goes through [send] on the main thread; the
 * machine decides; this speaks, sounds, buzzes and switches.
 *
 * Nothing here decides what to say: that is ModeMachine's, executed by FallbackChecks. This file
 * only maps effects to the voice and the feedback player, which is why it is short.
 */
class Session(private val context: Context, val prefs: Prefs) {
    private val main = Handler(Looper.getMainLooper())

    var state: State by mutableStateOf(State.initial(shelf(context), CameraState.DENIED)); private set
    /** The last line shown when there is no voice engine; null while the voice works. */
    var shown: String? by mutableStateOf(null); private set
    var paused: Boolean by mutableStateOf(false); private set
    var earcons: Boolean by mutableStateOf(prefs.earcons); private set
    var haptics: Boolean by mutableStateOf(prefs.haptics); private set

    private val feedback = FeedbackPlayer(context, { earcons }, { haptics })
    private val voice = Voice(context) { text ->
        main.post {
            shown = text
            feedback.play(Earcon.FALLBACK)
        }
    }
    private var band: LightBand? = null

    /** The app opened: the camera's permission is known by now, the last mode is in Prefs. */
    fun start(camera: CameraState, afterCrash: Boolean) {
        state = State.initial(shelf(context), camera)
        send(Event.Started(prefs.lastMode, afterCrash))
    }

    fun send(event: Event) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { send(event) }; return }
        val step = ModeMachine.step(state, event)
        val before = state.active
        state = step.state
        for (e in step.effects) when (e) {
            // Logged so a phone session can read what was handed to the voice without hearing
            // it (DEVICE_FINDINGS says which: logged is not heard). Nothing here leaves the phone.
            is Effect.Say -> Labels.said(context, e.said).let { Log.i("IkshanaSay", it); voice.say(it) }
            is Effect.Sound -> { Log.i("IkshanaSay", "earcon ${e.earcon}"); feedback.play(e.earcon) }
            is Effect.Run -> if (e.mode != before) band = null
            Effect.OfferCameraDoors -> Unit // the screen shows them whenever the mode is Help
        }
        if (state.wanted.onRow) prefs.lastMode = state.wanted
    }

    /** Light level: spoken when the band changes, with the hysteresis in LightLevel. */
    fun onLuma(mean: Double) = main.post {
        if (state.active != Mode.LIGHT) return@post
        val next = LightLevel.next(band, mean)
        if (next != band) {
            band = next
            context.getString(Labels.light(next)).let { Log.i("IkshanaSay", it); voice.say(it) }
        }
    }

    fun repeat() = voice.repeat()
    fun next() { if (!voice.next()) feedback.play(Earcon.REPEAT) }
    fun togglePause() { paused = voice.togglePause() }

    fun stepMode(by: Int) {
        val row = Mode.row
        val i = row.indexOf(state.wanted).coerceAtLeast(0)
        send(Event.Choose(row[(i + by + row.size) % row.size]))
    }

    fun switchEarcons(on: Boolean) { earcons = on; prefs.earcons = on; if (on) feedback.play(Earcon.MODE) }
    fun switchHaptics(on: Boolean) { haptics = on; prefs.haptics = on; if (on) feedback.play(Earcon.MODE) }

    /** Can [mode] run as itself now? For the row's state description. */
    fun runsAsItself(mode: Mode): Boolean = ModeMachine.land(state, mode).mode == mode

    fun close() {
        voice.shutdown()
        feedback.release()
    }

    companion object {
        /**
         * The models this install carries, as assets/models/<id>.tflite (the asset pack and the
         * full APK both land there, as in Stave's ModelStore). Empty in P0.
         */
        fun shelf(context: Context): Set<String> {
            val files = try { context.assets.list("models")?.toSet().orEmpty() } catch (_: Throwable) { emptySet() }
            return Models.all.filter { "$it.tflite" in files }.toSet()
        }
    }
}
