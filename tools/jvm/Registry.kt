/**
 * Every suite, in the order they run. RunnerChecks holds this list honest against the files.
 * (Shape from ../stave-android/tools/jvm/Registry.kt.)
 */
object Registry {
    val all: List<Suite> = listOf(
        FallbackChecks,
        SpeechTableChecks,
        LightChecks,
        GestureChecks,
        EarconChecks,
        ReadTextChecks,
        MoneyChecks,
        VoiceChoiceChecks,
        RunnerChecks
    )
}
