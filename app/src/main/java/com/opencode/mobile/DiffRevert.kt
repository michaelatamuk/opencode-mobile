package com.opencode.mobile

/**
 * Reverses one file's unified diff against its current contents.
 *
 * Returns null when any hunk does not match, so callers never write a partial
 * revert. Text only; binary patches are rejected upstream.
 */
object DiffRevert {

    private data class Hunk(
        val newStart: Int,
        val oldLines: List<String>,
        val newLines: List<String>
    )

    fun revert(current: String, patch: String): String? {
        val hunks = parse(patch)
        if (hunks.isEmpty()) return null

        val eol = if (current.contains("\r\n")) "\r\n" else "\n"
        val endsWithNewline = current.endsWith("\n")
        val normalized = current.replace("\r\n", "\n")

        val lines = if (normalized.isEmpty()) {
            mutableListOf()
        } else {
            normalized.removeSuffix("\n").split("\n").toMutableList()
        }

        var offset = 0
        for (hunk in hunks) {
            val start = hunk.newStart - 1 + offset
            if (start < 0 || start + hunk.newLines.size > lines.size) return null
            for (i in hunk.newLines.indices) {
                if (lines[start + i] != hunk.newLines[i]) return null
            }
            repeat(hunk.newLines.size) { lines.removeAt(start) }
            lines.addAll(start, hunk.oldLines)
            offset += hunk.oldLines.size - hunk.newLines.size
        }

        val joined = lines.joinToString("\n")
        val restored = if (endsWithNewline && joined.isNotEmpty()) joined + "\n" else joined
        return if (eol == "\r\n") restored.replace("\n", "\r\n") else restored
    }

    private fun parse(patch: String): List<Hunk> {
        val hunks = mutableListOf<Hunk>()
        var newStart = -1
        var oldLines = mutableListOf<String>()
        var newLines = mutableListOf<String>()

        fun flush() {
            if (newStart >= 0) {
                hunks += Hunk(newStart, oldLines.toList(), newLines.toList())
            }
            oldLines = mutableListOf()
            newLines = mutableListOf()
        }

        patch.lineSequence().forEach { line ->
            when {
                line.startsWith("@@") -> {
                    flush()
                    newStart = Regex("^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@")
                        .find(line)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()
                        ?: -1
                }

                line.startsWith("+++") || line.startsWith("---") ||
                    line.startsWith("diff ") || line.startsWith("index ") -> Unit

                line.startsWith("\\") -> Unit

                line.startsWith("+") -> newLines += line.substring(1)

                line.startsWith("-") -> oldLines += line.substring(1)

                line.startsWith(" ") -> {
                    val text = line.substring(1)
                    oldLines += text
                    newLines += text
                }

                // Unified diffs sometimes drop the leading space on blank context lines.
                line.isEmpty() -> {
                    oldLines += ""
                    newLines += ""
                }
            }
        }
        flush()
        return hunks
    }
}
