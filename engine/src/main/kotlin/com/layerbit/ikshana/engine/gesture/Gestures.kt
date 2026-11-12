package com.layerbit.ikshana.engine.gesture

import kotlin.math.abs

/**
 * The gestures of the brief's section 4, the same on every screen: a tap anywhere pauses the
 * voice and a second tap resumes it; a swipe down repeats; a swipe up reads the next thing.
 * Left and right step through the modes, so the row is reachable without finding it.
 *
 * Classified here, in dp and milliseconds, so the thresholds are executed by GestureChecks
 * rather than tuned by feel. With TalkBack on, the system takes single-finger gestures for its
 * own and these reach the app only through its own actions (the app exposes them as custom
 * accessibility actions); this classifier is for TalkBack off, which is how many low-vision
 * users and every sighted user hold the phone.
 */
enum class Gesture { TAP, SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT, NONE }

object Gestures {
    /** Further than this is not a tap: Android's own touch slop is 8 dp; a shaking hand needs more. */
    const val TAP_SLOP_DP = 24f
    /** A tap held longer than this is a press, which nothing here uses. */
    const val TAP_MAX_MS = 400L
    /** A swipe travels at least this far along its axis. */
    const val SWIPE_MIN_DP = 64f
    /** And takes no longer than this: a slow drag is someone steadying the camera, not a command. */
    const val SWIPE_MAX_MS = 900L
    /** Its main axis must dominate the other by this ratio, or it is a diagonal and nothing. */
    const val AXIS_RATIO = 1.5f

    fun classify(dxPx: Float, dyPx: Float, durationMs: Long, density: Float): Gesture {
        require(density > 0f) { "density $density" }
        val dx = dxPx / density
        val dy = dyPx / density
        val ax = abs(dx)
        val ay = abs(dy)
        if (ax <= TAP_SLOP_DP && ay <= TAP_SLOP_DP) {
            return if (durationMs <= TAP_MAX_MS) Gesture.TAP else Gesture.NONE
        }
        if (durationMs > SWIPE_MAX_MS) return Gesture.NONE
        return when {
            ay >= SWIPE_MIN_DP && ay >= ax * AXIS_RATIO -> if (dy > 0) Gesture.SWIPE_DOWN else Gesture.SWIPE_UP
            ax >= SWIPE_MIN_DP && ax >= ay * AXIS_RATIO -> if (dx > 0) Gesture.SWIPE_RIGHT else Gesture.SWIPE_LEFT
            else -> Gesture.NONE
        }
    }
}
