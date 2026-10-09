package com.jev.toolkit

import android.content.Context
import android.net.Uri
import java.util.zip.ZipInputStream

object ApkAnalyzer {
    fun inspect(context: Context, uri: Uri): String {
        val entries = mutableListOf<String>()
        var count = 0
        var metadataBytes = 0L
        var unity = false
        var il2cpp = false
        var mono = false
        var unreal = false
        var metadata = false
        val input = context.contentResolver.openInputStream(uri)
            ?: return "Unable to open selected file"
        input.use { stream ->
            ZipInputStream(stream).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    count++
                    if (count > 150000) return "Archive has too many entries"
                    val name = entry.name.replace('\\', '/')
                    val lower = name.lowercase()
                    if (lower.endsWith("/libunity.so")) unity = true
                    if (lower.endsWith("/libil2cpp.so")) il2cpp = true
                    if (lower.endsWith("/libmono.so")) mono = true
                    if (lower.endsWith("/libue4.so") || lower.endsWith("/libunreal.so")) unreal = true
                    if (lower.endsWith("/global-metadata.dat")) {
                        metadata = true
                        metadataBytes = entry.size
                    }
                    if (lower.endsWith("/libunity.so") ||
                        lower.endsWith("/libil2cpp.so") ||
                        lower.endsWith("/libmono.so") ||
                        lower.endsWith("/global-metadata.dat") ||
                        lower.endsWith("/libue4.so")) {
                        if (entries.size < 30) entries.add(name)
                    }
                    zip.closeEntry()
                }
            }
        }
        return buildString {
            appendLine("APK analysis complete")
            appendLine("Archive entries: $count")
            appendLine("Unity library: ${if (unity) "Found" else "Not found"}")
            appendLine("IL2CPP library: ${if (il2cpp) "Found" else "Not found"}")
            appendLine("Mono library: ${if (mono) "Found" else "Not found"}")
            appendLine("Unreal library: ${if (unreal) "Found" else "Not found"}")
            appendLine("IL2CPP metadata: ${if (metadata) "Found" else "Not found"}")
            if (metadataBytes >= 0 && metadata) appendLine("Metadata bytes: $metadataBytes (unknown if -1)")
            appendLine()
            appendLine("Relevant APK entries:")
            entries.forEach { appendLine(it) }
            appendLine()
            appendLine("This is static APK inspection, not live memory access.")
        }
    }
}
