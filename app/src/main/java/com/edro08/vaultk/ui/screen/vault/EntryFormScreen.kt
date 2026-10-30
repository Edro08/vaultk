package com.edro08.vaultk.ui.screen.vault

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.edro08.vaultk.R
import com.edro08.vaultk.domain.security.Totp
import com.edro08.vaultk.domain.vault.VaultAttachment
import com.edro08.vaultk.domain.vault.VaultEntry
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.component.VaultkHeader
import com.edro08.vaultk.ui.component.VaultkBottomNavigation
import com.edro08.vaultk.ui.component.VaultkDestination
import com.edro08.vaultk.ui.component.VaultkIconTile
import com.edro08.vaultk.ui.theme.VaultkSpacing
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import java.util.Locale
import com.edro08.vaultk.application.vault.EntryFormData
import com.edro08.vaultk.application.vault.CustomFieldDraft
import com.edro08.vaultk.domain.vault.VaultValue
import com.edro08.vaultk.ui.screen.generator.PasswordGeneratorScreen

private fun VaultValue.asFormText(): String {
    val chars = valueAsChars
    return try { String(chars) } finally { chars.fill('\u0000') }
}

private fun VaultEntry.formData(): EntryFormData = EntryFormData(id, title, username,
    password?.asFormText().orEmpty(), url, notes, tags, expiresAt,
    otp?.asFormText().orEmpty(), customFields["PIN"]?.asFormText().orEmpty(),
    customFields.filterKeys { it != "PIN" }.map { (name, value) ->
        CustomFieldDraft(name, value.asFormText(), value.isProtected)
    }, attachments)

