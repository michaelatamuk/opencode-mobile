package com.opencode.mobile.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Base64
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Difference
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import com.opencode.mobile.ChatUiState
import com.opencode.mobile.ChatViewModel
import com.opencode.mobile.CommandOption
import com.opencode.mobile.DraftStore
import com.opencode.mobile.FileDiff
import com.opencode.mobile.Msg
import com.opencode.mobile.ModelOption
import com.opencode.mobile.OpenCodeRepository
import com.opencode.mobile.Part
import com.opencode.mobile.PendingPermission
import com.opencode.mobile.PresetStore
import com.opencode.mobile.QueuedMessage
import com.opencode.mobile.R
import com.opencode.mobile.ScheduleItem
import com.opencode.mobile.SecurePrefs
import com.opencode.mobile.SeenStore
import com.opencode.mobile.SessionPreset
import com.opencode.mobile.SkillOption
import com.opencode.mobile.VcsInfo
import com.opencode.mobile.WatchState
import com.opencode.mobile.friendlyError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    repo: OpenCodeRepository,
    sessionID: String?,
    showBack: Boolean,
    onBack: () -> Unit,
    onSessionCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vm: ChatViewModel = viewModel(key = "chat:${sessionID ?: "draft"}") {
        ChatViewModel(repo, sessionID, context)
    }
    val state by vm.state.collectAsStateWithLifecycle()

    var input by remember(sessionID) { mutableStateOf(DraftStore.get(context, sessionID)) }
    var picked by remember { mutableStateOf<List<PickedAttachment>>(emptyList()) }
    var showTune by remember { mutableStateOf(false) }
    var showDiff by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmUndo by remember { mutableStateOf(false) }
    var showLibrary by remember { mutableStateOf(false) }
    var pickedSkills by remember { mutableStateOf<List<SkillOption>>(emptyList()) }
    var presets by remember { mutableStateOf(PresetStore.all(context)) }
    var savePreset by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var viewingSkill by remember { mutableStateOf<SkillOption?>(null) }
    var showAb by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showCompare by remember { mutableStateOf(false) }
    var showSchedules by remember { mutableStateOf(false) }
    var showShell by remember { mutableStateOf(false) }
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

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            input = if (input.isBlank()) spoken else input.trimEnd() + " " + spoken
            DraftStore.save(context, sessionID, input)
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val attachment = withContext(Dispatchers.IO) {
                        AttachmentLoader.loadFile(context, uri)
                    }
                    if (picked.size < 8) picked = picked + PickedAttachment(attachment, uri)
                } catch (_: Exception) {
                }
            }
        }
    }
    val voicePrompt = stringResource(R.string.chat_voice_prompt)
    val voiceLanguage = remember {
        SecurePrefs.get(context).getString("voice_language", "").orEmpty()
    }
    val voiceName = remember {
        SecurePrefs.get(context).getString("voice_name", "").orEmpty()
    }
    val tts = remember {
        var reference: TextToSpeech? = null
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                if (voiceLanguage.isNotBlank()) {
                    reference?.language = java.util.Locale.forLanguageTag(voiceLanguage)
                }
                if (voiceName.isNotBlank()) {
                    reference?.voice = reference?.voices?.firstOrNull { it.name == voiceName }
                        ?: reference?.voice
                }
            }
        }.also { reference = it }
    }
    var speakingText by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    val speak: (String) -> Unit = { text ->
        if (text.isNotBlank()) {
            if (speakingText == text) {
                tts.stop()
                speakingText = null
            } else {
                tts.stop()
                if (voiceLanguage.isNotBlank()) {
                    tts.language = java.util.Locale.forLanguageTag(voiceLanguage)
                }
                if (voiceName.isNotBlank()) {
                    tts.voice = tts.voices?.firstOrNull { it.name == voiceName } ?: tts.voice
                }
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "message")
                speakingText = text
            }
        }
    }

    DisposableEffect(sessionID) {
        if (sessionID != null) WatchState.activeSessionID = sessionID
        onDispose {
            if (sessionID != null && WatchState.activeSessionID == sessionID) {
                WatchState.activeSessionID = null
            }
        }
    }

    // A draft becomes a real session on first send; follow it.
    LaunchedEffect(state.sessionID) {
        if (sessionID == null && state.sessionID.isNotBlank()) {
            onSessionCreated(state.sessionID)
        }
    }

    val displayMessages = remember(state.messages, query) {
        if (query.isBlank()) state.messages
        else state.messages.filter { it.plainText.contains(query, ignoreCase = true) }
    }

    val lastId = state.messages.lastOrNull()?.id
    val lastLength = state.messages.lastOrNull()?.plainText?.length
    val screenshotOnlyText = stringResource(R.string.chat_screenshot_only)
    val seenStore = remember(context) { SeenStore(context) }
    var initialPositioned by remember(sessionID) { mutableStateOf(false) }

    LaunchedEffect(lastId, state.updated) {
        if (sessionID == null) return@LaunchedEffect
        val last = state.messages.lastOrNull()
        val stamp = maxOf(
            System.currentTimeMillis(),
            state.updated,
            last?.created ?: 0L
        )
        seenStore.markSeen(sessionID, stamp)
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
            if (searchOpen) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_search)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Text(
                    "${displayMessages.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { searchOpen = false; query = "" }) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_cancel)
                    )
                }
            } else {
            Column(Modifier.weight(1f)) {
                Text(
                    if (sessionID == null) stringResource(R.string.session_new) else state.title,
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
            IconButton(onClick = { searchOpen = true }) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.chat_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = { showTune = true }) {
                Icon(
                    Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.chat_model_agent),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (sessionID != null) {
                IconButton(onClick = { showDiff = true }) {
                    Icon(
                        Icons.Rounded.Difference,
                        contentDescription = stringResource(R.string.chat_changes),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.chat_actions),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_branch_session)) },
                            onClick = {
                                menuOpen = false
                                vm.fork(null) { child -> onSessionCreated(child) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_undo_turn)) },
                            enabled = !state.busy,
                            onClick = {
                                menuOpen = false
                                confirmUndo = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_compact)) },
                            enabled = !state.busy,
                            onClick = {
                                menuOpen = false
                                vm.compact()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_library)) },
                            onClick = {
                                menuOpen = false
                                showLibrary = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_save_preset)) },
                            onClick = {
                                menuOpen = false
                                presetName = ""
                                savePreset = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_ab)) },
                            enabled = !state.busy,
                            onClick = {
                                menuOpen = false
                                showAb = true
                            }
                        )
                        if (state.parentID.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_compare)) },
                                enabled = !state.busy,
                                onClick = {
                                    menuOpen = false
                                    showCompare = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_schedules)) },
                            onClick = {
                                menuOpen = false
                                showSchedules = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_shell)) },
                            onClick = {
                                menuOpen = false
                                showShell = true
                            }
                        )
                    }
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
                items(displayMessages, key = { it.id }) { message ->
                    Box(Modifier.animateItem()) {
                        when (message.role) {
                            "user" -> UserMessage(
                                message = message,
                                onBranch = { before ->
                                    vm.fork(before) { child -> onSessionCreated(child) }
                                }
                            )
                            else -> AssistantMessage(message, onSpeak = speak)
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
                    Modifier
                        .fillMaxWidth()
                        .clickable { showQueue = true }
                        .padding(horizontal = 18.dp, vertical = 2.dp),
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
            if (pickedSkills.isNotEmpty()) {
                FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pickedSkills.forEach { skill ->
                        AssistChip(
                            onClick = { pickedSkills = pickedSkills - skill },
                            label = { Text(skill.name) },
                            trailingIcon = {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }
            if (state.sending) {
                if (state.sendProgress > 0f) {
                    LinearProgressIndicator(
                        progress = { state.sendProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
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
                onValueChange = {
                    input = it
                    DraftStore.save(context, sessionID, it)
                },
                attachments = picked.map {
                    AttachmentPreview(it.uri, it.attachment.name, it.attachment.mime)
                },
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
                        vm.send(
                            text = text.ifBlank { screenshotOnlyText },
                            files = files,
                            skillIDs = pickedSkills.map { it.id }
                        ) { ok ->
                            if (!ok) {
                                input = text
                                DraftStore.save(context, sessionID, text)
                            } else {
                                pickedSkills = emptyList()
                                DraftStore.clear(context, null)
                                DraftStore.clear(context, state.sessionID.ifBlank { null })
                            }
                        }
                    }
                },
                onStop = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.interrupt()
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                onVoice = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, voicePrompt)
                    if (voiceLanguage.isNotBlank()) {
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceLanguage)
                    }
                    runCatching { voiceLauncher.launch(intent) }
                },
                onAttachFile = { runCatching { fileLauncher.launch(arrayOf("*/*")) } }
            )
        }
    }

    state.pendingPermission?.let { p ->
        PermissionSheet(permission = p, onDecision = vm::reply)
    }

    state.pendingForm?.let { form ->
        FormSheet(form = form, onSubmit = { answers -> vm.answerForm(form.id, answers) })
    }

    if (showTune) {
        TuneSheet(
            state = state,
            presets = presets,
            onDismiss = { showTune = false },
            onAgent = { vm.selectAgent(it) },
            onModel = { provider, model -> vm.selectModel(provider, model) },
            onCompact = {
                showTune = false
                vm.compact()
            },
            onApplyPreset = { preset ->
                if (preset.agentID.isNotBlank()) vm.selectAgent(preset.agentID)
                if (preset.modelID.isNotBlank()) {
                    vm.selectModel(preset.providerID, preset.modelID)
                }
                if (preset.prompt.isNotBlank()) {
                    input = preset.prompt
                    DraftStore.save(context, sessionID, preset.prompt)
                }
                showTune = false
            },
            onDeletePreset = { preset ->
                PresetStore.delete(context, preset.id)
                presets = PresetStore.all(context)
            }
        )
    }

    if (savePreset) {
        AlertDialog(
            onDismissRequest = { savePreset = false },
            title = { Text(stringResource(R.string.chat_save_preset)) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text(stringResource(R.string.chat_preset_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            PresetStore.save(
                                context,
                                SessionPreset(
                                    id = System.currentTimeMillis().toString(),
                                    name = presetName.trim(),
                                    agentID = state.agentID,
                                    modelID = state.modelID,
                                    providerID = state.modelProviderID,
                                    prompt = input
                                )
                            )
                            presets = PresetStore.all(context)
                        }
                        savePreset = false
                    }
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { savePreset = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDiff && sessionID != null) {
        DiffSheet(
            repo = repo,
            sessionID = sessionID,
            messages = state.messages,
            directory = state.directory,
            onDismiss = { showDiff = false }
        )
    }

    if (showLibrary) {
        LibrarySheet(
            commands = state.commands,
            skills = state.skills,
            selected = pickedSkills,
            onDismiss = { showLibrary = false },
            onCommand = { command ->
                input = "/${command.name} "
                DraftStore.save(context, sessionID, input)
                showLibrary = false
            },
            onToggleSkill = { skill ->
                pickedSkills = if (pickedSkills.any { it.id == skill.id }) {
                    pickedSkills.filterNot { it.id == skill.id }
                } else {
                    pickedSkills + skill
                }
            },
            onViewSkill = { viewingSkill = it }
        )
    }

    viewingSkill?.let { skill ->
        SkillSheet(skill = skill, onDismiss = { viewingSkill = null })
    }

    if (showAb) {
        AbSheet(
            models = state.models.filterNot {
                it.id == state.modelID && it.providerID == state.modelProviderID
            },
            onDismiss = { showAb = false },
            onPick = { model ->
                showAb = false
                vm.runAb(model.providerID, model.id) { child -> onSessionCreated(child) }
            }
        )
    }

    if (showQueue && state.queued.isNotEmpty()) {
        QueueSheet(
            queued = state.queued,
            onCancel = vm::cancelQueued,
            onSteer = vm::steerQueued,
            onEdit = { inboxID, text, delivery -> vm.editQueued(inboxID, text, delivery) },
            onDismiss = { showQueue = false }
        )
    }

    if (showCompare && state.parentID.isNotBlank()) {
        CompareSheet(
            repo = repo,
            parentID = state.parentID,
            branchText = state.messages
                .lastOrNull { it.role == "assistant" && it.plainText.isNotBlank() }
                ?.plainText
                .orEmpty(),
            onDismiss = { showCompare = false }
        )
    }

    if (showSchedules && sessionID != null) {
        SchedulesSheet(
            repo = repo,
            sessionID = sessionID,
            onDismiss = { showSchedules = false }
        )
    }

    if (showShell && sessionID != null) {
        ShellSheet(
            repo = repo,
            sessionID = sessionID,
            onDismiss = { showShell = false }
        )
    }

    if (confirmUndo) {
        AlertDialog(
            onDismissRequest = { confirmUndo = false },
            title = { Text(stringResource(R.string.chat_undo_title)) },
            text = { Text(stringResource(R.string.chat_undo_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmUndo = false
                    vm.undoLastTurn()
                }) { Text(stringResource(R.string.chat_undo_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUndo = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private data class TouchedFile(val path: String, val count: Int, val status: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiffSheet(
    repo: OpenCodeRepository,
    sessionID: String,
    messages: List<Msg>,
    directory: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var diffs by remember { mutableStateOf<List<FileDiff>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var viewFile by remember { mutableStateOf<String?>(null) }
    var pendingRevert by remember { mutableStateOf<FileDiff?>(null) }
    var revertNote by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var vcs by remember { mutableStateOf<VcsInfo?>(null) }
    val scope = rememberCoroutineScope()

    // When the project has no VCS diff (or the session lives outside the repo),
    // fall back to the files the agent actually edited in this conversation.
    val touched = remember(messages) {
        val editingTools = setOf(
            "edit", "write", "patch", "multiedit", "apply_patch",
            "create_file", "str_replace_editor"
        )
        messages.flatMap { it.parts }
            .filterIsInstance<Part.Tool>()
            .filter { it.name.lowercase() in editingTools }
            .mapNotNull { part ->
                val input = runCatching { org.json.JSONObject(part.input) }.getOrNull()
                    ?: return@mapNotNull null
                val path = listOf("filePath", "path", "file", "filename")
                    .firstNotNullOfOrNull { key ->
                        input.optString(key).takeIf { it.isNotBlank() }
                    }
                if (path == null) null else path to part.status
            }
            .groupBy({ it.first }, { it.second })
            .map { (path, statuses) ->
                TouchedFile(path, statuses.size, statuses.lastOrNull().orEmpty())
            }
            .sortedBy { it.path.lowercase() }
    }

    LaunchedEffect(sessionID, reload) {
        try {
            diffs = repo.sessionDiff(sessionID)
        } catch (e: Exception) {
            error = friendlyError(e)
            diffs = emptyList()
        }
        vcs = runCatching { repo.vcsInfo(directory) }.getOrNull()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 700.dp)) {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
            ) {
                Text(
                    stringResource(R.string.chat_changes),
                    style = MaterialTheme.typography.headlineSmall
                )
                val count = if (diffs.isNullOrEmpty()) touched.size else diffs!!.size
                Text(
                    buildString {
                        append(stringResource(R.string.chat_changes_count, count))
                        if (diffs?.isEmpty() == true && touched.isNotEmpty()) {
                            append(" · ")
                            append(stringResource(R.string.chat_changes_conversation))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (revertNote != null) {
                Text(
                    revertNote!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
            }

            vcs?.let { info ->
                if (info.branch.isNotBlank()) {
                    Text(
                        buildString {
                            if (info.provider.isNotBlank()) append(info.provider).append(" · ")
                            append(info.branch)
                            if (info.defaultBranch.isNotBlank() &&
                                info.defaultBranch != info.branch
                            ) {
                                append(" (default ").append(info.defaultBranch).append(")")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                }
            }

            when {
                error != null -> Text(
                    error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                diffs == null -> CircularProgressIndicator(
                    Modifier.padding(24.dp).size(28.dp)
                )

                diffs!!.isEmpty() && touched.isEmpty() -> Text(
                    stringResource(R.string.chat_no_changes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                diffs!!.isEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(touched, key = { it.path }) { file ->
                        ChangedFileRow(
                            file = file,
                            directory = directory,
                            onClick = { viewFile = file.path }
                        )
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(diffs!!) { diff ->
                        DiffCard(diff, directory) { pendingRevert = diff }
                    }
                }
            }
        }
    }

    if (viewFile != null) {
        FileSheet(
            repo = repo,
            path = viewFile!!,
            directory = directory,
            onDismiss = { viewFile = null }
        )
    }

    val revertedText = stringResource(R.string.chat_diff_reverted)
    val revertFailedText = stringResource(R.string.chat_diff_revert_failed)

    pendingRevert?.let { diff ->
        AlertDialog(
            onDismissRequest = { pendingRevert = null },
            title = { Text(stringResource(R.string.chat_diff_revert_title)) },
            text = { Text(stringResource(R.string.chat_diff_revert_body, diff.file)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRevert = null
                    scope.launch {
                        val ok = runCatching {
                            repo.revertFile(diff.file, directory, diff.patch)
                        }.getOrDefault(false)
                        revertNote = if (ok) revertedText else revertFailedText
                        if (ok) reload += 1
                    }
                }) { Text(stringResource(R.string.chat_diff_revert)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRevert = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileSheet(
    repo: OpenCodeRepository,
    path: String,
    directory: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var content by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(path) {
        try {
            content = repo.readFile(path, directory).take(120_000)
        } catch (e: Exception) {
            error = friendlyError(e)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                path.replace('\\', '/').substringAfterLast('/'),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            when {
                error != null -> Text(
                    error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )

                content == null -> CircularProgressIndicator(Modifier.size(28.dp))

                else -> Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            content!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

private fun relativePath(path: String, directory: String): String {
    val p = path.replace('\\', '/')
    val d = directory.replace('\\', '/').trimEnd('/')
    return if (d.isNotBlank() && p.startsWith(d, ignoreCase = true)) {
        p.substring(d.length).trimStart('/')
    } else {
        p
    }
}

@Composable
private fun ChangedFileRow(file: TouchedFile, directory: String, onClick: () -> Unit) {
    val relative = relativePath(file.path, directory)
    val name = relative.substringAfterLast('/')
    val dir = relative.substringBeforeLast('/', "")
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (dir.isNotBlank()) {
                    Text(
                        dir,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (file.count > 1) {
                Text(
                    stringResource(R.string.chat_file_edits, file.count),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            if (file.status.isNotBlank() && file.status != "completed") {
                Text(
                    file.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DiffCard(diff: FileDiff, directory: String, onRevert: () -> Unit) {
    var expanded by remember(diff.file) { mutableStateOf(false) }
    val relative = relativePath(diff.file, directory)
    val name = relative.substringAfterLast('/')
    val dir = relative.substringBeforeLast('/', "")

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (dir.isNotBlank()) {
                        Text(
                            dir,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    "+${diff.additions}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "-${diff.deletions}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.chat_diff_hide else R.string.chat_diff_show
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(Modifier.horizontalScroll(rememberScrollState())) {
                            Column(Modifier.padding(8.dp)) {
                                diff.patch.lines().take(150).forEach { line ->
                                    Text(
                                        line.ifEmpty { " " },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.5.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 1,
                                        color = when {
                                            line.startsWith("+") -> Color(0xFF4CAF50)
                                            line.startsWith("-") -> MaterialTheme.colorScheme.error
                                            line.startsWith("@@") -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (diff.patch.isNotBlank()) {
                        TextButton(
                            onClick = onRevert,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                        ) {
                            Text(
                                stringResource(R.string.chat_diff_revert),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TuneSheet(
    state: ChatUiState,
    presets: List<SessionPreset>,
    onDismiss: () -> Unit,
    onAgent: (String) -> Unit,
    onModel: (String, String) -> Unit,
    onCompact: () -> Unit,
    onApplyPreset: (SessionPreset) -> Unit,
    onDeletePreset: (SessionPreset) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.chat_model_agent), style = MaterialTheme.typography.headlineSmall)

            Text(
                stringResource(
                    R.string.chat_session_stats,
                    String.format(java.util.Locale.getDefault(), "$%.2f", state.cost),
                    state.tokensInput,
                    state.tokensOutput
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val currentAgent = state.agents.firstOrNull { it.id == state.agentID }?.name
                ?: state.agentID.ifBlank { "—" }
            val currentModel = state.models.firstOrNull {
                it.id == state.modelID && it.providerID == state.modelProviderID
            }?.name ?: state.modelID.ifBlank { "—" }
            Text(
                stringResource(R.string.chat_current_selection, currentAgent, currentModel),
                style = MaterialTheme.typography.labelLarge
            )
            if (state.selecting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            if (state.contextUsed > 0L) {
                val limit = state.contextLimit
                val ratio = if (limit > 0) {
                    (state.contextUsed.toFloat() / limit).coerceIn(0f, 1f)
                } else {
                    0f
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.chat_context),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (limit > 0) {
                                    stringResource(
                                        R.string.chat_context_used,
                                        compactTokens(state.contextUsed),
                                        compactTokens(limit),
                                        (ratio * 100).toInt()
                                    )
                                } else {
                                    stringResource(
                                        R.string.chat_context_used_unknown,
                                        compactTokens(state.contextUsed)
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (ratio > 0.85f) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (limit > 0) {
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = if (ratio > 0.85f) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }
                        TextButton(
                            onClick = onCompact,
                            enabled = !state.busy,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text(stringResource(R.string.chat_compact))
                        }
                    }
                }
            }

            if (presets.isNotEmpty()) {
                Text(
                    stringResource(R.string.chat_presets),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                presets.forEach { preset ->
                    Surface(
                        onClick = { onApplyPreset(preset) },
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(preset.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    listOf(preset.agentID, preset.modelID)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDeletePreset(preset) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (state.agents.isNotEmpty()) {
                Text(
                    stringResource(R.string.chat_agents),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    stringResource(R.string.chat_agent_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.agents.forEach { agent ->
                        FilterChip(
                            selected = agent.id == state.agentID,
                            onClick = { onAgent(agent.id) },
                            enabled = !state.selecting,
                            label = { Text(agent.name) },
                            leadingIcon = if (agent.id == state.agentID) {
                                {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
                state.agents.firstOrNull { it.id == state.agentID }
                    ?.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { description ->
                        Text(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
            }

            if (state.models.isNotEmpty()) {
                Text(
                    stringResource(R.string.chat_models),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
                var modelQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = modelQuery,
                    onValueChange = { modelQuery = it },
                    label = { Text(stringResource(R.string.chat_search_models)) },
                    singleLine = true,
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                state.models.filter { model ->
                    modelQuery.isBlank() ||
                        model.name.contains(modelQuery, ignoreCase = true) ||
                        model.id.contains(modelQuery, ignoreCase = true) ||
                        model.providerID.contains(modelQuery, ignoreCase = true)
                }.sortedWith(
                    compareByDescending<ModelOption> {
                        it.id == state.modelID && it.providerID == state.modelProviderID
                    }.thenBy { it.name }
                ).forEach { model ->
                    val selected = model.id == state.modelID &&
                        model.providerID == state.modelProviderID
                    Surface(
                        onClick = { onModel(model.providerID, model.id) },
                        enabled = !state.selecting,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(model.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${model.providerID} · ${model.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            state.error?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun compactTokens(value: Long): String = when {
    value >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000 -> String.format(java.util.Locale.US, "%.1fk", value / 1_000.0)
    else -> value.toString()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FormSheet(form: FormRequest, onSubmit: (Map<String, Any>) -> Unit) {    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbSheet(
    models: List<ModelOption>,
    onDismiss: () -> Unit,
    onPick: (ModelOption) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.chat_ab), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.chat_ab_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (models.isEmpty()) {
                Text(
                    stringResource(R.string.chat_library_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            models.forEach { model ->
                Surface(
                    onClick = { onPick(model) },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(model.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${model.providerID} · ${model.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillSheet(skill: SkillOption, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(skill.name, style = MaterialTheme.typography.headlineSmall)
            if (skill.description.isNotBlank()) {
                Text(
                    skill.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            if (skill.content.isNotBlank()) {
                MarkdownText(skill.content)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySheet(
    commands: List<CommandOption>,
    skills: List<SkillOption>,
    selected: List<SkillOption>,
    onDismiss: () -> Unit,
    onCommand: (CommandOption) -> Unit,
    onToggleSkill: (SkillOption) -> Unit,
    onViewSkill: (SkillOption) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.chat_library),
                style = MaterialTheme.typography.headlineSmall
            )

            if (commands.isNotEmpty()) {
                Text(
                    stringResource(R.string.chat_commands),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                commands.forEach { command ->
                    Surface(
                        onClick = { onCommand(command) },
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                "/${command.name}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (command.description.isNotBlank()) {
                                Text(
                                    command.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (skills.isNotEmpty()) {
                Text(
                    stringResource(R.string.chat_skills),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
                skills.forEach { skill ->
                    val isSelected = selected.any { it.id == skill.id }
                    Surface(
                        onClick = { onToggleSkill(skill) },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(skill.name, style = MaterialTheme.typography.titleSmall)
                                if (skill.description.isNotBlank()) {
                                    Text(
                                        skill.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (skill.content.isNotBlank()) {
                                IconButton(
                                    onClick = { onViewSkill(skill) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Info,
                                        contentDescription = stringResource(R.string.chat_skill_details),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (commands.isEmpty() && skills.isEmpty()) {
                Text(
                    stringResource(R.string.chat_library_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchedulesSheet(
    repo: OpenCodeRepository,
    sessionID: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var items by remember { mutableStateOf<List<ScheduleItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf(60) }
    var mode by remember { mutableStateOf("interval") }
    var cron by remember { mutableStateOf("") }
    var at by remember { mutableStateOf("") }
    var endsTimes by remember { mutableStateOf("") }
    var ifRun by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            try {
                items = repo.schedules(sessionID)
                error = null
            } catch (e: Exception) {
                error = friendlyError(e)
                items = emptyList()
            }
        }
    }

    LaunchedEffect(sessionID) { reload() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.chat_schedules),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                stringResource(R.string.chat_schedules_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(stringResource(R.string.chat_schedule_prompt)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("interval" to "Every", "cron" to "Cron", "at" to "At").forEach { (value, label) ->
                    FilterChip(
                        selected = mode == value,
                        onClick = { mode = value },
                        label = { Text(label) }
                    )
                }
            }
            when (mode) {
                "interval" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15 to "15m", 60 to "1h", 1440 to "1d").forEach { (value, label) ->
                        FilterChip(
                            selected = minutes == value,
                            onClick = { minutes = value },
                            label = { Text(label) }
                        )
                    }
                }

                "cron" -> OutlinedTextField(
                    value = cron,
                    onValueChange = { cron = it },
                    label = { Text("Cron") },
                    placeholder = { Text("0 9 * * 1-5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                else -> OutlinedTextField(
                    value = at,
                    onValueChange = { at = it },
                    label = { Text("At") },
                    placeholder = { Text("2026-11-20T09:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val whenReady = when (mode) {
                "interval" -> true
                "cron" -> cron.isNotBlank()
                else -> at.isNotBlank()
            }
            OutlinedTextField(
                value = endsTimes,
                onValueChange = { endsTimes = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Stop after N runs (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ifRun,
                onValueChange = { ifRun = it },
                label = { Text("Wake only if this command succeeds (optional)") },
                placeholder = { Text("git diff --quiet") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (prompt.isNotBlank() && whenReady) {
                        scope.launch {
                            try {
                                val spec = when (mode) {
                                    "cron" -> org.json.JSONObject().put("cron", cron.trim())
                                    "at" -> org.json.JSONObject().put("at", at.trim())
                                    else -> org.json.JSONObject()
                                        .put("every", "$minutes minutes")
                                }
                                endsTimes.toIntOrNull()?.takeIf { it > 0 }?.let { times ->
                                    spec.put("ends", org.json.JSONObject().put("times", times))
                                }
                                if (ifRun.isNotBlank()) {
                                    spec.put(
                                        "if",
                                        org.json.JSONObject().put("run", ifRun.trim())
                                    )
                                }
                                repo.createSchedule(sessionID, prompt.trim(), spec)
                                prompt = ""
                                endsTimes = ""
                                ifRun = ""
                                reload()
                            } catch (e: Exception) {
                                error = friendlyError(e)
                            }
                        }
                    }
                },
                enabled = prompt.isNotBlank() && whenReady,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.chat_schedule_add))
            }
            if (error != null) {
                Text(
                    error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            when {
                items == null -> CircularProgressIndicator(Modifier.size(24.dp))
                items!!.isEmpty() -> Text(
                    stringResource(R.string.chat_schedule_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> items!!.forEach { schedule ->
                    val paused = schedule.status.contains("pause", ignoreCase = true)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    schedule.prompt.ifBlank { stringResource(R.string.chat_schedules) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    schedule.status +
                                        if (schedule.nextDueAt > System.currentTimeMillis()) {
                                            " · +" + ((schedule.nextDueAt - System.currentTimeMillis()) / 60_000) + "m"
                                        } else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (schedule.lastOutput.isNotBlank()) {
                                    Text(
                                        schedule.lastOutput.replace('\n', ' '),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { repo.setSchedulePaused(schedule.id, !paused) }
                                        reload()
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { repo.deleteSchedule(schedule.id) }
                                        reload()
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellSheet(
    repo: OpenCodeRepository,
    sessionID: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var command by remember { mutableStateOf("") }
    var output by remember { mutableStateOf<String?>(null) }
    var shellID by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun run() {
        if (command.isBlank() || running) return
        running = true
        error = null
        scope.launch {
            try {
                val shell = repo.runShell(sessionID, command.trim())
                shellID = shell.id
                delay(800)
                output = repo.shellOutput(shell.id)
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                running = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.chat_shell),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                stringResource(R.string.chat_shell_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text(stringResource(R.string.chat_shell_command)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { run() },
                    enabled = command.isNotBlank() && !running,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.chat_shell_run))
                }
                OutlinedButton(
                    onClick = {
                        val id = shellID ?: return@OutlinedButton
                        scope.launch {
                            output = runCatching { repo.shellOutput(id) }.getOrDefault(output ?: "")
                        }
                    },
                    enabled = shellID != null,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.chat_shell_refresh))
                }
            }
            if (error != null) {
                Text(
                    error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        output ?: if (running) stringResource(R.string.chat_shell_running)
                        else stringResource(R.string.chat_shell_output),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompareSheet(
    repo: OpenCodeRepository,
    parentID: String,
    branchText: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var original by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val diff = remember(original, branchText) {
        val source = original
        if (source.isNullOrBlank() || branchText.isBlank()) null
        else WordDiff.mark(
            source,
            branchText,
            Color(0x55FF6B6B),
            Color(0x554CAF50)
        )
    }

    LaunchedEffect(parentID) {
        try {
            val page = repo.messages(parentID)
            original = page.messages
                .lastOrNull { it.role == "assistant" && it.plainText.isNotBlank() }
                ?.plainText
                .orEmpty()
        } catch (e: Exception) {
            error = friendlyError(e)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.chat_compare),
                style = MaterialTheme.typography.headlineSmall
            )
            if (error != null) {
                Text(
                    error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
            val originalText = diff?.first
                ?: AnnotatedString((original ?: "—").ifBlank { "—" })
            val branchAnnotated = diff?.second
                ?: AnnotatedString(branchText.ifBlank { "—" })
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompareSection(
                        label = stringResource(R.string.chat_compare_original),
                        text = originalText,
                        loading = original == null && error == null,
                        modifier = Modifier.weight(1f)
                    )
                    CompareSection(
                        label = stringResource(R.string.chat_compare_branch),
                        text = branchAnnotated,
                        loading = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                CompareSection(
                    label = stringResource(R.string.chat_compare_original),
                    text = originalText,
                    loading = original == null && error == null
                )
                Spacer(Modifier.height(4.dp))
                CompareSection(
                    label = stringResource(R.string.chat_compare_branch),
                    text = branchAnnotated,
                    loading = false
                )
            }
        }
    }
}

@Composable
private fun CompareSection(
    label: String,
    text: AnnotatedString,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.padding(14.dp)) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(22.dp))
                } else {
                    SelectionContainer {
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    queued: List<QueuedMessage>,
    onCancel: (String) -> Unit,
    onSteer: (String) -> Unit,
    onEdit: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editing by remember { mutableStateOf<QueuedMessage?>(null) }
    var editText by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.chat_queue_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                stringResource(R.string.chat_queue_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            queued.forEach { item ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.text.replace('\n', ' ').take(240),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                item.delivery,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                editing = item
                                editText = item.text
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.chat_queue_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (item.delivery != "steer") {
                            TextButton(onClick = { onSteer(item.id) }) {
                                Text(
                                    stringResource(R.string.chat_queue_steer),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        IconButton(
                            onClick = { onCancel(item.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.action_cancel),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    editing?.let { item ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.chat_queue_edit_title)) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEdit(item.id, editText, item.delivery)
                    editing = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun Base64Image(mime: String, base64: String, modifier: Modifier = Modifier) {
    val image by produceState<ImageBitmap?>(null, base64) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    val bitmap = image
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(R.string.chat_attached_image),
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun UserMessage(message: Msg, onBranch: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            message.parts.forEach { part ->
                when (part) {
                    is Part.Image -> Base64Image(
                        mime = part.mime,
                        base64 = part.base64,
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(16.dp))
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
            if (message.id.isNotBlank()) {
                IconButton(
                    onClick = { onBranch(message.id) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Rounded.CallSplit,
                        contentDescription = stringResource(R.string.chat_branch_from_here),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityPart(part: Part, pending: Boolean) {
    when (part) {
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
            pending = pending,
            modifier = Modifier.fillMaxWidth()
        )

        else -> Unit
    }
}

@Composable
private fun ActivityGroup(parts: List<Part>, pending: Boolean, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val tools = parts.filterIsInstance<Part.Tool>()
    val thoughts = parts.filterIsInstance<Part.Reasoning>()
    val running = pending && tools.any { it.status == "running" || it.status == "streaming" }
    val names = tools.map { it.name }.distinct().take(3)
    val prettyNames = names.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }

    val usedOne = stringResource(R.string.chat_activity_used_one)
    val usedMany = stringResource(R.string.chat_activity_used, tools.size)
    val working = stringResource(
        R.string.chat_activity_working,
        names.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "…"
    )
    val thoughtsLabel = if (thoughts.size == 1) {
        val total = thoughts.first().durationMs
        if (total >= 1000) {
            stringResource(R.string.chat_activity_thought_for, (total / 1000).toInt())
        } else {
            stringResource(R.string.chat_activity_thought)
        }
    } else {
        stringResource(R.string.chat_activity_thoughts, thoughts.size)
    }

    val summary = when {
        running -> working
        else -> listOf(
            thoughtsLabel.takeIf { thoughts.isNotEmpty() },
            buildString {
                if (tools.isNotEmpty()) {
                    append(if (tools.size == 1) usedOne else usedMany)
                    if (prettyNames.isNotBlank()) append(" · ").append(prettyNames)
                }
            }.takeIf { tools.isNotEmpty() }
        ).filterNotNull().joinToString(" · ")
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (running) {
                    ActivityBars()
                } else {
                    Icon(
                        Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    parts.forEach { part ->
                        when (part) {
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
                                pending = pending,
                                modifier = Modifier.fillMaxWidth()
                            )

                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

private sealed interface DisplayBlock {
    data class TextBlock(val text: String) : DisplayBlock
    data class Activity(val parts: List<Part>) : DisplayBlock
}

private fun groupDisplayParts(parts: List<Part>): List<DisplayBlock> {
    val out = mutableListOf<DisplayBlock>()
    val activity = mutableListOf<Part>()
    fun flush() {
        if (activity.isNotEmpty()) {
            out += DisplayBlock.Activity(activity.toList())
            activity.clear()
        }
    }
    parts.forEach { part ->
        when (part) {
            is Part.Text -> {
                flush()
                if (part.text.isNotBlank()) out += DisplayBlock.TextBlock(part.text)
            }

            is Part.Tool, is Part.Reasoning -> activity += part
            is Part.Image -> Unit
        }
    }
    flush()
    return out
}

@Composable
private fun AssistantMessage(message: Msg, onSpeak: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!message.hasVisibleContent) {
            if (message.pending) TypingIndicator()
        } else {
            val blocks = remember(message.parts) { groupDisplayParts(message.parts) }
            blocks.forEach { block ->
                when (block) {
                    is DisplayBlock.TextBlock -> SelectionContainer {
                        MarkdownText(block.text)
                    }

                    is DisplayBlock.Activity -> {
                        val items = block.parts.filter {
                            it is Part.Tool || it is Part.Reasoning
                        }
                        if (items.size == 1) {
                            ActivityPart(items.first(), message.pending)
                        } else if (items.isNotEmpty()) {
                            ActivityGroup(
                                parts = block.parts,
                                pending = message.pending,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            if (message.pending) TypingIndicator(compact = true)

            if (!message.pending && message.plainText.isNotBlank()) {
                val clipboard = LocalClipboardManager.current
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(
                        onClick = { onSpeak(message.plainText) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Rounded.VolumeUp,
                            contentDescription = stringResource(R.string.chat_read_aloud),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
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
