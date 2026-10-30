package com.edro08.vaultk.ui.screen.vault

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edro08.vaultk.R
import com.edro08.vaultk.ui.LocalCopyCredential
import com.edro08.vaultk.domain.Totp
import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.component.VaultkHeader
import com.edro08.vaultk.ui.component.VaultkIconTile
import com.edro08.vaultk.ui.theme.VaultkSpacing
import com.edro08.vaultk.domain.VaultValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun EntryDetailHeader(entry: VaultEntry, groupName: String, onBack: () -> Unit,
    onEdit: () -> Unit, onSave: (() -> Unit)? = null,
    onDuplicate: () -> Unit = {}, onMove: () -> Unit = {}, onDelete: () -> Unit = {},
    onRestore: (() -> Unit)? = null, onLock: (() -> Unit)? = null) {
    var menuOpen by remember(entry.id) { mutableStateOf(false) }
    VaultkHeader(title = entry.title, subtitle = groupName, onBack = onBack, icon = Icons.Default.Key,
        actions = {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, stringResource(R.string.detail_edit))
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.content_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (onSave != null) DropdownMenuItem(
                        text = { Text(stringResource(R.string.content_save_action)) },
                        leadingIcon = { Icon(Icons.Default.Save, null) },
                        onClick = { menuOpen = false; onSave() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.detail_duplicate)) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, onClick = { menuOpen = false; onDuplicate() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.detail_move)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) }, onClick = { menuOpen = false; onMove() })
                    if (onRestore != null) DropdownMenuItem(text = { Text(stringResource(R.string.detail_restore)) },
                        leadingIcon = { Icon(Icons.Default.Restore, null) },
                        onClick = { menuOpen = false; onRestore() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.detail_delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menuOpen = false; onDelete() })
                }
            }
        })
}

