/*
 * Root build file for the Calculator project.
 *
 * All plugins are declared here with `apply false` so each subproject can
 * opt in via the version catalog. Keeping plugin versions in one place
 * avoids drift between modules.
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.android.junit5) apply false
}
