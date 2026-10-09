package com.jev.toolkit

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.widget.*
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.text.TextWatcher
import android.text.Editable
import kotlin.concurrent.thread

/** In-app read-only APK explorer and separate local-process live tools. */
class JevPanel(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var pane: LinearLayout? = null
    private var toggle: Button? = null
    private var report: TextView? = null
    private var results: LinearLayout? = null
    private var search: EditText? = null
    private var metadata: ByteArray? = null
    private var lastReport = ""
    private var apkName = "APK"
    private var token = 0
    private var expanded = false
    private var currentTab = 0
    private var explorer: LinearLayout? = null
    private var live: LinearLayout? = null

    private fun button(title: String, action: () -> Unit) = Button(context).apply {
        text = title
        setOnClickListener { action() }
    }
    private fun column() = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private fun showTab(tab: Int) {
        currentTab = tab
        explorer?.visibility = if (tab == 0) View.VISIBLE else View.GONE
        live?.visibility = if (tab == 1) View.VISIBLE else View.GONE
    }
    fun attach(root: ViewGroup, openApkPicker: () -> Unit) {
        if (toggle != null) return
        val density = context.resources.displayMetrics.density
        val paneView = column().apply {
            setBackgroundColor(0xF020242A.toInt())
            visibility = View.GONE
            setPadding((8*density).toInt(), 0, (8*density).toInt(), 0)
        }
        val tabs = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.addView(button("OFFLINE APK") { showTab(0) }, LinearLayout.LayoutParams(0, -2, 1f))
        tabs.addView(button("LIVE / TEST") { showTab(1) }, LinearLayout.LayoutParams(0, -2, 1f))
        paneView.addView(tabs)
        val offline = column()
        offline.addView(button("ANALYZE APK FILE") { openApkPicker() })
        offline.addView(TextView(context).apply {
            text = "Offline analysis of selected APK; does not inspect a running game."
            setTextColor(-1)
        })
        val query = EditText(context).apply {
            hint = "Search class or namespace (all types)"
            setSingleLine(true)
        }
        search = query
        offline.addView(query)
        val resultsBox = column()
        results = resultsBox
        offline.addView(resultsBox)
        val output = TextView(context).apply {
            setTextColor(-1)
            text = "Choose an APK to inspect its IL2CPP metadata."
            setTextIsSelectable(true)
        }
        report = output
        offline.addView(button("SHARE REPORT (TEXT)") {
            if (lastReport.isNotBlank()) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "JEV Offline APK Report")
                    putExtra(Intent.EXTRA_TEXT, lastReport)
                }
                context.startActivity(Intent.createChooser(intent, "Export JEV report"))
            } else Toast.makeText(context, "Analyze an APK first", Toast.LENGTH_SHORT).show()
        })
        offline.addView(button("SHOW / HIDE APK REPORT") {
            expanded = !expanded
            output.visibility = if (expanded) View.VISIBLE else View.GONE
        })
        output.visibility = View.GONE
        offline.addView(output)
        explorer = offline

        val liveBox = column()
        liveBox.addView(TextView(context).apply {
            text = "Live tools inspect only this JEV process, not another app."
            setTextColor(-1)
        })
        val liveOutput = TextView(context).apply { setTextColor(-1); text = "No scan yet"; setTextIsSelectable(true) }
        liveBox.addView(button("SCAN ASSEMBLIES") {
            liveOutput.text = "Scanning..."
            thread {
                val r = try { JevBridge.inspect() } catch (e: Throwable) { "Scan failed: ${e.message}" }
                main.post { liveOutput.text = r }
            }
        })
        val liveQuery = EditText(context).apply { hint = "Live class filter"; setSingleLine(true) }
        liveBox.addView(liveQuery)
        liveBox.addView(button("EXPLORE LIVE IL2CPP") {
            val q = liveQuery.text.toString()
            thread {
                val r = try { JevBridge.explore(q) } catch (e: Throwable) { "Scan failed: ${e.message}" }
                main.post { liveOutput.text = r }
            }
        })
        liveBox.addView(button("EDITABLE TEST FIELDS") { liveOutput.text = JevBridge.variables() })
        val key = EditText(context).apply { hint = "Registered field name"; setSingleLine(true) }
        val value = EditText(context).apply { hint = "New value"; setSingleLine(true) }
        liveBox.addView(key)
        liveBox.addView(value)
        liveBox.addView(button("SET REGISTERED FLOAT") {
            val v = value.text.toString().toFloatOrNull()
            liveOutput.text = if (v != null && JevBridge.setFloat(key.text.toString(), v)) "Updated" else "Not registered or invalid"
        })
        liveBox.addView(button("SET REGISTERED INT / BOOL") {
            liveOutput.text = if (JevBridge.setTyped(key.text.toString(), value.text.toString())) "Updated" else "Not registered or invalid"
        })
        liveBox.addView(liveOutput)
        live = liveBox
        val scrollContent = column().apply { addView(offline); addView(liveBox) }
        paneView.addView(ScrollView(context).apply { addView(scrollContent) }, LinearLayout.LayoutParams(-1, 0, 1f))
        showTab(0)
        query.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refresh() }
            override fun afterTextChanged(s: Editable?) {}
        })
        val floating = button("JEV") {
            paneView.visibility = if (paneView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        val screen = context.resources.displayMetrics
        root.addView(paneView, android.widget.FrameLayout.LayoutParams(
            screen.widthPixels - (16*density).toInt(),
            screen.heightPixels - (176*density).toInt(),
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply { topMargin = (76*density).toInt() })
        root.addView(floating, android.widget.FrameLayout.LayoutParams(
            (88*density).toInt(), (52*density).toInt(), Gravity.TOP or Gravity.END
        ).apply { topMargin = (16*density).toInt(); marginEnd = (12*density).toInt() })
        toggle = floating
        pane = paneView
    }

    private fun refresh() {
        val data = metadata ?: return
        val target = results ?: return
        val query = search?.text?.toString().orEmpty()
        val generation = ++token
        target.removeAllViews()
        target.addView(TextView(context).apply { text = "Searching metadata..."; setTextColor(-1) })
        thread(name = "jev-metadata-filter") {
            val parsed = try { MetadataExplorer.explore(data, query, 150) }
                catch (e: Throwable) { "Explorer error: ${e.message}" }
            main.post {
                if (generation != token) return@post
                target.removeAllViews()
                val lines = parsed.lines()
                val firstClass = lines.indexOfFirst { it.startsWith("class ") }
                val summary = if (firstClass >= 0) lines.take(firstClass).joinToString("\n") else parsed
                target.addView(TextView(context).apply { text = summary; setTextColor(-1); setTextIsSelectable(true) })
                if (firstClass < 0) return@post
                var className: String? = null
                val members = ArrayList<String>()
                fun addClass() {
                    val name = className ?: return
                    val detail = members.joinToString("\n").ifEmpty { "No fields or methods" }
                    val entry = column()
                    val body = TextView(context).apply { text = detail; setTextColor(-1); visibility = View.GONE; setTextIsSelectable(true) }
                    entry.addView(button(name) { body.visibility = if (body.visibility == View.VISIBLE) View.GONE else View.VISIBLE })
                    entry.addView(body)
                    target.addView(entry)
                }
                for (line in lines.drop(firstClass)) {
                    if (line.startsWith("class ")) {
                        addClass()
                        className = line.removePrefix("class ")
                        members.clear()
                    } else if (line.startsWith("  ")) members.add(line.trim())
                }
                addClass()
            }
        }
    }
    fun analyzeApk(uri: Uri) {
        val generation = ++token
        metadata = null
        apkName = uri.lastPathSegment ?: "APK"
        report?.text = "Analyzing selected APK..."
        results?.removeAllViews()
        showTab(0)
        thread(name = "jev-apk-analyzer") {
            var found: ByteArray? = null
            val result = try {
                ApkAnalyzer.inspect(context, uri, "", onMetadata = { found = it })
            } catch (e: Throwable) { "APK analysis failed: ${e.message}" }
            main.post {
                if (generation != token) return@post
                metadata = found
                lastReport = result
                report?.text = result
                if (found == null) {
                    results?.addView(TextView(context).apply { text = result; setTextColor(-1) })
                } else refresh()
            }
        }
    }
    fun detach(root: ViewGroup) {
        token++
        toggle?.let { root.removeView(it) }
        pane?.let { root.removeView(it) }
        toggle = null
        pane = null
        metadata = null
    }
}
