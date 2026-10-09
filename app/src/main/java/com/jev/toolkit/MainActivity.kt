package com.jev.toolkit

import android.app.Activity
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.content.Intent
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import android.view.Gravity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : Activity() {
    companion object {
        const val ACTION_PICK_FOR_OVERLAY = "com.jev.toolkit.PICK_FOR_OVERLAY"
        init { System.loadLibrary("jevtool") }
    }
    private var pickForOverlay = false
    private val apkPickerRequest = 701
    private lateinit var surface: GLSurfaceView
    private lateinit var root: FrameLayout
    private val panel by lazy { JevPanel(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        surface = object : GLSurfaceView(this) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                val action = event.actionMasked
                val x = event.x
                val y = event.y
                queueEvent { nativeTouch(x, y, action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) }
                return true
            }
        }
        surface.setEGLContextClientVersion(3)
        surface.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) { nativeInit() }
            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) { nativeResize(width, height) }
            override fun onDrawFrame(gl: GL10?) { nativeFrame() }
        })
        root.addView(surface)
        val hint = TextView(this).apply { text = "JEV Toolkit • Tap JEV for native scan"; setTextColor(-1); gravity = Gravity.TOP }
        root.addView(hint)
        val overlayControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val startOverlay = Button(this).apply {
            text = "Floating JEV"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                } else {
                    startService(Intent(this@MainActivity, FloatingOverlayService::class.java))
                }
            }
        }
        val stopOverlay = Button(this).apply {
            text = "Stop overlay"
            setOnClickListener {
                stopService(Intent(this@MainActivity, FloatingOverlayService::class.java))
            }
        }
        val testOverlay = Button(this).apply {
            text = "Test native menu"
            setOnClickListener { startActivity(Intent(this@MainActivity, OverlayTestActivity::class.java)) }
        }
        overlayControls.addView(testOverlay)
        overlayControls.addView(startOverlay)
        overlayControls.addView(stopOverlay)
        root.addView(overlayControls, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ))
        panel.attach(root) { openApkPicker() }
        if (intent?.action == ACTION_PICK_FOR_OVERLAY) {
            pickForOverlay = true
            root.post { openApkPicker() }
        }
        setContentView(root)
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_PICK_FOR_OVERLAY) {
            pickForOverlay = true
            openApkPicker()
        }
    }
    private fun openApkPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, apkPickerRequest)
    }
    @Deprecated("Legacy activity result for compatibility with API 26")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == apkPickerRequest && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            if (pickForOverlay) {
                pickForOverlay = false
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                catch (_: SecurityException) { }
                startService(Intent(this, FloatingOverlayService::class.java).apply {
                    action = FloatingOverlayService.ACTION_ANALYZE
                    putExtra(FloatingOverlayService.EXTRA_APK, uri.toString())
                })
            } else panel.analyzeApk(uri)
        }
    }
    override fun onResume() { super.onResume(); surface.onResume() }
    override fun onPause() { surface.onPause(); super.onPause() }
    override fun onDestroy() { panel.detach(root); super.onDestroy() }
    private external fun nativeInit()
    private external fun nativeResize(w: Int, h: Int)
    private external fun nativeTouch(x: Float, y: Float, down: Boolean)
    private external fun nativeFrame()
}
