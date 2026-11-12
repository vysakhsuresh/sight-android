import com.layerbit.ikshana.engine.feedback.Feedback
import com.layerbit.ikshana.engine.mode.Earcon
import kotlin.math.abs

/**
 * Every earcon and its haptic twin. The tone is measured (its pitch from zero crossings, its
 * edges, its peak), not trusted from the table; the twin is held to the tone's length and shape,
 * because a deaf-blind user has only the twin.
 */
object EarconChecks : Suite {
    override val name = "earcon"

    /** Zero crossings per second over a slice: twice the frequency of a sine. */
    private fun hz(pcm: ShortArray, from: Int, to: Int, rate: Int): Double {
        var n = 0
        for (i in from + 1 until to) if ((pcm[i - 1] < 0) != (pcm[i] < 0)) n++
        return n / 2.0 / ((to - from).toDouble() / rate)
    }

    override fun run() {
        val rate = 48_000
        for (e in Earcon.entries) {
            section(e.name)
            val pcm = Feedback.pcm(e, rate)
            val ms = Feedback.lengthMs(e)
            near(pcm.size.toDouble(), ms * rate / 1000.0, rate / 1000.0 * 2, "$e length in samples")
            val peak = pcm.maxOf { abs(it.toInt()) }
            check(peak in 1..(Short.MAX_VALUE * Feedback.PEAK + 1).toInt()) { "$e peak $peak over half scale" }
            // No click: the first and last samples of every tone are near zero.
            check(abs(pcm.first().toInt()) < 200 && abs(pcm.last().toInt()) < 200) { "$e starts or ends on ${pcm.first()} / ${pcm.last()}: a click" }
            val twin = Feedback.twin(e)
            check(abs(twin.totalMs - ms) <= 10) { "$e twin is ${twin.totalMs} ms, the tone $ms" }
            check(twin.amplitudes.all { it in 0..255 }) { "$e twin amplitudes out of range" }
            check(twin.amplitudes.count { it > 0 } >= 1) { "$e twin never vibrates" }
        }

        section("rising rises and falling falls, heard and felt")
        fun direction(e: Earcon): Double {
            val p = Feedback.pcm(e, rate)
            val q = p.size / 4
            // Second quarter against the fourth: 768 Hz against 939 for the 660-990 glide.
            return hz(p, 3 * q, 4 * q, rate) - hz(p, q, 2 * q, rate)
        }
        check(direction(Earcon.GATE_OPEN) > 100) { "gate-open must rise: ${direction(Earcon.GATE_OPEN)} Hz" }
        check(direction(Earcon.GATE_CLOSED) < -100) { "gate-closed must fall: ${direction(Earcon.GATE_CLOSED)} Hz" }
        near(hz(Feedback.pcm(Earcon.MODE, rate), 600, 2800, rate), 880.0, 30.0, "mode note")
        val up = Feedback.twin(Earcon.GATE_OPEN).amplitudes
        val down = Feedback.twin(Earcon.GATE_CLOSED).amplitudes
        check(up.toList().zipWithNext().all { (a, b) -> b >= a } && up.last() > up.first()) { "gate-open's twin must grow: ${up.toList()}" }
        check(down.toList().zipWithNext().all { (a, b) -> b <= a } && down.last() < down.first()) { "gate-closed's twin must fade: ${down.toList()}" }

        section("two notes are two pulses")
        for (e in listOf(Earcon.FALLBACK, Earcon.REPEAT)) {
            val a = Feedback.twin(e).amplitudes
            val pulses = a.toList().zipWithNext().count { (x, y) -> x == 0 && y > 0 } + 1
            check(pulses == 2) { "$e twin has $pulses pulses: ${a.toList()}" }
        }
        val fb = Feedback.twin(Earcon.FALLBACK).amplitudes
        check(fb.first() > fb.last()) { "the fallback's two notes fall, so its pulses must too" }
    }
}
