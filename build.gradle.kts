// Versions as Stave has them (../stave-android/build.gradle.kts, 12 Nov 2026): the family moves
// its toolchain together, and a version chosen here alone is one nobody else has built with.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}
