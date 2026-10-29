package com.edro08.vaultk.data

import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class GraphDriveProbeTest {
    @Test fun checksOwnDriveWithGetAndDoesNotFollowRedirects() {
        val connection = StubConnection(200)
        var requestedUrl: URL? = null
        GraphDriveProbe { url -> requestedUrl = url; connection }.check("test-token")
        assertEquals("https://graph.microsoft.com/v1.0/me/drive?\$select=id", requestedUrl.toString())
        assertEquals("GET", connection.requestMethod)
        assertEquals("Bearer test-token", connection.getRequestProperty("Authorization"))
        assertFalse(connection.instanceFollowRedirects)
        assertEquals(10_000, connection.connectTimeout)
        assertEquals(10_000, connection.readTimeout)
        assertEquals(true, connection.disconnected)
    }

    @Test fun deniesNonSuccessWithoutExposingTokenOrResponse() {
        for (status in listOf(302, 401, 403, 404, 500)) {
            val connection = StubConnection(status)
            val failure = assertThrows(IllegalStateException::class.java) {
                GraphDriveProbe { connection }.check("private-token")
            }
            assertFalse(failure.message.orEmpty().contains("private-token"))
            assertEquals(true, connection.disconnected)
        }
    }

    private class StubConnection(private val status: Int) :
        HttpURLConnection(URL("https://graph.microsoft.com/")) {
        var disconnected = false
        override fun getResponseCode() = status
        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
    }
}
