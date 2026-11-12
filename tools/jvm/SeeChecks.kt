import com.layerbit.ikshana.engine.see.Describe
import com.layerbit.ikshana.engine.see.DescribePacer
import com.layerbit.ikshana.engine.see.Find
import com.layerbit.ikshana.engine.see.FrameGate
import com.layerbit.ikshana.engine.see.Gate
import com.layerbit.ikshana.engine.see.Prompts
import com.layerbit.ikshana.engine.see.Question
import com.layerbit.ikshana.engine.see.Side

/**
 * Describe, Find and Ask, the parts that are ours. A vision model answers every frame, black or
 * smeared, and answers with confidence; these checks hold the gate in front of it and the cuts
 * after it, and the one rule that matters most for Find: no box, no direction.
 */
object SeeChecks : Suite {
    override val name = "see"

    private fun plane(w: Int, h: Int, f: (Int, Int) -> Int) = ByteArray(w * h) { i -> f(i % w, i / w).toByte() }

    override fun run() {
        section("the frame gate")
        val w = 320; val h = 240
        check(FrameGate.check(plane(w, h) { _, _ -> 8 }, w, h) == Gate.TOO_DARK) { "black is too dark" }
        check(FrameGate.check(plane(w, h) { _, _ -> 140 }, w, h) == Gate.BLURRED) { "a flat grey field is all blur" }
        check(FrameGate.check(plane(w, h) { x, y -> if ((x / 8 + y / 8) % 2 == 0) 40 else 220 }, w, h) == Gate.OK) { "a sharp checkerboard is fine" }
        check(FrameGate.check(plane(w, h) { x, _ -> 100 + x * 100 / w }, w, h) == Gate.BLURRED) { "a smooth ramp has no edges" }
        // bright pixels are negative as signed bytes; the mean must still be bright
        check(FrameGate.check(plane(w, h) { x, y -> if ((x / 8 + y / 8) % 2 == 0) 200 else 250 }, w, h) == Gate.OK) { "bright frame read as dark (signed bytes)" }

        section("what reaches the voice from a description")
        check(Describe.clean("The image shows a steel tumbler on a kitchen counter.") == "A steel tumbler on a kitchen counter.") { "preamble" }
        check(Describe.clean("In this image, there are two cups on a table") == "Two cups on a table.") { "stacked preambles, full stop added" }
        check(Describe.clean("A bus stop. A red bus on the left. People waiting. A shop behind.") == "A bus stop. A red bus on the left.") { "two sentences" }
        check(Describe.clean("I seem to see a door.") == "I seem to see a door.") { "whole words only" }
        check(Describe.clean("   ") == null && Describe.clean("The image shows") == null) { "nothing left is null, not silence dressed as text" }
        val long = (1..80).joinToString(" ") { "word" }
        check(Describe.clean(long)!!.split(' ').size == Describe.MAX_WORDS) { "word cap" }

        section("Describe speaks again only when the scene changes")
        val wall = DescribePacer.thumb(plane(w, h) { _, _ -> 120 }, w, h)
        val door = DescribePacer.thumb(plane(w, h) { x, _ -> if (x < w / 2) 30 else 200 }, w, h)
        val p = DescribePacer()
        check(p.due(0, wall)) { "the first frame is described" }
        p.described(0, wall)
        check(!p.due(DescribePacer.MIN_GAP_MS + 1, wall)) { "the same wall is not described again" }
        check(!p.due(1_000, door)) { "a new scene inside the gap waits" }
        check(p.due(DescribePacer.MIN_GAP_MS + 1, door)) { "a new scene after the gap is described" }
        p.clear()
        check(p.due(1, wall)) { "after leaving the mode, the first frame again" }

        section("Find: a box becomes a direction, and nothing else does")
        check(Find.parseBox("[100, 200, 300, 400]")!!.contentEquals(doubleArrayOf(.1, .2, .3, .4))) { "parse" }
        check(Find.parseBox("The bottle is at [0, 0, 0, 0]") == null) { "an empty box is no box" }
        check(Find.parseBox("[500, 200, 300, 400]") == null) { "inverted box" }
        check(Find.parseBox("[100, 200, 1300, 400]") == null) { "off the grid" }
        check(Find.parseBox("bottle on the left") == null) { "words are not a box" }
        check(Find.place(doubleArrayOf(.0, .4, .2, .6)).side == Side.LEFT) { "left" }
        check(Find.place(doubleArrayOf(.8, .4, 1.0, .6)).side == Side.RIGHT) { "right" }
        check(Find.place(doubleArrayOf(.4, .4, .6, .6)).side == Side.AHEAD) { "ahead" }
        check(Find.place(doubleArrayOf(.1, .1, .9, .9)).near && !Find.place(doubleArrayOf(.45, .45, .55, .55)).near) { "near is big" }
        check(Find.saysAbsent("No.") && Find.saysAbsent("There is no bottle") && Find.saysAbsent("I can't see one")) { "absent" }
        check(!Find.saysAbsent("Yes, [1, 2, 3, 4]") && !Find.saysAbsent("Notebook [1,2,3,4]")) { "'Notebook' is not 'no'" }

        section("Ask and the prompts")
        check(Question.entries.map { it.id }.toSet().size == Question.entries.size) { "question ids unique" }
        check(Question.entries.all { it.english.endsWith("?") }) { "questions are questions" }
        check(Prompts.find("bottle").contains("[x1, y1, x2, y2]") && Prompts.find("bottle").contains("0 to 1000")) { "find prompt asks for the grid parseBox reads" }
        check(Prompts.DESCRIBE.contains("identity")) { "describe prompt forbids guessing who" }
    }
}
