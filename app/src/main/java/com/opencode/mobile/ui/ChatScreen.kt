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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.input.KeyboardType
import com.opencode.mobile.FormField
import com.opencode.mobile.FormRequest
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
import androidx.compose.material.icons.rounded.ContentCopy
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
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
import com.opencode.mobile.SeenStore
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
    val seenStore = remember(context) { SeenStore(context) }
    var initialPositioned by remember(sessionID) { mutableStateOf(false) }

    LaunchedEffect(lastId) {
        val last = state.messages.lastOrNull() ?: return@LaunchedEffect
        seenStore.markSeen(sessionID, if (last.created > 0) last.created else System.currentTimeMillis())
    }

    // Jump straight to the newest message. Animating across dozens of items is what
    // left the view stranded mid-history on long sessions.
    LaunchedEffect(state.messages.size) {
        if (!initialPositioned && state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
            initialPositioned = true
        }
    }

    LaunchedEffect(lastId, lastLength) {
        if (!initialPositioned || state.messages.isEmpty()) return@LaunchedEffect
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastVisible >= state.messages.lastIndex - 2) {
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
                Text(
                    state.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AnimatedVisibility(visible = state.busy) {
                    Row(
                        Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActivityBars()
                        Spacer(Modifier.width(7.dp))
                        Text(
                            stringResource(R.string.chat_working),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        if (state.busy) {
            StreamingHairline()
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

            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.background, Color.Transparent)
                        )
                    )
            )

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
            if (state.queued.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulsingDot(
                        color = MaterialTheme.colorScheme.tertiary,
                        pulsing = true,
                        size = 6
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.chat_queued, state.queued.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.queued.first().text.replace('\n', ' ').take(60),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            state.error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            Composer(
                value = input,
                onValueChange = { input = it },
                attachments = picked.map { AttachmentPreview(it.uri, it.attachment.name) },
                busy = state.busy,
                onAttach = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemove = { preview ->
                    picked = picked.filterNot { it.uri == preview.uri }
                },
                onSend = {
                    val text = input.trim()
                    val files = picked.map { it.attachment }
                    if (text.isNotEmpty() || files.isNotEmpty()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        input = ""
                        picked = emptyList()
                        vm.send(text.ifBlank { screenshotOnlyText }, files) { ok ->
                            if (!ok) input = text
                        }
                    }
                },
                onStop = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.interrupt()
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    state.pendingPermission?.let { p ->
        PermissionSheet(permission = p, onDecision = vm::reply)
    }

    state.pendingForm?.let { form ->
        FormSheet(form = form, onSubmit = { answers -> vm.answerForm(form.id, answers) })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FormSheet(form: FormRequest, onSubmit: (Map<String, Any>) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val values = remember(form.id) { mutableStateMapOf<String, Any>() }

    LaunchedEffect(form.id) {
        form.fields.forEach { field ->
            if (values.containsKey(field.key)) return@forEach
            when (val fallback = field.default) {
                is Boolean -> values[field.key] = fallback
                is Number -> values[field.key] = fallback.toString()
                is String -> values[field.key] = fallback
                is org.json.JSONArray -> values[field.key] =
                    (0 until fallback.length()).map { fallback.optString(it) }

                else -> Unit
            }
        }
    }

    fun answerOf(field: FormField, raw: Any): Any = when (field.type) {
        "integer" -> (raw as? String)?.toIntOrNull() ?: raw
        "number" -> (raw as? String)?.toDoubleOrNull() ?: raw
        else -> raw
    }

    val complete = form.fields.all { field ->
        if (!field.required) return@all true
        val value = values[field.key] ?: return@all false
        when (value) {
            is String -> value.isNotBlank()
            is Collection<*> -> value.isNotEmpty()
            else -> true
        }
    }

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(form.title, style = MaterialTheme.typography.headlineSmall)

            form.fields.forEach { field ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(field.title, style = MaterialTheme.typography.titleSmall)
                    if (field.description.isNotBlank()) {
                        Text(
                            field.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    when {
                        field.type == "boolean" -> {
                            Switch(
                                checked = values[field.key] as? Boolean ?: false,
                                onCheckedChange = { values[field.key] = it }
                            )
                        }

                        field.type == "multiselect" -> {
                            val selected = (values[field.key] as? List<*>)?.filterIsInstance<String>()
                                ?: emptyList()
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                field.options.forEach { option ->
                                    FilterChip(
                                        selected = option.value in selected,
                                        onClick = {
                                            values[field.key] = if (option.value in selected) {
                                                selected - option.value
                                            } else {
                                                selected + option.value
                                            }
                                        },
                                        label = { Text(option.label) }
                                    )
                                }
                            }
                        }

                        field.options.isNotEmpty() -> {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                field.options.forEach { option ->
                                    FilterChip(
                                        selected = values[field.key] == option.value,
                                        onClick = { values[field.key] = option.value },
                                        label = { Text(option.label) }
                                    )
                                }
                            }
                        }

                        else -> {
                            OutlinedTextField(
                                value = values[field.key] as? String ?: "",
                                onValueChange = { values[field.key] = it },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = if (field.type == "integer" || field.type == "number") {
                                    KeyboardOptions(keyboardType = KeyboardType.Number)
                                } else {
                                    KeyboardOptions.Default
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val answers = form.fields
                        .mapNotNull { field ->
                            val raw = values[field.key] ?: return@mapNotNull null
                            field.key to answerOf(field, raw)
                        }
                        .toMap()
                    onSubmit(answers)
                },
                enabled = complete,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.form_submit)) }
        }
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

            if (!message.pending && message.plainText.isNotBlank()) {
                val clipboard = LocalClipboardManager.current
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(message.plainText)) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.chat_copy_message),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
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
