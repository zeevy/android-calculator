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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.calculator.MainActivity
import com.calculator.R
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Hosts the floating calculator overlay.
 *
 * The overlay is a single `WindowManager`-attached view; the service
 * exists only so the system keeps the window alive once the user
 * leaves the app. A drag handler on the header rewrites the
 * `WindowManager.LayoutParams.x`/`y`, and a tiny in-place evaluator
 * (operand / op / operand) drives the keypad - lighter than wiring
 * the full Compose calculator into a non-Activity context.
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

    // Mini calculator state. Desk-calculator style:
    //   - `current` is what the display is showing (the operand the user
    //     is currently editing, or the last computed result).
    //   - `pending`/`pendingOp` capture the left side of the binary op
    //     waiting for a right side.
    //   - `freshEntry` flags that the next digit should REPLACE the
    //     display instead of appending to it (typically true right
    //     after `=`, an operator press, or a clear).
    private var current: String = "0"
    private var pending: BigDecimal? = null
    private var pendingOp: Op? = null
    private var freshEntry: Boolean = true

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundCompat()
        addOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
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

    private fun addOverlay() {
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

        windowManager.addView(view, params)
        overlayView = view
        layoutParams = params

        displayText = view.findViewById(R.id.floating_display)
        wireDragging(view.findViewById(R.id.floating_header), params)
        wireButtons(view)
        renderDisplay()
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

    @Suppress("LongMethod")
    private fun wireButtons(root: View) {
        fun digit(id: Int, d: String) {
            root.findViewById<Button>(id).setOnClickListener { onDigit(d) }
        }
        digit(R.id.floating_0, "0"); digit(R.id.floating_1, "1")
        digit(R.id.floating_2, "2"); digit(R.id.floating_3, "3")
        digit(R.id.floating_4, "4"); digit(R.id.floating_5, "5")
        digit(R.id.floating_6, "6"); digit(R.id.floating_7, "7")
        digit(R.id.floating_8, "8"); digit(R.id.floating_9, "9")

        root.findViewById<Button>(R.id.floating_dot).setOnClickListener { onDot() }
        // Backspace: tap deletes one character, long-press clears the
        // whole expression. Replaces a separate C key.
        val backspace = root.findViewById<Button>(R.id.floating_back)
        backspace.setOnClickListener { onBackspace() }
        backspace.setOnLongClickListener {
            onClear()
            true
        }
        root.findViewById<Button>(R.id.floating_pct).setOnClickListener { onPercent() }
        root.findViewById<Button>(R.id.floating_add).setOnClickListener { onOp(Op.ADD) }
        root.findViewById<Button>(R.id.floating_sub).setOnClickListener { onOp(Op.SUB) }
        root.findViewById<Button>(R.id.floating_mul).setOnClickListener { onOp(Op.MUL) }
        root.findViewById<Button>(R.id.floating_div).setOnClickListener { onOp(Op.DIV) }
        root.findViewById<Button>(R.id.floating_eq).setOnClickListener { onEquals() }

        root.findViewById<View>(R.id.floating_close).setOnClickListener { stopSelf() }
    }

    // ----- Calc state -----

    private fun onDigit(d: String) {
        current = if (freshEntry || current == "0") d else current + d
        freshEntry = false
        renderDisplay()
    }

    private fun onDot() {
        if (freshEntry) {
            current = "0."
            freshEntry = false
        } else if (!current.contains('.')) {
            current = "$current."
        }
        renderDisplay()
    }

    private fun onBackspace() {
        current = if (current.length > 1) current.dropLast(1) else "0"
        if (current == "-") current = "0"
        renderDisplay()
    }

    private fun onClear() {
        current = "0"
        pending = null
        pendingOp = null
        freshEntry = true
        renderDisplay()
    }

    private fun onOp(op: Op) {
        commit()
        pendingOp = op
        freshEntry = true
        renderDisplay()
    }

    private fun onEquals() {
        commit()
        pendingOp = null
        freshEntry = true
        renderDisplay()
    }

    private fun onPercent() {
        val value = current.toBigDecimalOrNull() ?: return
        current = value.divide(BigDecimal(100), MathContext.DECIMAL64).stripTrailingZeros().toPlainString()
        freshEntry = true
        renderDisplay()
    }

    /** Apply pending + current → new pending; fold errors into "Err". */
    private fun commit() {
        val rhs = current.toBigDecimalOrNull() ?: return
        val lhs = pending
        val op = pendingOp
        val result = if (lhs == null || op == null) {
            rhs
        } else {
            applyOp(lhs, rhs, op) ?: run {
                current = "Err"
                pending = null
                return
            }
        }
        pending = result
        current = result.stripTrailingZeros().toPlainString()
    }

    private fun applyOp(lhs: BigDecimal, rhs: BigDecimal, op: Op): BigDecimal? =
        runCatching {
            when (op) {
                Op.ADD -> lhs.add(rhs, MathContext.DECIMAL64)
                Op.SUB -> lhs.subtract(rhs, MathContext.DECIMAL64)
                Op.MUL -> lhs.multiply(rhs, MathContext.DECIMAL64)
                // BigDecimal.divide throws on non-terminating decimals
                // without a MathContext; pass DECIMAL64 so 1/3 doesn't
                // raise ArithmeticException.
                Op.DIV ->
                    if (rhs.signum() == 0) null
                    else lhs.divide(rhs, MathContext.DECIMAL64)
            }
        }.getOrNull()

    private fun renderDisplay() {
        displayText.text = current
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

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingCalculatorService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notif: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
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

    private enum class Op { ADD, SUB, MUL, DIV }

    companion object {
        const val ACTION_STOP = "com.calculator.feature.floating.action.STOP"
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

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingCalculatorService::class.java))
        }
    }
}

@Suppress("unused")
private fun BigDecimal.scaleSafely() =
    this.setScale(MAX_FRACTION_DIGITS, RoundingMode.HALF_UP).stripTrailingZeros()

private const val MAX_FRACTION_DIGITS = 8
