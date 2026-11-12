pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Tesseract for Android (Apache-2.0) is published only on JitPack. Filtered to its one
        // group, so no other dependency can ever resolve from a repository that builds on demand.
        exclusiveContent {
            forRepository { maven("https://jitpack.io") }
            filter { includeGroup("cz.adaptech.tesseract4android") }
        }
    }
}

// Two modules of code, as Stave has them (../stave-android/settings.gradle.kts).
//
// :engine is pure Kotlin - it imports nothing from android.* - and holds every rule and every
// piece of arithmetic that can be separated from the framework: the mode state machine and its
// fallbacks, the frame gate, the field rules, reading order, the earcon tones, the gesture
// classifier. It compiles with the standalone Kotlin compiler and its suites run in seconds on
// any machine with a JDK (tools/jvm/run.sh), which is what lets a rule be *executed* rather than
// reasoned about. For an app whose first law is "it never goes silent", the proof that every
// failure ends in a spoken sentence has to be something a machine runs, not a review.
//
// :app is the Android half: Compose, CameraX, the voice, the doors, LiteRT. It depends on
// :engine and never the other way round.
rootProject.name = "Ikshana"
include(":engine")
include(":app")
