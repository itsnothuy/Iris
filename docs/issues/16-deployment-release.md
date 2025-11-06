# Issue #16: Deployment & Release Pipeline

## 🎯 Epic: Production Deployment & Release Management
**Priority**: P1 (High)  
**Estimate**: 8-10 days  
**Dependencies**: All previous issues (#00-#15)  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 16 Deployment & Release

## 📋 Overview
Implement comprehensive deployment and release pipeline for iris_android to ensure reliable, secure, and efficient delivery to production. This system provides automated build optimization, release management, app store deployment, monitoring integration, and rollback capabilities for enterprise-grade software delivery.

## 🎯 Goals
- **Automated Release Pipeline**: End-to-end automated deployment from commit to app store
- **Build Optimization**: Advanced build optimization for minimal APK/AAB size and maximum performance
- **Security & Signing**: Secure code signing and certificate management
- **Progressive Rollout**: Staged rollout with monitoring and automatic rollback capabilities
- **Release Management**: Version management, changelog generation, and release notes
- **Production Monitoring**: Real-time monitoring with alerting and crash reporting

## 📝 Detailed Tasks

### 1. Build Optimization & Configuration

#### 1.1 Advanced Build Configuration
Create `app/build.gradle.kts` (optimized):

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

android {
    namespace = "com.iris.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.iris.android"
        minSdk = 26
        targetSdk = 34
        versionCode = generateVersionCode()
        versionName = generateVersionName()

        testInstrumentationRunner = "com.iris.android.HiltTestRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // NDK configuration for native libraries
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        
        // ProGuard configuration
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            storeFile = file("keystore/iris-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            
            buildConfigField("String", "BUILD_TYPE", "\"debug\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            buildConfigField("String", "API_BASE_URL", "\"https://api-dev.iris-ai.com\"")
            
            // Debug-specific configurations
            manifestPlaceholders["crashlyticsCollectionEnabled"] = false
            manifestPlaceholders["performanceMonitoringEnabled"] = false
        }
        
        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            
            buildConfigField("String", "BUILD_TYPE", "\"staging\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            buildConfigField("String", "API_BASE_URL", "\"https://api-staging.iris-ai.com\"")
            
            manifestPlaceholders["crashlyticsCollectionEnabled"] = true
            manifestPlaceholders["performanceMonitoringEnabled"] = true
            
            signingConfig = signingConfigs.getByName("release")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            
            buildConfigField("String", "BUILD_TYPE", "\"release\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            buildConfigField("String", "API_BASE_URL", "\"https://api.iris-ai.com\"")
            
            manifestPlaceholders["crashlyticsCollectionEnabled"] = true
            manifestPlaceholders["performanceMonitoringEnabled"] = true
            
            signingConfig = signingConfigs.getByName("release")
            
            // Advanced optimizations
            optimization {
                keepRules {
                    // Keep important classes for reflection
                    ignoreWarnings()
                }
            }
        }
    }
    
    // Product flavors for different distributions
    flavorDimensions += "distribution"
    productFlavors {
        create("playstore") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"playstore\"")
        }
        
        create("fdroid") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"fdroid\"")
            // Remove proprietary dependencies for F-Droid
        }
        
        create("enterprise") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"enterprise\"")
            // Enterprise-specific configurations
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-Xcontext-receivers"
        )
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
        dataBinding = false
    }
    
    composeCompiler {
        enableStrongSkippingMode = true
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
    }
    
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/versions/**",
                "/META-INF/INDEX.LIST",
                "/META-INF/io.netty.versions.properties",
                "DebugProbesKt.bin"
            )
        }
        
        // JNI library optimization
        jniLibs {
            useLegacyPackaging = false
        }
    }
    
    // Bundle configuration for Play App Signing
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
    
    // Test options
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        
        managedDevices {
            devices {
                create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel6Api33") {
                    device = "Pixel 6"
                    apiLevel = 33
                    systemImageSource = "aosp"
                }
                
                create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel4Api30") {
                    device = "Pixel 4"
                    apiLevel = 30
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

dependencies {
    // Core libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    
    // Image Loading
    implementation(libs.coil.compose)
    
    // Permissions
    implementation(libs.accompanist.permissions)
    
    // System UI Controller
    implementation(libs.accompanist.systemuicontroller)
    
    // Adaptive layouts
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    
    // ML/AI Libraries
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.onnxruntime.android)
    
    // Monitoring and Analytics
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.performance)
    implementation(libs.firebase.config)
    
    // Core library desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.robolectric)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.android.compiler)
    
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)
}

// Version generation functions
fun generateVersionCode(): Int {
    val major = 1
    val minor = 0
    val patch = 0
    val build = (System.getenv("GITHUB_RUN_NUMBER") ?: "0").toInt()
    return major * 1000000 + minor * 10000 + patch * 100 + build
}

fun generateVersionName(): String {
    val major = 1
    val minor = 0
    val patch = 0
    val preRelease = System.getenv("PRE_RELEASE") ?: ""
    val buildNumber = System.getenv("GITHUB_RUN_NUMBER") ?: "local"
    
    return if (preRelease.isNotEmpty()) {
        "$major.$minor.$patch-$preRelease+$buildNumber"
    } else {
        "$major.$minor.$patch+$buildNumber"
    }
}

// Custom tasks for release management
tasks.register("generateReleaseNotes") {
    group = "release"
    description = "Generate release notes from git commits"
    
    doLast {
        val releaseNotesFile = file("${buildDir}/release-notes.txt")
        val gitLog = "git log --oneline --since='1 week ago'".execute()
        releaseNotesFile.writeText(gitLog)
        println("Release notes generated: ${releaseNotesFile.absolutePath}")
    }
}

tasks.register("validateRelease") {
    group = "release"
    description = "Validate release build before deployment"
    
    dependsOn("assembleRelease", "testReleaseUnitTest", "lintRelease")
    
    doLast {
        println("Release validation completed successfully")
    }
}

// ProGuard optimization
tasks.withType<com.android.build.gradle.tasks.R8Task> {
    doFirst {
        println("Starting R8 optimization for ${name}")
    }
    doLast {
        println("R8 optimization completed for ${name}")
    }
}

// Extension function for executing shell commands
fun String.execute(): String {
    val process = ProcessBuilder(*split(" ").toTypedArray())
        .directory(rootDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    
    process.waitFor(60, TimeUnit.SECONDS)
    return process.inputStream.bufferedReader().readText().trim()
}
```

#### 1.2 ProGuard Rules Optimization
Create `app/proguard-rules.pro`:

```proguard
# Android optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep application class
-keep public class com.iris.android.IrisApplication

# Keep all model classes for serialization
-keep class com.iris.android.data.model.** { *; }
-keep class com.iris.android.core.model.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep @dagger.hilt.InstallIn class *

# Keep Room database classes
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Keep Retrofit and OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowshrinking,allowoptimization interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.*

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keep @androidx.compose.runtime.Stable class *
-keep @androidx.compose.runtime.Immutable class *

# Keep TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.nnapi.** { *; }

# Keep ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# Keep native method names
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Crashlytics
-keepattributes *Annotation*
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Optimize resources
-optimizeresources
-keepresources res/raw/**
-keepresources res/drawable-*dpi/**
```

### 2. Release Automation Pipeline

#### 2.1 Advanced GitHub Actions Workflow
Create `.github/workflows/release.yml`:

```yaml
name: Release Pipeline

on:
  push:
    tags:
      - 'v*'
  workflow_dispatch:
    inputs:
      release_type:
        description: 'Release type'
        required: true
        default: 'patch'
        type: choice
        options:
          - patch
          - minor
          - major
      prerelease:
        description: 'Is this a pre-release?'
        required: false
        type: boolean
        default: false

env:
  GRADLE_OPTS: -Dorg.gradle.daemon=false -Dorg.gradle.workers.max=2 -Dorg.gradle.parallel=false

jobs:
  prepare-release:
    name: Prepare Release
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.version.outputs.version }}
      changelog: ${{ steps.changelog.outputs.changelog }}
      
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      with:
        fetch-depth: 0
        token: ${{ secrets.GITHUB_TOKEN }}
        
    - name: Setup Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '18'
        
    - name: Install semantic-release
      run: |
        npm install -g semantic-release @semantic-release/changelog @semantic-release/git
        
    - name: Generate version and changelog
      id: version
      run: |
        if [[ "${{ github.event_name }}" == "workflow_dispatch" ]]; then
          # Manual release
          CURRENT_VERSION=$(git describe --tags --abbrev=0 2>/dev/null || echo "v0.0.0")
          case "${{ github.event.inputs.release_type }}" in
            "major")
              NEW_VERSION=$(echo $CURRENT_VERSION | awk -F. '{print "v" ($1+1) ".0.0"}' | sed 's/v v/v/')
              ;;
            "minor")
              NEW_VERSION=$(echo $CURRENT_VERSION | awk -F. '{print $1 "." ($2+1) ".0"}')
              ;;
            "patch")
              NEW_VERSION=$(echo $CURRENT_VERSION | awk -F. '{print $1 "." $2 "." ($3+1)}')
              ;;
          esac
        else
          # Tag push
          NEW_VERSION=${GITHUB_REF#refs/tags/}
        fi
        echo "version=$NEW_VERSION" >> $GITHUB_OUTPUT
        
    - name: Generate changelog
      id: changelog
      run: |
        LAST_TAG=$(git describe --tags --abbrev=0 HEAD~1 2>/dev/null || git rev-list --max-parents=0 HEAD)
        CHANGELOG=$(git log --pretty=format:"* %s (%h)" $LAST_TAG..HEAD)
        echo "changelog<<EOF" >> $GITHUB_OUTPUT
        echo "$CHANGELOG" >> $GITHUB_OUTPUT
        echo "EOF" >> $GITHUB_OUTPUT

  build-and-test:
    name: Build and Test
    runs-on: ubuntu-latest
    needs: prepare-release
    strategy:
      matrix:
        build-type: [debug, staging, release]
        
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Setup Gradle
      uses: gradle/gradle-build-action@v2
      with:
        gradle-home-cache-cleanup: true
        
    - name: Decrypt secrets
      run: |
        echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 --decode > app/keystore/iris-release-key.jks
        
    - name: Build ${{ matrix.build-type }}
      run: |
        case "${{ matrix.build-type }}" in
          "debug")
            ./gradlew assembleDebug
            ;;
          "staging")
            ./gradlew assembleStaging
            ;;
          "release")
            ./gradlew bundleRelease assembleRelease
            ;;
        esac
      env:
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        
    - name: Run tests for ${{ matrix.build-type }}
      run: ./gradlew test${{ matrix.build-type }}UnitTest
      
    - name: Run lint for ${{ matrix.build-type }}
      run: ./gradlew lint${{ matrix.build-type }}
      
    - name: Upload build artifacts
      uses: actions/upload-artifact@v4
      with:
        name: build-artifacts-${{ matrix.build-type }}
        path: |
          app/build/outputs/apk/**/*.apk
          app/build/outputs/bundle/**/*.aab
          app/build/reports/lint-results-*.html
        retention-days: 30

  security-scan:
    name: Security Scan
    runs-on: ubuntu-latest
    needs: prepare-release
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Run CodeQL Analysis
      uses: github/codeql-action/init@v3
      with:
        languages: java, kotlin
        
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Build for analysis
      run: ./gradlew assembleDebug
      
    - name: Perform CodeQL Analysis
      uses: github/codeql-action/analyze@v3
      
    - name: Run dependency check
      uses: dependency-check/Dependency-Check_Action@main
      with:
        project: 'iris-android'
        path: '.'
        format: 'ALL'
        args: >
          --enableRetired
          --enableExperimental
          --failOnCVSS 7
          
    - name: Upload security results
      uses: actions/upload-artifact@v4
      with:
        name: security-scan-results
        path: reports/

  performance-test:
    name: Performance Testing
    runs-on: macos-latest
    needs: prepare-release
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Setup Gradle
      uses: gradle/gradle-build-action@v2
      
    - name: Run performance benchmarks
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 33
        target: google_apis
        arch: x86_64
        profile: pixel_6
        script: ./gradlew connectedBenchmarkAndroidTest
        
    - name: Upload performance results
      uses: actions/upload-artifact@v4
      with:
        name: performance-test-results
        path: app/build/reports/benchmark/

  create-release:
    name: Create GitHub Release
    runs-on: ubuntu-latest
    needs: [prepare-release, build-and-test, security-scan, performance-test]
    if: success()
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Download build artifacts
      uses: actions/download-artifact@v4
      with:
        name: build-artifacts-release
        path: artifacts/
        
    - name: Create GitHub Release
      uses: softprops/action-gh-release@v1
      with:
        tag_name: ${{ needs.prepare-release.outputs.version }}
        name: Release ${{ needs.prepare-release.outputs.version }}
        body: |
          ## 🚀 What's New
          
          ${{ needs.prepare-release.outputs.changelog }}
          
          ## 📱 Downloads
          
          - **Android App Bundle (Recommended)**: For Google Play Store
          - **APK**: For direct installation
          
          ## 🔧 Technical Details
          
          - **Min SDK**: 26 (Android 8.0)
          - **Target SDK**: 34 (Android 14)
          - **Architecture**: arm64-v8a, armeabi-v7a
          
          ## 🛡️ Security
          
          This release has been scanned for security vulnerabilities and passed all checks.
          
        files: |
          artifacts/bundle/release/app-release.aab
          artifacts/apk/release/app-release.apk
        draft: ${{ github.event.inputs.prerelease == 'true' }}
        prerelease: ${{ github.event.inputs.prerelease == 'true' }}
        generate_release_notes: true

  deploy-play-store:
    name: Deploy to Play Store
    runs-on: ubuntu-latest
    needs: [create-release]
    if: success() && !github.event.inputs.prerelease
    environment: production
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Download build artifacts
      uses: actions/download-artifact@v4
      with:
        name: build-artifacts-release
        path: artifacts/
        
    - name: Deploy to Play Store (Internal Testing)
      uses: r0adkll/upload-google-play@v1
      with:
        serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}
        packageName: com.iris.android
        releaseFiles: artifacts/bundle/release/app-release.aab
        track: internal
        status: completed
        whatsNewDirectory: fastlane/metadata/android/en-US/changelogs
        
    - name: Promote to Alpha (if staging successful)
      uses: r0adkll/upload-google-play@v1
      if: success()
      with:
        serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}
        packageName: com.iris.android
        track: alpha
        inAppUpdatePriority: 3
        
  deploy-firebase:
    name: Deploy to Firebase App Distribution
    runs-on: ubuntu-latest
    needs: [create-release]
    if: success()
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Download build artifacts
      uses: actions/download-artifact@v4
      with:
        name: build-artifacts-release
        path: artifacts/
        
    - name: Deploy to Firebase App Distribution
      uses: wzieba/Firebase-Distribution-Github-Action@v1
      with:
        appId: ${{ secrets.FIREBASE_APP_ID }}
        serviceCredentialsFileContent: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}
        groups: testers, qa-team
        file: artifacts/apk/release/app-release.apk
        releaseNotes: |
          Release ${{ needs.prepare-release.outputs.version }}
          
          ${{ needs.prepare-release.outputs.changelog }}

  update-documentation:
    name: Update Documentation
    runs-on: ubuntu-latest
    needs: [create-release]
    if: success()
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      with:
        token: ${{ secrets.GITHUB_TOKEN }}
        
    - name: Update version in documentation
      run: |
        sed -i "s/version: .*/version: ${{ needs.prepare-release.outputs.version }}/" docs/README.md
        sed -i "s/Latest Release: .*/Latest Release: ${{ needs.prepare-release.outputs.version }}/" README.md
        
    - name: Commit documentation updates
      run: |
        git config --local user.email "action@github.com"
        git config --local user.name "GitHub Action"
        git add .
        git diff --staged --quiet || git commit -m "docs: update version to ${{ needs.prepare-release.outputs.version }}"
        git push

  notify-release:
    name: Notify Team
    runs-on: ubuntu-latest
    needs: [create-release, deploy-play-store, deploy-firebase]
    if: always()
    
    steps:
    - name: Notify Slack
      uses: 8398a7/action-slack@v3
      with:
        status: ${{ job.status }}
        channel: '#releases'
        message: |
          🚀 *Release ${{ needs.prepare-release.outputs.version }}* 
          
          Status: ${{ job.status == 'success' && '✅ Successful' || '❌ Failed' }}
          
          📱 *Play Store*: ${{ needs.deploy-play-store.result == 'success' && 'Deployed' || 'Failed' }}
          🔥 *Firebase*: ${{ needs.deploy-firebase.result == 'success' && 'Deployed' || 'Failed' }}
          
          *Changes:*
          ${{ needs.prepare-release.outputs.changelog }}
      env:
        SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

