package com.layerbit.ikshana.engine.see

import com.layerbit.ikshana.engine.feedback.Pattern
import kotlin.math.abs

/**
 * The rules between the vision model and the voice, for Describe, Find and Ask (brief, Pillars
 * B to D). Pure, so SeeChecks executes them. The model itself is not here: what it may be shown,
 * how its words are cut before a blind listener hears them, and how a box it draws becomes
 * "to your left" are.
 */

/** Whether a frame is worth showing the model. A blurred or black frame gets an answer anyway,
 *  and that answer is invented; so the frame is refused and the listener told why. */
enum class Gate { OK, TOO_DARK, BLURRED }

object FrameGate {
    /** Mean luma below this (0..255) and the scene is not there to describe. Interim: from
     *  LightLevel's "dark" edge; to be set from the test set's dark frames (P1). */
    const val DARK = 28

    /** Mean absolute 4-neighbour Laplacian below this and edges are smeared. Interim, from first
     *  principles: a sharp indoor frame gives tens, a shaken one a few. Set from the test set. */
    const val SHARP = 4.0

    /** Sample every [step]th pixel of a tight luma plane; a full 1280x960 pass costs more than
     *  the answer is worth on a 4 GB phone. */
    fun check(y: ByteArray, width: Int, height: Int, step: Int = 4): Gate {
        require(width > 2 * step && height > 2 * step) { "frame $width x $height" }
        var sum = 0L; var n = 0
        var lap = 0L; var m = 0
        var r = step
        while (r < height - step) {
            var c = step
            while (c < width - step) {
                val v = y[r * width + c].toInt() and 0xFF   // signed bytes: the bug LightChecks caught
                sum += v; n++
                val l = 4 * v - (y[(r - step) * width + c].toInt() and 0xFF) - (y[(r + step) * width + c].toInt() and 0xFF) -
                    (y[r * width + c - step].toInt() and 0xFF) - (y[r * width + c + step].toInt() and 0xFF)
                lap += abs(l); m++
                c += step
            }
            r += step
        }
        if (sum / n < DARK) return Gate.TOO_DARK
        return if (lap.toDouble() / m < SHARP) Gate.BLURRED else Gate.OK
    }
}

object Describe {
    /** At most this many sentences reach the voice: a description is a glance, not a tour; the
     *  listener asks for more (Ask) when they want it. */
    const val MAX_SENTENCES = 2

    /** And at most this many words, for a model that writes one endless sentence. */
    const val MAX_WORDS = 40

    /** Openings a small model puts before every answer; spoken, they are noise the listener hears
     *  a hundred times a day. Lower case, matched at the start of the text. */
    private val preambles = listOf(
        "the image shows", "the image depicts", "the image features", "the photo shows", "the picture shows",
        "this image shows", "this is an image of", "this is a picture of", "this is a photo of",
        "in this image,", "in the image,", "in this picture,", "i can see", "i see", "the scene shows",
        "the image is of", "it looks like", "it appears to be", "there is", "there are",
        // the chosen model's own habit on the VizWiz comparison (12 Nov): it echoes the prompt
        "in front of the camera for a blind person", "in front of the camera", "the main thing in front of the camera is"
    )

    /**
     * The words to speak, or null when nothing worth saying is left. Preambles go (keeping what
     * follows, capitalised: "There are two cups" becomes "Two cups"), whitespace collapses, the
     * first [MAX_SENTENCES] sentences stay, cut at [MAX_WORDS], ending with a full stop.
     */
    fun clean(raw: String): String? {
        var t = raw.replace(Regex("\\s+"), " ").trim().trim('"')
        var changed = true
        while (changed) {
            changed = false
            for (p in preambles) if (t.lowercase().startsWith(p) && (t.length == p.length || !t[p.length].isLetter())) {
                // whole words only: "I seem" is not "I see"
                t = t.substring(p.length).trimStart(' ', ',', ':'); changed = true
            }
        }
        if (t.isBlank()) return null
        val sentences = Regex("(?<=[.!?])\\s+").split(t).filter { it.isNotBlank() }.take(MAX_SENTENCES)
        var words = sentences.joinToString(" ").split(' ')
        if (words.size > MAX_WORDS) words = words.take(MAX_WORDS)
        var out = words.joinToString(" ").trimEnd(',', ';', ':', ' ')
        if (out.isEmpty()) return null
        out = out.replaceFirstChar { it.uppercaseChar() }
        if (out.last() !in ".!?") out += "."
        return out
    }
}

