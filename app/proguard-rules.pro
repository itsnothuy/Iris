# ProGuard/R8 Rules for iris_android
# Production-ready configuration for on-device AI assistant

# ============================================================================
# General Android Rules
# ============================================================================

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*

# Keep generic signatures for reflection
-keepattributes Signature

# Keep exceptions
-keepattributes Exceptions

# ============================================================================
# Kotlin Rules
# ============================================================================

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# Kotlin lambda expressions
-dontwarn java.lang.invoke.*
-dontwarn **$serializer

# ============================================================================
# Native Code / JNI Rules
# ============================================================================

# Keep all native method names for JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI interfaces for llama.cpp integration
-keep class com.nervesparks.iris.core.** { *; }
-keep interface com.nervesparks.iris.core.** { *; }

# Keep native library classes
-keep class com.nervesparks.iris.llama.** { *; }

# Keep classes that interface with native code
-keepclassmembers class * {
    @com.nervesparks.iris.annotations.NativeCallback *;
}

# ============================================================================
# Jetpack Compose Rules
# ============================================================================

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ============================================================================
# Room Database Rules
# ============================================================================

# Keep Room annotations
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# Keep Room TypeConverters
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.EntityInsertionAdapter
-keep class * extends androidx.room.EntityDeletionOrUpdateAdapter
-keep class * extends androidx.room.SharedSQLiteStatement

# ============================================================================
# Data Classes & Serialization
# ============================================================================

# Keep data classes used in persistence
-keep class com.nervesparks.iris.data.local.** { *; }
-keep class com.nervesparks.iris.data.model.** { *; }

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================================
# ViewModel & Lifecycle Rules
# ============================================================================

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Keep lifecycle methods
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# ============================================================================
# Reflection & Dynamic Loading
# ============================================================================

# Keep classes used via reflection
-keep class com.nervesparks.iris.**.Factory { *; }
-keep class com.nervesparks.iris.**.Builder { *; }

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# Security & Privacy Rules
# ============================================================================

# Keep security-related classes
-keep class com.nervesparks.iris.data.privacy.** { *; }
-keep class com.nervesparks.iris.security.** { *; }

# Keep encryption-related classes
-keep class androidx.security.crypto.** { *; }

# ============================================================================
# AI Model & Inference Rules
# ============================================================================

# Keep AI model interfaces and implementations
-keep class com.nervesparks.iris.ai.** { *; }
-keep interface com.nervesparks.iris.ai.** { *; }

# Keep model parameter classes
-keep class com.nervesparks.iris.model.** { *; }

# ============================================================================
# Navigation & UI Rules
# ============================================================================

# Keep navigation destinations
-keep class * extends androidx.navigation.Navigator

# Keep custom views
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ============================================================================
# Third-Party Libraries
# ============================================================================

# OkHttp (if used for future features)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ============================================================================
# Optimization Settings
# ============================================================================

# Number of optimization passes
-optimizationpasses 5

# Optimization filters
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Allow aggressive optimization
-allowaccessmodification
-mergeinterfacesaggressively

# ============================================================================
# Warning Suppression
# ============================================================================

# Suppress warnings for missing classes (they may not be used)
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============================================================================
# Debug-Specific Rules
# ============================================================================

# In debug builds, keep everything for easier debugging
# These will be ignored in release builds

# ============================================================================
# Additional Notes
# ============================================================================
# 
# This configuration is designed for:
# - On-device AI inference with native libraries
# - Privacy-first architecture (no network calls)
# - Jetpack Compose UI
# - Room database persistence
# - Kotlin coroutines
# 
# When adding new features:
# 1. Test thoroughly with ProGuard enabled
# 2. Check for any reflection-based code
# 3. Add specific rules for new libraries
# 4. Verify native JNI interfaces work
# 
# To test ProGuard rules:
# ./gradlew assembleRelease
# Analyze the generated mapping file and test the app
