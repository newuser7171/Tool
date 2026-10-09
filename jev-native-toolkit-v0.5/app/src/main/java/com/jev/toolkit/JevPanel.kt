package com.jev.toolkit

import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import kotlin.concurrent.thread

/** In-app floating button and panel. No overlay permission needed. */
class JevPanel(private val context: Context) {
    private var panel: LinearLayout? = null
    private var button: Button? = null
    /** Attach to your own Activity window using the Activity decor view instead of a system overlay. */
    fun attach(root: android.view.ViewGroup) {
        if (button != null) return
        val toggle = Button(context).apply { text = "JEV" }
        val pane = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE20242A.toInt())
            visibility = android.view.View.GONE
        }
        val report = TextView(context).apply { setTextColor(-1); text = "Press Scan to inspect IL2CPP" }
        val scan = Button(context).apply { text = "Scan assemblies"; setOnClickListener {
            report.text = "Inspecting..."
            thread(name = "jev-assembly-scan") {
                val result = try { JevBridge.inspect() } catch (t: Throwable) { "Scan failed: ${t.message}" }
                Handler(Looper.getMainLooper()).post { report.text = result }
            }
        } }
        val filter = EditText(context).apply { hint = "Class / namespace / assembly filter"; setSingleLine(true) }
        val explore = Button(context).apply { text = "Explore IL2CPP classes"; setOnClickListener {
            val query = filter.text.toString()
            report.text = "Scanning IL2CPP metadata..."
            thread(name = "jev-il2cpp-scan") {
                val result = try { JevBridge.explore(query) } catch (t: Throwable) { "Scan failed: ${t.message}" }
                Handler(Looper.getMainLooper()).post { report.text = result }
            }
        } }
        val list = Button(context).apply { text = "Editable test fields"; setOnClickListener { report.text = JevBridge.variables() } }
        val key = EditText(context).apply { hint = "Registered field name"; setSingleLine(true) }
        val value = EditText(context).apply { hint = "New float value"; inputType = 8194 }
        val applyTyped = Button(context).apply { text = "Set registered int / bool"; setOnClickListener {
            report.text = if (JevBridge.setTyped(key.text.toString(), value.text.toString())) "Updated" else "Invalid or unregistered value"
        } }
        val apply = Button(context).apply { text = "Set registered field"; setOnClickListener {
            val v = value.text.toString().toFloatOrNull()
            report.text = if(v != null && JevBridge.setFloat(key.text.toString(),v)) "Updated" else "Not registered or out of range"
        } }
        pane.addView(scan); pane.addView(filter); pane.addView(explore); pane.addView(list); pane.addView(key); pane.addView(value); pane.addView(apply); pane.addView(applyTyped)
        val scroll = ScrollView(context).apply { addView(report) }
        val density = context.resources.displayMetrics.density
        pane.addView(scroll, LinearLayout.LayoutParams(
            (300 * density).toInt(), (260 * density).toInt()
        ))
        toggle.setOnClickListener { pane.visibility = if(pane.visibility == android.view.View.VISIBLE) android.view.View.GONE else android.view.View.VISIBLE }
        root.addView(toggle)
        root.addView(pane)
        button=toggle; panel=pane
    }
    fun detach(root: android.view.ViewGroup) {
        button?.let { root.removeView(it) }; panel?.let { root.removeView(it) }
        button=null; panel=null
    }
}
