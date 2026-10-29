package com.edro08.vaultk

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.edro08.vaultk.application.EditVault
import com.edro08.vaultk.application.SynchronizeCloudCopy
import com.edro08.vaultk.data.KdbxVaultValueFactory
import com.edro08.vaultk.domain.CloudConflictResolution
import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.CloudCopyConflict
import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudCopyState
import com.edro08.vaultk.domain.CloudCopySync
import com.edro08.vaultk.domain.CloudSyncOutcome
import com.edro08.vaultk.domain.VaultContent
import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.domain.VaultGroup
import com.edro08.vaultk.ui.screen.vault.VaultContentScreen
import com.edro08.vaultk.ui.theme.VaultkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CloudVaultContentScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun remoteCopyCanBeReadButHasNoEditingActions() {
        val entry = VaultEntry("entry", "Synthetic", "user", "", "", null)
        val content = VaultContent("Synthetic", VaultGroup("root", "Root", emptyList(), listOf(entry)))
        compose.setContent { VaultkTheme {
            VaultContentScreen(
                content = content,
                fallbackName = "sample.kdbx",
                onClose = {},
                onChangeVault = {},
                biometricEnabled = false,
                onEnableBiometric = { key, complete -> key.fill('\u0000'); complete(null) },
                onDisableBiometric = {},
                isCloudVault = true
            )
        } }
        compose.onNodeWithText("Synthetic · OneDrive · Solo lectura").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ver estado").performClick()
        compose.onNodeWithText("Solo lectura:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Synthetic").performClick()
        compose.onNodeWithContentDescription("Editar").assertDoesNotExist()
        compose.onNodeWithText("Nueva entrada").assertDoesNotExist()
    }

    @Test fun eligibleCloudCopySavesPendingAndSeparatesUploadFromReconcile() {
        val id = CloudCopyId("account", "drive", "item")
        var copy by mutableStateOf(CloudCopy(id, "sample.kdbx", "v1", 1024L, 1L, CloudCopyState.Clean))
        val entry = VaultEntry("entry", "Synthetic", "user", "", "", null)
        var currentContent by mutableStateOf(VaultContent("Synthetic",
            VaultGroup("root", "Root", emptyList(), listOf(entry))))
        var localSaves = 0
        var syncCalls = 0
        var reconcileCalls = 0
        val syncPort = object : CloudCopySync {
            override fun synchronize(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) {
                syncCalls++
                if (syncCalls == 1) {
                    copy = copy.copy(state = CloudCopyState.Pending, uploadAttempted = true)
                    callback(Result.success(CloudSyncOutcome.StillPending))
                } else {
                    copy = copy.copy(eTag = "v2", state = CloudCopyState.Clean, uploadAttempted = false)
                    callback(Result.success(CloudSyncOutcome.Verified(copy)))
                }
            }
            override fun reconcile(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) {
                reconcileCalls++
                copy = copy.copy(state = CloudCopyState.Pending, uploadAttempted = false)
                callback(Result.success(CloudSyncOutcome.Rearmed(copy)))
            }
            override fun refresh(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) =
                callback(Result.success(CloudSyncOutcome.UpToDate(copy)))
            override fun resolveConflict(
                id: CloudCopyId,
                resolution: CloudConflictResolution,
                callback: (Result<CloudSyncOutcome>) -> Unit
            ) = callback(Result.success(CloudSyncOutcome.Verified(copy)))
        }
        compose.setContent { VaultkTheme {
            VaultContentScreen(
                content = currentContent,
                fallbackName = "sample.kdbx",
                onClose = {},
                onChangeVault = {},
                biometricEnabled = false,
                onEnableBiometric = { key, complete -> key.fill('\u0000'); complete(null) },
                onDisableBiometric = {},
                copy = copy,
                editVault = EditVault(KdbxVaultValueFactory()),
                onCloudSave = { changed, complete ->
                    localSaves++
                    currentContent = changed
                    copy = copy.copy(state = CloudCopyState.Pending, uploadAttempted = false)
                    complete(null)
                },
                synchronize = SynchronizeCloudCopy(syncPort),
                cloudAutoSync = false,
                isCloudVault = true
            )
        } }
        compose.onNodeWithText("Synthetic · OneDrive · Sincronizado").assertIsDisplayed()
        compose.onNodeWithText("Synthetic").performClick()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Duplicar").performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Synthetic · OneDrive · Pendiente").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { assertEquals(1, localSaves) }
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Subir cambios pendientes").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("No se pudo confirmar el resultado con OneDrive", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Reconciliar con OneDrive").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Se verificó la versión base en OneDrive", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Subir cambios pendientes").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Synthetic · OneDrive · Sincronizado").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle {
            assertEquals(2, syncCalls)
            assertEquals(1, reconcileCalls)
        }
    }

    @Test fun autoSyncRefreshesOnOpenDownloadsRemoteAndSyncsOnSaveAndBlocksConflict() {
        val id = CloudCopyId("account", "drive", "item")
        var copy by mutableStateOf(CloudCopy(id, "sample.kdbx", "v1", 1024L, 1L, CloudCopyState.Clean))
        val initialEntry = VaultEntry("entry-1", "InitialEntry", "user", "", "", null)
        val remoteEntry = VaultEntry("entry-2", "RemoteDownloadedEntry", "user", "", "", null)
        var currentContent by mutableStateOf(VaultContent("Synthetic",
            VaultGroup("root", "Root", emptyList(), listOf(initialEntry))))
        var refreshCalls = 0
        var syncCalls = 0
        var remoteDownloads = 0
        var simulateConflictOnNextSave = false
        val resolutions = mutableListOf<CloudConflictResolution>()
        val syncPort = object : CloudCopySync {
            override fun refresh(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) {
                refreshCalls++
                if (refreshCalls == 1) {
                    copy = copy.copy(eTag = "v2", state = CloudCopyState.Clean)
                    callback(Result.success(CloudSyncOutcome.Downloaded(copy)))
                } else {
                    callback(Result.success(CloudSyncOutcome.UpToDate(copy)))
                }
            }
            override fun synchronize(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) {
                syncCalls++
                if (simulateConflictOnNextSave) {
                    copy = copy.copy(
                        state = CloudCopyState.Conflict,
                        uploadAttempted = false,
                        conflict = CloudCopyConflict("v3", 1024L, 1L, "v4-remote", 2048L, 2L)
                    )
                    callback(Result.success(CloudSyncOutcome.Conflict))
                } else {
                    copy = copy.copy(eTag = "v3", state = CloudCopyState.Clean, uploadAttempted = false)
                    callback(Result.success(CloudSyncOutcome.Verified(copy)))
                }
            }
            override fun reconcile(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) = Unit
            override fun resolveConflict(
                id: CloudCopyId,
                resolution: CloudConflictResolution,
                callback: (Result<CloudSyncOutcome>) -> Unit
            ) {
                resolutions += resolution
                copy = copy.copy(eTag = "v5-resolved", state = CloudCopyState.Clean, conflict = null)
                callback(Result.success(CloudSyncOutcome.Verified(copy)))
            }
        }
        compose.setContent { VaultkTheme {
            VaultContentScreen(
                content = currentContent,
                fallbackName = "sample.kdbx",
                onClose = {},
                onChangeVault = {},
                biometricEnabled = false,
                onEnableBiometric = { key, complete -> key.fill('\u0000'); complete(null) },
                onDisableBiometric = {},
                copy = copy,
                editVault = EditVault(KdbxVaultValueFactory()),
                onCloudSave = { changed, complete ->
                    currentContent = changed
                    copy = copy.copy(state = CloudCopyState.Pending, uploadAttempted = false)
                    complete(null)
                },
                synchronize = SynchronizeCloudCopy(syncPort),
                cloudAutoSync = true,
                onRemoteDownloaded = {
                    remoteDownloads++
                    currentContent = VaultContent("Synthetic",
                        VaultGroup("root", "Root", emptyList(), listOf(initialEntry, remoteEntry)))
                },
                isCloudVault = true
            )
        } }
        // 1. Automatic refresh on open downloads remote changes and reloads content.
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("RemoteDownloadedEntry").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Se descargaron cambios remotos de OneDrive", substring = true).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, refreshCalls)
            assertEquals(1, remoteDownloads)
        }

        // 2. Manual "Sincronizar ahora" from the menu on Clean copy checks OneDrive and reports UpToDate.
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Sincronizar ahora").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("La copia local ya está sincronizada con OneDrive", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle { assertEquals(2, refreshCalls) }

        // 3. Saving a local change automatically triggers synchronize and returns to Clean.
        compose.onNodeWithText("InitialEntry").performClick()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Duplicar").performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Cambios verificados en OneDrive", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle { assertEquals(1, syncCalls) }

        // 4. Remote conflict during save blocks editing, displays both versions, and allows explicit KeepLocal resolution.
        simulateConflictOnNextSave = true
        compose.onNodeWithText("RemoteDownloadedEntry").performClick()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Duplicar").performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Conflicto con OneDrive: edición bloqueada", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Nueva entrada").assertDoesNotExist()
        compose.onNodeWithText("Versión local: 1024 B (base v3) · Versión en OneDrive: 2048 B (v4-remote)").assertIsDisplayed()
        compose.onNodeWithText("Usar versión de OneDrive").assertIsDisplayed()
        compose.onNodeWithText("Conservar versión local").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Se conservó la versión local y se sincronizó con OneDrive.", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle {
            assertEquals(2, syncCalls)
            assertEquals(listOf(CloudConflictResolution.KeepLocal), resolutions)
        }
    }

    @Test fun cloudVaultShowsOriginLastSyncStatusDialogUnlinkConfirmationAndSettingsControls() {
        val id = CloudCopyId("account", "drive", "item")
        val copy = CloudCopy(id, "sample.kdbx", "etag-v1", 1024L, 1_700_000_000_000L, CloudCopyState.Clean)
        val entry = VaultEntry("entry-1", "SampleEntry", "user", "", "", null)
        val content = VaultContent("Synthetic", VaultGroup("root", "Root", emptyList(), listOf(entry)))
        var autoSyncEnabled by mutableStateOf(true)
        var allowMeteredEnabled by mutableStateOf(false)
        var unlinked = 0
        var refreshCalls = 0
        val syncPort = object : CloudCopySync {
            override fun refresh(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) {
                refreshCalls++
                callback(Result.success(CloudSyncOutcome.UpToDate(copy)))
            }
            override fun synchronize(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) = Unit
            override fun reconcile(id: CloudCopyId, callback: (Result<CloudSyncOutcome>) -> Unit) = Unit
            override fun resolveConflict(
                id: CloudCopyId,
                resolution: CloudConflictResolution,
                callback: (Result<CloudSyncOutcome>) -> Unit
            ) = Unit
        }
        compose.setContent { VaultkTheme {
            VaultContentScreen(
                content = content,
                fallbackName = "sample.kdbx",
                onClose = {},
                onChangeVault = {},
                biometricEnabled = false,
                onEnableBiometric = { key, complete -> key.fill('\u0000'); complete(null) },
                onDisableBiometric = {},
                copy = copy,
                editVault = EditVault(KdbxVaultValueFactory()),
                onCloudSave = { _, complete -> complete(null) },
                synchronize = SynchronizeCloudCopy(syncPort),
                cloudAutoSync = autoSyncEnabled,
                cloudAllowMetered = allowMeteredEnabled,
                isMeteredNetwork = { true },
                onUnlink = { unlinked++ },
                onCloudAutoSyncChange = { autoSyncEnabled = it },
                onCloudAllowMeteredChange = { allowMeteredEnabled = it },
                isCloudVault = true
            )
        } }
        // 1. Metered network pauses automatic sync on open and displays unified local layout + header subtitle.
        compose.onNodeWithText("Buscar en el vault…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Seleccionar grupo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Más opciones").assertIsDisplayed()
        compose.onNodeWithText("Synthetic · OneDrive · Sincronizado").assertIsDisplayed()
        compose.onNodeWithText("Sincronización automática en pausa por red de datos móviles", substring = true)
            .assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, refreshCalls) }

        // 2. Cloud icon button in the header opens status dialog showing origin, ETag, and last sync.
        compose.onNodeWithContentDescription("Ver estado").performClick()
        compose.onNodeWithText("Estado de sincronización").assertIsDisplayed()
        compose.onNodeWithText("OneDrive · copia privada").assertIsDisplayed()
        compose.onNodeWithText("ETag base:").assertIsDisplayed()
        compose.onNodeWithText("etag-v1").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()

        // 3. "Desvincular" from menu opens confirmation dialog distinguishing unlinking from deletion.
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Desvincular").performClick()
        compose.onNodeWithText("¿Quitar referencia activa?").assertIsDisplayed()
        compose.onNodeWithText("no se borrarán", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Desvincular").performClick()
        compose.runOnIdle { assertEquals(1, unlinked) }

        // 4. Bottom navigation opens "Ajustes" with OneDrive auto-sync and metered network switches.
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Sincronización con OneDrive").assertIsDisplayed()
        compose.onNodeWithText("Usar datos móviles").performClick()
        compose.runOnIdle { assertEquals(true, allowMeteredEnabled) }
    }
}
