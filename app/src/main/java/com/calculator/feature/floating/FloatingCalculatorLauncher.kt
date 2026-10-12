package com.calculator.feature.floating

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Returns a click handler that starts the floating calculator.
 *
 * On Android 13 and later it first asks for the notification permission, so the
 * overlay's ongoing notification and its Stop action show in the shade. The
 * overlay starts whether the user allows or denies; its own close button works
 * either way.
 */
@Composable
fun rememberFloatingCalculatorLauncher(): () -> Unit {
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            FloatingCalculatorService.startOrRequestPermission(context)
        }
    return remember(context, permissionLauncher) {
        {
            val needsNotificationPermission =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
            if (needsNotificationPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                FloatingCalculatorService.startOrRequestPermission(context)
            }
        }
    }
}
