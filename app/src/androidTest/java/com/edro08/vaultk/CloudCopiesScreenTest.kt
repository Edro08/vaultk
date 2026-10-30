package com.edro08.vaultk

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.edro08.vaultk.application.cloud.ManageCloudCopies
import com.edro08.vaultk.application.cloud.SynchronizeCloudCopy
import com.edro08.vaultk.domain.cloud.CloudConflictResolution
import com.edro08.vaultk.domain.cloud.CloudCopy
import com.edro08.vaultk.domain.cloud.CloudCopyConflict
import com.edro08.vaultk.domain.cloud.CloudCopyId
import com.edro08.vaultk.domain.cloud.CloudCopyRepository
import com.edro08.vaultk.domain.cloud.CloudCopyState
import com.edro08.vaultk.domain.cloud.CloudCopySync
import com.edro08.vaultk.domain.cloud.CloudSyncAction
import com.edro08.vaultk.domain.cloud.CloudSyncLogEntry
import com.edro08.vaultk.domain.cloud.CloudSyncOutcome
import com.edro08.vaultk.domain.vault.VaultContent
import com.edro08.vaultk.domain.vault.OpenedVault
import com.edro08.vaultk.ui.screen.storage.CloudCopiesScreen
import com.edro08.vaultk.ui.theme.VaultkTheme
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CloudCopiesScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun deletionRequiresConfirmationAndDoesNotInvokeRemoteOperations() {
        val copy = CloudCopy(CloudCopyId("account", "drive", "item"), "synthetic.kdbx",
            "v1", 12, 100L, CloudCopyState.Clean, "test@example.com")
        val fake = object : CloudCopyRepository {
            var entries = listOf(copy)
            var removals = 0
            override fun list() = entries
            override fun selected(): CloudCopy? = null
            override fun select(id: CloudCopyId?) = Unit
            override fun open(id: CloudCopyId, key: CharArray): VaultContent = error("Not opened")
            override fun openVersioned(id: CloudCopyId, key: CharArray): OpenedVault = error("Not opened")
            override fun savePending(id: CloudCopyId, expectedFingerprint: ByteArray, key: CharArray,
                original: VaultContent, changed: VaultContent): OpenedVault = error("Not saved")
            override fun promote(copy: CloudCopy, source: InputStream): CloudCopy = error("Not downloaded")
            override fun remove(id: CloudCopyId) { removals++; entries = entries.filterNot { it.id == id } }
            override fun recoverySnapshots(id: CloudCopyId) = 0
            override fun logs(id: CloudCopyId?) = emptyList<CloudSyncLogEntry>()
        }
        compose.setContent { VaultkTheme {
            CloudCopiesScreen(ManageCloudCopies(fake), null, onBack = {}, onSelect = {}, onDeselect = {})
        } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("synthetic.kdbx").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Opciones de synthetic.kdbx").performClick()
        compose.onNodeWithText("Eliminar copia de este dispositivo").performClick()
        compose.onNodeWithText("El archivo de OneDrive no se borrará.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(0, fake.removals) }
        compose.onNodeWithContentDescription("Opciones de synthetic.kdbx").performClick()
        compose.onNodeWithText("Eliminar copia de este dispositivo").performClick()
        compose.onNodeWithText("Eliminar copia de este dispositivo").performClick()
        compose.waitUntil(5_000) { fake.removals == 1 }
    }

    @Test fun pendingCopySeparatesSingleUploadFromReconcileAndReportsConflictAndVerification() {
        val id = CloudCopyId("account", "drive", "item")
        val initial = CloudCopy(id, "synthetic.kdbx", "v1", 12, 100L, CloudCopyState.Pending,
            "test@example.com", uploadAttempted = false)
        var current = initial
        var syncCalls = 0
        var reconcileCalls = 0
        val fakeRepo = object : CloudCopyRepository {
            override fun list() = listOf(current)
            override fun selected(): CloudCopy? = null
            override fun select(id: CloudCopyId?) = Unit
            override fun open(id: CloudCopyId, key: CharArray): VaultContent = error("Not opened")
            override fun openVersioned(id: CloudCopyId, key: CharArray): OpenedVault = error("Not opened")
            override fun savePending(id: CloudCopyId, expectedFingerprint: ByteArray, key: CharArray,
                original: VaultContent, changed: VaultContent): OpenedVault = error("Not saved")
            override fun promote(copy: CloudCopy, source: InputStream): CloudCopy = error("Not downloaded")
            override fun remove(id: CloudCopyId) = error("Pending copy must not be removed")
            override fun recoverySnapshots(id: CloudCopyId) = 2
            override fun logs(filterId: CloudCopyId?) = listOf(
                CloudSyncLogEntry(100L, id, CloudSyncAction.SavePending, CloudCopyState.Pending, 12L, "v1")
            )
        }
        val fakeSync = object : CloudCopySync {
            override fun synchronize(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) {
                syncCalls++
                if (syncCalls == 1) {
                    current = current.copy(uploadAttempted = true)
                    callback(Result.success(CloudSyncOutcome.Conflict))
                } else {
                    current = current.copy(eTag = "v2", state = CloudCopyState.Clean, uploadAttempted = false)
                    callback(Result.success(CloudSyncOutcome.Verified(current)))
                }
            }
            override fun reconcile(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) {
                reconcileCalls++
                current = current.copy(uploadAttempted = false)
                callback(Result.success(CloudSyncOutcome.Rearmed(current)))
            }
            override fun refresh(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) =
                callback(Result.success(CloudSyncOutcome.UpToDate(current)))
            override fun resolveConflict(
                id: CloudCopyId,
                resolution: CloudConflictResolution,
                callback: (Result<CloudSyncOutcome>) -> Unit
            ) = callback(Result.success(CloudSyncOutcome.Verified(current)))
        }
        compose.setContent { VaultkTheme {
            CloudCopiesScreen(ManageCloudCopies(fakeRepo), null, onBack = {}, onSelect = {},
                onDeselect = {}, synchronize = SynchronizeCloudCopy(fakeSync))
        } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Subir cambios pendientes").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Copias de recuperación cifradas: 2").assertIsDisplayed()
        compose.onNodeWithText("Registro de operaciones", substring = true).performClick()
        compose.onNodeWithText("Guardado pendiente · 12 B · v1").assertIsDisplayed()
        compose.onNodeWithText("Subir cambios pendientes").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Reconciliar con OneDrive").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Conflicto en OneDrive", substring = true).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, syncCalls)
            assertEquals(0, reconcileCalls)
        }
        compose.onNodeWithText("Reconciliar con OneDrive").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Subir cambios pendientes").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Se verificó la versión base en OneDrive", substring = true).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, syncCalls)
            assertEquals(1, reconcileCalls)
        }
        compose.onNodeWithText("Subir cambios pendientes").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Sin cambios locales pendientes").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Cambios verificados en OneDrive", substring = true).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(2, syncCalls)
            assertEquals(1, reconcileCalls)
        }
    }

    @Test fun conflictCopyDisplaysBothVersionsAndAllowsExplicitKeepLocalOrUseRemoteWithoutAutoMerge() {
        val id = CloudCopyId("account", "drive", "item")
        val conflict = CloudCopyConflict("base-v1", 120L, 100L, "remote-v2", 160L, 200L)
        var current = CloudCopy(id, "synthetic.kdbx", "base-v1", 120L, 100L,
            CloudCopyState.Conflict, "test@example.com", uploadAttempted = false, conflict = conflict)
        val resolutions = mutableListOf<CloudConflictResolution>()
        val fakeRepo = object : CloudCopyRepository {
            override fun list() = listOf(current)
            override fun selected(): CloudCopy? = null
            override fun select(id: CloudCopyId?) = Unit
            override fun open(id: CloudCopyId, key: CharArray): VaultContent = error("Not opened")
            override fun openVersioned(id: CloudCopyId, key: CharArray): OpenedVault = error("Not opened")
            override fun savePending(id: CloudCopyId, expectedFingerprint: ByteArray, key: CharArray,
                original: VaultContent, changed: VaultContent): OpenedVault = error("Not saved")
            override fun promote(copy: CloudCopy, source: InputStream): CloudCopy = error("Not downloaded")
            override fun remove(id: CloudCopyId) = error("Conflict copy must not be removed")
            override fun recoverySnapshots(id: CloudCopyId) = 3
            override fun logs(filterId: CloudCopyId?) = listOf(
                CloudSyncLogEntry(200L, id, CloudSyncAction.ConflictDetected, CloudCopyState.Conflict, 160L, "remote-v2")
            )
        }
        val fakeSync = object : CloudCopySync {
            override fun synchronize(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) = Unit
            override fun reconcile(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) = Unit
            override fun refresh(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) = Unit
            override fun resolveConflict(
                id: CloudCopyId,
                resolution: CloudConflictResolution,
                callback: (Result<CloudSyncOutcome>) -> Unit
            ) {
                resolutions += resolution
                current = current.copy(eTag = "remote-v2", bytes = 160L, state = CloudCopyState.Clean, conflict = null)
                callback(Result.success(CloudSyncOutcome.Downloaded(current)))
            }
        }
        compose.setContent { VaultkTheme {
            CloudCopiesScreen(ManageCloudCopies(fakeRepo), null, onBack = {}, onSelect = {},
                onDeselect = {}, synchronize = SynchronizeCloudCopy(fakeSync))
        } }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Versión local: 120 B (base base-v1) · Versión en OneDrive: 160 B (remote-v2)")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Conservar versión local").assertIsDisplayed()
        compose.onNodeWithText("Usar versión de OneDrive").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Se activó la versión de OneDrive", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle {
            assertEquals(listOf(CloudConflictResolution.UseRemote), resolutions)
        }
    }
}
