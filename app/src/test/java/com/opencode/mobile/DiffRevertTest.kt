package com.opencode.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiffRevertTest {

    @Test
    fun reverts_a_replaced_line() {
        val patch = """
            @@ -1,3 +1,3 @@
             a
            -b
            +B
             c
        """.trimIndent() + "\n"
        assertEquals("a\nb\nc\n", DiffRevert.revert("a\nB\nc\n", patch))
    }

    @Test
    fun reverts_an_inserted_line() {
        val patch = """
            @@ -1,2 +1,3 @@
             a
            +X
             b
        """.trimIndent() + "\n"
        assertEquals("a\nb\n", DiffRevert.revert("a\nX\nb\n", patch))
    }

    @Test
    fun reverts_a_deleted_line() {
        val patch = """
            @@ -1,3 +1,2 @@
             a
            -b
             c
        """.trimIndent() + "\n"
        assertEquals("a\nb\nc\n", DiffRevert.revert("a\nc\n", patch))
    }

    @Test
    fun preserves_crlf() {
        val patch = """
            @@ -1,2 +1,2 @@
             a
            -b
            +B
        """.trimIndent() + "\n"
        assertEquals("a\r\nb\r\n", DiffRevert.revert("a\r\nB\r\n", patch))
    }

    @Test
    fun refuses_when_the_file_no_longer_matches() {
        val patch = """
            @@ -1,3 +1,3 @@
             a
            -b
            +B
             c
        """.trimIndent() + "\n"
        assertNull(DiffRevert.revert("a\nCHANGED\nc\n", patch))
    }

    @Test
    fun refuses_an_empty_patch() {
        assertNull(DiffRevert.revert("a\n", ""))
    }
}
