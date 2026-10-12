package com.calculator.feature.floating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.calculator.R
import com.calculator.core.data.di.SettingsEntryPoint
import com.calculator.core.data.settings.UserSettings
import com.calculator.core.math.EvaluationResult
import com.calculator.core.math.Evaluator
import com.calculator.core.math.ExpressionEditor
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.MathContext
import java.math.RoundingMode

/**
 * Hosts the floating calculator overlay.
 *
 * The overlay is a single `WindowManager`-attached view; the service
 * exists only so the system keeps the window alive once the user
 * leaves the app. A drag handler on the header rewrites the
 * `WindowManager.LayoutParams.x`/`y`. The keypad builds an expression
 * with the shared [ExpressionEditor] and evaluates it with [Evaluator],
 * so it follows the same rules (operator precedence included) as the
 * app; only the window is View-based, because Compose needs more setup
 * outside an Activity.
 *
 * The service starts itself as a foreground service so the system
 * doesn't reclaim it; the notification it posts is mandatory on API
 * 26+ and includes a Stop action so the user can dismiss the overlay
 * from the shade as well as from the X button.
 */
class FloatingCalculatorService : Service() {

    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private lateinit var windowManager: WindowManager
    private lateinit var displayText: TextView
    private var numberTextSize = 0f

    // Mini calculator state: the expression typed so far, built with the
    // same ExpressionEditor rules as the app, whether it is a result just
    // produced by `=`, and an error message to show instead of it.
    private var expression: String = ""
    private var afterEquals: Boolean = false
    private var errorText: String? = null
    private var precision: Int = UserSettings.DEFAULT_PRECISION
    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // startForeground must run even when we stop at once, or the system
        // crashes the app for a foreground service that never went foreground.
        startForegroundCompat()
        // A caller may start the service without the overlay permission (or it
        // is revoked in between); stop quietly instead of crashing in addView.
        if (!Settings.canDrawOverlays(this) || !addOverlay()) {
            stopSelf()
            return
        }
        // Follow the precision setting, like the app's calculator.
        val settings = EntryPointAccessors.fromApplication(this, SettingsEntryPoint::class.java).settingsRepository()
        scope.launch { settings.settings.collect { precision = it.precision } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        overlayView?.let { v ->
            // removeView throws if the view isn't actually attached
            // (race during teardown); guard with isAttachedToWindow.
            if (v.isAttachedToWindow) runCatching { windowManager.removeView(v) }
        }
        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ----- Overlay setup -----

    /** Adds the overlay window. Returns false when the system refuses it. */
    private fun addOverlay(): Boolean {
        val view = LayoutInflater.from(this).inflate(R.layout.floating_calculator, null)
        // TYPE_APPLICATION_OVERLAY is the only legal type on API 26+ for
        // overlays drawn by user apps. Older types (TYPE_SYSTEM_ALERT,
        // TYPE_PHONE) are gated to system apps.
        //
        // Width is FIXED (not WRAP_CONTENT) because the keypad uses
        // weight=1 layouts: with a wrap-content parent the buttons have
        // no width to distribute and the whole window collapses to the
        // intrinsic width of the longest text label (about 3 digits
        // wide). The fixed width here is what gives the columns room
        // to actually spread.
        val density = resources.displayMetrics.density
        val widthPx = (WIDTH_DP * density).toInt()
        val params = WindowManager.LayoutParams(
            widthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // NOT_FOCUSABLE so the overlay doesn't steal IME / back-press
            // from whatever app is underneath it.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = (INITIAL_X_DP * density).toInt()
        params.y = (INITIAL_Y_DP * density).toInt()

        try {
            windowManager.addView(view, params)
        } catch (e: WindowManager.BadTokenException) {
            Log.w(TAG, "Overlay window refused", e)
            return false
        } catch (e: SecurityException) {
            Log.w(TAG, "Overlay permission missing", e)
            return false
        }
        overlayView = view
        layoutParams = params

        displayText = view.findViewById(R.id.floating_display)
        numberTextSize = displayText.textSize
        wireDragging(view.findViewById(R.id.floating_header), params)
        wireButtons(view)
        renderDisplay()
        return true
    }

    private fun wireDragging(header: View, params: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (startX + (event.rawX - touchX)).toInt()
                    params.y = (startY + (event.rawY - touchY)).toInt()
                    overlayView?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                else -> false
            }
        }
    }

