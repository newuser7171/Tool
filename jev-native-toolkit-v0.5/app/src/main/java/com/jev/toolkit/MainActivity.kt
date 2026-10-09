package com.jev.toolkit

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import android.view.Gravity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : Activity() {
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
        panel.attach(root)
        setContentView(root)
    }
    override fun onResume() { super.onResume(); surface.onResume() }
    override fun onPause() { surface.onPause(); super.onPause() }
    override fun onDestroy() { panel.detach(root); super.onDestroy() }
    private external fun nativeInit()
    private external fun nativeResize(w: Int, h: Int)
    private external fun nativeTouch(x: Float, y: Float, down: Boolean)
    private external fun nativeFrame()
    companion object { init { System.loadLibrary("jevtool") } }
}
