/*
 * Macrobenchmark + baseline-profile-generator module.
 *
 * Two responsibilities:
 *
 *  1. **Run macrobenchmarks** that measure cold/warm start and frame
 *     timing on a real device. Source files under `src/main/java/.../
 *     StartupBenchmark.kt` and `ScrollBenchmark.kt` drive these.
 *
 *  2. **Generate a baseline profile** at `app/src/main/baseline-prof.txt`
 *     by exercising the most-trodden paths and recording which classes
 *     and methods get JIT'd. The `androidx.baselineprofile` plugin
 *     wires this into the app's release build automatically.
 *
 * Run with:
 *
 *     ./gradlew :baselineprofile:generateBaselineProfile
 *     ./gradlew :baselineprofile:connectedBenchmarkAndroidTest
 *
 * Both require a connected device or a rooted emulator with the
 * benchmark requirements satisfied (debuggable=false test, AOT
 * compile, etc. - the plugin handles most of it).
 */
import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baseline.profile)
}

android {
    namespace = "com.calculator.baselineprofile"
    compileSdk =
        libs.versions.compile.sdk
            .get()
            .toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk =
            libs.versions.min.sdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.target.sdk
                .get()
                .toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    // A virtual device the benchmark can target if no physical device is
    // attached. CI uses this; humans normally just plug in a real phone
    // (the rules in androidx.baselineprofile detect either).
    @Suppress("UnstableApiUsage")
    testOptions.managedDevices.devices {
        create<ManagedVirtualDevice>("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "aosp"
        }
    }
}

baselineProfile {
    // Treat the managed virtual device above as the default target so
    // a contributor can run `./gradlew :app:generateBaselineProfile`
    // without manually attaching a phone.
    managedDevices += "pixel6Api34"
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
    implementation(libs.junit4)
}
