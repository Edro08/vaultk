package com.edro08.vaultk.ui.screen

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.edro08.vaultk.R
import com.edro08.vaultk.domain.Appearance
import com.edro08.vaultk.domain.FileAccessMode
import com.edro08.vaultk.domain.VaultSaveProgress
import com.edro08.vaultk.domain.VaultSaveStage
import com.edro08.vaultk.domain.VaultContent
import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.domain.VaultGroup
import com.edro08.vaultk.domain.groupsAndSelf
import com.edro08.vaultk.application.EditVault
import com.edro08.vaultk.domain.isInside
import com.edro08.vaultk.ui.component.EmptyState
import com.edro08.vaultk.ui.component.EntryCard
import com.edro08.vaultk.ui.component.PasswordField
import com.edro08.vaultk.ui.component.VaultkBottomNavigation
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.component.VaultkDestination
import com.edro08.vaultk.ui.component.VaultkHeader
import com.edro08.vaultk.ui.component.VaultkIconTile
import com.edro08.vaultk.ui.theme.VaultkSpacing
import com.edro08.vaultk.ui.theme.VaultkTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect

/** Local vault changes are persisted when the in-memory tree changes. */
@Composable
fun VaultContentScreen(content: VaultContent, fallbackName: String, onClose: () -> Unit,
    onSave: ((VaultContent, (VaultSaveProgress) -> Unit, (String?) -> Unit) -> Unit)? = null,
    biometricEnabled: Boolean = false,
    onEnableBiometric: ((CharArray, (String?) -> Unit) -> Unit)? = null,
    onDisableBiometric: (() -> Unit)? = null,
    onLock: () -> Unit = onClose,
    timeoutSeconds: Int = 300,
    onTimeoutChange: (Int) -> Unit = {},
    onExternalFormPickerChange: (Boolean) -> Unit = {},
    appearance: Appearance = Appearance.Dark,
    onAppearanceChange: (Appearance) -> Unit = {}, clipboardSeconds: Int = 30,
    onClipboardChange: (Int) -> Unit = {}, searchNotes: Boolean = true,
    onSearchNotesChange: (Boolean) -> Unit = {}, showDetails: Boolean = true,
    onShowDetailsChange: (Boolean) -> Unit = {}, onChangeVault: () -> Unit = {},
    fileAccessMode: FileAccessMode = FileAccessMode.Standard, fileAccessGranted: Boolean = false,
    onFileAccessChange: (FileAccessMode) -> Unit = {}, onManageFileAccess: () -> Unit = {},
    editVault: EditVault) {
    var root by remember(content.root.id) { mutableStateOf(content.root) }
    val latestRoot by rememberUpdatedState(root)
    DisposableEffect(Unit) {
        onDispose {
            fun clear(group: VaultGroup) {
                group.entries.forEach { entry -> entry.attachments.forEach { it.bytes?.fill(0) } }
                group.groups.forEach(::clear)
            }
            clear(latestRoot)
        }
    }
    val groups = remember(root) { root.groupsAndSelf() }
    var groupId by remember(content.root.id) { mutableStateOf(content.root.id) }
    var searchQuery by remember(content.root.id) { mutableStateOf("") }
    val searchResults = remember(root, searchQuery, searchNotes) { editVault.search(root, searchQuery, searchNotes) }
    var entry by remember(content.root.id) { mutableStateOf<VaultEntry?>(null) }
    var formOpen by remember(content.root.id) { mutableStateOf(false) }
    var generatedPassword by remember(content.root.id) { mutableStateOf<String?>(null) }
    var editingEntry by remember(content.root.id) { mutableStateOf<VaultEntry?>(null) }
    var hasEdits by remember(content.root.id) { mutableStateOf(false) }
    var destination by remember(content.root.id) { mutableStateOf(VaultkDestination.Vault) }
    var menuOpen by remember { mutableStateOf(false) }
    var groupPickerOpen by remember { mutableStateOf(false) }
    var movePickerOpen by remember { mutableStateOf(false) }
    var deleteDialogOpen by remember { mutableStateOf(false) }
    var discardDialogOpen by remember { mutableStateOf(false) }
    var lockRequested by remember { mutableStateOf(false) }
    var changeVaultRequested by remember { mutableStateOf(false) }
    var accessRequested by remember { mutableStateOf(false) }
    var manageAccessRequested by remember { mutableStateOf(false) }
    var timeoutDialogOpen by remember { mutableStateOf(false) }
    var biometricDialogOpen by remember { mutableStateOf(false) }
    var biometricPassword by remember { mutableStateOf("") }
    var configuringBiometric by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveStartedAtMillis by remember { mutableLongStateOf(0L) }
    var saveProgress by remember { mutableStateOf(VaultSaveProgress(VaultSaveStage.Preparing)) }
    val snackbar = remember { SnackbarHostState() }
    val biometricSuccess = stringResource(R.string.content_biometric_enabled)
    val scope = rememberCoroutineScope()
    val currentGroup = groups.firstOrNull { it.id == groupId } ?: root
    val inRecycleBin = content.recycleBinId?.let { root.isInside(groupId, it) } == true
    val hasDraftGroups = groups.any { it.id.startsWith("draft:") }
    val hasDraftEntries = groups.any { group -> group.entries.any { it.id.startsWith("draft:") } }
    val hasDrafts = hasDraftGroups || hasDraftEntries || hasEdits
    LaunchedEffect(content) {
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
    val saveChanges: () -> Unit = {
        if (hasDrafts && !saving && onSave != null) {
            saveStartedAtMillis = SystemClock.elapsedRealtime()
            saveProgress = VaultSaveProgress(VaultSaveStage.Preparing)
            saving = true
            onSave(content.copy(root = root), { update ->
                if (saveProgress.stage != update.stage) saveProgress = VaultSaveProgress(update.stage)
            }) { error ->
                saving = false
                if (error != null) scope.launch { snackbar.showSnackbar(error) }
            }
        }
    }
    LaunchedEffect(root) {
        if (root !== content.root) saveChanges()
    }
    val requestClose = { if (!saving) { if (hasDrafts) discardDialogOpen = true else onClose() } }
    val requestLock = { if (!saving) {
        if (hasDrafts) { lockRequested = true; discardDialogOpen = true } else onLock()
    } }
    val requestChangeVault = { if (!saving) {
        if (hasDrafts) { changeVaultRequested = true; discardDialogOpen = true } else onChangeVault()
    } }
    val requestFileAccess: (FileAccessMode) -> Unit = { mode ->
        if (!saving) {
            if (mode == FileAccessMode.Full && !fileAccessGranted && hasDrafts) {
                accessRequested = true
                discardDialogOpen = true
            } else onFileAccessChange(mode)
        }
    }
    val requestManageAccess = { if (!saving) {
        if (hasDrafts) { manageAccessRequested = true; discardDialogOpen = true }
        else onManageFileAccess()
    } }
    if (formOpen) {
        EntryFormScreen(currentGroup.name.ifBlank { stringResource(R.string.content_root) },
            onSave = { form ->
                val previous = editingEntry
                val result = editVault.upsert(root, groupId, form, previous)
                root = result.root
                if (previous != null) hasEdits = true
                formOpen = false
                editingEntry = null
                generatedPassword = null
                entry = result.entry
            }, onBack = { formOpen = false; editingEntry = null; generatedPassword = null },
            initialEntry = editingEntry, initialPassword = generatedPassword,
            onNavigate = { target ->
                formOpen = false; editingEntry = null; generatedPassword = null
                entry = null; destination = target
            },
            onExternalPickerChange = onExternalFormPickerChange)
        return
    }
    BackHandler { requestClose() }
    BackHandler(entry != null || destination != VaultkDestination.Vault) {
        if (entry != null) entry = null else destination = VaultkDestination.Vault
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (entry != null) {
                EntryDetailHeader(entry!!, currentGroup.name.ifBlank { stringResource(R.string.content_root) },
                    onBack = { entry = null },
                    onEdit = { editingEntry = entry; formOpen = true },
                     onSave = if (hasDrafts && onSave != null && !saving) saveChanges else null,
                     onDuplicate = {
                        val result = editVault.duplicate(root, groupId, entry!!)
                        root = result.root
                        entry = result.entry
                    },
                    onMove = { movePickerOpen = true },
                    onDelete = { deleteDialogOpen = true },
                     onRestore = if (inRecycleBin) { { movePickerOpen = true } } else null,
                     onLock = requestLock)
            } else if (destination == VaultkDestination.Vault) {
                Row(Modifier.fillMaxWidth().padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                            Text(stringResource(R.string.content_vault_status,
                                content.name.ifBlank { fallbackName.removeSuffix(".kdbx") },
                                stringResource(R.string.vault_origin_local),
                                stringResource(if (hasDrafts) R.string.content_unsaved_status else R.string.content_open_status)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                      CloseMenu(menuOpen, { menuOpen = it }, requestClose,
                           if (hasDrafts && onSave != null && !saving) saveChanges else null,
                          biometricEnabled, if (onEnableBiometric != null) { { biometricDialogOpen = true } } else null,
                           onDisableBiometric, requestLock, requestChangeVault)
                }
            } else {
                VaultkHeader(
                    title = stringResource(if (destination == VaultkDestination.Settings)
                        R.string.common_settings_tab else R.string.generator_title),
                      actions = { CloseMenu(menuOpen, { menuOpen = it }, requestClose,
                           if (hasDrafts && onSave != null && !saving) saveChanges else null,
                          biometricEnabled, if (onEnableBiometric != null) { { biometricDialogOpen = true } } else null,
                          onDisableBiometric, requestLock) },
                )
            }
        },
        bottomBar = { VaultkBottomNavigation(destination) { target ->
            entry = null
            destination = target
        } },
        floatingActionButton = {
            if (destination == VaultkDestination.Vault && entry == null) {
                FloatingActionButton(onClick = { editingEntry = null; formOpen = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_new_entry))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        when {
            entry != null -> EntryDetailScreen(entry!!, Modifier.padding(insets), showDetails) { message ->
                scope.launch { snackbar.showSnackbar(message) }
            }
            destination == VaultkDestination.Generator -> PasswordGeneratorScreen(Modifier.padding(insets),
                onUse = { password ->
                    generatedPassword = password
                    editingEntry = null
                    entry = null
                    destination = VaultkDestination.Vault
                    formOpen = true
                })
            destination == VaultkDestination.Settings -> {
                SettingsScreen(appearance, onAppearanceChange,
                    biometricEnabled, onBiometricClick = {
                        if (biometricEnabled) onDisableBiometric?.invoke()
                        else if (onEnableBiometric != null) biometricDialogOpen = true
                    }, timeoutSeconds, { timeoutDialogOpen = true }, clipboardSeconds, onClipboardChange,
                    searchNotes, onSearchNotesChange, showDetails, onShowDetailsChange,
                    Modifier.padding(insets), fileAccessMode,
                    fileAccessGranted, requestFileAccess, requestManageAccess)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(insets),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = VaultkSpacing.normal, end = VaultkSpacing.normal,
                    top = VaultkSpacing.medium, bottom = VaultkSpacing.extraLarge),
                verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
            ) {
                item {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(stringResource(R.string.content_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {{
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, stringResource(R.string.content_search_clear))
                            }
                        }} else null)
                }
                if (searchQuery.isNotBlank()) {
                    item {
                        Text(stringResource(R.string.content_search_results, searchResults.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (searchResults.isEmpty()) item {
                        VaultkCard { EmptyState(stringResource(R.string.content_search_empty),
                            stringResource(R.string.content_search_empty_description)) }
                    }
                    items(searchResults, key = { it.entry.id }) { result ->
                        val path = result.groupPath.filter { it.isNotBlank() }.joinToString(" / ")
                            .ifBlank { stringResource(R.string.content_root) }
                        EntryCard(result.entry.title,
                            if (result.entry.username.isBlank()) path
                            else stringResource(R.string.content_search_result_context, result.entry.username, path),
                            onClick = { groupId = result.groupId; entry = result.entry })
                    }
                } else {
                    item {
                        Text(stringResource(R.string.content_group),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        VaultkCard(Modifier.clickable { groupPickerOpen = true }) {
                            Row(Modifier.fillMaxWidth().padding(VaultkSpacing.medium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
                                VaultkIconTile(Icons.Default.Folder, null)
                                Text(currentGroup.name.ifBlank { stringResource(R.string.content_root) },
                                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                Icon(Icons.Default.ArrowDropDown, stringResource(R.string.content_select_group))
                            }
                        }
                    }
                    if (hasDraftGroups) item {
                        Text(stringResource(R.string.groups_session_notice),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (hasDraftEntries || hasEdits) item {
                        Text(stringResource(if (hasEdits) R.string.form_changes_notice else R.string.form_session_notice),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        Text(stringResource(R.string.content_entries),
                            modifier = Modifier.padding(top = VaultkSpacing.medium),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (currentGroup.entries.isEmpty()) {
                        item {
                            VaultkCard { EmptyState(stringResource(R.string.content_empty),
                                stringResource(R.string.content_empty_description)) }
                        }
                    } else {
                        items(currentGroup.entries, key = { it.id }) { item ->
                            EntryCard(item.title, item.username, onClick = { entry = item }, actions = {
                                if (item.id.startsWith("draft:")) Text(stringResource(R.string.form_draft),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error)
                            })
                        }
                    }
                }
            }
        }
    }
    if (groupPickerOpen) {
        GroupPickerSheet(root, groupId,
            onDismiss = { groupPickerOpen = false },
            onSelect = { groupId = it; groupPickerOpen = false },
             onCreate = { parentId, name ->
                val (updated, id) = editVault.createGroup(root, parentId, name)
                root = updated
                groupId = id
                groupPickerOpen = false
            })
    }
    if (movePickerOpen && entry != null) {
        GroupPickerSheet(root, groupId,
            onDismiss = { movePickerOpen = false },
            onSelect = { targetId ->
                val item = entry!!
                root = editVault.move(root, groupId, targetId, item.id)
                groupId = targetId
                hasEdits = true
                movePickerOpen = false
            }, onCreate = { _, _ -> }, selectionMode = true,
            selectable = { target ->
                target.id != groupId && (content.recycleBinId == null ||
                    !root.isInside(target.id, content.recycleBinId))
            })
    }
    if (deleteDialogOpen && entry != null) AlertDialog(
        onDismissRequest = { deleteDialogOpen = false },
        title = { Text(stringResource(R.string.detail_delete_title)) },
        text = { Text(stringResource(if (inRecycleBin || content.recycleBinId == null)
            R.string.detail_delete_permanent_message else R.string.detail_delete_trash_message)) },
        confirmButton = {
            TextButton(onClick = {
                val item = entry!!
                root = editVault.delete(root, groupId, item.id, content.recycleBinId)
                hasEdits = true
                deleteDialogOpen = false
                entry = null
            }) { Text(stringResource(R.string.detail_delete_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { deleteDialogOpen = false }) {
                Text(stringResource(R.string.groups_cancel))
            }
        },
    )
    if (discardDialogOpen) {
        AlertDialog(
            onDismissRequest = { discardDialogOpen = false },
             title = { Text(stringResource(if (hasDraftEntries || hasEdits) R.string.content_discard_title else R.string.groups_discard_title)) },
              text = { Text(stringResource(when {
                   lockRequested -> R.string.content_lock_discard_message
                   changeVaultRequested -> R.string.content_change_vault_discard_message
                    accessRequested || manageAccessRequested -> R.string.settings_access_discard_message
                  hasDraftEntries || hasEdits -> R.string.content_discard_message
                  else -> R.string.groups_discard_message
              })) },
            confirmButton = {
                 TextButton(onClick = {
                     discardDialogOpen = false
                      if (lockRequested) onLock()
                      else if (changeVaultRequested) onChangeVault()
                       else if (accessRequested) onFileAccessChange(FileAccessMode.Full)
                       else if (manageAccessRequested) onManageFileAccess()
                      else onClose()
                 }) {
                    Text(stringResource(R.string.groups_discard_confirm))
                }
            },
            dismissButton = {
                  TextButton(onClick = {
                      discardDialogOpen = false; lockRequested = false; changeVaultRequested = false
                       accessRequested = false; manageAccessRequested = false
                  }) {
                    Text(stringResource(R.string.groups_discard_cancel))
                }
            },
        )
    }
    if (saving) VaultSaveDialog(saveProgress, saveStartedAtMillis)
    if (timeoutDialogOpen) LockTimeoutDialog(timeoutSeconds, onTimeoutChange) { timeoutDialogOpen = false }
    if (biometricDialogOpen) AlertDialog(
        onDismissRequest = { if (!configuringBiometric) { biometricPassword = ""; biometricDialogOpen = false } },
        title = { Text(stringResource(R.string.content_biometric_enable)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
                Text(stringResource(R.string.content_biometric_description))
                PasswordField(biometricPassword, { biometricPassword = it },
                    label = stringResource(R.string.unlock_master_key_hint))
                if (configuringBiometric) CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(enabled = biometricPassword.isNotEmpty() && !configuringBiometric,
                modifier = Modifier.testTag("biometric-enable-confirm"), onClick = {
                val key = biometricPassword.toCharArray()
                biometricPassword = ""
                configuringBiometric = true
                onEnableBiometric?.invoke(key) { error ->
                    configuringBiometric = false
                    biometricDialogOpen = false
                    scope.launch { snackbar.showSnackbar(error ?: biometricSuccess) }
                }
            }) { Text(stringResource(R.string.content_biometric_enable)) }
        },
        dismissButton = {
            TextButton(enabled = !configuringBiometric,
                onClick = { biometricPassword = ""; biometricDialogOpen = false }) {
                Text(stringResource(R.string.groups_cancel))
            }
        },
    )
}

@Composable
private fun CloseMenu(expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onClose: () -> Unit,
    onSave: (() -> Unit)?, biometricEnabled: Boolean, onEnableBiometric: (() -> Unit)?,
    onDisableBiometric: (() -> Unit)?, onLock: () -> Unit,
    onChangeVault: (() -> Unit)? = null) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.content_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.content_lock)) },
                onClick = { onExpandedChange(false); onLock() })
            if (onChangeVault != null) DropdownMenuItem(
                text = { Text(stringResource(R.string.unlock_change_vault)) },
                onClick = { onExpandedChange(false); onChangeVault() })
            if (onSave != null) DropdownMenuItem(text = { Text(stringResource(R.string.content_save_action)) },
                onClick = { onExpandedChange(false); onSave() })
            if (biometricEnabled && onDisableBiometric != null)
                DropdownMenuItem(text = { Text(stringResource(R.string.content_biometric_disable)) },
                    onClick = { onExpandedChange(false); onDisableBiometric() })
            else if (!biometricEnabled && onEnableBiometric != null)
                DropdownMenuItem(text = { Text(stringResource(R.string.content_biometric_enable)) },
                    onClick = { onExpandedChange(false); onEnableBiometric() })
            DropdownMenuItem(text = { Text(stringResource(R.string.content_close)) },
                onClick = { onExpandedChange(false); onClose() })
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VaultHomePreview() {
    VaultkTheme {
        VaultContentScreen(
            VaultContent("Edro", VaultGroup("root", "Raíz",
                listOf(VaultGroup("services", "Servicios", emptyList(),
                    listOf(VaultEntry("entry", "Mega", "usuario", "", "", null)))), emptyList())),
            "Edro.kdbx", {}, editVault = EditVault(com.edro08.vaultk.domain.VaultValueFactory { _, _ ->
                error("Preview does not edit entries")
            }),
        )
    }
}
