import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}
kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }
android {
    namespace = "pe.edu.upeu.solshiran"
    compileSdk = 36
    defaultConfig {
        applicationId = "pe.edu.upeu.solshiran"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
configurations.configureEach { resolutionStrategy.force("com.squareup.okhttp3:okhttp:4.12.0") }
