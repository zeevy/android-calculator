/*
 * Root settings for the Calculator project.
 *
 * Configures plugin and dependency resolution so every module can share a
 * single source of truth via the version catalog in gradle/libs.versions.toml.
 */
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Auto-provisions JDK toolchains (e.g. JDK 17) on machines that don't already
// have them installed. Without this, contributors must install the exact JDK
// version listed in libs.versions.toml manually.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    // Fail the build if any module declares its own repositories; we want them
    // centralized here so dependencies are reviewable in one place.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Calculator"

include(":app")
include(":baselineprofile")
