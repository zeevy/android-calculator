package com.calculator.feature.floating

import android.app.Activity
import android.os.Bundle

/**
 * Headless trampoline for the "Floating calculator" launcher shortcut.
 *
 * Launching the foreground service requires an active context (the
 * launcher's process can't call `startForegroundService` on our
 * behalf), so the shortcut points here. The activity uses a fully-
 * translucent theme and calls `finish()` immediately - the user sees
 * the overlay appear (or the system settings page if SYSTEM_ALERT_WINDOW
 * hasn't been granted yet) with no flash from MainActivity's splash.
 *
 * It does not ask for the notification permission: it finishes before a
 * dialog could show. The overlay works without it; the user is asked the
 * first time they start it from Settings.
 */
class FloatingShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FloatingCalculatorService.startOrRequestPermission(this)
        finish()
    }
}
