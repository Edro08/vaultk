package com.edro08.vaultk.ui.screen

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.edro08.vaultk.domain.VaultDocument
import com.edro08.vaultk.ui.uri
import com.edro08.vaultk.domain.FileAccessMode
import com.edro08.vaultk.R
import com.edro08.vaultk.ui.component.EmptyState
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.component.VaultkHeader
import com.edro08.vaultk.ui.component.VaultkIconTile
import com.edro08.vaultk.ui.theme.VaultkSpacing
import com.edro08.vaultk.ui.theme.VaultkTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun OpenVaultScreen(
    recent: List<VaultDocument>,
    onBack: () -> Unit,
    onDevice: () -> Unit,
    onRecent: (VaultDocument) -> Unit,
    errorMessage: String? = null,
    onErrorShown: () -> Unit = {},
    fileAccessMode: FileAccessMode = FileAccessMode.Standard,
    currentUri: Uri? = null,
    unavailableRecent: VaultDocument? = null,
    onUnavailableDismiss: () -> Unit = {},
    onRelocateRecent: ((VaultDocument) -> Unit)? = null,
    onRemoveRecent: ((VaultDocument) -> Unit)? = null,
) {
    val snackbar = remember { SnackbarHostState() }
    var removeConfirmation by remember { mutableStateOf<VaultDocument?>(null) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbar.showSnackbar(errorMessage)
            onErrorShown()
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { VaultkHeader(stringResource(R.string.open_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.large),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.normal),
        ) {
            Text(stringResource(R.string.open_intro), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.open_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            SourceCard(stringResource(R.string.common_device),
                stringResource(R.string.open_device_full_description),
                Icons.Default.PhoneAndroid, onClick = onDevice)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = VaultkSpacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small),
            ) {
                Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.common_recent), style = MaterialTheme.typography.titleMedium)
            }
            if (recent.isEmpty()) {
                VaultkCard { EmptyState(stringResource(R.string.open_no_recent), stringResource(R.string.open_no_recent_description)) }
            } else {
                VaultkCard {
                    recent.forEachIndexed { index, document ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onRecent(document) }
                                .padding(VaultkSpacing.normal),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
                        ) {
                            VaultkIconTile(Icons.Default.Lock, null)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(document.name, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(if (document.uri.scheme == "file")
                                    R.string.open_recent_local else R.string.open_recent_document),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(document.openedAt?.let { time ->
                                    stringResource(R.string.open_last_opened,
                                        DateFormat.getDateTimeInstance().format(Date(time)))
                                } ?: stringResource(R.string.open_never_opened),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (document.uri == currentUri) Text(stringResource(R.string.open_current),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            if (onRelocateRecent != null || onRemoveRecent != null) {
                                var menuOpen by remember(document.uri) { mutableStateOf(false) }
                                androidx.compose.foundation.layout.Box {
                                    IconButton(onClick = { menuOpen = true }) {
                                        Icon(Icons.Default.MoreVert,
                                            stringResource(R.string.open_recent_actions, document.name))
                                    }
                                    DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                                        if (onRelocateRecent != null) DropdownMenuItem(
                                            text = { Text(stringResource(R.string.open_locate_again)) },
                                            onClick = { menuOpen = false; onRelocateRecent(document) })
                                        if (onRemoveRecent != null) DropdownMenuItem(
                                            text = { Text(stringResource(R.string.open_remove_recent)) },
                                            onClick = { menuOpen = false; removeConfirmation = document })
                                    }
                                }
                            } else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                                tint = MaterialTheme.colorScheme.secondary)
                        }
                        if (index != recent.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
    if (unavailableRecent != null) AlertDialog(onDismissRequest = onUnavailableDismiss,
        title = { Text(stringResource(R.string.open_unavailable_title)) },
        text = { Text(stringResource(R.string.open_unavailable_message, unavailableRecent.name)) },
        confirmButton = { TextButton(onClick = { onUnavailableDismiss(); onRelocateRecent?.invoke(unavailableRecent) }) {
            Text(stringResource(R.string.open_locate_again))
        } },
        dismissButton = { TextButton(onClick = onUnavailableDismiss) {
            Text(stringResource(R.string.groups_cancel))
        } })
    if (removeConfirmation != null) AlertDialog(onDismissRequest = { removeConfirmation = null },
        title = { Text(stringResource(R.string.open_remove_title)) },
        text = { Text(stringResource(R.string.open_remove_message, removeConfirmation!!.name)) },
        confirmButton = { TextButton(onClick = {
            val document = removeConfirmation!!
            removeConfirmation = null
            onRemoveRecent?.invoke(document)
        }) { Text(stringResource(R.string.open_remove_recent)) } },
        dismissButton = { TextButton(onClick = { removeConfirmation = null }) {
            Text(stringResource(R.string.groups_cancel))
        } })
}

@Composable
private fun SourceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    VaultkCard(Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(VaultkSpacing.normal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
        ) {
            VaultkIconTile(icon, null)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OpenVaultPreview() {
    VaultkTheme { OpenVaultScreen(emptyList(), {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun OpenVaultLightPreview() {
    VaultkTheme(darkTheme = false) { OpenVaultScreen(emptyList(), {}, {}, {}) }
}
