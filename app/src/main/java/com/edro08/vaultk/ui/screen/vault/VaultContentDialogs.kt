package com.edro08.vaultk.ui.screen.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edro08.vaultk.R
import com.edro08.vaultk.domain.cloud.CloudCopy
import com.edro08.vaultk.domain.cloud.CloudCopyState
import com.edro08.vaultk.domain.settings.FileAccessMode
import com.edro08.vaultk.ui.component.PasswordField
import com.edro08.vaultk.ui.theme.VaultkSpacing
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CloseMenu(
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
            DropdownMenuItem(
                text = { Text(stringResource(R.string.content_lock)) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                onClick = { onExpandedChange(false); onLock() }
            )
            if (onChangeVault != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.unlock_change_vault)) },
                    leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                    onClick = { onExpandedChange(false); onChangeVault() }
                )
            }
            if (onSave != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.content_save_action)) },
                    leadingIcon = { Icon(Icons.Outlined.Save, contentDescription = null) },
                    onClick = { onExpandedChange(false); onSave() }
                )
            }

            val hasCloud = (cloudSyncLabel != null && onCloudSync != null) || onCloudViewStatus != null || onCloudUnlink != null
            if (hasCloud) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                if (cloudSyncLabel != null && onCloudSync != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(cloudSyncLabel)) },
                        leadingIcon = { Icon(Icons.Outlined.CloudSync, contentDescription = null) },
                        onClick = { onExpandedChange(false); onCloudSync() }
                    )
                }
                if (onCloudViewStatus != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cloud_view_status)) },
                        leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                        onClick = { onExpandedChange(false); onCloudViewStatus() }
                    )
                }
                if (onCloudUnlink != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cloud_unlink_copy)) },
                        leadingIcon = { Icon(Icons.Outlined.LinkOff, contentDescription = null) },
                        onClick = { onExpandedChange(false); onCloudUnlink() }
                    )
                }
            }

            val hasBiometric = (biometricEnabled && onDisableBiometric != null) || (!biometricEnabled && onEnableBiometric != null)
            if (hasBiometric) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                if (biometricEnabled && onDisableBiometric != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.content_biometric_disable)) },
                        leadingIcon = { Icon(Icons.Outlined.Fingerprint, contentDescription = null) },
                        onClick = { onExpandedChange(false); onDisableBiometric() }
                    )
                } else if (!biometricEnabled && onEnableBiometric != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.content_biometric_enable)) },
                        leadingIcon = { Icon(Icons.Outlined.Fingerprint, contentDescription = null) },
                        onClick = { onExpandedChange(false); onEnableBiometric() }
                    )
                }
            }

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

@Composable
internal fun DeleteEntryDialog(
    inRecycleBin: Boolean,
    hasRecycleBin: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_delete_title)) },
        text = {
            Text(stringResource(if (inRecycleBin || !hasRecycleBin)
                R.string.detail_delete_permanent_message else R.string.detail_delete_trash_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.detail_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.groups_cancel))
            }
        }
    )
}

@Composable
internal fun DiscardChangesDialog(
    hasDraftEntries: Boolean,
    hasEdits: Boolean,
    lockRequested: Boolean,
    changeVaultRequested: Boolean,
    accessRequested: Boolean,
    manageAccessRequested: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (hasDraftEntries || hasEdits)
                R.string.content_discard_title else R.string.groups_discard_title))
        },
        text = {
            Text(stringResource(when {
                lockRequested -> R.string.content_lock_discard_message
                changeVaultRequested -> R.string.content_change_vault_discard_message
                accessRequested || manageAccessRequested -> R.string.settings_access_discard_message
                hasDraftEntries || hasEdits -> R.string.content_discard_message
                else -> R.string.groups_discard_message
            }))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.groups_discard_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.groups_discard_cancel))
            }
        }
    )
}

@Composable
internal fun CloudUnlinkDialog(
    copyName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cloud_unlink_title)) },
        text = { Text(stringResource(R.string.cloud_unlink_message, copyName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.cloud_unlink_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.groups_cancel))
            }
        }
    )
}

@Composable
internal fun BiometricPasswordDialog(
    password: String,
    onPasswordChange: (String) -> Unit,
    configuring: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!configuring) onDismiss() },
        title = { Text(stringResource(R.string.content_biometric_enable)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
                Text(stringResource(R.string.content_biometric_description))
                PasswordField(
                    password,
                    onPasswordChange,
                    label = stringResource(R.string.unlock_master_key_hint)
                )
                if (configuring) CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(
                enabled = password.isNotEmpty() && !configuring,
                modifier = Modifier.testTag("biometric-enable-confirm"),
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.content_biometric_enable))
            }
        },
        dismissButton = {
            TextButton(enabled = !configuring, onClick = onDismiss) {
                Text(stringResource(R.string.groups_cancel))
            }
        }
    )
}

@Composable
internal fun CloudStatusDialog(
    copy: CloudCopy?,
    fallbackName: String,
    editable: Boolean,
    inConflict: Boolean,
    syncing: Boolean,
    syncFailed: Boolean,
    isWarningStatus: Boolean,
    cloudSyncActionLabel: Int?,
    triggerCloudSyncAction: (() -> Unit)?,
    saving: Boolean,
    onDismiss: () -> Unit
) {
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
        onDismissRequest = onDismiss,
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
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(stringResource(R.string.groups_cancel))
                }
                if (triggerCloudSyncAction != null && cloudSyncActionLabel != null) {
                    Button(
                        onClick = {
                            onDismiss()
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
