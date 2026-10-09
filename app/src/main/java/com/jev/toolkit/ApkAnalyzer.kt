package com.jev.toolkit

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream

/** Offline, read-only Unity APK inspection. Does not execute the selected APK. */
object ApkAnalyzer {
    private const val MAX_METADATA = 64 * 1024 * 1024
    private const val MAX_ENTRIES = 150000
    private const val MAGIC = 0xFAB11BAF.toInt()

    fun inspect(context: Context, uri: Uri, filter: String = ""): String {
        var count = 0
        var unity = false
        var il2cpp = false
        var mono = false
        var unreal = false
        var metadataFound = false
        var metadataResult = "Not found"
        val matches = mutableListOf<String>()
        val architectures = sortedSetOf<String>()
        val input = context.contentResolver.openInputStream(uri)
            ?: return "Cannot open the selected file"
        input.use { stream ->
            ZipInputStream(stream.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (++count > MAX_ENTRIES) return "APK rejected: too many entries"
                    val name = entry.name.replace('\\', '/')
                    val lower = name.lowercase()
                    if (lower.startsWith("lib/") && lower.endsWith(".so")) {
                        val arch = name.split('/').getOrNull(1)
                        if (arch != null) architectures.add(arch)
                    }
                    val relevant = when {
                        lower.endsWith("/libunity.so") -> { unity = true; true }
                        lower.endsWith("/libil2cpp.so") -> { il2cpp = true; true }
                        lower.endsWith("/libmono.so") || lower.endsWith("/libmonobdwgc-2.0.so") -> {
                            mono = true; true
                        }
                        lower.endsWith("/libue4.so") || lower.endsWith("/libunreal.so") -> {
                            unreal = true; true
                        }
                        lower.endsWith("/global-metadata.dat") -> {
                            metadataFound = true
                            metadataResult = if (entry.size > MAX_METADATA) {
                                "Too large to inspect safely"
                            } else {
                                val buffer = ByteArrayOutputStream()
                                val chunk = ByteArray(8192)
                                var total = 0
                                var oversized = false
                                while (true) {
                                    val n = zip.read(chunk)
                                    if (n < 0) break
                                    total += n
                                    if (total > MAX_METADATA) { oversized = true; break }
                                    buffer.write(chunk, 0, n)
                                }
                                if (oversized) "Too large to inspect safely"
                                else analyzeMetadata(buffer.toByteArray()) + "\n" + MetadataExplorer.explore(buffer.toByteArray(), filter)
                            }
                            true
                        }
                        else -> false
                    }
                    if (relevant && matches.size < 40) matches.add(name)
                    zip.closeEntry()
                }
            }
        }
        return buildString {
            appendLine("JEV Offline APK Report")
            appendLine("Entries: $count")
            appendLine("Architectures: ${architectures.joinToString().ifEmpty { "None detected" }}")
            appendLine("Unity: ${if (unity) "Yes" else "No"}")
            appendLine("IL2CPP: ${if (il2cpp) "Yes" else "No"}")
            appendLine("Mono: ${if (mono) "Yes" else "No"}")
            appendLine("Unreal: ${if (unreal) "Yes" else "No"}")
            appendLine("Metadata file: ${if (metadataFound) "Yes" else "No"}")
            appendLine()
            appendLine("Metadata details:")
            appendLine(metadataResult)
            appendLine("Matching files:")
            matches.forEach { appendLine(it) }
            appendLine()
            appendLine("Static file analysis only. Split APKs may require inspecting additional APK files.")
        }
    }

    private fun analyzeMetadata(bytes: ByteArray): String {
        if (bytes.size < 32) return "Metadata file is too small"
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (bb.int != MAGIC) return "Metadata header is not standard IL2CPP (may be encrypted or modified)"
        val version = bb.int
        if (version !in 16..40) return "Unsupported metadata version: $version"
        val stringOffset = bb.int
        val stringSize = bb.int
        if (stringOffset < 0 || stringSize < 0 ||
            stringOffset.toLong() + stringSize > bytes.size.toLong()) {
            return "Metadata v$version: string table bounds invalid"
        }
        // Header begins with the string-literal table. Actual metadata string
        // table is at header byte offsets 24 and 28.
        if (bytes.size < 32) return "Truncated metadata header"
        val namesOffset = bb.getInt(24)
        val namesSize = bb.getInt(28)
        if (namesOffset < 0 || namesSize < 0 ||
            namesOffset.toLong() + namesSize > bytes.size.toLong()) {
            return "Metadata v$version: names table bounds invalid"
        }
        val samples = mutableListOf<String>()
        var start = namesOffset
        val end = namesOffset + namesSize
        var pos = start
        while (pos < end && samples.size < 40) {
            if (bytes[pos].toInt() == 0) {
                val length = pos - start
                if (length in 4..100) {
                    val candidate = String(bytes, start, length, Charsets.UTF_8)
                    if (candidate.all { it.isLetterOrDigit() || it in "._`+<>" } &&
                        candidate.any { it.isLetter() }) samples.add(candidate)
                }
                start = pos + 1
            }
            pos++
        }
        return buildString {
            appendLine("IL2CPP metadata header valid")
            appendLine("Metadata version: $version")
            appendLine("Metadata size: ${bytes.size} bytes")
            appendLine("String table: $namesSize bytes")
            appendLine("Sample metadata strings (not verified class names):")
            samples.forEach { appendLine("  $it") }
        }
    }
}
