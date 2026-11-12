import com.layerbit.ikshana.engine.gesture.Gesture
import com.layerbit.ikshana.engine.gesture.Gestures

/**
 * The gestures of section 4, in pixels at the moto's density (2.625: 1080 px over 411 dp) and at
 * 1.0, so a threshold written in pixels by mistake fails on one of the two.
 */
object GestureChecks : Suite {
    override val name = "gesture"

    override fun run() {
        for (d in listOf(1f, 2.625f)) {
            section("density $d")
            fun g(dxDp: Float, dyDp: Float, ms: Long) = Gestures.classify(dxDp * d, dyDp * d, ms, d)
            check(g(0f, 0f, 80) == Gesture.TAP) { "a still tap" }
            check(g(20f, -18f, 200) == Gesture.TAP) { "a shaking tap within 24 dp is still a tap" }
            check(g(0f, 0f, 700) == Gesture.NONE) { "a long press is nothing" }
            check(g(0f, 120f, 250) == Gesture.SWIPE_DOWN) { "down repeats" }
            check(g(0f, -120f, 250) == Gesture.SWIPE_UP) { "up reads the next" }
            check(g(-120f, 10f, 250) == Gesture.SWIPE_LEFT) { "left" }
            check(g(120f, -10f, 250) == Gesture.SWIPE_RIGHT) { "right" }
            check(g(100f, 100f, 250) == Gesture.NONE) { "a diagonal is nothing" }
            check(g(0f, 50f, 250) == Gesture.NONE) { "50 dp is neither a tap nor a swipe" }
            check(g(0f, 200f, 1500) == Gesture.NONE) { "a slow drag is steadying the camera" }
            check(g(0f, 64f, 900) == Gesture.SWIPE_DOWN) { "exactly the minimum at exactly the limit" }
        }
    }
}
