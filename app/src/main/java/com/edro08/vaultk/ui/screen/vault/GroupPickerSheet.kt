package com.edro08.vaultk.ui.screen.vault

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.edro08.vaultk.R
import com.edro08.vaultk.domain.vault.VaultGroup
import com.edro08.vaultk.domain.vault.groupsAndSelf
import com.edro08.vaultk.ui.component.EmptyState
import com.edro08.vaultk.ui.component.VaultkBottomSheet
import com.edro08.vaultk.ui.theme.VaultkSpacing

private data class GroupLocation(val group: VaultGroup, val parentId: String?, val path: String)

private fun VaultGroup.locations(parentId: String? = null, path: String = ""): List<GroupLocation> {
    val fullPath = if (path.isEmpty()) name else "$path / $name"
    return listOf(GroupLocation(this, parentId, fullPath)) +
        groups.flatMap { it.locations(id, fullPath) }
}

fun findGroupPath(root: VaultGroup, targetId: String): List<VaultGroup>? {
    if (root.id == targetId) return listOf(root)
    for (child in root.groups) {
        val path = findGroupPath(child, targetId)
        if (path != null) return listOf(root) + path
    }
    return null
}

@Composable
fun GroupPickerSheet(
    root: VaultGroup,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onCreate: (parentId: String, name: String) -> Unit,
    onRename: (groupId: String, newName: String) -> Unit = { _, _ -> },
    selectionMode: Boolean = false,
    selectable: (VaultGroup) -> Boolean = { true },
) {
    val locations = remember(root) { root.locations() }
    var browseId by remember(selectedId) { mutableStateOf(selectedId) }
    var query by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var targetParentGroup by remember { mutableStateOf<VaultGroup?>(null) }
    var newName by remember { mutableStateOf("") }
    var renamingGroup by remember { mutableStateOf<VaultGroup?>(null) }
    var renameText by remember { mutableStateOf("") }

    val currentPath = remember(root, browseId) {
        findGroupPath(root, browseId) ?: listOf(root)
    }
    val browse = locations.firstOrNull { it.group.id == browseId } ?: locations.first()
    val filtered = if (query.isBlank()) emptyList() else
        locations.filter { it.group.name.contains(query.trim(), ignoreCase = true) }
    val maxSheetContentHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f

    VaultkBottomSheet(onDismiss = onDismiss, skipPartiallyExpanded = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetContentHeight)
                .padding(horizontal = VaultkSpacing.normal)
                .padding(bottom = VaultkSpacing.large),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
        ) {
            // 1. Top bar: ← Back, [📁🔍] Explorar grupos, X Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPath.size > 1) {
                    IconButton(
                        onClick = {
                            browseId = currentPath[currentPath.size - 2].id
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.groups_parent),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(11.dp).padding(start = 2.dp, top = 2.dp)
                        )
                    }
                    Text(
                        text = stringResource(if (selectionMode) R.string.detail_move_title else R.string.groups_explore_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Breadcrumbs path: Raíz › Cuentas-Y with metadata summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentPath.forEachIndexed { index, group ->
                        val isLast = index == currentPath.lastIndex
                        val groupLabel = group.name.ifBlank { stringResource(R.string.content_root) }
                        if (isLast) {
                            Text(
                                text = groupLabel,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Text(
                                text = groupLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.clickable { browseId = group.id }
                            )
                            Text(
                                text = " › ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                val currentSubgroupCountText = when (browse.group.groups.size) {
                    0 -> stringResource(R.string.groups_no_subgroups_count)
                    1 -> stringResource(R.string.groups_one_subgroup_count)
                    else -> stringResource(R.string.groups_many_subgroups_count, browse.group.groups.size)
                }
                val currentEntryCountText = when (browse.group.entries.size) {
                    0 -> stringResource(R.string.groups_empty_entries_count)
                    1 -> stringResource(R.string.groups_one_entry_count)
                    else -> stringResource(R.string.groups_many_entries_count, browse.group.entries.size)
                }
                Text(
                    text = "$currentSubgroupCountText · $currentEntryCountText",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 3. Search input: [🔍] Buscar grupo
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        stringResource(R.string.groups_search),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Section header: "Subgrupos" and "Nuevo grupo" button (when query is blank)
            if (query.isBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val subgroupsTitle = if (browse.group.groups.isNotEmpty()) {
                        "${stringResource(R.string.groups_subgroups_title)} (${browse.group.groups.size})"
                    } else {
                        stringResource(R.string.groups_subgroups_title)
                    }
                    Text(
                        text = subgroupsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!selectionMode) {
                        Surface(
                            onClick = { targetParentGroup = null; newName = ""; showCreate = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(10.dp).padding(start = 2.dp, top = 2.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.groups_new),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 5. List: Subgroups or Search results
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (query.isBlank()) {
                    if (browse.group.groups.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = VaultkSpacing.large),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.groups_no_children),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(browse.group.groups, key = { it.id }) { child ->
                            val hasChildren = child.groups.isNotEmpty()
                            val subgroupText = when (child.groups.size) {
                                0 -> stringResource(R.string.groups_no_subgroups_count)
                                1 -> stringResource(R.string.groups_one_subgroup_count)
                                else -> stringResource(R.string.groups_many_subgroups_count, child.groups.size)
                            }
                            val entryText = when (child.entries.size) {
                                0 -> stringResource(R.string.groups_empty_entries_count)
                                1 -> stringResource(R.string.groups_one_entry_count)
                                else -> stringResource(R.string.groups_many_entries_count, child.entries.size)
                            }
                            val metadataText = "$subgroupText · $entryText"

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(if (hasChildren) "group-enter-${child.id}" else "group-select-${child.id}")
                                    .clickable(enabled = selectable(child)) {
                                        if (hasChildren) {
                                            browseId = child.id
                                        } else {
                                            onSelect(child.id)
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = child.name.ifBlank { stringResource(R.string.content_root) },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = metadataText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!selectionMode) {
                                        Box {
                                            var menuExpanded by remember { mutableStateOf(false) }
                                            IconButton(
                                                onClick = { menuExpanded = true },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("group-more-${child.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = stringResource(R.string.content_more),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = menuExpanded,
                                                onDismissRequest = { menuExpanded = false },
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = stringResource(R.string.groups_action_create_subgroup),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                Icons.Outlined.Folder,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            Icon(
                                                                Icons.Default.Add,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.size(10.dp).padding(start = 2.dp, top = 2.dp)
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        menuExpanded = false
                                                        targetParentGroup = child
                                                        newName = ""
                                                        showCreate = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = stringResource(R.string.groups_action_rename),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    },
                                                    onClick = {
                                                        menuExpanded = false
                                                        renamingGroup = child
                                                        renameText = child.name
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (filtered.isEmpty()) {
                        item {
                            EmptyState(
                                stringResource(R.string.groups_no_results),
                                stringResource(R.string.groups_no_results_description)
                            )
                        }
                    } else {
                        items(filtered, key = { it.group.id }) { option ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group-select-${option.group.id}")
                                    .clickable(enabled = selectable(option.group)) {
                                        onSelect(option.group.id)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.group.name.ifBlank { stringResource(R.string.content_root) },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = option.path,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Bottom action: "Usar grupo [Nombre]"
            val currentGroupName = browse.group.name.ifBlank { stringResource(R.string.content_root) }
            Button(
                onClick = { onSelect(browse.group.id) },
                enabled = selectable(browse.group),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("group-use-current")
                    .testTag("group-select-${browse.group.id}")
            ) {
                Text(
                    text = stringResource(R.string.groups_use_group, currentGroupName),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val noticeText = when {
                selectionMode -> stringResource(R.string.detail_move_hint)
                root.groupsAndSelf().any { it.id.startsWith("draft:") } -> stringResource(R.string.groups_session_notice)
                else -> null
            }
            if (noticeText != null) {
                Text(
                    text = noticeText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = VaultkSpacing.tiny)
                )
            }
        }
    }

    if (showCreate) {
        val effectiveParent = targetParentGroup ?: browse.group
        val trimmed = newName.trim()
        val valid = trimmed.isNotEmpty() && effectiveParent.groups.none { it.name.equals(trimmed, ignoreCase = true) }
        val targetPath = remember(root, effectiveParent.id) {
            findGroupPath(root, effectiveParent.id) ?: listOf(effectiveParent)
        }
        val rootLabel = stringResource(R.string.content_root)
        val parentPath = targetPath.joinToString(" › ") {
            it.name.ifBlank { rootLabel }
        }

        Dialog(onDismissRequest = { showCreate = false; targetParentGroup = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.groups_new),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.groups_create_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.groups_name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text(stringResource(R.string.groups_placeholder_example)) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (trimmed.isNotEmpty() && !valid) {
                            Text(
                                text = stringResource(R.string.groups_duplicate),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.groups_create_parent_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = parentPath,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreate = false; targetParentGroup = null }) {
                            Text(
                                text = stringResource(R.string.groups_cancel),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                        Button(
                            onClick = {
                                showCreate = false
                                onCreate(effectiveParent.id, trimmed)
                                targetParentGroup = null
                            },
                            enabled = valid,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.groups_create_action),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    renamingGroup?.let { groupToRename ->
        val trimmedRename = renameText.trim()
        val parent = locations.firstOrNull { it.group.id == groupToRename.id }?.parentId?.let { pId ->
            locations.firstOrNull { it.group.id == pId }?.group
        } ?: root
        val validRename = trimmedRename.isNotEmpty() &&
            (trimmedRename.equals(groupToRename.name, ignoreCase = true) ||
             parent.groups.none { it.id != groupToRename.id && it.name.equals(trimmedRename, ignoreCase = true) })

        Dialog(onDismissRequest = { renamingGroup = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.groups_rename_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.groups_rename_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.groups_name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            placeholder = { Text(stringResource(R.string.groups_placeholder_example)) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (trimmedRename.isNotEmpty() && !validRename) {
                            Text(
                                text = stringResource(R.string.groups_duplicate),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { renamingGroup = null }) {
                            Text(
                                text = stringResource(R.string.groups_cancel),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                        Button(
                            onClick = {
                                onRename(groupToRename.id, trimmedRename)
                                renamingGroup = null
                            },
                            enabled = validRename && trimmedRename != groupToRename.name,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.groups_rename_save),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
