package com.edro08.vaultk.data

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.edro08.vaultk.R
import com.edro08.vaultk.domain.CloudAccount
import com.edro08.vaultk.domain.CloudAccountSession
import com.edro08.vaultk.domain.CloudDriveItem
import com.edro08.vaultk.domain.CloudDownload
import com.edro08.vaultk.domain.CloudConflictResolution
import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudCopyProtectedException
import com.edro08.vaultk.domain.CloudCopySync
import com.edro08.vaultk.domain.CloudSyncOutcome
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import java.util.concurrent.Executors

/** MSAL owns its token cache; this adapter exposes only account identity to the core. */
class MsalCloudAccountSession(context: Context,
    private val copies: PrivateCloudCopyStore = PrivateCloudCopyStore(java.io.File(context.filesDir, "onedrive-copies")))
    : CloudAccountSession<Activity>, CloudCopySync {
    private val pending = mutableListOf<(Result<ISingleAccountPublicClientApplication>) -> Unit>()
    private var application: ISingleAccountPublicClientApplication? = null
    private var initializing = false
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val graph = GraphDriveProbe()
    private val browser = GraphDriveBrowser(appContext, copies)
    private val smallUploader = GraphSmallPendingUploader(copies)
    private val smallVerifier = GraphSmallUploadVerifier(copies)
    private val network = Executors.newSingleThreadExecutor()

    private fun <T> deliver(callback: (Result<T>) -> Unit, result: Result<T>) {
        if (Looper.myLooper() == Looper.getMainLooper()) callback(result)
        else main.post { callback(result) }
    }

    private fun withApplication(callback: (Result<ISingleAccountPublicClientApplication>) -> Unit) {
        application?.let { callback(Result.success(it)); return }
        pending += callback
        if (initializing) return
        initializing = true
        try {
            PublicClientApplication.createSingleAccountPublicClientApplication(appContext, R.raw.msal_config,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(app: ISingleAccountPublicClientApplication) {
                        main.post { application = app; finish(Result.success(app)) }
                    }
                    override fun onError(exception: MsalException) {
                        main.post { finish(Result.failure(exception)) }
                    }
                })
        } catch (exception: Exception) {
            finish(Result.failure(exception))
        }
    }

    private fun finish(result: Result<ISingleAccountPublicClientApplication>) {
        initializing = false
        val callbacks = pending.toList()
        pending.clear()
        callbacks.forEach { it(result) }
    }

    override fun current(callback: (Result<CloudAccount?>) -> Unit) = withApplication { result ->
        result.onSuccess { app ->
            app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: IAccount?) = deliver(callback, Result.success(activeAccount?.asDomain()))
                override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) = Unit
                override fun onError(exception: MsalException) = deliver(callback, Result.failure(exception))
            })
        }.onFailure { callback(Result.failure(it)) }
    }

    override fun connect(host: Activity, callback: (Result<CloudAccount?>) -> Unit) = withApplication { result ->
        result.onSuccess { app ->
            val parameters = SignInParameters.builder().withActivity(host)
                .withScopes(listOf("Files.ReadWrite"))
                .withCallback(object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) =
                        deliver(callback, Result.success(authenticationResult.account.asDomain()))
                    override fun onError(exception: MsalException) = deliver(callback, Result.failure(exception))
                    override fun onCancel() = deliver(callback, Result.success(null))
                }).build()
            app.signIn(parameters)
        }.onFailure { callback(Result.failure(it)) }
    }

    override fun disconnect(callback: (Result<Unit>) -> Unit) = withApplication { result ->
        result.onSuccess { app ->
            app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() = deliver(callback, Result.success(Unit))
                override fun onError(exception: MsalException) = deliver(callback, Result.failure(exception))
            })
        }.onFailure { callback(Result.failure(it)) }
    }

    override fun checkDriveAccess(callback: (Result<Unit>) -> Unit) = withApplication { result ->
        result.onSuccess { app ->
            app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: IAccount?) {
                    if (activeAccount == null) {
                        deliver(callback, Result.failure(IllegalStateException("No active account")))
                        return
                    }
                    val parameters = AcquireTokenSilentParameters.Builder()
                        .withScopes(listOf("Files.ReadWrite"))
                        .forAccount(activeAccount)
                        .fromAuthority(activeAccount.authority)
                        .withCallback(object : SilentAuthenticationCallback {
                            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                                network.execute {
                                    val checked = runCatching { graph.check(authenticationResult.accessToken) }
                                    deliver(callback, checked)
                                }
                            }
                            override fun onError(exception: MsalException) =
                                deliver(callback, Result.failure(exception))
                        }).build()
                    app.acquireTokenSilentAsync(parameters)
                }
                override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) = Unit
                override fun onError(exception: MsalException) = deliver(callback, Result.failure(exception))
            })
        }.onFailure { deliver(callback, Result.failure(it)) }
    }

    private fun withToken(callback: (Result<Triple<String, String, String>>) -> Unit) = withApplication { result ->
        result.onSuccess { app ->
            app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: IAccount?) {
                    if (activeAccount == null) {
                        deliver(callback, Result.failure(IllegalStateException("No active account")))
                        return
                    }
                    val parameters = AcquireTokenSilentParameters.Builder()
                        .withScopes(listOf("Files.ReadWrite"))
                        .forAccount(activeAccount)
                        .fromAuthority(activeAccount.authority)
                        .withCallback(object : SilentAuthenticationCallback {
                            override fun onSuccess(authenticationResult: IAuthenticationResult) =
                                deliver(callback, Result.success(Triple(activeAccount.id,
                                    activeAccount.username.orEmpty(), authenticationResult.accessToken)))
                            override fun onError(exception: MsalException) =
                                deliver(callback, Result.failure(exception))
                        }).build()
                    app.acquireTokenSilentAsync(parameters)
                }
                override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) = Unit
                override fun onError(exception: MsalException) = deliver(callback, Result.failure(exception))
            })
        }.onFailure { deliver(callback, Result.failure(it)) }
    }

    override fun listDrive(folder: CloudDriveItem?, callback: (Result<List<CloudDriveItem>>) -> Unit) =
        withToken { result ->
            result.onSuccess { (_, _, token) ->
                network.execute { deliver(callback, runCatching { browser.list(token, folder) }) }
            }.onFailure { deliver(callback, Result.failure(it)) }
        }

    override fun download(item: CloudDriveItem, progress: (Long, Long?) -> Unit,
        callback: (Result<CloudDownload>) -> Unit) = withToken { result ->
        result.onSuccess { (accountId, accountName, token) ->
            network.execute {
                val downloaded = runCatching {
                    browser.download(token, accountId, item, accountName) { done, total ->
                        main.post { progress(done, total) }
                    }
                }
                deliver(callback, downloaded)
            }
        }.onFailure { deliver(callback, Result.failure(it)) }
    }

    override fun synchronize(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) =
        withToken { result ->
            result.onSuccess { (accountId, _, token) ->
                network.execute {
                    val outcome = runCatching {
                        require(id.accountId == accountId) { "Account identity mismatch" }
                        uploadAndVerify(token, accountId, id)
                    }
                    deliver(callback, outcome)
                }
            }.onFailure { deliver(callback, Result.failure(it)) }
        }

    override fun reconcile(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) =
        withToken { result ->
            result.onSuccess { (accountId, _, token) ->
                network.execute {
                    val outcome = runCatching {
                        require(id.accountId == accountId) { "Account identity mismatch" }
                        when (val reconciled = smallVerifier.reconcile(token, accountId, id)) {
                            is GraphSmallUploadVerifier.ReconcileOutcome.Confirmed ->
                                CloudSyncOutcome.Verified(reconciled.copy)
                            is GraphSmallUploadVerifier.ReconcileOutcome.Rearmed ->
                                CloudSyncOutcome.Rearmed(reconciled.copy)
                            GraphSmallUploadVerifier.ReconcileOutcome.Conflict -> {
                                smallVerifier.prepareConflict(token, accountId, id)
                                CloudSyncOutcome.Conflict
                            }
                            GraphSmallUploadVerifier.ReconcileOutcome.Missing -> CloudSyncOutcome.Missing
                            GraphSmallUploadVerifier.ReconcileOutcome.StillPending -> CloudSyncOutcome.StillPending
                        }
                    }
                    deliver(callback, outcome)
                }
            }.onFailure { deliver(callback, Result.failure(it)) }
        }

    override fun refresh(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) =
        withToken { result ->
            result.onSuccess { (accountId, _, token) ->
                network.execute {
                    val outcome = runCatching {
                        require(id.accountId == accountId) { "Account identity mismatch" }
                        when (val refreshed = smallVerifier.refresh(token, accountId, id)) {
                            is GraphSmallUploadVerifier.RefreshOutcome.UpToDate ->
                                CloudSyncOutcome.UpToDate(refreshed.copy)
                            is GraphSmallUploadVerifier.RefreshOutcome.Downloaded ->
                                CloudSyncOutcome.Downloaded(refreshed.copy)
                            is GraphSmallUploadVerifier.RefreshOutcome.Confirmed ->
                                CloudSyncOutcome.Verified(refreshed.copy)
                            is GraphSmallUploadVerifier.RefreshOutcome.Rearmed ->
                                CloudSyncOutcome.Rearmed(refreshed.copy)
                            GraphSmallUploadVerifier.RefreshOutcome.Conflict -> {
                                smallVerifier.prepareConflict(token, accountId, id)
                                CloudSyncOutcome.Conflict
                            }
                            GraphSmallUploadVerifier.RefreshOutcome.Missing -> CloudSyncOutcome.Missing
                            GraphSmallUploadVerifier.RefreshOutcome.StillPending -> CloudSyncOutcome.StillPending
                            GraphSmallUploadVerifier.RefreshOutcome.SyncError ->
                                throw java.io.IOException("Unable to refresh cloud copy")
                        }
                    }
                    deliver(callback, outcome)
                }
            }.onFailure { deliver(callback, Result.failure(it)) }
        }

    override fun resolveConflict(
        id: CloudCopyId,
        resolution: CloudConflictResolution,
        callback: (Result<CloudSyncOutcome>) -> Unit
    ) {
        if (resolution == CloudConflictResolution.UseRemote) {
            val cached = runCatching { copies.readCopy(id) }.getOrNull()
            val cachedConflict = cached?.conflict
            if (cached != null && cachedConflict != null) {
                network.execute {
                    val outcome = runCatching {
                        val resolved = copies.resolveConflictUseRemote(id, cached.eTag, cachedConflict.remoteEtag)
                        CloudSyncOutcome.Downloaded(resolved)
                    }
                    deliver(callback, outcome)
                }
                return
            }
        }
        withToken { result ->
            result.onSuccess { (accountId, _, token) ->
                network.execute {
                    val outcome = runCatching {
                        require(id.accountId == accountId) { "Account identity mismatch" }
                        val current = copies.readCopy(id)?.takeIf { it.conflict != null }
                            ?: smallVerifier.prepareConflict(token, accountId, id)
                            ?: throw CloudCopyProtectedException()
                        val conflict = current.conflict ?: throw CloudCopyProtectedException()
                        when (resolution) {
                            CloudConflictResolution.UseRemote -> {
                                val resolved = copies.resolveConflictUseRemote(id, current.eTag, conflict.remoteEtag)
                                CloudSyncOutcome.Downloaded(resolved)
                            }
                            CloudConflictResolution.KeepLocal -> {
                                copies.resolveConflictKeepLocal(id, current.eTag, conflict.remoteEtag)
                                uploadAndVerify(token, accountId, id)
                            }
                        }
                    }
                    deliver(callback, outcome)
                }
            }.onFailure { deliver(callback, Result.failure(it)) }
        }
    }

    private fun uploadAndVerify(token: String, accountId: String, id: CloudCopyId): CloudSyncOutcome =
        when (smallVerifier.checkBeforeUpload(token, accountId, id)) {
            GraphSmallUploadVerifier.PreUploadOutcome.Conflict -> {
                smallVerifier.prepareConflict(token, accountId, id)
                CloudSyncOutcome.Conflict
            }
            GraphSmallUploadVerifier.PreUploadOutcome.Missing -> CloudSyncOutcome.Missing
            GraphSmallUploadVerifier.PreUploadOutcome.StillPending -> CloudSyncOutcome.StillPending
            GraphSmallUploadVerifier.PreUploadOutcome.ReadyToUpload ->
                when (val attempt = smallUploader.upload(token, accountId, id)) {
                    is GraphSmallPendingUploader.Outcome.AwaitingVerification ->
                        when (val verified = smallVerifier.verify(token, accountId, id, attempt)) {
                            is GraphSmallUploadVerifier.Outcome.Confirmed ->
                                CloudSyncOutcome.Verified(verified.copy)
                            GraphSmallUploadVerifier.Outcome.StillPending ->
                                CloudSyncOutcome.StillPending
                        }
                    GraphSmallPendingUploader.Outcome.Conflict -> {
                        smallVerifier.prepareConflict(token, accountId, id)
                        CloudSyncOutcome.Conflict
                    }
                    GraphSmallPendingUploader.Outcome.Missing -> CloudSyncOutcome.Missing
                    GraphSmallPendingUploader.Outcome.Uncertain -> CloudSyncOutcome.StillPending
                }
        }

    private fun IAccount.asDomain() = CloudAccount(id, username.orEmpty())
}
