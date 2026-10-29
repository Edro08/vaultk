package com.edro08.vaultk.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edro08.vaultk.R
import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudCopyState
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.linguafranca.pwdb.format.KdbxCredentials
import org.linguafranca.pwdb.format.KdbxHeader
import org.linguafranca.pwdb.format.KdbxStreamFormat
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase

/** Explicitly opt-in, real-service experiment. Never run as part of the regular test suite. */
@RunWith(AndroidJUnit4::class)
class GraphConditionalWriteExperimentTest {
    private val graph = "https://graph.microsoft.com/v1.0"

    @Test fun staleIfMatchOnSmallContentMustNotReplaceSyntheticFile() = withSyntheticFile { token, path, id ->
        val base = item(token, path)
        val changed = request("PUT", "$path/content", token, "remote-C".toByteArray())
        assertTrue("Synthetic control update failed: ${changed.code}", changed.code in 200..299)
        val c = item(token, path)
        assertTrue("Control ETag did not change", base.getString("eTag") != c.getString("eTag"))
        val stale = request("PUT", "$path/content", token, "candidate-B".toByteArray(),
            mapOf("If-Match" to base.getString("eTag")))
        assertEquals("PUT /content did not reject a stale ETag on synthetic ID $id", 412, stale.code)
        assertEquals(id, item(token, path).getString("id"))
        assertEquals(c.getString("eTag"), item(token, path).getString("eTag"))
        assertArrayEquals("remote-C".toByteArray(), content(token, path))
    }

    @Test fun matchingIfMatchOnSmallContentUpdatesSameSyntheticItem() = withSyntheticFile { token, path, id ->
        val base = item(token, path)
        val uploaded = request("PUT", "$path/content", token, "candidate-B".toByteArray(),
            mapOf("If-Match" to base.getString("eTag"), "Content-Type" to "application/octet-stream"))
        assertTrue("Conditional PUT /content failed: ${uploaded.code} / ${uploaded.errorCode()}",
            uploaded.code in 200..299)
        assertEquals(id, JSONObject(uploaded.body).getString("id"))
        val after = item(token, path)
        assertEquals(id, after.getString("id"))
        assertTrue("Successful replacement did not advance ETag", after.getString("eTag") != base.getString("eTag"))
        assertArrayEquals("candidate-B".toByteArray(), content(token, path))
    }

