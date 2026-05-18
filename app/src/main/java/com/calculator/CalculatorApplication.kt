package com.calculator

import android.app.Application
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
class CalculatorApplication : Application()
