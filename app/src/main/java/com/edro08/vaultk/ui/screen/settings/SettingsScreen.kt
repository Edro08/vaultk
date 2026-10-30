package com.edro08.vaultk.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edro08.vaultk.R
import com.edro08.vaultk.domain.Appearance
import com.edro08.vaultk.domain.SecurityOptions
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.theme.VaultkSpacing

@Composable
fun SettingsScreen(
    appearance: Appearance,
    onAppearanceChange: (Appearance) -> Unit, biometricEnabled: Boolean,
    onBiometricClick: (() -> Unit)?, timeoutSeconds: Int, onTimeoutClick: () -> Unit,
    clipboardSeconds: Int, onClipboardChange: (Int) -> Unit,
    searchNotes: Boolean, onSearchNotesChange: (Boolean) -> Unit,
    showDetails: Boolean, onShowDetailsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    fileAccessGranted: Boolean = false,
    onManageFileAccess: () -> Unit = {},
    cloudAutoSync: Boolean = true, onCloudAutoSyncChange: (Boolean) -> Unit = {},
    cloudAllowMetered: Boolean = true, onCloudAllowMeteredChange: (Boolean) -> Unit = {}
) {
    var clipboardDialog by remember { mutableStateOf(false) }
    val systemDarkTheme = isSystemInDarkTheme()
    LazyColumn(
        modifier
            .fillMaxSize()
            .testTag("settings-list"), contentPadding = PaddingValues(
            start = VaultkSpacing.normal, end = VaultkSpacing.normal,
            top = VaultkSpacing.small, bottom = VaultkSpacing.normal
        ),
        verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
    ) {
        item {
            VaultkCard {
                Column(
                    Modifier.padding(
                        horizontal = VaultkSpacing.normal,
                        vertical = VaultkSpacing.small
                    ),
                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                    ) {
                        Icon(
                            Icons.Default.Palette, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                stringResource(R.string.settings_theme),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.settings_theme_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectableGroup()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.large
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.large
                            )
                            .padding(VaultkSpacing.tiny),
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
                    ) {
                        ThemeSegment(
                            stringResource(R.string.settings_dark), Icons.Default.DarkMode,
                            appearance == Appearance.Dark || appearance == Appearance.System && systemDarkTheme,
                            modifier = Modifier.weight(1f)
                        ) { onAppearanceChange(Appearance.Dark) }
                        ThemeSegment(
                            stringResource(R.string.settings_light), Icons.Default.LightMode,
                            appearance == Appearance.Light || appearance == Appearance.System && !systemDarkTheme,
                            modifier = Modifier.weight(1f)
                        ) { onAppearanceChange(Appearance.Light) }
                    }
                }
            }
        }
        item {
            VaultkCard {
                Column(Modifier.padding(vertical = VaultkSpacing.small)) {
                    Row(
                        Modifier.padding(
                            horizontal = VaultkSpacing.normal,
                            vertical = VaultkSpacing.small
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                    ) {
                        Icon(
                            Icons.Default.Lock, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.settings_security),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (onBiometricClick != null) {
                        SecuritySettingRow(
                            stringResource(R.string.settings_biometrics),
                            null, stringResource(
                                if (biometricEnabled) R.string.settings_enabled
                                else R.string.settings_disabled
                            ), onBiometricClick
                        )
                        HorizontalDivider(Modifier.padding(horizontal = VaultkSpacing.normal))
                    }
                    SecuritySettingRow(
                        stringResource(R.string.content_lock_timeout),
                        lockTimeoutDescription(timeoutSeconds),
                        lockTimeoutLabel(timeoutSeconds), onTimeoutClick
                    )
                    HorizontalDivider(Modifier.padding(horizontal = VaultkSpacing.normal))
                    SecuritySettingRow(
                        stringResource(R.string.settings_clipboard),
                        stringResource(R.string.settings_clipboard_description),
                        stringResource(R.string.settings_clipboard_seconds, clipboardSeconds),
                        { clipboardDialog = true })
                }
            }
        }
        item {
            VaultkCard {
                Column(
                    Modifier.padding(
                        horizontal = VaultkSpacing.normal,
                        vertical = VaultkSpacing.small
                    ),
                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.settings_file_access),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        stringResource(R.string.settings_access_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.settings_access_full_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(
                                if (fileAccessGranted) R.string.settings_access_active
                                else R.string.settings_access_inactive
                            ),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onManageFileAccess) {
                            Text(stringResource(R.string.settings_manage_permission))
                        }
                    }
                }
            }
        }
        item {
            VaultkCard {
                Column(Modifier.padding(vertical = VaultkSpacing.small)) {
                    Row(
                        Modifier.padding(
                            horizontal = VaultkSpacing.normal,
                            vertical = VaultkSpacing.small
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                    ) {
                        Icon(
                            Icons.Default.Cloud, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.settings_cloud_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    SettingsSwitch(
                        stringResource(R.string.settings_cloud_auto_sync),
                        stringResource(R.string.settings_cloud_auto_sync_description),
                        cloudAutoSync,
                        onCloudAutoSyncChange
                    )
                    HorizontalDivider(Modifier.padding(horizontal = VaultkSpacing.normal))
                    SettingsSwitch(
                        stringResource(R.string.settings_cloud_metered),
                        stringResource(R.string.settings_cloud_metered_description),
                        cloudAllowMetered,
                        onCloudAllowMeteredChange
                    )
                }
            }
        }
        item {
            VaultkCard {
                Column(Modifier.padding(vertical = VaultkSpacing.small)) {
                    Row(
                        Modifier.padding(
                            horizontal = VaultkSpacing.normal,
                            vertical = VaultkSpacing.small
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                    ) {
                        Icon(
                            Icons.Default.Settings, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.settings_general),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    SettingsSwitch(
                        stringResource(R.string.settings_search_notes),
                        stringResource(R.string.settings_search_notes_description),
                        searchNotes,
                        onSearchNotesChange
                    )
                    HorizontalDivider(Modifier.padding(horizontal = VaultkSpacing.normal))
                    SettingsSwitch(
                        stringResource(R.string.settings_show_details),
                        stringResource(R.string.settings_show_details_description),
                        showDetails,
                        onShowDetailsChange
                    )
                }
            }
        }
    }
    if (clipboardDialog) TimeChoiceDialog(
        stringResource(R.string.settings_clipboard),
        stringResource(R.string.settings_clipboard_dialog_description),
        SecurityOptions.clipboardTimeoutSeconds, clipboardSeconds,
        label = { stringResource(R.string.settings_clipboard_seconds, it) },
        onSelect = { onClipboardChange(it); clipboardDialog = false },
        onDismiss = { clipboardDialog = false })
}

@Composable
private fun ThemeSegment(
    label: String, icon: ImageVector, selected: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier
            .heightIn(min = 48.dp)
            .background(container, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag("theme-$label"), contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = foreground
            )
            Text(label, color = foreground, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SecuritySettingRow(
    title: String,
    description: String?,
    value: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag("setting-$title")
            .padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.small),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) Text(
                description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FileAccessOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = VaultkSpacing.small),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LockTimeoutDialog(timeoutSeconds: Int, onTimeoutChange: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.content_lock_timeout)) }, text = {
            Column(Modifier
                .verticalScroll(rememberScrollState())
                .selectableGroup()) {
                Text(
                    stringResource(R.string.content_lock_timeout_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SecurityOptions.lockTimeoutSeconds.forEach { seconds ->
                    LockTimeoutOption(
                        seconds, lockTimeoutLabel(seconds), lockTimeoutDescription(seconds),
                        timeoutSeconds == seconds
                    ) { onTimeoutChange(seconds); onDismiss() }
                }
            }
        }, confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.groups_cancel)) }
        })
}

