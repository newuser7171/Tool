package com.jev.toolkit

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** A host-owned view overlay; no special overlay permission required. */
class JevHostPanel(private val activity: Activity) {
    private var root: FrameLayout? = null

    fun attach(container: FrameLayout) {
        if (root != null) return
        val layer = FrameLayout(activity)
        layer.isClickable = false
        container.addView(layer, FrameLayout.LayoutParams(-1, -1))
        root = layer

        val menu = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(238, 22, 27, 36))
            visibility = View.GONE
            setPadding(20, 16, 20, 16)
        }
        val params = FrameLayout.LayoutParams(dp(300), dp(290), Gravity.TOP or Gravity.START)
        params.leftMargin = dp(12)
        params.topMargin = dp(135)
        layer.addView(menu, params)

        val title = TextView(activity).apply {
            text = "JEV • Host Inspector"
            textSize = 18f
            setTextColor(Color.WHITE)
        }
        menu.addView(title)

        val report = TextView(activity).apply {
            text = "This panel runs inside the host app process."
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(16), 0, 0)
        }
        menu.addView(report)

        val scan = TextView(activity).apply {
            text = "SCAN HOST"
            textSize = 15f
            setTextColor(Color.CYAN)
            setPadding(0, dp(16), 0, dp(16))
        }
        menu.addView(scan)
        scan.setOnClickListener {
            report.text = "Scanning..."
            Thread {
                val value = try { JevBridge.inspect() } catch (e: Throwable) {
                    "Inspector unavailable: ${e.javaClass.simpleName}"
                }
                activity.runOnUiThread { if (!activity.isFinishing) report.text = value }
            }.start()
        }

        val launcher = TextView(activity).apply {
            text = "JEV"
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.rgb(92, 216, 183))
            gravity = Gravity.CENTER
        }
        val launchParams = FrameLayout.LayoutParams(dp(60), dp(48), Gravity.TOP or Gravity.START)
        launchParams.leftMargin = dp(12)
        launchParams.topMargin = dp(75)
        layer.addView(launcher, launchParams)
        launcher.setOnClickListener {
            menu.visibility = if (menu.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        launcher.setOnTouchListener(object : View.OnTouchListener {
            var x = 0f
            var y = 0f
            var left = 0
            var top = 0
            var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                val lp = v.layoutParams as FrameLayout.LayoutParams
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        x = e.rawX; y = e.rawY
                        left = lp.leftMargin; top = lp.topMargin
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.rawX - x
                        val dy = e.rawY - y
                        if (kotlin.math.abs(dx) > dp(6) || kotlin.math.abs(dy) > dp(6)) moved = true
                        if (moved) {
                            lp.leftMargin = (left + dx.toInt()).coerceIn(0, (layer.width - v.width).coerceAtLeast(0))
                            lp.topMargin = (top + dy.toInt()).coerceIn(0, (layer.height - v.height).coerceAtLeast(0))
                            v.layoutParams = lp
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) v.performClick()
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> return true
                }
                return false
            }
        })
    }

    fun detach() {
        (root?.parent as? FrameLayout)?.removeView(root)
        root = null
    }

    private fun dp(v: Int): Int = (v * activity.resources.displayMetrics.density + .5f).toInt()
}
