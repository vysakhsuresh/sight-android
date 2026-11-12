package com.layerbit.ikshana.engine.feedback

import com.layerbit.ikshana.engine.mode.Earcon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Earcons and their haptic twins, made from numbers rather than shipped as sound files.
 *
 * Every earcon has a twin for the deaf-blind (brief, section 4): the same length, and the same
 * shape felt instead of heard: a rising tone is a vibration that grows, a falling one fades, a
 * two-note fall is two pulses. EarconChecks holds each pair together, so a tone changed without
 * its twin fails on the JVM.
 *
 * A tone is a glide from [Tone.fromHz] to [Tone.toHz], geometric so the rise sounds even, with a
 * raised-cosine edge at each end: a tone that starts on a non-zero sample clicks, and the click
 * is louder than the tone on a phone speaker. Peak is half of full scale so an earcon under
 * speech never drowns it.
 */
data class Tone(val fromHz: Double, val toHz: Double, val ms: Int, val gapAfterMs: Int = 0)

/** Android's VibrationEffect.createWaveform shape: segment lengths and 0..255 amplitudes. */
class Pattern(val timingsMs: LongArray, val amplitudes: IntArray) {
    init { require(timingsMs.size == amplitudes.size) { "timings and amplitudes differ in length" } }
    val totalMs: Long get() = timingsMs.sum()
}

object Feedback {
    const val PEAK = 0.5
    const val EDGE_MS = 8

    fun tones(earcon: Earcon): List<Tone> = when (earcon) {
        // Brief, section 4: one short rising tone when the frame gate opens, one falling when it closes.
        Earcon.GATE_OPEN -> listOf(Tone(660.0, 990.0, 120))
        Earcon.GATE_CLOSED -> listOf(Tone(990.0, 660.0, 120))
        // A mode came on as asked: one clear note.
        Earcon.MODE -> listOf(Tone(880.0, 880.0, 70))
        // Landed below what was asked: two notes falling, before the sentence that says why.
        Earcon.FALLBACK -> listOf(Tone(784.0, 784.0, 90, gapAfterMs = 40), Tone(587.0, 587.0, 120))
        // Looked and found nothing: one low, soft note.
        Earcon.NOTHING -> listOf(Tone(440.0, 440.0, 90))
        // The same sentence would have been said twice: two quick identical ticks, "still here".
        Earcon.REPEAT -> listOf(Tone(660.0, 660.0, 40, gapAfterMs = 60), Tone(660.0, 660.0, 40))
    }

    fun lengthMs(earcon: Earcon): Int = tones(earcon).sumOf { it.ms + it.gapAfterMs }

    /** 16-bit mono PCM at [rate]. */
    fun pcm(earcon: Earcon, rate: Int = 48_000): ShortArray {
        val out = ArrayList<Short>()
        for (t in tones(earcon)) {
            val n = t.ms * rate / 1000
            val edge = EDGE_MS * rate / 1000
            var phase = 0.0
            val k = ln(t.toHz / t.fromHz) / n
            for (i in 0 until n) {
                val hz = t.fromHz * exp(k * i)
                phase += 2 * PI * hz / rate
                val gain = when {
                    i < edge -> 0.5 - 0.5 * cos(PI * i / edge)
                    i >= n - edge -> 0.5 - 0.5 * cos(PI * (n - 1 - i) / edge)
                    else -> 1.0
                }
                out += (sin(phase) * gain * PEAK * Short.MAX_VALUE).roundToInt().toShort()
            }
            repeat(t.gapAfterMs * rate / 1000) { out += 0 }
        }
        return out.toShortArray()
    }

    /**
     * The twin: each tone becomes one vibration of the same length, cut into 10 ms steps whose
     * amplitude follows the pitch (higher is stronger), and each gap a pause of the same length.
     */
    fun twin(earcon: Earcon): Pattern {
        val timings = ArrayList<Long>()
        val amps = ArrayList<Int>()
        val all = tones(earcon)
        val lo = all.minOf { minOf(it.fromHz, it.toHz) }
        val hi = all.maxOf { maxOf(it.fromHz, it.toHz) }
        for (t in all) {
            val steps = maxOf(1, t.ms / 10)
            for (s in 0 until steps) {
                val ms = if (s == steps - 1) t.ms - 10 * (steps - 1) else 10
                val f = (s + 0.5) / steps
                val hz = t.fromHz * exp(ln(t.toHz / t.fromHz) * f)
                val rel = if (hi > lo) (hz - lo) / (hi - lo) else 1.0
                timings += ms.toLong()
                amps += (90 + rel * 165).roundToInt().coerceIn(1, 255)
            }
            if (t.gapAfterMs > 0) { timings += t.gapAfterMs.toLong(); amps += 0 }
        }
        return Pattern(timings.toLongArray(), amps.toIntArray())
    }
}
