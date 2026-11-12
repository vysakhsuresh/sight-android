/** The voice chosen for each language: offline and installed first, then country, quality, latency. */
object VoiceChoiceChecks : Suite {
    override val name = "voicechoice"

    override fun run() {
        val v = listOf(
            com.layerbit.ikshana.engine.read.VoiceInfo("en-us-x-net", "en", "US", 500, 300, needsNetwork = true, installed = true),
            com.layerbit.ikshana.engine.read.VoiceInfo("en-us-x-local", "en", "US", 400, 200, false, true),
            com.layerbit.ikshana.engine.read.VoiceInfo("en-in-x-low", "en", "IN", 300, 200, false, true),
            com.layerbit.ikshana.engine.read.VoiceInfo("en-in-x-high", "en", "IN", 400, 200, false, true),
            com.layerbit.ikshana.engine.read.VoiceInfo("ml-in-x-missing", "ml", "IN", 400, 200, false, installed = false),
            com.layerbit.ikshana.engine.read.VoiceInfo("ml-in-x-net", "ml", "IN", 400, 200, true, true)
        )
        fun pick(tag: String) = com.layerbit.ikshana.engine.read.VoiceChoice.pick(v, tag)?.name
        check(pick("en-IN") == "en-in-x-high") { "Indian English, best quality: ${pick("en-IN")}" }
        check(pick("en-US") == "en-us-x-local") { "never the network voice: ${pick("en-US")}" }
        // No country asked: quality first (both 400 here), then latency, then the name.
        check(pick("en") == "en-in-x-high") { "no country asked: the best offline one: ${pick("en")}" }
        check(pick("ml-IN") == null) { "Malayalam only online or not installed: none, so the caller falls back: ${pick("ml-IN")}" }
    }
}
