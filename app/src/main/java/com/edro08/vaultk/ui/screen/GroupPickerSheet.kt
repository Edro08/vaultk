package com.edro08.vaultk.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.edro08.vaultk.R
import com.edro08.vaultk.domain.VaultGroup
import com.edro08.vaultk.domain.groupsAndSelf
import com.edro08.vaultk.ui.component.EmptyState
import com.edro08.vaultk.ui.component.VaultkBottomSheet
import com.edro08.vaultk.ui.theme.VaultkSpacing

private data class GroupLocation(val group: VaultGroup, val parentId: String?, val path: String)

private fun VaultGroup.locations(parentId: String? = null, path: String = ""): List<GroupLocation> {
    val fullPath = if (path.isEmpty()) name else "$path / $name"
    return listOf(GroupLocation(this, parentId, fullPath)) +
        groups.flatMap { it.locations(id, fullPath) }
}

@Composable
fun GroupPickerSheet(
    root: VaultGroup,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onCreate: (parentId: String, name: String) -> Unit,
    selectionMode: Boolean = false,
    selectable: (VaultGroup) -> Boolean = { true },
) {
    val locations = remember(root) { root.locations() }
    var browseId by remember { mutableStateOf(selectedId) }
    var query by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val browse = locations.firstOrNull { it.group.id == browseId } ?: locations.first()
    val filtered = if (query.isBlank()) emptyList() else
        locations.filter { it.group.name.contains(query.trim(), ignoreCase = true) }
    val maxSheetContentHeight = LocalConfiguration.current.screenHeightDp.dp * 0.75f

    VaultkBottomSheet(onDismiss = onDismiss, skipPartiallyExpanded = true) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxSheetContentHeight)
            .padding(horizontal = VaultkSpacing.normal).padding(bottom = VaultkSpacing.large),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (selectionMode) R.string.detail_move_title else R.string.groups_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.common_close))
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.groups_search)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (query.isBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (browse.parentId != null) IconButton(onClick = { browseId = browse.parentId }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.groups_parent))
                    }
                    Text(browse.group.name.ifBlank { stringResource(R.string.content_root) },
                        style = MaterialTheme.typography.titleMedium)
                }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)) {
                if (query.isBlank()) {
                    item(key = "current") {
                        GroupRow(browse.group, browse.group.id == selectedId,
                            subtitle = stringResource(R.string.groups_current),
                            onSelect = { onSelect(browse.group.id) }, enabled = selectable(browse.group))
                    }
                    items(browse.group.groups, key = { it.id }) { child ->
                        GroupRow(child, child.id == selectedId,
                            onSelect = { onSelect(child.id) },
                            onEnter = if (child.groups.isNotEmpty()) { { browseId = child.id } } else null,
                            enabled = selectable(child))
                    }
                    if (browse.group.groups.isEmpty()) item {
                        Text(stringResource(R.string.groups_no_children),
                            modifier = Modifier.padding(VaultkSpacing.medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    if (filtered.isEmpty()) item {
                        EmptyState(stringResource(R.string.groups_no_results),
                            stringResource(R.string.groups_no_results_description))
                    }
                    items(filtered, key = { it.group.id }) { option ->
                        GroupRow(option.group, option.group.id == selectedId,
                            subtitle = option.path,
                            onSelect = { onSelect(option.group.id) }, enabled = selectable(option.group))
                    }
                }
            }
            if (query.isBlank() && !selectionMode) {
                Button(onClick = { newName = ""; showCreate = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Text(stringResource(R.string.groups_new), modifier = Modifier.padding(start = VaultkSpacing.small))
                }
            }
            Text(stringResource(when {
                    selectionMode -> R.string.detail_move_hint
                    root.groupsAndSelf().any { it.id.startsWith("draft:") } -> R.string.groups_session_notice
                    else -> R.string.groups_save_hint
                }),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
    }
    if (showCreate) {
        val trimmed = newName.trim()
        val valid = trimmed.isNotEmpty() && browse.group.groups.none { it.name.equals(trimmed, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.groups_new)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
                    Text(stringResource(R.string.groups_create_warning))
                    OutlinedTextField(value = newName, onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.groups_name)) }, singleLine = true)
                    if (trimmed.isNotEmpty() && !valid) {
                        Text(stringResource(R.string.groups_duplicate), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = { showCreate = false; onCreate(browse.group.id, trimmed) }) {
                    Text(stringResource(R.string.groups_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text(stringResource(R.string.groups_cancel)) }
            },
        )
    }
}

@Composable
private fun GroupRow(
    group: VaultGroup,
    selected: Boolean,
    subtitle: String? = null,
    onSelect: () -> Unit,
    onEnter: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val label = group.name.ifBlank { stringResource(R.string.content_root) }
    Row(
        Modifier.fillMaxWidth().testTag("group-select-${group.id}")
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClickLabel = stringResource(R.string.groups_choose, label), onClick = onSelect)
            .padding(horizontal = VaultkSpacing.small, vertical = VaultkSpacing.medium),
        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Folder, null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (group.id.startsWith("draft:")) Text(stringResource(R.string.groups_draft),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
        if (onEnter != null) IconButton(onClick = onEnter, modifier = Modifier.testTag("group-enter-${group.id}")) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                stringResource(R.string.groups_enter, label))
        }
    }
}
