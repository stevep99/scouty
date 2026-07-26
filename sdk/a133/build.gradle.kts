plugins {
    alias(libs.plugins.android.library)
}

android {
    compileSdk = 34
    namespace = "io.github.stevep99.scouty.motors.sdk_a133"

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":sdk:common"))
    implementation(libs.kermit)
    implementation(libs.kotlinx.coroutines.android)
}