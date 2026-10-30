package com.edro08.vaultk.ui.screen.storage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.edro08.vaultk.R
import com.edro08.vaultk.application.vault.BrowseLocalVaults
import com.edro08.vaultk.domain.vault.LocalVaultItem
import com.edro08.vaultk.domain.vault.VaultId
import com.edro08.vaultk.ui.component.EmptyState
import com.edro08.vaultk.ui.component.VaultkHeader
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.component.VaultkIconTile
import com.edro08.vaultk.ui.theme.VaultkSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException

@Composable
fun LocalVaultPickerScreen(browser: BrowseLocalVaults, onBack: () -> Unit,
    onSelect: (VaultId) -> Unit) {
    var folder by remember(browser) { mutableStateOf(browser.root) }
    var query by remember { mutableStateOf("") }
    var folders by remember { mutableStateOf<List<LocalVaultItem>>(emptyList()) }
    var vaults by remember { mutableStateOf<List<LocalVaultItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var unavailable by remember { mutableStateOf(false) }
    val goBack = {
        if (query.isNotEmpty()) query = ""
        else if (folder != browser.root) folder = browser.parentOf(folder)
        else onBack()
    }
    BackHandler { goBack() }
    LaunchedEffect(browser, folder, query) {
        loading = true
        unavailable = false
        if (query.isNotBlank()) delay(200)
        val result = runCatching { browser.list(folder, query) }
        result.onSuccess { listing -> folders = listing.directories; vaults = listing.vaults }
            .onFailure {
                if (it is CancellationException) throw it
                folders = emptyList(); vaults = emptyList(); unavailable = true
            }
        loading = false
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { VaultkHeader(stringResource(R.string.open_local_title),
            subtitle = if (query.isBlank()) folder.value else null, onBack = goBack) }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = VaultkSpacing.normal),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.open_local_search)) },
                leadingIcon = { Icon(Icons.Default.Search, null) })
            when {
                loading -> CircularProgressIndicator()
                unavailable -> VaultkCard(Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = stringResource(R.string.open_local_unavailable),
                        description = "",
                        icon = Icons.Outlined.ErrorOutline
                    )
                }
                folders.isEmpty() && vaults.isEmpty() -> VaultkCard(Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = stringResource(R.string.open_local_empty),
                        description = "",
                        icon = Icons.Default.Folder
                    )
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                    items(folders, key = { it.id.value }) { directory ->
                        LocalFileRow(directory, false, { folder = directory.id })
                    }
                    items(vaults, key = { it.id.value }) { vault ->
                        LocalFileRow(vault, query.isNotBlank(), { onSelect(vault.id) }, true)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalFileRow(file: LocalVaultItem, showPath: Boolean, onClick: () -> Unit, vault: Boolean = false) {
    VaultkCard(Modifier.clickable(onClick = onClick).testTag("local-${file.name}")) {
        Row(Modifier.fillMaxWidth().padding(VaultkSpacing.medium), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
            VaultkIconTile(if (vault) Icons.Default.Lock else Icons.Default.Folder, null)
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.titleMedium)
                if (showPath) Text(file.parent?.value.orEmpty(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.secondary)
        }
    }
}
