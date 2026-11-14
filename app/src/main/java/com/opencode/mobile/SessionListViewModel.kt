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

data class SessionListUiState(
    val sessions: List<SessionItem> = emptyList(),
    val previews: Map<String, String> = emptyMap(),
    val projects: List<ProjectOption> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val failures: Int = 0
)

class SessionListViewModel(
    private val repo: OpenCodeRepository,
    private val cache: SessionCache? = null,
    private val context: android.content.Context? = null
) : ViewModel() {

    private val _state = MutableStateFlow(SessionListUiState())
    val state: StateFlow<SessionListUiState> = _state.asStateFlow()

    private val previewCache = mutableMapOf<String, Pair<Long, String>>()

    init {
        cache?.read()?.takeIf { it.isNotEmpty() }?.let { cached ->
            _state.update { it.copy(sessions = cached, loading = false) }
        }
        viewModelScope.launch {
            runCatching { repo.projects() }.getOrNull()?.let { list ->
                _state.update { it.copy(projects = list) }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                refreshNow()
                delay(3_000)
            }
        }
    }

    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            try {
                refreshNow()
            } finally {
                _state.update { it.copy(refreshing = false) }
            }
        }
    }

    /**
     * Fetch a preview only for sessions whose `updated` changed, capped so the
     * poll stays cheap. Cached entries survive between refreshes.
     */
    private suspend fun loadPreviews(sessions: List<SessionItem>) {
        val stale = sessions
            .filter { previewCache[it.id]?.first != it.updated }
            .take(12)
        if (stale.isEmpty()) return

        stale.forEach { session ->
            val text = runCatching { repo.lastPreview(session.id) }.getOrNull()
            previewCache[session.id] = session.updated to (text ?: "")
        }

        _state.update { current ->
            current.copy(previews = previewCache.mapValues { it.value.second })
        }
    }

    private suspend fun refreshNow() {
        try {
            val sessions = repo.sessions()
            cache?.write(sessions)
            context?.let { UsageStore.record(it, sessions) }
            _state.update {
                it.copy(sessions = sessions, loading = false, error = null, failures = 0)
            }
            loadPreviews(sessions)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val failures = _state.value.failures + 1
            // A single dropped poll is normal on mobile; only surface sustained failure.
            _state.update {
                it.copy(
                    loading = false,
                    failures = failures,
                    error = if (failures >= 2) friendlyError(e) else it.error
                )
            }
        }
    }

    fun create(onCreated: (SessionItem) -> Unit) {
        viewModelScope.launch {
            try {
                val id = repo.createSession()
                onCreated(
                    SessionItem(
                        id = id,
                        title = "New session",
                        updated = System.currentTimeMillis(),
                        created = System.currentTimeMillis(),
                        cost = 0.0
                    )
                )
                refreshNow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }

    fun rename(id: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                repo.renameSession(id, title.trim())
                refreshNow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteSession(id)
                _state.update { st -> st.copy(sessions = st.sessions.filterNot { it.id == id }) }
                refreshNow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }

    /** Interrupts a running turn from the session list. */
    fun stop(id: String) {
        viewModelScope.launch {
            try {
                repo.interrupt(id)
                refreshNow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendlyError(e)) }
            }
        }
    }
}
