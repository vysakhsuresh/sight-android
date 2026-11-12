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
import com.layerbit.ikshana.camera.CameraDoor
import com.layerbit.ikshana.engine.mode.Hint
import com.layerbit.ikshana.engine.read.NothingPacer
import com.layerbit.ikshana.engine.money.Money
import com.layerbit.ikshana.engine.money.MoneyVote
import com.layerbit.ikshana.engine.read.ReadMemory
import com.layerbit.ikshana.engine.read.ReadText
import com.layerbit.ikshana.engine.read.Scripts
import com.layerbit.ikshana.read.Reader
import com.layerbit.ikshana.see.VisionModel
import com.layerbit.ikshana.engine.see.Describe
import com.layerbit.ikshana.engine.see.DescribePacer
import com.layerbit.ikshana.engine.see.FrameGate
import com.layerbit.ikshana.engine.see.Gate
import com.layerbit.ikshana.engine.see.Prompts
import com.layerbit.ikshana.engine.mode.Line
import com.layerbit.ikshana.engine.mode.Said
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
    /** The mode as the analysis thread sees it; written on main, read there. */
    @Volatile private var activeNow: Mode = Mode.HELP
    @Volatile var readLanguages: String = prefs.readLanguages; private set
    var readLanguagesShown: String by mutableStateOf(prefs.readLanguages); private set
    private val reader = Reader(context)
    /** Its own reader, English only: notes print their value in Latin numerals, and one
     *  language makes the recogniser faster and surer of digits than five would. */
    private val moneyReader = Reader(context)
    private val vote = MoneyVote()
    private val memory = ReadMemory()
    private val pacer = NothingPacer()
    private var lastLight = 0L
    /** The vision model, opened on the analysis thread the first time Describe needs it
     *  (seconds, and most of a gigabyte), then kept: reopening per mode switch would make
     *  every return to Describe as slow as the first. */
    private var vision: VisionModel? = null
    private val describePacer = DescribePacer()
    /** Whether "Looking." has been said since Describe was entered: once is reassurance,
     *  before every description it would be noise. */
    @Volatile private var lookedSinceEnter = false

    /** Read by the camera's thread: colour frames only while Describe runs. */
    val wantsColour: Boolean get() = activeNow == Mode.DESCRIBE

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
            is Effect.Run -> if (e.mode != before) {
                band = null; memory.clear(); vote.clear(); describePacer.clear(); lookedSinceEnter = false
            }
            Effect.OfferCameraDoors -> Unit // the screen shows them whenever the mode is Help
        }
        if (state.wanted.onRow) prefs.lastMode = state.wanted
        activeNow = state.active
    }

    /**
     * A settled camera frame, on the analysis thread. Light level samples it twice a second;
     * Read runs the recogniser on it, which takes as long as it takes (the camera drops the
     * frames that arrive meanwhile), and everything it finds passes the engine's rules before
     * it is said: junk dropped, lines already said skipped, each line in its script's voice.
     */
    fun onFrame(f: CameraDoor.Frame) {
        when (activeNow) {
            Mode.LIGHT -> {
                val now = System.currentTimeMillis()
                if (now - lastLight < 500) return
                lastLight = now
                onLuma(LightLevel.meanLuma(f.y, f.width, f.height, f.rowStride))
            }
            Mode.READ -> readFrame(f)
            Mode.MONEY -> moneyFrame(f)
            Mode.DESCRIBE -> describeFrame(f)
            else -> Unit
        }
    }

    private fun readFrame(f: CameraDoor.Frame) {
        val langs = readLanguages
        if (!reader.open(langs)) { send(Event.ModelFailed(Models.OCR_TESSERACT)); return }
        val started = System.currentTimeMillis()
        val up = ReadText.rotate(f.y, f.width, f.height, f.rowStride, f.rotation)
        val lines = try { reader.read(up.bytes, up.width, up.height) } catch (e: Throwable) {
            Log.w("Ikshana", "read failed", e); send(Event.ModelFailed(Models.OCR_TESSERACT)); return
        }
        val kept = ReadText.clean(lines)
        val mean = LightLevel.meanLuma(f.y, f.width, f.height, f.rowStride)
        Log.i("IkshanaRead", "${up.width}x${up.height} in ${System.currentTimeMillis() - started} ms: ${lines.size} lines, ${kept.size} kept")
        main.post {
            if (activeNow != Mode.READ) return@post
            for (line in memory.fresh(kept)) {
                Log.i("IkshanaSay", line)
                voice.say(line, Scripts.localeOf(line))
            }
            if (pacer.onRead(System.currentTimeMillis(), kept.isNotEmpty())) {
                send(Event.NothingFound(if (LightLevel.bandOf(mean) == LightBand.DARK) Hint.TOO_DARK else Hint.NONE))
            }
        }
    }

    private fun moneyFrame(f: CameraDoor.Frame) {
        if (!moneyReader.open("eng")) { send(Event.ModelFailed(Models.OCR_TESSERACT)); return }
        val up = ReadText.rotate(f.y, f.width, f.height, f.rowStride, f.rotation)
        val lines = try { moneyReader.read(up.bytes, up.width, up.height) } catch (e: Throwable) {
            Log.w("Ikshana", "money read failed", e); send(Event.ModelFailed(Models.OCR_TESSERACT)); return
        }
        // Low-confidence lines still count here: a lone numeral is short and Tesseract is unsure
        // of short lines, but the vote across frames is what guards against a wrong value.
        val value = Money.valueOf(lines.map { it.text })
        val mean = LightLevel.meanLuma(f.y, f.width, f.height, f.rowStride)
        Log.i("IkshanaMoney", "frame read ${lines.map { it.text }} -> $value")
        main.post {
            if (activeNow != Mode.MONEY) return@post
            val say = vote.onFrame(value)
            if (say != null) {
                Money.words(say).let { Log.i("IkshanaSay", it); voice.say(it) }
                feedback.buzz(Money.pattern(say))
            }
            if (pacer.onRead(System.currentTimeMillis(), value != null)) {
                send(Event.NothingFound(if (LightLevel.bandOf(mean) == LightBand.DARK) Hint.TOO_DARK else Hint.NONE))
            }
        }
    }

    /**
     * Describe: a frame the scene has changed enough to describe (DescribePacer), that the gate
     * lets through (no black or smeared frame reaches the model, FrameGate), is described by the
     * vision model and cut to two plain sentences (Describe.clean) before it is spoken.
     */
    private fun describeFrame(f: CameraDoor.Frame) {
        val rgb = f.colour ?: return
        val now = System.currentTimeMillis()
        val tight = ReadText.rotate(f.y, f.width, f.height, f.rowStride, 0)
        val thumb = DescribePacer.thumb(tight.bytes, tight.width, tight.height)
        if (!describePacer.due(now, thumb)) return
        when (FrameGate.check(tight.bytes, tight.width, tight.height)) {
            Gate.TOO_DARK -> {
                describePacer.described(now, thumb)
                send(Event.NothingFound(Hint.TOO_DARK)); return
            }
            // Not marked described: the next sharp frame of the same scene should go through.
            Gate.BLURRED -> {
                main.post { if (activeNow == Mode.DESCRIBE && pacer.onRead(now, false)) send(Event.NothingFound(Hint.BLURRED)) }
                return
            }
            Gate.OK -> Unit
        }
        describePacer.described(now, thumb)
        if (!lookedSinceEnter) {
            lookedSinceEnter = true
            main.post { if (activeNow == Mode.DESCRIBE) Labels.said(context, Said(Line.LOOKING)).let { Log.i("IkshanaSay", it); voice.say(it) } }
        }
        val model = vision ?: VisionModel.open(VisionModel.folder(context), THREADS)?.also { vision = it }
        if (model == null) { send(Event.ModelFailed(Models.VLM)); return }
        val started = System.currentTimeMillis()
        val raw = try { model.ask(rgb.bytes, rgb.width, rgb.height, Prompts.DESCRIBE, 64) } catch (e: Throwable) {
            Log.w("Ikshana", "describe failed", e); null
        }
        val text = raw?.let { Describe.clean(it) }
        Log.i("IkshanaDescribe", "${rgb.width}x${rgb.height} in ${System.currentTimeMillis() - started} ms: $raw")
        main.post {
            if (activeNow != Mode.DESCRIBE) return@post
            pacer.onRead(System.currentTimeMillis(), text != null)
            if (raw == null) send(Event.ModelFailed(Models.VLM))
            else if (text == null) send(Event.NothingFound(Hint.NONE))
            else { Log.i("IkshanaSay", text); voice.say(text, "en-IN") }
        }
    }

    /** Settings: the next reading language in Reader.CHOICES. */
    fun nextReadLanguages() {
        val c = Reader.CHOICES
        val next = c[(c.indexOf(readLanguages) + 1).mod(c.size)]
        readLanguages = next; readLanguagesShown = next; prefs.readLanguages = next
        memory.clear()
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
        reader.close()
        moneyReader.close()
        vision?.close()
        voice.shutdown()
        feedback.release()
    }

    companion object {
        /** The moto's two big cores and two of its small: more threads than big cores slows the
         *  matrix work down on a phone, measured in Stave's runners. To be measured here. */
        const val THREADS = 4

        /**
         * The models this install carries, as assets/models/<id>.tflite (the asset pack and the
         * full APK both land there, as in Stave's ModelStore), and the interim reader's
         * language files under assets/tessdata.
         */
        fun shelf(context: Context): Set<String> {
            val files = try { context.assets.list("models")?.toSet().orEmpty() } catch (_: Throwable) { emptySet() }
            val packed = Models.all.filter { "$it.tflite" in files }.toSet() +
                (if (VisionModel.files(VisionModel.folder(context)) != null) setOf(Models.VLM) else emptySet())
            return if (Reader.inPack(context)) packed + Models.OCR_TESSERACT else packed
        }
    }
}
