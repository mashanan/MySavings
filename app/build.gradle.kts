plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    // Reads app/google-services.json (not in git) into resources such as default_web_client_id.
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.hanan.mysavings"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hanan.mysavings"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                // Whole-app R8. The template limited it with packageScope = androidx/kotlin/kotlinx,
                // which changed class visibility inside Kotlin's stdlib while our classes outside
                // that scope still extended them: the release build crashed on launch with an
                // IllegalAccessError. Libraries ship their own keep rules, so full scope is safe.
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    bundle {
        language {
            // Keep every translation in the base APK. Play would otherwise only ship the phone's
            // system language, and picking another language in the app would find no strings.
            enableSplit = false
        }
    }
    androidResources {
        // Builds the list of supported languages from the values-xx folders, so Android 13+
        // shows them under Settings > Apps > MySavings > Language. See res/resources.properties.
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
    }

    // ML Kit's OCR engine is a ~7-11 MB native library per CPU type, and a phone only needs
    // its own. Release builds therefore produce one APK per ABI instead of one with all four.
    // (Play Store does the same automatically from an App Bundle: ./gradlew bundleRelease.)
    // Debug builds and bundles are not split (bundles split per device on Play already).
    splits {
        abi {
            isEnable = gradle.startParameter.taskNames.any {
                it.contains("assemble", ignoreCase = true) && it.contains("Release", ignoreCase = true)
            }
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
}

room {
    // Exported schemas are committed so future migrations can be reviewed and tested.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    // Per-app language (AppCompatDelegate.setApplicationLocales), back-ported to Android 10-12.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    // Bundled model: ships inside the APK so OCR works offline from the first launch.
    implementation(libs.mlkit.text.recognition)
    implementation(libs.kotlinx.coroutines.play.services)
    // Sign-in: Firebase Auth, with Credential Manager showing the Google account picker.
    // Sync: Firestore as the cloud copy; Room stays the source of truth.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}