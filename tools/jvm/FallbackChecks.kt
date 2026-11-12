import com.layerbit.ikshana.engine.mode.CameraState
import com.layerbit.ikshana.engine.mode.Earcon
import com.layerbit.ikshana.engine.mode.Effect
import com.layerbit.ikshana.engine.mode.Event
import com.layerbit.ikshana.engine.mode.Hint
import com.layerbit.ikshana.engine.mode.Line
import com.layerbit.ikshana.engine.mode.Mode
import com.layerbit.ikshana.engine.mode.ModeMachine
import com.layerbit.ikshana.engine.mode.ModeTable
import com.layerbit.ikshana.engine.mode.Models
import com.layerbit.ikshana.engine.mode.Said
import com.layerbit.ikshana.engine.mode.State
import com.layerbit.ikshana.engine.mode.Step

/**
 * The owner's second rule, executed: Ikshana never goes silent.
 *
 * Walks every state the mode machine can be in, over every combination of models present, absent
 * and failed, every camera state, both thermal states and every mode the user can want, and
 * applies every event to each. For each step it asks the machine's own output:
 *   - is the mode it landed in one the phone can run (camera there if needed, every model of the
 *     tier present and not failed)?
 *   - did anything the user can notice change, or did the user ask for something? Then a sentence
 *     was spoken, or (the sentence being exactly the last one) the "repeat" earcon was played;
 *   - landing in Help put the camera doors on the screen?
 *   - did a changed mode come with the instruction to run it?
 * Then the scenarios the owner named, with the sentence each must produce.
 */
object FallbackChecks : Suite {
    override val name = "fallback"

    /** Models move in the units they ship in: the OCR pair loads together or not at all. */
    private val units: List<Set<String>> = listOf(
        setOf(Models.OCR_DETECTOR, Models.OCR_RECOGNISER),
        setOf(Models.OCR_DETECTOR_LITE, Models.OCR_RECOGNISER_LITE),
        setOf(Models.CURRENCY), setOf(Models.VLM), setOf(Models.VLM_LITE),
        setOf(Models.TRANSLATE), setOf(Models.TRANSLATE_LITE), setOf(Models.OCR_TESSERACT)
    )

    private fun runnable(s: State): Boolean {
        val needs = ModeTable.of(s.active)
        val cameraOk = s.camera == CameraState.GRANTED || s.camera == CameraState.PICTURE
        if (needs.camera && !cameraOk) return false
        val tier = needs.tiers.getOrNull(s.tier) ?: return false
        return tier.all { it in s.shelf && it !in s.failed }
    }

    private fun answered(step: Step) =
        step.spoken.isNotEmpty() || step.effects.contains(Effect.Sound(Earcon.REPEAT))

    private fun events(): List<Event> = buildList {
        Mode.entries.forEach { add(Event.Started(it, false)); add(Event.Started(it, true)) }
        Mode.entries.forEach { add(Event.Choose(it)) }
        Models.all.forEach { add(Event.ModelFailed(it)) }
        CameraState.entries.forEach { add(Event.Camera(it)) }
        Hint.entries.forEach { add(Event.NothingFound(it)) }
        add(Event.TimedOut)
        add(Event.Thermal(true)); add(Event.Thermal(false))
    }

    private fun userFacing(e: Event) =
        e is Event.Started || e is Event.Choose || e is Event.NothingFound || e is Event.TimedOut

