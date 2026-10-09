package com.jev.toolkit

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** External overlay: never accesses another app's private memory. */
class FloatingOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private lateinit var params: WindowManager.LayoutParams
    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (root != null) return START_STICKY
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(12); y = dp(130) }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.argb(235, 23, 30, 41))
        }
        fun item(label: String, size: Float = 14f): TextView = TextView(this).apply {
            text = label
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(9), dp(10), dp(9))
        }
        val toggle = item("JEV  +", 18f)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val status = item("External mode: offline APK inspection. No access to other apps' memory.", 12f)
        status.setTextColor(Color.LTGRAY)
        panel.addView(status)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val open = item("Open Toolkit")
        val hide = item("Hide")
        actions.addView(open)
        actions.addView(hide)
        panel.addView(actions)
        val scroll = ScrollView(this)
        val explanation = item(
            "Use JEV Toolkit to select a Unity APK and explore its metadata. " +
            "This overlay stays above games without changing them.", 13f
        )
        scroll.addView(explanation)
        panel.addView(scroll, LinearLayout.LayoutParams(dp(300), dp(120)))
        val stop = item("Stop overlay")
        stop.setTextColor(Color.YELLOW)
        panel.addView(stop)
        open.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }
        hide.setOnClickListener { panel.visibility = View.GONE; toggle.text = "JEV  +" }
        stop.setOnClickListener { stopSelf() }
        toggle.setOnClickListener {
            val expanded = panel.visibility != View.VISIBLE
            panel.visibility = if (expanded) View.VISIBLE else View.GONE
            toggle.text = if (expanded) "JEV  −" else "JEV  +"
        }
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        toggle.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (kotlin.math.abs(dx) > dp(8) || kotlin.math.abs(dy) > dp(8)) dragging = true
                    if (dragging) {
                        val bounds = resources.displayMetrics
                        params.x = (startX + dx.toInt()).coerceIn(0, (bounds.widthPixels - dp(52)).coerceAtLeast(0))
                        params.y = (startY + dy.toInt()).coerceIn(0, (bounds.heightPixels - dp(52)).coerceAtLeast(0))
                        wm.updateViewLayout(container, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!dragging) v.performClick(); true }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        container.addView(toggle)
        container.addView(panel)
        root = container
        wm.addView(container, params)
        return START_STICKY
    }

    override fun onDestroy() {
        root?.let { wm.removeView(it) }
        root = null
        super.onDestroy()
    }
}