    private fun wireButtons(root: View) {
        val keys =
            mapOf(
                R.id.floating_0 to "0",
                R.id.floating_1 to "1",
                R.id.floating_2 to "2",
                R.id.floating_3 to "3",
                R.id.floating_4 to "4",
                R.id.floating_5 to "5",
                R.id.floating_6 to "6",
                R.id.floating_7 to "7",
                R.id.floating_8 to "8",
                R.id.floating_9 to "9",
                R.id.floating_dot to ".",
                R.id.floating_pct to "%",
                R.id.floating_add to "+",
                R.id.floating_sub to "-",
                R.id.floating_mul to "×",
                R.id.floating_div to "÷",
            )
        keys.forEach { (id, symbol) -> root.findViewById<Button>(id).setOnClickListener { onKey(symbol) } }
        // Backspace: tap deletes one character, long-press clears the
        // whole expression. Replaces a separate C key.
        val backspace = root.findViewById<Button>(R.id.floating_back)
        backspace.setOnClickListener { edit(expression.dropLast(1)) }
        backspace.setOnLongClickListener {
            edit("")
            true
        }
        root.findViewById<Button>(R.id.floating_eq).setOnClickListener { onEquals() }
        root.findViewById<View>(R.id.floating_close).setOnClickListener { stopSelf() }
    }

    // ----- Calc state -----

    private fun onKey(symbol: String) = edit(ExpressionEditor.append(expression, symbol, afterEquals))

    private fun edit(next: String) {
        expression = next
        afterEquals = false
        errorText = null
        renderDisplay()
    }

    private fun onEquals() {
        if (expression.isBlank()) return
        val (toEvaluate, _) = ExpressionEditor.completeForEquals(expression)
        val evaluator = Evaluator(mathContext = MathContext(precision, RoundingMode.HALF_EVEN))
        when (val result = evaluator.evaluate(toEvaluate)) {
            is EvaluationResult.Success -> {
                expression = ExpressionEditor.resultText(result.value)
                afterEquals = true
            }
            is EvaluationResult.Error -> errorText = getString(errorMessage(result))
        }
        renderDisplay()
    }

    private fun renderDisplay() {
        val error = errorText
        displayText.text = error ?: expression.ifEmpty { "0" }
        // A sentence does not fit the narrow window at number size.
        displayText.setTextSize(TypedValue.COMPLEX_UNIT_PX, if (error != null) numberTextSize / 2 else numberTextSize)
    }

    // ----- Foreground notification -----

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANCE_LOW: silent / no heads-up; the notification is
            // here for the system requirement, not the user.
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                getString(R.string.floating_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            channel.setShowBadge(false)
            nm.createNotificationChannel(channel)
        }

        // Resolves to the app's launcher activity, so this feature does
        // not depend on the activity class.
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingCalculatorService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notif: Notification = NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_qs_calculator)
            .setContentTitle(getString(R.string.floating_notif_title))
            .setContentText(getString(R.string.floating_notif_body))
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.floating_notif_stop), stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }
    }

    companion object {
        const val ACTION_STOP = "com.calculator.feature.floating.action.STOP"
        private const val TAG = "FloatingCalculator"
        private const val NOTIFICATION_CHANNEL = "floating_calculator"
        private const val NOTIFICATION_ID = 1001
        private const val WIDTH_DP = 260
        private const val INITIAL_X_DP = 24
        private const val INITIAL_Y_DP = 120

        /**
         * Starts the overlay if the overlay-permission is granted;
         * otherwise launches the system settings page so the user can
         * grant it. Returns true if the start was kicked off, false if
         * the user needs to grant permission first.
         */
        fun startOrRequestPermission(context: Context): Boolean {
            if (!Settings.canDrawOverlays(context)) {
                val intent =
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        .setData(android.net.Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return false
            }
            context.startForegroundService(Intent(context, FloatingCalculatorService::class.java))
            return true
        }
    }
}

/** The message shown for an engine error. */
private fun errorMessage(error: EvaluationResult.Error): Int =
    when (error) {
        EvaluationResult.Error.DivisionByZero -> R.string.basic_error_division_by_zero
        is EvaluationResult.Error.Syntax -> R.string.basic_error_syntax
        is EvaluationResult.Error.UnknownToken -> R.string.basic_error_unknown_token
        is EvaluationResult.Error.Domain -> R.string.basic_error_domain
    }
