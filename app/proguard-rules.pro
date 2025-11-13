# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Timber logging in debug builds
-keep class timber.log.** { *; }

# Keep Accessibility Service
-keep class com.voicemode.accessibility.VoiceModeAccessibilityService { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Kotlin
-keep class kotlin.Metadata { *; }

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
