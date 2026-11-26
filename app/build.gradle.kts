import java.util.Properties

plugins {
    alias(libs.plugins.android.application) // o AGP 9 já traz o Kotlin embutido: sem kotlin-android
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
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

    // A keystore e as senhas ficam fora do git (keystore.properties na raiz). Sem o arquivo, o release sai sem assinatura.
    val assinatura = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { arquivo ->
        Properties().apply { arquivo.inputStream().use(::load) }
    }
    signingConfigs {
        if (assinatura != null) create("release") {
            storeFile = file(assinatura.getProperty("storeFile"))
            storePassword = assinatura.getProperty("storePassword")
            keyAlias = assinatura.getProperty("keyAlias")
            keyPassword = assinatura.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.palette)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
