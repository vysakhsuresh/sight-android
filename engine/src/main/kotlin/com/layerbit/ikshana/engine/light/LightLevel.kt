package com.layerbit.ikshana.engine.light

/**
 * How much light the camera sees, in four words, for the person who cannot tell if the lamp is on.
 *
 * It is the floor mode with a camera (Mode.LIGHT): no model, nothing to load, nothing that can
 * fail but the camera itself, which is why it sits under Read in the fallback chain. The input
 * is the Y plane CameraX hands ImageAnalysis (luma 0..255, BT.601 limited or full range; the
 * difference moves the band edges by a few levels and the hysteresis absorbs it).
 *
 * Bands are spoken only when they change, and a change needs the mean to cross the edge by
 * [HYSTERESIS] levels: a mean sitting on an edge flickers by a level or two frame to frame from
 * sensor noise and auto-exposure hunting, and a voice saying "dim, bright, dim" is worse than
 * none. The camera's auto-exposure also pulls every scene toward middle grey, so the edges were
 * chosen on the luma it delivers, not on lux: a dark room under auto-exposure still reads far
 * below a lit one because the exposure runs out of range first. P0 values; P1's set and the
 * phone set the final ones (docs/PENDING_TESTS.md).
 */
enum class LightBand(val id: String) { DARK("dark"), DIM("dim"), BRIGHT("bright"), GLARE("glare") }

object LightLevel {
    /** Upper edges of DARK, DIM and BRIGHT on mean luma; above the last is GLARE. */
    val EDGES = intArrayOf(28, 80, 200)
    const val HYSTERESIS = 6

    /**
     * Frames are not read for this long after the camera's first frame. Seen on the moto on
     * 12 November: the first analysed frame read "Dark." while auto-exposure was still opening
     * up, and "Bright light." a second later, in a lit room. A false "dark" is the one reading
     * this mode must never give, so the camera is given the time to settle first.
     */
    const val SETTLE_MS = 1500L

    /**
     * Mean luma over a grid of samples, every [step]th pixel each way. The full plane is 2 MP on
     * the moto; a 1-in-8 grid is 30k samples, enough for a mean to a fraction of a level, and
     * keeps the analysis thread far under a frame.
     */
    fun meanLuma(y: ByteArray, width: Int, height: Int, rowStride: Int, step: Int = 8): Double {
        require(width > 0 && height > 0 && rowStride >= width && step >= 1) { "bad plane $width x $height / $rowStride" }
        var sum = 0L
        var n = 0L
        var row = 0
        while (row < height) {
            val base = row * rowStride
            var col = 0
            while (col < width) {
                sum += y[base + col].toInt() and 0xFF
                n++
                col += step
            }
            row += step
        }
        return sum.toDouble() / n
    }

    /**
     * Mean luma of ARGB pixels (a picture from the gallery, when the camera is not there), with
     * BT.601's weights so it reads on the same scale as the camera's Y plane. Integer weights
     * summing to 256: 77 R + 150 G + 29 B.
     */
    fun meanLumaArgb(pixels: IntArray): Double {
        require(pixels.isNotEmpty()) { "no pixels" }
        var sum = 0L
        for (p in pixels) sum += (77 * ((p shr 16) and 0xFF) + 150 * ((p shr 8) and 0xFF) + 29 * (p and 0xFF)) shr 8
        return sum.toDouble() / pixels.size
    }

    /** The band for [mean] with no history. */
    fun bandOf(mean: Double): LightBand {
        for (i in EDGES.indices) if (mean < EDGES[i]) return LightBand.entries[i]
        return LightBand.GLARE
    }

    /**
     * The band for [mean] given the band last spoken: it moves only when the mean is past the
     * shared edge by the hysteresis. Two bands apart moves at once (a lamp switched on).
     */
    fun next(previous: LightBand?, mean: Double): LightBand {
        val plain = bandOf(mean)
        if (previous == null || plain == previous) return plain
        val p = previous.ordinal
        val q = plain.ordinal
        if (kotlin.math.abs(q - p) > 1) return plain
        val edge = EDGES[minOf(p, q)]
        return if (q > p) { if (mean >= edge + HYSTERESIS) plain else previous }
        else { if (mean < edge - HYSTERESIS) plain else previous }
    }
}
