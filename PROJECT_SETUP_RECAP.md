# Iris Android Project Setup - Complete Recap

## Overview
This document provides a comprehensive recap of all the fixes and changes made to successfully build and run the Iris Android application with llama.cpp integration. The project was successfully pushed to [https://github.com/itsnothuy/Iris.git](https://github.com/itsnothuy/Iris.git).

## Project Structure
The project is an Android application that integrates llama.cpp for local AI model inference. It includes:
- Main Android app module
- llama.cpp native module
- Model pack module
- Complete UI with chat interface, model management, and settings

## Major Issues Fixed

### 1. AndroidManifest.xml Issues

**Problem**: XML parsing errors and missing activity references

**Before**:
```xml
<!--<?xml version="1.0" encoding="utf-8"?>-->
<!--<manifest xmlns:android="http://schemas.android.com/apk/res/android"-->
<!--    xmlns:tools="http://schemas.android.com/tools">-->

<!--    <application-->
<!--        android:allowBackup="true"-->
<!--        android:dataExtractionRules="@xml/data_extraction_rules"-->
<!--        android:fullBackupContent="@xml/backup_rules"-->
<!--        android:icon="@mipmap/ic_launcher"-->
<!--        android:label="@string/app_name"-->
<!--        android:roundIcon="@mipmap/ic_launcher_round"-->
<!--        android:supportsRtl="true"-->
<!--        android:theme="@style/Theme.Android" />-->

<!--</manifest>-->

<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.LlamaAndroid"
        tools:targetApi="31">
        <activity
            android:name=".Copy_screen"
            android:exported="false" />
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

**After**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.LlamaAndroid"
        tools:targetApi="31">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

**Changes Made**:
- Removed commented XML declarations that were causing parsing errors
- Removed reference to non-existent `Copy_screen` activity

### 2. Package Namespace Issues

**Problem**: Code was using `com.example.android` but should use `com.nervesparks.iris`

**Before**:
```kotlin
package com.example.android
```

**After**:
```kotlin
package com.nervesparks.iris
```

**Files Updated**:
- All Kotlin files in the project
- Directory structure moved from `com/example/android/` to `com/nervesparks/iris/`

### 3. Build Configuration Issues

**Problem**: minSdk version mismatch and missing CURL disable flag

**Before** (app/build.gradle.kts):
```kotlin
android {
    namespace = "com.example.android"
    compileSdk = 35

    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "com.example.android"
        minSdk = 24
        targetSdk = 34
```

**After**:
```kotlin
android {
    namespace = "com.nervesparks.iris"
    compileSdk = 35

    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "com.nervesparks.iris"
        minSdk = 28
        targetSdk = 34
```

**Before** (llama/build.gradle.kts):
```kotlin
        externalNativeBuild {
            cmake {
                arguments += "-DLLAMA_BUILD_COMMON=ON"
                arguments += "-DCMAKE_BUILD_TYPE=Release"
                cppFlags += listOf()
                arguments += listOf()

                cppFlags("")
            }
        }
        ndk {
            // Add NDK properties if wanted, e.g.
            // abiFilters += listOf("arm64-v8a")
        }
```

**After**:
```kotlin
        externalNativeBuild {
            cmake {
                arguments += "-DLLAMA_BUILD_COMMON=ON"
                arguments += "-DCMAKE_BUILD_TYPE=Release"
                arguments += "-DLLAMA_CURL=OFF"
                cppFlags += listOf()
                arguments += listOf()

                cppFlags("")
            }
        }
        ndk {
            // Add NDK properties if wanted, e.g.
            abiFilters += listOf("arm64-v8a")
        }
```

### 4. C++ API Updates

**Problem**: llama.cpp API had breaking changes that needed to be updated

**Before** (llama/src/main/cpp/llama-android.cpp):
```cpp
auto model = llama_load_model_from_file(path_to_model, model_params);
llama_free_model(reinterpret_cast<llama_model *>(model));
llama_context * context = llama_new_context_with_model(model, ctx_params);
llama_kv_cache_clear(context);
const auto eot = llama_token_eot(model);
```

**After**:
```cpp
auto model = llama_model_load_from_file(path_to_model, model_params);
llama_model_free(reinterpret_cast<llama_model *>(model));
llama_context * context = llama_init_from_model(model, ctx_params);
llama_memory_clear(llama_get_memory(context), true);
const auto eot = llama_vocab_eot(llama_model_get_vocab(model));
```

### 5. CMake Configuration

**Problem**: Missing JSON include paths for nlohmann/json

**Before** (llama/src/main/cpp/CMakeLists.txt):
```cmake
add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        llama-android.cpp)
```

**After**:
```cmake
add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        llama-android.cpp)

# Add include directories for json.hpp
target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
    ../../../../../llama.cpp/vendor
    ../../../../../llama.cpp/vendor/nlohmann
)
```

### 6. Splash Screen Issues

**Problem**: Splash screen configuration causing issues on older API levels

**Solution**: Created API-specific themes file

**Created** (app/src/main/res/values-v31/themes.xml):
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.LlamaAndroid" parent="android:Theme.Material.Light.NoActionBar" >
        <item name="android:windowSplashScreenBackground">@color/black</item>
    </style>
</resources>
```

