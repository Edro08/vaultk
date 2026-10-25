package com.edro08.vaultk.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.edro08.vaultk.R
import com.edro08.vaultk.ui.component.ActionButton
import com.edro08.vaultk.ui.component.EmptyState
import com.edro08.vaultk.ui.component.EntryCard
import com.edro08.vaultk.ui.component.InputField
import com.edro08.vaultk.ui.component.LoadingState
import com.edro08.vaultk.ui.component.PasswordField
import com.edro08.vaultk.ui.component.SectionCard
import com.edro08.vaultk.ui.component.VaultCard
import com.edro08.vaultk.ui.component.VaultkBottomNavigation
import com.edro08.vaultk.ui.component.VaultkBottomSheet
import com.edro08.vaultk.ui.component.VaultkDestination
import com.edro08.vaultk.ui.component.VaultkHeader
import com.edro08.vaultk.ui.theme.VaultkSpacing
import com.edro08.vaultk.ui.theme.VaultkTheme

/** Temporary component gallery for phase 01; the unlock screen replaces this in phase 02. */
@Composable
fun DesignSystemScreen() {
    var title by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(VaultkDestination.Vault) }
    var showSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { VaultkHeader(title = stringResource(R.string.app_name), subtitle = stringResource(R.string.demo_phase)) },
        bottomBar = { VaultkBottomNavigation(selected, onSelect = { selected = it }) },
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.large),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.normal),
        ) {
            Text(stringResource(R.string.demo_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.demo_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            VaultCard(stringResource(R.string.demo_file), stringResource(R.string.vault_origin_local), stringResource(R.string.demo_last_opened), onClick = { showSheet = true })
            EntryCard(stringResource(R.string.demo_entry), stringResource(R.string.demo_username), onClick = { showSheet = true })
            SectionCard(stringResource(R.string.demo_general), subtitle = stringResource(R.string.demo_general_description), icon = Icons.Default.Folder) {
                InputField(title, onValueChange = { title = it }, label = stringResource(R.string.demo_field_title), placeholder = stringResource(R.string.demo_field_hint))
                PasswordField(password, onValueChange = { password = it })
            }
            ActionButton(stringResource(R.string.demo_open_sheet), onClick = { showSheet = true }, icon = Icons.Default.Add, modifier = Modifier.fillMaxWidth())
            EmptyState(stringResource(R.string.demo_empty_title), stringResource(R.string.demo_empty_description))
            LoadingState(stringResource(R.string.demo_loading))
        }
    }
    if (showSheet) {
        VaultkBottomSheet(onDismiss = { showSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(VaultkSpacing.large),
                verticalArrangement = Arrangement.spacedBy(VaultkSpacing.normal),
            ) {
                Text(stringResource(R.string.demo_sheet_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.demo_sheet_description))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DesignSystemDarkPreview() {
    VaultkTheme { DesignSystemScreen() }
}

@Preview(showBackground = true)
@Composable
private fun DesignSystemLightPreview() {
    VaultkTheme(darkTheme = false) { DesignSystemScreen() }
}
