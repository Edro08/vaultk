package com.edro08.vaultk.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.content_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (onLock != null) DropdownMenuItem(text = { Text(stringResource(R.string.content_lock)) },
                        onClick = { menuOpen = false; onLock() })
                    if (onSave != null) DropdownMenuItem(
                        text = { Text(stringResource(R.string.content_save_action)) },
                        onClick = { menuOpen = false; onSave() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.detail_duplicate)) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, onClick = { menuOpen = false; onDuplicate() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.detail_move)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) }, onClick = { menuOpen = false; onMove() })
                    if (onRestore != null) DropdownMenuItem(text = { Text(stringResource(R.string.detail_restore)) },
                        onClick = { menuOpen = false; onRestore() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.detail_delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menuOpen = false; onDelete() })
                }
            }
        })
}

@Composable
fun EntryDetailScreen(entry: VaultEntry, modifier: Modifier = Modifier, showDetails: Boolean = true,
    onMessage: (String) -> Unit) {
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(VaultkSpacing.normal),
        verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
        if (entry.username.isNotBlank()) item {
            DetailSection(stringResource(R.string.content_username_label), Icons.Default.Person,
                actions = {
                    DetailIconAction(stringResource(R.string.detail_copy_username), Icons.Default.ContentCopy) {
                        copy(entry.username)
                    }
                }) {
                Text(entry.username, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (entry.password != null) item {
            DetailSection(stringResource(R.string.detail_password), Icons.Default.Key,
                actions = {
                    DetailIconAction(stringResource(if (passwordVisible) R.string.detail_hide_password else R.string.detail_show_password),
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility) {
                        passwordVisible = !passwordVisible
                    }
                    DetailIconAction(stringResource(R.string.detail_copy_password), Icons.Default.ContentCopy,
                        modifier = Modifier.testTag("detail-copy-password")) {
                        val chars = entry.password.valueAsChars
                        try { copy(String(chars), sensitive = true) } finally { chars.fill('\u0000') }
                    }
                }) {
                if (passwordVisible) {
                    val chars = entry.password.valueAsChars
                    val visiblePassword = String(chars)
                    chars.fill('\u0000')
                    Text(visiblePassword, style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                } else Text(stringResource(R.string.detail_hidden_password),
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (entry.url.isNotBlank()) item {
            DetailSection(stringResource(R.string.content_url_label), Icons.Default.Link,
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
                }) {
                Text(entry.url, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (entry.notes.isNotBlank()) item {
            DetailSection(stringResource(R.string.content_notes_label), Icons.AutoMirrored.Filled.Notes,
                actions = {
                    DetailIconAction(stringResource(R.string.detail_copy_notes), Icons.Default.ContentCopy) {
                        copy(entry.notes)
                    }
                }) {
                Text(entry.notes, style = MaterialTheme.typography.bodyLarge)
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
            PropertiesSection(stringResource(R.string.detail_properties), Icons.Default.Info) {
                entry.created?.let { instant ->
                    val formatted = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()).format(instant)
                    PropertyRow(stringResource(R.string.detail_created_label), formatted)
                }
                entry.modified?.let { instant ->
                    val formatted = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()).format(instant)
                    PropertyRow(stringResource(R.string.detail_modified_label), formatted)
                }
                entry.expiresAt?.let { instant ->
                    val formatted = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()).format(instant)
                    PropertyRow(stringResource(R.string.detail_expires_label), formatted)
                }
                if (entry.tags.isNotEmpty()) {
                    PropertyRow(stringResource(R.string.detail_tags)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small),
                            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                            entry.tags.forEach { tag ->
                                Surface(color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(VaultkSpacing.small)) {
                                    Text(tag, modifier = Modifier.padding(horizontal = VaultkSpacing.medium,
                                        vertical = VaultkSpacing.small), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
                PropertyRow(stringResource(R.string.detail_uuid_label), entry.id)
                TextButton(onClick = { copy(entry.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Text(stringResource(R.string.detail_copy_uuid), modifier = Modifier.padding(start = VaultkSpacing.small))
                }
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
