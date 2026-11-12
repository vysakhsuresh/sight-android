import com.layerbit.ikshana.engine.light.LightBand
import com.layerbit.ikshana.engine.light.LightLevel

/**
 * The floor mode with a camera. The mean is checked on planes with a known answer, including a
 * row stride wider than the image (CameraX pads rows, and reading the padding as picture moves
 * the mean); the bands on their edges; and the hysteresis that keeps a mean sitting on an edge
 * from making the voice flicker.
 */
object LightChecks : Suite {
    override val name = "light"

    private fun plane(w: Int, h: Int, stride: Int, pad: Int, value: (Int, Int) -> Int): ByteArray {
        val b = ByteArray(stride * h) { pad.toByte() }
        for (r in 0 until h) for (c in 0 until w) b[r * stride + c] = value(r, c).toByte()
        return b
    }

    override fun run() {
        section("mean luma on known planes")
        near(LightLevel.meanLuma(plane(64, 48, 64, 0) { _, _ -> 200 }, 64, 48, 64), 200.0, 1e-9, "flat 200 (above 127: bytes are unsigned)")
        near(LightLevel.meanLuma(plane(64, 48, 80, 255) { _, _ -> 10 }, 64, 48, 80), 10.0, 1e-9, "padding of 255 must not count")
        // Left half 0, right half 240: the 1-in-8 grid samples both halves equally.
        near(LightLevel.meanLuma(plane(64, 48, 64, 0) { _, c -> if (c < 32) 0 else 240 }, 64, 48, 64), 120.0, 1e-9, "half and half")
        near(LightLevel.meanLuma(plane(7, 5, 7, 0) { r, c -> r * 7 + c }, 7, 5, 7, step = 1), 17.0, 1e-9, "every pixel, ramp 0..34")

        section("a picture's pixels on the camera's scale")
        near(LightLevel.meanLumaArgb(intArrayOf(0xFFFFFFFF.toInt())), 255.0, 1.0, "white")
        near(LightLevel.meanLumaArgb(intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt())), 0.0, 1e-9, "black, alpha ignored")
        near(LightLevel.meanLumaArgb(intArrayOf(0xFF00FF00.toInt())), 149.4, 1.0, "green weighs most (0.587)")
        near(LightLevel.meanLumaArgb(intArrayOf(0xFF0000FF.toInt())), 28.9, 1.0, "blue weighs least (0.114)")

        section("bands on their edges")
        check(LightLevel.bandOf(0.0) == LightBand.DARK) { "0 is dark" }
        check(LightLevel.bandOf(27.9) == LightBand.DARK && LightLevel.bandOf(28.0) == LightBand.DIM) { "dark/dim edge at 28" }
        check(LightLevel.bandOf(79.9) == LightBand.DIM && LightLevel.bandOf(80.0) == LightBand.BRIGHT) { "dim/bright edge at 80" }
        check(LightLevel.bandOf(199.9) == LightBand.BRIGHT && LightLevel.bandOf(200.0) == LightBand.GLARE) { "bright/glare edge at 200" }

        section("hysteresis: a mean on the edge does not flicker")
        var band: LightBand? = null
        val wobble = listOf(78.0, 81.0, 79.0, 82.0, 77.0, 83.0, 80.0)
        val heard = mutableListOf<LightBand>()
        for (m in wobble) { val b = LightLevel.next(band, m); if (b != band) heard += b; band = b }
        check(heard == listOf(LightBand.DIM)) { "a mean wobbling 77..83 across the 80 edge was spoken as $heard" }
        check(LightLevel.next(LightBand.DIM, 86.0) == LightBand.BRIGHT) { "past the edge by the hysteresis must move" }
        check(LightLevel.next(LightBand.BRIGHT, 75.0) == LightBand.BRIGHT) { "75 is within the hysteresis below 80" }
        check(LightLevel.next(LightBand.BRIGHT, 73.0) == LightBand.DIM) { "73 is past it" }
        check(LightLevel.next(LightBand.DARK, 120.0) == LightBand.BRIGHT) { "a lamp switched on moves two bands at once" }
        check(LightLevel.next(null, 5.0) == LightBand.DARK) { "no history takes the plain band" }
    }
}