### 3. App Store Optimization

#### 3.1 Play Store Metadata
Create `fastlane/metadata/android/en-US/`:

```
title.txt:
Iris AI - Your Personal Assistant

short_description.txt:
Intelligent on-device AI assistant for conversations, productivity, and creative tasks.

full_description.txt:
🤖 **Iris AI - The Future of Personal AI Assistance**

Experience the power of artificial intelligence right on your Android device with Iris AI, your intelligent personal assistant that prioritizes privacy, performance, and productivity.

✨ **Key Features:**

🔒 **Privacy-First Design**
• All AI processing happens on your device
• No data sent to external servers
• Complete control over your conversations
• GDPR and privacy-compliant

🚀 **Powerful On-Device AI**
• Advanced language models optimized for mobile
• Support for text, images, and voice input
• Real-time responses without internet dependency
• Adaptive performance across all Android devices

💬 **Intelligent Conversations**
• Natural language understanding
• Context-aware responses
• Multi-turn conversations with memory
• Custom prompts and templates

🎯 **Productivity Features**
• Document analysis and summarization
• Code generation and debugging
• Creative writing assistance
• Task planning and organization

🎨 **Multimodal Capabilities**
• Image analysis and description
• OCR text extraction
• Voice input and speech synthesis
• Document processing

⚡ **Optimized Performance**
• Intelligent memory management
• Thermal-aware processing
• Battery optimization
• Smooth 60+ FPS interface

🛡️ **Enterprise-Grade Security**
• Content filtering and safety checks
• Secure model management
• Encrypted local storage
• Professional deployment options

📱 **Modern Android Experience**
• Material Design 3 interface
• Adaptive layouts for all screen sizes
• Full accessibility support
• Dark and light themes

Whether you're a student, professional, creator, or just curious about AI, Iris AI provides powerful assistance while keeping your data private and secure.

**System Requirements:**
• Android 8.0 (API 26) or higher
• 4GB RAM recommended
• 2GB free storage space
• ARMv8 or x86_64 processor

Download Iris AI today and experience the future of personal AI assistance!

keywords.txt:
AI, artificial intelligence, assistant, chatbot, privacy, on-device, machine learning, productivity, conversations, voice, text, image, OCR, offline, android, material design
```

