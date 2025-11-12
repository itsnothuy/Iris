plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
}

android {
    namespace = "com.nervesparks.iris.core.multimodal"
    compileSdk = 35
    
    // NDK version for native builds (when native integration is enabled)
    // ndkVersion = "27.0.12077973"

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        
        // Native build configuration (commented out until submodules are added)
        // externalNativeBuild {
        //     cmake {
        //         cppFlags += listOf("-std=c++17", "-fexceptions", "-frtti")
        //         arguments += listOf(
        //             "-DANDROID_STL=c++_shared",
        //             "-DGGML_USE_CPU=ON",
        //             "-DGGML_USE_LLAMAFILE=OFF"
        //         )
        //     }
        // }
        
        // Target architectures for native libraries
        // ndk {
        //     abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        // }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
    
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
    
    // CMake configuration for native builds (commented out until ready)
    // externalNativeBuild {
    //     cmake {
    //         path = file("src/main/cpp/CMakeLists.txt")
    //         version = "3.22.1"
    //     }
    // }
}

dependencies {
    // Project modules
    implementation(project(":common"))
    implementation(project(":core-hw"))
    implementation(project(":core-models"))

    // Core Android
    implementation(libs.androidx.core.ktx)
    
    // Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    
    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// KAPT configuration for stable Hilt compilation
kapt {
    correctErrorTypes = true
    useBuildCache = true
    
    arguments {
        arg("dagger.hilt.shareTestComponents", "true")
        arg("dagger.hilt.disableModulesHaveInstallInCheck", "true")
    }
}
