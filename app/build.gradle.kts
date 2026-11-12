import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Shape and versions from ../stave-android/app/build.gradle.kts (12 Nov 2026). Not yet carried:
// the play/full flavours and the asset pack (they arrive with the first model, P2), the
// baseline profile, and the native build (app/src/main/cpp arrives with the first native code).
// Release signing credentials live only in a local, git-ignored keystore.properties (see
// keystore.properties.example; release.yml writes one from the secrets) - never in the repo.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasKeystoreProperties = keystorePropertiesFile.exists()
val keystoreProperties = Properties().apply {
    if (hasKeystoreProperties) load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.layerbit.ikshana"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.layerbit.ikshana"
        // The family floor (Still, Sheaf, Stave). CameraX runs from 21; nothing here needs more.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    signingConfigs {
        if (hasKeystoreProperties) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasKeystoreProperties) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
        // Memory-mapped by the model runners from P2, which a compressed asset cannot be.
        noCompress += listOf("tflite", "bin")
    }

    // Every language rides in every install, as Stave: the app chooses its own language, and a
    // Play split would hold only the phone's.
    bundle {
        language { enableSplit = false }
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

composeCompiler {
    // Compose compares an unstable parameter by identity; the file says which types may be
    // compared by equals and why (Stave's lesson, carried).
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

dependencies {
    implementation(project(":engine"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.annotation)
    // Declared, not imported: the manifest names its InitializationProvider to strip Emoji2's
    // downloader from it, and lint needs the class on the path to accept that (MissingClass).
    implementation(libs.androidx.startup)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Everything the source imports directly, declared (tools/check_dependencies.py holds it).
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.text)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    // The camera (Apache-2.0).
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
}

// DEPENDENCIES DELIBERATELY NOT HERE
//
// No networking library of any kind, and the manifest removes the permission so none could be
// used by accident. No analytics and no crash reporter: a crash is caught, spoken and recovered
// on the phone (IkshanaApplication), and nothing about it leaves. LiteRT is in the version
// catalogue and joins here with the first model (P2), not before: a runtime with nothing to run
// is size and surface for no gain.
