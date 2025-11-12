# Release Signing Configuration

## Create Release Keystore

```bash
# Generate release keystore (run this once)
keytool -genkey -v -keystore iris-release.keystore -alias iris-app -keyalg RSA -keysize 2048 -validity 10000

# Store keystore securely - DO NOT commit to git
# Add iris-release.keystore to .gitignore
```

## Configure Signing in app/build.gradle.kts

Add this to your `android` block in `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("path/to/iris-release.keystore")
            storePassword = "YOUR_STORE_PASSWORD"
            keyAlias = "iris-app"
            keyPassword = "YOUR_KEY_PASSWORD"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## Environment Variables (Recommended)

For security, use environment variables:

```bash
# Add to your shell profile
export IRIS_KEYSTORE_PASSWORD="your_keystore_password"
export IRIS_KEY_PASSWORD="your_key_password"
```

Then in build.gradle.kts:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("iris-release.keystore")
        storePassword = System.getenv("IRIS_KEYSTORE_PASSWORD")
        keyAlias = "iris-app"
        keyPassword = System.getenv("IRIS_KEY_PASSWORD")
    }
}
```

## Build Release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`