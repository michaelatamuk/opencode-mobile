package com.opencode.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
                        append(text.substring(i + 2, end))
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
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = inlineBg,
                            fontSize = 13.sp
                        )
                    ) {
                        append(text.substring(i + 1, end))
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
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseMarkdown(text) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Prose -> Text(
                    text = proseAnnotated(block.text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                is MdBlock.Code -> CodeBlock(block.lang, block.code)

                is MdBlock.Table -> TableBlock(block.rows)
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

// --------------------------------------------------------------- tool calls ---
@Composable
private fun statusColor(status: String): Color = when (status) {
    "completed" -> MaterialTheme.colorScheme.secondary
    "error" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun ToolCallCard(
    name: String,
    status: String,
    summary: String = "",
    input: String = "",
    output: String = "",
    error: String = "",
    modifier: Modifier = Modifier
) {
    val accent = statusColor(status)
    var expanded by remember { mutableStateOf(false) }
    val hasDetails = input.isNotBlank() || output.isNotBlank() || error.isNotBlank()
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.animateContentSize()
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (hasDetails) Modifier.clickable { expanded = !expanded } else Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PulsingDot(color = accent, pulsing = status == "running" || status == "streaming")
                Spacer(Modifier.width(9.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    status,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
                if (hasDetails) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp).rotate(chevron)
                    )
                }
            }
            if (summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (input.isNotBlank()) ToolDetail(stringResource(R.string.chat_input), input)
                    if (output.isNotBlank()) ToolDetail(stringResource(R.string.chat_output), output)
                    if (error.isNotBlank()) {
                        ToolDetail(stringResource(R.string.chat_error), error, MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun ReasoningBlock(
    text: String,
    durationMs: Long,
    pending: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "reasoningChevron")
    val label = when {
        pending -> stringResource(R.string.chat_thinking)
        durationMs >= 1000 -> stringResource(R.string.chat_thought_for, (durationMs / 1000).toInt())
        else -> stringResource(R.string.chat_thought)
    }

    Column(modifier) {
        Row(
            Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) stringResource(R.string.chat_hide_thoughts)
                else stringResource(R.string.chat_show_thoughts),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp).rotate(chevron)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 21.dp, top = 2.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun ToolDetail(label: String, text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                SelectionContainer {
                    Text(
                        text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = color,
                        modifier = Modifier.padding(10.dp)
                    )
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
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
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
    val delta = System.currentTimeMillis() - millis
    if (delta < 60_000) return stringResource(R.string.time_just_now)
    val minutes = delta / 60_000
    if (minutes < 60) return stringResource(R.string.time_minutes_ago, minutes.toInt())
    val hours = minutes / 60
    if (hours < 24) return stringResource(R.string.time_hours_ago, hours.toInt())
    val days = hours / 24
    if (days < 7) return stringResource(R.string.time_days_ago, days.toInt())
    return String.format(Locale.getDefault(), "%tY-%<tm-%<td", java.util.Date(millis))
}

fun formatCost(cost: Double): String =
    if (abs(cost) < 0.005) "" else String.format(Locale.getDefault(), "$%.2f", cost)
