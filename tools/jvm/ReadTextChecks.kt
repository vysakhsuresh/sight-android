import com.layerbit.ikshana.engine.read.NothingPacer
import com.layerbit.ikshana.engine.read.OcrLine
import com.layerbit.ikshana.engine.read.ReadMemory
import com.layerbit.ikshana.engine.read.ReadText
import com.layerbit.ikshana.engine.read.Scripts

/**
 * Live Read's rules, with real strings in the five scripts: junk dropped and text kept (a
 * Malayalam word with its vowel signs must survive), the same sign read twice spoken once, the
 * camera's sideways plane turned upright, "nothing found" paced.
 */
object ReadTextChecks : Suite {
    override val name = "read"

    override fun run() {
        section("clean: text kept, junk dropped")
        val kept = ReadText.clean(listOf(
            OcrLine("  PLATFORM   4 ", 91),
            OcrLine("|/_ =~ |", 88),
            OcrLine("EXP 03/27", 59),
            OcrLine("മലയാളം", 80),
            OcrLine("हिन्दी", 77),
            OcrLine("தமிழ்", 75),
            OcrLine("ಕನ್ನಡ", 72),
            OcrLine("తెలుగు", 70),
            OcrLine("a", 95),
            OcrLine("MRP Rs. 45.00", 84)
        ))
        check(kept == listOf("PLATFORM 4", "മലയാളം", "हिन्दी", "தமிழ்", "ಕನ್ನಡ", "తెలుగు", "MRP Rs. 45.00")) { "kept $kept" }

        section("memory: the same sign twice is said once")
        val m = ReadMemory(capacity = 3)
        check(m.fresh(listOf("PLATFORM 4", "Exit")) == listOf("PLATFORM 4", "Exit")) { "first sight" }
        check(m.fresh(listOf("Platform 4.", "EXIT", "Ernakulam")) == listOf("Ernakulam")) { "the sweep back" }
        check(m.fresh(listOf("മലയാളം")) == listOf("മലയാളം")) { "a new Malayalam line" }
        check(m.fresh(listOf("mala yalam")).size == 1) { "a different line is not the same key" }
        // Capacity 3: "PLATFORM 4" was the oldest and is forgotten, so it is read again.
        check(m.fresh(listOf("PLATFORM 4")) == listOf("PLATFORM 4")) { "an old sign seen again is read again" }
        check(ReadText.key("മലയാളം") == "മലയാളം") { "vowel signs are part of the key: ${ReadText.key("മലയാളം")}" }
        // The loop the owner heard (13 Nov): one strip read a little differently on every frame.
        val strip = ReadMemory()
        check(strip.fresh(listOf("Paracetamol Tablets IP 500 mg")).size == 1) { "the strip, first sight" }
        check(strip.fresh(listOf("Paracetamal Tablets IP 500 mg", "PARACETAMOL TABLETS I.P. 500mg","Paracetarnol Tablets IP 500 mg", "aracetamol Tablets IP 500 mg")).isEmpty()) { "the same strip misread is not new" }
        check(strip.fresh(listOf("Paracetamol Tablets IP 650 mg")).size == 1) { "a different dose is new: digits must agree" }
        check(strip.fresh(listOf("Paracetamol")).isEmpty()) { "part of a line already said is not new" }
        check(strip.fresh(listOf("Ibuprofen Tablets IP 500 mg")).size == 1) { "another medicine is new" }
        check(!ReadMemory.same("platform4", "platform5")) { "platform 4 is not platform 5" }
        check(!ReadMemory.same("exit", "exif") && !ReadMemory.same("hotel", "hostel")) { "short words match exactly" }
        check(!ReadMemory.same("platform4", "platform45")) { "platform 4 is not part of platform 45" }

        section("rotate: the sideways sensor plane turned upright")
        // 3 x 2 plane with a padded stride of 4: rows "abc." and "def."
        val p = byteArrayOf(1, 2, 3, 99, 4, 5, 6, 99)
        fun rot(d: Int) = ReadText.rotate(p, 3, 2, 4, d)
        check(rot(0).bytes.toList() == listOf<Byte>(1, 2, 3, 4, 5, 6) && rot(0).width == 3) { "0: ${rot(0).bytes.toList()}" }
        // 90 clockwise: the bottom row becomes the left column.
        check(rot(90).bytes.toList() == listOf<Byte>(4, 1, 5, 2, 6, 3) && rot(90).width == 2 && rot(90).height == 3) { "90: ${rot(90).bytes.toList()}" }
        check(rot(180).bytes.toList() == listOf<Byte>(6, 5, 4, 3, 2, 1)) { "180: ${rot(180).bytes.toList()}" }
        check(rot(270).bytes.toList() == listOf<Byte>(3, 6, 2, 5, 1, 4)) { "270: ${rot(270).bytes.toList()}" }
        check(rot(-90).bytes.toList() == rot(270).bytes.toList()) { "-90 is 270" }

        section("each line in the voice of its script")
        val want = mapOf("എറണാകുളം" to "ml-IN", "प्लेटफॉर्म 4" to "hi-IN", "சென்னை" to "ta-IN", "ಬೆಂಗಳೂರು" to "kn-IN",
            "హైదరాబాద్" to "te-IN", "PLATFORM 4" to "en-IN", "MRP ₹45 മരുന്ന്" to "ml-IN", "Exit / നിർഗമനം" to "ml-IN", "" to "en-IN")
        for ((line, tag) in want) check(Scripts.localeOf(line) == tag) { "'$line' -> ${Scripts.localeOf(line)}, want $tag" }

        section("nothing found: said after 4 s of nothing, then every 10 s, reset by text")
        val pacer = NothingPacer()
        val said = (0L..30_000L step 500L).filter { pacer.onRead(it, foundText = false) }
        check(said == listOf(4_000L, 14_000L, 24_000L)) { "said at $said" }
        check(!pacer.onRead(30_500, foundText = true)) { "text is never 'nothing'" }
        check(!pacer.onRead(31_000, false) && pacer.onRead(35_000, false)) { "text resets the wait" }
        val mid = NothingPacer()
        mid.onRead(0, false); mid.onRead(3_000, false)
        mid.clear()
        check(!mid.onRead(4_000, false) && !mid.onRead(7_000, false) && mid.onRead(8_000, false)) { "a new mode waits its own time" }
    }
}

