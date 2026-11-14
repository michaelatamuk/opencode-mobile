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
    val agents: List<AgentOption> = emptyList(),
    val models: List<ModelOption> = emptyList(),
    val agentID: String = "",
    val modelID: String = "",
    val modelProviderID: String = "",
    val cost: Double = 0.0,
    val tokensInput: Long = 0L,
    val tokensOutput: Long = 0L,
    val olderCursor: String? = null,
    val loadingOlder: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null
) {
    val busy: Boolean get() = messages.lastOrNull()?.pending == true
    val pendingPermission: PendingPermission? get() = permissions.firstOrNull()
    val pendingForm: FormRequest? get() = forms.firstOrNull()
}

class ChatViewModel(
    private val repo: OpenCodeRepository,
    sessionID: String?
) : ViewModel() {

    private var activeID: String? = sessionID
    private var pollJob: Job? = null

    private val _state = MutableStateFlow(ChatUiState(sessionID.orEmpty()))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val refreshMutex = Mutex()

    init {
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
                    tokensOutput = session.tokensOutput
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
    }

    fun selectAgent(agentID: String) {
        val id = activeID ?: return
        viewModelScope.launch {
            try {
                repo.setAgent(id, agentID)
                loadSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }

    fun selectModel(providerID: String, modelID: String) {
        val id = activeID ?: return
        viewModelScope.launch {
            try {
                repo.setModel(id, providerID, modelID)
                loadSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        val id = activeID ?: return
        refreshMutex.withLock {
            try {
                val page = repo.messages(id)
                _state.update {
                    it.copy(
                        messages = mergeMessages(it.messages, page.messages),
                        olderCursor = it.olderCursor ?: page.olderCursor,
                        error = null
                    )
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

    fun send(text: String, files: List<Attachment>, onDone: (Boolean) -> Unit) {
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
                }
                repo.send(
                    sessionID = id,
                    text = text,
                    files = files,
                    delivery = if (_state.value.busy) "steer" else null
                )
                ok = true
                refreshNow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            } finally {
                _state.update { it.copy(sending = false) }
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
}
