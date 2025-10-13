//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.kotlin.compose)
//}
//
//plugins {
//    id("com.android.application")
//    id("org.jetbrains.kotlin.android")
//}
//
//android {
//    namespace = "com.example.android"
//    compileSdk = 35
//
//    ndkVersion = "26.1.10909125"
//
//    defaultConfig {
//        applicationId = "com.example.android"
//        minSdk = 24
//        targetSdk = 36
//        versionCode = 1
//        versionName = "1.0"
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//    }
//
//    externalNativeBuild {
//        cmake { path = file("src/main/cpp/CMakeLists.txt") }
//    }
//    assetPacks += listOf(":model_pack")
//
//    ndkVersion = "27.0.12077973"
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
//}
//
//dependencies {
//
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.appcompat)
//    implementation(libs.material)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//}


plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
    id("jacoco")
}

android {
    namespace = "com.nervesparks.iris"
    compileSdk = 35

    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "com.nervesparks.iris"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            // Add NDK properties if wanted, e.g.
            // abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    buildTypes {
        getByName("debug") {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
}


dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.material:material:1.8.0-alpha07")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation(project(":llama"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.navigation:navigation-runtime-ktx:2.8.5")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.foundation:foundation-layout-android:1.7.6")
    implementation("androidx.games:games-activity:3.0.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room dependencies
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

}

// Jacoco configuration for code coverage
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/data/local/AppDatabase*.*",
        "**/ui/theme/*.*"
    )
    
    val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    
    val mainSrc = "${project.projectDir}/src/main/java"
    
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.buildDir) {
        include("**/*.exec", "**/*.ec")
    })
}

// Task to verify coverage meets threshold
tasks.register("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Verifies that test coverage meets the minimum threshold (80%)"
    dependsOn("jacocoTestReport")
    
    doLast {
        val reportFile = file("${project.buildDir}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        if (reportFile.exists()) {
            val coverage = extractCoverage(reportFile)
            val threshold = 0.80
            
            println("Code coverage: ${String.format("%.2f", coverage * 100)}%")
            
            if (coverage < threshold) {
                throw GradleException(
                    "Code coverage is below threshold. " +
                    "Expected: ${String.format("%.2f", threshold * 100)}%, " +
                    "Actual: ${String.format("%.2f", coverage * 100)}%"
                )
            } else {
                println("✓ Code coverage meets threshold (≥${String.format("%.2f", threshold * 100)}%)")
            }
        } else {
            println("Warning: Coverage report not found at ${reportFile.absolutePath}")
        }
    }
}

fun extractCoverage(reportFile: File): Double {
    val xmlContent = reportFile.readText()
    val counterRegex = """<counter type="INSTRUCTION"[^>]*covered="(\d+)"[^>]*missed="(\d+)"""".toRegex()
    val match = counterRegex.find(xmlContent)
    
    return if (match != null) {
        val covered = match.groupValues[1].toDouble()
        val missed = match.groupValues[2].toDouble()
        if (covered + missed > 0) {
            covered / (covered + missed)
        } else {
            0.0
        }
    } else {
        0.0
    }
}