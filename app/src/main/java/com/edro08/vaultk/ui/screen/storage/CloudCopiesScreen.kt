package com.edro08.vaultk.ui.screen.storage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edro08.vaultk.R
import com.edro08.vaultk.application.ManageCloudCopies
import com.edro08.vaultk.application.SynchronizeCloudCopy
import com.edro08.vaultk.domain.CloudConflictResolution
import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.CloudCopyId
import com.edro08.vaultk.domain.CloudCopyState
import com.edro08.vaultk.domain.CloudSyncAction
import com.edro08.vaultk.domain.CloudSyncLogEntry
import com.edro08.vaultk.domain.CloudSyncOutcome
import com.edro08.vaultk.ui.component.EmptyState
import com.edro08.vaultk.ui.component.VaultkBottomNavigation
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.component.VaultkDestination
import com.edro08.vaultk.ui.component.VaultkHeader
import com.edro08.vaultk.ui.component.VaultkIconTile
import com.edro08.vaultk.ui.component.VaultkNotificationCard
import com.edro08.vaultk.ui.theme.VaultkSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudCopiesScreen(
    copies: ManageCloudCopies,
    selected: CloudCopy?,
    onBack: () -> Unit,
    onSelect: (CloudCopy) -> Unit,
    onDeselect: () -> Unit,
    synchronize: SynchronizeCloudCopy? = null,
    destination: VaultkDestination = VaultkDestination.Vault,
    onDestinationChange: (VaultkDestination) -> Unit = {}
) {
    var items by remember { mutableStateOf<List<CloudCopy>?>(null) }
    var recoveryCounts by remember { mutableStateOf<Map<CloudCopyId, Int>>(emptyMap()) }
    var logs by remember { mutableStateOf<List<CloudSyncLogEntry>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var confirm by remember { mutableStateOf<CloudCopy?>(null) }
    var inUseNotice by remember { mutableStateOf<CloudCopy?>(null) }
    var removing by remember { mutableStateOf(false) }
    var removalFailed by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<Int?>(null) }
    var syncError by remember { mutableStateOf(false) }

    LaunchedEffect(syncMessage) {
        if (syncMessage != null) {
            delay(4000)
            syncMessage = null
        }
    }
    var logsExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val handleSyncResult: (Result<CloudSyncOutcome>) -> Unit = { result ->
        syncing = false
        result.onSuccess { outcome ->
            syncError = outcome !is CloudSyncOutcome.Verified &&
                outcome !is CloudSyncOutcome.UpToDate &&
                outcome !is CloudSyncOutcome.Downloaded &&
                outcome !is CloudSyncOutcome.Rearmed
            syncMessage = when (outcome) {
                is CloudSyncOutcome.Verified -> R.string.cloud_copies_sync_verified
                is CloudSyncOutcome.UpToDate -> R.string.cloud_copies_sync_up_to_date
                is CloudSyncOutcome.Downloaded -> R.string.cloud_copies_sync_downloaded
                is CloudSyncOutcome.Rearmed -> R.string.cloud_copies_sync_rearmed
                CloudSyncOutcome.Conflict -> R.string.cloud_copies_sync_conflict
                CloudSyncOutcome.Missing -> R.string.cloud_copies_sync_missing
                CloudSyncOutcome.StillPending -> R.string.cloud_copies_sync_uncertain
            }
        }.onFailure {
            syncError = true
            syncMessage = R.string.cloud_copies_sync_error
        }
        refresh++
    }

    LaunchedEffect(copies, refresh) {
        failed = false
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val loaded = copies.list()
                val counts = loaded.associate { it.id to copies.recoverySnapshots(it.id) }
                val history = copies.logs().take(10)
                Triple(loaded, counts, history)
            }
        }
        result.onSuccess { (loaded, counts, history) ->
            items = loaded
            recoveryCounts = counts
            logs = history
        }.onFailure { failed = true }
    }

    val formatFileSize: (Long) -> String = { bytes ->
        when {
            bytes >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_000_000f)
            bytes >= 1_000 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1_000f)
            else -> "$bytes B"
        }
    }

    val formatDownloadedDate: (Long) -> String = { timestamp ->
        val sdf = SimpleDateFormat("d MMM · h:mm a", Locale.getDefault())
        sdf.format(Date(timestamp))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { VaultkHeader(stringResource(R.string.cloud_copies_open), onBack = onBack) },
        bottomBar = {
            VaultkBottomNavigation(selected = destination, onSelect = onDestinationChange)
        }
    ) { insets ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = VaultkSpacing.normal),
                verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
            ) {
                Text(
                    stringResource(R.string.cloud_copies_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (syncing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        stringResource(R.string.cloud_copies_syncing),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when {
                    failed -> Text(stringResource(R.string.cloud_copies_error), color = MaterialTheme.colorScheme.error)
                    items == null -> CircularProgressIndicator()
                    items!!.isEmpty() -> {
                        VaultkCard(Modifier.fillMaxWidth()) {
                            EmptyState(
                                title = stringResource(R.string.cloud_copies_empty_title),
                                description = stringResource(R.string.cloud_copies_empty),
                                icon = Icons.Outlined.CloudOff
                            )
                        }
                    }
                    else -> {
                    Text(
                        text = stringResource(R.string.open_copies_count_header, items!!.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(items!!, key = { "${it.id.accountId}:${it.id.driveId}:${it.id.itemId}" }) { copy ->
                            VaultkCard(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(VaultkSpacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                                ) {
                                    // Row with icon, title, badge, account
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        VaultkIconTile(Icons.Outlined.Lock, null)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
                                        ) {
                                            Text(
                                                text = copy.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            // Status badges
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.cloud_short_status_readonly),
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (selected?.id == copy.id) {
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                                        modifier = Modifier.padding(vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.cloud_copies_active),
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Cloud,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.open_onedrive
                                                    ) + if (copy.accountName.isNotBlank()) " · ${copy.accountName}" else "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Size and downloaded date stats
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Storage,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = formatFileSize(copy.bytes),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "|",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.Download,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.open_copy_downloaded_at,
                                                formatDownloadedDate(copy.savedAt)
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Status badge (Sin cambios locales pendientes, etc.)
                                    val statusColor = if (copy.state == CloudCopyState.Clean) Color(0xFF44DB98) else MaterialTheme.colorScheme.error
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(when (copy.state) {
                                                CloudCopyState.Clean -> R.string.cloud_copies_clean
                                                CloudCopyState.Pending -> if (copy.uploadAttempted)
                                                    R.string.cloud_copies_pending_attempted else R.string.cloud_copies_pending
                                                CloudCopyState.Conflict -> R.string.cloud_copies_conflict
                                            }),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = statusColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // Recovery snapshots indicator if any
                                    val snapshots = recoveryCounts[copy.id] ?: 0
                                    if (snapshots > 0) {
                                        Text(
                                            stringResource(R.string.cloud_recovery_snapshots, snapshots),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Conflict or pending controls
                                    if (copy.state == CloudCopyState.Conflict) {
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
                                                OutlinedButton(enabled = !removing && !syncing, onClick = {
                                                    syncing = true
                                                    syncMessage = null
                                                    synchronize.resolveConflict(copy.id, CloudConflictResolution.KeepLocal) { res ->
                                                        handleSyncResult(res)
                                                        if (res.getOrNull() is CloudSyncOutcome.Verified) {
                                                            syncMessage = R.string.cloud_conflict_resolved_local
                                                        }
                                                    }
                                                }) {
                                                    Text(stringResource(R.string.cloud_conflict_keep_local))
                                                }
                                                OutlinedButton(enabled = !removing && !syncing, onClick = {
                                                    syncing = true
                                                    syncMessage = null
                                                    synchronize.resolveConflict(copy.id, CloudConflictResolution.UseRemote) { res ->
                                                        handleSyncResult(res)
                                                        if (res.getOrNull() is CloudSyncOutcome.Downloaded) {
                                                            syncMessage = R.string.cloud_conflict_resolved_remote
                                                        }
                                                    }
                                                }) {
                                                    Text(stringResource(R.string.cloud_conflict_use_remote))
                                                }
                                            }
                                        }
                                    }

                                    // Primary actions: "Abrir" + "Sincronizar" + More options (⋮)
                                    val isUpToDate = copy.state == CloudCopyState.Clean
                                    val syncButtonBg = if (isUpToDate) Color(0xFF1B382B) else MaterialTheme.colorScheme.surfaceVariant
                                    val syncContentColor = if (isUpToDate) Color(0xFF44DB98) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    val syncButtonBorder = if (isUpToDate) Color(0xFF265C43) else MaterialTheme.colorScheme.outlineVariant

                                    val triggerSync = {
                                        if (synchronize != null && !removing && !syncing) {
                                            syncing = true
                                            syncMessage = null
                                            when {
                                                copy.state == CloudCopyState.Pending && copy.uploadAttempted -> {
                                                    synchronize.reconcile(copy.id, handleSyncResult)
                                                }
                                                copy.state == CloudCopyState.Pending -> {
                                                    synchronize(copy.id, handleSyncResult)
                                                }
                                                else -> {
                                                    synchronize.refresh(copy.id, handleSyncResult)
                                                }
                                            }
                                        }
                                    }

                                    val syncButtonText = stringResource(
                                        when {
                                            copy.state == CloudCopyState.Pending && copy.uploadAttempted -> R.string.cloud_copies_reconcile
                                            copy.state == CloudCopyState.Pending -> R.string.cloud_copies_sync_now
                                            else -> R.string.cloud_copies_sync
                                        }
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = { onSelect(copy) },
                                            enabled = !removing && !syncing,
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Lock,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(VaultkSpacing.small))
                                            Text(
                                                text = stringResource(R.string.open_copy_button),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        if (synchronize != null && copy.state != CloudCopyState.Conflict) {
                                            Button(
                                                onClick = triggerSync,
                                                enabled = !removing && !syncing,
                                                modifier = Modifier.height(48.dp),
                                                shape = RoundedCornerShape(24.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = syncButtonBg,
                                                    contentColor = syncContentColor,
                                                    disabledContainerColor = syncButtonBg.copy(alpha = 0.5f),
                                                    disabledContentColor = syncContentColor.copy(alpha = 0.5f)
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, syncButtonBorder)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Cloud,
                                                    contentDescription = null,
                                                    tint = syncContentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(VaultkSpacing.tiny))
                                                Text(
                                                    text = syncButtonText,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = syncContentColor
                                                )
                                            }
                                        }

                                        var optionsMenuOpen by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(
                                                onClick = { optionsMenuOpen = true },
                                                enabled = !removing && !syncing,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = stringResource(R.string.open_recent_actions, copy.name),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = optionsMenuOpen,
                                                onDismissRequest = { optionsMenuOpen = false }
                                            ) {
                                                if (selected?.id == copy.id) {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.cloud_copies_deselect)) },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Outlined.LinkOff,
                                                                contentDescription = null
                                                            )
                                                        },
                                                        onClick = {
                                                            optionsMenuOpen = false
                                                            onDeselect()
                                                            refresh++
                                                        }
                                                    )
                                                }
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            stringResource(R.string.cloud_copies_delete),
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Delete,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    },
                                                    onClick = {
                                                        optionsMenuOpen = false
                                                        if (selected?.id == copy.id) {
                                                            inUseNotice = copy
                                                        } else {
                                                            confirm = copy
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Collapsible Operation Log card
                        item(key = "sync-log-section") {
                            VaultkCard(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier
                                        .clickable { logsExpanded = !logsExpanded }
                                        .padding(VaultkSpacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.cloud_sync_log_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${logs.size} operaciones · Sin secretos",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = if (logsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    AnimatedVisibility(visible = logsExpanded) {
                                        Column(
                                            modifier = Modifier.padding(top = VaultkSpacing.small),
                                            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
                                        ) {
                                            if (logs.isEmpty()) {
                                                Text(
                                                    stringResource(R.string.cloud_sync_log_empty),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            } else {
                                                logs.forEach { entry ->
                                                    val actionLabel = stringResource(
                                                        when (entry.action) {
                                                            CloudSyncAction.PromoteDownload -> R.string.cloud_log_action_promote
                                                            CloudSyncAction.SavePending -> R.string.cloud_log_action_save_pending
                                                            CloudSyncAction.UploadClaimed -> R.string.cloud_log_action_upload_claimed
                                                            CloudSyncAction.UploadVerified -> R.string.cloud_log_action_upload_verified
                                                            CloudSyncAction.UploadRearmed -> R.string.cloud_log_action_upload_rearmed
                                                            CloudSyncAction.BaseAdvanced -> R.string.cloud_log_action_base_advanced
                                                            CloudSyncAction.RemoteDownloaded -> R.string.cloud_log_action_remote_downloaded
                                                            CloudSyncAction.ConflictDetected -> R.string.cloud_log_action_conflict_detected
                                                            CloudSyncAction.ResolvedKeepLocal -> R.string.cloud_log_action_resolved_local
                                                            CloudSyncAction.ResolvedUseRemote -> R.string.cloud_log_action_resolved_remote
                                                        }
                                                    )
                                                    Text(
                                                        stringResource(
                                                            R.string.cloud_sync_log_entry,
                                                            actionLabel,
                                                            "${entry.bytes} B",
                                                            entry.detail
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (removalFailed) Text(
                stringResource(R.string.cloud_copies_delete_error),
                color = MaterialTheme.colorScheme.error
            )
        }

        AnimatedVisibility(
            visible = syncMessage != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VaultkSpacing.small)
        ) {
            if (syncMessage != null) {
                VaultkNotificationCard(
                    message = stringResource(syncMessage!!),
                    isError = syncError,
                    onDismiss = { syncMessage = null }
                )
            }
        }
    }
}

    if (confirm != null) AlertDialog(
        onDismissRequest = { if (!removing) confirm = null },
        title = { Text(stringResource(R.string.cloud_copies_delete_title)) },
        text = { Text(stringResource(R.string.cloud_copies_delete_message, confirm!!.name)) },
        confirmButton = {
            TextButton(enabled = !removing, onClick = {
                val copy = confirm ?: return@TextButton
                removing = true
                removalFailed = false
                scope.launch {
                    val result = runCatching { copies.remove(copy.id) }
                    removalFailed = result.isFailure
                    removing = false
                    confirm = null
                    refresh++
                }
            }) { Text(stringResource(R.string.cloud_copies_delete)) }
        },
        dismissButton = {
            TextButton(enabled = !removing, onClick = { confirm = null }) {
                Text(stringResource(R.string.groups_cancel))
            }
        }
    )

    if (inUseNotice != null) AlertDialog(
        onDismissRequest = { inUseNotice = null },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(stringResource(R.string.cloud_copies_delete_in_use_title)) },
        text = { Text(stringResource(R.string.cloud_copies_delete_in_use_message, inUseNotice!!.name)) },
        confirmButton = {
            TextButton(onClick = {
                val target = inUseNotice!!
                inUseNotice = null
                onDeselect()
                confirm = target
            }) {
                Text(
                    stringResource(R.string.cloud_copies_delete_in_use_action),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { inUseNotice = null }) {
                Text(stringResource(R.string.groups_cancel))
            }
        }
    )
}
