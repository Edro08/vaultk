package com.edro08.vaultk.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.edro08.vaultk.application.vault.EditVault
import com.edro08.vaultk.application.vault.BrowseLocalVaults
import com.edro08.vaultk.application.security.CopyCredential
import com.edro08.vaultk.data.security.AndroidCredentialClipboard
import com.edro08.vaultk.application.vault.OpenLocalVault
import com.edro08.vaultk.application.vault.SaveLocalVault
import com.edro08.vaultk.data.kdbx.KdbxReader
import com.edro08.vaultk.data.kdbx.KdbxWriter
import com.edro08.vaultk.data.kdbx.KdbxVaultValueFactory
import com.edro08.vaultk.data.settings.LocalVaultFiles
import com.edro08.vaultk.data.kdbx.LocalKdbxVaultRepository
import com.edro08.vaultk.data.kdbx.LocalVaultTransaction
import java.io.FileNotFoundException
import com.edro08.vaultk.application.cloud.ManageCloudAccount
import com.edro08.vaultk.application.cloud.ManageCloudCopies
import com.edro08.vaultk.application.cloud.SaveCloudCopyLocally
import com.edro08.vaultk.application.cloud.SynchronizeCloudCopy
import com.edro08.vaultk.application.security.ManageBiometricUnlock
import com.edro08.vaultk.application.settings.ManagePreferences
import com.edro08.vaultk.application.settings.ManageSessionSecurity
import com.edro08.vaultk.application.vault.ManageRecentVaults
import com.edro08.vaultk.application.security.VaultSessionController
import com.edro08.vaultk.data.cloud.MsalCloudAccountSession
import com.edro08.vaultk.data.cloud.PrivateCloudCopyStore
import com.edro08.vaultk.data.security.BiometricKeyStore
import com.edro08.vaultk.data.security.SessionSecurity
import com.edro08.vaultk.data.settings.AppSettings
import com.edro08.vaultk.data.settings.VaultSelectionStore

/** Android composition root: concrete adapters are known here, not by the use cases. */
class VaultkGraph(context: Context) {
    private val cloudCopyStore = PrivateCloudCopyStore(
        java.io.File(context.filesDir, "onedrive-copies"))
    val cloudCopies = ManageCloudCopies(
        cloudCopyStore)
    val saveCloudCopyLocally = SaveCloudCopyLocally(cloudCopyStore)
    private val msalCloudAccount = MsalCloudAccountSession(context, cloudCopyStore)
    val cloudAccount = ManageCloudAccount(msalCloudAccount)
    val synchronizeCloudCopy = SynchronizeCloudCopy(msalCloudAccount)
    val recentVaults = ManageRecentVaults(
        VaultSelectionStore(context))
    val biometricUnlock = ManageBiometricUnlock(
        BiometricKeyStore(context))
    private val localFiles = LocalVaultFiles()
    val preferences = ManagePreferences(AppSettings(context))
    val sessionSecurity = ManageSessionSecurity(SessionSecurity(context))
    val browseLocalVaults = BrowseLocalVaults(localFiles)
    val copyCredential = CopyCredential(AndroidCredentialClipboard(context))
    val sessionController = VaultSessionController(copyCredential)
    val editVault = EditVault(KdbxVaultValueFactory())

    private val reader = KdbxReader()
    private val repository = LocalKdbxVaultRepository(localFiles, reader, KdbxWriter(reader),
        LocalVaultTransaction(reader),
        attachmentInput = { uri -> context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException() },
        onTiming = { step, millis ->
            if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
                Log.i("VaultkSaveTiming", "$step=${millis}ms")
        })

    val openLocalVault = OpenLocalVault(repository)
    val saveLocalVault = SaveLocalVault(repository)
}
