//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.kotlin.compose)
//}
//
//android {
//    namespace = "com.app001.virtualcamera"
//    compileSdk = 36
//
//    defaultConfig {
//        applicationId = "com.app001.virtualcamera"
//        minSdk = 29
//        targetSdk = 36
//        versionCode = 1
//        versionName = "1.0"
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
//    kotlinOptions {
//        jvmTarget = "11"
//    }
//    buildFeatures {
//        compose = true
//    }
//}
//
//dependencies {
//
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.app001.virtualcamera"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.app001.virtualcamera"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }

        // Add NDK configuration
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // Add packaging options to exclude unnecessary files
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.google.android.material)

    // CameraX dependencies
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.camera:camera-extensions:1.3.1")

    // Root access library
//    implementation(libs.libsu.core)
//    implementation(libs.libsu.io)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.3")
    
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}