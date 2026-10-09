package com.jev.toolkit

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class FloatingOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var view: LinearLayout? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (view != null) return START_STICKY
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 200 }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 14, 20, 14)
            setBackgroundColor(Color.argb(225, 24, 29, 40))
        }
        val toggle = TextView(this).apply {
            text = "JEV  +"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(10, 8, 10, 8)
        }
        val details = TextView(this).apply {
            text = "JEV overlay active\\nOpen JEV Toolkit to analyze APK files.\\nLive inspection requires in-process integration.\\nTap Stop to remove this overlay."
            textSize = 14f
            setTextColor(Color.WHITE)
            visibility = android.view.View.GONE
        }
        val stop = TextView(this).apply {
            text = "Stop overlay"
            textSize = 15f
            setTextColor(Color.YELLOW)
            setPadding(10, 14, 10, 8)
            visibility = android.view.View.GONE
            setOnClickListener { stopSelf() }
        }
        toggle.setOnClickListener {
            val show = details.visibility != android.view.View.VISIBLE
            details.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
            stop.visibility = details.visibility
            toggle.text = if (show) "JEV  -" else "JEV  +"
        }
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        toggle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (kotlin.math.abs(event.rawX-downX) > 12 || kotlin.math.abs(event.rawY-downY) > 12) {
                        params.x = startX + (event.rawX-downX).toInt()
                        params.y = startY + (event.rawY-downY).toInt()
                        wm.updateViewLayout(container, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (kotlin.math.abs(event.rawX-downX) < 12 && kotlin.math.abs(event.rawY-downY) < 12) toggle.performClick()
                    true
                }
                else -> false
            }
        }
        container.addView(toggle)
        container.addView(details)
        container.addView(stop)
        view = container
        wm.addView(container, params)
        return START_STICKY
    }
    override fun onDestroy() {
        view?.let { wm.removeView(it) }
        view = null
        super.onDestroy()
    }
}
