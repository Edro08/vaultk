package com.edro08.vaultk.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.edro08.vaultk.application.EditVault
import com.edro08.vaultk.application.BrowseLocalVaults
import com.edro08.vaultk.application.CopyCredential
import com.edro08.vaultk.data.AndroidCredentialClipboard
import com.edro08.vaultk.application.OpenLocalVault
import com.edro08.vaultk.application.SaveLocalVault
import com.edro08.vaultk.data.KdbxReader
import com.edro08.vaultk.data.KdbxWriter
import com.edro08.vaultk.data.KdbxVaultValueFactory
import com.edro08.vaultk.data.LocalVaultFiles
import com.edro08.vaultk.data.LocalKdbxVaultRepository
import com.edro08.vaultk.data.LocalVaultTransaction
import java.io.FileNotFoundException

/** Android composition root: concrete adapters are known here, not by the use cases. */
class VaultkGraph(context: Context) {
    private val cloudCopyStore = com.edro08.vaultk.data.PrivateCloudCopyStore(
        java.io.File(context.filesDir, "onedrive-copies"))
    val cloudCopies = com.edro08.vaultk.application.ManageCloudCopies(
        cloudCopyStore)
    val saveCloudCopyLocally = com.edro08.vaultk.application.SaveCloudCopyLocally(cloudCopyStore)
    private val msalCloudAccount = com.edro08.vaultk.data.MsalCloudAccountSession(context, cloudCopyStore)
    val cloudAccount = com.edro08.vaultk.application.ManageCloudAccount(msalCloudAccount)
    val synchronizeCloudCopy = com.edro08.vaultk.application.SynchronizeCloudCopy(msalCloudAccount)
    val recentVaults = com.edro08.vaultk.application.ManageRecentVaults(
        com.edro08.vaultk.data.VaultSelectionStore(context))
    val biometricUnlock = com.edro08.vaultk.application.ManageBiometricUnlock(
        com.edro08.vaultk.data.BiometricKeyStore(context))
    private val localFiles = LocalVaultFiles()
    val preferences = com.edro08.vaultk.application.ManagePreferences(com.edro08.vaultk.data.AppSettings(context))
    val sessionSecurity = com.edro08.vaultk.application.ManageSessionSecurity(com.edro08.vaultk.data.SessionSecurity(context))
    val browseLocalVaults = BrowseLocalVaults(localFiles)
    val copyCredential = CopyCredential(AndroidCredentialClipboard(context))
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
