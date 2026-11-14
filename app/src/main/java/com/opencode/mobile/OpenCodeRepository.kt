package com.opencode.mobile

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

/**
 * Thin domain layer over [Api]: one place that defines what the app can ask the
 * server for, plus reconnection policy for the event stream.
 */
class OpenCodeRepository(val api: Api) {

    suspend fun sessions(): List<SessionItem> = api.sessions()

    suspend fun lastPreview(sessionID: String): String? = api.lastPreview(sessionID)

    suspend fun session(id: String): SessionItem = api.session(id)

    suspend fun agents(): List<AgentOption> = api.agents()

    suspend fun models(): List<ModelOption> = api.models()

    suspend fun commands(): List<CommandOption> = api.commands()

    suspend fun skills(): List<SkillOption> = api.skills()

    suspend fun runCommand(sessionID: String, name: String, text: String) =
        api.runCommand(sessionID, name, text)

    suspend fun projects(): List<ProjectOption> = api.projects()

    suspend fun setModel(sessionID: String, providerID: String, modelID: String) =
        api.setModel(sessionID, providerID, modelID)

    suspend fun setAgent(sessionID: String, agentID: String) = api.setAgent(sessionID, agentID)

    suspend fun sessionDiff(sessionID: String): List<FileDiff> = api.sessionDiff(sessionID)

    suspend fun createSession(): String = api.createSession()

    suspend fun forkSession(sessionID: String, before: String?): String =
        api.forkSession(sessionID, before)

    suspend fun stageRevert(sessionID: String, messageID: String, files: Boolean = true): List<String> =
        api.stageRevert(sessionID, messageID, files)

    suspend fun commitRevert(sessionID: String) = api.commitRevert(sessionID)

    suspend fun clearRevert(sessionID: String) = api.clearRevert(sessionID)

    suspend fun compactSession(sessionID: String) = api.compactSession(sessionID)

    suspend fun readFile(path: String, directory: String): String = api.readFile(path, directory)

    /**
     * Reverts one file by reverse-applying its patch. Returns false without
     * writing anything when the patch no longer matches the file.
     */
    suspend fun revertFile(path: String, directory: String, patch: String): Boolean {
        val current = api.readFile(path, directory)
        val reverted = DiffRevert.revert(current, patch) ?: return false
        api.writeFile(path, directory, reverted)
        return true
    }

    suspend fun renameSession(id: String, title: String) = api.renameSession(id, title)

    suspend fun deleteSession(id: String) = api.deleteSession(id)

    suspend fun messages(sessionID: String, olderCursor: String? = null): Api.MessagePage =
        api.messages(sessionID, limit = 50, olderCursor = olderCursor)

    suspend fun send(
        sessionID: String,
        text: String,
        files: List<Attachment> = emptyList(),
        delivery: String? = null,
        skillIDs: List<String> = emptyList()
    ) = api.send(sessionID, text, files, delivery, skillIDs)

    suspend fun interrupt(sessionID: String) = api.interrupt(sessionID)

    suspend fun permissions(): List<PendingPermission> = api.permissions()

    suspend fun forms(sessionID: String): List<FormRequest> = api.forms(sessionID)

    suspend fun answerForm(sessionID: String, formID: String, answers: Map<String, Any>) =
        api.answerForm(sessionID, formID, answers)

    suspend fun inbox(sessionID: String): List<QueuedMessage> = api.inbox(sessionID)

    suspend fun cancelInbox(sessionID: String, inboxID: String) =
        api.cancelInbox(sessionID, inboxID)

    suspend fun reply(sessionID: String, requestID: String, decision: String) =
        api.reply(sessionID, requestID, decision)

    /**
     * Event stream that reconnects with exponential backoff instead of dying on
     * the first network hiccup.
     */
    fun events(): Flow<String> = api.events().retryWhen { _, attempt ->
        delay(backoffMillis(attempt))
        true
    }

    private fun backoffMillis(attempt: Long): Long {
        val shift = attempt.coerceIn(0L, 5L).toInt()
        return minOf(30_000L, 1_000L shl shift)
    }
}
