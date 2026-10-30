package com.edro08.vaultk.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.edro08.vaultk.ui.theme.VaultkSpacing

import com.edro08.vaultk.ui.theme.vaultkCustomColors

@Composable
fun VaultkIconTile(icon: ImageVector, contentDescription: String?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.size(48.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.vaultkCustomColors.iconTileBorder),
        shape = MaterialTheme.shapes.small,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun VaultkCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        content()
    }
}

@Composable
fun VaultCard(
    name: String,
    origin: String,
    lastOpened: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VaultkCard(modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(VaultkSpacing.normal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
        ) {
            VaultkIconTile(Icons.Default.Lock, null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(origin, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                if (lastOpened != null) {
                    Text(lastOpened, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun EntryCard(
    title: String,
    username: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    VaultkCard(modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(VaultkSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
        ) {
            VaultkIconTile(Icons.Default.Key, null)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(username, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            actions()
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.Folder,
    content: @Composable () -> Unit,
) {
    VaultkCard(modifier) {
        Column(modifier = Modifier.padding(VaultkSpacing.normal), verticalArrangement = Arrangement.spacedBy(VaultkSpacing.normal)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
                VaultkIconTile(icon, null)
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}
