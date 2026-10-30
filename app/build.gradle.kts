plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.edro08.vaultk"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.edro08.vaultk"
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += setOf("META-INF/LICENSE.md", "META-INF/NOTICE.md", "META-INF/DEPENDENCIES")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.keepass.kdbx)
    implementation(libs.argon2kt)
    implementation(libs.stax.api)
    implementation(libs.google.code.scanner)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)
    implementation(libs.msal)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Connected tests may uninstall the target app and erase its private data.
// Run them only on an emulator/disposable device after an explicit opt-in.
tasks.matching { it.name.startsWith("connected") && it.name.endsWith("AndroidTest") }.configureEach {
    doFirst {
        check(providers.gradleProperty("vaultk.allowDestructiveConnectedTests").orNull == "true") {
            "Connected tests can uninstall Vaultk and erase private copies. Use a disposable " +
                "emulator/device and -Pvaultk.allowDestructiveConnectedTests=true. " +
                "Never run them on the phone containing the user's copies."
        }
    }
}