#### 3.2 Fastlane Configuration
Create `fastlane/Fastfile`:

```ruby
default_platform(:android)

platform :android do
  desc "Deploy a new version to the Google Play Store"
  lane :deploy do
    gradle(task: "clean bundleRelease")
    
    upload_to_play_store(
      track: 'internal',
      release_status: 'completed',
      aab: 'app/build/outputs/bundle/release/app-release.aab',
      skip_upload_metadata: false,
      skip_upload_images: false,
      skip_upload_screenshots: false
    )
  end

  desc "Deploy to alpha track"
  lane :alpha do
    upload_to_play_store(
      track: 'alpha',
      track_promote_to: 'alpha',
      skip_upload_changelogs: false
    )
  end

  desc "Deploy to production"
  lane :production do
    upload_to_play_store(
      track: 'production',
      track_promote_to: 'production',
      skip_upload_changelogs: false
    )
  end

  desc "Take screenshots"
  lane :screenshots do
    gradle(task: "assembleDebug assembleAndroidTest")
    screengrab
  end

  desc "Generate release notes"
  lane :release_notes do
    changelog = changelog_from_git_commits(
      pretty: "- %s",
      date_format: "short",
      match_lightweight_tag: false,
      merge_commit_filtering: "exclude_merges"
    )
    
    File.write("../fastlane/metadata/android/en-US/changelogs/#{get_version_code}.txt", changelog)
  end
end
```

