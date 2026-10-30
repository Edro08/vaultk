package com.edro08.vaultk.ui.screen.vault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.edro08.vaultk.domain.cloud.CloudCopy
import com.edro08.vaultk.domain.cloud.CloudCopyState
import com.edro08.vaultk.domain.vault.VaultContent
import com.edro08.vaultk.domain.vault.VaultEntry
import com.edro08.vaultk.domain.vault.VaultGroup
import com.edro08.vaultk.domain.vault.VaultSaveProgress
import com.edro08.vaultk.domain.vault.VaultSaveStage
import com.edro08.vaultk.domain.vault.groupsAndSelf
import com.edro08.vaultk.domain.vault.isInside
import com.edro08.vaultk.ui.component.VaultkDestination

/**
 * State holder for [VaultContentScreen].
 * Encapsulates group navigation, search query, dialog visibilities,
 * and save/sync execution states.
 */
internal class VaultContentState(
    initialContent: VaultContent,
    initialCopy: CloudCopy?
) {
    var root by mutableStateOf(initialContent.root)
    var groupId by mutableStateOf(initialContent.root.id)
    var searchQuery by mutableStateOf("")
    var entry by mutableStateOf<VaultEntry?>(null)
    var formOpen by mutableStateOf(false)
    var editingEntry by mutableStateOf<VaultEntry?>(null)
    var generatedPassword by mutableStateOf<String?>(null)
    var hasEdits by mutableStateOf(false)
    var destination by mutableStateOf(VaultkDestination.Vault)

    // Dialog visibilities
    var menuOpen by mutableStateOf(false)
    var groupPickerOpen by mutableStateOf(false)
    var movePickerOpen by mutableStateOf(false)
    var deleteDialogOpen by mutableStateOf(false)
    var discardDialogOpen by mutableStateOf(false)
    var statusDialog by mutableStateOf(false)
    var unlinkDialog by mutableStateOf(false)
    var timeoutDialogOpen by mutableStateOf(false)
    var biometricDialogOpen by mutableStateOf(false)
    var biometricPassword by mutableStateOf("")
    var configuringBiometric by mutableStateOf(false)

    // Discard dialog action flags
    var lockRequested by mutableStateOf(false)
    var changeVaultRequested by mutableStateOf(false)
    var accessRequested by mutableStateOf(false)
    var manageAccessRequested by mutableStateOf(false)

    // Save state
    var saving by mutableStateOf(false)
    var saveStartedAtMillis by mutableLongStateOf(0L)
    var saveProgress by mutableStateOf(VaultSaveProgress(VaultSaveStage.Preparing))
    var saveError by mutableStateOf<String?>(null)

    // Sync state
    var syncing by mutableStateOf(false)
    var syncMessage by mutableStateOf<Int?>(null)
    var syncFailed by mutableStateOf(false)
    var conflictDetected by mutableStateOf(initialCopy?.state == CloudCopyState.Conflict)

    val groups: List<VaultGroup>
        get() = root.groupsAndSelf()

    val currentGroup: VaultGroup
        get() = groups.firstOrNull { it.id == groupId } ?: root

    fun isInRecycleBin(recycleBinId: String?): Boolean =
        recycleBinId?.let { root.isInside(groupId, it) } == true

    fun hasDrafts(isCloudVault: Boolean): Boolean {
        if (isCloudVault) return false
        val allGroups = groups
        val hasDraftGroups = allGroups.any { it.id.startsWith("draft:") }
        val hasDraftEntries = allGroups.any { group -> group.entries.any { it.id.startsWith("draft:") } }
        return hasDraftGroups || hasDraftEntries || hasEdits
    }

    fun hasDraftEntries(): Boolean =
        groups.any { group -> group.entries.any { it.id.startsWith("draft:") } }

    fun resetDiscardFlags() {
        discardDialogOpen = false
        lockRequested = false
        changeVaultRequested = false
        accessRequested = false
        manageAccessRequested = false
    }

    fun selectGroup(id: String) {
        groupId = id
        groupPickerOpen = false
    }

    fun selectEntry(target: VaultEntry?) {
        entry = target
    }

    fun openEditForm(target: VaultEntry) {
        editingEntry = target
        generatedPassword = null
        formOpen = true
    }

    fun openNewEntryForm(prefilledPassword: String? = null) {
        editingEntry = null
        generatedPassword = prefilledPassword
        formOpen = true
    }

    fun closeForm() {
        formOpen = false
        editingEntry = null
        generatedPassword = null
    }

    fun syncFromExternalContent(content: VaultContent) {
        if (root !== content.root) {
            val previousGroup = groups.firstOrNull { it.id == groupId }
            val savedGroups = content.root.groupsAndSelf()
            val savedGroup = savedGroups.firstOrNull { it.id == groupId }
                ?: savedGroups.firstOrNull { it.name == previousGroup?.name }
                ?: content.root
            val selectedEntry = entry
            entry = selectedEntry?.let { selected ->
                savedGroup.entries.firstOrNull { it.id == selected.id }
                    ?: savedGroup.entries.firstOrNull { it.title == selected.title }
            }
            groupId = savedGroup.id
            root = content.root
            hasEdits = false
        }
    }
}

@Composable
internal fun rememberVaultContentState(
    content: VaultContent,
    copy: CloudCopy?
): VaultContentState {
    return remember(content.root.id) {
        VaultContentState(content, copy)
    }
}