/** The same single-scroll form serves new entries and existing entries. */
@Composable
fun EntryFormScreen(groupName: String, onSave: (EntryFormData) -> Unit, onBack: () -> Unit,
    initialEntry: VaultEntry? = null, onExternalPickerChange: (Boolean) -> Unit = {},
    onNavigate: (VaultkDestination) -> Unit = {}, initialPassword: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val initial = remember(initialEntry) { initialEntry?.formData() }
    val id = remember(initialEntry?.id) { initial?.id ?: "draft:${UUID.randomUUID()}" }
    var title by remember(id) { mutableStateOf(initial?.title.orEmpty()) }
    var titleTouched by remember(id) { mutableStateOf(false) }
    var username by remember(id) { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember(id) { mutableStateOf(initial?.password ?: initialPassword.orEmpty()) }
    var url by remember(id) { mutableStateOf(initial?.url.orEmpty()) }
    var notes by remember(id) { mutableStateOf(initial?.notes.orEmpty()) }
    val notesLimit = maxOf(2000, initial?.notes?.length ?: 0)
    var pin by remember(id) { mutableStateOf(initial?.pin.orEmpty()) }
    var otpInput by remember(id) { mutableStateOf(initial?.otp.orEmpty()) }
    var tagInput by remember { mutableStateOf("") }
    var expires by remember(id) { mutableStateOf(initial?.expiresAt != null) }
    var expiryDate by remember(id) { mutableStateOf(initial?.expiresAt?.atZone(ZoneId.systemDefault())?.toLocalDate()) }
    var passwordVisible by remember { mutableStateOf(false) }
    var otpVisible by remember { mutableStateOf(false) }
    var showGenerator by remember { mutableStateOf(false) }
    var pinVisible by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showClearPassword by remember { mutableStateOf(false) }
    var pendingDestination by remember { mutableStateOf<VaultkDestination?>(null) }
    var showCustomField by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customValue by remember { mutableStateOf("") }
    var customProtected by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<String?>(null) }
    val tags = remember(id) { mutableStateListOf<String>().apply { addAll(initial?.tags.orEmpty()) } }
    val fields = remember(id) { mutableStateListOf<CustomFieldDraft>().apply { addAll(initial?.fields.orEmpty()) } }
    val attachments = remember(id) { mutableStateListOf<VaultAttachment>().apply { addAll(initial?.attachments.orEmpty()) } }
    val otpUri = remember(otpInput) { Totp.credentialUri(otpInput) }
    val otpValid = otpInput.isBlank() || otpUri != null
    val changed = if (initial != null) title != initial.title || username != initial.username ||
        password != initial.password || url != initial.url || notes != initial.notes ||
        pin != initial.pin || otpInput != initial.otp || tags != initial.tags ||
        fields != initial.fields || attachments != initial.attachments ||
        expires != (initial.expiresAt != null) ||
        (expires && expiryDate != initial.expiresAt?.atZone(ZoneId.systemDefault())?.toLocalDate()) ||
        tagInput.isNotBlank()
    else title.isNotBlank() || username.isNotBlank() || password.isNotEmpty() || url.isNotBlank() ||
        notes.isNotBlank() || tags.isNotEmpty() || pin.isNotEmpty() || otpInput.isNotBlank() ||
        fields.isNotEmpty() || attachments.isNotEmpty() || expires || tagInput.isNotBlank()
    val currentChanged by rememberUpdatedState(changed)
    fun goBack() { if (currentChanged) showDiscard = true else onBack() }
    fun navigateTo(target: VaultkDestination) {
        if (target == VaultkDestination.Generator) showGenerator = true
        else if (target == VaultkDestination.Vault) {
            if (showGenerator) showGenerator = false else goBack()
        } else if (currentChanged) {
            showGenerator = false
            pendingDestination = target
            showDiscard = true
        } else onNavigate(target)
    }
    fun addTag() {
        val tag = tagInput.trim()
        if (tag.isNotEmpty() && tags.none { it.equals(tag, ignoreCase = true) }) tags.add(tag)
        tagInput = ""
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        onExternalPickerChange(false)
        if (uri != null && attachments.none { it.uri == uri.toString() }) {
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor: Cursor ->
                        if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                        else null
                    }
            }.getOrNull() ?: context.getString(R.string.form_unnamed_attachment)
            val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull() ?: "application/octet-stream"
            attachments.add(VaultAttachment(name, uri.toString(), mime))
        }
    }
    val attachmentError = stringResource(R.string.form_attachment_open_error)
    BackHandler { if (showGenerator) showGenerator = false else goBack() }
    if (showGenerator) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background,
            topBar = { VaultkHeader(stringResource(R.string.generator_title),
                onBack = { showGenerator = false }) },
            bottomBar = { VaultkBottomNavigation(VaultkDestination.Generator, ::navigateTo) }) { insets ->
            PasswordGeneratorScreen(Modifier.padding(insets), onUse = {
                password = it; passwordVisible = false; showGenerator = false
            })
        }
        return
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VaultkHeader(stringResource(if (initial != null) R.string.form_edit_title else R.string.form_new_title),
                subtitle = groupName,
                onBack = { goBack() },
                icon = if (initial != null) Icons.Default.Key else Icons.Default.Description, actions = {
                    IconButton(onClick = {
                        if (initial != null && !changed) { onBack(); return@IconButton }
                        val pendingTag = tagInput.trim()
                        val savedTags = tags.toList() + listOfNotNull(pendingTag.takeIf { tag ->
                            tag.isNotEmpty() && tags.none { it.equals(tag, ignoreCase = true) }
                        })
                        val originalExpiry = initial?.expiresAt
                        val savedExpiry = if (!expires) null else if (expiryDate ==
                            originalExpiry?.atZone(ZoneId.systemDefault())?.toLocalDate()) originalExpiry
                            else expiryDate?.atTime(23, 59, 59)?.atZone(ZoneId.systemDefault())?.toInstant()
                        onSave(EntryFormData(id, title.trim(), username, password, url.trim(), notes,
                            savedTags, savedExpiry,
                             otpUri.orEmpty(), pin, fields.toList(), attachments.toList()))
                    }, enabled = title.isNotBlank() && otpValid && (!expires || expiryDate != null)) {
                        Icon(Icons.Default.Save, stringResource(R.string.form_save))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.content_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.form_discard)) },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                                },
                                onClick = { menuOpen = false; goBack() }
                            )
                        }
                    }
                })
        }, bottomBar = { VaultkBottomNavigation(VaultkDestination.Vault, ::navigateTo) },
        snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
            .padding(VaultkSpacing.normal),
            verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
            FormSection(stringResource(R.string.form_general), stringResource(R.string.form_general_subtitle),
                Icons.Default.Description, initiallyExpanded = true) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleTouched = true },
                    label = { Text(stringResource(R.string.form_title)) },
                    placeholder = { Text(stringResource(R.string.form_title_hint)) },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (titleTouched && title.isBlank()) Text(stringResource(R.string.form_title_required),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.content_username_label)) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.detail_password)) },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                stringResource(if (passwordVisible) R.string.common_hide_password else R.string.common_show_password))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)) {
                    OutlinedButton(
                        onClick = { showGenerator = true },
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.Casino, null)
                        Text(stringResource(R.string.form_generate_password), Modifier.padding(start = VaultkSpacing.small))
                    }
                    if (password.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearPassword = true },
                            modifier = Modifier.heightIn(min = 44.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.DeleteOutline, null)
                            Text(stringResource(R.string.form_password_clear), Modifier.padding(start = VaultkSpacing.small))
                        }
                    }
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.content_url_label)) },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.detail_tags), style = MaterialTheme.typography.titleSmall)
                if (tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small),
                        verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(VaultkSpacing.small)
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = VaultkSpacing.small),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        tag,
                                        modifier = Modifier.padding(vertical = VaultkSpacing.tiny),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(onClick = { tags.remove(tag) }, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            Icons.Default.Close,
                                            stringResource(R.string.form_remove_tag, tag),
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text(stringResource(R.string.form_add_tag)) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        IconButton(onClick = { addTag() }, enabled = tagInput.isNotBlank()) {
                            Icon(Icons.Default.Add, stringResource(R.string.form_add_tag))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.form_expiry), modifier = Modifier.weight(1f).padding(start = VaultkSpacing.small))
                    Switch(checked = expires, onCheckedChange = { expires = it })
                }
                if (expires) {
                    val selectedDate = expiryDate ?: LocalDate.now()
                    OutlinedButton(onClick = {
                        DatePickerDialog(context, { _, year, month, day ->
                            expiryDate = LocalDate.of(year, month + 1, day)
                        }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DateRange, null)
                        Text(expiryDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                            .withLocale(Locale.getDefault()))
                            ?: stringResource(R.string.form_select_date),
                            modifier = Modifier.padding(start = VaultkSpacing.small))
                    }
                }
            }
            FormSection(
                title = stringResource(R.string.form_notes),
                subtitle = stringResource(R.string.form_notes_subtitle),
                icon = Icons.Default.Description,
                initiallyExpanded = true
            ) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= notesLimit) notes = it },
                    label = { Text(stringResource(R.string.content_notes_label)) },
                    placeholder = { Text(stringResource(R.string.form_notes_hint)) },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.form_notes_count, notes.length, notesLimit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
            FormSection(stringResource(R.string.form_advanced), stringResource(R.string.form_advanced_subtitle),
                Icons.Default.Settings, initiallyExpanded = initial?.let {
                    it.fields.isNotEmpty() || it.otp.isNotBlank() || it.pin.isNotBlank()
                } == true) {
                Text(stringResource(R.string.form_custom_fields), style = MaterialTheme.typography.titleSmall)
                fields.forEach { field ->
                    Row(Modifier.fillMaxWidth().clickable {
                        editingField = field.name
                        customName = field.name
                        customValue = field.value
                        customProtected = field.isProtected
                        showCustomField = true
                    }, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(field.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (field.isProtected) stringResource(R.string.form_hidden_value) else field.value,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { fields.remove(field) }) {
                            Icon(Icons.Default.Close, stringResource(R.string.form_remove_field, field.name))
                        }
                    }
                }
                OutlinedButton(onClick = { editingField = null; customName = ""; customValue = ""; customProtected = false; showCustomField = true },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Text(stringResource(R.string.form_add_field), modifier = Modifier.padding(start = VaultkSpacing.small))
                }
                OutlinedTextField(
                    value = otpInput,
                    onValueChange = { otpInput = it },
                    label = { Text(stringResource(R.string.form_totp_secret)) },
                    placeholder = { Text(stringResource(R.string.form_totp_hint)) },
                    leadingIcon = {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    visualTransformation = if (otpVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { otpVisible = !otpVisible }) {
                            Icon(if (otpVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                stringResource(if (otpVisible) R.string.common_hide_password else R.string.common_show_password))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!otpValid) Text(stringResource(R.string.form_invalid_totp), color = MaterialTheme.colorScheme.error)
                val invalidQr = stringResource(R.string.form_invalid_totp_qr)
                val scannerError = stringResource(R.string.form_totp_scan_error)
                OutlinedButton(onClick = {
                    onExternalPickerChange(true)
                    try {
                        val options = GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                        GmsBarcodeScanning.getClient(context, options).startScan()
                            .addOnSuccessListener { barcode ->
                                val uri = barcode.rawValue?.let(Totp::credentialUri)
                                onExternalPickerChange(false)
                                if (uri != null) { otpInput = uri; otpVisible = false }
                                else scope.launch { snackbar.showSnackbar(invalidQr) }
                            }
                            .addOnFailureListener {
                                onExternalPickerChange(false)
                                scope.launch { snackbar.showSnackbar(scannerError) }
                            }
                            .addOnCanceledListener { onExternalPickerChange(false) }
                    } catch (_: Exception) {
                        onExternalPickerChange(false)
                        scope.launch { snackbar.showSnackbar(scannerError) }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.form_scan_totp_qr))
                }
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text(stringResource(R.string.form_pin)) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                stringResource(if (pinVisible) R.string.common_hide_password else R.string.common_show_password))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FormSection(stringResource(R.string.form_attachments), stringResource(R.string.form_attachments_subtitle),
                Icons.Default.AttachFile, initiallyExpanded = initial?.attachments?.isNotEmpty() == true) {
                if (attachments.isEmpty()) Text(stringResource(R.string.form_no_attachments),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                attachments.forEach { attachment ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(attachment.name, modifier = Modifier.weight(1f), maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        TextButton(enabled = attachment.uri != null, onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(attachment.uri), attachment.mimeType)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            } catch (_: ActivityNotFoundException) { scope.launch { snackbar.showSnackbar(attachmentError) } }
                            catch (_: SecurityException) { scope.launch { snackbar.showSnackbar(attachmentError) } }
                        }) { Text(stringResource(R.string.form_open_attachment)) }
                        IconButton(onClick = { attachments.remove(attachment) }) {
                            Icon(Icons.Default.Close, stringResource(R.string.form_remove_attachment, attachment.name))
                        }
                    }
                }
                OutlinedButton(onClick = {
                    onExternalPickerChange(true)
                    try { launcher.launch(arrayOf("*/*")) }
                    catch (_: Exception) { onExternalPickerChange(false) }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Text(stringResource(R.string.form_add_attachment), modifier = Modifier.padding(start = VaultkSpacing.small))
                }
            }
            FormSection(
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

                FormPropertyRow(
                    icon = Icons.Default.Folder,
                    label = stringResource(R.string.form_property_group_label),
                    value = groupName.ifBlank { stringResource(R.string.content_root) }
                )

                val createdText = initialEntry?.created?.let { dateFormat.format(it) }
                    ?: stringResource(R.string.form_property_pending)
                FormPropertyRow(
                    icon = Icons.Outlined.Schedule,
                    label = stringResource(R.string.form_property_created_label),
                    value = createdText
                )

                val modifiedText = initialEntry?.modified?.let { dateFormat.format(it) }
                    ?: stringResource(R.string.form_property_pending)
                FormPropertyRow(
                    icon = Icons.Outlined.History,
                    label = stringResource(R.string.form_property_modified_label),
                    value = modifiedText
                )

                if (initialEntry != null) {
                    FormPropertyRow(
                        icon = Icons.Outlined.Fingerprint,
                        label = stringResource(R.string.form_property_uuid_label),
                        value = id,
                        isMonospace = true,
                        trailingAction = {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard?.setPrimaryClip(ClipData.newPlainText("UUID", id))
                                scope.launch {
                                    snackbar.showSnackbar(context.getString(R.string.form_property_uuid_copied))
                                }
                            }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.detail_copy_uuid),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }
    }
    if (showClearPassword) AlertDialog(onDismissRequest = { showClearPassword = false },
        title = { Text(stringResource(R.string.form_password_clear_title)) },
        text = { Text(stringResource(R.string.form_password_clear_message)) },
        confirmButton = { TextButton(onClick = {
            password = ""
            passwordVisible = false
            showClearPassword = false
        }) { Text(stringResource(R.string.form_password_clear_confirm)) } },
        dismissButton = { TextButton(onClick = { showClearPassword = false }) {
            Text(stringResource(R.string.groups_cancel))
        } })
    if (showDiscard) AlertDialog(onDismissRequest = { showDiscard = false; pendingDestination = null },
        title = { Text(stringResource(if (initial != null) R.string.form_discard_edit_title else R.string.form_discard_title)) },
        text = { Text(stringResource(if (initial != null) R.string.form_discard_edit_message else R.string.form_discard_message)) },
        confirmButton = { TextButton(onClick = {
            showDiscard = false
            val target = pendingDestination
            pendingDestination = null
            if (target != null) onNavigate(target) else onBack()
        }) {
            Text(stringResource(R.string.form_discard))
        } },
        dismissButton = { TextButton(onClick = { showDiscard = false; pendingDestination = null }) {
            Text(stringResource(R.string.groups_cancel))
        } })
    if (showCustomField) AlertDialog(onDismissRequest = { showCustomField = false },
        title = { Text(stringResource(if (editingField != null) R.string.form_edit_field else R.string.form_add_field)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
                OutlinedTextField(customName, { customName = it },
                    label = { Text(stringResource(R.string.form_field_name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(customValue, { customValue = it },
                    label = { Text(stringResource(R.string.form_field_value)) }, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (customProtected) PasswordVisualTransformation() else VisualTransformation.None)
                Row(Modifier.fillMaxWidth().toggleable(value = customProtected,
                    role = Role.Checkbox, onValueChange = { customProtected = it }),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = customProtected, onCheckedChange = null)
                    Text(stringResource(R.string.form_protect_field))
                }
                if (customName.trim().isNotEmpty() && fields.any {
                    it.name != editingField && it.name.equals(customName.trim(), ignoreCase = true)
                }) {
                    Text(stringResource(R.string.form_duplicate_field), color = MaterialTheme.colorScheme.error)
                }
                if (listOf("Title", "UserName", "Password", "URL", "Notes", "otp", "PIN", "Tags")
                        .any { it.equals(customName.trim(), ignoreCase = true) }) {
                    Text(stringResource(R.string.form_reserved_field), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = customName.trim().isNotEmpty() &&
                fields.none { it.name != editingField && it.name.equals(customName.trim(), ignoreCase = true) } &&
                listOf("Title", "UserName", "Password", "URL", "Notes", "otp", "PIN", "Tags")
                    .none { it.equals(customName.trim(), ignoreCase = true) },
                onClick = {
                    val index = fields.indexOfFirst { it.name == editingField }
                    val updated = CustomFieldDraft(customName.trim(), customValue, customProtected)
                    if (index >= 0) fields[index] = updated else fields.add(updated)
                    showCustomField = false
                }) {
                Text(stringResource(if (editingField != null) R.string.form_update_field else R.string.form_add))
            }
        },
        dismissButton = { TextButton(onClick = { showCustomField = false }) { Text(stringResource(R.string.groups_cancel)) } })
}

@Composable
private fun FormSection(title: String, subtitle: String,
    icon: ImageVector, initiallyExpanded: Boolean,
    content: @Composable () -> Unit) {
    var expanded by remember(title, initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    VaultkCard {
        Column {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(VaultkSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
                VaultkIconTile(icon, null)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    stringResource(if (expanded) R.string.form_collapse else R.string.form_expand))
            }
            if (expanded) Column(Modifier.fillMaxWidth().padding(horizontal = VaultkSpacing.normal)
                .padding(bottom = VaultkSpacing.normal),
                verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) { content() }
        }
    }
}

@Composable
private fun FormPropertyRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null
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
            if (trailingAction != null) {
                trailingAction()
            }
        }
    }
}
