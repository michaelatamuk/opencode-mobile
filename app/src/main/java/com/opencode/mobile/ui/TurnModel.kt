package com.opencode.mobile.ui

import com.opencode.mobile.Msg
import com.opencode.mobile.Part

internal sealed interface TurnEntry {
    data class Steps(val parts: List<Part>) : TurnEntry
    data class Text(val body: String) : TurnEntry
}

/**
 * Assistant output in the order it happened: runs of tools/thoughts stay where
 * they occurred instead of collapsing to one block at the top of the turn.
 */
internal fun buildTurnEntries(messages: List<Msg>): List<TurnEntry> {
    val entries = mutableListOf<TurnEntry>()
    val run = mutableListOf<Part>()

    fun flush() {
        if (run.isNotEmpty()) {
            entries += TurnEntry.Steps(run.toList())
            run.clear()
        }
    }

    messages.forEach { message ->
        message.parts.forEach { part ->
            when (part) {
                is Part.Tool, is Part.Reasoning -> run += part
                is Part.Text -> if (part.text.isNotBlank()) {
                    flush()
                    entries += TurnEntry.Text(part.text)
                }

                else -> Unit
            }
        }
    }
    flush()
    return entries
}
