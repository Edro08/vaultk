package com.edro08.vaultk.ui.screen.storage

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edro08.vaultk.R
import com.edro08.vaultk.application.ManageCloudAccount
import com.edro08.vaultk.application.ManageCloudCopies
import com.edro08.vaultk.domain.CloudAccount
import com.edro08.vaultk.domain.CloudCopy
import com.edro08.vaultk.domain.CloudDownloadFailure
import com.edro08.vaultk.domain.CloudDownloadIssue
import com.edro08.vaultk.domain.CloudDriveItem
import com.edro08.vaultk.ui.component.VaultkBottomNavigation
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.component.VaultkDestination
import com.edro08.vaultk.ui.theme.VaultkSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneDriveScreen(
    session: ManageCloudAccount<Activity>,
    copies: ManageCloudCopies,
    host: Activity,
    onBack: () -> Unit,
    onCopies: () -> Unit,
    onSelectCopy: (CloudCopy) -> Unit,
    destination: VaultkDestination? = null,
    onDestinationChange: ((VaultkDestination) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf<CloudAccount?>(null) }
    var loadingAccount by remember { mutableStateOf(true) }
    var accountError by remember { mutableStateOf(false) }
    var connecting by remember { mutableStateOf(false) }

    var path by remember { mutableStateOf<List<CloudDriveItem>>(emptyList()) }
    var entries by remember { mutableStateOf<List<CloudDriveItem>>(emptyList()) }
    var loadingEntries by remember { mutableStateOf(false) }
    var failedEntries by remember { mutableStateOf(false) }
    var refreshEntries by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var localCopies by remember { mutableStateOf<List<CloudCopy>>(emptyList()) }
    var selectedFileForDetails by remember { mutableStateOf<CloudDriveItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var downloading by remember { mutableStateOf<CloudDriveItem?>(null) }
    var downloadedItemName by remember { mutableStateOf<String?>(null) }
    var downloadFailed by remember { mutableStateOf(false) }
    var downloadIssue by remember { mutableStateOf<CloudDownloadIssue?>(null) }
    var transferred by remember { mutableStateOf(0L) }
    var total by remember { mutableStateOf<Long?>(null) }

    var menuOpen by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    var checkingDrive by remember { mutableStateOf(false) }
    var driveCheckResult by remember { mutableStateOf<Boolean?>(null) }

    fun refreshLocalCopies() {
        scope.launch {
            val list = withContext(Dispatchers.IO) { runCatching { copies.list() }.getOrDefault(emptyList()) }
            localCopies = list
        }
    }

    fun loadAccount() {
        loadingAccount = true
        accountError = false
        session.current { result ->
            result.onSuccess { account = it }.onFailure { accountError = true }
            loadingAccount = false
            refreshLocalCopies()
        }
    }

    LaunchedEffect(session) {
        loadAccount()
    }

    LaunchedEffect(account, path, refreshEntries) {
        if (account != null) {
            val currentPath = path
            loadingEntries = true
            failedEntries = false
            entries = emptyList()
            session.listDrive(path.lastOrNull()) { result ->
                if (currentPath != path) return@listDrive
                result.onSuccess { entries = it }.onFailure { failedEntries = true }
                loadingEntries = false
                refreshLocalCopies()
            }
        }
    }

    val handleBack: () -> Unit = {
        when {
            selectedFileForDetails != null && downloading == null -> selectedFileForDetails = null
            path.isNotEmpty() && downloading == null -> path = path.dropLast(1)
            downloading == null -> onBack()
        }
    }

    BackHandler(enabled = true) { handleBack() }

    val formatFileSize: (Long) -> String = { bytes ->
        when {
            bytes >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_000_000f)
            bytes >= 1_000 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1_000f)
            else -> "$bytes B"
        }
    }

    val formatDate: (Long) -> String = { timestamp ->
        val sdf = SimpleDateFormat("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es"))
        sdf.format(Date(timestamp))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.small)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = handleBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(Modifier.width(VaultkSpacing.tiny))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.open_onedrive),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (account != null) {
                            Text(
                                text = stringResource(R.string.cloud_account_connected_status),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (account != null) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.content_more),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.cloud_menu_copies)) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        onCopies()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.cloud_drive_check)) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        checkingDrive = true
                                        driveCheckResult = null
                                        session.checkDriveAccess { res ->
                                            driveCheckResult = res.isSuccess
                                            checkingDrive = false
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.cloud_account_disconnect)) },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        confirmDisconnect = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (destination != null && onDestinationChange != null) {
                VaultkBottomNavigation(selected = destination, onSelect = onDestinationChange)
            }
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = VaultkSpacing.normal)
        ) {
            if (loadingAccount) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (account == null) {
                // DISCONNECTED STATE (Mockup 1)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = VaultkSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.large)
                ) {
                    Text(
                        text = stringResource(R.string.cloud_account_connect_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = VaultkSpacing.large, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Cloud,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(VaultkSpacing.large))

                            Text(
                                text = stringResource(R.string.cloud_account_card_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(Modifier.height(VaultkSpacing.small))

                            Text(
                                text = stringResource(R.string.cloud_account_card_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(VaultkSpacing.extraLarge))

                            Button(
                                onClick = {
                                    connecting = true
                                    accountError = false
                                    session.connect(host) { result ->
                                        result.onSuccess {
                                            if (it != null) account = it
                                        }.onFailure {
                                            accountError = true
                                        }
                                        connecting = false
                                    }
                                },
                                enabled = !connecting,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (connecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(VaultkSpacing.small))
                                }
                                Text(
                                    text = stringResource(R.string.cloud_account_connect),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (accountError) {
                        Text(
                            text = stringResource(R.string.cloud_account_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // CONNECTED STATE (Mockup 2)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = VaultkSpacing.tiny),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.cloud_browser_search_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    // Breadcrumbs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VaultkSpacing.tiny, vertical = VaultkSpacing.tiny),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.cloud_browser_root_folder),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (path.isEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (path.isEmpty()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                if (path.isNotEmpty() && downloading == null) path = emptyList()
                            }
                        )
                        path.forEachIndexed { index, segment ->
                            Text(
                                text = " › ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val isLast = index == path.lastIndex
                            Text(
                                text = segment.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isLast) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable(enabled = !isLast && downloading == null) {
                                    path = path.take(index + 1)
                                }
                            )
                        }
                    }

                    // Content list or progress
                    val filteredEntries = entries.filter {
                        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when {
                            loadingEntries -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            failedEntries -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        stringResource(R.string.cloud_browser_error),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(VaultkSpacing.small))
                                    Button(onClick = { refreshEntries++ }) {
                                        Text(stringResource(R.string.cloud_account_retry))
                                    }
                                }
                            }
                            entries.isEmpty() -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        stringResource(R.string.cloud_browser_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            filteredEntries.isEmpty() -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        stringResource(R.string.open_local_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
                                ) {
                                    items(filteredEntries, key = { "${it.driveId}:${it.itemId}" }) { item ->
                                        val isDownloaded = localCopies.any {
                                            it.id.driveId == item.driveId && it.id.itemId == item.itemId
                                        }

                                        VaultkCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = downloading == null) {
                                                    if (item.folder) {
                                                        path = path + item
                                                    } else {
                                                        selectedFileForDetails = item
                                                        downloadFailed = false
                                                        downloadIssue = null
                                                        downloadedItemName = null
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(VaultkSpacing.medium),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                                    modifier = Modifier.size(42.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = if (item.folder) Icons.Outlined.Folder else Icons.Outlined.Lock,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (item.folder) {
                                                            stringResource(R.string.cloud_browser_folder_label)
                                                        } else {
                                                            val sizeStr = formatFileSize(item.size ?: 0L)
                                                            val statusStr = if (isDownloaded) {
                                                                stringResource(R.string.cloud_browser_downloaded_status)
                                                            } else {
                                                                stringResource(R.string.cloud_browser_available_status)
                                                            }
                                                            "$sizeStr · $statusStr"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // FILE DETAILS BOTTOM SHEET (Mockup 3)
    if (selectedFileForDetails != null) {
        val file = selectedFileForDetails!!
        val matchingCopy = localCopies.firstOrNull {
            it.id.driveId == file.driveId && it.id.itemId == file.itemId
        }
        val isDownloaded = matchingCopy != null

        ModalBottomSheet(
            onDismissRequest = {
                if (downloading == null) selectedFileForDetails = null
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = Color(0xFF141422)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VaultkSpacing.large)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(VaultkSpacing.small))
                    Text(
                        text = stringResource(R.string.cloud_details_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            if (downloading == null) selectedFileForDetails = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.cloud_details_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Table of details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
                ) {
                    // Archivo
                    DetailRow(
                        label = stringResource(R.string.cloud_details_file),
                        content = {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )

                    // Estado
                    DetailRow(
                        label = stringResource(R.string.cloud_details_status),
                        content = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
                            ) {
                                if (isDownloaded) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF44DB98),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.cloud_details_status_downloaded),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF44DB98)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Cloud,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.cloud_details_status_remote),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )

                    // Tamaño
                    DetailRow(
                        label = stringResource(R.string.cloud_details_size),
                        content = {
                            Text(
                                text = formatFileSize(file.size ?: 0L),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )

                    // Ubicación
                    val pathSegments = if (path.isEmpty()) "" else " › " + path.joinToString(" › ") { it.name }
                    DetailRow(
                        label = stringResource(R.string.cloud_details_location),
                        content = {
                            Text(
                                text = "OneDrive › ${stringResource(R.string.cloud_browser_root_folder)}$pathSegments",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )

                    // Última mod.
                    DetailRow(
                        label = stringResource(R.string.cloud_details_last_modified),
                        content = {
                            Text(
                                text = formatDate(matchingCopy?.savedAt ?: System.currentTimeMillis()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )

                    // Tipo
                    DetailRow(
                        label = stringResource(R.string.cloud_details_type),
                        content = {
                            Text(
                                text = stringResource(R.string.cloud_details_type_value),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )
                }

                Spacer(Modifier.height(VaultkSpacing.tiny))

                // Explanatory note
                Text(
                    text = stringResource(
                        if (isDownloaded) R.string.cloud_details_explanation_downloaded
                        else R.string.cloud_details_explanation_pending
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )

                // Download Progress / Status
                if (downloading != null) {
                    val totalBytes = total ?: 0L
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
                    ) {
                        if (totalBytes > 0L) {
                            LinearProgressIndicator(
                                progress = { (transferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = stringResource(R.string.cloud_browser_downloading, transferred, totalBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (downloadFailed) {
                    Text(
                        text = stringResource(
                            when (downloadIssue) {
                                CloudDownloadIssue.Metadata -> R.string.cloud_download_metadata_error
                                CloudDownloadIssue.Redirect -> R.string.cloud_download_redirect_error
                                CloudDownloadIssue.Host -> R.string.cloud_download_host_error
                                CloudDownloadIssue.Transfer -> R.string.cloud_download_transfer_error
                                CloudDownloadIssue.Size -> R.string.cloud_download_size_error
                                CloudDownloadIssue.Signature -> R.string.cloud_download_signature_error
                                CloudDownloadIssue.Version -> R.string.cloud_download_version_error
                                CloudDownloadIssue.Storage -> R.string.cloud_download_storage_error
                                CloudDownloadIssue.Protected -> R.string.cloud_download_protected_error
                                null -> R.string.cloud_browser_download_error
                            }
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (downloadedItemName != null) {
                    Text(
                        text = stringResource(R.string.cloud_details_downloaded_success),
                        color = Color(0xFF44DB98),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(VaultkSpacing.small))

                // Bottom action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { selectedFileForDetails = null },
                        enabled = downloading == null,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = stringResource(R.string.cloud_details_close),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (isDownloaded) {
                        Button(
                            onClick = {
                                selectedFileForDetails = null
                                matchingCopy?.let { onSelectCopy(it) }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.cloud_details_open),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                downloading = file
                                downloadedItemName = null
                                downloadFailed = false
                                downloadIssue = null
                                transferred = 0
                                total = file.size
                                session.download(file, { count, size ->
                                    transferred = count
                                    total = size
                                }) { result ->
                                    result.onSuccess {
                                        downloadedItemName = file.name
                                        refreshLocalCopies()
                                    }.onFailure {
                                        downloadFailed = true
                                        downloadIssue = (it as? CloudDownloadFailure)?.issue
                                    }
                                    downloading = null
                                }
                            },
                            enabled = downloading == null,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.cloud_details_download),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Disconnect Dialog
    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text(stringResource(R.string.cloud_account_disconnect_title)) },
            text = { Text(stringResource(R.string.cloud_account_disconnect_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        session.disconnect {
                            account = null
                            path = emptyList()
                            entries = emptyList()
                            confirmDisconnect = false
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.cloud_account_disconnect),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = false }) {
                    Text(stringResource(R.string.groups_cancel))
                }
            }
        )
    }

    // Drive Check Dialog
    if (checkingDrive || driveCheckResult != null) {
        AlertDialog(
            onDismissRequest = { if (!checkingDrive) driveCheckResult = null },
            title = { Text(stringResource(R.string.cloud_drive_check)) },
            text = {
                if (checkingDrive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.cloud_drive_checking))
                    }
                } else when (driveCheckResult) {
                    true -> Text(stringResource(R.string.cloud_drive_access_ok))
                    false -> Text(
                        stringResource(R.string.cloud_drive_access_error),
                        color = MaterialTheme.colorScheme.error
                    )
                    null -> Unit
                }
            },
            confirmButton = {
                if (!checkingDrive) {
                    TextButton(onClick = { driveCheckResult = null }) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}
