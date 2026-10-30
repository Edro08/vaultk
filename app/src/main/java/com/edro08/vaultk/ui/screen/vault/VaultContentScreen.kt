package com.edro08.vaultk.ui.screen.vault

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.edro08.vaultk.R
import com.edro08.vaultk.application.EditVault
import com.edro08.vaultk.application.SynchronizeCloudCopy
import com.edro08.vaultk.domain.Appearance
import com.edro08.vaultk.domain.CloudConflictResolution
import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.CloudCopyState
import com.edro08.vaultk.domain.CloudSyncOutcome
import com.edro08.vaultk.domain.FileAccessMode
import com.edro08.vaultk.domain.VaultSaveProgress
import com.edro08.vaultk.domain.VaultSaveStage
import com.edro08.vaultk.domain.VaultContent
import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.domain.VaultGroup
import com.edro08.vaultk.domain.groupsAndSelf
import com.edro08.vaultk.domain.isInside
import com.edro08.vaultk.domain.searchEntries
import com.edro08.vaultk.ui.component.EmptyState
import com.edro08.vaultk.ui.component.EntryCard
import com.edro08.vaultk.ui.component.PasswordField
import com.edro08.vaultk.ui.component.VaultkBottomNavigation
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.component.VaultkDestination
import com.edro08.vaultk.ui.component.VaultkHeader
import com.edro08.vaultk.ui.component.VaultkIconTile
import com.edro08.vaultk.ui.component.VaultkNotificationCard
import com.edro08.vaultk.ui.theme.VaultkSpacing
import com.edro08.vaultk.ui.theme.VaultkTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import com.edro08.vaultk.domain.VaultValueFactory
import com.edro08.vaultk.ui.screen.storage.VaultSaveDialog
import com.edro08.vaultk.ui.screen.generator.PasswordGeneratorScreen
import com.edro08.vaultk.ui.screen.settings.LockTimeoutDialog
import com.edro08.vaultk.ui.screen.settings.SettingsScreen

