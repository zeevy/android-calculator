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

# kotlinx.serialization: the navigation routes (navigation/Destinations.kt)
# are @Serializable objects; keep their generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.calculator.**$$serializer { *; }
-keepclasseswithmembers class com.calculator.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt-generated components are kept by the Hilt Gradle plugin; no extra rules needed here.
