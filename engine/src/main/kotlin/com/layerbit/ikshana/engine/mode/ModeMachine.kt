package com.layerbit.ikshana.engine.mode

/**
 * The mode state machine: what Ikshana does next when the user asks for something, or when
 * something fails underneath it. Pure: a state and an event go in, a new state and the effects to
 * carry out come out. The app performs the effects and feeds back what happened as events.
 *
 * The owner's rule is the invariant FallbackChecks walks for every state and every event:
 *   - an event that changes what the user is in (mode, tier, camera) ends in a spoken sentence;
 *   - an event that answers something the user did or is waiting on (choosing a mode, a search
 *     that found nothing, a restart, a timeout) ends in a spoken sentence;
 *   - the one exception is a sentence identical to the last one spoken, which is not said twice
 *     (brief, section 4) but answered with the "repeat" earcon and its haptic twin, so the user
 *     still hears that the app is alive.
 * And the landing mode is always one the phone can run: the chain ends at HELP, which needs
 * nothing.
 */

enum class CameraState { GRANTED, DENIED, BUSY, PICTURE }

/** Why a search came back empty, as the frame gate or the model read it. */
enum class Hint { TOO_DARK, BLURRED, TOO_FAR, TOO_CLOSE, NONE }

enum class Earcon { MODE, FALLBACK, REPEAT, NOTHING, GATE_OPEN, GATE_CLOSED }

sealed interface Event {
    /** The app opened. [lastMode] is the mode it was in last time; [afterCrash] if it died. */
    data class Started(val lastMode: Mode, val afterCrash: Boolean) : Event
    /** The user chose a mode from the row (or by voice, later). */
    data class Choose(val mode: Mode) : Event
    /** A model refused to load, ran out of memory, or failed its self-check on this phone. */
    data class ModelFailed(val model: String) : Event
    /** The camera permission or the camera itself changed. */
    data class Camera(val state: CameraState) : Event
    /** The active mode looked and found nothing. */
    data class NothingFound(val hint: Hint) : Event
    /** The active mode's answer did not come within its budget. */
    data object TimedOut : Event
    /** The phone's thermal state crossed the throttle line, either way. */
    data class Thermal(val hot: Boolean) : Event
}

sealed interface Effect {
    data class Say(val said: Said) : Effect
    data class Sound(val earcon: Earcon) : Effect
    /** Run [mode] at [tier] (an index into ModeTable's tiers). */
    data class Run(val mode: Mode, val tier: Int) : Effect
    /** Put the two doors a blind user needs when the camera is gone on the screen and in focus. */
    data object OfferCameraDoors : Effect
}

data class State(
    /** What the user asked for last; the machine climbs back to it when things recover. */
    val wanted: Mode,
    val active: Mode,
    val tier: Int,
    val camera: CameraState,
    /** Model ids the pack carries. */
    val shelf: Set<String>,
    /** Model ids that failed on this phone this session, or that measurement ruled out. */
    val failed: Set<String>,
    val hot: Boolean,
    val lastSaid: Said?
) {
    companion object {
        fun initial(shelf: Set<String>, camera: CameraState, failed: Set<String> = emptySet()) =
            State(Mode.READ, Mode.HELP, 0, camera, shelf, failed, hot = false, lastSaid = null)
    }
}

data class Step(val state: State, val effects: List<Effect>) {
    val spoken: List<Said> get() = effects.filterIsInstance<Effect.Say>().map { it.said }
}

/** Where a wanted mode lands, and why. */
data class Landing(val mode: Mode, val tier: Int, val blockedByCamera: Boolean, val blockedByFailure: Boolean)

object ModeMachine {

    fun usable(state: State, model: String) = model in state.shelf && model !in state.failed

    private fun cameraOk(camera: CameraState) = camera == CameraState.GRANTED || camera == CameraState.PICTURE

    /**
     * Walk down from [wanted] to the first mode this phone can run now. The walk is bounded by
     * the table's length, so a cycle written into the table by mistake ends here rather than
     * hanging the app (and FallbackChecks fails on the cycle itself).
     */
    fun land(state: State, wanted: Mode): Landing {
        var mode = wanted
        var camera = false
        var failure = false
        repeat(Mode.entries.size + 1) {
            val needs = ModeTable.of(mode)
            if (needs.camera && !cameraOk(state.camera)) {
                camera = true
            } else {
                val tier = needs.tiers.indexOfFirst { t -> t.all { usable(state, it) } }
                if (tier >= 0) return Landing(mode, tier, camera, failure)
                if (needs.tiers.any { t -> t.all { it in state.shelf } }) failure = true
            }
            mode = needs.below ?: return Landing(Mode.HELP, 0, camera, failure)
        }
        return Landing(Mode.HELP, 0, camera, failure)
    }

