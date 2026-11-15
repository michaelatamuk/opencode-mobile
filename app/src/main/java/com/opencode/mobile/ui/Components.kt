package com.opencode.mobile.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.opencode.mobile.R
import java.util.Locale
import kotlin.math.abs

// ---------------------------------------------------------------- markdown ---

private sealed interface MdBlock {
    data class Prose(val text: String) : MdBlock
    data class Code(val lang: String, val code: String) : MdBlock
    data class Table(val rows: List<List<String>>) : MdBlock
}

private fun looksLikeTableRow(line: String): Boolean =
    line.trimStart().startsWith("|") && line.trimEnd().endsWith("|")

private fun splitRow(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

private fun isTableSeparator(cell: String): Boolean =
    cell.isNotEmpty() && cell.contains('-') && cell.all { it == '-' || it == ':' || it == ' ' }

private fun parseMarkdown(src: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = src.lines()
    val prose = StringBuilder()
    var i = 0

    fun flushProse() {
        val text = prose.toString().trim('\n')
        if (text.isNotBlank()) blocks += MdBlock.Prose(text)
        prose.setLength(0)
    }

    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("```")) {
            flushProse()
            val lang = line.trim().removePrefix("```").trim()
            i++
            val code = StringBuilder()
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                code.appendLine(lines[i])
                i++
            }
            if (i < lines.size) i++
            blocks += MdBlock.Code(lang, code.toString().trimEnd())
        } else if (looksLikeTableRow(line)) {
            flushProse()
            val tableLines = mutableListOf<String>()
            while (i < lines.size && looksLikeTableRow(lines[i])) {
                tableLines += lines[i]
                i++
            }
            val rows = tableLines
                .map { splitRow(it) }
                .filterIndexed { index, cells ->
                    !(index == 1 && cells.isNotEmpty() && cells.all { isTableSeparator(it) })
                }
            if (rows.isNotEmpty()) blocks += MdBlock.Table(rows)
        } else {
            prose.appendLine(line)
            i++
        }
    }
    flushProse()
    return blocks
}

private fun AnnotatedString.Builder.appendInline(text: String, inlineBg: Color, linkColor: Color) {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        // Keep parsing inside bold: URLs and inline code inside
                        // **…** must stay tappable.
                        appendInline(text.substring(i + 2, end), inlineBg, linkColor)
                    }
                    i = end + 2
                } else {
                    append("**")
                    i += 2
                }
            }

            text[i] == '[' -> {
                val close = text.indexOf("](", i + 1)
                val end = if (close > i) text.indexOf(')', close + 2) else -1
                if (close > i && end > close) {
                    val label = text.substring(i + 1, close)
                    val url = text.substring(close + 2, end).trim()
                    withLink(LinkAnnotation.Url(url, linkStyles(linkColor))) {
                        append(label.ifBlank { url })
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }

            text.startsWith("http://", i) || text.startsWith("https://", i) -> {
                var end = i
                while (end < text.length && !text[end].isWhitespace()) end++
                val raw = text.substring(i, end)
                val url = raw.trimEnd('.', ',', ')', ']', '"', '\'', '>')
                withLink(LinkAnnotation.Url(url, linkStyles(linkColor))) {
                    append(url)
                }
                i += url.length
                if (url.length < raw.length) {
                    append(raw.substring(url.length))
                    i += raw.length - url.length
                }
            }

            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    val inner = text.substring(i + 1, end)
                    val url = inner.trimEnd('.', ',', ')', ']', '"', '\'', '>')
                    val isUrl = (inner.startsWith("http://") || inner.startsWith("https://")) &&
                        inner.none { it.isWhitespace() }
                    if (isUrl) {
                        // A URL wrapped in backticks is still a URL: keep the code
                        // look but make it tappable.
                        withLink(LinkAnnotation.Url(url, linkStyles(linkColor))) {
                            appendInlineCode(url, inlineBg)
                        }
                        if (url.length < inner.length) {
                            appendInlineCode(inner.substring(url.length), inlineBg)
                        }
                    } else {
                        appendInlineCode(inner, inlineBg)
                    }
                    i = end + 1
                } else {
                    append('`')
                    i++
                }
            }

            else -> {
                append(text[i])
                i++
            }
        }
    }
}

private fun linkStyles(color: Color) = TextLinkStyles(
    style = SpanStyle(color = color, textDecoration = TextDecoration.Underline)
)

/**
 * Inline code shares the body font size (13sp inside 18sp body used to jump the
 * baseline) and gets hair-space padding so the highlight does not look like a
 * stray gray rectangle.
 */
private fun AnnotatedString.Builder.appendInlineCode(text: String, background: Color) {
    withStyle(
        SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = background,
            letterSpacing = 0.2.sp
        )
    ) {
        append('\u2009')
        append(text)
        append('\u2009')
    }
}

