package com.edro08.vaultk.data

import com.edro08.vaultk.domain.CloudCopyId
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** One attempt against the original item ID. Never retries or changes local sync state. */
internal class GraphSmallPendingUploader(private val copies: PrivateCloudCopyStore,
    private val open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }) {
    sealed interface Outcome {
        /** Response metadata is promising, but a separate remote verification is still required. */
        data class AwaitingVerification(val itemId: String, val eTag: String) : Outcome
        data object Conflict : Outcome
        data object Missing : Outcome
        data object Uncertain : Outcome
    }

    fun upload(token: String, authenticatedAccountId: String, id: CloudCopyId): Outcome {
        require(token.isNotBlank() && authenticatedAccountId.isNotBlank())
        require(id.accountId == authenticatedAccountId) { "Account identity mismatch" }
        copies.claimSmallPendingUpload(id)
        return copies.withSmallPendingCiphertext(id) { pending, ciphertext ->
            val url = URL("https://graph.microsoft.com/v1.0/drives/${segment(id.driveId)}/items/" +
                "${segment(id.itemId)}/content")
            // Once the request begins even an I/O failure may mean the server committed it.
            // Do not retry, infer success, or release the Pending snapshot on any failure.
            try {
                val connection = open(url)
                try {
                    connection.requestMethod = "PUT"
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 30_000
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("If-Match", pending.eTag)
                    connection.setRequestProperty("Content-Type", "application/octet-stream")
                    connection.doOutput = true
                    connection.setFixedLengthStreamingMode(pending.bytes)
                    connection.outputStream.use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var remaining = pending.bytes
                        while (remaining > 0) {
                            val read = ciphertext.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (read <= 0) throw IOException("Pending ciphertext truncated")
                            output.write(buffer, 0, read)
                            remaining -= read
                        }
                        if (ciphertext.read() != -1) throw IOException("Pending ciphertext grew")
                    }
                    when (connection.responseCode) {
                        HttpURLConnection.HTTP_PRECON_FAILED -> Outcome.Conflict
                        HttpURLConnection.HTTP_NOT_FOUND -> Outcome.Missing
                        HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED -> {
                            val bytes = connection.inputStream.use { it.readNBytes(MAX_RESPONSE_BYTES + 1) }
                            if (bytes.size > MAX_RESPONSE_BYTES) Outcome.Uncertain
                            else {
                                val item = JSONObject(String(bytes, Charsets.UTF_8))
                                val tag = item.optString("eTag")
                                if (item.optString("id") != id.itemId || tag.isBlank() ||
                                    tag == pending.eTag || item.optLong("size", -1) != pending.bytes ||
                                    item.optJSONObject("parentReference")?.optString("driveId") != id.driveId)
                                    Outcome.Uncertain
                                else Outcome.AwaitingVerification(id.itemId, tag)
                            }
                        }
                        else -> Outcome.Uncertain
                    }
                } finally { connection.disconnect() }
            } catch (_: Exception) {
                Outcome.Uncertain
            }
        }
    }

    private fun segment(value: String) = java.net.URLEncoder.encode(value, "UTF-8")
    private companion object { const val MAX_RESPONSE_BYTES = 1_000_000 }
}