@Composable
fun EntryDetailScreen(
    entry: VaultEntry,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true,
    groupName: String = "",
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val copyCredential = LocalCopyCredential.current
    val scope = rememberCoroutineScope()
    var passwordVisible by remember(entry.id, entry.password) { mutableStateOf(false) }
    val otp = remember(entry.id, entry.otp) {
        entry.otp?.valueAsChars?.let { chars ->
            try { Totp.parse(String(chars)) } finally { chars.fill('\u0000') }
        }
    }
    DisposableEffect(otp) { onDispose { otp?.clear() } }
    var now by remember(entry.id, entry.otp) { mutableLongStateOf(Instant.now().epochSecond) }
    LaunchedEffect(otp) {
        if (otp != null) while (true) { now = Instant.now().epochSecond; delay(1_000) }
    }
    val copied = stringResource(R.string.detail_copied)
    val invalidUrl = stringResource(R.string.detail_invalid_url)
    val noBrowser = stringResource(R.string.detail_no_browser)
    fun copy(value: String, sensitive: Boolean = true) {
        requireNotNull(copyCredential)(context.getString(R.string.detail_clip_label), value, sensitive)
        scope.launch { onMessage(copied) }
    }

    LazyColumn(modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(VaultkSpacing.normal),
        verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
        val hasCredentials = entry.username.isNotBlank() || entry.password != null || entry.url.isNotBlank()
        if (hasCredentials) item {
            VaultkCard {
                Column(Modifier.fillMaxWidth()) {
                    var needsDivider = false
                    if (entry.username.isNotBlank()) {
                        CredentialRow(
                            title = stringResource(R.string.content_username_label),
                            icon = Icons.Default.Person,
                            actions = {
                                DetailIconAction(stringResource(R.string.detail_copy_username), Icons.Default.ContentCopy) {
                                    copy(entry.username)
                                }
                            }
                        ) {
                            Text(entry.username, style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        needsDivider = true
                    }
                    if (entry.password != null) {
                        if (needsDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        CredentialRow(
                            title = stringResource(R.string.detail_password),
                            icon = Icons.Default.Key,
                            actions = {
                                DetailIconAction(
                                    label = stringResource(if (passwordVisible) R.string.detail_hide_password else R.string.detail_show_password),
                                    icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                                ) {
                                    passwordVisible = !passwordVisible
                                }
                                DetailIconAction(
                                    label = stringResource(R.string.detail_copy_password),
                                    icon = Icons.Default.ContentCopy,
                                    modifier = Modifier.testTag("detail-copy-password")
                                ) {
                                    val chars = entry.password.valueAsChars
                                    try { copy(String(chars), sensitive = true) } finally { chars.fill('\u0000') }
                                }
                            }
                        ) {
                            if (passwordVisible) {
                                val chars = entry.password.valueAsChars
                                val visiblePassword = String(chars)
                                chars.fill('\u0000')
                                Text(visiblePassword, style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            } else {
                                Text(stringResource(R.string.detail_hidden_password),
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        needsDivider = true
                    }
                    if (entry.url.isNotBlank()) {
                        if (needsDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        CredentialRow(
                            title = stringResource(R.string.content_url_label),
                            icon = Icons.Default.Link,
                            actions = {
                                DetailIconAction(stringResource(R.string.detail_open_url), Icons.AutoMirrored.Filled.OpenInNew) {
                                    val uri = runCatching { Uri.parse(entry.url) }.getOrNull()
                                    if (uri?.scheme?.lowercase(Locale.ROOT) !in listOf("https", "http") || uri?.host.isNullOrBlank()) {
                                        onMessage(invalidUrl)
                                    } else try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    } catch (_: ActivityNotFoundException) { onMessage(noBrowser) }
                                    catch (_: SecurityException) { onMessage(noBrowser) }
                                }
                                DetailIconAction(stringResource(R.string.detail_copy_url), Icons.Default.ContentCopy) {
                                    copy(entry.url)
                                }
                            }
                        ) {
                            Text(entry.url, style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (entry.notes.isNotBlank()) item {
            DetailCollapsibleSection(
                title = stringResource(R.string.content_notes_label),
                subtitle = stringResource(R.string.form_notes_subtitle),
                icon = Icons.Default.Description,
                initiallyExpanded = false,
                headerAction = {
                    DetailIconAction(stringResource(R.string.detail_copy_notes), Icons.Default.ContentCopy) {
                        copy(entry.notes)
                    }
                }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(VaultkSpacing.medium)
                    )
                }
            }
        }
        if (otp != null) item {
            DetailSection(stringResource(R.string.detail_totp), Icons.Default.Timer) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                    Text(otp.codeAt(now), modifier = Modifier.weight(1f).testTag("detail-totp-code"),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary)
                    DetailIconAction(stringResource(R.string.detail_copy_totp), Icons.Default.ContentCopy,
                        modifier = Modifier.testTag("detail-copy-totp")) {
                        copy(otp.codeAt(Instant.now().epochSecond), sensitive = true)
                    }
                    Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { otp.secondsLeft(now).toFloat() / otp.period },
                            modifier = Modifier.fillMaxSize())
                        Text(stringResource(R.string.detail_seconds, otp.secondsLeft(now)),
                            modifier = Modifier.testTag("detail-totp-remaining"),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (entry.customFields.isNotEmpty()) item {
            DetailSection(stringResource(R.string.form_custom_fields), Icons.Default.Key) {
                entry.customFields.forEach { (name, value) ->
                    CustomFieldDetail(name, value, ::copy)
                }
            }
        }
        if (entry.attachments.isNotEmpty()) item {
            DetailSection(stringResource(R.string.form_attachments), Icons.Default.AttachFile) {
                entry.attachments.forEach { attachment ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(attachment.name, modifier = Modifier.weight(1f), maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        if (attachment.uri != null) DetailIconAction(stringResource(R.string.form_open_attachment),
                            Icons.AutoMirrored.Filled.OpenInNew) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW)
                                    .setDataAndType(Uri.parse(attachment.uri), attachment.mimeType)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            } catch (_: ActivityNotFoundException) { onMessage(noBrowser) }
                            catch (_: SecurityException) { onMessage(noBrowser) }
                        }
                    }
                }
            }
        }
        if (showDetails) item {
            DetailCollapsibleSection(
                title = stringResource(R.string.detail_properties),
                subtitle = stringResource(R.string.form_properties_subtitle),
                icon = Icons.Default.Info,
                initiallyExpanded = false
            ) {
                val dateFormat = remember {
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(Locale.getDefault())
                        .withZone(ZoneId.systemDefault())
                }

                if (groupName.isNotBlank()) {
                    DetailPropertyItem(
                        icon = Icons.Default.Folder,
                        label = stringResource(R.string.form_property_group_label),
                        value = groupName
                    )
                }

                entry.created?.let { instant ->
                    DetailPropertyItem(
                        icon = Icons.Outlined.Schedule,
                        label = stringResource(R.string.detail_created_label).removeSuffix(":"),
                        value = dateFormat.format(instant)
                    )
                }

                entry.modified?.let { instant ->
                    DetailPropertyItem(
                        icon = Icons.Outlined.History,
                        label = stringResource(R.string.detail_modified_label).removeSuffix(":"),
                        value = dateFormat.format(instant)
                    )
                }

                entry.expiresAt?.let { instant ->
                    DetailPropertyItem(
                        icon = Icons.Outlined.DateRange,
                        label = stringResource(R.string.detail_expires_label).removeSuffix(":"),
                        value = dateFormat.format(instant)
                    )
                }

                if (entry.tags.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = VaultkSpacing.medium, vertical = VaultkSpacing.small),
                            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
                        ) {
                            Text(
                                text = stringResource(R.string.detail_tags),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small),
                                verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
                            ) {
                                entry.tags.forEach { tag ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(VaultkSpacing.small)
                                    ) {
                                        Text(
                                            text = tag,
                                            modifier = Modifier.padding(
                                                horizontal = VaultkSpacing.medium,
                                                vertical = VaultkSpacing.small
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DetailPropertyItem(
                    icon = Icons.Outlined.Fingerprint,
                    label = stringResource(R.string.detail_uuid_label).removeSuffix(":"),
                    value = entry.id,
                    isMonospace = true
                )

                OutlinedButton(
                    onClick = { copy(entry.id, sensitive = false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.detail_copy_uuid),
                        modifier = Modifier.padding(start = VaultkSpacing.small)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailCollapsibleSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    initiallyExpanded: Boolean = false,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var expanded by remember(title, initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    VaultkCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(VaultkSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
            ) {
                VaultkIconTile(icon, null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (headerAction != null && expanded) {
                    headerAction()
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(if (expanded) R.string.form_collapse else R.string.form_expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VaultkSpacing.normal)
                        .padding(bottom = VaultkSpacing.normal),
                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun CredentialRow(
    title: String,
    icon: ImageVector,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
    ) {
        VaultkIconTile(icon, null)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
        if (actions != null) {
            Row(verticalAlignment = Alignment.CenterVertically) { actions() }
        }
    }
}

@Composable
private fun DetailPropertyItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VaultkSpacing.medium, vertical = VaultkSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = if (isMonospace) MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    else MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, icon: ImageVector, actions: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit) {
    VaultkCard {
        Row(Modifier.fillMaxWidth().padding(VaultkSpacing.normal),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
            VaultkIconTile(icon, null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                content()
            }
            if (actions != null) Row(verticalAlignment = Alignment.CenterVertically) { actions() }
        }
    }
}

@Composable
private fun DetailActions(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically) { content() }
}

@Composable
private fun DetailIconAction(label: String, icon: ImageVector, modifier: Modifier = Modifier,
    onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PropertiesSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    VaultkCard {
        Column(Modifier.fillMaxWidth().padding(VaultkSpacing.normal),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
                VaultkIconTile(icon, null)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    PropertyRow(label) {
        Text(value, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PropertyRow(label: String, content: @Composable () -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth().padding(vertical = VaultkSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(2f)) { content() }
    }
}

@Composable
private fun CustomFieldDetail(name: String, value: VaultValue, copy: (String, Boolean) -> Unit) {
    var visible by remember(name, value) { mutableStateOf(false) }
    Text(name, style = MaterialTheme.typography.labelLarge)
    if (value.isProtected && !visible) {
        Text(stringResource(R.string.form_hidden_value))
    } else {
        val chars = value.valueAsChars
        val text = try { String(chars) } finally { chars.fill('\u0000') }
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
    DetailActions {
        if (value.isProtected) DetailIconAction(
            stringResource(if (visible) R.string.detail_hide_field else R.string.detail_show_field, name),
            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility) { visible = !visible }
        DetailIconAction(stringResource(R.string.detail_copy_field, name), Icons.Default.ContentCopy) {
            val chars = value.valueAsChars
            try { copy(String(chars), value.isProtected) } finally { chars.fill('\u0000') }
        }
    }
}
