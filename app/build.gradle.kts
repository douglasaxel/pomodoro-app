plugins {
    alias(libs.plugins.android.application) // o AGP 9 já traz o Kotlin embutido: sem kotlin-android
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.exemplo.pomodoro"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.exemplo.pomodoro"
        minSdk = 33 // ADR-0001
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
