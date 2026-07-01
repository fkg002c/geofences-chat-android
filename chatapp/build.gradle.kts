plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.dagger.hilt)
}

android {
    namespace = "com.ruinkogr.chatapp"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.ruinkogr.chatapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

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
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    //Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    ksp(libs.kotlin.metadata.jvm)
    //Room
    implementation(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    // Retrofit + OkHttp
    implementation(libs.okhttp.android)
    implementation(libs.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    // Ktor
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)
    //DataStore
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    //Encrypted Prefs
    implementation(libs.androidx.security.crypto)
    // Material Icons
    implementation(libs.androidx.compose.material.icons.core.android)
    // Appcompat
    implementation(libs.androidx.appcompat)
    // Navigation Compose
    implementation(libs.androidx.navigation.compose)
}

configurations.configureEach {
    exclude(group = "com.intellij", module = "annotations")
}