### 4. Monitoring & Observability

#### 4.1 Production Monitoring Setup
Create `app/src/main/kotlin/monitoring/ProductionMonitoring.kt`:

```kotlin
@Singleton
class ProductionMonitoringImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceManager: PerformanceManager
) : ProductionMonitoring {
    
    companion object {
        private const val TAG = "ProductionMonitoring"
    }
    
    override suspend fun initialize() {
        if (BuildConfig.BUILD_TYPE == "release") {
            initializeCrashlytics()
            initializePerformanceMonitoring()
            initializeAnalytics()
            startHealthChecks()
        }
    }
    
    private fun initializeCrashlytics() {
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(true)
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            setCustomKey("device_info", getDeviceInfo())
        }
    }
    
    private fun initializePerformanceMonitoring() {
        FirebasePerformance.getInstance().apply {
            isPerformanceCollectionEnabled = true
        }
    }
    
    private fun initializeAnalytics() {
        FirebaseAnalytics.getInstance(context).apply {
            setAnalyticsCollectionEnabled(true)
            setDefaultEventParameters(Bundle().apply {
                putString("app_version", BuildConfig.VERSION_NAME)
                putString("device_class", getDeviceClass())
            })
        }
    }
    
    private suspend fun startHealthChecks() {
        // Start periodic health monitoring
        while (true) {
            try {
                performHealthCheck()
                delay(60_000) // Check every minute
            } catch (e: Exception) {
                Log.e(TAG, "Health check failed", e)
                recordException(e, "health_check_failed")
            }
        }
    }
    
    private suspend fun performHealthCheck() {
        val healthMetrics = mutableMapOf<String, Any>()
        
        // Memory health
        val memoryUsage = performanceManager.getCurrentPerformanceMetrics().memoryUsage
        healthMetrics["memory_usage_percent"] = memoryUsage.usagePercentage
        
        // Performance health
        val performanceMetrics = performanceManager.getCurrentPerformanceMetrics()
        healthMetrics["cpu_usage_percent"] = performanceMetrics.cpuUsage.totalUsage
        healthMetrics["thermal_state"] = performanceMetrics.thermalInfo.status
        
        // Record health metrics
        recordCustomMetrics("app_health", healthMetrics)
        
        // Alert on critical conditions
        if (memoryUsage.usagePercentage > 90f) {
            recordException(
                Exception("Critical memory usage: ${memoryUsage.usagePercentage}%"),
                "critical_memory_usage"
            )
        }
    }
    
    override fun recordException(exception: Throwable, context: String?) {
        FirebaseCrashlytics.getInstance().apply {
            if (context != null) {
                setCustomKey("error_context", context)
            }
            recordException(exception)
        }
    }
    
    override fun recordCustomMetrics(event: String, parameters: Map<String, Any>) {
        val bundle = Bundle()
        parameters.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putFloat(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        
        FirebaseAnalytics.getInstance(context).logEvent(event, bundle)
    }
    
    override fun setUserProperty(name: String, value: String) {
        FirebaseAnalytics.getInstance(context).setUserProperty(name, value)
    }
    
    private fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} (${Build.VERSION.RELEASE})"
    }
    
    private fun getDeviceClass(): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return when {
            memoryInfo.totalMem < 2L * 1024 * 1024 * 1024 -> "low_end"      // < 2GB
            memoryInfo.totalMem < 4L * 1024 * 1024 * 1024 -> "mid_range"    // < 4GB
            memoryInfo.totalMem < 8L * 1024 * 1024 * 1024 -> "high_end"     // < 8GB
            else -> "flagship"                                                // 8GB+
        }
    }
}
```

