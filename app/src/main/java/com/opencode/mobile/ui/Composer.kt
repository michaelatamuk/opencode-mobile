package com.opencode.mobile.ui

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.opencode.mobile.R

data class AttachmentPreview(val uri: Uri, val name: String, val mime: String = "")

/**
 * 2026 composer: one rounded surface, a growing text field on the same optical
 * baseline as its controls, thumbnails above, and a single primary action that
 * morphs between send and stop.
 */
@Composable
fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    attachments: List<AttachmentPreview>,
    busy: Boolean,
    onAttach: () -> Unit,
    onRemove: (AttachmentPreview) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    maxAttachments: Int = 8,
    status: (@Composable () -> Unit)? = null,
    contextFraction: Float = -1f,
    onContextClick: (() -> Unit)? = null,
    onVoice: (() -> Unit)? = null,
    onAttachFile: (() -> Unit)? = null,
    onCamera: (() -> Unit)? = null,
    delivery: String = "steer",
    onDeliveryChange: (String) -> Unit = {}
) {
    val hasContent = value.isNotBlank() || attachments.isNotEmpty()
    val primaryIsStop = busy && !hasContent
    val primaryIsMic = !busy && !hasContent && onVoice != null
    val queueing = busy && delivery == "queue"

    val sendScale by animateFloatAsState(
        targetValue = if (hasContent || busy) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "sendScale"
    )
    // Stop is the only red thing in the composer, so "interrupt" reads instantly.
    val actionColor by animateColorAsState(
        targetValue = if (primaryIsStop) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        animationSpec = tween(180),
        label = "actionColor"
    )
    val actionContent = if (primaryIsStop) MaterialTheme.colorScheme.onError
    else MaterialTheme.colorScheme.onPrimary

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.animateContentSize()) {
            // Context is a meter, not a chip: a hairline along the top edge of the input.
            if (contextFraction >= 0.5f) {
                val critical = contextFraction >= 0.85f
                val fill = (if (critical) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary).copy(alpha = 0.55f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clickable(
                            enabled = onContextClick != null,
                            onClick = { onContextClick?.invoke() }
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(contextFraction.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(fill)
                    )
                }
                if (critical) {
                    Text(
                        stringResource(
                            R.string.chat_context_low_space,
                            (contextFraction * 100).toInt()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { onContextClick?.invoke() }
                    )
                }
            }
            if (attachments.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${attachments.size}/$maxAttachments",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(attachments) { item ->
                            Box {
                                if (item.mime.startsWith("image/") || item.mime.isBlank()) {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    )
                                } else {
                                    Column(
                                        Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.InsertDriveFile,
                                            contentDescription = item.name,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            item.name.substringAfterLast('.').take(4).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(18.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .clickable { onRemove(item) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.chat_remove_attachment),
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live turn state lives with the input, not as a separate floating bubble.
            if (status != null) {
                status()
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var attachMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { attachMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.chat_attach),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = attachMenu,
                        onDismissRequest = { attachMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_attach_photo)) },
                            onClick = {
                                attachMenu = false
                                onAttach()
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                            }
                        )
                        if (onCamera != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_attach_camera)) },
                                onClick = {
                                    attachMenu = false
                                    onCamera()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                                }
                            )
                        }
                        if (onAttachFile != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_attach_file)) },
                                onClick = {
                                    attachMenu = false
                                    onAttachFile()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.AttachFile, contentDescription = null)
                                }
                            )
                        }
                    }
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 6,
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    stringResource(
                                        when {
                                            !busy -> R.string.chat_placeholder
                                            queueing -> R.string.chat_placeholder_queue
                                            else -> R.string.chat_placeholder_busy
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    }
                )

                if (busy && hasContent) {
                    // One control only: while typing, the primary morphs to Send.
                    Spacer(Modifier.width(2.dp))
                }

                Spacer(Modifier.width(6.dp))

                var deliveryMenu by remember { mutableStateOf(false) }
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = {
                                when {
                                    primaryIsStop -> onStop()
                                    primaryIsMic -> onVoice?.invoke()
                                    else -> onSend()
                                }
                            },
                            enabled = primaryIsStop || primaryIsMic || hasContent,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = actionColor,
                                contentColor = actionContent,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .graphicsLayer { scaleX = sendScale; scaleY = sendScale }
                        ) {
                            when {
                                primaryIsStop -> Icon(
                                    Icons.Rounded.Stop,
                                    contentDescription = stringResource(R.string.action_stop),
                                    modifier = Modifier.size(16.dp)
                                )

                                primaryIsMic -> Icon(
                                    Icons.Rounded.Mic,
                                    contentDescription = stringResource(R.string.chat_voice_input),
                                    modifier = Modifier.size(20.dp)
                                )

                                queueing -> Icon(
                                    Icons.Rounded.PlaylistAdd,
                                    contentDescription = stringResource(R.string.chat_queue_add),
                                    modifier = Modifier.size(18.dp)
                                )

                                else -> Icon(
                                    Icons.Rounded.Send,
                                    contentDescription = if (busy) {
                                        stringResource(R.string.chat_steer_now)
                                    } else {
                                        stringResource(R.string.action_send)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (busy && hasContent) {
                            IconButton(
                                onClick = { deliveryMenu = true },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowDropDown,
                                    contentDescription = stringResource(R.string.chat_delivery_busy),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = deliveryMenu,
                        onDismissRequest = { deliveryMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_steer_now)) },
                            trailingIcon = {
                                if (!queueing) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                deliveryMenu = false
                                onDeliveryChange("steer")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_queue_add)) },
                            trailingIcon = {
                                if (queueing) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                deliveryMenu = false
                                onDeliveryChange("queue")
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Three bouncing bars: the "alive" indicator used instead of a filled status chip.
 */
@Composable
fun ActivityBars(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    barWidth: androidx.compose.ui.unit.Dp = 3.dp,
    maxHeight: androidx.compose.ui.unit.Dp = 11.dp
) {
    val transition = rememberInfiniteTransition(label = "activity")
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val height by transition.animateFloat(
                initialValue = 0.27f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 520 + index * 110,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            Box(
                Modifier
                    .width(barWidth)
                    .height(maxHeight * height)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Slow aurora under the header while a turn streams; never a spinner. */
@Composable
fun StreamingHairline(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "hairline")
    val shift by transition.animateFloat(
        initialValue = -620f,
        targetValue = 1720f,
        animationSpec = infiniteRepeatable(
            animation = tween(6200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shift"
    )
    val warm = MaterialTheme.colorScheme.secondary
    Box(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = 0.10f),
                        color.copy(alpha = 0.55f),
                        warm.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    startX = shift,
                    endX = shift + 560f
                )
            )
    )
}
