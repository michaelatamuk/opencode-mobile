package com.opencode.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class MergeMessagesTest {

    private fun text(id: String, created: Long, value: String) = Msg(
        id = id,
        role = "assistant",
        parts = listOf(Part.Text(value)),
        pending = false,
        created = created
    )

    @Test
    fun `newer page appends in chronological order`() {
        val existing = listOf(text("a", 1, "a"), text("b", 2, "b"))
        val incoming = listOf(text("c", 3, "c"), text("d", 4, "d"))

        assertEquals(
            listOf("a", "b", "c", "d"),
            mergeMessages(existing, incoming).map { it.id }
        )
    }

    @Test
    fun `older page is placed before existing history`() {
        val existing = listOf(text("c", 3, "c"))
        val incoming = listOf(text("a", 1, "a"), text("b", 2, "b"))

        assertEquals(
            listOf("a", "b", "c"),
            mergeMessages(existing, incoming).map { it.id }
        )
    }

    @Test
    fun `incoming message replaces the same id`() {
        val existing = listOf(text("a", 1, "old"))
        val incoming = listOf(text("a", 1, "new"))

        val merged = mergeMessages(existing, incoming)

        assertEquals(1, merged.size)
        assertEquals("new", (merged.first().parts.first() as Part.Text).text)
    }

    @Test
    fun `empty incoming keeps loaded history`() {
        val existing = listOf(text("a", 1, "a"))

        assertEquals(existing, mergeMessages(existing, emptyList()))
    }
}
