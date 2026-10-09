package com.jev.toolkit

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** External, read-only APK metadata inspector; never accesses game process memory. */
class FloatingOverlayService : Service() {
    companion object { const val ACTION_ANALYZE = "com.jev.toolkit.ANALYZE_OVERLAY"; const val EXTRA_APK = "apk_uri" }
    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var root: LinearLayout? = null
    private var result: TextView? = null
    private var search: EditText? = null
    private var currentUri: Uri? = null
    private var generation = 0
    private fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (root == null) createOverlay()
        if (intent?.action == ACTION_ANALYZE) {
            intent.getStringExtra(EXTRA_APK)?.let {
                currentUri = Uri.parse(it)
                runAnalysis()
            }
        }
        return START_STICKY
    }

    private fun label(text: String, size: Float = 14f) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.WHITE)
        setPadding(dp(8), dp(9), dp(8), dp(9))
    }

    private fun createOverlay() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(12); y = dp(130) }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(Color.argb(235, 23, 30, 41))
        }
        val bubble = label("JEV +", 18f)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val header = label("JEV | Offline IL2CPP Inspector", 15f)
        panel.addView(header)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val pick = label("Choose APK")
        val minimize = label("Minimize")
        actions.addView(pick)
        actions.addView(minimize)
        panel.addView(actions)
        val filter = EditText(this).apply {
            hint = "Search class / namespace"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine(true)
        }
        search = filter
        panel.addView(filter, LinearLayout.LayoutParams(dp(300), dp(48)))
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val scan = label("Search / Refresh")
        val done = label("Done typing")
        buttons.addView(scan)
        buttons.addView(done)
        panel.addView(buttons)
        val output = label("Choose an APK in JEV Toolkit. Results will appear here.", 12f)
        output.setTextIsSelectable(false)
        result = output
        val scroll = ScrollView(this).apply { addView(output) }
        panel.addView(scroll, LinearLayout.LayoutParams(dp(300), dp(240)))
        val footer = label("Static metadata only • No game memory access", 11f)
        footer.setTextColor(Color.LTGRAY)
        panel.addView(footer)
        val stop = label("Stop overlay")
        stop.setTextColor(Color.YELLOW)
        panel.addView(stop)

        fun setKeyboard(enabled: Boolean) {
            params.flags = if (enabled)
                params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            else params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            wm.updateViewLayout(container, params)
            if (enabled) {
                filter.requestFocus()
                filter.post {
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .showSoftInput(filter, InputMethodManager.SHOW_IMPLICIT)
                }
            } else {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(filter.windowToken, 0)
                filter.clearFocus()
            }
        }
        filter.setOnFocusChangeListener { _, focused -> if (focused && params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0) setKeyboard(true) }
        done.setOnClickListener { setKeyboard(false) }
        scan.setOnClickListener { setKeyboard(false); runAnalysis() }
        pick.setOnClickListener {
            setKeyboard(false)
            startActivity(Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_PICK_FOR_OVERLAY
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
        }
        minimize.setOnClickListener { setKeyboard(false); panel.visibility = View.GONE; bubble.text = "JEV +" }
        stop.setOnClickListener { stopSelf() }
        bubble.setOnClickListener {
            val show = panel.visibility != View.VISIBLE
            panel.visibility = if (show) View.VISIBLE else View.GONE
            bubble.text = if (show) "JEV −" else "JEV +"
        }
        var startRawX = 0f
        var startRawY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        bubble.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startRawX = event.rawX; startRawY = event.rawY
                    startX = params.x; startY = params.y; moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startRawX).toInt()
                    val dy = (event.rawY - startRawY).toInt()
                    if (kotlin.math.abs(dx) > dp(8) || kotlin.math.abs(dy) > dp(8)) moved = true
                    if (moved) {
                        val bounds = resources.displayMetrics
                        params.x = (startX + dx).coerceIn(0, (bounds.widthPixels - dp(52)).coerceAtLeast(0))
                        params.y = (startY + dy).coerceIn(0, (bounds.heightPixels - dp(52)).coerceAtLeast(0))
                        wm.updateViewLayout(container, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!moved) v.performClick(); true }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        container.addView(bubble)
        container.addView(panel)
        root = container
        wm.addView(container, params)
    }

    private fun runAnalysis() {
        val uri = currentUri
        if (uri == null) {
            result?.text = "Choose an APK first."
            return
        }
        val request = ++generation
        val filter = search?.text?.toString().orEmpty()
        result?.text = "Analyzing APK offline..."
        Thread {
            val report = try { ApkAnalyzer.inspect(this, uri, filter) }
                catch (e: Exception) { "Cannot inspect APK: ${e.message ?: e.javaClass.simpleName}" }
            android.os.Handler(mainLooper).post {
                if (request == generation && root != null) result?.text = report
            }
        }.start()
    }

    override fun onDestroy() {
        generation++
        root?.let { wm.removeView(it) }
        root = null
        result = null
        search = null
        super.onDestroy()
    }
}
