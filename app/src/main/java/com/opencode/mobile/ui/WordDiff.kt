package com.opencode.mobile.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/** Word-level highlight of what changed between two answers. */
object WordDiff {

    fun mark(
        original: String,
        branch: String,
        removed: Color,
        added: Color
    ): Pair<AnnotatedString, AnnotatedString>? {
        val a = tokens(original)
        val b = tokens(branch)
        if (a.isEmpty() || b.isEmpty()) return null
        if (a.size > 1200 || b.size > 1200) return null
        if (a.size.toLong() * b.size > 1_400_000) return null

        val dp = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) {
            for (j in b.indices.reversed()) {
                dp[i][j] = if (a[i] == b[j]) {
                    dp[i + 1][j + 1] + 1
                } else {
                    maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        val commonA = BooleanArray(a.size)
        val commonB = BooleanArray(b.size)
        var i = 0
        var j = 0
        while (i < a.size && j < b.size) {
            if (a[i] == b[j]) {
                commonA[i] = true
                commonB[j] = true
                i++
                j++
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                i++
            } else {
                j++
            }
        }

        val left = buildAnnotatedString {
            a.forEachIndexed { index, token ->
                if (commonA[index]) append(token)
                else withStyle(SpanStyle(background = removed)) { append(token) }
            }
        }
        val right = buildAnnotatedString {
            b.forEachIndexed { index, token ->
                if (commonB[index]) append(token)
                else withStyle(SpanStyle(background = added)) { append(token) }
            }
        }
        return left to right
    }

    private fun tokens(text: String): List<String> =
        Regex("\\S+|\\s+").findAll(text).map { it.value }.toList()
}
