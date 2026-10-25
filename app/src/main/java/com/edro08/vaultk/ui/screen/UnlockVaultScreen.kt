package com.edro08.vaultk.ui.screen

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.edro08.vaultk.ui.component.ActionButton
import com.edro08.vaultk.R
import com.edro08.vaultk.ui.component.EmptyState
import com.edro08.vaultk.ui.component.PasswordField
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.component.VaultkIconTile
import com.edro08.vaultk.ui.component.VaultkHeader
import com.edro08.vaultk.ui.component.VaultkBottomNavigation
import com.edro08.vaultk.ui.component.VaultkDestination
import com.edro08.vaultk.ui.theme.VaultkSpacing
import com.edro08.vaultk.ui.theme.VaultkTheme
import kotlinx.coroutines.delay
import java.util.Locale

/** Display information only; the SAF reference stays in the selection store. */
data class SelectedVault(
    val name: String,
    val origin: String,
    val lastOpened: String?,
    val status: String,
    val biometricEnabled: Boolean = false,
    val identity: String = name,
)

@Composable
fun UnlockVaultScreen(
    vault: SelectedVault?,
    modifier: Modifier = Modifier,
    onSelectVault: () -> Unit,
    onUnlock: ((CharArray) -> Unit)? = null,
    onBiometricUnlock: (() -> Unit)? = null,
    loading: Boolean = false,
    openingStartedAtMillis: Long = 0L,
    errorMessage: String? = null,
    settingsContent: @Composable (Modifier) -> Unit = {},
) {
    var destination by rememberSaveable { mutableStateOf(VaultkDestination.Vault) }
    BackHandler(destination != VaultkDestination.Vault) { destination = VaultkDestination.Vault }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { VaultkHeader(stringResource(when (destination) {
            VaultkDestination.Vault -> R.string.unlock_title
            VaultkDestination.Generator -> R.string.generator_title
            VaultkDestination.Settings -> R.string.common_settings_tab
        })) },
        bottomBar = { VaultkBottomNavigation(destination) { destination = it } },
    ) { insets ->
        if (destination == VaultkDestination.Settings) {
            settingsContent(Modifier.padding(insets))
        } else if (destination == VaultkDestination.Generator) {
            PasswordGeneratorScreen(Modifier.padding(insets))
        } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.large),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.large),
        ) {
            if (vault == null) {
                VaultkCard {
                    EmptyState(
                        title = stringResource(R.string.unlock_no_vault),
                        description = stringResource(R.string.unlock_no_vault_description),
                        action = {
                            ActionButton(stringResource(R.string.common_select_vault), onClick = onSelectVault, icon = Icons.Default.FolderOpen)
                        },
                    )
                }
            } else {
                SelectedVaultRow(vault, onSelectVault = onSelectVault)
                if (vault.biometricEnabled) {
                    VaultkCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(VaultkSpacing.normal),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
                        ) {
                            VaultkIconTile(Icons.Default.Security, null)
                            Text(
                                stringResource(R.string.unlock_biometric_protected),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                // Keep transient credentials outside saved instance state; switching vaults clears them.
                key(vault.identity) {
                    var password by remember { mutableStateOf("") }
                    Column(verticalArrangement = Arrangement.spacedBy(VaultkSpacing.normal)) {
                        Text(stringResource(R.string.unlock_master_key), style = MaterialTheme.typography.titleMedium)
                        PasswordField(password, onValueChange = { password = it }, label = stringResource(R.string.unlock_master_key_hint))
                        Button(
                            onClick = {
                                val key = password.toCharArray()
                                password = ""
                                onUnlock?.invoke(key)
                            },
                            enabled = password.isNotBlank() && onUnlock != null && !loading,
                            modifier = Modifier.fillMaxWidth().testTag("unlock-manual"),
                            shape = MaterialTheme.shapes.small,
                        ) { Text(stringResource(R.string.unlock_action)) }
                        if (loading) {
                            key(openingStartedAtMillis) { OpeningElapsed(openingStartedAtMillis) }
                        }
                        if (errorMessage != null) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        if (onUnlock == null) {
                            Text(
                                stringResource(R.string.unlock_read_pending),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = VaultkSpacing.normal),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small),
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.secondary)
                    Text(stringResource(R.string.unlock_fingerprint_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(if (vault.biometricEnabled) R.string.unlock_fingerprint_ready else R.string.unlock_fingerprint_pending),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (vault.biometricEnabled) {
                        Button(onClick = { onBiometricUnlock?.invoke() }, enabled = onBiometricUnlock != null) {
                            Text(stringResource(R.string.unlock_fingerprint_action))
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun OpeningElapsed(startedAtMillis: Long) {
    var elapsedMillis by remember(startedAtMillis) { mutableStateOf(0L) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            delay(100)
            elapsedMillis = (SystemClock.elapsedRealtime() - startedAtMillis).coerceAtLeast(0L)
        }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = VaultkSpacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
        Row(horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
            verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(VaultkSpacing.large))
            Text(stringResource(R.string.unlock_opening), style = MaterialTheme.typography.titleSmall)
        }
        Text(formatOpeningElapsed(elapsedMillis), style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("unlock-elapsed"))
        Text(stringResource(R.string.unlock_elapsed_label), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun formatOpeningElapsed(elapsedMillis: Long): String {
    val tenths = elapsedMillis.coerceAtLeast(0L) / 100
    val minutes = tenths / 600
    val seconds = (tenths / 10) % 60
    return String.format(Locale.ROOT, "%02d:%02d.%d", minutes, seconds, tenths % 10)
}

@Composable
private fun SelectedVaultRow(vault: SelectedVault, onSelectVault: () -> Unit) {
    VaultkCard(Modifier
        .clickable(onClickLabel = stringResource(R.string.unlock_change_vault), onClick = onSelectVault)
        .testTag("selected-vault-row")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(VaultkSpacing.normal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
            VaultkIconTile(Icons.Default.Lock, null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)) {
                Text(vault.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(vault.origin, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (vault.lastOpened != null) {
                    Text(stringResource(R.string.unlock_last_opened, vault.lastOpened),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(stringResource(R.string.unlock_status, vault.status),
                    color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyVaultPreview() {
    VaultkTheme { UnlockVaultScreen(vault = null, onSelectVault = {}) }
}

@Preview(showBackground = true)
@Composable
private fun SelectedVaultPreview() {
    VaultkTheme {
        UnlockVaultScreen(SelectedVault(stringResource(R.string.demo_file), stringResource(R.string.vault_origin_local),
            stringResource(R.string.demo_today), stringResource(R.string.vault_locked)), onSelectVault = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LightEmptyVaultPreview() {
    VaultkTheme(darkTheme = false) { UnlockVaultScreen(vault = null, onSelectVault = {}) }
}