@Composable
private fun proseAnnotated(src: String): AnnotatedString {
    val inlineBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val linkColor = MaterialTheme.colorScheme.primary
    return remember(src, inlineBg, linkColor) {
        buildAnnotatedString {
            val headingRegex = Regex("^(#{1,6})\\s+")
            src.lines().forEachIndexed { index, raw ->
                if (index > 0) append('\n')
                val heading = headingRegex.find(raw)
                if (heading != null) {
                    val level = heading.groupValues[1].length
                    val body = raw.substring(heading.range.last + 1)
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = when (level) {
                                1 -> 17.sp
                                2 -> 16.sp
                                else -> 15.sp
                            }
                        )
                    ) { appendInline(body, inlineBg, linkColor) }
                } else {
                    val line = when {
                        raw.startsWith("- ") || raw.startsWith("* ") -> "• " + raw.substring(2)
                        else -> raw
                    }
                    appendInline(line, inlineBg, linkColor)
                }
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, contentAlpha: Float = 1f) {
    val blocks = remember(text) { parseMarkdown(text) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Prose -> Text(
                    text = proseAnnotated(block.text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )

                is MdBlock.Code -> if (block.lang.equals("mermaid", ignoreCase = true)) {
                    MermaidBlock(block.code)
                } else {
                    CodeBlock(block.lang, block.code)
                }

                is MdBlock.Table -> TableBlock(block.rows)
            }
        }
    }
}

@Composable
private fun MermaidBlock(code: String) {
    var open by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "MERMAID",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { open = true }) {
                    Text(
                        stringResource(R.string.chat_view_diagram),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text(
                code,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            )
        }
    }
    if (open) {
        MermaidViewer(code = code, onDismiss = { open = false })
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MermaidViewer(code: String, onDismiss: () -> Unit) {
    val dark = isSystemInDarkTheme()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.setSupportZoom(true)
                            val escaped = code
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                            val background = if (dark) "#0B1020" else "#FFFFFF"
                            val theme = if (dark) "dark" else "default"
                            val html = """
                                <!doctype html>
                                <html><head>
                                <meta name="viewport" content="width=device-width, initial-scale=1">
                                <script src="mermaid.min.js"></script>
                                <style>
                                  body { margin: 0; padding: 16px; background: $background; }
                                  .mermaid { display: flex; justify-content: center; }
                                </style>
                                </head><body>
                                <pre class="mermaid">$escaped</pre>
                                <script>
                                  mermaid.initialize({
                                    startOnLoad: true,
                                    securityLevel: 'strict',
                                    theme: '$theme'
                                  });
                                </script>
                                </body></html>
                            """.trimIndent()
                            loadDataWithBaseURL(
                                "file:///android_asset/",
                                html,
                                "text/html",
                                "utf-8",
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                FilledTonalIconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlock(lang: String, code: String) {
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    lang.ifBlank { stringResource(R.string.chat_code_label) }.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.chat_copy_code),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 2.dp, bottom = 14.dp)
                )
            }
        }
    }
}

@Composable
fun TableBlock(rows: List<List<String>>) {
    val columns = rows.maxOfOrNull { it.size } ?: return
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            rows.forEachIndexed { rowIndex, row ->
                val header = rowIndex == 0
                Row(
                    Modifier.background(
                        if (header) MaterialTheme.colorScheme.surfaceContainerHigh
                        else Color.Transparent
                    )
                ) {
                    repeat(columns) { column ->
                        Box(
                            Modifier
                                .width(150.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                proseAnnotated(row.getOrElse(column) { "" }),
                                style = if (header) MaterialTheme.typography.labelLarge
                                else MaterialTheme.typography.bodySmall,
                                color = if (header) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (rowIndex < rows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun PulsingDot(color: Color, pulsing: Boolean, size: Int = 7) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "dot")
        val value by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotAlpha"
        )
        value
    } else {
        1f
    }
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ------------------------------------------------------------------ shimmer ---

@Composable
fun ShimmerBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "shift"
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(shift, 0f),
                    end = Offset(shift + 320f, 220f)
                )
            )
    )
}

@Composable
fun SessionSkeleton() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShimmerBlock(Modifier.fillMaxWidth(0.62f).height(16.dp))
        ShimmerBlock(Modifier.fillMaxWidth(0.34f).height(10.dp))
    }
}

// -------------------------------------------------------------------- misc ---

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun relativeTime(millis: Long): String {
    if (millis <= 0L) return ""
    val zone = java.time.ZoneId.systemDefault()
    val at = java.time.Instant.ofEpochMilli(millis).atZone(zone)
    val today = java.time.LocalDate.now(zone)
    val day = at.toLocalDate()
    val locale = Locale.getDefault()
    return when {
        day == today -> at.format(java.time.format.DateTimeFormatter.ofLocalizedTime(
            java.time.format.FormatStyle.SHORT
        ))
        day == today.minusDays(1) -> stringResource(R.string.time_yesterday)
        day.isAfter(today.minusDays(7)) -> at.format(
            java.time.format.DateTimeFormatter.ofPattern("EEE", locale)
        )
        else -> at.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", locale))
    }
}

fun formatCost(cost: Double): String =
    if (abs(cost) < 0.005) "" else String.format(Locale.getDefault(), "$%.2f", cost)
