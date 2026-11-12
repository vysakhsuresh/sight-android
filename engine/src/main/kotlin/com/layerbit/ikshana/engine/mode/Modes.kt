package com.layerbit.ikshana.engine.mode

/**
 * The modes, what each needs from the phone, and the one each falls to.
 *
 * The owner's second rule is that Ikshana never goes silent: every mode has a mode below it, and
 * the floor of the floor is [HELP], which needs nothing at all and speaks what to do next. The
 * chain is data, not branches scattered through the screens, so FallbackChecks can walk every
 * path through it on the JVM and prove each one ends in speech.
 *
 * The six on the row are the brief's section 4. [LIGHT] and [HELP] are not on the row: they are
 * where the app lands when the row's modes cannot run. LIGHT is pure arithmetic over the camera's
 * luma (LightLevel), so it works on any phone with a camera and no model; HELP needs no camera.
 */
enum class Mode(val id: String, val onRow: Boolean) {
    READ("read", true),
    MONEY("money", true),
    DESCRIBE("describe", true),
    FIND("find", true),
    TRANSLATE("translate", true),
    ASK("ask", true),
    LIGHT("light", false),
    HELP("help", false);

    companion object {
        val row: List<Mode> = entries.filter { it.onRow }
        fun byId(id: String): Mode? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Model ids as docs/MODELS.md names them. A tier is a set of models that must all load for the
 * mode to run at that tier; tier 0 is the best the pack carries, later tiers are the lighter
 * twins chosen when the phone cannot carry the first (measured at first run, never assumed).
 */
object Models {
    const val OCR_DETECTOR = "ocr.detector"
    const val OCR_RECOGNISER = "ocr.recogniser"
    const val OCR_DETECTOR_LITE = "ocr.detector.lite"
    const val OCR_RECOGNISER_LITE = "ocr.recogniser.lite"
    const val CURRENCY = "currency"
    const val VLM = "vlm"
    const val VLM_LITE = "vlm.lite"
    const val TRANSLATE = "translate"
    const val TRANSLATE_LITE = "translate.lite"

    val all: List<String> = listOf(
        OCR_DETECTOR, OCR_RECOGNISER, OCR_DETECTOR_LITE, OCR_RECOGNISER_LITE,
        CURRENCY, VLM, VLM_LITE, TRANSLATE, TRANSLATE_LITE
    )
}

/** What a mode needs: the camera (or a picked picture), and one of its tiers. */
class Needs(val camera: Boolean, val tiers: List<Set<String>>, val below: Mode?)

object ModeTable {
    private val ocr = setOf(Models.OCR_DETECTOR, Models.OCR_RECOGNISER)
    private val ocrLite = setOf(Models.OCR_DETECTOR_LITE, Models.OCR_RECOGNISER_LITE)

    /**
     * Why each mode falls where it does:
     * - Money falls to Read because every Indian note carries its value in numerals; reading them
     *   is worse than recognising the note and far better than nothing.
     * - Describe and Translate fall to Read: text is the one thing the smaller models still find.
     * - Find and Ask fall to Describe, which uses the same vision model's lighter twin first.
     * - Read falls to Light, which needs no model; Light falls to Help, which needs no camera.
     */
    val needs: Map<Mode, Needs> = mapOf(
        Mode.READ to Needs(true, listOf(ocr, ocrLite), Mode.LIGHT),
        Mode.MONEY to Needs(true, listOf(setOf(Models.CURRENCY)), Mode.READ),
        Mode.DESCRIBE to Needs(true, listOf(setOf(Models.VLM), setOf(Models.VLM_LITE)), Mode.READ),
        Mode.FIND to Needs(true, listOf(setOf(Models.VLM), setOf(Models.VLM_LITE)), Mode.DESCRIBE),
        Mode.TRANSLATE to Needs(
            true,
            listOf(ocr + Models.TRANSLATE, ocrLite + Models.TRANSLATE, ocrLite + Models.TRANSLATE_LITE),
            Mode.READ
        ),
        Mode.ASK to Needs(true, listOf(setOf(Models.VLM), setOf(Models.VLM_LITE)), Mode.DESCRIBE),
        Mode.LIGHT to Needs(true, listOf(emptySet()), Mode.HELP),
        Mode.HELP to Needs(false, listOf(emptySet()), null)
    )

    fun of(mode: Mode): Needs = needs.getValue(mode)
}
