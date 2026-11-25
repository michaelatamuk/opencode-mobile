package com.opencode.mobile.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.UnfoldLess
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
import androidx.compose.material3.ModalBottomSheetProperties
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
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
import com.opencode.mobile.AgentOption
import com.opencode.mobile.ModelOption
import com.opencode.mobile.OpenCodeRepository
import com.opencode.mobile.Part
import com.opencode.mobile.PendingPermission
import com.opencode.mobile.PresetStore
import com.opencode.mobile.QueuedMessage
import com.opencode.mobile.R
import com.opencode.mobile.ApiException
import com.opencode.mobile.ContextSummary
import com.opencode.mobile.FileEntry
import com.opencode.mobile.ReferenceItem
import com.opencode.mobile.InstructionEntry
import com.opencode.mobile.JobItem
import com.opencode.mobile.RunningShell
import com.opencode.mobile.SavedPermission
import com.opencode.mobile.UiPrefs
import com.opencode.mobile.VcsChange
import com.opencode.mobile.WorktreeEntry
import com.opencode.mobile.ScheduleItem
import com.opencode.mobile.SchedulesDisabledException
import com.opencode.mobile.SecurePrefs
import com.opencode.mobile.SeenStore
import com.opencode.mobile.SessionPreset
import com.opencode.mobile.SkillOption
import com.opencode.mobile.VcsInfo
import com.opencode.mobile.WatchState
import com.opencode.mobile.friendlyError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    openDiffFirst: Boolean = false,
    onDiffOpened: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vm: ChatViewModel = viewModel(key = "chat:${sessionID ?: "draft"}") {
        ChatViewModel(repo, sessionID, context)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val showTools by UiPrefs.showTools.collectAsStateWithLifecycle()

    var input by remember(sessionID) { mutableStateOf(DraftStore.get(context, sessionID)) }
    var picked by remember { mutableStateOf<List<PickedAttachment>>(emptyList()) }
    var showTune by remember { mutableStateOf(false) }
    var showDiff by remember { mutableStateOf(openDiffFirst) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmUndo by remember { mutableStateOf(false) }
    var confirmRegenerate by remember { mutableStateOf(false) }
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
    var showWorkspace by remember { mutableStateOf(false) }
    var showProject by remember { mutableStateOf(false) }
    var showOverview by remember { mutableStateOf(false) }
    var runningShells by remember { mutableStateOf<List<RunningShell>>(emptyList()) }
    var runningJobs by remember { mutableStateOf<List<JobItem>>(emptyList()) }
    var shellRefreshKey by remember { mutableStateOf(0) }
    var dismissedRunId by remember { mutableStateOf<String?>(null) }
    var steerMode by remember { mutableStateOf(true) }
    LaunchedEffect(openDiffFirst) {
        if (openDiffFirst) {
            showDiff = true
            onDiffOpened()
        }
    }

    // Steer is the default for a fresh busy turn; queueing is an explicit exception.
    LaunchedEffect(state.busy) {
        if (!state.busy) steerMode = true
    }

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

    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = captureUri
        if (ok && uri != null) {
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

    fun takePhoto() {
        val dir = java.io.File(context.cacheDir, "captures").apply { mkdirs() }
        val file = java.io.File(dir, "capture-${System.currentTimeMillis()}.jpg")
        runCatching {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            captureUri = uri
            cameraLauncher.launch(uri)
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

    val seenStore = remember(context) { SeenStore(context) }
    val previousSeen = remember(sessionID) {
        if (sessionID == null) 0L else seenStore.lastSeen(sessionID)
    }

    val displayMessages = remember(state.messages, query) {
        if (query.isBlank()) state.messages
        else state.messages.filter { it.plainText.contains(query, ignoreCase = true) }
    }
    val blocks = remember(displayMessages) { buildChatBlocks(displayMessages) }

    val unread = remember(displayMessages, previousSeen) {
        if (previousSeen <= 0L) {
            emptyList()
        } else {
            displayMessages.filter {
                it.created > previousSeen &&
                    it.role == "assistant" &&
                    !it.pending &&
                    it.plainText.isNotBlank()
            }
        }
    }
    val firstUnread = unread.firstOrNull()
    val unreadBlockIndex = remember(blocks, firstUnread) {
        if (firstUnread == null) -1 else blocks.indexOfFirst { block ->
            when (block) {
                is ChatBlock.UserBlock -> block.message.id == firstUnread.id
                is ChatBlock.TurnBlock -> block.messages.any { it.id == firstUnread.id }
                else -> false
            }
        }
    }

    val lastAssistant = state.messages.lastOrNull { it.role == "assistant" }
    val lastUser = state.messages.lastOrNull { it.role == "user" }
    val lastTools = remember(lastAssistant?.id) {
        lastAssistant?.parts?.filterIsInstance<Part.Tool>().orEmpty()
    }
    val showRunSummary = sessionID != null && !state.busy && lastAssistant != null &&
        lastTools.isNotEmpty() && dismissedRunId != lastAssistant.id

    val lastId = state.messages.lastOrNull()?.id
    val screenshotOnlyText = stringResource(R.string.chat_screenshot_only)
    var initialPositioned by remember(sessionID) { mutableStateOf(false) }
    var restoreKey by remember(sessionID) { mutableStateOf<String?>(null) }
    // Follow the live turn unless the reader has scrolled up. Lazy item indices
    // are not message indices: one item can hold a whole turn of messages.
    var stickToBottom by remember(sessionID) { mutableStateOf(true) }
    var wasBusy by remember(sessionID) { mutableStateOf(false) }
    var completionTick by remember(sessionID) { mutableStateOf(0) }
    val dragging by listState.interactionSource.collectIsDraggedAsState()

    // A run ending is the emotional payoff; give it one soft beat, not a toast.
    LaunchedEffect(state.busy) {
        if (wasBusy && !state.busy) {
            completionTick++
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (SecurePrefs.get(context).getBoolean("completion_sound", false)) {
                scope.launch {
                    val tone = runCatching {
                        android.media.ToneGenerator(
                            android.media.AudioManager.STREAM_NOTIFICATION,
                            55
                        )
                    }.getOrNull() ?: return@launch
                    runCatching {
                        tone.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 140)
                    }
                    delay(420)
                    runCatching { tone.release() }
                }
            }
        }
        wasBusy = state.busy
    }

    suspend fun snapToNewest() {
        val last = blocks.lastIndex
        if (last < 0) return
        runCatching { listState.scrollToItem(last) }
        // The last item may be a whole turn and taller than the viewport, so
        // scrolling to its index stops at its top; continue to the real edge.
        runCatching { listState.scrollBy(200_000f) }
    }

    val lightCard = !androidx.compose.foundation.isSystemInDarkTheme()

    fun shareRunCard() {
        val title = state.title.ifBlank { context.getString(R.string.session_new) }
        val agent = state.agents.firstOrNull { it.id == state.agentID }?.name.orEmpty()
        val model = state.models.firstOrNull {
            it.id == state.modelID && it.providerID == state.modelProviderID
        }?.name.orEmpty()
        val subtitle = listOf(agent, model)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        val steps = state.messages.flatMap { it.parts }
            .count { it is Part.Tool || it is Part.Reasoning }
        val editingTools = setOf(
            "edit", "write", "patch", "multiedit", "apply_patch",
            "create_file", "str_replace_editor"
        )
        val editedFiles = state.messages.flatMap { it.parts }
            .filterIsInstance<Part.Tool>()
            .filter { it.name.lowercase() in editingTools }
            .mapNotNull { part ->
                val obj = runCatching { org.json.JSONObject(part.input) }.getOrNull()
                    ?: return@mapNotNull null
                listOf("filePath", "path", "file", "filename")
                    .firstNotNullOfOrNull { key ->
                        obj.optString(key).takeIf { it.isNotBlank() }
                    }
            }
            .toSet()
            .size
        val start = state.messages.mapNotNull { it.created.takeIf { c -> c > 0 } }.minOrNull()
        val end = state.messages.mapNotNull { it.completed.takeIf { c -> c > 0 } }.maxOrNull()
        val elapsed = start?.let {
            formatElapsed(((end ?: System.currentTimeMillis()) - it).coerceAtLeast(0L))
        }
        val stats = buildString {
            if (elapsed != null) append(elapsed)
            if (steps > 0) {
                if (isNotEmpty()) append(" · ")
                append(steps).append(if (steps == 1) " step" else " steps")
            }
            if (editedFiles > 0) {
                if (isNotEmpty()) append(" · ")
                append(editedFiles).append(if (editedFiles == 1) " file" else " files")
            }
            if (state.cost > 0.0) {
                if (isNotEmpty()) append(" · ")
                append(String.format(java.util.Locale.US, "$%.2f", state.cost))
            }
        }
        val quote = state.messages.lastOrNull { it.role == "assistant" }
            ?.plainText.orEmpty().trim()
        val footer = "opencode mobile · " + java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern(
                "d MMM yyyy",
                java.util.Locale.getDefault()
            )
        )
        scope.launch(Dispatchers.IO) {
            val bitmap = runCatching {
                renderRunCard(title, subtitle, stats, quote, footer, lightCard)
            }.getOrNull() ?: return@launch
            val dir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
            val file = java.io.File(dir, "run-${System.currentTimeMillis()}.png")
            runCatching {
                java.io.FileOutputStream(file).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
            val uri = runCatching {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }.getOrNull() ?: return@launch
            withContext(Dispatchers.Main) {
                runCatching {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            send,
                            context.getString(R.string.chat_share_card)
                        )
                    )
                }
            }
        }
    }

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

    LaunchedEffect(dragging) {
        if (dragging) stickToBottom = false
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.canScrollForward }.collect { canForward ->
            if (!canForward) stickToBottom = true
        }
    }

    // Jump straight to the newest message. Animating across dozens of items is what
    // left the view stranded mid-history on long sessions.
    LaunchedEffect(state.messages.size) {
        if (!initialPositioned && blocks.isNotEmpty()) {
            snapToNewest()
            initialPositioned = true
        }
    }

    LaunchedEffect(state.messages) {
        if (!initialPositioned || blocks.isEmpty() || !stickToBottom) return@LaunchedEffect
        snapToNewest()
    }

    LaunchedEffect(lastId) {
        if (state.messages.lastOrNull()?.role == "user") {
            stickToBottom = true
            snapToNewest()
        }
    }

    LaunchedEffect(blocks, restoreKey) {
        val key = restoreKey ?: return@LaunchedEffect
        val index = blocks.indexOfFirst { it.key == key }
        if (index >= 0) {
            runCatching { listState.scrollToItem(index) }
            restoreKey = null
        }
    }

    // Running shells live outside the message stream: a command can outlive the
    // turn that started it. Poll the workspace list and surface it by the composer.
    LaunchedEffect(state.directory, shellRefreshKey) {
        if (state.directory.isBlank()) return@LaunchedEffect
        while (true) {
            runningShells = runCatching { repo.runningShells(state.directory) }
                .getOrDefault(emptyList())
            delay(3_000)
        }
    }

    LaunchedEffect(sessionID) {
        val id = sessionID ?: return@LaunchedEffect
        while (true) {
            runningJobs = runCatching { repo.jobs(id) }.getOrDefault(emptyList())
            delay(5_000)
        }
    }

    val planTodos = remember(state.messages) {
        state.messages.asReversed()
            .flatMap { it.parts }
            .filterIsInstance<Part.Tool>()
            .firstNotNullOfOrNull { parseTodos(it.input) }
    }
    val planDone = planTodos?.count { it.status == "completed" } ?: 0
    val contextFraction = if (state.contextLimit > 0) {
        (state.contextUsed.toFloat() / state.contextLimit).coerceIn(0f, 1f)
    } else {
        0f
    }

    LaunchedEffect(listState.firstVisibleItemIndex, state.olderCursor) {
        if (state.messages.isNotEmpty() &&
            state.olderCursor != null &&
            listState.firstVisibleItemIndex <= 2 &&
            !state.loadingOlder
        ) {
            // Remember what the reader was looking at; the request can add many
            // messages but only a few lazy items. Day pills are rebuilt from the
            // oldest loaded message, so they are not stable anchors.
            val anchor = blocks.drop(listState.firstVisibleItemIndex)
                .firstOrNull { it !is ChatBlock.DaySeparator }
                ?.key
            vm.loadOlder { added ->
                if (added > 0) restoreKey = anchor
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (sessionID == null) stringResource(R.string.session_new) else state.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable(enabled = sessionID != null) { showOverview = true }
                        )
                        if (state.busy) {
                            Spacer(Modifier.width(8.dp))
                            ActivityBars(barWidth = 2.dp, maxHeight = 9.dp)
                        }
                    }
                    val agentName = state.agents.firstOrNull { it.id == state.agentID }?.name
                        .orEmpty()
                    val modelName = state.models.firstOrNull {
                        it.id == state.modelID && it.providerID == state.modelProviderID
                    }?.name.orEmpty()
                    val subtitle = listOf(agentName, modelName)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                                text = { Text(stringResource(R.string.chat_changes)) },
                                onClick = {
                                    menuOpen = false
                                    showDiff = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_share_transcript)) },
                                onClick = {
                                    menuOpen = false
                                    val body = buildString {
                                        appendLine("# ${state.title}")
                                        state.messages.forEach { msg ->
                                            val text = msg.plainText.trim()
                                            if (text.isNotEmpty()) {
                                                appendLine()
                                                appendLine(
                                                    if (msg.role == "user") "**You**" else "**Agent**"
                                                )
                                                appendLine()
                                                appendLine(text)
                                            }
                                        }
                                    }
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, state.title)
                                        putExtra(Intent.EXTRA_TEXT, body)
                                    }
                                    runCatching {
                                        context.startActivity(
                                            Intent.createChooser(
                                                send,
                                                context.getString(R.string.chat_share_transcript)
                                            )
                                        )
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_share_card)) },
                                onClick = {
                                    menuOpen = false
                                    shareRunCard()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_branch_session)) },
                                onClick = {
                                    menuOpen = false
                                    vm.fork(null) { child -> onSessionCreated(child) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_regenerate)) },
                                enabled = !state.busy && state.messages.any { it.role == "user" },
                                onClick = {
                                    menuOpen = false
                                    confirmRegenerate = true
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
                                text = {
                                    Text(
                                        if (showTools) "Hide tool calls"
                                        else "Show tool calls"
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    UiPrefs.setShowTools(context, !showTools)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_project)) },
                                onClick = {
                                    menuOpen = false
                                    showProject = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_workspace)) },
                                onClick = {
                                    menuOpen = false
                                    showWorkspace = true
                                }
                            )
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
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom)
            ) {
                itemsIndexed(blocks, key = { _, block -> block.key }) { index, block ->
                    Box(
                        Modifier.animateItem(
                            fadeInSpec = tween(200),
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            fadeOutSpec = tween(140)
                        )
                    ) {
                        Column {
                        if (index == unreadBlockIndex && unread.isNotEmpty()) {
                            UnreadSeparator()
                        }
                        when (block) {
                            is ChatBlock.DaySeparator -> DayPill(block.at)
                            is ChatBlock.UserBlock -> UserMessage(
                                message = block.message,
                                onBranch = { before ->
                                    vm.fork(before) { child -> onSessionCreated(child) }
                                },
                                onQuote = { text ->
                                    input = text.lineSequence()
                                        .joinToString("\n") { "> $it" } + "\n\n"
                                },
                                onEdit = {
                                    val body = block.message.plainText
                                    vm.fork(block.message.id) { child ->
                                        DraftStore.save(context, child, body)
                                        onSessionCreated(child)
                                    }
                                }
                            )
                            is ChatBlock.TurnBlock -> TurnMessage(
                                messages = block.messages,
                                directory = state.directory,
                                showTools = showTools,
                                onSpeak = speak,
                                onFork = { before ->
                                    vm.fork(before) { child -> onSessionCreated(child) }
                                },
                                onRegenerate = if (block.key == blocks.lastOrNull()?.key && !state.busy) {
                                    { vm.regenerateLast { prompt -> input = prompt } }
                                } else {
                                    null
                                }
                            )
                        }
                        }
                    }
                }
                val assistant = lastAssistant
                if (showRunSummary && assistant != null) {
                    item(key = "run-summary-${assistant.id}") {
                        RunSummaryCard(
                            assistant = assistant,
                            user = lastUser,
                            tools = lastTools,
                            onChanges = { showDiff = true },
                            onShare = { shareRunCard() },
                            onKeep = { dismissedRunId = assistant.id },
                            onRevert = { confirmUndo = true },
                            onContinue = {
                                input = "Continue from where you left off."
                                dismissedRunId = assistant.id
                            },
                            onDismiss = { dismissedRunId = assistant.id }
                        )
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
                        scope.launch {
                            stickToBottom = true
                            snapToNewest()
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

            // The strip is an overlay, not a sibling above the composer: giving it
            // layout height would shrink the list whenever a run starts or stops,
            // and a list sitting at its end would lose its last lines and show the
            // jump-to-latest button. The list reserves 40dp for it instead.
            CompletionFlash(
                tick = completionTick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 8.dp)
            )

            if (blocks.isEmpty() && !state.busy) {
                EmptyChatMark(Modifier.align(Alignment.Center))
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { vm.steerQueued(state.queued.first().id) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            stringResource(R.string.chat_queue_steer),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
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
            state.error?.let { message ->
                PulsingError(message)
            }
            if (state.offlineQueued > 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.flushPending() }
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
                        stringResource(R.string.chat_offline_queued, state.offlineQueued),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            val hasChips = runningShells.isNotEmpty() ||
                runningJobs.isNotEmpty() || planTodos != null || contextFraction >= 0.6f
            if (hasChips) {
                FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (runningShells.isNotEmpty() || runningJobs.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (runningShells.isNotEmpty()) {
                                    Row(
                                        Modifier
                                            .clickable { showShell = true }
                                            .padding(
                                                start = 12.dp,
                                                end = if (runningJobs.isEmpty()) 12.dp else 8.dp,
                                                top = 7.dp,
                                                bottom = 7.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PulsingDot(
                                            color = MaterialTheme.colorScheme.primary,
                                            pulsing = true,
                                            size = 6
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            "${runningShells.size} " +
                                                if (runningShells.size == 1) "shell"
                                                else "shells",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                if (runningShells.isNotEmpty() && runningJobs.isNotEmpty()) {
                                    Box(
                                        Modifier
                                            .width(1.dp)
                                            .height(16.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                }
                                if (runningJobs.isNotEmpty()) {
                                    Row(
                                        Modifier
                                            .clickable { showWorkspace = true }
                                            .padding(
                                                start = if (runningShells.isEmpty()) 12.dp else 8.dp,
                                                end = 12.dp,
                                                top = 7.dp,
                                                bottom = 7.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (runningShells.isEmpty()) {
                                            PulsingDot(
                                                color = MaterialTheme.colorScheme.primary,
                                                pulsing = true,
                                                size = 6
                                            )
                                            Spacer(Modifier.width(7.dp))
                                        }
                                        Text(
                                            "${runningJobs.size} " +
                                                if (runningJobs.size == 1) "job"
                                                else "jobs",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (planTodos != null) {
                        AssistChip(
                            onClick = { showOverview = true },
                            label = { Text("$planDone/${planTodos.size} steps") }
                        )
                    }
                    if (contextFraction >= 0.6f) {
                        AssistChip(
                            onClick = { showOverview = true },
                            label = { Text("${(contextFraction * 100).toInt()}% context") }
                        )
                    }
                }
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
                delivery = if (state.busy) {
                    if (steerMode) "steer" else "queue"
                } else {
                    "steer"
                },
                onDeliveryChange = { mode -> steerMode = mode == "steer" },
                status = {
                    if (state.busy) {
                        ComposerStatus(
                            messages = state.messages,
                            onBackground = { vm.background() }
                        )
                    }
                },
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
                            skillIDs = pickedSkills.map { it.id },
                            delivery = if (state.busy) {
                                if (steerMode) "steer" else "queue"
                            } else {
                                null
                            }
                        ) { ok ->
                            if (!ok) {
                                input = text
                                DraftStore.save(context, sessionID, text)
                            } else {
                                steerMode = true
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
                onAttachFile = { runCatching { fileLauncher.launch(arrayOf("*/*")) } },
                onCamera = { takePhoto() }
            )
        }
    }

    state.pendingPermission?.let { p ->
        PermissionSheet(permission = p, onDecision = { decision ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.reply(decision)
        })
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
            directory = state.directory,
            onDismiss = { showShell = false },
            onShellsChanged = { shellRefreshKey++ }
        )
    }

    if (showWorkspace && sessionID != null) {
        WorkspaceSheet(
            repo = repo,
            sessionID = sessionID,
            directory = state.directory,
            projectID = state.projectID,
            cost = state.cost,
            tokensInput = state.tokensInput,
            tokensOutput = state.tokensOutput,
            messages = state.messages,
            onDismiss = { showWorkspace = false },
            onImported = { imported ->
                showWorkspace = false
                onSessionCreated(imported)
            }
        )
    }

    if (showProject && sessionID != null) {
        ProjectSheet(
            repo = repo,
            directory = state.directory,
            projectID = state.projectID,
            onDismiss = { showProject = false }
        )
    }

    if (showOverview && sessionID != null) {
        OverviewSheet(
            repo = repo,
            sessionID = sessionID,
            state = state,
            onDismiss = { showOverview = false },
            onChanges = {
                showOverview = false
                showDiff = true
            },
            onWorkspace = {
                showOverview = false
                showWorkspace = true
            }
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

    if (confirmRegenerate) {
        AlertDialog(
            onDismissRequest = { confirmRegenerate = false },
            title = { Text(stringResource(R.string.chat_regenerate_title)) },
            text = { Text(stringResource(R.string.chat_regenerate_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRegenerate = false
                    vm.regenerateLast { prompt ->
                        vm.send(prompt, emptyList()) { }
                    }
                }) { Text(stringResource(R.string.chat_regenerate_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRegenerate = false }) {
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
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var truncated by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(path) {
        try {
            val raw = repo.readFile(path, directory)
            truncated = raw.length > 120_000
            content = if (truncated) raw.take(120_000) else raw
            draft = content.orEmpty()
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    path.replace('\\', '/').substringAfterLast('/'),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                if (!editing && content != null && !truncated) {
                    TextButton(onClick = {
                        draft = content.orEmpty()
                        editing = true
                        note = null
                    }) {
                        Text(stringResource(R.string.action_edit))
                    }
                }
                if (editing) {
                    TextButton(
                        enabled = !saving,
                        onClick = {
                            saving = true
                            scope.launch {
                                try {
                                    repo.writeFile(path, directory, draft)
                                    content = draft
                                    editing = false
                                    note = "Saved"
                                    error = null
                                } catch (e: Exception) {
                                    error = friendlyError(e)
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                    TextButton(
                        enabled = !saving,
                        onClick = {
                            editing = false
                            draft = content.orEmpty()
                        }
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
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

                editing -> OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 520.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    ),
                    maxLines = 2000
                )

                else -> Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (truncated) {
                        Text(
                            stringResource(R.string.file_view_only),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (note != null) {
                        Text(
                            note!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var tab by remember { mutableStateOf(0) }
    var modelQuery by remember { mutableStateOf("") }

    val currentAgent = state.agents.firstOrNull { it.id == state.agentID }?.name
        ?: state.agentID.ifBlank { "—" }
    val currentModel = state.models.firstOrNull {
        it.id == state.modelID && it.providerID == state.modelProviderID
    }?.name ?: state.modelID.ifBlank { "—" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            TuneHeader(
                agent = currentAgent,
                model = currentModel,
                loading = state.selecting,
                onDismiss = onDismiss
            )
            TuneSwitch(selected = tab, onSelect = { tab = it })
            if (tab == 0) {
                TuneSearch(query = modelQuery, onQuery = { modelQuery = it })
            } else {
                Text(
                    stringResource(R.string.chat_agent_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 12.dp,
                        bottom = 2.dp
                    )
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (tab == 0) {
                    ModelsTab(
                        models = state.models,
                        selectedID = state.modelID,
                        selectedProvider = state.modelProviderID,
                        query = modelQuery,
                        enabled = !state.selecting,
                        onSelect = onModel
                    )
                } else {
                    AgentsTab(
                        agents = state.agents,
                        selectedID = state.agentID,
                        enabled = !state.selecting,
                        presets = presets,
                        onSelect = onAgent,
                        onApplyPreset = onApplyPreset,
                        onDeletePreset = onDeletePreset
                    )
                }
            }
            TuneFooter(state = state, onCompact = onCompact)
        }
    }
}

@Composable
private fun TuneHeader(
    agent: String,
    model: String,
    loading: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 2.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.chat_model_agent),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "$agent · $model",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(role = Role.Button, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.action_cancel),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    if (loading) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(2.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
    }
}

@Composable
private fun TuneSwitch(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        TuneSegment(
            label = stringResource(R.string.chat_models),
            active = selected == 0,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(0) }
        )
        TuneSegment(
            label = stringResource(R.string.chat_agents),
            active = selected == 1,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(1) }
        )
    }
}

@Composable
private fun TuneSegment(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "tuneSegment"
    )
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TuneSearch(query: String, onQuery: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQuery,
        placeholder = {
            Text(
                stringResource(R.string.chat_search_models),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = if (query.isNotBlank()) {
            {
                IconButton(onClick = { onQuery("") }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun ModelsTab(
    models: List<ModelOption>,
    selectedID: String,
    selectedProvider: String,
    query: String,
    enabled: Boolean,
    onSelect: (String, String) -> Unit
) {
    val filtered = remember(models, query, selectedID, selectedProvider) {
        models
            .filter { model ->
                query.isBlank() ||
                    model.name.contains(query, ignoreCase = true) ||
                    model.id.contains(query, ignoreCase = true) ||
                    model.providerID.contains(query, ignoreCase = true) ||
                    model.providerName.contains(query, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<ModelOption> {
                    it.id == selectedID && it.providerID == selectedProvider
                }.thenBy { it.providerName.ifBlank { it.providerID } }
                    .thenBy { it.name }
            )
    }
    val groups = remember(filtered) {
        filtered.groupBy { it.providerName.ifBlank { it.providerID } }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (filtered.isEmpty()) {
            item { EmptyListHint() }
        }
        groups.forEach { (provider, list) ->
            item(key = "provider:$provider") {
                TuneSectionLabel(provider, list.size)
            }
            items(list, key = { "${it.providerID}:${it.id}" }) { model ->
                TuneRow(
                    title = model.name,
                    meta = buildString {
                        append(
                            stringResource(
                                R.string.chat_context_caption,
                                if (model.contextLimit > 0) {
                                    compactTokens(model.contextLimit)
                                } else {
                                    "—"
                                }
                            )
                        )
                        if (model.family.isNotBlank()) append(" · ").append(model.family)
                    },
                    monogram = monogram(model.providerName.ifBlank { model.providerID }),
                    selected = model.id == selectedID && model.providerID == selectedProvider,
                    enabled = enabled,
                    onClick = { onSelect(model.providerID, model.id) },
                    tint = providerAccent(model.providerID.ifBlank { model.providerName })
                )
            }
        }
    }
}

@Composable
private fun AgentsTab(
    agents: List<AgentOption>,
    selectedID: String,
    enabled: Boolean,
    presets: List<SessionPreset>,
    onSelect: (String) -> Unit,
    onApplyPreset: (SessionPreset) -> Unit,
    onDeletePreset: (SessionPreset) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(agents, key = { "agent:${it.id}" }) { agent ->
            TuneRow(
                title = agent.name,
                meta = agent.description.takeIf { it.isNotBlank() },
                metaLines = 2,
                monogram = monogram(agent.name),
                selected = agent.id == selectedID,
                enabled = enabled,
                onClick = { onSelect(agent.id) }
            )
        }
        if (presets.isNotEmpty()) {
            item(key = "presets") {
                TuneSectionLabel(stringResource(R.string.chat_presets), presets.size)
            }
            items(presets, key = { "preset:${it.name}:${it.agentID}:${it.modelID}" }) { preset ->
                PresetRow(
                    preset = preset,
                    onApply = { onApplyPreset(preset) },
                    onDelete = { onDeletePreset(preset) }
                )
            }
        }
    }
}

@Composable
private fun TuneSectionLabel(label: String, count: Int? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.9.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun TuneRow(
    title: String,
    meta: String?,
    monogram: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    metaLines: Int = 1,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "tuneRow"
    )
    val contentAlpha = if (enabled) 1f else 0.45f
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = if (selected) 0.18f else 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                monogram,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!meta.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    maxLines = metaLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        SelectionDot(selected = selected, enabled = enabled)
    }
}

@Composable
private fun SelectionDot(selected: Boolean, enabled: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.86f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "selectionDot"
    )
    Box(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(22.dp)
            .clip(CircleShape)
            .background(
                if (selected) accent.copy(alpha = if (enabled) 1f else 0.4f) else Color.Transparent
            )
            .then(
                if (selected) Modifier
                else Modifier.border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun EmptyListHint() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.sessions_no_match),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PresetRow(
    preset: SessionPreset,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onApply)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Tune,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(preset.name, style = MaterialTheme.typography.titleSmall)
            Text(
                listOf(preset.agentID, preset.modelID)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TuneFooter(state: ChatUiState, onCompact: () -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (state.contextUsed > 0L) {
            val limit = state.contextLimit
            val ratio = if (limit > 0) {
                (state.contextUsed.toFloat() / limit).coerceIn(0f, 1f)
            } else {
                0f
            }
            val overBudget = ratio > 0.85f
            val accent = when {
                overBudget -> MaterialTheme.colorScheme.error
                ratio > 0.65f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.chat_context),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (overBudget) accent else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (ratio > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth(ratio)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
            }
        }

        if (state.tokensInput > 0L || state.tokensOutput > 0L || state.cost > 0.0) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.chat_tokens_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    buildString {
                        append(compactTokens(state.tokensInput)).append(" in")
                        if (state.tokensOutput > 0L) {
                            append(" · ").append(compactTokens(state.tokensOutput)).append(" out")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.chat_spend_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (state.cost > 0.0) {
                        String.format(java.util.Locale.getDefault(), "$%.2f", state.cost)
                    } else {
                        stringResource(R.string.chat_cost_not_reported)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        CompactAction(enabled = !state.busy, onCompact = onCompact)

        state.error?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CompactAction(enabled: Boolean, onCompact: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onCompact)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.UnfoldLess,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.chat_compact),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f)
            )
            Text(
                stringResource(R.string.chat_compact_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.5f
                )
            )
        }
    }
}

/** Deterministic accent per provider so grouped models are scannable at a glance. */
@Composable
private fun providerAccent(key: String): Color {
    val hue = ((key.hashCode() % 360) + 360) % 360
    return Color.hsl(
        hue.toFloat(),
        saturation = 0.52f,
        lightness = if (isSystemInDarkTheme()) 0.72f else 0.44f
    )
}

private fun monogram(value: String): String {
    val parts = value.trim().split(' ', '-', '_', '.', ':').filter { it.isNotEmpty() }
    val letters = parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
    return if (letters.isEmpty()) "?" else letters.joinToString("")
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SchedulesSheet(
    repo: OpenCodeRepository,
    sessionID: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    val configLine = stringResource(R.string.chat_schedules_disabled_snippet)
    var items by remember { mutableStateOf<List<ScheduleItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var disabled by remember { mutableStateOf(false) }
    var useAutomations by remember { mutableStateOf(true) }
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
                items = repo.automations(sessionID)
                useAutomations = true
                error = null
                disabled = false
            } catch (e: Exception) {
                // OpenCode before 2.4 has no automation API; keep those servers working.
                if (e is ApiException && (e.status == 404 || e.status == 405)) {
                    try {
                        items = repo.schedules(sessionID)
                        useAutomations = false
                        error = null
                        disabled = false
                    } catch (legacy: Exception) {
                        error = friendlyError(legacy)
                        disabled = legacy is SchedulesDisabledException
                        items = emptyList()
                    }
                } else {
                    error = friendlyError(e)
                    disabled = false
                    items = emptyList()
                }
            }
        }
    }

    LaunchedEffect(sessionID) { reload() }

    // Back closes the keyboard first, then the sheet. Without taking the back
    // press here the sheet's dialog would dismiss on the first tap while typing.
    BackHandler {
        if (imeVisible) {
            keyboard?.hide()
            focusManager.clearFocus()
        } else {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
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
                    placeholder = { Text("2026-12-01T09:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val whenReady = when (mode) {
                "interval" -> true
                "cron" -> cron.isNotBlank()
                else -> at.isNotBlank()
            }
            if (!useAutomations) {
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
            }
            Button(
                onClick = {
                    if (prompt.isNotBlank() && whenReady) {
                        scope.launch {
                            try {
                                if (useAutomations) {
                                    val trigger = when (mode) {
                                        "cron" -> org.json.JSONObject()
                                            .put("type", "cron")
                                            .put("cron", cron.trim())
                                        "at" -> org.json.JSONObject()
                                            .put("type", "at")
                                            .put("at", at.trim())
                                        else -> org.json.JSONObject()
                                            .put("type", "every")
                                            .put("every", "$minutes minutes")
                                    }
                                    repo.createAutomation(sessionID, prompt.trim(), trigger)
                                } else {
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
                                }
                                prompt = ""
                                endsTimes = ""
                                ifRun = ""
                                keyboard?.hide()
                                focusManager.clearFocus()
                                reload()
                            } catch (e: Exception) {
                                error = friendlyError(e)
                                if (e is SchedulesDisabledException) disabled = true
                            }
                        }
                    }
                },
                enabled = prompt.isNotBlank() && whenReady && !disabled,
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
            if (disabled) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            configLine,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { clipboard.setText(AnnotatedString(configLine)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.chat_schedules_copy))
                            }
                            TextButton(
                                onClick = { reload() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.chat_schedules_recheck))
                            }
                        }
                    }
                }
            }
            when {
                items == null -> CircularProgressIndicator(Modifier.size(24.dp))
                items!!.isEmpty() && error == null -> Text(
                    stringResource(R.string.chat_schedule_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> items!!.forEach { schedule ->
                    val paused = schedule.status.contains("pause", ignoreCase = true)
                    val whenText = when (schedule.whenType) {
                        "every" -> stringResource(
                            R.string.chat_schedule_when_every,
                            compactInterval(schedule.whenValue)
                        )
                        "cron" -> stringResource(R.string.chat_schedule_when_cron, schedule.whenValue)
                        "at" -> stringResource(R.string.chat_schedule_when_at, schedule.whenValue)
                        "in" -> stringResource(R.string.chat_schedule_when_in, schedule.whenValue)
                        else -> ""
                    }
                    val millisLeft = schedule.nextDueAt - System.currentTimeMillis()
                    val meta = mutableListOf<String>()
                    if (whenText.isNotBlank()) meta.add(whenText)
                    if (schedule.status.isNotBlank()) {
                        meta.add(schedule.status.replaceFirstChar { it.uppercase() })
                    }
                    if (millisLeft > 0) meta.add("+${millisLeft / 60_000}m")
                    if (schedule.remainingRuns > 0) {
                        meta.add(stringResource(R.string.chat_schedule_runs_left, schedule.remainingRuns))
                    }
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
                                    meta.joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (schedule.condition.isNotBlank()) {
                                    Text(
                                        stringResource(
                                            R.string.chat_schedule_condition,
                                            schedule.condition
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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
                                        runCatching {
                                            if (useAutomations) {
                                                repo.setAutomationPaused(schedule.id, !paused)
                                            } else {
                                                repo.setSchedulePaused(schedule.id, !paused)
                                            }
                                        }
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
                                        runCatching {
                                            if (useAutomations) {
                                                repo.deleteAutomation(schedule.id)
                                            } else {
                                                repo.deleteSchedule(schedule.id)
                                            }
                                        }
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

/** "60 minutes" -> "1h", "1440 minutes" -> "1d"; free text is kept as-is. */
private fun compactInterval(value: String): String {
    val match = Regex("^(\\d+)\\s*(second|minute|hour|day)s?$", RegexOption.IGNORE_CASE)
        .find(value.trim()) ?: return value
    val amount = match.groupValues[1].toLongOrNull()?.takeIf { it > 0 } ?: return value
    val seconds = when (match.groupValues[2].lowercase()) {
        "second" -> amount
        "minute" -> amount * 60
        "hour" -> amount * 3_600
        else -> amount * 86_400
    }
    return when {
        seconds % 86_400 == 0L -> "${seconds / 86_400}d"
        seconds % 3_600 == 0L -> "${seconds / 3_600}h"
        seconds % 60 == 0L -> "${seconds / 60}m"
        else -> "${seconds}s"
    }
}

@Composable
private fun ShellsPill(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        label = { Text(pluralStringResource(R.plurals.chat_shells_count, count, count)) },
        leadingIcon = {
            PulsingDot(color = MaterialTheme.colorScheme.primary, pulsing = true, size = 6)
        },
        modifier = modifier
    )
}

/** "5:54" under an hour, "1:02:03" above it. */
private fun elapsedLabel(startedAt: Long): String {
    if (startedAt <= 0L) return ""
    val total = ((System.currentTimeMillis() - startedAt).coerceAtLeast(0L)) / 1_000
    return if (total >= 3_600) {
        "%d:%02d:%02d".format(total / 3_600, (total % 3_600) / 60, total % 60)
    } else {
        "%d:%02d".format(total / 60, total % 60)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ShellSheet(
    repo: OpenCodeRepository,
    sessionID: String,
    directory: String,
    onDismiss: () -> Unit,
    onShellsChanged: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    var command by remember { mutableStateOf("") }
    var shells by remember { mutableStateOf<List<RunningShell>>(emptyList()) }
    var selectedID by remember { mutableStateOf<String?>(null) }
    var output by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refreshShells() {
        scope.launch {
            runCatching { repo.runningShells(directory) }
                .onSuccess { shells = it }
        }
    }

    LaunchedEffect(directory) {
        while (true) {
            runCatching { repo.runningShells(directory) }
                .onSuccess { shells = it }
            delay(2_000)
        }
    }

    val selected = shells.firstOrNull { it.id == selectedID }
    LaunchedEffect(selectedID, selected == null) {
        val id = selectedID ?: return@LaunchedEffect
        while (true) {
            output = runCatching { repo.shellOutput(id, directory) }
                .getOrDefault(output ?: "")
            if (selected == null) break
            delay(1_500)
        }
    }

    fun run() {
        if (command.isBlank() || running) return
        running = true
        error = null
        scope.launch {
            try {
                val shell = repo.runShell(sessionID, directory, command.trim())
                command = ""
                keyboard?.hide()
                focusManager.clearFocus()
                selectedID = shell.id
                output = runCatching { repo.shellOutput(shell.id, directory) }
                    .getOrDefault("")
                onShellsChanged()
                refreshShells()
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                running = false
            }
        }
    }

    fun stop(id: String) {
        scope.launch {
            runCatching { repo.stopShell(id, directory) }
            if (selectedID == id) {
                selectedID = null
                output = null
            }
            onShellsChanged()
            refreshShells()
        }
    }

    BackHandler {
        if (imeVisible) {
            keyboard?.hide()
            focusManager.clearFocus()
        } else {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
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
                stringResource(R.string.chat_shells_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                stringResource(R.string.chat_shells_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (shells.isEmpty()) {
                Text(
                    stringResource(R.string.chat_shells_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.chat_shells_running),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                shells.forEach { shell ->
                    Surface(
                        color = if (shell.id == selectedID) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedID = shell.id }
                    ) {
                        Row(
                            Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PulsingDot(
                                color = MaterialTheme.colorScheme.primary,
                                pulsing = true,
                                size = 6
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                shell.command.replace('\n', ' '),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                elapsedLabel(shell.startedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { stop(shell.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Stop,
                                    contentDescription = stringResource(R.string.action_stop),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Text(
                stringResource(R.string.chat_shell_run_hint),
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
            Button(
                onClick = { run() },
                enabled = command.isNotBlank() && !running,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.chat_shell_run))
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
                    if (selected != null) {
                        Text(
                            selected.command.replace('\n', ' '),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        output ?: if (running || selected != null) {
                            stringResource(R.string.chat_shell_running)
                        } else {
                            stringResource(R.string.chat_shell_output)
                        },
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
private fun UserMessage(
    message: Msg,
    onBranch: (String) -> Unit,
    onQuote: (String) -> Unit,
    onEdit: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val text = message.parts.filterIsInstance<Part.Text>()
        .joinToString("\n\n") { it.text }
        .trim()

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    else -> Unit
                }
            }
            if (message.id.isNotBlank()) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.chat_message_actions),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        if (text.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_copy_message)) },
                                onClick = {
                                    menuOpen = false
                                    clipboard.setText(AnnotatedString(text))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_quote_message)) },
                                onClick = {
                                    menuOpen = false
                                    onQuote(text)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_edit_resend)) },
                                onClick = {
                                    menuOpen = false
                                    onEdit()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_branch_from_here)) },
                            onClick = {
                                menuOpen = false
                                onBranch(message.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

private sealed interface ChatBlock {
    val key: String

    data class DaySeparator(val at: Long) : ChatBlock {
        override val key: String = "day-$at"
    }

    data class UserBlock(val message: Msg) : ChatBlock {
        override val key: String = message.id
    }

    data class TurnBlock(val messages: List<Msg>) : ChatBlock {
        override val key: String = messages.first().id
    }
}

private fun dayOf(ts: Long): LocalDate? {
    if (ts <= 0L) return null
    return runCatching {
        Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
}

private fun buildChatBlocks(messages: List<Msg>): List<ChatBlock> {
    val blocks = mutableListOf<ChatBlock>()
    val turn = mutableListOf<Msg>()
    var lastDay: LocalDate? = null

    fun flush() {
        if (turn.isNotEmpty()) {
            blocks += ChatBlock.TurnBlock(turn.toList())
            turn.clear()
        }
    }

    messages.forEach { message ->
        val day = dayOf(message.created)
        if (day != null && day != lastDay) {
            flush()
            blocks += ChatBlock.DaySeparator(message.created)
            lastDay = day
        }
        if (message.role == "user") {
            flush()
            blocks += ChatBlock.UserBlock(message)
        } else {
            turn += message
        }
    }
    flush()
    return blocks
}

@Composable
private fun DayPill(at: Long) {
    val day = remember(at) { dayOf(at) } ?: return
    val today = remember { LocalDate.now() }
    val label = when (day) {
        today -> stringResource(R.string.chat_day_today)
        today.minusDays(1) -> stringResource(R.string.chat_day_yesterday)
        else -> remember(day) { day.format(DateTimeFormatter.ofPattern("d MMM")) }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CircleShape
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}


@Composable
private fun TurnMessage(
    messages: List<Msg>,
    directory: String,
    showTools: Boolean,
    onSpeak: (String) -> Unit,
    onFork: (String) -> Unit,
    onRegenerate: (() -> Unit)?
) {
    val pending = messages.lastOrNull()?.pending == true
    val lastAssistantId = messages.lastOrNull { it.role == "assistant" }?.id.orEmpty()
    val entries = remember(messages) { buildTurnEntries(messages) }
    val fullText = remember(entries) {
        entries.filterIsInstance<TurnEntry.Text>().joinToString("\n\n") { it.body }
    }
    val lastTextIndex = entries.indexOfLast { it is TurnEntry.Text }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entries.forEachIndexed { index, entry ->
            when (entry) {
                is TurnEntry.Steps -> if (showTools) {
                    StepLine(
                        parts = entry.parts,
                        directory = directory,
                        pending = pending
                    )
                }

                is TurnEntry.Text -> SelectionContainer {
                    // Intermediate narration stays quieter; the latest block is the answer.
                    // A block cursor rides the streaming text like a real terminal.
                    val body = if (pending && index == lastTextIndex) {
                        entry.body.trimEnd() + " ▍"
                    } else {
                        entry.body
                    }
                    MarkdownText(
                        body,
                        contentAlpha = if (index == lastTextIndex) 1f else 0.72f
                    )
                }
            }
        }
        if (!pending && fullText.isNotBlank()) {
            val clipboard = LocalClipboardManager.current
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = { onSpeak(fullText) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Rounded.VolumeUp,
                        contentDescription = stringResource(R.string.chat_read_aloud),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(fullText)) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.chat_copy_message),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(17.dp)
                    )
                }
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.chat_message_actions),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        if (lastAssistantId.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_branch_from_here)) },
                                onClick = {
                                    menuOpen = false
                                    onFork(lastAssistantId)
                                }
                            )
                        }
                        if (onRegenerate != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_regenerate)) },
                                onClick = {
                                    menuOpen = false
                                    onRegenerate()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One run of consecutive tools and thoughts, reported the way the TUI does it:
 * "Used Shell", "Used 2 Write, Shell", "Used 1 Thought". Tapping opens the
 * detailed steps in place.
 */
@Composable
private fun StepLine(
    parts: List<Part>,
    directory: String,
    pending: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val summary = stepSummary(parts)
    val running = pending && parts.any {
        it is Part.Tool && (it.status == "running" || it.status == "streaming")
    }
    val expandLabel = stringResource(
        if (expanded) R.string.chat_activity_collapse else R.string.chat_activity_expand
    )
    val toggleLabel = stringResource(R.string.chat_activity_toggle, summary, expandLabel)

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button) { expanded = !expanded }
                .semantics { contentDescription = toggleLabel }
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveSpine(
                active = running,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.width(18.dp).fillMaxHeight()
            )
            Text(
                summary,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 9.dp)
            )
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(15.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp)
            ) {
                parts.forEachIndexed { index, part ->
                    when (part) {
                        is Part.Tool -> ToolStep(part, directory)
                        is Part.Reasoning -> ThoughtStep(part, pending)
                        else -> Unit
                    }
                    if (index < parts.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * A vertical timeline spine for step runs: a solid line with a node, plus a
 * travelling spark while the run is active.
 */
@Composable
private fun LiveSpine(
    active: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "spine")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinePhase"
    )
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(1.5.dp)
                .background(lineColor)
        )
        if (active) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val x = size.width / 2f
                        drawCircle(
                            color = color.copy(alpha = 0.9f),
                            radius = 2.4.dp.toPx(),
                            center = Offset(x, phase * size.height)
                        )
                    }
            )
        }
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (active) color else color.copy(alpha = 0.85f))
        )
    }
}

/** One soft beat when a run stops: a green check that fades out on its own. */
@Composable
private fun CompletionFlash(tick: Int, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(tick) {
        if (tick == 0) return@LaunchedEffect
        visible = true
        delay(1100)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(140)),
        exit = fadeOut(tween(320))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.chat_turn_done),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/** The quiet personality of an empty chat: a terminal tile with a live caret. */
@Composable
private fun EmptyChatMark(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "emptyChat")
    val caretAlpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(760, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emptyCaret"
    )
    Box(
        modifier
            .size(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ">",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Box(
                Modifier
                    .padding(start = 3.dp)
                    .width(2.dp)
                    .height(14.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = caretAlpha),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
private fun stepSummary(parts: List<Part>): String {
    val thoughts = parts.count { it is Part.Reasoning }
    val tools = parts.filterIsInstance<Part.Tool>()
    val names = tools.map { prettyTool(it.name) }.distinct()
    val shown = names.take(3).joinToString(", ")
    val pieces = buildList {
        when {
            thoughts == 1 -> add(stringResource(R.string.chat_activity_thoughts_one))
            thoughts > 1 -> add(stringResource(R.string.chat_activity_thoughts, thoughts))
        }
        if (tools.size == 1) add(shown)
        else if (tools.size > 1) add("${tools.size} $shown")
    }
    return stringResource(R.string.chat_activity_used, pieces.joinToString(", "))
}


private fun toolIcon(name: String): ImageVector = when (name.lowercase()) {
    "bash", "shell" -> Icons.Rounded.Terminal
    "edit", "write", "patch", "multiedit", "apply_patch", "create_file",
    "str_replace_editor" -> Icons.Rounded.Edit

    "read", "read_file" -> Icons.Rounded.InsertDriveFile
    "grep", "glob", "search" -> Icons.Rounded.Search
    "webfetch", "websearch" -> Icons.Rounded.Public
    "task" -> Icons.Rounded.CallSplit
    else -> Icons.Rounded.Build
}

private fun prettyTool(name: String): String =
    when (name.lowercase()) {
        "todowrite", "todo", "todoread" -> "Plan"
        else -> name.replaceFirstChar { it.uppercase() }
    }

/** Turns a turn span into `12s`, `2m 05s` or `1h 12m`; null below a second. */
/** 1080×1350 share card: dark banner, run title, stats and an answer excerpt. */
private fun renderRunCard(
    title: String,
    subtitle: String,
    stats: String,
    quote: String,
    footer: String,
    light: Boolean
): Bitmap {
    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val background = if (light) {
        intArrayOf(0xFFFBFCFF.toInt(), 0xFFEFF3FF.toInt(), 0xFFE7ECFF.toInt())
    } else {
        intArrayOf(0xFF0B1020.toInt(), 0xFF101A33.toInt(), 0xFF0D1630.toInt())
    }
    val glow = if (light) 0x22FFB86B else 0x38FFB86B
    val titleColor = if (light) 0xFF10142B.toInt() else 0xFFF2F5FF.toInt()
    val subColor = if (light) 0xFF5B6070.toInt() else 0xFFC3CBE3.toInt()
    val statsColor = if (light) 0xFF1B2340.toInt() else 0xFFDCE7FF.toInt()
    val pillColor = if (light) 0x14000000 else 0x22FFFFFF
    val quoteColor = if (light) 0xFF1B2340.toInt() else 0xFFDCE3FF.toInt()
    val footerColor = if (light) 0xFF7A8194.toInt() else 0xFF8B93AD.toInt()
    val brandColor = if (light) 0xFF2B47C7.toInt() else 0xFF8FA8FF.toInt()

    canvas.drawRect(
        0f, 0f, width.toFloat(), height.toFloat(),
        Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                background,
                null,
                Shader.TileMode.CLAMP
            )
        }
    )
    canvas.drawRect(
        0f, 0f, width.toFloat(), height.toFloat(),
        Paint().apply {
            shader = RadialGradient(
                930f, 90f, 760f,
                glow, 0x00FFB86B,
                Shader.TileMode.CLAMP
            )
        }
    )

    canvas.drawRoundRect(
        RectF(80f, 80f, 176f, 176f), 28f, 28f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4A66FF.toInt() }
    )
    val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0A0E18.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val prompt = Path().apply {
        moveTo(110f, 108f)
        lineTo(130f, 128f)
        lineTo(110f, 148f)
    }
    canvas.drawPath(prompt, glyph)
    canvas.drawLine(140f, 148f, 158f, 148f, glyph)

    canvas.drawText(
        "OPENCODE MOBILE",
        206f, 132f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandColor
            textSize = 26f
            letterSpacing = 0.12f
        }
    )

    var y = 290f
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = titleColor
        textSize = 66f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    y = drawCardText(canvas, title, 80f, y, width - 160, titlePaint, 3)
    if (subtitle.isNotBlank()) {
        y += 16f
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subColor
            textSize = 30f
        }
        y = drawCardText(canvas, subtitle, 80f, y, width - 160, subPaint, 1)
    }
    if (stats.isNotBlank()) {
        y += 30f
        val statsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statsColor
            textSize = 26f
        }
        val pad = 28f
        val pillWidth = statsPaint.measureText(stats) + pad * 2
        canvas.drawRoundRect(
            RectF(80f, y, 80f + pillWidth, y + 64f), 32f, 32f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pillColor }
        )
        canvas.drawText(stats, 80f + pad, y + 41f, statsPaint)
        y += 64f
    }
    if (quote.isNotBlank()) {
        y += 40f
        canvas.drawRoundRect(
            RectF(80f, y, 88f, y + 260f), 4f, 4f,
            Paint().apply { color = 0xFF4A66FF.toInt() }
        )
        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = quoteColor
            textSize = 36f
        }
        drawCardText(canvas, quote, 116f, y, width - 200, quotePaint, 7)
    }

    canvas.drawText(
        footer,
        80f, height - 80f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = footerColor
            textSize = 24f
        }
    )
    return bitmap
}

private fun drawCardText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    width: Int,
    paint: TextPaint,
    maxLines: Int
): Float {
    if (text.isBlank()) return y
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.15f)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
    return y + layout.height
}

private fun formatElapsed(ms: Long): String? {
    if (ms < 1000) return null
    val total = ms / 1000
    return when {
        total < 60 -> "${total}s"
        total < 3600 -> "${total / 60}m ${(total % 60).toString().padStart(2, '0')}s"
        else -> "${total / 3600}h ${((total % 3600) / 60).toString().padStart(2, '0')}m"
    }
}

@Composable
private fun statusLabel(status: String): String = when (status) {
    "completed" -> stringResource(R.string.chat_status_completed)
    "error", "failed" -> stringResource(R.string.chat_status_failed)
    "running", "streaming" -> stringResource(R.string.chat_status_running)
    else -> status
}

private data class TodoItem(val text: String, val status: String)

private fun parseTodos(raw: String): List<TodoItem>? {
    if (raw.isBlank()) return null
    val obj = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return null
    val arr = obj.optJSONArray("todos") ?: obj.optJSONArray("items") ?: return null
    if (arr.length() == 0) return null
    val items = (0 until arr.length()).mapNotNull { index ->
        val o = arr.optJSONObject(index) ?: return@mapNotNull null
        val text = o.optString("content")
            .ifBlank { o.optString("title") }
            .ifBlank { o.optString("text") }
        if (text.isBlank()) null else TodoItem(text, o.optString("status"))
    }
    return items.takeIf { it.isNotEmpty() }
}

/** `todowrite` output as an actual checklist instead of a JSON blob. */
@Composable
private fun TodoStep(items: List<TodoItem>) {
    val done = items.count { it.status == "completed" }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.chat_plan),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$done/${items.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items.forEach { item ->
            val completed = item.status == "completed"
            val active = item.status == "in_progress" || item.status == "in-progress"
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    when {
                        completed -> Icons.Rounded.CheckCircle
                        active -> Icons.Rounded.PlayArrow
                        else -> Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = when {
                        completed -> MaterialTheme.colorScheme.secondary
                        active -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (completed) TextDecoration.LineThrough else null
                )
            }
        }
    }
}

@Composable
private fun ToolStep(part: Part.Tool, directory: String) {
    val todos = remember(part.input) { parseTodos(part.input) }
    if (todos != null) {
        TodoStep(todos)
        return
    }
    var open by remember { mutableStateOf(false) }
    val raw = part.summary.ifBlank { part.name }
    val relative = relativePath(raw, directory)
    val looksLikePath = relative.contains('/')
    val title = if (looksLikePath) relative.substringAfterLast('/') else raw
    val dir = if (looksLikePath) relative.substringBeforeLast('/', "") else ""
    val hasDetails = part.input.isNotBlank() ||
        part.output.isNotBlank() ||
        part.error.isNotBlank()
    val status = statusLabel(part.status)
    val rowLabel = buildString {
        append(title)
        append(", ").append(prettyTool(part.name))
        if (dir.isNotBlank()) append(", ").append(dir)
        append(", ").append(status)
    }
    val actionHint = if (!hasDetails) {
        null
    } else {
        stringResource(if (open) R.string.chat_activity_collapse else R.string.chat_step_details)
    }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = hasDetails, role = Role.Button) { open = !open }
                .semantics {
                    contentDescription = rowLabel
                    if (actionHint != null) stateDescription = actionHint
                }
                .heightIn(min = 44.dp)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                toolIcon(part.name),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        append(prettyTool(part.name))
                        if (dir.isNotBlank()) append(" · ").append(dir)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            when (part.status) {
                "completed" -> Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )

                "error", "failed" -> Icon(
                    Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )

                "running", "streaming" -> PulsingDot(
                    color = MaterialTheme.colorScheme.primary,
                    pulsing = true,
                    size = 6
                )
            }
            if (hasDetails) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        AnimatedVisibility(visible = open) {
            Column(
                Modifier.padding(start = 26.dp, end = 4.dp).padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (part.input.isNotBlank()) StepDetail(stringResource(R.string.chat_input), part.input)
                if (part.output.isNotBlank()) StepDetail(stringResource(R.string.chat_output), part.output)
                if (part.error.isNotBlank()) {
                    StepDetail(
                        stringResource(R.string.chat_error),
                        part.error,
                        MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ThoughtStep(part: Part.Reasoning, pending: Boolean) {
    var open by remember { mutableStateOf(false) }
    val label = when {
        part.durationMs >= 1000 -> stringResource(
            R.string.chat_activity_thought_for,
            (part.durationMs / 1000).toInt()
        )

        pending -> stringResource(R.string.chat_thinking)
        else -> stringResource(R.string.chat_activity_thought)
    }
    val hint = stringResource(
        if (open) R.string.chat_activity_collapse else R.string.chat_step_details
    )
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { open = !open }
                .semantics {
                    contentDescription = label
                    stateDescription = hint
                }
                .heightIn(min = 44.dp)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        AnimatedVisibility(visible = open) {
            Text(
                part.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 25.dp, end = 4.dp, bottom = 6.dp)
            )
        }
    }
}

@Composable
private fun StepDetail(label: String, text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Text(
                    text.take(4000),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = color
                )
            }
        }
    }
}

/**
 * The single live indicator for a running turn: sits above the composer, so it
 * stays visible no matter where the transcript is scrolled. Mirrors the TUI's
 * bottom "Working" line instead of duplicating it inside the transcript.
 */
@Composable
private fun UnreadSeparator() {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            stringResource(R.string.chat_new_marker),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        HorizontalDivider(
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun ComposerStatus(
    messages: List<Msg>,
    onBackground: () -> Unit
) {
    // Pending assistant messages are the live turn. Taking trailing messages up to
    // the last user message breaks after a steer prompt, and resetting the timer to
    // zero whenever no assistant message exists yet leaves "Working 0s" on screen.
    val live = remember(messages) { messages.filter { it.pending } }
    val tool = remember(live) {
        live.flatMap { it.parts }
            .filterIsInstance<Part.Tool>()
            .lastOrNull { it.status == "running" || it.status == "streaming" }
            ?.name
    }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val busySince = remember { System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val turnStart = remember(live) {
        live.mapNotNull { it.created.takeIf { c -> c > 0 } }.minOrNull()
    }
    // If the server clock runs ahead of the phone, a server timestamp would keep
    // the timer pinned at zero; fall back to when this strip appeared instead.
    val start = turnStart?.takeIf { it <= now + 2_000L } ?: busySince
    val elapsed = if (start > 0L) (now - start).coerceAtLeast(0L) else 0L
    val label = buildString {
        append(stringResource(R.string.chat_activity_working, formatElapsed(elapsed) ?: "…"))
        if (tool != null) append(" · ").append(tool.replaceFirstChar { it.uppercase() })
    }
    val canBackground = tool != null &&
        tool.lowercase() in setOf("bash", "shell", "codemode", "code", "job")

    // A quiet one-line status inside the composer beats a floating pill above it.
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 10.dp, top = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tool != null) {
            ActivityBars(color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            TypingIndicator(compact = true)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // Detach only tools that can actually run in the background.
        if (canBackground) {
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.chat_background),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBackground)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun PulsingError(message: String) {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(message) {
        alpha.snapTo(1f)
        alpha.animateTo(0.45f, tween(160))
        alpha.animateTo(1f, tween(240))
    }
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error.copy(alpha = alpha.value),
        maxLines = 2,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
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

private val editedToolNames = setOf(
    "edit", "write", "patch", "multiedit", "apply_patch",
    "create_file", "str_replace_editor"
)

private fun editedPaths(tools: List<Part.Tool>): List<String> =
    tools.filter { it.name.lowercase() in editedToolNames }
        .mapNotNull { part ->
            val input = runCatching { org.json.JSONObject(part.input) }.getOrNull()
                ?: return@mapNotNull null
            listOf("filePath", "path", "file", "filename")
                .firstNotNullOfOrNull { key ->
                    input.optString(key).takeIf { it.isNotBlank() }
                }
        }
        .distinct()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewSheet(
    repo: OpenCodeRepository,
    sessionID: String,
    state: ChatUiState,
    onDismiss: () -> Unit,
    onChanges: () -> Unit,
    onWorkspace: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var branch by remember { mutableStateOf("") }
    var changeCount by remember { mutableStateOf(0) }
    var jobCount by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    val planTodos = remember(state.messages) {
        state.messages.asReversed()
            .flatMap { it.parts }
            .filterIsInstance<Part.Tool>()
            .firstNotNullOfOrNull { parseTodos(it.input) }
    }
    val planDone = planTodos?.count { it.status == "completed" } ?: 0
    val contextFraction = if (state.contextLimit > 0) {
        (state.contextUsed.toFloat() / state.contextLimit).coerceIn(0f, 1f)
    } else {
        0f
    }
    val agentName = state.agents.firstOrNull { it.id == state.agentID }?.name.orEmpty()
    val modelName = state.models.firstOrNull {
        it.id == state.modelID && it.providerID == state.modelProviderID
    }?.name.orEmpty()

    LaunchedEffect(sessionID) {
        loading = true
        branch = runCatching { repo.vcsInfo(state.directory) }
            .getOrNull()
            ?.branch
            .orEmpty()
        changeCount = runCatching {
            val diffs = repo.sessionDiff(sessionID)
            if (diffs.isNotEmpty()) {
                diffs.size
            } else {
                editedPaths(
                    state.messages.flatMap { it.parts }.filterIsInstance<Part.Tool>()
                ).size
            }
        }.getOrDefault(0)
        jobCount = runCatching { repo.jobs(sessionID).size }.getOrDefault(0)
        loading = false
    }

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
            Text("Session overview", style = MaterialTheme.typography.headlineSmall)
            Text(
                state.title.ifBlank { stringResource(R.string.session_new) },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                listOf(agentName, modelName).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (branch.isNotBlank()) {
                Text(
                    branch,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Context", style = MaterialTheme.typography.titleSmall)
            LinearProgressIndicator(
                progress = { contextFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Text(
                if (state.contextLimit > 0) {
                    "${state.contextUsed} / ${state.contextLimit} tokens · ${(contextFraction * 100).toInt()}%"
                } else {
                    "${state.contextUsed} tokens"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Cost \$%.4f · tokens in %d / out %d".format(
                    state.cost,
                    state.tokensInput,
                    state.tokensOutput
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Plan", style = MaterialTheme.typography.titleSmall)
            if (planTodos == null) {
                Text(
                    "No active plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "$planDone/${planTodos.size} steps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                planTodos.take(7).forEach { todo ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (todo.status == "completed") Icons.Rounded.CheckCircle
                            else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (todo.status == "completed") {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(todo.text, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (planTodos.size > 7) {
                    Text(
                        "+${planTodos.size - 7} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Changes",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text("$changeCount", style = MaterialTheme.typography.labelMedium)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Background jobs",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (loading) "…" else "$jobCount",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (state.pendingPermission != null) {
                Text(
                    "Waiting for approval: ${state.pendingPermission!!.action}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (state.pendingForm != null) {
                Text(
                    "Waiting for an answer: ${state.pendingForm!!.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onChanges,
                    enabled = changeCount > 0,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Review changes")
                }
                TextButton(onClick = onWorkspace) {
                    Text("Workspace")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RunSummaryCard(
    assistant: Msg,
    user: Msg?,
    tools: List<Part.Tool>,
    onChanges: () -> Unit,
    onShare: () -> Unit,
    onKeep: () -> Unit,
    onRevert: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    val files = remember(tools) { editedPaths(tools) }
    val errors = tools.count { it.status == "error" || it.status == "failed" }
    val duration = if (user != null && assistant.completed > user.created) {
        assistant.completed - user.created
    } else {
        0L
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (errors > 0) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = if (errors > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (errors > 0) "Run finished with errors" else "Run finished",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                buildString {
                    formatElapsed(duration)?.let { append(it).append(" · ") }
                    append(tools.size).append(if (tools.size == 1) " tool" else " tools")
                    if (files.isNotEmpty()) {
                        append(" · ").append(files.size)
                            .append(if (files.size == 1) " file" else " files")
                    }
                    if (errors > 0) append(" · ").append(errors).append(" errors")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (files.isNotEmpty()) {
                Text(
                    files.take(3).joinToString("\n"),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (files.isNotEmpty()) {
                    Button(
                        onClick = onChanges,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Review changes")
                    }
                }
                TextButton(onClick = onContinue) {
                    Text(stringResource(R.string.chat_run_continue))
                }
                TextButton(onClick = onKeep) {
                    Text(stringResource(R.string.chat_run_keep))
                }
                TextButton(onClick = onRevert) {
                    Text(
                        stringResource(R.string.chat_run_revert),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(onClick = onShare) {
                    Text(stringResource(R.string.chat_share_card))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectSheet(
    repo: OpenCodeRepository,
    directory: String,
    projectID: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var branch by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf<String?>(null) }
    var instructionDraft by remember { mutableStateOf("") }
    var editingInstruction by remember { mutableStateOf(false) }
    var savingInstruction by remember { mutableStateOf(false) }
    var filePath by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<FileEntry>?>(null) }
    var references by remember { mutableStateOf<List<ReferenceItem>>(emptyList()) }
    var worktrees by remember { mutableStateOf<List<WorktreeEntry>>(emptyList()) }
    var worktreeName by remember { mutableStateOf("") }
    var viewFile by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf<WorktreeEntry?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var projectReload by remember { mutableStateOf(0) }
    var fileReload by remember { mutableStateOf(0) }

    LaunchedEffect(directory, projectReload) {
        if (directory.isBlank()) {
            error = "This session has no project directory."
            return@LaunchedEffect
        }
        branch = runCatching { repo.vcsInfo(directory) }.getOrNull()?.branch.orEmpty()
        instruction = try {
            repo.readFile("AGENTS.md", directory)
        } catch (e: ApiException) {
            if (e.status == 404) "" else {
                error = friendlyError(e)
                null
            }
        } catch (e: Exception) {
            error = friendlyError(e)
            null
        }
        instructionDraft = instruction.orEmpty()
        references = runCatching { repo.references(directory) }.getOrDefault(emptyList())
        worktrees = if (projectID.isBlank()) {
            emptyList()
        } else {
            runCatching { repo.worktrees(projectID) }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(filePath, directory, fileReload) {
        if (directory.isBlank()) return@LaunchedEffect
        files = runCatching { repo.listFiles(directory, filePath) }
            .getOrElse {
                error = friendlyError(it)
                emptyList()
            }
    }

    fun saveInstruction() {
        if (directory.isBlank() || savingInstruction) return
        savingInstruction = true
        scope.launch {
            try {
                repo.writeFile("AGENTS.md", directory, instructionDraft)
                instruction = instructionDraft
                editingInstruction = false
                error = null
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                savingInstruction = false
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
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.chat_project),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.workspace_close))
                }
            }
            Text(
                directory.ifBlank { "No workspace directory" },
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (branch.isNotBlank()) {
                Text(
                    branch,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Instructions · AGENTS.md", style = MaterialTheme.typography.titleSmall)
            if (editingInstruction) {
                OutlinedTextField(
                    value = instructionDraft,
                    onValueChange = { instructionDraft = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    ),
                    maxLines = 1000
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { saveInstruction() },
                        enabled = !savingInstruction,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                    TextButton(onClick = {
                        editingInstruction = false
                        instructionDraft = instruction.orEmpty()
                    }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            } else {
                Text(
                    instruction.orEmpty().ifBlank { "No AGENTS.md yet." }.take(1_500),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = {
                    instructionDraft = instruction.orEmpty()
                    editingInstruction = true
                }) {
                    Text(if (instruction.orEmpty().isBlank()) "Create AGENTS.md" else "Edit AGENTS.md")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Files", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (filePath.isNotBlank()) {
                    TextButton(onClick = {
                        filePath = filePath.substringBeforeLast('/', "")
                    }) {
                        Text("Up")
                    }
                }
                TextButton(onClick = { fileReload++ }) {
                    Text(stringResource(R.string.workspace_refresh))
                }
            }
            Text(
                "/" + filePath,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when {
                files == null -> CircularProgressIndicator(Modifier.size(22.dp))
                files!!.isEmpty() -> Text(
                    "No files here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> {
                    files!!.take(80).forEach { entry ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (entry.directory) {
                                        filePath = entry.path.replace('\\', '/')
                                    } else {
                                        viewFile = entry.path.replace('\\', '/')
                                    }
                                }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (entry.directory) Icons.Rounded.Folder
                                else Icons.Rounded.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                entry.path.replace('\\', '/').substringAfterLast('/'),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (files!!.size > 80) {
                        Text(
                            "+${files!!.size - 80} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("References", style = MaterialTheme.typography.titleSmall)
            if (references.isEmpty()) {
                Text(
                    "No references.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                references.take(12).forEach { reference ->
                    Text(
                        reference.name.ifBlank { reference.detail },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        listOf(reference.type, reference.detail)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Worktrees", style = MaterialTheme.typography.titleSmall)
            if (projectID.isBlank()) {
                Text(
                    "This session has no project yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = worktreeName,
                        onValueChange = { worktreeName = it },
                        label = { Text(stringResource(R.string.workspace_worktree_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val name = worktreeName.trim()
                            if (name.isNotEmpty()) {
                                scope.launch {
                                    try {
                                        repo.createWorktree(projectID, name)
                                        worktreeName = ""
                                        projectReload++
                                    } catch (e: Exception) {
                                        error = friendlyError(e)
                                    }
                                }
                            }
                        },
                        enabled = worktreeName.isNotBlank(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.workspace_create))
                    }
                }
                worktrees.take(12).forEach { entry ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            entry.directory,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (entry.strategy.isNotBlank()) {
                            IconButton(
                                onClick = { confirmRemove = entry },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.workspace_remove_worktree),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    viewFile?.let { file ->
        FileSheet(
            repo = repo,
            path = file,
            directory = directory,
            onDismiss = { viewFile = null }
        )
    }

    confirmRemove?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(R.string.workspace_remove_worktree)) },
            text = {
                Text(
                    stringResource(R.string.workspace_remove_worktree_body) +
                        "\n\n" + entry.directory
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = null
                    scope.launch {
                        runCatching { repo.removeWorktree(projectID, entry.directory, true) }
                            .onFailure { error = friendlyError(it) }
                        projectReload++
                    }
                }) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) {
                    Text(stringResource(R.string.workspace_cancel))
                }
            }
        )
    }
}

private data class PickedAttachment(val attachment: com.opencode.mobile.Attachment, val uri: Uri)

// ---- Workspace -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceSheet(
    repo: OpenCodeRepository,
    sessionID: String,
    directory: String,
    projectID: String,
    cost: Double,
    tokensInput: Long,
    tokensOutput: Long,
    messages: List<Msg>,
    onDismiss: () -> Unit,
    onImported: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableStateOf("jobs") }
    val tabs = listOf(
        "jobs" to stringResource(R.string.workspace_jobs),
        "context" to stringResource(R.string.workspace_context),
        "permissions" to stringResource(R.string.workspace_permissions),
        "instructions" to stringResource(R.string.workspace_instructions),
        "files" to stringResource(R.string.workspace_files),
        "worktrees" to stringResource(R.string.workspace_worktrees),
        "usage" to stringResource(R.string.workspace_usage),
        "session" to stringResource(R.string.workspace_session)
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.chat_workspace),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.workspace_close))
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { (key, label) ->
                    FilterChip(
                        selected = tab == key,
                        onClick = { tab = key },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    "jobs" -> JobsTab(repo, sessionID)
                    "context" -> ContextTab(repo, sessionID)
                    "permissions" -> PermissionsTab(repo)
                    "instructions" -> InstructionsTab(repo, sessionID)
                    "files" -> FilesTab(repo, directory)
                    "worktrees" -> WorktreesTab(repo, projectID, directory)
                    "usage" -> UsageTab(repo, directory, cost, tokensInput, tokensOutput)
                    else -> SessionTab(repo, sessionID, messages, onImported)
                }
            }
        }
    }
}

@Composable
private fun WorkspaceHeader(title: String, onRefresh: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (onRefresh != null) {
            TextButton(onClick = onRefresh) {
                Text(stringResource(R.string.workspace_refresh))
            }
        }
    }
}

@Composable
private fun WorkspaceLoading() {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(24.dp))
    }
}

@Composable
private fun WorkspaceEmpty(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun WorkspaceError(message: String, onRetry: () -> Unit) {
    Column {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.workspace_refresh))
        }
    }
}

@Composable
private fun JobsTab(repo: OpenCodeRepository, sessionID: String) {
    var jobs by remember { mutableStateOf<List<JobItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        try {
            jobs = repo.jobs(sessionID)
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = friendlyError(e)
        }
    }

    LaunchedEffect(sessionID) {
        while (true) {
            load()
            delay(3_000)
        }
    }

    Column(Modifier.fillMaxSize()) {
        WorkspaceHeader("Background jobs") { scope.launch { load() } }
        when {
            jobs == null && error == null -> WorkspaceLoading()
            error != null -> WorkspaceError(error!!) { scope.launch { load() } }
            jobs!!.isEmpty() -> WorkspaceEmpty(stringResource(R.string.workspace_empty_jobs))
            else -> LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(jobs!!) { job ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    job.title.ifBlank { job.kind },
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    buildString {
                                        append(job.kind.ifBlank { "job" })
                                        append(" · ").append(job.status)
                                        if (job.startedAt > 0) {
                                            append(" · ").append(elapsedLabel(job.startedAt))
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { repo.cancelJob(job.id) }
                                        load()
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Stop,
                                    contentDescription = stringResource(R.string.action_stop),
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

@Composable
private fun ContextTab(repo: OpenCodeRepository, sessionID: String) {
    var summary by remember { mutableStateOf<ContextSummary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        if (loading) return
        loading = true
        scope.launch {
            try {
                summary = repo.contextSummary(sessionID)
                error = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(sessionID) { load() }

    Column(Modifier.fillMaxSize()) {
        WorkspaceHeader("Context") { load() }
        when {
            summary == null && error == null -> WorkspaceLoading()
            error != null -> WorkspaceError(error!!) { load() }
            else -> LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val s = summary!!
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${s.total} entries", style = MaterialTheme.typography.titleSmall)
                            Text(
                                s.counts.entries.joinToString(" · ") { "${it.value} ${it.key}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Tokens in ${s.inputTokens} · out ${s.outputTokens} · \$%.4f".format(s.cost),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (s.compactionSummary.isNotBlank()) {
                    item {
                        Text("Compaction summary", style = MaterialTheme.typography.titleSmall)
                        SelectionContainer {
                            Text(
                                s.compactionSummary.take(2_500),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (s.recent.isNotEmpty()) {
                    item { Text("Recent context", style = MaterialTheme.typography.titleSmall) }
                    items(s.recent) { entry ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                entry.type.take(9),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(64.dp)
                            )
                            Text(
                                entry.text,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsTab(repo: OpenCodeRepository) {
    var items by remember { mutableStateOf<List<SavedPermission>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        try {
            items = repo.savedPermissions()
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = friendlyError(e)
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize()) {
        WorkspaceHeader("Saved permissions") { scope.launch { load() } }
        when {
            items == null && error == null -> WorkspaceLoading()
            error != null -> WorkspaceError(error!!) { scope.launch { load() } }
            items!!.isEmpty() -> WorkspaceEmpty(stringResource(R.string.workspace_empty_permissions))
            else -> LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items!!) { permission ->
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
                                    "${permission.action} · ${permission.resource}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    permission.effect,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { repo.deleteSavedPermission(permission.id) }
                                        load()
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

@Composable
private fun InstructionsTab(repo: OpenCodeRepository, sessionID: String) {
    var items by remember { mutableStateOf<List<InstructionEntry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<String?>(null) }
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var editorError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        try {
            items = repo.instructions(sessionID)
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = friendlyError(e)
        }
    }

    fun open(entry: InstructionEntry?) {
        editingKey = entry?.key
        key = entry?.key.orEmpty()
        value = entry?.value.orEmpty()
        editorError = null
        showEditor = true
    }

    fun save() {
        val name = key.trim()
        if (!Regex("^[a-z0-9][a-z0-9._-]*$").matches(name)) {
            editorError = "Use lowercase letters, digits, dots, dashes and underscores."
            return
        }
        scope.launch {
            try {
                repo.putInstruction(sessionID, name, value)
                showEditor = false
                load()
            } catch (e: Exception) {
                editorError = friendlyError(e)
            }
        }
    }

    LaunchedEffect(sessionID) { load() }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Instructions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { open(null) }) {
                Text(stringResource(R.string.workspace_add))
            }
            TextButton(onClick = { scope.launch { load() } }) {
                Text(stringResource(R.string.workspace_refresh))
            }
        }
        when {
            items == null && error == null -> WorkspaceLoading()
            error != null -> WorkspaceError(error!!) { scope.launch { load() } }
            items!!.isEmpty() -> WorkspaceEmpty(stringResource(R.string.workspace_empty_instructions))
            else -> LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items!!) { entry ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().clickable { open(entry) }
                    ) {
                        Row(
                            Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.key,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    entry.value.replace('\n', ' '),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { repo.deleteInstruction(sessionID, entry.key) }
                                        load()
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

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingKey == null) "New instruction" else "Edit instruction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text(stringResource(R.string.workspace_instruction_key)) },
                        singleLine = true,
                        enabled = editingKey == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text(stringResource(R.string.workspace_instruction_value)) },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (editorError != null) {
                        Text(
                            editorError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { save() }) {
                    Text(stringResource(R.string.workspace_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) {
                    Text(stringResource(R.string.workspace_cancel))
                }
            }
        )
    }
}

@Composable
private fun FilesTab(repo: OpenCodeRepository, directory: String) {
    var status by remember { mutableStateOf<List<VcsChange>?>(null) }
    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        if (loading) return
        loading = true
        scope.launch {
            try {
                branches = repo.vcsBranches(directory)
                status = repo.vcsStatus(directory)
                error = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(directory) { if (directory.isNotBlank()) load() }

    Column(Modifier.fillMaxSize()) {
        WorkspaceHeader("Files") { load() }
        if (directory.isBlank()) {
            WorkspaceEmpty("No workspace directory for this session.")
            return@Column
        }
        if (branches.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(branches.take(12)) { branch ->
                    AssistChip(onClick = {}, label = { Text(branch) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        when {
            status == null && error == null -> WorkspaceLoading()
            error != null -> WorkspaceError(error!!) { load() }
            status!!.isEmpty() -> WorkspaceEmpty(stringResource(R.string.workspace_empty_files))
            else -> LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(status!!) { change ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            change.status.uppercase().take(4),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.width(42.dp)
                        )
                        Text(
                            change.path,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (change.additions > 0 || change.deletions > 0) {
                            Text(
                                "+${change.additions} -${change.deletions}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorktreesTab(repo: OpenCodeRepository, projectID: String, directory: String) {
    var items by remember { mutableStateOf<List<WorktreeEntry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<WorktreeEntry?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        if (projectID.isBlank()) {
            items = emptyList()
            return
        }
        try {
            items = repo.worktrees(projectID)
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = friendlyError(e)
        }
    }

    LaunchedEffect(projectID) { load() }

    Column(Modifier.fillMaxSize()) {
        WorkspaceHeader("Worktrees") { scope.launch { load() } }
        if (projectID.isBlank()) {
            WorkspaceEmpty("This session has no project yet.")
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.workspace_worktree_name)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    scope.launch {
                        try {
                            repo.createWorktree(projectID, name.trim())
                            name = ""
                            load()
                        } catch (e: Exception) {
                            error = friendlyError(e)
                        }
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.workspace_create))
            }
        }
        Spacer(Modifier.height(8.dp))
        when {
            items == null && error == null -> WorkspaceLoading()
            error != null -> WorkspaceError(error!!) { scope.launch { load() } }
            items!!.isEmpty() -> WorkspaceEmpty(stringResource(R.string.workspace_empty_worktrees))
            else -> LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items!!) { entry ->
                    val isRoot = entry.strategy.isBlank() || entry.directory == directory
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.directory,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (isRoot) "current" else entry.strategy.ifBlank { "worktree" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isRoot) {
                            IconButton(
                                onClick = { confirm = entry },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.workspace_remove_worktree),
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

    confirm?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.workspace_remove_worktree)) },
            text = {
                Text(
                    stringResource(R.string.workspace_remove_worktree_body) +
                        "\n\n" + entry.directory
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = null
                        scope.launch {
                            runCatching { repo.removeWorktree(projectID, entry.directory, true) }
                                .onFailure { error = friendlyError(it) }
                            load()
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) {
                    Text(stringResource(R.string.workspace_cancel))
                }
            }
        )
    }
}

@Composable
private fun UsageTab(
    repo: OpenCodeRepository,
    directory: String,
    cost: Double,
    tokensInput: Long,
    tokensOutput: Long
) {
    var limits by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        if (loading) return
        loading = true
        scope.launch {
            try {
                limits = repo.usageLimits(directory)
                error = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                loading = false
                loaded = true
            }
        }
    }

    LaunchedEffect(directory) { load() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        WorkspaceHeader("Usage") { load() }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("This session", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Tokens in $tokensInput · out $tokensOutput · \$%.4f".format(cost),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Server limits", style = MaterialTheme.typography.titleSmall)
        when {
            !loaded -> WorkspaceLoading()
            error != null -> WorkspaceError(error!!) { load() }
            limits == null -> WorkspaceEmpty(stringResource(R.string.workspace_no_limits))
            else -> SelectionContainer {
                Text(
                    limits!!,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun SessionTab(
    repo: OpenCodeRepository,
    sessionID: String,
    messages: List<Msg>,
    onImported: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun shareText(fileName: String, mime: String, body: String) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                val dir = java.io.File(context.cacheDir, "shared")
                dir.mkdirs()
                val file = java.io.File(dir, fileName)
                file.writeText(body)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Export session"))
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                busy = false
            }
        }
    }

    fun markdownTranscript(): String {
        val out = StringBuilder("# Session $sessionID\n\n")
        messages.forEach { message ->
            out.append("## ").append(message.role.replaceFirstChar { it.uppercase() })
                .append("\n\n")
            message.parts.forEach { part ->
                when (part) {
                    is Part.Text -> if (part.text.isNotBlank()) {
                        out.append(part.text).append("\n\n")
                    }

                    is Part.Tool -> out.append("- `").append(part.name).append("` — ")
                        .append(part.status).append("\n")

                    is Part.Image -> out.append("_[image]_\n")
                    else -> Unit
                }
            }
            out.append("\n")
        }
        return out.toString()
    }

    fun htmlTranscript(): String {
        fun esc(value: String): String = value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        val body = messages.joinToString("\n") { message ->
            val parts = message.parts.joinToString("\n") { part ->
                when (part) {
                    is Part.Text -> "<p>" + esc(part.text).replace("\n", "<br>") + "</p>"
                    is Part.Tool -> "<pre>" + esc(part.name + " (" + part.status + ")") + "</pre>"
                    is Part.Image -> "<p><em>[image]</em></p>"
                    else -> ""
                }
            }
            "<section><h2>" + esc(message.role.replaceFirstChar { it.uppercase() }) +
                "</h2>" + parts + "</section>"
        }
        return """
            <!doctype html>
            <html><head><meta charset="utf-8">
            <title>Session $sessionID</title>
            <style>
            body{font-family:system-ui,sans-serif;max-width:760px;margin:0 auto;padding:28px;line-height:1.55;color:#16181d;background:#fff}
            h1{font-size:24px} h2{font-size:15px;text-transform:uppercase;letter-spacing:.08em;color:#5a6170;margin-top:28px}
            p{margin:8px 0} pre{background:#f4f5f7;padding:10px;border-radius:8px;white-space:pre-wrap;font-size:12px}
            section{border-bottom:1px solid #e6e8ec;padding-bottom:12px}
            </style></head><body>
            <h1>Session $sessionID</h1>
            $body
            </body></html>
        """.trimIndent()
    }

    fun export() {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                val bundle = repo.exportSession(sessionID)
                val dir = java.io.File(context.cacheDir, "shared")
                dir.mkdirs()
                val file = java.io.File(dir, "opencode-session-$sessionID.json")
                file.writeText(bundle.toString())
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Export session"))
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                busy = false
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        error = null
        scope.launch {
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                val root = org.json.JSONObject(text)
                val bundle = root.optJSONObject("data") ?: root
                val imported = repo.importSession(bundle)
                if (imported.isBlank()) {
                    error = "Import returned no session"
                } else {
                    onImported(imported)
                }
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Session", style = MaterialTheme.typography.titleMedium)
        Text(
            sessionID,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
        TextButton(
            onClick = { clipboard.setText(AnnotatedString(sessionID)) }
        ) {
            Text(stringResource(R.string.action_copy_id))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text("Export", style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.workspace_export_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = { export() },
            enabled = !busy,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.workspace_export))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    shareText(
                        "opencode-session-$sessionID.md",
                        "text/markdown",
                        markdownTranscript()
                    )
                },
                enabled = !busy,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.workspace_export_md))
            }
            OutlinedButton(
                onClick = {
                    shareText(
                        "opencode-session-$sessionID.html",
                        "text/html",
                        htmlTranscript()
                    )
                },
                enabled = !busy,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.workspace_export_html))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text("Import", style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.workspace_import_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = {
                picker.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            enabled = !busy,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.workspace_import))
        }
        if (busy) {
            CircularProgressIndicator(Modifier.size(22.dp))
        }
        error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
