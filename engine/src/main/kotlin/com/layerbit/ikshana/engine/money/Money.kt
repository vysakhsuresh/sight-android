package com.layerbit.ikshana.engine.money

import com.layerbit.ikshana.engine.feedback.Pattern

/**
 * Money, interim: the note's value read from its printed numerals by the same offline reader as
 * Read, until our own currency model (P3, item 18) is trained on photos of every note. Every
 * Indian note prints its value several times in numerals; nothing else on a note is a bare 10,
 * 20, 50, 100, 200, 500 or 2000 (the serial number is letters and six digits run together).
 *
 * Wrong money is the worst answer this app can give, so the rule is strict: a value is spoken
 * only when [AGREE] of the last [WINDOW] frames read the same one, and a frame that reads two
 * different values counts for neither. Genuine or not is never claimed (brief, Pillar B).
 */
object Money {
    val NOTES = listOf(10, 20, 50, 100, 200, 500, 2000)
    const val WINDOW = 3
    const val AGREE = 2

    private val TOKEN = Regex("(?<![0-9A-Za-z])(10|20|50|100|200|500|2000)(?![0-9])")

    /**
     * The one value a frame's text shows, or null if it shows none or more than one. "₹500",
     * "500", "Rs.500" and "500/-" all count; "1500", "5000", "AB500" and "500123" do not.
     */
    fun valueOf(lines: List<String>): Int? {
        val found = lines.flatMap { l -> TOKEN.findAll(l.replace(" ", "")).map { it.value.toInt() }.toList() }.toSet()
        return found.singleOrNull()
    }

    /** "Five hundred rupees": the certain part first, short, nothing else (brief, section 4). */
    fun words(value: Int): String = when (value) {
        10 -> "Ten rupees."
        20 -> "Twenty rupees."
        50 -> "Fifty rupees."
        100 -> "One hundred rupees."
        200 -> "Two hundred rupees."
        500 -> "Five hundred rupees."
        2000 -> "Two thousand rupees."
        else -> throw IllegalArgumentException("not a note: $value")
    }

    /**
     * The value felt, for the deaf-blind and for a crowded bus: one long pulse per hundred
     * (MANI's habit, brief Pillar B) for 100 to 500; two very long pulses for 2000; short ticks
     * for the notes under a hundred, one per ten up to 20 and five for 50. Every pattern differs
     * from every other in count or length, which MoneyChecks holds.
     */
    fun pattern(value: Int): Pattern {
        val (n, on) = when (value) {
            10 -> 1 to 60L
            20 -> 2 to 60L
            50 -> 5 to 60L
            100, 200, 500 -> value / 100 to 220L
            2000 -> 2 to 700L
            else -> throw IllegalArgumentException("not a note: $value")
        }
        val t = ArrayList<Long>(); val a = ArrayList<Int>()
        repeat(n) { i -> t += on; a += 255; if (i < n - 1) { t += 180L; a += 0 } }
        return Pattern(t.toLongArray(), a.toIntArray())
    }
}

/** The last frames' readings, and the value they agree on. */
class MoneyVote {
    private val recent = ArrayDeque<Int?>()
    private var spoken: Int? = null

    /** Feed one frame's value (null for none); returns a value to speak now, or null. */
    fun onFrame(value: Int?): Int? {
        recent.addLast(value)
        while (recent.size > Money.WINDOW) recent.removeFirst()
        val agreed = recent.filterNotNull().groupingBy { it }.eachCount().entries
            .firstOrNull { it.value >= Money.AGREE }?.key
        // The same note held still is said once; a different note, or the same one again after
        // the camera has looked away for a full window, is said again.
        if (recent.size == Money.WINDOW && recent.all { it == null }) spoken = null
        if (agreed == null || agreed == spoken) return null
        spoken = agreed
        return agreed
    }

    fun clear() { recent.clear(); spoken = null }
}
