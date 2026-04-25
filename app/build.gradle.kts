import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Release signing stays outside the repo and is read from Gradle properties or env vars.
val releaseStoreFile = providers.gradleProperty("GLYPH_RELEASE_STORE_FILE")
    .orElse(providers.environmentVariable("GLYPH_RELEASE_STORE_FILE"))
val releaseStorePassword = providers.gradleProperty("GLYPH_RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("GLYPH_RELEASE_STORE_PASSWORD"))
val releaseKeyAlias = providers.gradleProperty("GLYPH_RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("GLYPH_RELEASE_KEY_ALIAS"))
val releaseKeyPassword = providers.gradleProperty("GLYPH_RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("GLYPH_RELEASE_KEY_PASSWORD"))

val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it.isPresent }

android {
    namespace = "com.sajenko.glyphtoys"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sajenko.glyphtoys"
        minSdk = 34
        targetSdk = 36
        versionCode = 6
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
}

gradle.taskGraph.whenReady {
    val requestedReleaseTask = allTasks.any { task ->
        task.project == project && task.name.contains("Release", ignoreCase = true)
    }
    if (requestedReleaseTask && !hasReleaseSigning) {
        throw GradleException(
            "Release signing is not configured. Set GLYPH_RELEASE_STORE_FILE, " +
                "GLYPH_RELEASE_STORE_PASSWORD, GLYPH_RELEASE_KEY_ALIAS, and " +
                "GLYPH_RELEASE_KEY_PASSWORD in ~/.gradle/gradle.properties or env vars."
        )
    }
}
