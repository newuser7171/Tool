package com.jev.toolkit

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** Exercises the same host-driven overlay API used by Unity integrations. */
class OverlayTestActivity : Activity() {
    private lateinit var surface: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surface = object : GLSurfaceView(this) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                val x = event.x
                val y = event.y
                val down = when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> true
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> false
                    else -> return true
                }
                queueEvent { nativeOverlayTouch(x, y, down) }
                return true
            }
        }
        surface.setEGLContextClientVersion(3)
        surface.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                nativeOverlayInit()
            }
            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                nativeOverlayResize(width, height)
            }
            override fun onDrawFrame(gl: GL10?) {
                nativeOverlayFrame()
            }
        })
        setContentView(surface)
    }

    override fun onResume() { super.onResume(); surface.onResume() }
    override fun onPause() { surface.onPause(); super.onPause() }

    private external fun nativeOverlayInit()
    private external fun nativeOverlayResize(w: Int, h: Int)
    private external fun nativeOverlayTouch(x: Float, y: Float, down: Boolean)
    private external fun nativeOverlayFrame()

    companion object {
        init { System.loadLibrary("jevtool") }
    }
}
