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

    @Test
    fun `a disabled schedule feature is not reported as a password problem`() {
        assertEquals(SchedulesDisabledException::class.java, scheduleFailure(ApiException(403, "")).javaClass)
        assertEquals(
            "Schedules are turned off on this server. Add the flag below to its config, then check again.",
            friendlyError(SchedulesDisabledException())
        )
    }

    @Test
    fun `a server without schedule routes says so`() {
        assertEquals(SchedulesUnsupportedException::class.java, scheduleFailure(ApiException(404, "")).javaClass)
        assertEquals(
            "This server version has no schedules. Update OpenCode on the computer.",
            friendlyError(SchedulesUnsupportedException())
        )
    }
}
