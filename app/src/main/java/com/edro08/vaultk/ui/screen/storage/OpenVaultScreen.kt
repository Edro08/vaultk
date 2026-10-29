package com.edro08.vaultk.ui.screen.storage

import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.edro08.vaultk.R
import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.FileAccessMode
import com.edro08.vaultk.domain.VaultDocument
import com.edro08.vaultk.ui.component.EmptyState
import com.edro08.vaultk.ui.component.VaultkBottomNavigation
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.component.VaultkDestination
import com.edro08.vaultk.ui.component.VaultkHeader
import com.edro08.vaultk.ui.component.VaultkNotificationCard
import com.edro08.vaultk.ui.component.VaultkIconTile
import com.edro08.vaultk.ui.theme.VaultkSpacing
import com.edro08.vaultk.ui.theme.VaultkTheme
import com.edro08.vaultk.ui.uri
import java.text.DateFormat
import java.util.Date

@Composable
fun OpenVaultScreen(
    recent: List<VaultDocument>,
    onBack: () -> Unit,
    onDevice: () -> Unit,
    onRecent: (VaultDocument) -> Unit,
    onOneDrive: (() -> Unit)? = null,
    onCloudCopies: (() -> Unit)? = null,
    errorMessage: String? = null,
    onErrorShown: () -> Unit = {},
    fileAccessMode: FileAccessMode = FileAccessMode.Standard,
    currentUri: Uri? = null,
    unavailableRecent: VaultDocument? = null,
    onUnavailableDismiss: () -> Unit = {},
    onRelocateRecent: ((VaultDocument) -> Unit)? = null,
    onRemoveRecent: ((VaultDocument) -> Unit)? = null,
    cloudCopies: List<CloudCopy> = emptyList(),
    onCloudCopyClick: ((CloudCopy) -> Unit)? = null,
    oneDriveConnected: Boolean? = null,
    destination: VaultkDestination = VaultkDestination.Vault,
    onDestinationChange: (VaultkDestination) -> Unit = {},
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
        bottomBar = {
            VaultkBottomNavigation(selected = destination, onSelect = onDestinationChange)
        },
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                VaultkNotificationCard(
                    message = data.visuals.message,
                    isError = true,
                    icon = Icons.Outlined.ErrorOutline,
                    onDismiss = { data.dismiss() }
                )
            }
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.large),
        ) {
            Text(
                text = stringResource(R.string.open_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // SECTION 1: DISPOSITIVO
            Column(verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                Text(
                    text = stringResource(R.string.open_device_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                ActionTileCard(
                    title = stringResource(R.string.open_local_files_title),
                    subtitle = stringResource(R.string.open_local_files_subtitle),
                    icon = Icons.Outlined.PhoneAndroid,
                    onClick = onDevice
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // SECTION 2: ALMACENAMIENTO EN LA NUBE
            if (onOneDrive != null) {
                Column(verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                    Text(
                        text = stringResource(R.string.open_cloud_storage_section),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    val connectedStatus = when (oneDriveConnected) {
                        true -> stringResource(R.string.open_onedrive_connected)
                        false -> stringResource(R.string.open_onedrive_not_connected)
                        null -> null
                    }
                    val connectedColor = if (oneDriveConnected == true) Color(0xFF44DB98) else MaterialTheme.colorScheme.onSurfaceVariant
                    ActionTileCard(
                        title = stringResource(R.string.open_onedrive),
                        subtitle = stringResource(R.string.open_onedrive_subtitle),
                        statusTag = connectedStatus,
                        statusColor = connectedColor,
                        statusIcon = if (oneDriveConnected == true) Icons.Outlined.CheckCircle else null,
                        icon = Icons.Outlined.Cloud,
                        onClick = onOneDrive
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // SECTION 3: TUS VAULTS RECIENTES
            Column(verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                Text(
                    text = stringResource(R.string.open_recent_vaults_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )

                if (cloudCopies.isEmpty() && recent.isEmpty()) {
                    VaultkCard {
                        EmptyState(
                            stringResource(R.string.open_no_recent),
                            stringResource(R.string.open_no_recent_description)
                        )
                    }
                } else {
                    // Cloud copies first (or integrated with recent)
                    cloudCopies.forEach { copy ->
                        ActionTileCard(
                            title = copy.name,
                            subtitle = stringResource(R.string.open_recent_cloud_subtitle),
                            statusTag = stringResource(R.string.open_recent_clean_status),
                            statusColor = Color(0xFF44DB98),
                            icon = Icons.Outlined.Lock,
                            onClick = { onCloudCopyClick?.invoke(copy) }
                        )
                    }

                    // Local / Document recent vaults
                    recent.forEach { document ->
                        var menuOpen by remember(document.uri) { mutableStateOf(false) }
                        val isCurrent = document.uri == currentUri
                        val lastOpenedText = document.openedAt?.let { time ->
                            stringResource(R.string.open_last_opened,
                                DateFormat.getDateTimeInstance().format(Date(time)))
                        }
                        ActionTileCard(
                            title = document.name,
                            subtitle = stringResource(
                                if (document.uri.scheme == "file") R.string.open_recent_local_subtitle
                                else R.string.open_recent_document
                            ),
                            statusTag = if (isCurrent) stringResource(R.string.open_current) else lastOpenedText,
                            extraTag = if (isCurrent && lastOpenedText != null) lastOpenedText else null,
                            statusColor = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            statusIcon = if (isCurrent) Icons.Outlined.CheckCircle else null,
                            icon = Icons.Outlined.Lock,
                            onClick = { onRecent(document) },
                            trailing = {
                                if (onRelocateRecent != null || onRemoveRecent != null) {
                                    Box {
                                        IconButton(onClick = { menuOpen = true }) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                stringResource(R.string.open_recent_actions, document.name),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                                            if (onRelocateRecent != null) DropdownMenuItem(
                                                text = { Text(stringResource(R.string.open_locate_again)) },
                                                onClick = { menuOpen = false; onRelocateRecent(document) }
                                            )
                                            if (onRemoveRecent != null) DropdownMenuItem(
                                                text = { Text(stringResource(R.string.open_remove_recent)) },
                                                onClick = { menuOpen = false; removeConfirmation = document }
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // BOTTOM ACTION: Gestionar copias sin conexión
            if (onCloudCopies != null) {
                Spacer(Modifier.height(VaultkSpacing.medium))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCloudCopies)
                        .padding(vertical = VaultkSpacing.medium),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(VaultkSpacing.small))
                    Text(
                        text = stringResource(R.string.open_manage_offline_copies),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (unavailableRecent != null) AlertDialog(
        onDismissRequest = onUnavailableDismiss,
        title = { Text(stringResource(R.string.open_unavailable_title)) },
        text = { Text(stringResource(R.string.open_unavailable_message, unavailableRecent.name)) },
        confirmButton = {
            TextButton(onClick = { onUnavailableDismiss(); onRelocateRecent?.invoke(unavailableRecent) }) {
                Text(stringResource(R.string.open_locate_again))
            }
        },
        dismissButton = {
            TextButton(onClick = onUnavailableDismiss) {
                Text(stringResource(R.string.groups_cancel))
            }
        }
    )

    if (removeConfirmation != null) AlertDialog(
        onDismissRequest = { removeConfirmation = null },
        title = { Text(stringResource(R.string.open_remove_title)) },
        text = { Text(stringResource(R.string.open_remove_message, removeConfirmation!!.name)) },
        confirmButton = {
            TextButton(onClick = {
                val document = removeConfirmation!!
                removeConfirmation = null
                onRemoveRecent?.invoke(document)
            }) { Text(stringResource(R.string.open_remove_recent)) }
        },
        dismissButton = {
            TextButton(onClick = { removeConfirmation = null }) {
                Text(stringResource(R.string.groups_cancel))
            }
        }
    )
}

@Composable
private fun ActionTileCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusTag: String? = null,
    extraTag: String? = null,
    statusColor: Color = Color(0xFF44DB98),
    statusIcon: ImageVector? = Icons.Outlined.CheckCircle,
    trailing: (@Composable () -> Unit)? = null
) {
    VaultkCard(modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VaultkSpacing.medium, vertical = VaultkSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
        ) {
            VaultkIconTile(icon, null)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (statusTag != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
                    ) {
                        if (statusIcon != null) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = statusTag,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (extraTag != null) {
                    Text(
                        text = extraTag,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
