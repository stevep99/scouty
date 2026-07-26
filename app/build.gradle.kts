plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.stevep99.scouty"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.stevep99.scouty"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "robot"
    productFlavors {
        // Generic/tablet build: no physical robot, no movement SDK.
        create("generic") {
            dimension = "robot"
            buildConfigField("String", "ROBOT_SDK", "\"io.github.stevep99.scouty.motors.sdk.ScoutyGenericRobotSdkConfig\"")
            isDefault = true
        }
        // A133 robot build: includes the A133 movement SDK and native libraries.
        create("a133") {
            dimension = "robot"
            buildConfigField("String", "ROBOT_SDK", "\"io.github.stevep99.scouty.motors.sdk.ScoutyA133RobotSdkConfig\"")
            ndk {
                abiFilters += setOf("armeabi-v7a")
            }
        }
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
        buildConfig = true
        compose = true
    }
    packaging {
        jniLibs {
            pickFirsts.add("lib/*/libc++_shared.so")
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kermit)
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":sdk:common"))
    add("a133Implementation", project(":sdk:a133"))
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}