/**
 * When Describe speaks again. Every frame would be a voice that never stops and a phone that
 * burns; once would leave the listener who turned round in silence. So: the first settled frame,
 * then again only when the scene has changed (a coarse grid of mean brightness moved by
 * [CHANGE] on average) and at least [MIN_GAP_MS] after the last description began.
 */
class DescribePacer {
    private var last: IntArray? = null
    private var lastAt = -1L

    fun due(nowMs: Long, thumb: IntArray): Boolean {
        val l = last ?: return true
        if (nowMs - lastAt < MIN_GAP_MS) return false
        return difference(l, thumb) >= CHANGE
    }

    fun described(nowMs: Long, thumb: IntArray) { last = thumb; lastAt = nowMs }

    fun clear() { last = null; lastAt = -1L }

    companion object {
        const val MIN_GAP_MS = 6_000L
        /** Mean change per grid cell (0..255) that counts as a new scene. Interim: a hand
         *  passing is about 10, turning to face another wall 30 or more. Set on the phone. */
        const val CHANGE = 18.0
        const val GRID_W = 16
        const val GRID_H = 12

        /** The mean of each of GRID_W x GRID_H cells of a tight luma plane. */
        fun thumb(y: ByteArray, width: Int, height: Int): IntArray {
            val sums = LongArray(GRID_W * GRID_H); val counts = IntArray(GRID_W * GRID_H)
            var r = 0
            while (r < height) {
                var c = 0
                while (c < width) {
                    val cell = (r * GRID_H / height) * GRID_W + (c * GRID_W / width)
                    sums[cell] += (y[r * width + c].toInt() and 0xFF).toLong(); counts[cell]++
                    c += 4
                }
                r += 4
            }
            return IntArray(sums.size) { if (counts[it] == 0) 0 else (sums[it] / counts[it]).toInt() }
        }

        fun difference(a: IntArray, b: IntArray): Double = a.indices.sumOf { abs(a[it] - b[it]) }.toDouble() / a.size
    }
}

/** Where a found thing is, as the listener's body knows it. */
data class Place(val side: Side, val near: Boolean)
enum class Side { LEFT, AHEAD, RIGHT }

object Find {
    /** The middle fifth either side of centre is "ahead": a hand reaching straight finds it. */
    const val AHEAD_HALF_WIDTH = 0.2

    /** A box covering this share of the frame or more is within reach. Interim, from a hand's
     *  length on the moto's main camera; set on the phone. */
    const val NEAR_AREA = 0.15

    /**
     * A box the model wrote, in its 0..1000 grid ("[x1, y1, x2, y2]", the form the prompt asks
     * for), as fractions of the frame. Null when there is no box, or it is not a box: a model
     * asked "is there a bottle?" that answers with a number list it invented must not send a
     * blind user's hand somewhere.
     */
    fun parseBox(text: String): DoubleArray? {
        val m = Regex("\\[\\s*(\\d{1,4})\\s*,\\s*(\\d{1,4})\\s*,\\s*(\\d{1,4})\\s*,\\s*(\\d{1,4})\\s*]").find(text) ?: return null
        val v = m.groupValues.drop(1).map { it.toInt() }
        if (v.any { it > 1000 } || v[2] <= v[0] || v[3] <= v[1]) return null
        return DoubleArray(4) { v[it] / 1000.0 }
    }

