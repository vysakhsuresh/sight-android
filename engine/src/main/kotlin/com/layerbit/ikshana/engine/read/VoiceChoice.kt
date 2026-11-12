package com.layerbit.ikshana.engine.read

/**
 * Which of the phone's installed voices speaks a language, chosen rather than left to the
 * engine's default. The owner asked for more natural speech (12 Nov); the phone's engine usually
 * holds several voices per language and its default is often not its best.
 *
 * Rules, in order: the language must match; the voice must work offline (the app has no network,
 * and a network voice would fall silent mid-sentence); it must be installed; then the country the
 * tag asks for (Indian English over American), then higher quality, then lower latency.
 */
data class VoiceInfo(
    val name: String,
    val language: String,      // ISO 639, as Locale.getLanguage(): "ml", "en"
    val country: String,       // "IN", "US", or ""
    val quality: Int,          // android.speech.tts.Voice.QUALITY_*: 100 very low .. 500 very high
    val latency: Int,          // LATENCY_*: 100 very low .. 500 very high
    val needsNetwork: Boolean,
    val installed: Boolean
)

object VoiceChoice {
    /** The best voice for [tag] ("ml-IN"), or null when none is offline and installed. */
    fun pick(voices: List<VoiceInfo>, tag: String): VoiceInfo? {
        val lang = tag.substringBefore('-').lowercase()
        val country = tag.substringAfter('-', "").uppercase()
        return voices
            .filter { it.language.lowercase() == lang && !it.needsNetwork && it.installed }
            .sortedWith(
                compareByDescending<VoiceInfo> { country.isNotEmpty() && it.country.uppercase() == country }
                    .thenByDescending { it.quality }
                    .thenBy { it.latency }
                    .thenBy { it.name }
            )
            .firstOrNull()
    }

    /**
     * A little under the engine's default rate. Listeners who use speech all day often run it
     * faster, so this is only the first-run default; a Settings control comes with the low-vision
     * pass (P7). 0.95 reads as unhurried without dragging.
     */
    const val RATE = 0.95f
}
