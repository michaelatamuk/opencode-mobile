package com.opencode.mobile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.opencode.mobile.AttachmentLoader
import com.opencode.mobile.ChatViewModel
import com.opencode.mobile.Msg
import com.opencode.mobile.OpenCodeRepository
import com.opencode.mobile.Part
import com.opencode.mobile.PendingPermission
import com.opencode.mobile.R
import com.opencode.mobile.WatchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    repo: OpenCodeRepository,
    sessionID: String,
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: ChatViewModel = viewModel(key = "chat:$sessionID") {
        ChatViewModel(repo, sessionID)
    }
    val state by vm.state.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<List<PickedAttachment>>(emptyList()) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val attachment = withContext(Dispatchers.IO) {
                        AttachmentLoader.load(context, uri)
                    }
                    if (picked.size < 8) picked = picked + PickedAttachment(attachment, uri)
                } catch (_: Exception) {
                }
            }
        }
    }

    DisposableEffect(sessionID) {
        WatchState.activeSessionID = sessionID
        onDispose {
            if (WatchState.activeSessionID == sessionID) WatchState.activeSessionID = null
        }
    }

    val lastId = state.messages.lastOrNull()?.id
    val lastLength = state.messages.lastOrNull()?.plainText?.length
    val screenshotOnlyText = stringResource(R.string.chat_screenshot_only)
    LaunchedEffect(lastId, lastLength) {
        if (state.messages.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(state.messages.lastIndex) }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, state.olderCursor) {
        if (state.messages.isNotEmpty() &&
            state.olderCursor != null &&
            listState.firstVisibleItemIndex <= 2 &&
            !state.loadingOlder
        ) {
            vm.loadOlder { added ->
                if (added > 0) {
                    scope.launch {
                        listState.scrollToItem(listState.firstVisibleItemIndex + added)
                    }
                }
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    AnimatedVisibility(visible = state.busy) {
                        Row(Modifier.padding(start = 8.dp)) { WorkingPill() }
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    Box(Modifier.animateItem()) {
                        when (message.role) {
                            "user" -> UserMessage(message)
                            else -> AssistantMessage(message)
                        }
                    }
                }
            }

            if (listState.canScrollForward) {
                FilledTonalIconButton(
                    onClick = {
                        if (state.messages.isNotEmpty()) {
                            scope.launch {
                                listState.animateScrollToItem(state.messages.lastIndex)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Rounded.ArrowDownward,
                        contentDescription = stringResource(R.string.chat_jump_latest),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(Modifier.navigationBarsPadding()) {
            state.error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column {
                    if (picked.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(picked) { item ->
                                Box {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = item.attachment.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    )
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(3.dp)
                                            .size(18.dp)
                                            .background(Color.Black.copy(alpha = 0.62f), CircleShape)
                                            .clickable { picked = picked - item },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.chat_remove_attachment),
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.padding(start = 2.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        IconButton(onClick = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Icon(
                                Icons.Rounded.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.chat_attach),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    stringResource(if (state.busy) R.string.chat_placeholder_busy else R.string.chat_placeholder),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            maxLines = 6,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                        Spacer(Modifier.width(6.dp))

                        val hasContent = input.isNotBlank() || picked.isNotEmpty()
                        val primaryIsStop = state.busy && !hasContent

                        // A second, quiet stop appears only when send and stop are both useful.
                        AnimatedVisibility(visible = state.busy && hasContent) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vm.interrupt()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Stop,
                                    contentDescription = stringResource(R.string.action_stop),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        AnimatedContent(
                            targetState = primaryIsStop,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.82f) + fadeIn(tween(120))) togetherWith
                                    (scaleOut(targetScale = 0.82f) + fadeOut(tween(90)))
                            },
                            label = "primaryAction"
                        ) { stop ->
                            if (stop) {
                                FilledIconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        vm.interrupt()
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Stop,
                                        contentDescription = stringResource(R.string.action_stop),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            } else {
                                FilledIconButton(
                                    onClick = {
                                        val text = input.trim()
                                        val files = picked.map { it.attachment }
                                        if (text.isEmpty() && files.isEmpty()) return@FilledIconButton
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        input = ""
                                        picked = emptyList()
                                        vm.send(text.ifBlank { screenshotOnlyText }, files) { ok ->
                                            if (!ok) input = text
                                        }
                                    },
                                    enabled = hasContent,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Send,
                                        contentDescription = stringResource(R.string.action_send),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.pendingPermission?.let { p ->
        PermissionSheet(permission = p, onDecision = vm::reply)
    }
}

@Composable
private fun UserMessage(message: Msg) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            message.parts.forEach { part ->
                when (part) {
                    is Part.Image -> AsyncImage(
                        model = "data:${part.mime};base64,${part.base64}",
                        contentDescription = stringResource(R.string.chat_attached_image),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )

                    is Part.Text -> Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 6.dp,
                            bottomEnd = 20.dp,
                            bottomStart = 20.dp
                        ),
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                part.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun AssistantMessage(message: Msg) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!message.hasVisibleContent) {
            if (message.pending) TypingIndicator()
        } else {
            message.parts.forEach { part ->
                when (part) {
                    is Part.Text -> if (part.text.isNotBlank()) {
                        SelectionContainer { MarkdownText(part.text) }
                    }

                    is Part.Tool -> ToolCallCard(
                        name = part.name,
                        status = part.status,
                        summary = part.summary,
                        input = part.input,
                        output = part.output,
                        error = part.error,
                        modifier = Modifier.fillMaxWidth()
                    )

                    is Part.Reasoning -> ReasoningBlock(
                        text = part.text,
                        durationMs = part.durationMs,
                        pending = message.pending,
                        modifier = Modifier.fillMaxWidth()
                    )

                    is Part.Image -> Unit
                }
            }
            if (message.pending) TypingIndicator(compact = true)
        }
    }
}

@Composable
private fun TypingIndicator(compact: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        Modifier.padding(vertical = if (compact) 2.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(600, delayMillis = index * 160, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                Modifier
                    .size(if (compact) 5.dp else 7.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        CircleShape
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionSheet(
    permission: PendingPermission,
    onDecision: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.permission_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(permission.action, style = MaterialTheme.typography.titleSmall)
            if (permission.detail.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        permission.detail,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onDecision("once") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.permission_allow_once)) }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onDecision("always") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.permission_always)) }
                TextButton(
                    onClick = { onDecision("reject") },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.permission_reject), color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun WorkingPill() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = CircleShape
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsingDot(
                color = MaterialTheme.colorScheme.secondary,
                pulsing = true,
                size = 6
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.chat_working),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private data class PickedAttachment(val attachment: com.opencode.mobile.Attachment, val uri: Uri)
