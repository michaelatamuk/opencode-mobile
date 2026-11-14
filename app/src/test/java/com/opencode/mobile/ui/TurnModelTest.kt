package com.opencode.mobile.ui

import com.opencode.mobile.Msg
import com.opencode.mobile.Part
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnModelTest {

    private fun tool(name: String) = Part.Tool(
        name = name,
        status = "completed",
        summary = "",
        input = "",
        output = "",
        error = ""
    )

    private fun msg(id: String, vararg parts: Part) =
        Msg(id = id, role = "assistant", parts = parts.toList(), pending = false)

    private fun describe(entries: List<TurnEntry>): List<String> = entries.map { entry ->
        when (entry) {
            is TurnEntry.Text -> "text:${entry.body}"
            is TurnEntry.Steps -> "steps:" + entry.parts.joinToString(",") { part ->
                when (part) {
                    is Part.Tool -> part.name.replaceFirstChar { it.uppercase() }
                    is Part.Reasoning -> "thought"
                    else -> "?"
                }
            }
        }
    }

    @Test
    fun keepsTextAndStepsInTheOrderTheyHappened() {
        val entries = buildTurnEntries(
            listOf(
                msg("a", Part.Text("intro"), tool("read")),
                msg("b", Part.Text("after"), tool("write"))
            )
        )
        assertEquals(
            listOf("text:intro", "steps:Read", "text:after", "steps:Write"),
            describe(entries)
        )
    }

    @Test
    fun groupsConsecutiveToolsAndThoughts() {
        val entries = buildTurnEntries(
            listOf(msg("a", tool("read"), Part.Reasoning("hmm", 900), tool("write")))
        )
        assertEquals(listOf("steps:Read,thought,Write"), describe(entries))
    }

    @Test
    fun mergesRunsAcrossMessagesWithNoTextBetween() {
        val entries = buildTurnEntries(
            listOf(msg("a", tool("read")), msg("b", tool("write")))
        )
        assertEquals(listOf("steps:Read,Write"), describe(entries))
    }

    @Test
    fun dropsBlankTextAndEmptyTurns() {
        assertTrue(buildTurnEntries(emptyList()).isEmpty())
        assertTrue(buildTurnEntries(listOf(msg("a", Part.Text(""), Part.Text("   ")))).isEmpty())
    }
}
