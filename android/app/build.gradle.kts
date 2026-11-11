import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tripfair.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tripfair.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Backend address, set as tripfair.apiBaseUrl in local.properties (not committed).
        // The default is a backend on this computer, reached after `adb reverse tcp:3000 tcp:3000`
        // (emulator or USB phone). Android 17 blocks apps from LAN addresses such as 10.0.2.2
        // unless they hold the local network permission; loopback is not affected.
        val localProperties = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
        }
        val apiBaseUrl = localProperties.getProperty("tripfair.apiBaseUrl", "http://localhost:3000/api/v1/")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        // Google Maps SDK key, set as MAPS_API_KEY in local.properties (not committed). Without
        // it the app still runs: maps show their markers on blank tiles.
        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY", "")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("boolean", "HAS_MAPS_KEY", mapsApiKey.isNotEmpty().toString())
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // The app forces Vietnamese (MainActivity), so an app bundle must keep the Vietnamese
    // texts of its libraries (e.g. the date picker) on phones set to another language.
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.maps.compose)
    implementation(libs.play.services.location)
    // Request logs only in debug builds, so tokens never reach a release build's log.
    debugImplementation(libs.okhttp.logging)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