    @Test fun syntheticKdbxPendingUploadIsVerifiedBeforeBecomingClean() {
        assumeTrue("Requires explicit -e vaultk.graphExperiment true",
            InstrumentationRegistry.getArguments().getString("vaultk.graphExperiment") == "true")
        val accountResult = accountAndToken()
        val accountId = accountResult.first
        val token = accountResult.second
        val name = "vaultk-conditional-experiment-${UUID.randomUUID()}.kdbx"
        val key = "synthetic-test-only".toCharArray()
        val original = ByteArrayOutputStream().also { output ->
            KdbxDatabase().apply { rootGroup.addEntry("Synthetic") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials(String(key).toByteArray()), output)
        }.toByteArray()
        val created = request("PUT", "$graph/me/drive/root:/$name:/content", token, original)
        assertEquals("Only a newly created synthetic item is eligible", 201, created.code)
        val item = JSONObject(created.body)
        val id = CloudCopyId(accountId, item.getJSONObject("parentReference").getString("driveId"),
            item.getString("id"))
        val path = "$graph/drives/${segment(id.driveId)}/items/${segment(id.itemId)}"
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "synthetic-pending-sync-${UUID.randomUUID()}")
        try {
            val store = PrivateCloudCopyStore(root)
            val copy = CloudCopy(id, name, item.getString("eTag"), original.size.toLong(),
                System.currentTimeMillis(), CloudCopyState.Clean)
            store.promote(copy, ByteArrayInputStream(original))
            val opened = store.openVersioned(id, key)
            store.savePending(id, opened.fingerprint, key, opened.content, opened.content)
            val attempted = GraphSmallPendingUploader(store).upload(token, accountId, id)
            assertTrue("Conditional PUT did not provide verified metadata: $attempted",
                attempted is GraphSmallPendingUploader.Outcome.AwaitingVerification)
            assertEquals(CloudCopyState.Pending, store.list().single().state)
            val verified = GraphSmallUploadVerifier(store).verify(token, accountId, id,
                attempted as GraphSmallPendingUploader.Outcome.AwaitingVerification)
            assertTrue("Ciphertext was not confirmed", verified is GraphSmallUploadVerifier.Outcome.Confirmed)
            val restarted = PrivateCloudCopyStore(root)
            assertEquals(CloudCopyState.Clean, restarted.list().single().state)
            assertEquals(id.itemId, restarted.list().single().id.itemId)
            assertEquals((verified as GraphSmallUploadVerifier.Outcome.Confirmed).copy.eTag,
                restarted.list().single().eTag)
            assertEquals(2, root.walkTopDown().count { it.extension == "kdbx" })
            assertEquals("Synthetic", restarted.open(id, key).root.entries.single().title)
        } finally {
            root.deleteRecursively()
            assertEquals("Synthetic remote cleanup failed: $name", 204,
                request("DELETE", path, token).code)
            key.fill('\u0000')
            original.fill(0)
        }
    }

    @Test fun remoteChangeAfterSyntheticKdbxPendingSavePreservesBothVersions() {
        assumeTrue("Requires explicit -e vaultk.graphExperiment true",
            InstrumentationRegistry.getArguments().getString("vaultk.graphExperiment") == "true")
        val (accountId, token) = accountAndToken()
        val name = "vaultk-conflict-experiment-${UUID.randomUUID()}.kdbx"
        val key = "synthetic-test-only".toCharArray()
        fun synthetic(title: String) = ByteArrayOutputStream().also { output ->
            KdbxDatabase().apply { rootGroup.addEntry(title) }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials(String(key).toByteArray()), output)
        }.toByteArray()
        val initial = synthetic("Before")
        val created = request("PUT", "$graph/me/drive/root:/$name:/content", token, initial)
        assertEquals("Only a newly created synthetic item is eligible", 201, created.code)
        val createdItem = JSONObject(created.body)
        val id = CloudCopyId(accountId, createdItem.getJSONObject("parentReference").getString("driveId"),
            createdItem.getString("id"))
        val path = "$graph/drives/${segment(id.driveId)}/items/${segment(id.itemId)}"
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "synthetic-conflict-sync-${UUID.randomUUID()}")
        try {
            val store = PrivateCloudCopyStore(root)
            store.promote(CloudCopy(id, name, createdItem.getString("eTag"), initial.size.toLong(),
                System.currentTimeMillis(), CloudCopyState.Clean), ByteArrayInputStream(initial))
            val opened = store.openVersioned(id, key)
            store.savePending(id, opened.fingerprint, key, opened.content, opened.content)
            val localFingerprint = store.openVersioned(id, key).fingerprint
            val remoteC = synthetic("Remote C")
            val changed = request("PUT", "$path/content", token, remoteC)
            assertTrue("Synthetic remote control update failed: ${changed.code}", changed.code in 200..299)
            val newTag = item(token, path).getString("eTag")
            assertTrue(newTag != createdItem.getString("eTag"))
            assertEquals(GraphSmallPendingUploader.Outcome.Conflict,
                GraphSmallPendingUploader(store).upload(token, accountId, id))
            assertThrows(com.edro08.vaultk.domain.CloudCopyProtectedException::class.java) {
                GraphSmallPendingUploader(PrivateCloudCopyStore(root)).upload(token, accountId, id)
            }
            assertEquals(CloudCopyState.Pending, PrivateCloudCopyStore(root).list().single().state)
            assertEquals(createdItem.getString("eTag"), store.list().single().eTag)
            assertArrayEquals(localFingerprint, store.openVersioned(id, key).fingerprint)
            assertArrayEquals(remoteC, content(token, path))
            remoteC.fill(0)
        } finally {
            root.deleteRecursively()
            assertEquals("Synthetic remote cleanup failed: $name", 204,
                request("DELETE", path, token).code)
            initial.fill(0)
            key.fill('\u0000')
        }
    }

    @Test fun syntheticKdbxInterruptedAttemptReconcilesWithRealGraphWithoutSecondPut() {
        assumeTrue("Requires explicit -e vaultk.graphExperiment true",
            InstrumentationRegistry.getArguments().getString("vaultk.graphExperiment") == "true")
        val (accountId, token) = accountAndToken()
        val name = "vaultk-reconcile-experiment-${UUID.randomUUID()}.kdbx"
        val key = "synthetic-test-only".toCharArray()
        val initial = ByteArrayOutputStream().also { output ->
            KdbxDatabase().apply { rootGroup.addEntry("Before") }.save(
                KdbxStreamFormat(KdbxHeader(4)), KdbxCredentials(String(key).toByteArray()), output)
        }.toByteArray()
        val created = request("PUT", "$graph/me/drive/root:/$name:/content", token, initial)
        assertEquals("Only a newly created synthetic item is eligible", 201, created.code)
        val item = JSONObject(created.body)
        val id = CloudCopyId(accountId, item.getJSONObject("parentReference").getString("driveId"),
            item.getString("id"))
        val path = "$graph/drives/${segment(id.driveId)}/items/${segment(id.itemId)}"
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "synthetic-reconcile-sync-${UUID.randomUUID()}")
        try {
            val store = PrivateCloudCopyStore(root)
            store.promote(CloudCopy(id, name, item.getString("eTag"), initial.size.toLong(),
                System.currentTimeMillis(), CloudCopyState.Clean), ByteArrayInputStream(initial))
            val opened = store.openVersioned(id, key)
            val edited = opened.content.copy(root = opened.content.root.copy(
                entries = opened.content.root.entries.map { it.copy(title = "Reconciled") }))
            store.savePending(id, opened.fingerprint, key, opened.content, edited)

            // Simulate crash after durable claim before PUT reached Graph: reconcile must rearm.
            store.claimSmallPendingUpload(id)
            assertThrows(com.edro08.vaultk.domain.CloudCopyProtectedException::class.java) {
                GraphSmallPendingUploader(PrivateCloudCopyStore(root)).upload(token, accountId, id)
            }
            val rearmed = GraphSmallUploadVerifier(PrivateCloudCopyStore(root)).reconcile(token, accountId, id)
            assertTrue("Expected Rearmed on unchanged base ETag, got $rearmed",
                rearmed is GraphSmallUploadVerifier.ReconcileOutcome.Rearmed)

            // Perform the conditional PUT, then simulate crash before verification: reconcile must confirm via GET.
            val uploaded = GraphSmallPendingUploader(store).upload(token, accountId, id)
            assertTrue("Expected AwaitingVerification, got $uploaded",
                uploaded is GraphSmallPendingUploader.Outcome.AwaitingVerification)
            assertThrows(com.edro08.vaultk.domain.CloudCopyProtectedException::class.java) {
                GraphSmallPendingUploader(PrivateCloudCopyStore(root)).upload(token, accountId, id)
            }
            val confirmed = GraphSmallUploadVerifier(PrivateCloudCopyStore(root)).reconcile(token, accountId, id)
            assertTrue("Expected Confirmed via GET reconciliation, got $confirmed",
                confirmed is GraphSmallUploadVerifier.ReconcileOutcome.Confirmed)
            val clean = PrivateCloudCopyStore(root).list().single()
            assertEquals(CloudCopyState.Clean, clean.state)
            assertEquals("Reconciled", PrivateCloudCopyStore(root).open(id, key).root.entries.single().title)
        } finally {
            root.deleteRecursively()
            assertEquals("Synthetic remote cleanup failed: $name", 204,
                request("DELETE", path, token).code)
            initial.fill(0)
            key.fill('\u0000')
        }
    }

    @Test fun sessionCommitMustRejectChangeAfterCreation() = withSyntheticFile { token, path, id ->
        val base = item(token, path)
        val staleSetup = request("PUT", "$path/content", token, "remote-C".toByteArray())
        assertTrue(staleSetup.code in 200..299)
        val c = item(token, path)
        val staleSession = request("POST", "$path/createUploadSession", token,
            "{\"deferCommit\":true}".toByteArray(), mapOf("If-Match" to base.getString("eTag"),
                "Content-Type" to "application/json"))
        assertEquals("Session creation must reject stale ETag", 412, staleSession.code)

        val session = request("POST", "$path/createUploadSession", token,
            "{\"deferCommit\":true}".toByteArray(), mapOf("If-Match" to c.getString("eTag"),
                "Content-Type" to "application/json"))
        assertTrue("Session creation failed: ${session.code}", session.code in 200..299)
        val uploadUrl = JSONObject(session.body).getString("uploadUrl")
        val parsed = URL(uploadUrl)
        assertEquals("https", parsed.protocol)
        assertTrue(parsed.host.isNotBlank() && parsed.port in listOf(-1, 443))
        try {
            val upload = request("PUT", uploadUrl, null, "candidate-B".toByteArray(),
                mapOf("Content-Range" to "bytes 0-10/11", "Content-Type" to "application/octet-stream"))
            assertEquals("deferCommit published data at the final fragment", 202, upload.code)
            assertArrayEquals("remote-C".toByteArray(), content(token, path))

            val advance = request("PUT", "$path/content", token, "remote-D".toByteArray())
            assertTrue(advance.code in 200..299)
            val d = item(token, path)
            val driveId = d.getJSONObject("parentReference").getString("driveId")
            // This Graph commit-by-ID is a hypothesis, not production code. The documented
            // example targets a parent path; reject any response other than a proven 412.
            val commit = request("PUT", "$path", token, JSONObject()
                .put("@microsoft.graph.sourceUrl", uploadUrl).toString().toByteArray(),
                mapOf("If-Match" to c.getString("eTag"), "Content-Type" to "application/json"))
            assertEquals("Conditional commit unsupported or ignored for synthetic drive $driveId", 412, commit.code)
            assertEquals(id, item(token, path).getString("id"))
            assertEquals(d.getString("eTag"), item(token, path).getString("eTag"))
            assertArrayEquals("remote-D".toByteArray(), content(token, path))
        } finally {
            // A failed experiment must not leave a preauthorized session usable until expiry.
            runCatching { request("DELETE", uploadUrl, null) }
        }
    }

    @Test fun sessionCommitWithMatchingEtagMustUpdateSameSyntheticItem() = withSyntheticFile { token, path, id ->
        val base = item(token, path)
        val session = request("POST", "$path/createUploadSession", token,
            "{\"deferCommit\":true}".toByteArray(), mapOf("If-Match" to base.getString("eTag"),
                "Content-Type" to "application/json"))
        assertTrue("Session creation failed: ${session.code}", session.code in 200..299)
        val uploadUrl = JSONObject(session.body).getString("uploadUrl")
        val parsed = URL(uploadUrl)
        assertEquals("https", parsed.protocol)
        assertTrue(parsed.host.isNotBlank() && parsed.port in listOf(-1, 443))
        try {
            val upload = request("PUT", uploadUrl, null, "candidate-B".toByteArray(),
                mapOf("Content-Range" to "bytes 0-10/11", "Content-Type" to "application/octet-stream"))
            assertEquals("Upload must remain deferred", 202, upload.code)
            assertArrayEquals("base-A".toByteArray(), content(token, path))
            val commit = request("PUT", path, token, JSONObject()
                .put("@microsoft.graph.sourceUrl", uploadUrl).toString().toByteArray(),
                mapOf("If-Match" to base.getString("eTag"), "Content-Type" to "application/json"))
            assertTrue("Conditional commit failed: ${commit.code} / ${commit.errorCode()}",
                commit.code in 200..299)
            assertEquals(id, JSONObject(commit.body).getString("id"))
            val after = item(token, path)
            assertEquals(id, after.getString("id"))
            assertTrue("Commit did not advance ETag", after.getString("eTag") != base.getString("eTag"))
            assertArrayEquals("candidate-B".toByteArray(), content(token, path))
        } finally {
            runCatching { request("DELETE", uploadUrl, null) }
        }
    }

    private inline fun withSyntheticFile(test: (String, String, String) -> Unit) {
        assumeTrue("Requires explicit -e vaultk.graphExperiment true",
            InstrumentationRegistry.getArguments().getString("vaultk.graphExperiment") == "true")
        val token = token()
        val name = "vaultk-conditional-experiment-${UUID.randomUUID()}.txt"
        val created = request("PUT", "$graph/me/drive/root:/$name:/content", token, "base-A".toByteArray())
        assertEquals("Fresh synthetic file must be created, never replaced", 201, created.code)
        val metadata = JSONObject(created.body)
        val id = metadata.getString("id")
        val driveId = metadata.getJSONObject("parentReference").getString("driveId")
        val path = "$graph/drives/${segment(driveId)}/items/${segment(id)}"
        try { test(token, path, id) }
        finally {
            val removed = request("DELETE", path, token)
            assertEquals("Synthetic file cleanup failed: $name", 204, removed.code)
        }
    }

    private fun token(): String = accountAndToken().second

    private fun accountAndToken(): Pair<String, String> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appResult = CompletableFuture<ISingleAccountPublicClientApplication>()
        PublicClientApplication.createSingleAccountPublicClientApplication(context, R.raw.msal_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(app: ISingleAccountPublicClientApplication) = appResult.complete(app).let { Unit }
                override fun onError(exception: MsalException) = appResult.completeExceptionally(exception).let { Unit }
            })
        val app = appResult.get(30, TimeUnit.SECONDS)
        val accountResult = CompletableFuture<IAccount>()
        app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                if (activeAccount == null) accountResult.completeExceptionally(IllegalStateException("No account"))
                else accountResult.complete(activeAccount)
            }
            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) = Unit
            override fun onError(exception: MsalException) { accountResult.completeExceptionally(exception) }
        })
        val account = accountResult.get(30, TimeUnit.SECONDS)
        val tokenResult = CompletableFuture<String>()
        app.acquireTokenSilentAsync(AcquireTokenSilentParameters.Builder()
            .withScopes(listOf("Files.ReadWrite")).forAccount(account).fromAuthority(account.authority)
            .withCallback(object : SilentAuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    tokenResult.complete(authenticationResult.accessToken)
                }
                override fun onError(exception: MsalException) { tokenResult.completeExceptionally(exception) }
            }).build())
        return account.id to tokenResult.get(30, TimeUnit.SECONDS)
    }

    private fun item(token: String, path: String) = JSONObject(request("GET",
        "$path?\$select=id,eTag,parentReference", token).also { assertEquals(200, it.code) }.body)

    private fun content(token: String, path: String): ByteArray {
        val redirect = request("GET", "$path/content", token)
        assertTrue("Expected preauthenticated Graph redirect", redirect.code in 301..399)
        val url = URL(redirect.location ?: error("Missing redirect"))
        assertEquals("https", url.protocol)
        assertTrue(url.host.isNotBlank() && url.port in listOf(-1, 443))
        val downloaded = request("GET", url.toString(), null)
        assertEquals(200, downloaded.code)
        return downloaded.bytes
    }

    private data class Response(val code: Int, val bytes: ByteArray, val location: String?) {
        val body: String get() = bytes.toString(Charsets.UTF_8)
        // Graph's machine-readable error code only; never print its message/body (may contain sourceUrl).
        fun errorCode(): String = runCatching {
            JSONObject(body).getJSONObject("error").getString("code")
        }.getOrDefault("unavailable").take(60).filter { it.isLetterOrDigit() || it in "_-" }
    }

    private fun request(method: String, url: String, token: String?, body: ByteArray? = null,
        headers: Map<String, String> = emptyMap()): Response {
        val target = URL(url)
        require(target.protocol == "https" && target.port in listOf(-1, 443))
        if (token != null) require(target.host == "graph.microsoft.com")
        val connection = target.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            if (body != null) {
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            val bytes = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.use { it.readNBytes(1_000_001) } ?: byteArrayOf()
            require(bytes.size <= 1_000_000)
            return Response(code, bytes, connection.getHeaderField("Location"))
        } catch (_: IOException) {
            // A preauthorized upload URL must never appear in test failure output.
            throw IOException("Experiment transport failure ($method)")
        } finally { connection.disconnect() }
    }

    private fun segment(id: String) = java.net.URLEncoder.encode(id, "UTF-8")
}