/** Unified vault presentation for both local and OneDrive private copies. */
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
    cloudAutoSync: Boolean = true, onCloudAutoSyncChange: (Boolean) -> Unit = {},
    cloudAllowMetered: Boolean = true, onCloudAllowMeteredChange: (Boolean) -> Unit = {},
    editVault: EditVault? = null,
    isCloudVault: Boolean = false,
    copy: CloudCopy? = null,
    onCloudSave: ((VaultContent, (String?) -> Unit) -> Unit)? = null,
    synchronize: SynchronizeCloudCopy? = null,
    isMeteredNetwork: () -> Boolean = { false },
    onCopyUpdated: () -> Unit = {},
    onRemoteDownloaded: () -> Unit = {},
    onViewStatus: (() -> Unit)? = null,
    onUnlink: (() -> Unit)? = null) {
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
    val searchResults = remember(root, searchQuery, searchNotes, editVault) {
        editVault?.search(root, searchQuery, searchNotes) ?: root.searchEntries(searchQuery, searchNotes)
    }
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
    var saveError by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<Int?>(null) }
    var syncFailed by remember { mutableStateOf(false) }
    var conflictDetected by remember(copy?.id) { mutableStateOf(copy?.state == CloudCopyState.Conflict) }
    var statusDialog by remember { mutableStateOf(false) }
    var unlinkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(syncMessage) {
        if (syncMessage != null) {
            delay(4000)
            syncMessage = null
        }
    }

    LaunchedEffect(saveError) {
        if (saveError != null) {
            delay(4000)
            saveError = null
        }
    }
    val currentCopy by rememberUpdatedState(copy)
    val latestSynchronize by rememberUpdatedState(synchronize)
    val latestAllowMetered by rememberUpdatedState(cloudAllowMetered)
    val latestIsMeteredNetwork by rememberUpdatedState(isMeteredNetwork)
    val latestOnCopyUpdated by rememberUpdatedState(onCopyUpdated)
    val latestOnRemoteDownloaded by rememberUpdatedState(onRemoteDownloaded)
    val inConflict = isCloudVault && (copy?.state == CloudCopyState.Conflict || conflictDetected)
    val editable = if (isCloudVault) {
        editVault != null && onCloudSave != null && !inConflict && (copy == null || copy.bytes in 8..250_000_000L)
    } else {
        editVault != null
    }
    val snackbar = remember { SnackbarHostState() }
    val biometricSuccess = stringResource(R.string.content_biometric_enabled)
    val scope = rememberCoroutineScope()
    val currentGroup = groups.firstOrNull { it.id == groupId } ?: root
    val inRecycleBin = content.recycleBinId?.let { root.isInside(groupId, it) } == true
    val hasDraftGroups = !isCloudVault && groups.any { it.id.startsWith("draft:") }
    val hasDraftEntries = !isCloudVault && groups.any { group -> group.entries.any { it.id.startsWith("draft:") } }
    val hasDrafts = !isCloudVault && (hasDraftGroups || hasDraftEntries || hasEdits)
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
    val handleSyncResult: (Boolean, Boolean) -> (Result<CloudSyncOutcome>) -> Unit = { fromRefresh, manual ->
        { result ->
            syncing = false
            result.onSuccess { outcome ->
                when (outcome) {
                    is CloudSyncOutcome.Verified -> {
                        conflictDetected = false
                        syncFailed = false
                        syncMessage = R.string.cloud_copies_sync_verified
                        latestOnCopyUpdated()
                    }
                    is CloudSyncOutcome.UpToDate -> {
                        conflictDetected = false
                        syncFailed = false
                        syncMessage = if (manual) R.string.cloud_copies_sync_up_to_date else null
                        latestOnCopyUpdated()
                    }
                    is CloudSyncOutcome.Downloaded -> {
                        conflictDetected = false
                        syncFailed = false
                        syncMessage = R.string.cloud_copies_sync_downloaded
                        latestOnRemoteDownloaded()
                        latestOnCopyUpdated()
                    }
                    is CloudSyncOutcome.Rearmed -> {
                        conflictDetected = false
                        syncFailed = false
                        syncMessage = R.string.cloud_copies_sync_rearmed
                        latestOnCopyUpdated()
                    }
                    CloudSyncOutcome.Conflict -> {
                        conflictDetected = true
                        syncFailed = true
                        syncMessage = R.string.cloud_copies_sync_conflict
                        latestOnCopyUpdated()
                    }
                    CloudSyncOutcome.Missing -> {
                        syncFailed = true
                        syncMessage = R.string.cloud_copies_sync_missing
                        latestOnCopyUpdated()
                    }
                    CloudSyncOutcome.StillPending -> {
                        latestOnCopyUpdated()
                        val latest = currentCopy
                        if (fromRefresh && latest?.uploadAttempted != true && !manual) {
                            syncFailed = false
                        } else {
                            syncFailed = true
                            syncMessage = if (latest?.uploadAttempted == true)
                                R.string.cloud_copies_sync_uncertain
                            else R.string.cloud_copies_sync_pending_offline
                        }
                    }
                }
            }.onFailure {
                syncFailed = true
                val latest = currentCopy
                syncMessage = if (latest?.state == CloudCopyState.Pending) {
                    if (latest.uploadAttempted) R.string.cloud_copies_sync_uncertain
                    else R.string.cloud_copies_sync_pending_offline
                } else R.string.cloud_copies_sync_error
                latestOnCopyUpdated()
            }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, copy?.id, cloudAutoSync, isCloudVault) {
        val observer = LifecycleEventObserver { _, event ->
            val active = currentCopy
            val sync = latestSynchronize
            if (isCloudVault && event == Lifecycle.Event.ON_START && cloudAutoSync && sync != null &&
                active != null && !saving && !syncing && !formOpen &&
                active.state != CloudCopyState.Conflict && !conflictDetected) {
                if (!latestAllowMetered && latestIsMeteredNetwork()) {
                    syncFailed = false
                    syncMessage = R.string.cloud_metered_paused
                } else {
                    syncing = true
                    sync.refresh(active.id, handleSyncResult(true, false))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val persistCloudChange: (VaultGroup, VaultEntry?) -> Unit = { updatedRoot, updatedEntry ->
        val previousRoot = root
        val previousEntry = entry
        root = updatedRoot
        entry = updatedEntry
        saveError = null
        saving = true
        onCloudSave?.invoke(content.copy(root = updatedRoot)) { error ->
            saving = false
            if (error != null) {
                root = previousRoot
                entry = previousEntry
                saveError = error
            } else {
                onCopyUpdated()
                val active = currentCopy
                if (cloudAutoSync && synchronize != null && active != null && !syncing) {
                    if (!latestAllowMetered && latestIsMeteredNetwork()) {
                        syncFailed = false
                        syncMessage = R.string.cloud_metered_paused
                    } else {
                        syncing = true
                        syncMessage = null
                        if (active.uploadAttempted) {
                            synchronize.reconcile(active.id, handleSyncResult(false, false))
                        } else {
                            synchronize(active.id, handleSyncResult(false, false))
                        }
                    }
                }
            }
        }
    }
    val saveChanges: () -> Unit = {
        if (!isCloudVault && hasDrafts && !saving && onSave != null) {
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
        if (!isCloudVault && root !== content.root) saveChanges()
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
    val cloudSyncActionLabel: Int? = if (isCloudVault && copy != null && synchronize != null && !inConflict) {
        when {
            copy.state == CloudCopyState.Pending && copy.uploadAttempted -> R.string.cloud_copies_reconcile
            copy.state == CloudCopyState.Pending -> R.string.cloud_copies_sync_now
            else -> R.string.cloud_sync_now
        }
    } else null
    val triggerCloudSyncAction: (() -> Unit)? = if (cloudSyncActionLabel != null && copy != null && synchronize != null) {
        {
            if (!saving && !syncing) {
                syncing = true
                syncMessage = null
                when {
                    copy.state == CloudCopyState.Pending && copy.uploadAttempted ->
                        synchronize.reconcile(copy.id, handleSyncResult(false, true))
                    copy.state == CloudCopyState.Pending ->
                        synchronize(copy.id, handleSyncResult(false, true))
                    else ->
                        synchronize.refresh(copy.id, handleSyncResult(true, true))
                }
            }
        }
    } else null
    val isWarningStatus = isCloudVault && (
        inConflict ||
            copy?.state == CloudCopyState.Pending ||
            (syncFailed && copy?.state == CloudCopyState.Clean)
    )
    if (formOpen && editable && editVault != null) {
        EntryFormScreen(
            currentGroup.name.ifBlank { stringResource(R.string.content_root) },
            onSave = { form ->
                val previous = editingEntry
                val result = editVault.upsert(root, groupId, form, previous)
                formOpen = false
                editingEntry = null
                generatedPassword = null
                if (isCloudVault) {
                    persistCloudChange(result.root, result.entry)
                } else {
                    root = result.root
                    if (previous != null) hasEdits = true
                    entry = result.entry
                }
            }, onBack = { formOpen = false; editingEntry = null; generatedPassword = null },
            initialEntry = editingEntry, initialPassword = generatedPassword,
            onNavigate = { target ->
                formOpen = false; editingEntry = null; generatedPassword = null
                entry = null; destination = target
            },
            onExternalPickerChange = onExternalFormPickerChange
        )
        return
    }
    BackHandler { requestClose() }
    BackHandler(entry != null || destination != VaultkDestination.Vault) {
        if (entry != null) entry = null else destination = VaultkDestination.Vault
    }
    val cloudIcon = when {
        inConflict || (syncFailed && copy?.state == CloudCopyState.Clean) -> Icons.Outlined.CloudOff
        saving || syncing -> Icons.Outlined.CloudSync
        copy?.state == CloudCopyState.Pending -> Icons.Outlined.CloudUpload
        copy == null || !editable || copy.bytes > 250_000_000L -> Icons.Outlined.CloudQueue
        else -> Icons.Outlined.CloudDone
    }
    val cloudIconTint = when {
        saving || syncing -> MaterialTheme.colorScheme.primary
        isWarningStatus -> MaterialTheme.colorScheme.error
        cloudIcon == Icons.Outlined.CloudDone -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (entry != null) {
                if (editable && editVault != null) {
                    EntryDetailHeader(
                        entry!!,
                        currentGroup.name.ifBlank { stringResource(R.string.content_root) },
                        onBack = { entry = null },
                        onEdit = { editingEntry = entry; formOpen = true },
                        onSave = if (hasDrafts && onSave != null && !saving) saveChanges else null,
                        onDuplicate = {
                            val result = editVault.duplicate(root, groupId, entry!!)
                            if (isCloudVault) {
                                persistCloudChange(result.root, result.entry)
                            } else {
                                root = result.root
                                entry = result.entry
                            }
                        },
                        onMove = { movePickerOpen = true },
                        onDelete = { deleteDialogOpen = true },
                        onRestore = if (inRecycleBin) {
                            { movePickerOpen = true }
                        } else null
                    )
                } else {
                    VaultkHeader(
                        title = entry!!.title,
                        subtitle = currentGroup.name.ifBlank { stringResource(R.string.content_root) },
                        onBack = { entry = null },
                        icon = Icons.Default.Key
                    )
                }
            } else if (destination == VaultkDestination.Vault) {
                val cloudShortStatusRes = when {
                    !isCloudVault -> if (hasDrafts) R.string.content_unsaved_status else R.string.content_open_status
                    copy == null && !editable -> R.string.cloud_short_status_readonly
                    copy != null && copy.bytes > 250_000_000L -> R.string.cloud_short_status_readonly
                    inConflict -> R.string.cloud_short_status_conflict
                    saving || syncing -> R.string.cloud_short_status_syncing
                    copy?.state == CloudCopyState.Pending -> R.string.cloud_short_status_pending
                    syncFailed && copy?.state == CloudCopyState.Clean -> R.string.cloud_short_status_error
                    editable -> R.string.cloud_short_status_clean
                    else -> R.string.cloud_short_status_readonly
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                            Box(
                                Modifier.size(8.dp).background(
                                    when {
                                        isWarningStatus -> MaterialTheme.colorScheme.error
                                        saving || syncing -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.tertiary
                                    },
                                    CircleShape
                                )
                            )
                            Text(stringResource(R.string.content_vault_status,
                                content.name.ifBlank { fallbackName.removeSuffix(".kdbx") },
                                stringResource(if (isCloudVault) R.string.open_onedrive else R.string.vault_origin_local),
                                stringResource(cloudShortStatusRes)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (isCloudVault) {
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (saving || syncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(38.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                            }
                            IconButton(
                                onClick = { statusDialog = true },
                                modifier = Modifier.testTag("cloud-status-button")
                            ) {
                                Icon(
                                    imageVector = if (saving || syncing) Icons.Outlined.CloudSync else cloudIcon,
                                    contentDescription = stringResource(R.string.cloud_view_status),
                                    tint = cloudIconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    CloseMenu(
                        expanded = menuOpen,
                        onExpandedChange = { menuOpen = it },
                        onClose = requestClose,
                        onSave = if (hasDrafts && onSave != null && !saving) saveChanges else null,
                        biometricEnabled = biometricEnabled,
                        onEnableBiometric = if (onEnableBiometric != null) { { biometricDialogOpen = true } } else null,
                        onDisableBiometric = onDisableBiometric,
                        onLock = requestLock,
                        onChangeVault = requestChangeVault,
                        cloudSyncLabel = cloudSyncActionLabel,
                        onCloudSync = triggerCloudSyncAction,
                        onCloudViewStatus = if (isCloudVault) { { statusDialog = true } } else null,
                        onCloudUnlink = if (isCloudVault && copy != null && onUnlink != null) { { unlinkDialog = true } } else null
                    )
                }
            } else {
                VaultkHeader(
                    title = stringResource(if (destination == VaultkDestination.Settings)
                        R.string.common_settings_tab else R.string.generator_title),
                    actions = {
                        if (isCloudVault) {
                            Box(
                                modifier = Modifier.size(42.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (saving || syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(38.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    )
                                }
                                IconButton(
                                    onClick = { statusDialog = true },
                                    modifier = Modifier.testTag("cloud-status-button")
                                ) {
                                    Icon(
                                        imageVector = if (saving || syncing) Icons.Outlined.CloudSync else cloudIcon,
                                        contentDescription = stringResource(R.string.cloud_view_status),
                                        tint = cloudIconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        CloseMenu(
                            expanded = menuOpen,
                            onExpandedChange = { menuOpen = it },
                            onClose = requestClose,
                            onSave = if (hasDrafts && onSave != null && !saving) saveChanges else null,
                            biometricEnabled = biometricEnabled,
                            onEnableBiometric = if (onEnableBiometric != null) { { biometricDialogOpen = true } } else null,
                            onDisableBiometric = onDisableBiometric,
                            onLock = requestLock,
                            cloudSyncLabel = cloudSyncActionLabel,
                            onCloudSync = triggerCloudSyncAction,
                            onCloudViewStatus = if (isCloudVault) { { statusDialog = true } } else null,
                            onCloudUnlink = if (isCloudVault && copy != null && onUnlink != null) { { unlinkDialog = true } } else null
                        )
                    },
                )
            }
        },
        bottomBar = { VaultkBottomNavigation(destination) { target ->
            entry = null
            destination = target
        } },
        floatingActionButton = {
            if (destination == VaultkDestination.Vault && entry == null && editable && !saving && !syncing) {
                FloatingActionButton(
                    onClick = { editingEntry = null; formOpen = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_new_entry))
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                VaultkNotificationCard(
                    message = data.visuals.message,
                    isError = false,
                    onDismiss = { data.dismiss() }
                )
            }
        },
    ) { insets ->
        when {
            entry != null -> EntryDetailScreen(
                entry!!,
                Modifier.padding(insets),
                showDetails,
                groupName = currentGroup.name.ifBlank { stringResource(R.string.content_root) }
            ) { message ->
                scope.launch { snackbar.showSnackbar(message) }
            }
            destination == VaultkDestination.Generator -> PasswordGeneratorScreen(
                Modifier.padding(
                    insets
                ),
                onUse = if (editable) {
                    { password ->
                        generatedPassword = password
                        editingEntry = null
                        entry = null
                        destination = VaultkDestination.Vault
                        formOpen = true
                    }
                } else null)
            destination == VaultkDestination.Settings -> {
                SettingsScreen(
                    appearance,
                    onAppearanceChange,
                    biometricEnabled,
                    onBiometricClick = {
                        if (biometricEnabled) onDisableBiometric?.invoke()
                        else if (onEnableBiometric != null) biometricDialogOpen = true
                    },
                    timeoutSeconds,
                    { timeoutDialogOpen = true },
                    clipboardSeconds,
                    onClipboardChange,
                    searchNotes,
                    onSearchNotesChange,
                    showDetails,
                    onShowDetailsChange,
                    Modifier.padding(insets),
                    fileAccessGranted,
                    requestManageAccess,
                    cloudAutoSync,
                    onCloudAutoSyncChange,
                    cloudAllowMetered,
                    onCloudAllowMeteredChange
                )
            }
            else -> Box(modifier = Modifier.fillMaxSize().padding(insets)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = VaultkSpacing.normal, end = VaultkSpacing.normal,
                        top = VaultkSpacing.medium, bottom = VaultkSpacing.extraLarge * 2),
                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
                ) {
                    if (isCloudVault && inConflict && copy != null) {
                        item {
                            VaultkCard {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(VaultkSpacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
                                ) {
                                    Text(
                                        stringResource(R.string.cloud_status_conflict),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    copy.conflict?.let { conflict ->
                                        Text(
                                            stringResource(
                                                R.string.cloud_conflict_versions,
                                                "${conflict.localBytes} B",
                                                conflict.baseEtag,
                                                "${conflict.remoteBytes} B",
                                                conflict.remoteEtag
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (synchronize != null) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                                            OutlinedButton(enabled = !saving && !syncing, onClick = {
                                                syncing = true
                                                syncMessage = null
                                                synchronize.resolveConflict(copy.id, CloudConflictResolution.KeepLocal) { result ->
                                                    handleSyncResult(false, true)(result)
                                                    if (result.getOrNull() is CloudSyncOutcome.Verified) {
                                                        syncMessage = R.string.cloud_conflict_resolved_local
                                                    }
                                                }
                                            }) {
                                                Text(stringResource(R.string.cloud_conflict_keep_local))
                                            }
                                            OutlinedButton(enabled = !saving && !syncing, onClick = {
                                                syncing = true
                                                syncMessage = null
                                                synchronize.resolveConflict(copy.id, CloudConflictResolution.UseRemote) { result ->
                                                    handleSyncResult(false, true)(result)
                                                    if (result.getOrNull() is CloudSyncOutcome.Downloaded) {
                                                        syncMessage = R.string.cloud_conflict_resolved_remote
                                                    }
                                                }
                                            }) {
                                                Text(stringResource(R.string.cloud_conflict_use_remote))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
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
                        val rootLabel = stringResource(R.string.content_root)
                        val formattedGroupPath = remember(root, currentGroup.id) {
                            (findGroupPath(root, currentGroup.id) ?: listOf(currentGroup))
                                .joinToString(" › ") { it.name.ifBlank { rootLabel } }
                        }
                        VaultkCard(Modifier.clickable { groupPickerOpen = true }) {
                            Row(Modifier.fillMaxWidth().padding(VaultkSpacing.medium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
                                VaultkIconTile(Icons.Default.Folder, null)
                                Text(
                                    text = formattedGroupPath,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.content_select_group)
                                )
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
                            VaultkCard {
                                EmptyState(
                                    title = stringResource(R.string.content_empty),
                                    description = stringResource(R.string.content_empty_description),
                                    icon = Icons.Outlined.FolderOpen
                                )
                            }
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

            val hasFab = destination == VaultkDestination.Vault && entry == null && editable && !saving && !syncing
            AnimatedVisibility(
                    visible = (syncMessage != null && !syncing) || saveError != null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (hasFab) 80.dp else VaultkSpacing.small)
                ) {
                    val msg = when {
                        saveError != null -> saveError!!
                        syncMessage != null -> {
                            val displayed = if (
                                syncMessage == R.string.cloud_copies_sync_pending_offline &&
                                copy?.uploadAttempted == true
                            ) {
                                R.string.cloud_copies_sync_uncertain
                            } else {
                                syncMessage!!
                            }
                            stringResource(displayed)
                        }
                        else -> ""
                    }
                    val isErr = saveError != null || syncFailed
                    val isCleanSync = !isErr && (
                        syncMessage == R.string.cloud_copies_sync_up_to_date ||
                        syncMessage == R.string.cloud_copies_sync_downloaded ||
                        syncMessage == R.string.cloud_copies_sync_verified
                    )
                    VaultkNotificationCard(
                        message = msg,
                        isError = isErr,
                        icon = if (isCleanSync) Icons.Outlined.CloudDone else null,
                        onDismiss = {
                            syncMessage = null
                            saveError = null
                        }
                    )
                }
            }
        }
    }
    if (groupPickerOpen) {
        GroupPickerSheet(
            root, groupId,
            onDismiss = { groupPickerOpen = false },
            onSelect = { groupId = it; groupPickerOpen = false },
            onCreate = { parentId, name ->
                if (editable && editVault != null) {
                    val (updated, id) = editVault.createGroup(root, parentId, name)
                    groupId = id
                    groupPickerOpen = false
                    if (isCloudVault) {
                        persistCloudChange(updated, null)
                    } else {
                        root = updated
                    }
                }
            },
            onRename = { targetId, newName ->
                if (editable && editVault != null) {
                    val updated = editVault.renameGroup(root, targetId, newName)
                    if (isCloudVault) {
                        persistCloudChange(updated, null)
                    } else {
                        root = updated
                        hasEdits = true
                    }
                }
            },
            selectionMode = !editable
        )
    }
    if (movePickerOpen && entry != null && editable && editVault != null) {
        GroupPickerSheet(
            root, groupId,
            onDismiss = { movePickerOpen = false },
            onSelect = { targetId ->
                val item = entry!!
                val updated = editVault.move(root, groupId, targetId, item.id)
                groupId = targetId
                movePickerOpen = false
                if (isCloudVault) {
                    persistCloudChange(updated, item)
                } else {
                    root = updated
                    hasEdits = true
                }
            }, onCreate = { _, _ -> }, selectionMode = true,
            selectable = { target ->
                target.id != groupId && (content.recycleBinId == null ||
                        !root.isInside(target.id, content.recycleBinId))
            })
    }
    if (deleteDialogOpen && entry != null && editable && editVault != null) AlertDialog(
        onDismissRequest = { deleteDialogOpen = false },
        title = { Text(stringResource(R.string.detail_delete_title)) },
        text = { Text(stringResource(if (inRecycleBin || content.recycleBinId == null)
            R.string.detail_delete_permanent_message else R.string.detail_delete_trash_message)) },
        confirmButton = {
            TextButton(onClick = {
                val item = entry!!
                val updated = editVault.delete(root, groupId, item.id, content.recycleBinId)
                deleteDialogOpen = false
                if (isCloudVault) {
                    persistCloudChange(updated, null)
                } else {
                    root = updated
                    hasEdits = true
                    entry = null
                }
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
    if (saving && !isCloudVault) VaultSaveDialog(saveProgress, saveStartedAtMillis)
    if (statusDialog && isCloudVault) {
        val statusRes = when {
            copy == null && !editable -> R.string.cloud_read_only
            copy != null && copy.bytes > 250_000_000L -> R.string.cloud_status_oversize
            inConflict -> R.string.cloud_status_conflict
            syncing -> R.string.cloud_status_syncing
            copy?.state == CloudCopyState.Pending && copy.uploadAttempted -> R.string.cloud_status_pending_attempted
            copy?.state == CloudCopyState.Pending -> R.string.cloud_status_pending
            syncFailed && copy?.state == CloudCopyState.Clean -> R.string.cloud_status_error
            editable -> R.string.cloud_status_clean
            else -> R.string.cloud_read_only
        }
        val formatFileSize: (Long) -> String = { bytes ->
            when {
                bytes >= 1_048_576L -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576f)
                bytes >= 1_024L -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1_024f)
                else -> "$bytes B"
            }
        }
        AlertDialog(
            onDismissRequest = { statusDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = stringResource(R.string.cloud_status_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                ) {
                    // Estado 1: Dot + Origen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (isWarningStatus) MaterialTheme.colorScheme.error else Color(0xFF44DB98),
                                    CircleShape
                                )
                        )
                        Text(
                            text = stringResource(R.string.cloud_origin),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Estado 2: Icono + Estado de sincronización
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
                    ) {
                        val statusIcon = when {
                            inConflict || (syncFailed && copy?.state == CloudCopyState.Clean) -> Icons.Outlined.ErrorOutline
                            copy?.state == CloudCopyState.Pending -> Icons.Outlined.CloudUpload
                            copy == null && !editable -> Icons.Outlined.Info
                            else -> Icons.Default.Check
                        }
                        val statusIconTint = when {
                            inConflict || (syncFailed && copy?.state == CloudCopyState.Clean) -> MaterialTheme.colorScheme.error
                            copy?.state == CloudCopyState.Pending -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = statusIconTint
                        )
                        Text(
                            text = stringResource(statusRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (inConflict || copy?.state == CloudCopyState.Pending ||
                                (syncFailed && copy?.state == CloudCopyState.Clean))
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(VaultkSpacing.tiny))

                    // Detalles con iconos
                    // 1. Origen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.cloud_status_detail_origin, stringResource(R.string.open_onedrive)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 2. Archivo
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.cloud_status_detail_file_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = copy?.name ?: fallbackName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (copy != null) {
                        // 3. Tamaño
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.cloud_status_detail_size_label, formatFileSize(copy.bytes)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.cloud_status_detail_size_bytes, copy.bytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 4. Última sinc.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(
                                    R.string.cloud_status_detail_last_sync,
                                    DateFormat.getDateTimeInstance().format(Date(copy.savedAt))
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 5. ETag base
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.cloud_status_detail_etag_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = copy.eTag,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = VaultkSpacing.small),
                    horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                ) {
                    OutlinedButton(
                        onClick = { statusDialog = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(stringResource(R.string.groups_cancel))
                    }
                    if (triggerCloudSyncAction != null && cloudSyncActionLabel != null) {
                        Button(
                            onClick = {
                                statusDialog = false
                                triggerCloudSyncAction()
                            },
                            enabled = !saving && !syncing,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(cloudSyncActionLabel),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            dismissButton = null
        )
    }
    if (unlinkDialog && copy != null && onUnlink != null) AlertDialog(
        onDismissRequest = { unlinkDialog = false },
        title = { Text(stringResource(R.string.cloud_unlink_title)) },
        text = { Text(stringResource(R.string.cloud_unlink_message, copy.name)) },
        confirmButton = {
            TextButton(onClick = { unlinkDialog = false; onUnlink() }) {
                Text(stringResource(R.string.cloud_unlink_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = { unlinkDialog = false }) {
                Text(stringResource(R.string.groups_cancel))
            }
        }
    )
    if (timeoutDialogOpen) LockTimeoutDialog(timeoutSeconds, onTimeoutChange) {
        timeoutDialogOpen = false
    }
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
private fun CloseMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onSave: (() -> Unit)?,
    biometricEnabled: Boolean,
    onEnableBiometric: (() -> Unit)?,
    onDisableBiometric: (() -> Unit)?,
    onLock: () -> Unit,
    onChangeVault: (() -> Unit)? = null,
    cloudSyncLabel: Int? = null,
    onCloudSync: (() -> Unit)? = null,
    onCloudViewStatus: (() -> Unit)? = null,
    onCloudUnlink: (() -> Unit)? = null
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.content_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            // Sección 1: Vault
            DropdownMenuItem(
                text = { Text(stringResource(R.string.content_lock)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null)
                },
                onClick = { onExpandedChange(false); onLock() }
            )
            if (onChangeVault != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.unlock_change_vault)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    },
                    onClick = { onExpandedChange(false); onChangeVault() }
                )
            }
            if (onSave != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.content_save_action)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                    },
                    onClick = { onExpandedChange(false); onSave() }
                )
            }

            // Sección 2: Temas online / OneDrive
            val hasCloud = (cloudSyncLabel != null && onCloudSync != null) || onCloudViewStatus != null || onCloudUnlink != null
            if (hasCloud) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                if (cloudSyncLabel != null && onCloudSync != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(cloudSyncLabel)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.CloudSync, contentDescription = null)
                        },
                        onClick = { onExpandedChange(false); onCloudSync() }
                    )
                }
                if (onCloudViewStatus != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cloud_view_status)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Info, contentDescription = null)
                        },
                        onClick = { onExpandedChange(false); onCloudViewStatus() }
                    )
                }
                if (onCloudUnlink != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cloud_unlink_copy)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.LinkOff, contentDescription = null)
                        },
                        onClick = { onExpandedChange(false); onCloudUnlink() }
                    )
                }
            }

            // Sección 3: Biometría
            val hasBiometric = (biometricEnabled && onDisableBiometric != null) || (!biometricEnabled && onEnableBiometric != null)
            if (hasBiometric) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                if (biometricEnabled && onDisableBiometric != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.content_biometric_disable)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                        },
                        onClick = { onExpandedChange(false); onDisableBiometric() }
                    )
                } else if (!biometricEnabled && onEnableBiometric != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.content_biometric_enable)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                        },
                        onClick = { onExpandedChange(false); onEnableBiometric() }
                    )
                }
            }

            // Sección 4: Cierre del vault
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.content_close),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = { onExpandedChange(false); onClose() }
            )
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
            "Edro.kdbx", {}, editVault = EditVault(VaultValueFactory { _, _ ->
                error("Preview does not edit entries")
            }),
        )
    }
}
