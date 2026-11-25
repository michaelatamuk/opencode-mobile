package com.opencode.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ChatUiState(
    val sessionID: String,
    val title: String = "Session",
    val messages: List<Msg> = emptyList(),
    val permissions: List<PendingPermission> = emptyList(),
    val forms: List<FormRequest> = emptyList(),
    val queued: List<QueuedMessage> = emptyList(),
    val offlineQueued: Int = 0,
    val agents: List<AgentOption> = emptyList(),
    val models: List<ModelOption> = emptyList(),
    val commands: List<CommandOption> = emptyList(),
    val skills: List<SkillOption> = emptyList(),
    val agentID: String = "",
    val modelID: String = "",
    val modelProviderID: String = "",
    val cost: Double = 0.0,
    val tokensInput: Long = 0L,
    val tokensOutput: Long = 0L,
    val updated: Long = 0L,
    val directory: String = "",
    val projectID: String = "",
    val parentID: String = "",
    val olderCursor: String? = null,
    val loadingOlder: Boolean = false,
    val sending: Boolean = false,
    val sendProgress: Float = 0f,
    val selecting: Boolean = false,
    val error: String? = null
) {
    /**
     * A run is live while the newest *assistant* message is still pending. Looking
     * at the newest message of any role breaks steering: a steer prompt is a user
     * message appended after the running assistant message, and user messages are
     * never pending, so the composer would drop out of Stop while the turn runs.
     */
    val busy: Boolean get() =
        messages.lastOrNull { it.role == "assistant" }?.pending == true
    val pendingPermission: PendingPermission? get() = permissions.firstOrNull()
    val pendingForm: FormRequest? get() = forms.firstOrNull()

    /** Approximate context size from the newest assistant message's usage. */
    val contextUsed: Long
        get() = messages.lastOrNull { it.role == "assistant" && it.tokensInput > 0 }
            ?.let { it.tokensInput + it.tokensCacheRead } ?: 0L

    val contextLimit: Long
        get() = models.firstOrNull { it.id == modelID && it.providerID == modelProviderID }
            ?.contextLimit ?: 0L
}

