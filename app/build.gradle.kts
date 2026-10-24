/*
 * App module build script.
 *
 * This module is the single application module. Once feature complexity
 * justifies it (typically when build times suffer or feature isolation
 * matters), individual features can be split into library modules under
 * `feature/<name>/` and `core/<name>/` per the layout in CLAUDE.md.
 */
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.android.junit5)
}

android {
    namespace = "com.calculator"
    compileSdk =
        libs.versions.compile.sdk
            .get()
            .toInt()

    defaultConfig {
        // The Play package name. It can never change once the app is on Play.
        // The Kotlin packages keep the shorter `com.calculator` namespace.
        applicationId = "in.co.gorest.calculator"
        minSdk =
            libs.versions.min.sdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.target.sdk
                .get()
                .toInt()
        // Phase 11 versioning. Bumping `versionName` always pairs with
        // a bump of `versionCode` (monotonic across the lifetime of
        // the app). See docs/RELEASE.md for the formula.
        versionCode = 10303
        versionName = "1.3.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // The Release workflow fills these from GitHub secrets. Without them the
    // release build stays unsigned, so local and CI builds need no key.
    val releaseKeystore = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull
    if (releaseKeystore != null) {
        signingConfigs.create("release") {
            storeFile = file(releaseKeystore)
            storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
        }
    }

    buildTypes {
        debug {
            // Keep debug builds fast; no shrinking, app is suffixed so it
            // can coexist on-device with a release install.
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // R8 shrink + resource shrink to meet the < 15 MB install-size goal.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Signed with the upload key only when the RELEASE_KEYSTORE_*
            // variables are set. See "One-time setup for Google Play" in
            // docs/RELEASE.md.
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // Exclude duplicated metadata that surfaces from various libraries
            // and would otherwise fail the merge step.
            excludes +=
                setOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "META-INF/LICENSE*",
                    "META-INF/NOTICE*",
                )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            // Same default locale on every machine, so formatting tests do
            // not depend on how the contributor's computer is set up. Tests
            // for other locales set them explicitly.
            all {
                it.systemProperty("user.language", "en")
                it.systemProperty("user.country", "US")
            }
        }
    }

    // The Room migration test reads the exported schemas as assets.
    // Robolectric only sees the merged assets of the variant under test
    // (not the `test` source set), so they go into debug builds only.
    sourceSets.getByName("debug").assets.srcDir("$projectDir/schemas")
}

// Exported Room schemas are committed, so every schema change shows up in
// review and the migration test can open old versions.
room {
    schemaDirectory("$projectDir/schemas")
}

// Kotlin extension lives at the project level, not inside `android { }`.
kotlin {
    jvmToolchain(
        libs.versions.java.toolchain
            .get()
            .toInt(),
    )
}

dependencies {
    // ----- Compose (BOM keeps versions in sync) -----
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    // Robolectric `createComposeRule()` launches a generic
    // ComponentActivity declared by the compose-ui-test-manifest AAR.
    // Adding it as `implementation` makes the declaration land in the
    // merged manifest of every build variant (debug / release) so
    // testReleaseUnitTest can resolve the activity. The AAR contains
    // only the manifest stub - no runtime code, so the production cost
    // is one extra `<activity>` line. The stub declares it exported;
    // AndroidManifest.xml overrides that to exported="false".
    implementation(libs.compose.ui.test.manifest)

    // ----- AndroidX core / lifecycle / navigation -----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.startup)
    // Phase 10: Glance for the home-screen widget.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // ----- Persistence -----
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ----- DI -----
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)

    // ----- Async + serialization -----
    // kotlinx.serialization.json is required by Navigation Compose's
    // type-safe routes; it is the only reason serialization is on the
    // classpath now that the network layer is gone. Don't remove it.
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // No network deps: Retrofit / OkHttp and their catalog entries were
    // removed when the currency converter was deleted, along with the
    // INTERNET permission. The app is fully offline.

    // ----- Unit tests (JUnit5 for our own tests; JUnit4 vintage runner
    //                  carries Robolectric tests since Robolectric is
    //                  JUnit4-only - both run from the same task) -----
    testImplementation(libs.junit5.jupiter.api)
    testImplementation(libs.junit5.jupiter.params)
    testRuntimeOnly(libs.junit5.jupiter.engine)
    testImplementation(libs.junit5.vintage.engine)
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    // Compose UI test on the JVM via Robolectric: the same APIs
    // (createComposeRule, onNodeWithText, performClick, ...) run
    // headlessly on `./gradlew test`, no emulator or device needed.
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.bom)
    testImplementation(libs.compose.ui.test.manifest)
    // Kotest property-based testing. Used by EvaluatorPropertyTest -
    // generates random inputs and asserts mathematical identities hold
    // across hundreds of samples, catching the kind of edge case a
    // hand-written golden catalogue can miss. We deliberately skip the
    // kotest-runner-junit5 plugin here because our tests use plain
    // @org.junit.jupiter.api.Test entry points and call checkAll
    // directly inside the test bodies - that keeps the JUnit 5 Jupiter
    // engine in charge of discovery and avoids the runner-conflict
    // failures that come from layering two engines.
    testImplementation(libs.kotest.property)

    // ----- Detekt formatting rules -----
    detektPlugins(libs.detekt.formatting)
}

// ----- ktlint configuration -----
ktlint {
    version.set("1.4.1")
    android.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
    }
    filter {
        // Skip generated sources (Hilt, Room, KSP outputs).
        exclude { it.file.path.contains("/build/") }
    }
}

// ----- detekt configuration -----
detekt {
    config.setFrom(rootProject.files("config/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
}

tasks.withType<Detekt>().configureEach {
    jvmTarget =
        libs.versions.java.toolchain
            .get()
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(true)
    }
}