    fun place(box: DoubleArray): Place {
        val cx = (box[0] + box[2]) / 2
        val side = when {
            cx < 0.5 - AHEAD_HALF_WIDTH -> Side.LEFT
            cx > 0.5 + AHEAD_HALF_WIDTH -> Side.RIGHT
            else -> Side.AHEAD
        }
        return Place(side, (box[2] - box[0]) * (box[3] - box[1]) >= NEAR_AREA)
    }

    /** Whether the model said the thing is absent. Checked before any box: "No bottle [0,0,0,0]". */
    fun saysAbsent(text: String): Boolean =
        Regex("^\\s*(no|none|not (found|visible|present|here)|there is no|i (can't|cannot|do not|don't) see)\\b", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)

    /** The model's whole answer as a place, or null: absence is read first, so "No [0,0,500,500]"
     *  sends nobody anywhere, and words without a box are no direction either. */
    fun answer(text: String): Place? = if (saysAbsent(text)) null else parseBox(text)?.let(::place)

    /** Felt with the words: a long pulse when it is within reach, a short tick when it is there
     *  but further, so the hand knows whether to stretch or to walk. */
    fun pattern(p: Place): Pattern = Pattern(longArrayOf(if (p.near) NEAR_MS else FAR_MS), intArrayOf(255))
    const val NEAR_MS = 600L
    const val FAR_MS = 80L
}

/** What Find can be asked for until a spoken name can be heard (P6): the things a blind person
 *  most often searches a room for. [english] is what the model is asked; the voice names them
 *  by [id] in the phone's language. A person is found, never named. */
enum class Thing(val id: String, val english: String) {
    DOOR("door", "door"), CHAIR("chair", "chair"), TABLE("table", "table"), CUP("cup", "cup"),
    BOTTLE("bottle", "bottle"), BAG("bag", "bag"), PHONE("phone", "mobile phone"), PERSON("person", "person")
}

/** The ready question or thing, stepped by a swipe up; it wraps, so the list has no dead end. */
class Choice<T>(private val items: List<T>) {
    init { require(items.isNotEmpty()) { "nothing to choose" } }
    private var index = 0
    val current: T get() = items[index]
    fun next(): T { index = (index + 1) % items.size; return current }
}

/** Ask's one question in flight: chosen on the main thread, taken once by the next clear frame
 *  on the camera's thread, so a question is answered once, not again on every frame. */
class AskTurn {
    private var pending: Question? = null
    @Synchronized fun choose(q: Question) { pending = q }
    @Synchronized fun take(): Question? { val q = pending; pending = null; return q }
    /** Whether a question waits: with none, a dark frame is nobody's business, not "nothing found". */
    @Synchronized fun waiting(): Boolean = pending != null
    @Synchronized fun clear() { pending = null }
}

/**
 * Questions a listener can ask by choosing rather than speaking: the commonest questions blind
 * people asked of photos (VizWiz's questions are mostly these), for when no offline speech
 * recogniser is on the phone. Ids, so the screen and the voice name them in the phone's language.
 */
enum class Question(val id: String, val english: String) {
    WHAT_IS_THIS("what", "What is this?"),
    COLOUR("colour", "What colour is this?"),
    EXPIRY("expiry", "What is the expiry date?"),
    SCREEN("screen", "What does the screen say?"),
    LIGHT_ON("light", "Is a light on?"),
    DOSE("dose", "How do I take this medicine?")
}

/** What the model is asked. In the engine so the checks see the exact words. */
object Prompts {
    const val DESCRIBE = "Describe what is in front of the camera for a blind person, in at most two short " +
        "sentences. Name the main things and where they are. Do not guess anyone's identity, age or feelings. " +
        "If you cannot tell, say so."
    fun find(thing: String) = "Is there a $thing in this picture? If yes, answer only with its box as " +
        "[x1, y1, x2, y2] on a 0 to 1000 grid. If not, answer only: No."
    fun ask(question: String) = "A blind person holding this camera asks: $question Answer in one or two " +
        "short sentences. If the picture does not show the answer, say what they should move to show it."
}
