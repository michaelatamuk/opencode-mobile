package com.opencode.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
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
    val olderCursor: String? = null,
    val loadingOlder: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null
) {
    val busy: Boolean get() = messages.lastOrNull()?.pending == true
    val pendingPermission: PendingPermission? get() = permissions.firstOrNull()
}

class ChatViewModel(
    private val repo: OpenCodeRepository,
    private val sessionID: String
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState(sessionID))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val refreshMutex = Mutex()

    init {
        viewModelScope.launch {
            loadSession()
            while (isActive) {
                refreshNow()
                delay(5_000)
            }
        }
        viewModelScope.launch {
            // Reconnecting event stream; each event triggers a prompt refresh.
            repo.events().collect { refreshNow() }
        }
    }

    private suspend fun loadSession() {
        try {
            val session = repo.session(sessionID)
            _state.update { it.copy(title = session.title) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Header keeps the placeholder title; refresh() reports real errors.
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        refreshMutex.withLock {
            try {
                val page = repo.messages(sessionID)
                _state.update {
                    it.copy(
                        messages = mergeMessages(it.messages, page.messages),
                        olderCursor = it.olderCursor ?: page.olderCursor,
                        error = null
                    )
                }
                val pending = repo.permissions().filter { it.sessionID == sessionID }
                _state.update { it.copy(permissions = pending) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }

    fun loadOlder(onAdded: (Int) -> Unit) {
        val cursor = _state.value.olderCursor ?: return
        if (_state.value.loadingOlder) return
        viewModelScope.launch {
            _state.update { it.copy(loadingOlder = true) }
            try {
                val before = _state.value.messages.size
                val page = repo.messages(sessionID, cursor)
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
                repo.send(
                    sessionID = sessionID,
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
        viewModelScope.launch {
            try {
                repo.interrupt(sessionID)
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
}
