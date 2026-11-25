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

    suspend fun setArchived(id: String, archived: Boolean) = api.setArchived(id, archived)

    suspend fun messages(sessionID: String, olderCursor: String? = null): Api.MessagePage =
        api.messages(sessionID, limit = 50, olderCursor = olderCursor)

    suspend fun send(
        sessionID: String,
        text: String,
        files: List<Attachment> = emptyList(),
        delivery: String? = null,
        skillIDs: List<String> = emptyList(),
        onProgress: ((Float) -> Unit)? = null
    ) = api.send(sessionID, text, files, delivery, skillIDs, onProgress)

    suspend fun interrupt(sessionID: String) = api.interrupt(sessionID)

    suspend fun background(sessionID: String) = api.background(sessionID)

    suspend fun permissions(): List<PendingPermission> = api.permissions()

    suspend fun forms(sessionID: String): List<FormRequest> = api.forms(sessionID)

    suspend fun answerForm(sessionID: String, formID: String, answers: Map<String, Any>) =
        api.answerForm(sessionID, formID, answers)

    suspend fun inbox(sessionID: String): List<QueuedMessage> = api.inbox(sessionID)

    suspend fun cancelInbox(sessionID: String, inboxID: String) =
        api.cancelInbox(sessionID, inboxID)

    suspend fun updateInbox(sessionID: String, inboxID: String, delivery: String) =
        api.updateInbox(sessionID, inboxID, delivery)

    suspend fun automations(sessionID: String): List<ScheduleItem> = api.automations(sessionID)

    suspend fun createAutomation(
        sessionID: String,
        prompt: String,
        trigger: org.json.JSONObject
    ) = api.createAutomation(sessionID, prompt, trigger)

    suspend fun setAutomationPaused(automationID: String, paused: Boolean) =
        api.setAutomationPaused(automationID, paused)

    suspend fun deleteAutomation(automationID: String) = api.deleteAutomation(automationID)

    suspend fun schedules(sessionID: String): List<ScheduleItem> = api.schedules(sessionID)

    suspend fun createSchedule(sessionID: String, prompt: String, whenSpec: org.json.JSONObject) =
        api.createSchedule(sessionID, prompt, whenSpec)

    suspend fun setSchedulePaused(scheduleID: String, paused: Boolean) =
        api.setSchedulePaused(scheduleID, paused)

    suspend fun deleteSchedule(scheduleID: String) = api.deleteSchedule(scheduleID)

    suspend fun mcpServers(): List<McpServer> = api.mcpServers()

    suspend fun mcpConnect(name: String) = api.mcpConnect(name)

    suspend fun mcpDisconnect(name: String) = api.mcpDisconnect(name)

    suspend fun mcpResources(): List<McpResource> = api.mcpResources()

    suspend fun integrations(): List<IntegrationItem> = api.integrations()

    suspend fun connectIntegrationKey(id: String, key: String) =
        api.connectIntegrationKey(id, key)

    suspend fun runningShells(directory: String): List<RunningShell> =
        api.runningShells(directory)

    suspend fun runShell(sessionID: String, directory: String, command: String): ShellRun =
        api.runShell(sessionID, directory, command)

    suspend fun shellOutput(shellID: String, directory: String): String =
        api.shellOutput(shellID, directory)

    suspend fun stopShell(shellID: String, directory: String): Unit =
        api.stopShell(shellID, directory)

    suspend fun vcsInfo(directory: String): VcsInfo? = api.vcsInfo(directory)

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
