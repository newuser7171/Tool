package com.jev.toolkit

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Read-only parser for standard Unity IL2CPP global-metadata.dat files.
 * Supports common v24-v29 layouts using validated record-size detection.
 * Encrypted/modified metadata and signatures reconstructed from libil2cpp
 * are intentionally not guessed.
 */
object MetadataExplorer {
    private data class Table(val offset: Int, val size: Int) {
        fun count(stride: Int) = size / stride
        fun valid(total: Int) = offset >= 0 && size >= 0 &&
            offset.toLong() + size <= total.toLong()
    }
    private data class Layout(val typeStride: Int, val methodStride: Int, val fieldStride: Int)
    private data class TypeInfo(val name: String, val namespace: String,
                                val methodStart: Int, val methodCount: Int,
                                val fieldStart: Int, val fieldCount: Int)

    private fun fallbackStrings(
        bytes: ByteArray, strings: Table, version: Int, filter: String, reason: String
    ): String {
        if (!strings.valid(bytes.size)) return "Metadata v$version: invalid string table"
        val found = ArrayList<String>()
        val needle = filter.trim()
        val end = strings.offset + strings.size
        var start = strings.offset
        var cursor = start
        while (cursor < end && found.size < 150) {
            if (bytes[cursor].toInt() == 0) {
                val len = cursor - start
                if (len in 3..160) {
                    val candidate = String(bytes, start, len, Charsets.UTF_8)
                    if (candidate.all { it.isLetterOrDigit() || it in "._+<>/\u0060" } &&
                        candidate.any { it.isLetter() } &&
                        (needle.isEmpty() || candidate.contains(needle, true))) {
                        found.add(candidate)
                    }
                }
                start = cursor + 1
            }
            cursor++
        }
        return buildString {
            appendLine("IL2CPP metadata v$version")
            appendLine("Compatibility fallback: $reason")
            appendLine("Showing raw metadata strings, NOT verified class/method/field definitions.")
            found.forEach { appendLine(it) }
            if (found.isEmpty()) appendLine("No matching readable strings")
        }
    }

    fun explore(bytes: ByteArray, filter: String = "", maxTypes: Int = 150): String {
        if (bytes.size < 200) return "Metadata is too short for type definitions"
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun int(at: Int): Int = b.getInt(at)
        if (int(0) != 0xFAB11BAF.toInt()) return "Invalid IL2CPP metadata signature"
        val version = int(4)
        if (version !in 16..40) return "Unrecognized metadata version: $version"
        fun table(at: Int): Table = Table(int(at), int(at + 4))
        val strings = table(24)
        val methods = table(48)
        val fields = table(96)
        val types = table(160)
        if (listOf(strings, methods, fields, types).any { !it.valid(bytes.size) }) {
            return "Invalid metadata table bounds"
        }
        if (types.size == 0) return fallbackStrings(bytes, strings, version, filter, "No type definitions found")
        fun stringAt(index: Int): String? {
            if (index < 0 || index >= strings.size) return null
            val start = strings.offset + index
            var end = start
            val limit = minOf(strings.offset + strings.size, start + 256)
            while (end < limit && bytes[end].toInt() != 0) end++
            if (end == limit) return null
            return String(bytes, start, end - start, Charsets.UTF_8)
                .takeIf { it.none { c -> c.isISOControl() } }
        }
        fun readTypes(stride: Int): List<TypeInfo>? {
            if (types.size % stride != 0) return null
            val n = types.count(stride)
            if (n == 0 || n > 500000) return null
            val result = ArrayList<TypeInfo>(minOf(n, 20000))
            var validNames = 0
            for (i in 0 until n) {
                val base = types.offset + i * stride
                val name = stringAt(int(base)) ?: return null
                val ns = stringAt(int(base + 4)) ?: return null
                val fieldStart = int(base + 40)
                val methodStart = int(base + 44)
                val methodCount = b.getShort(base + 72).toInt() and 0xffff
                val fieldCount = b.getShort(base + 76).toInt() and 0xffff
                if (fieldStart < 0 || methodStart < 0 ||
                    fieldStart.toLong() + fieldCount > 10000000L ||
                    methodStart.toLong() + methodCount > 10000000L) return null
                if (name.isNotBlank()) validNames++
                result.add(TypeInfo(name, ns, methodStart, methodCount, fieldStart, fieldCount))
            }
            return if (validNames >= n / 2) result else null
        }
        val layouts = (88..128 step 4).flatMap { typeStride ->
            listOf(28, 32, 36, 40, 44, 48, 52, 56).flatMap { methodStride ->
                listOf(12, 16, 20, 24).map { fieldStride ->
                    Layout(typeStride, methodStride, fieldStride)
                }
            }
        }
        val selected = layouts.firstNotNullOfOrNull { layout ->
            val ts = readTypes(layout.typeStride) ?: return@firstNotNullOfOrNull null
            if (methods.size % layout.methodStride != 0 ||
                fields.size % layout.fieldStride != 0) return@firstNotNullOfOrNull null
            val methodTotal = methods.count(layout.methodStride)
            val fieldTotal = fields.count(layout.fieldStride)
            if (ts.any { it.methodStart.toLong() + it.methodCount > methodTotal ||
                         it.fieldStart.toLong() + it.fieldCount > fieldTotal }) {
                return@firstNotNullOfOrNull null
            }
            Pair(layout, ts)
        } ?: return fallbackStrings(bytes, strings, version, filter, "Type layout not recognized")

        val (layout, definitions) = selected
        val needle = filter.trim()
        val matches = definitions.filter {
            needle.isEmpty() || it.name.contains(needle, true) ||
                it.namespace.contains(needle, true)
        }
        return buildString {
            appendLine("IL2CPP metadata v$version")
            appendLine("Types: ${definitions.size}")
            appendLine("Methods: ${methods.count(layout.methodStride)}")
            appendLine("Fields: ${fields.count(layout.fieldStride)}")
            appendLine("Matches: ${matches.size}")
            appendLine("Showing up to $maxTypes types")
            appendLine()
            for (type in matches.take(maxTypes)) {
                appendLine("class ${if (type.namespace.isEmpty()) "" else type.namespace + "."}${type.name}")
                for (i in 0 until minOf(type.fieldCount, 60)) {
                    val base = fields.offset + (type.fieldStart + i) * layout.fieldStride
                    val name = stringAt(int(base)) ?: "<unknown>"
                    appendLine("  field $name")
                }
                if (type.fieldCount > 60) appendLine("  ... more fields")
                for (i in 0 until minOf(type.methodCount, 80)) {
                    val base = methods.offset + (type.methodStart + i) * layout.methodStride
                    val name = stringAt(int(base)) ?: "<unknown>"
                    val params = b.getShort(base + 30).toInt() and 0xffff
                    appendLine("  method $name (parameter count: $params)")
                }
                if (type.methodCount > 80) appendLine("  ... more methods")
                appendLine()
            }
            if (matches.size > maxTypes) appendLine("Filter to narrow results.")
            appendLine("Names/counts come from metadata; runtime types, offsets and signatures require libil2cpp.")
        }
    }
}