### 5. Rollback and Recovery

#### 5.1 Automated Rollback System
Create `.github/workflows/rollback.yml`:

```yaml
name: Emergency Rollback

on:
  workflow_dispatch:
    inputs:
      target_version:
        description: 'Version to rollback to'
        required: true
        type: string
      reason:
        description: 'Reason for rollback'
        required: true
        type: string

jobs:
  rollback:
    name: Emergency Rollback
    runs-on: ubuntu-latest
    environment: production
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      with:
        ref: ${{ github.event.inputs.target_version }}
        
    - name: Validate rollback target
      run: |
        if ! git tag -l | grep -q "^${{ github.event.inputs.target_version }}$"; then
          echo "Error: Tag ${{ github.event.inputs.target_version }} does not exist"
          exit 1
        fi
        
    - name: Download rollback artifacts
      run: |
        gh release download ${{ github.event.inputs.target_version }} --pattern "*.aab" --dir artifacts/
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        
    - name: Rollback Play Store
      uses: r0adkll/upload-google-play@v1
      with:
        serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}
        packageName: com.iris.android
        releaseFiles: artifacts/app-release.aab
        track: production
        status: completed
        whatsNewDirectory: .github/rollback-notes
        
    - name: Create rollback issue
      uses: actions/github-script@v7
      with:
        script: |
          github.rest.issues.create({
            owner: context.repo.owner,
            repo: context.repo.repo,
            title: `🚨 Emergency Rollback to ${{ github.event.inputs.target_version }}`,
            body: `## Rollback Details
            
            **Target Version**: ${{ github.event.inputs.target_version }}
            **Reason**: ${{ github.event.inputs.reason }}
            **Triggered By**: @${{ github.actor }}
            **Time**: ${new Date().toISOString()}
            
            ## Next Steps
            
            - [ ] Verify rollback is successful
            - [ ] Investigate root cause
            - [ ] Plan fix for the issue
            - [ ] Test fix thoroughly
            - [ ] Plan new release
            
            ## Monitoring
            
            Monitor the following metrics after rollback:
            - Crash rates
            - User engagement
            - Performance metrics
            - User feedback
            `,
            labels: ['bug', 'critical', 'rollback']
          })
          
    - name: Notify team
      uses: 8398a7/action-slack@v3
      with:
        status: success
        channel: '#alerts'
        message: |
          🚨 **EMERGENCY ROLLBACK COMPLETED**
          
          **From**: Current production version
          **To**: ${{ github.event.inputs.target_version }}
          **Reason**: ${{ github.event.inputs.reason }}
          **Triggered by**: @${{ github.actor }}
          
          Please monitor production metrics closely.
      env:
        SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

## 🧪 Testing Strategy

### Pre-Release Testing
- [ ] **Automated Testing**: Full test suite execution on multiple Android versions
- [ ] **Performance Testing**: Benchmarking against previous releases
- [ ] **Security Scanning**: Vulnerability assessment and code analysis
- [ ] **Compatibility Testing**: Testing across different device configurations

### Release Validation
- [ ] **Smoke Testing**: Critical functionality validation
- [ ] **Integration Testing**: End-to-end workflow verification
- [ ] **Performance Regression**: Comparison with baseline metrics
- [ ] **Security Verification**: Final security checks before deployment

### Post-Release Monitoring
- [ ] **Crash Monitoring**: Real-time crash rate tracking
- [ ] **Performance Monitoring**: Application performance metrics
- [ ] **User Feedback**: App store reviews and ratings monitoring
- [ ] **Business Metrics**: User engagement and retention tracking

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **Automated Pipeline**: Complete end-to-end automation from commit to app store
- [ ] **Build Optimization**: Optimized APK/AAB size and performance
- [ ] **Security Compliance**: All security scans passing
- [ ] **Progressive Rollout**: Staged deployment with monitoring
- [ ] **Rollback Capability**: Emergency rollback procedures tested and documented

### Technical Criteria
- [ ] **Build Time**: Release build completes in <30 minutes
- [ ] **APK Size**: Release APK under 50MB
- [ ] **Security**: Zero critical vulnerabilities in release
- [ ] **Monitoring**: Real-time monitoring and alerting functional

### Quality Criteria
- [ ] **Release Notes**: Automated generation and publishing
- [ ] **Documentation**: Release process fully documented
- [ ] **Team Training**: Team trained on release and rollback procedures
- [ ] **Incident Response**: Incident response procedures defined and tested

## 🔗 Related Issues
- **Depends on**: All previous issues (#00-#15)
- **Completes**: Full project implementation

## 📋 Definition of Done
- [ ] Complete automated release pipeline functional
- [ ] Build optimization achieving target metrics
- [ ] Security scanning and compliance verification
- [ ] Progressive rollout system implemented
- [ ] Emergency rollback procedures tested
- [ ] Production monitoring and alerting active
- [ ] App store metadata and screenshots uploaded
- [ ] Release documentation complete
- [ ] Team training on release process completed
- [ ] Code review completed and approved

---

**Note**: This deployment and release pipeline ensures reliable, secure, and efficient delivery of iris_android to production with comprehensive monitoring and rollback capabilities.