@Composable
private fun lockTimeoutLabel(seconds: Int): String = when {
    seconds == 0 -> stringResource(R.string.settings_lock_immediately)
    seconds < 60 -> stringResource(R.string.content_lock_timeout_seconds, seconds)
    else -> stringResource(R.string.content_lock_timeout_minutes, seconds / 60)
}

@Composable
private fun lockTimeoutDescription(seconds: Int): String = when (seconds) {
    0 -> stringResource(R.string.settings_lock_immediately_description)
    30 -> stringResource(R.string.settings_lock_seconds_description, seconds)
    60 -> stringResource(R.string.settings_lock_one_minute_description)
    else -> stringResource(R.string.settings_lock_minutes_description, seconds / 60)
}

@Composable
private fun LockTimeoutOption(
    seconds: Int, label: String, description: String, selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag("lock-timeout-$seconds")
            .padding(vertical = VaultkSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimeChoiceDialog(
    title: String, description: String, options: List<Int>, selected: Int,
    label: @Composable (Int) -> String, onSelect: (Int) -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                Text(
                    description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                options.forEach { seconds ->
                    SettingsOption(label(seconds), selected == seconds) { onSelect(seconds) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.groups_cancel)) } })
}

@Composable
private fun SettingsSwitch(
    title: String,
    summary: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("setting-switch-row-$title")
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                summary, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.testTag("setting-switch-$title")
        )
    }
}

@Composable
private fun SettingsOption(
    label: String, selected: Boolean, modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag("time-choice-$label")
            .padding(VaultkSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label)
    }
}
