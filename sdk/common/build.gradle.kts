plugins {
    alias(libs.plugins.android.library)
}

android {
    compileSdk = 34
    namespace = "io.github.stevep99.scouty.motors.sdk"

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