    fun step(state: State, event: Event): Step = when (event) {
        is Event.Started -> {
            val s = state.copy(wanted = rowOrRead(event.lastMode), lastSaid = null)
            val land = land(s, s.wanted)
            val opening = if (event.afterCrash) Said(Line.RESTARTED, listOf(land.mode)) else Said(Line.WELCOME)
            val effects = mutableListOf<Effect>(Effect.Say(opening))
            var next = s.copy(active = land.mode, tier = land.tier, lastSaid = opening)
            // After a crash the restart sentence already names the mode, so only Help's own
            // sentence follows it (how to allow the camera is never left unsaid); a fresh start
            // says why the mode is not the one asked for, if it is not.
            val why = explain(s.wanted, land, failed = false)
            if (why != null && (!event.afterCrash || land.mode == Mode.HELP)) {
                effects += Effect.Say(why); next = next.copy(lastSaid = why)
            }
            effects += Effect.Run(land.mode, land.tier)
            if (land.mode == Mode.HELP) effects += Effect.OfferCameraDoors
            Step(next, effects)
        }

        is Event.Choose -> resettle(state.copy(wanted = event.mode), announceSame = true)

        is Event.ModelFailed -> {
            if (event.model in state.failed) Step(state, emptyList())
            else resettle(state.copy(failed = state.failed + event.model), announceSame = false, failed = true)
        }

        is Event.Camera -> {
            if (event.state == state.camera) Step(state, emptyList())
            else {
                val before = state.active
                val s = state.copy(camera = event.state)
                val land = land(s, s.wanted)
                val said = when {
                    event.state == CameraState.BUSY -> Said(Line.CAMERA_BUSY)
                    land.mode == Mode.HELP -> Said(Line.NO_CAMERA)
                    before == Mode.HELP -> Said(Line.CAMERA_BACK, listOf(land.mode))
                    else -> explain(s.wanted, land, failed = false) ?: Said(Line.MODE_ON, listOf(land.mode))
                }
                speak(s.copy(active = land.mode, tier = land.tier), said, land, Earcon.FALLBACK)
            }
        }

        is Event.NothingFound -> {
            val line = when (event.hint) {
                Hint.TOO_DARK -> Line.TOO_DARK
                Hint.BLURRED -> Line.HOLD_STILL
                Hint.TOO_FAR -> Line.MOVE_CLOSER
                Hint.TOO_CLOSE -> Line.MOVE_BACK
                Hint.NONE -> Line.NOTHING_HERE
            }
            sayOnce(state, Said(line), Earcon.NOTHING)
        }

        is Event.TimedOut -> sayOnce(state, Said(Line.TOO_LONG, listOf(state.active)), Earcon.NOTHING)

        is Event.Thermal -> {
            if (event.hot == state.hot) Step(state, emptyList())
            else sayOnce(state.copy(hot = event.hot), Said(if (event.hot) Line.WARM else Line.COOL), Earcon.NOTHING)
        }
    }

    /** Only the row's modes are remembered as "wanted"; a floor mode is never the user's choice. */
    private fun rowOrRead(mode: Mode) = if (mode.onRow) mode else Mode.READ

    /** Land the wanted mode again after a choice or a failure, and say where it landed. */
    private fun resettle(state: State, announceSame: Boolean, failed: Boolean = false): Step {
        val land = land(state, state.wanted)
        val changed = land.mode != state.active || land.tier != state.tier
        if (!changed && !announceSame) return Step(state, emptyList())
        val said = explain(state.wanted, land, failed)
            ?: Said(Line.MODE_ON, listOf(land.mode))
        val earcon = if (land.mode == state.wanted && land.tier == 0) Earcon.MODE else Earcon.FALLBACK
        return speak(state.copy(active = land.mode, tier = land.tier), said, land, earcon)
    }

    /**
     * The sentence that says why the user is not where they asked to be, or null when they are.
     * It names what was asked for and where the app landed, which is "what was tried and what to
     * do next" in the fewest words.
     */
    private fun explain(wanted: Mode, land: Landing, failed: Boolean): Said? = when {
        land.mode == Mode.HELP -> Said(Line.NO_CAMERA)
        land.mode == wanted && land.tier == 0 -> null
        land.mode == wanted -> Said(Line.LIGHTER, listOf(wanted))
        failed || land.blockedByFailure -> Said(Line.COULD_NOT_START, listOf(wanted, land.mode))
        else -> Said(Line.NOT_READY, listOf(wanted, land.mode))
    }

    private fun speak(state: State, said: Said, land: Landing, earcon: Earcon): Step {
        val effects = mutableListOf<Effect>()
        effects += Effect.Run(land.mode, land.tier)
        if (land.mode == Mode.HELP) effects += Effect.OfferCameraDoors
        val step = sayOnce(state, said, earcon)
        return Step(step.state, effects + step.effects)
    }

    /** Say it, unless it is exactly what was said last: then the "repeat" earcon, never silence. */
    private fun sayOnce(state: State, said: Said, earcon: Earcon): Step =
        if (said == state.lastSaid) Step(state, listOf(Effect.Sound(Earcon.REPEAT)))
        else Step(state.copy(lastSaid = said), listOf(Effect.Sound(earcon), Effect.Say(said)))
}