    override fun run() {
        section("the chain: every mode reaches Help, with no cycle, and Help needs nothing")
        for (m in Mode.entries) {
            val seen = mutableListOf<Mode>()
            var at: Mode? = m
            while (at != null) {
                check(at !in seen) { "cycle in the fallback chain from $m: $seen -> $at" }
                seen += at
                at = ModeTable.of(at).below
            }
            check(seen.last() == Mode.HELP) { "$m's chain ends at ${seen.last()}, not Help" }
        }
        val help = ModeTable.of(Mode.HELP)
        check(!help.camera && help.tiers.single().isEmpty()) { "Help must need nothing" }
        check(ModeTable.of(Mode.LIGHT).tiers.single().isEmpty()) { "Light must need no model" }
        check(ModeTable.needs.keys == Mode.entries.toSet()) { "a mode has no row in ModeTable" }
        for (m in Mode.entries) for (t in ModeTable.of(m).tiers) for (id in t) {
            check(id in Models.all) { "$m names unknown model $id" }
        }

        section("every state x every event: lands runnable, and never goes silent")
        val evs = events()
        var steps = 0
        val combos = Math.pow(3.0, units.size.toDouble()).toInt()
        for (c in 0 until combos) {
            var shelf = emptySet<String>()
            var failed = emptySet<String>()
            var k = c
            for (u in units) {
                when (k % 3) { 1 -> shelf = shelf + u; 2 -> { shelf = shelf + u; failed = failed + u } }
                k /= 3
            }
            for (cam in CameraState.entries) for (hot in listOf(false, true)) for (wanted in Mode.row) {
                val land = ModeMachine.land(State.initial(shelf, cam, failed), wanted)
                val base = State(wanted, land.mode, land.tier, cam, shelf, failed, hot, null)
                check(runnable(base)) { "land() chose an unrunnable $land for $wanted, shelf $shelf, failed $failed, $cam" }
                for (e in evs) {
                    val step = ModeMachine.step(base, e)
                    steps++
                    val n = step.state
                    check(runnable(n)) { "after $e from $base the app is in ${n.active}/${n.tier}, which cannot run" }
                    val changed = n.active != base.active || n.tier != base.tier || n.camera != base.camera || n.hot != base.hot
                    if (changed || userFacing(e)) {
                        check(answered(step)) { "SILENT: $e from $base -> ${step.effects}" }
                    }
                    if (n.active != base.active || n.tier != base.tier || e is Event.Started) {
                        check(step.effects.contains(Effect.Run(n.active, n.tier))) { "$e moved to ${n.active}/${n.tier} without running it: ${step.effects}" }
                    }
                    if (n.active == Mode.HELP && (base.active != Mode.HELP || e is Event.Started)) {
                        check(Effect.OfferCameraDoors in step.effects) { "$e landed in Help without the camera doors" }
                        check(step.spoken.any { it.line == Line.NO_CAMERA || it.line == Line.CAMERA_BUSY }) {
                            "$e landed in Help without saying how to get the camera back: ${step.spoken}"
                        }
                    }
                    if (e is Event.ModelFailed) {
                        val tier = ModeTable.of(n.active).tiers[n.tier]
                        check(e.model !in tier) { "${e.model} failed and is still in use by ${n.active}" }
                    }
                    // The same event again: either spoken anew, the repeat earcon, or nothing the
                    // user could notice changed and nothing was asked.
                    val again = ModeMachine.step(n, e)
                    val changedAgain = again.state.active != n.active || again.state.tier != n.tier
                    if (userFacing(e) || changedAgain) {
                        check(answered(again)) { "SILENT the second time: $e from $n -> ${again.effects}" }
                    }
                    // "Never twice" means never twice in a row within a session. Started opens a
                    // new one (a new process, after a crash a second "restarted" is the truth),
                    // so the history it is compared with is empty.
                    val heard = (if (e is Event.Started) emptyList() else listOfNotNull(n.lastSaid)) + again.spoken
                    check(heard.zipWithNext().none { (a, b) -> a == b }) { "said twice in a row after $e: $heard" }
                }
            }
        }
        println("      $steps steps, every one answered")

        section("the sentences the owner named")
        val all = Models.all.toSet()
        // The P0 phone: no model in the pack yet. Read cannot run; Light can.
        var st = ModeMachine.step(State.initial(emptySet(), CameraState.GRANTED), Event.Started(Mode.READ, false))
        check(st.spoken == listOf(Said(Line.WELCOME), Said(Line.NOT_READY, listOf(Mode.READ, Mode.LIGHT)))) { "P0 start said ${st.spoken}" }
        check(st.state.active == Mode.LIGHT) { "P0 start is in ${st.state.active}" }
        // Everything present: Money as asked.
        val full = ModeMachine.step(State.initial(all, CameraState.GRANTED), Event.Started(Mode.READ, false)).state
        st = ModeMachine.step(full, Event.Choose(Mode.MONEY))
        check(st.spoken == listOf(Said(Line.MODE_ON, listOf(Mode.MONEY)))) { "Money said ${st.spoken}" }
        check(st.effects.first() == Effect.Run(Mode.MONEY, 0)) { "Money ran ${st.effects}" }
        // The vision model fails to load: the lighter twin takes over and says so.
        val describing = ModeMachine.step(full, Event.Choose(Mode.DESCRIBE)).state
        st = ModeMachine.step(describing, Event.ModelFailed(Models.VLM))
        check(st.spoken == listOf(Said(Line.LIGHTER, listOf(Mode.DESCRIBE))) && st.state.tier == 1) { "lighter twin: ${st.spoken} tier ${st.state.tier}" }
        // Both fail: Read and Money still work, and Ask lands on Read saying it could not start.
        val asking = ModeMachine.step(st.state, Event.Choose(Mode.ASK)).state
        st = ModeMachine.step(asking, Event.ModelFailed(Models.VLM_LITE))
        check(st.spoken == listOf(Said(Line.COULD_NOT_START, listOf(Mode.ASK, Mode.READ)))) { "Ask with no vision model: ${st.spoken}" }
        check(st.state.active == Mode.READ) { "Ask fell to ${st.state.active}" }
        check(ModeMachine.step(st.state, Event.Choose(Mode.MONEY)).state.active == Mode.MONEY) { "Money must survive the vision model's failure" }
        // The camera is denied: say how to allow it and offer the gallery; then it comes back.
        st = ModeMachine.step(full, Event.Camera(CameraState.DENIED))
        check(st.spoken == listOf(Said(Line.NO_CAMERA)) && Effect.OfferCameraDoors in st.effects) { "denied: ${st.effects}" }
        st = ModeMachine.step(st.state, Event.Camera(CameraState.GRANTED))
        check(st.spoken == listOf(Said(Line.CAMERA_BACK, listOf(Mode.READ))) && st.state.active == Mode.READ) { "granted again: ${st.spoken}" }
        // A picture from the gallery stands in for the camera.
        val denied = ModeMachine.step(full, Event.Camera(CameraState.DENIED)).state
        check(ModeMachine.step(denied, Event.Camera(CameraState.PICTURE)).state.active == Mode.READ) { "a picked picture must run Read" }
        // Nothing found: say so and what to change; the same again is not said twice.
        st = ModeMachine.step(full, Event.NothingFound(Hint.TOO_DARK))
        check(st.spoken == listOf(Said(Line.TOO_DARK))) { "too dark: ${st.spoken}" }
        st = ModeMachine.step(st.state, Event.NothingFound(Hint.TOO_DARK))
        check(st.spoken.isEmpty() && st.effects == listOf(Effect.Sound(Earcon.REPEAT))) { "too dark twice: ${st.effects}" }
        st = ModeMachine.step(st.state, Event.NothingFound(Hint.TOO_FAR))
        check(st.spoken == listOf(Said(Line.MOVE_CLOSER))) { "too far: ${st.spoken}" }
        // A crash: spoken, and the mode comes back where it was.
        st = ModeMachine.step(State.initial(all, CameraState.GRANTED), Event.Started(Mode.DESCRIBE, true))
        check(st.spoken == listOf(Said(Line.RESTARTED, listOf(Mode.DESCRIBE))) && st.state.active == Mode.DESCRIBE) { "restart: ${st.spoken}" }
        // A crash with the camera gone still says how to get it back.
        st = ModeMachine.step(State.initial(all, CameraState.DENIED), Event.Started(Mode.FIND, true))
        check(st.spoken == listOf(Said(Line.RESTARTED, listOf(Mode.HELP)), Said(Line.NO_CAMERA))) { "restart without camera: ${st.spoken}" }
        // Only the interim reader in the pack: Read is Read, said plainly, not "the lighter model".
        val interim = ModeMachine.step(State.initial(setOf(Models.OCR_TESSERACT), CameraState.GRANTED), Event.Started(Mode.READ, false))
        check(interim.spoken == listOf(Said(Line.WELCOME)) && interim.state.active == Mode.READ) { "interim start: ${interim.spoken}" }
        st = ModeMachine.step(interim.state, Event.Choose(Mode.READ))
        check(st.spoken == listOf(Said(Line.MODE_ON, listOf(Mode.READ))) && Effect.Sound(Earcon.MODE) in st.effects) { "interim Read: ${st.effects}" }
        // Ours in the pack but failing here: then the interim one is the lighter one, and says so.
        val both = State.initial(setOf(Models.OCR_DETECTOR, Models.OCR_RECOGNISER, Models.OCR_TESSERACT), CameraState.GRANTED)
        st = ModeMachine.step(ModeMachine.step(both, Event.Started(Mode.READ, false)).state, Event.ModelFailed(Models.OCR_RECOGNISER))
        check(st.spoken == listOf(Said(Line.LIGHTER, listOf(Mode.READ))) && st.state.tier == 2) { "ours failing: ${st.spoken}" }
        // Heat, both ways.
        st = ModeMachine.step(full, Event.Thermal(true))
        check(st.spoken == listOf(Said(Line.WARM))) { "warm: ${st.spoken}" }
        check(ModeMachine.step(st.state, Event.Thermal(false)).spoken == listOf(Said(Line.COOL))) { "cool" }
        // Recovery climbs back: the user wanted Find; with the camera back Find runs, not Read.
        val finding = ModeMachine.step(full, Event.Choose(Mode.FIND)).state
        val lost = ModeMachine.step(finding, Event.Camera(CameraState.DENIED)).state
        check(ModeMachine.step(lost, Event.Camera(CameraState.GRANTED)).state.active == Mode.FIND) { "recovery must return to the wanted mode" }
    }
}
