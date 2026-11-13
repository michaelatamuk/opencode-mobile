package com.opencode.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.UnknownHostException

class FriendlyErrorTest {

    @Test
    fun `auth failure is explained`() {
        assertEquals("Wrong or missing server password", friendlyError(ApiException(401, "")))
        assertEquals("Wrong or missing server password", friendlyError(ApiException(403, "")))
    }

    @Test
    fun `server error includes the status`() {
        assertEquals("Server error (503)", friendlyError(ApiException(503, "")))
    }

    @Test
    fun `unknown host is explained`() {
        assertEquals("Server address not found", friendlyError(UnknownHostException("nope")))
    }

    @Test
    fun `not found is explained`() {
        assertEquals("Not found on this server", friendlyError(ApiException(404, "")))
    }
}
