# ProGuard / R8 rules for the Calculator release build.
#
# The default `proguard-android-optimize.txt` already handles:
#   - AndroidX
#   - Kotlin metadata
#   - Compose
#   - kotlinx.coroutines
#
# Add app-specific keep rules below as new libraries are integrated.

# Keep the application class so the manifest reference resolves after shrinking.
-keep class com.calculator.CalculatorApplication { *; }

# kotlinx.serialization: keep serializer companions for @Serializable types.
# Replace `com.calculator.**` with the actual packages containing serializable
# DTOs when the currency feature is wired in.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.calculator.**$$serializer { *; }
-keepclassmembers class com.calculator.** {
    *** Companion;
}
-keepclasseswithmembers class com.calculator.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp: preserve generic signatures used to construct calls.
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Hilt-generated components are kept by the Hilt Gradle plugin; no extra rules needed here.
