package com.edro08.vaultk.data.cloud

import android.content.Context
import com.edro08.vaultk.domain.cloud.CloudDownload
import com.edro08.vaultk.domain.cloud.CloudDownloadFailure
import com.edro08.vaultk.domain.cloud.CloudDownloadIssue
import com.edro08.vaultk.domain.cloud.CloudDriveItem
import com.edro08.vaultk.domain.cloud.CloudCopy
import com.edro08.vaultk.domain.cloud.CloudCopyId
import com.edro08.vaultk.domain.cloud.CloudCopyProtectedException
import com.edro08.vaultk.domain.cloud.CloudCopyRepository
import com.edro08.vaultk.domain.cloud.CloudCopyState
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Graph I/O stays behind the account adapter. Only encrypted bytes enter private storage. */
internal class GraphDriveBrowser(private val context: Context, private val copies: CloudCopyRepository,
    private val open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }) {
    private val graph = "https://graph.microsoft.com/v1.0"

    fun list(token: String, folder: CloudDriveItem?): List<CloudDriveItem> {
        require(folder == null || folder.folder)
        val root = if (folder == null) json("$graph/me/drive/root?\$select=id,parentReference", token) else null
        val driveId = folder?.driveId ?: root!!.getJSONObject("parentReference").getString("driveId")
        val itemId = folder?.itemId ?: root!!.getString("id")
        val base = "$graph/drives/${segment(driveId)}/items/${segment(itemId)}/children"
        var next: String? = "$base?\$select=id,name,size,eTag,folder,file&\$top=200"
        val items = mutableListOf<CloudDriveItem>()
        val visited = mutableSetOf<String>()
        while (next != null) {
            // Graph's nextLink is untrusted input: never forward the bearer to another host/path.
            require(next.startsWith("$base?") && visited.add(next))
            val response = json(next, token)
            val values = response.getJSONArray("value")
            for (index in 0 until values.length()) {
                val node = values.getJSONObject(index)
                val isFolder = node.has("folder") && !node.isNull("folder")
                val name = node.getString("name")
                if (isFolder || node.has("file") && name.endsWith(".kdbx", ignoreCase = true)) {
                    items += CloudDriveItem(driveId, node.getString("id"), name, isFolder,
                        node.optLong("size").takeIf { node.has("size") }, node.optString("eTag").ifBlank { null })
                }
            }
            next = response.optString("@odata.nextLink").ifBlank { null }
            if (items.size > 10_000 || visited.size > 100) throw IOException("Drive listing too large")
        }
        return items.sortedWith(compareByDescending<CloudDriveItem> { it.folder }.thenBy { it.name.lowercase() })
    }

    fun download(token: String, accountId: String, item: CloudDriveItem, accountName: String = "",
        progress: (Long, Long?) -> Unit): CloudDownload {
        if (item.folder || !item.name.endsWith(".kdbx", ignoreCase = true))
            throw CloudDownloadFailure(CloudDownloadIssue.Metadata)
        val path = "$graph/drives/${segment(item.driveId)}/items/${segment(item.itemId)}"
        val metadata = at(CloudDownloadIssue.Metadata) { json("$path?\$select=id,name,size,eTag,file", token) }
        if (metadata.getString("id") != item.itemId || !metadata.has("file") ||
            !metadata.getString("name").endsWith(".kdbx", ignoreCase = true))
            throw CloudDownloadFailure(CloudDownloadIssue.Metadata)
        val expected = at(CloudDownloadIssue.Metadata) { metadata.getLong("size") }
        val tag = at(CloudDownloadIssue.Metadata) { metadata.getString("eTag") }
        if (expected < 8 || expected > MAX_BYTES || tag.isBlank())
            throw CloudDownloadFailure(CloudDownloadIssue.Size)
        val directory = File(context.cacheDir, "onedrive-staging").apply {
            if (!exists() && !mkdirs()) throw CloudDownloadFailure(CloudDownloadIssue.Storage)
        }
        val partial = File(directory, "${UUID.randomUUID()}.part")
        try {
            // Graph /content returns a service-generated, short-lived URL. The CDN host varies;
            // trust only this immediate Graph response, require HTTPS, never forward the OAuth token
            // and never follow a second redirect (which could leak the preauthenticated URL).
            val location = at(CloudDownloadIssue.Redirect) {
                val redirect = connection(URL("$path/content"), token)
                try {
                    if (redirect.responseCode !in 301..399) throw IOException("Download redirect unavailable")
                    redirect.getHeaderField("Location") ?: throw IOException("Download URL unavailable")
                } finally { redirect.disconnect() }
            }
            val url = at(CloudDownloadIssue.Host) { URL(location) }
            val uri = at(CloudDownloadIssue.Host) { url.toURI() }
            if (url.protocol != "https" || url.host.isBlank() || uri.userInfo != null ||
                uri.fragment != null || url.port !in listOf(-1, 443))
                throw CloudDownloadFailure(CloudDownloadIssue.Host)
            at(CloudDownloadIssue.Transfer) {
                val transfer = connection(url, null)
                try {
                    if (transfer.responseCode != HttpURLConnection.HTTP_OK) throw IOException("Download failed")
                var written = 0L
                transfer.inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            written += count
                            if (written > expected || written > MAX_BYTES)
                                throw CloudDownloadFailure(CloudDownloadIssue.Size)
                            output.write(buffer, 0, count)
                            progress(written, expected)
                        }
                        output.flush()
                    }
                }
                    if (written != expected) throw CloudDownloadFailure(CloudDownloadIssue.Size)
                    if (!validSignature(partial)) throw CloudDownloadFailure(CloudDownloadIssue.Signature)
                } finally { transfer.disconnect() }
            }
            val latest = at(CloudDownloadIssue.Version) { json("$path?\$select=id,size,eTag", token) }
            if (latest.getString("id") != item.itemId || latest.getString("eTag") != tag ||
                latest.getLong("size") != expected) throw CloudDownloadFailure(CloudDownloadIssue.Version)
            // Only after both Graph checks, publish a new immutable ciphertext blob and its
            // atomic metadata. An earlier cacheDir download (22.02) is never moved or deleted.
            val copy = CloudCopy(CloudCopyId(accountId, item.driveId, item.itemId),
                metadata.getString("name"), tag, expected, System.currentTimeMillis(), CloudCopyState.Clean,
                accountName)
            try {
                at(CloudDownloadIssue.Storage) { partial.inputStream().use { copies.promote(copy, it) } }
            } catch (_: CloudCopyProtectedException) {
                throw CloudDownloadFailure(CloudDownloadIssue.Protected)
            }
            return CloudDownload(item.driveId, item.itemId, tag, expected)
        } finally { partial.delete() }
    }

    private inline fun <T> at(issue: CloudDownloadIssue, block: () -> T): T = try {
        block()
    } catch (error: CloudDownloadFailure) {
        throw error
    } catch (error: CloudCopyProtectedException) {
        throw error
    } catch (_: Exception) {
        throw CloudDownloadFailure(issue)
    }

    private fun validSignature(file: File): Boolean = file.inputStream().use { input ->
        val header = ByteArray(8)
        input.read(header) == 8 && header.copyOfRange(0, 4).contentEquals(byteArrayOf(3, -39, -94, -102)) &&
            header.copyOfRange(4, 8).contentEquals(byteArrayOf(103, -5, 75, -75))
    }

    private fun json(url: String, token: String): JSONObject {
        val response = connection(URL(url), token)
        return try {
            if (response.responseCode != HttpURLConnection.HTTP_OK) throw IOException("Graph request failed")
            val bytes = response.inputStream.use { it.readNBytes(2_000_001) }
            if (bytes.size > 2_000_000) throw IOException("Graph response too large")
            JSONObject(String(bytes, Charsets.UTF_8))
        } finally { response.disconnect() }
    }

    private fun connection(url: URL, token: String?): HttpURLConnection {
        val connection = open(url)
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000
        if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
        return connection
    }

    private fun segment(value: String) = java.net.URLEncoder.encode(value, "UTF-8")
    private companion object { const val MAX_BYTES = 512L * 1024 * 1024 }
}