**Updated** (app/src/main/res/values/themes.xml):
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.LlamaAndroid" parent="android:Theme.Material.Light.NoActionBar" >
    </style>
</resources>
```

### 7. Removed Problematic Files

**Problem**: Incomplete/placeholder files causing compilation errors

**Files Removed**:
- `app/src/main/java/com/nervesparks/iris/core/ml/ModelDownloader.kt`
- `app/src/main/java/com/nervesparks/iris/core/ml/ModelManager.kt`

These files contained incomplete implementations and were not needed for the core functionality.

## Build Process

### Final Build Commands
```bash
./gradlew clean
./gradlew build
./gradlew installDebug
./gradlew assembleRelease
```

### Emulator Setup
```bash
~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.0
~/Library/Android/sdk/platform-tools/adb shell am start -n com.nervesparks.iris/.MainActivity
```

## Project Features

The Iris Android app includes:
- **Chat Interface**: Real-time conversation with AI models
- **Model Management**: Download and manage different AI models
- **Settings**: Configure model parameters and app preferences
- **Benchmark**: Test model performance
- **Search**: Find and download models from repositories

## Technical Stack

- **Language**: Kotlin + C++
- **UI Framework**: Jetpack Compose
- **AI Engine**: llama.cpp
- **Architecture**: MVVM with ViewModel
- **Build System**: Gradle with Kotlin DSL
- **Minimum SDK**: 28 (Android 9.0)
- **Target SDK**: 34 (Android 14)

## Repository Structure

```
Android/
├── app/                    # Main Android application
├── llama/                  # llama.cpp native module
├── model_pack/            # Model packaging module
├── gradle/                # Gradle wrapper and configuration
├── build.gradle.kts       # Root build configuration
├── settings.gradle.kts    # Project settings
└── *.md                   # Documentation files
```

## Success Metrics

✅ **Build Success**: Project compiles without errors
✅ **Emulator Deployment**: App installs and runs on Android emulator
✅ **GitHub Integration**: Code successfully pushed to repository
✅ **API Compatibility**: All llama.cpp API calls updated to latest version
✅ **Package Structure**: Correct namespace and directory structure
✅ **Dependencies**: All build dependencies resolved

## Next Steps

1. **Model Testing**: Test with actual AI models
2. **Performance Optimization**: Optimize for mobile devices
3. **UI Polish**: Enhance user interface and experience
4. **Documentation**: Add comprehensive user documentation
5. **Testing**: Implement unit and integration tests

## Conclusion

The Iris Android project has been successfully set up and is now ready for development and testing. All major compilation issues have been resolved, and the app can be built, installed, and run on Android emulators and devices. 