class ChatViewModel(
    private val repo: OpenCodeRepository,
    sessionID: String?,
    private val appContext: android.content.Context? = null
) : ViewModel() {

    private var activeID: String? = sessionID
    private var pollJob: Job? = null
    private var flushing = false

    private val _state = MutableStateFlow(ChatUiState(sessionID.orEmpty()))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val refreshMutex = Mutex()
    private var cachedSignature: String? = null

    init {
        // Seed the last-known transcript before the first network round-trip so
        // an offline open still shows the conversation.
        if (sessionID != null && appContext != null) {
            viewModelScope.launch {
                val cached = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    TranscriptCache.load(appContext, sessionID)
                }
                if (cached.isNotEmpty() && _state.value.messages.isEmpty()) {
                    _state.update { it.copy(messages = cached) }
                    cachedSignature = signatureOf(cached)
                }
            }
        }
        viewModelScope.launch { loadOptions() }
        if (sessionID != null) {
            viewModelScope.launch { loadSession() }
            startPolling()
            viewModelScope.launch {
                repo.events().collect { refreshNow() }
            }
        }
    }

    private fun startPolling() {
        if (pollJob != null) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                refreshNow()
                delay(5_000)
            }
        }
    }

    private fun signatureOf(messages: List<Msg>): String =
        messages.size.toString() + ":" +
            (messages.lastOrNull()?.id ?: "") + ":" +
            (messages.lastOrNull()?.parts?.size ?: 0) + ":" +
            (messages.lastOrNull()?.plainText?.length ?: 0)

    private suspend fun loadSession() {
        val id = activeID ?: return
        try {
            val session = repo.session(id)
            _state.update {
                it.copy(
                    title = session.title,
                    modelID = session.modelID,
                    modelProviderID = session.modelProviderID,
                    agentID = session.agentID,
                    cost = session.cost,
                    tokensInput = session.tokensInput,
                    tokensOutput = session.tokensOutput,
                    updated = session.updated,
                    directory = session.directory,
                    projectID = session.projectID,
                    parentID = session.forkedFrom
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Header keeps the placeholder title; refresh() reports real errors.
        }
    }

    private suspend fun loadOptions() {
        runCatching { repo.agents() }.getOrNull()?.let { list ->
            _state.update { it.copy(agents = list) }
        }
        runCatching { repo.models() }.getOrNull()?.let { list ->
            _state.update { it.copy(models = list) }
        }
        runCatching { repo.commands() }.getOrNull()?.let { list ->
            _state.update { it.copy(commands = list) }
        }
        runCatching { repo.skills() }.getOrNull()?.let { list ->
            _state.update { it.copy(skills = list) }
        }
    }

    fun selectAgent(agentID: String) {
        val id = activeID ?: return
        viewModelScope.launch {
            _state.update { it.copy(selecting = true) }
            try {
                repo.setAgent(id, agentID)
                loadSession()
                _state.update { it.copy(error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            } finally {
                _state.update { it.copy(selecting = false) }
            }
        }
    }

    fun selectModel(providerID: String, modelID: String) {
        val id = activeID ?: return
        viewModelScope.launch {
            _state.update { it.copy(selecting = true) }
            try {
                repo.setModel(id, providerID, modelID)
                loadSession()
                _state.update { it.copy(error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            } finally {
                _state.update { it.copy(selecting = false) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        val id = activeID ?: return
        var reached = false
        refreshMutex.withLock {
            try {
                val page = repo.messages(id)
                reached = true
                _state.update {
                    it.copy(
                        messages = mergeMessages(it.messages, page.messages),
                        olderCursor = it.olderCursor ?: page.olderCursor,
                        error = null
                    )
                }
                val snapshot = _state.value.messages
                val signature = signatureOf(snapshot)
                val ctx = appContext
                if (ctx != null && signature != cachedSignature) {
                    cachedSignature = signature
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        TranscriptCache.save(ctx, id, snapshot)
                    }
                }
                val pending = repo.permissions().filter { it.sessionID == id }
                _state.update { it.copy(permissions = pending) }

                val forms = runCatching { repo.forms(id) }.getOrDefault(emptyList())
                _state.update { it.copy(forms = forms) }

                val queued = runCatching { repo.inbox(id) }.getOrDefault(emptyList())
                _state.update { it.copy(queued = queued) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
        if (reached) {
            appContext?.let { ctx ->
                _state.update {
                    it.copy(
                        offlineQueued = PendingStore.all(ctx)
                            .count { pending -> pending.sessionID == id }
                    )
                }
            }
            flushPending()
        }
    }

    /** Delivers prompts written while the server was unreachable. */
    fun flushPending() {
        val ctx = appContext ?: return
        val id = activeID ?: return
        if (flushing) return
        val pending = PendingStore.all(ctx).filter { it.sessionID == id }
        if (pending.isEmpty()) {
            _state.update { it.copy(offlineQueued = 0) }
            return
        }
        flushing = true
        viewModelScope.launch {
            try {
                for (item in pending) {
                    val files = item.attachments.mapNotNull { attachment ->
                        runCatching {
                            val bytes = java.io.File(attachment.path).readBytes()
                            Attachment(
                                name = attachment.name,
                                mime = attachment.mime,
                                base64 = android.util.Base64.encodeToString(
                                    bytes,
                                    android.util.Base64.NO_WRAP
                                )
                            )
                        }.getOrNull()
                    }
                    try {
                        repo.send(
                            sessionID = id,
                            text = item.text,
                            files = files,
                            skillIDs = item.skillIDs
                        )
                        PendingStore.remove(ctx, item.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        break
                    }
                }
            } finally {
                flushing = false
                _state.update {
                    it.copy(
                        offlineQueued = PendingStore.all(ctx)
                            .count { pendingItem -> pendingItem.sessionID == id }
                    )
                }
            }
        }
    }

    fun loadOlder(onAdded: (Int) -> Unit) {
        val id = activeID ?: return
        val cursor = _state.value.olderCursor ?: return
        if (_state.value.loadingOlder) return
        viewModelScope.launch {
            _state.update { it.copy(loadingOlder = true) }
            try {
                val before = _state.value.messages.size
                val page = repo.messages(id, cursor)
                _state.update {
                    it.copy(
                        messages = mergeMessages(it.messages, page.messages),
                        olderCursor = page.olderCursor
                    )
                }
                onAdded(_state.value.messages.size - before)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            } finally {
                _state.update { it.copy(loadingOlder = false) }
            }
        }
    }

    fun send(
        text: String,
        files: List<Attachment>,
        skillIDs: List<String> = emptyList(),
        delivery: String? = null,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(sending = true) }
            var ok = false
            try {
                // Draft sessions are created on the first message, like any modern
                // chat app: tapping "+" must not leave empty sessions behind.
                val id = activeID ?: repo.createSession().also { created ->
                    activeID = created
                    _state.update { it.copy(sessionID = created) }
                    startPolling()
                    // Fresh sessions inherit the active profile's defaults.
                    appContext?.let { ctx ->
                        ProfileStore.active(ctx)?.let { profile ->
                            if (profile.agentID.isNotBlank()) {
                                runCatching { repo.setAgent(created, profile.agentID) }
                            }
                            if (profile.modelID.isNotBlank()) {
                                runCatching {
                                    repo.setModel(created, profile.providerID, profile.modelID)
                                }
                            }
                        }
                    }
                }
                // A leading /command is routed to the command endpoint so the
                // server parses its arguments; ordinary prompts stay a prompt.
                val slash = Regex("^/([A-Za-z0-9_-]+)\\s*([\\s\\S]*)$").find(text.trim())
                val knownCommand = slash?.groupValues?.get(1)
                    ?.takeIf { name -> _state.value.commands.any { it.name == name } }
                if (knownCommand != null && files.isEmpty() && skillIDs.isEmpty()) {
                    repo.runCommand(id, knownCommand, slash.groupValues[2].trim())
                } else {
                    repo.send(
                        sessionID = id,
                        text = text,
                        files = files,
                        delivery = delivery ?: if (_state.value.busy) "steer" else null,
                        skillIDs = skillIDs,
                        onProgress = { progress ->
                            _state.update { it.copy(sendProgress = progress) }
                        }
                    )
                }
                ok = true
                refreshNow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val target = _state.value.sessionID
                if (e is java.io.IOException && target.isNotBlank() && appContext != null) {
                    val ctx = appContext
                    val saved = files.mapIndexedNotNull { index, file ->
                        runCatching {
                            val dir = java.io.File(ctx.filesDir, "pending")
                            dir.mkdirs()
                            val out = java.io.File(
                                dir,
                                "p-${System.currentTimeMillis()}-$index"
                            )
                            out.writeBytes(
                                android.util.Base64.decode(file.base64, android.util.Base64.DEFAULT)
                            )
                            PendingAttachment(file.name, file.mime, out.absolutePath)
                        }.getOrNull()
                    }
                    PendingStore.add(
                        ctx,
                        target,
                        _state.value.title,
                        text,
                        skillIDs,
                        saved
                    )
                    _state.update {
                        it.copy(
                            offlineQueued = PendingStore.all(ctx)
                                .count { pending -> pending.sessionID == target },
                            error = "Queued - will send when the server is back."
                        )
                    }
                    ok = true
                } else {
                    _state.update { it.copy(error = friendlyError(e)) }
                }
            } finally {
                _state.update { it.copy(sending = false, sendProgress = 0f) }
                onDone(ok)
            }
        }
    }

    fun interrupt() {
        val id = activeID ?: return
        viewModelScope.launch {
            try {
                repo.interrupt(id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            refreshNow()
        }
    }

    fun background() {
        val id = activeID ?: return
        viewModelScope.launch {
            try {
                repo.background(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
            refreshNow()
        }
    }

    fun reply(decision: String) {
        val request = _state.value.pendingPermission ?: return
        viewModelScope.launch {
            try {
                repo.reply(request.sessionID, request.id, decision)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
            refreshNow()
        }
    }

    fun answerForm(formID: String, answers: Map<String, Any>) {
        val id = activeID ?: return
        viewModelScope.launch {
            try {
                repo.answerForm(id, formID, answers)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
            refreshNow()
        }
    }

    /** Copies the conversation (up to [before], or all of it) into a new session. */
    fun fork(before: String?, onCreated: (String) -> Unit) {
        val id = activeID ?: return
        viewModelScope.launch {
            try {
                val child = repo.forkSession(id, before)
                onCreated(child)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }

    /**
     * Reverts the last user turn and its file changes, then commits it.
     * Staging first means a failed commit leaves a reversible state instead of
     * a half-applied revert.
     */
    fun undoLastTurn() {
        val id = activeID ?: return
        val lastUser = _state.value.messages.lastOrNull { it.role == "user" } ?: return
        viewModelScope.launch {
            try {
                repo.stageRevert(id, lastUser.id, files = true)
                repo.commitRevert(id)
                refreshNow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }

    /**
     * Reverts the last turn (files included) and hands the prompt back so the
     * caller can send it again — the one-tap "try that again" path.
     */
    fun regenerateLast(onPrompt: (String) -> Unit) {
        val id = activeID ?: return
        val lastUser = _state.value.messages.lastOrNull { it.role == "user" } ?: return
        val prompt = lastUser.plainText
        if (prompt.isBlank()) return
        viewModelScope.launch {
            try {
                repo.stageRevert(id, lastUser.id, files = true)
                repo.commitRevert(id)
                refreshNow()
                onPrompt(prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }

    fun compact() {
        val id = activeID ?: return
        viewModelScope.launch {
            try {
                repo.compactSession(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }

    /** Removes one queued/steered message that has not been delivered. */
    fun cancelQueued(inboxID: String) {
        val id = activeID ?: return
        viewModelScope.launch {
            try {
                repo.cancelInbox(id, inboxID)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
            refreshNow()
        }
    }

    /** Promotes a queued item to steer delivery. */
    fun steerQueued(inboxID: String) {
        val id = activeID ?: return
        viewModelScope.launch {
            try {
                repo.updateInbox(id, inboxID, "steer")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
            refreshNow()
        }
    }

    /** Edits a queued item by cancelling it and re-sending the new text. */
    fun editQueued(inboxID: String, text: String, delivery: String) {
        val id = activeID ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                repo.cancelInbox(id, inboxID)
                repo.send(id, text.trim(), delivery = delivery)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
            refreshNow()
        }
    }

    /**
     * Branches the conversation before the last user prompt and replays that
     * prompt on [providerID]/[modelID], so the two answers can be compared.
     */
    fun runAb(providerID: String, modelID: String, onCreated: (String) -> Unit) {
        val id = activeID ?: return
        val lastUser = _state.value.messages.lastOrNull { it.role == "user" } ?: return
        val prompt = lastUser.plainText
        if (prompt.isBlank()) return
        viewModelScope.launch {
            try {
                val child = repo.forkSession(id, lastUser.id)
                repo.setModel(child, providerID, modelID)
                repo.send(child, prompt)
                onCreated(child)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }
}
