package com.edro08.vaultk.data

import java.net.HttpURLConnection
import java.net.URL

/** A read-only Graph request; neither the response nor the bearer token is persisted. */
internal class GraphDriveProbe(
    private val open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) {
    fun check(accessToken: String) {
        val connection = open(URL("https://graph.microsoft.com/v1.0/me/drive?\$select=id"))
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false // Never forward the bearer token to a redirect target.
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                // Do not surface Graph response bodies, URLs or tokens in exception messages.
                throw IllegalStateException("Drive access check failed")
            }
        } finally {
            connection.disconnect()
        }
    }
}
