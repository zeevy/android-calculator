package com.calculator

import android.app.Application
import com.calculator.feature.shortcuts.RecentToolsRegistry
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * The [HiltAndroidApp] annotation triggers Hilt's code generation,
 * creating the application-level dependency graph that subsequent
 * `@AndroidEntryPoint` activities, fragments, services and view models
 * pull dependencies from.
 *
 * Keep this class small: heavy initialisation work should live in
 * AndroidX `Initializer` implementations so it can be lazily wired up
 * via the `androidx.startup` library, avoiding a slow cold start.
 */
@HiltAndroidApp
class CalculatorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Republish launcher shortcuts on cold start so Float (and any
        // saved recents) are present before the user has navigated this
        // session. setDynamicShortcuts is cheap (a single binder call to
        // the system shortcut service) and idempotent.
        RecentToolsRegistry.refresh(this)
    }
}
