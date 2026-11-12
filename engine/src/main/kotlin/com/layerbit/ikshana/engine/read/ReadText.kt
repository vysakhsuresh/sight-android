package com.layerbit.ikshana.engine.read

/**
 * The rules between the recogniser and the voice, for live Read (brief, Pillar A): hold the phone
 * and sweep; a line is spoken as soon as it is sure, and never twice.
 *
 * Pure, so ReadTextChecks executes them: which lines are text and which are the recogniser
 * hallucinating on a wood grain; when two readings of the same sign are the same line; when to
 * say "nothing found" without saying it every second.
 */
data class OcrLine(val text: String, val confidence: Int)

object ReadText {
    /** Below this mean word confidence (Tesseract's 0..100) a line is more likely noise than text. */
    const val MIN_CONFIDENCE = 60

    /**
     * Keep the lines worth saying, in reading order: whitespace collapsed, and a line dropped when
     * the recogniser was unsure of it, or when fewer than half of its code points are letters or
     * digits (texture read as "|/_ =~", the commonest junk on a camera frame), or when it has
     * fewer than two letters or digits at all. Code points, not chars: a Malayalam letter with
     * its vowel sign is two code points and must count as text (Squish's rule).
     */
    fun clean(lines: List<OcrLine>): List<String> = lines.mapNotNull { l ->
        val t = l.text.replace(Regex("\\s+"), " ").trim()
        if (l.confidence < MIN_CONFIDENCE || t.isEmpty()) return@mapNotNull null
        var meaningful = 0
        var visible = 0
        var i = 0
        while (i < t.length) {
            val cp = t.codePointAt(i)
            if (!Character.isWhitespace(cp)) {
                visible++
                val type = Character.getType(cp)
                if (Character.isLetterOrDigit(cp) || type == Character.NON_SPACING_MARK.toInt() ||
                    type == Character.COMBINING_SPACING_MARK.toInt()) meaningful++
            }
            i += Character.charCount(cp)
        }
        if (meaningful < 2 || meaningful * 2 < visible) null else t
    }

    /**
     * The key two readings of one line share: letters and digits only, case folded. A sign read
     * on one frame as "PLATFORM 4" and on the next as "Platform 4." is the same line.
     */
    fun key(line: String): String = buildString {
        var i = 0
        while (i < line.length) {
            val cp = line.codePointAt(i)
            val type = Character.getType(cp)
            if (Character.isLetterOrDigit(cp) || type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.COMBINING_SPACING_MARK.toInt()) appendCodePoint(Character.toLowerCase(cp))
            i += Character.charCount(cp)
        }
    }

    /**
     * Rotate a luma plane by the camera's rotation (0, 90, 180, 270 clockwise) into a tight
     * buffer of width x height after rotation, dropping the row padding. The sensor is mounted
     * landscape, so on a phone held upright every frame arrives turned 90 degrees, and a
     * recogniser given it sideways reads nothing.
     */
    fun rotate(y: ByteArray, width: Int, height: Int, rowStride: Int, degrees: Int): Rotated {
        val d = ((degrees % 360) + 360) % 360
        require(d % 90 == 0) { "rotation $degrees" }
        val (w, h) = if (d == 90 || d == 270) height to width else width to height
        val out = ByteArray(w * h)
        for (r in 0 until height) for (c in 0 until width) {
            val v = y[r * rowStride + c]
            val (x, yy) = when (d) {
                0 -> c to r
                90 -> (height - 1 - r) to c
                180 -> (width - 1 - c) to (height - 1 - r)
                else -> r to (width - 1 - c)
            }
            out[yy * w + x] = v
        }
        return Rotated(out, w, h)
    }

    class Rotated(val bytes: ByteArray, val width: Int, val height: Int)
}

/**
 * What live Read has already said this session, so a line is never spoken twice while the phone
 * sweeps back over it. Bounded: the oldest lines are forgotten first, so a sign seen again after
 * a long walk is read again.
 */
class ReadMemory(private val capacity: Int = 200) {
    private val said = LinkedHashSet<String>()

    /** The lines of [lines] not said before, in order; they are remembered as said. */
    fun fresh(lines: List<String>): List<String> {
        val out = ArrayList<String>()
        for (l in lines) {
            val k = ReadText.key(l)
            if (k.isEmpty() || k in said) continue
            said += k
            out += l
            while (said.size > capacity) said.remove(said.first())
        }
        return out
    }

    fun clear() = said.clear()
}

/**
 * When to say "nothing found". After every empty frame would be a voice that never stops; never
 * would be silence. So: once the camera has found nothing for [AFTER_MS], and then no more often
 * than every [EVERY_MS] while it stays empty. Any text found resets it.
 */
class NothingPacer {
    private var emptySince = -1L
    private var lastSaid = -1L

    fun onRead(nowMs: Long, foundText: Boolean): Boolean {
        if (foundText) { emptySince = -1L; lastSaid = -1L; return false }
        if (emptySince < 0) emptySince = nowMs
        if (nowMs - emptySince < AFTER_MS) return false
        if (lastSaid >= 0 && nowMs - lastSaid < EVERY_MS) return false
        lastSaid = nowMs
        return true
    }

    companion object {
        const val AFTER_MS = 4_000L
        const val EVERY_MS = 10_000L
    }
}

/**
 * Which voice a line is spoken in: the script most of its letters are written in, by Unicode
 * block. A Malayalam line sent to an English voice is spelled out letter by letter or skipped,
 * which is silence by another name. Returns a BCP-47 tag; English (India) when no Indic script
 * has the majority.
 */
object Scripts {
    private val blocks = listOf(
        0x0D00..0x0D7F to "ml-IN",
        0x0900..0x097F to "hi-IN",
        0x0B80..0x0BFF to "ta-IN",
        0x0C80..0x0CFF to "kn-IN",
        0x0C00..0x0C7F to "te-IN"
    )

    fun localeOf(line: String): String {
        val counts = HashMap<String, Int>()
        var latin = 0
        var i = 0
        while (i < line.length) {
            val cp = line.codePointAt(i)
            val tag = blocks.firstOrNull { cp in it.first }?.second
            if (tag != null) counts[tag] = (counts[tag] ?: 0) + 1
            else if (Character.isLetter(cp)) latin++
            i += Character.charCount(cp)
        }
        val best = counts.maxByOrNull { it.value } ?: return "en-IN"
        return if (best.value >= latin) best.key else "en-IN"
    }
}
