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

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
