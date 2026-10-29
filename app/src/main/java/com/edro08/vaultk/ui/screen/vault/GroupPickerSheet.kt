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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

private fun findGroupPath(root: VaultGroup, targetId: String): List<VaultGroup>? {
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
    selectionMode: Boolean = false,
    selectable: (VaultGroup) -> Boolean = { true },
) {
    val locations = remember(root) { root.locations() }
    var browseId by remember(selectedId) { mutableStateOf(selectedId) }
    var query by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

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
                IconButton(
                    onClick = {
                        if (currentPath.size > 1) {
                            browseId = currentPath[currentPath.size - 2].id
                        }
                    },
                    enabled = currentPath.size > 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.groups_parent),
                        tint = if (currentPath.size > 1) Color(0xFFBD9BDB) else Color.Transparent
                    )
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
                            tint = Color(0xFFBD9BDB),
                            modifier = Modifier.size(24.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFFBD9BDB),
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

            // 2. Breadcrumbs path: Raíz › Cuentas-Y
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
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
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = groupLabel,
                            color = Color(0xFFBD9BDB),
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { browseId = group.id }
                        )
                        Text(
                            text = " › ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
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
                    focusedContainerColor = Color(0xFF16151F),
                    unfocusedContainerColor = Color(0xFF16151F),
                    focusedBorderColor = Color(0xFF3E3654),
                    unfocusedBorderColor = Color(0xFF262335),
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
                    Text(
                        text = stringResource(R.string.groups_subgroups_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!selectionMode) {
                        Surface(
                            onClick = { newName = ""; showCreate = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF282338),
                            border = BorderStroke(1.dp, Color(0xFF3E3654))
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
                                        tint = Color(0xFFBD9BDB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFFBD9BDB),
                                        modifier = Modifier.size(10.dp).padding(start = 2.dp, top = 2.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.groups_new),
                                    color = Color(0xFFBD9BDB),
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
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group-enter-${child.id}")
                                    .clickable(enabled = selectable(child)) {
                                        browseId = child.id
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF16151F),
                                border = BorderStroke(1.dp, Color(0xFF262335))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = Color(0xFFBD9BDB),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = child.name.ifBlank { stringResource(R.string.content_root) },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                color = Color(0xFF16151F),
                                border = BorderStroke(1.dp, Color(0xFF262335))
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
                                        tint = Color(0xFFBD9BDB),
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
                    containerColor = Color(0xFFBD9BDB),
                    contentColor = Color(0xFF1D122C),
                    disabledContainerColor = Color(0xFFBD9BDB).copy(alpha = 0.38f),
                    disabledContentColor = Color(0xFF1D122C).copy(alpha = 0.38f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("group-use-current")
                    .testTag("group-select-${browse.group.id}")
            ) {
                Text(
                    text = stringResource(R.string.groups_use_group, currentGroupName),
                    style = MaterialTheme.typography.titleMedium,
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
        val trimmed = newName.trim()
        val valid = trimmed.isNotEmpty() && browse.group.groups.none { it.name.equals(trimmed, ignoreCase = true) }
        Dialog(onDismissRequest = { showCreate = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF171622),
                border = BorderStroke(1.dp, Color(0xFF2E2B3E)),
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
                            color = Color.White
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
                                focusedContainerColor = Color(0xFF13121C),
                                unfocusedContainerColor = Color(0xFF13121C),
                                focusedBorderColor = Color(0xFF3E3654),
                                unfocusedBorderColor = Color(0xFF262335),
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
                        color = Color(0xFF1E1D2D),
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
                                    tint = Color(0xFFBD9BDB),
                                    modifier = Modifier.size(18.dp)
                                )
                                val rootLabel = stringResource(R.string.content_root)
                                val parentPath = currentPath.joinToString(" › ") {
                                    it.name.ifBlank { rootLabel }
                                }
                                Text(
                                    text = parentPath,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreate = false }) {
                            Text(
                                text = stringResource(R.string.groups_cancel),
                                color = Color(0xFFBD9BDB),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                        Button(
                            onClick = {
                                showCreate = false
                                onCreate(browse.group.id, trimmed)
                            },
                            enabled = valid,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF282338),
                                contentColor = Color(0xFFBD9BDB),
                                disabledContainerColor = Color(0xFF282338).copy(alpha = 0.5f),
                                disabledContentColor = Color(0xFFBD9BDB).copy(alpha = 0.38f)
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
}
