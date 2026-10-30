package com.edro08.vaultk

import android.os.Bundle
import android.os.SystemClock
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.graphics.Color
import com.edro08.vaultk.domain.settings.VaultDocument
import com.edro08.vaultk.domain.cloud.CloudCopy
import com.edro08.vaultk.domain.cloud.biometricReference
import com.edro08.vaultk.ui.uri
import com.edro08.vaultk.app.VaultkGraph
import com.edro08.vaultk.ui.LocalCopyCredential
import androidx.compose.runtime.CompositionLocalProvider
import com.edro08.vaultk.application.security.VaultIdleTimer
import com.edro08.vaultk.domain.settings.Appearance
import com.edro08.vaultk.domain.settings.FileAccessMode
import com.edro08.vaultk.domain.vault.VaultConflictException
import com.edro08.vaultk.domain.vault.InvalidVaultFileException
import com.edro08.vaultk.domain.vault.InvalidVaultKeyException
import com.edro08.vaultk.ui.component.VaultkDestination
import com.edro08.vaultk.ui.screen.generator.PasswordGeneratorScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.edro08.vaultk.domain.vault.VaultContent
import com.edro08.vaultk.domain.vault.VaultId
import com.edro08.vaultk.ui.screen.storage.OpenVaultScreen
import com.edro08.vaultk.ui.screen.storage.OneDriveScreen
import com.edro08.vaultk.ui.screen.storage.CloudCopiesScreen
import com.edro08.vaultk.ui.screen.storage.LocalVaultPickerScreen
import com.edro08.vaultk.ui.screen.settings.LockTimeoutDialog
import com.edro08.vaultk.ui.screen.storage.SelectedVault
import com.edro08.vaultk.ui.screen.settings.SettingsScreen
import com.edro08.vaultk.ui.screen.storage.UnlockVaultScreen
import com.edro08.vaultk.ui.screen.vault.VaultContentScreen
import com.edro08.vaultk.ui.theme.VaultkTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.FileNotFoundException
import java.io.File
import java.text.DateFormat
import java.util.Date
import javax.crypto.Cipher

class MainActivity : FragmentActivity() {
    private val vaultIdleTimer = VaultIdleTimer()

    override fun onUserInteraction() {
        super.onUserInteraction()
        vaultIdleTimer.interact(SystemClock.elapsedRealtime())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setRecentsScreenshotEnabled(false)
        setContent {
            val graph = remember { VaultkGraph(applicationContext) }
            val store = graph.recentVaults
            val session = graph.sessionSecurity
            var timeoutSeconds by remember { mutableStateOf(session.timeoutSeconds) }
            var clipboardSeconds by remember { mutableStateOf(session.clipboardSeconds) }
            val settings = graph.preferences
            val localFiles = graph.browseLocalVaults
            var fileAccessMode by remember { mutableStateOf(settings.fileAccessMode) }
            var fullAccessGranted by remember { mutableStateOf(localFiles.permitted()) }
            var activateFullAccessOnReturn by remember { mutableStateOf(false) }
            var appearance by remember { mutableStateOf(settings.appearance) }
            var searchNotes by remember { mutableStateOf(settings.searchNotes) }
            var showDetails by remember { mutableStateOf(settings.showDetails) }
            var cloudAutoSync by remember { mutableStateOf(settings.cloudAutoSync) }
            var cloudAllowMetered by remember { mutableStateOf(settings.cloudAllowMetered) }
            val isMeteredNetwork: () -> Boolean = {
                getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered == true
            }
            var selectorDestination by rememberSaveable { mutableStateOf(VaultkDestination.Vault) }
            val biometricKeys = graph.biometricUnlock
            val openLocalVault = graph.openLocalVault
            val saveLocalVault = graph.saveLocalVault
            val scope = rememberCoroutineScope()
            var selected by remember { mutableStateOf(store.current()) }
            var cloudSelected by remember { mutableStateOf(graph.cloudCopies.selected()) }
            var recent by remember { mutableStateOf(store.recent()) }
            val activeReference = cloudSelected?.id?.biometricReference() ?: selected?.uri?.toString()
            var biometricEnabled by remember(activeReference) {
                mutableStateOf(activeReference?.let { biometricKeys.enabled(it) } == true)
            }
            val forgetReference: (Uri) -> Unit = { uri ->
                runCatching { biometricKeys.remove(uri.toString()) }
                if (uri.scheme == "content") runCatching {
                    contentResolver.persistedUriPermissions.firstOrNull { it.uri == uri }?.let { grant ->
                        val flags = (if (grant.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                            (if (grant.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
                        if (flags != 0) contentResolver.releasePersistableUriPermission(uri, flags)
                    }
                }
            }
            var opened by remember { mutableStateOf<VaultContent?>(null) }
            var opening by remember { mutableStateOf(false) }
            var openingStartedAtMillis by remember { mutableStateOf(0L) }
            var unlockError by remember { mutableStateOf<String?>(null) }
            var lockedTimeoutDialogOpen by remember { mutableStateOf(false) }
            var openJob by remember { mutableStateOf<Job?>(null) }
            var showSelector by rememberSaveable { mutableStateOf(false) }
            var localPickerOpen by rememberSaveable { mutableStateOf(false) }
            var cloudAccountOpen by rememberSaveable { mutableStateOf(false) }
            var cloudBrowserOpen by rememberSaveable { mutableStateOf(false) }
            var cloudCopiesOpen by rememberSaveable { mutableStateOf(false) }
            var pickerError by remember { mutableStateOf<String?>(null) }
            var unavailableRecent by remember { mutableStateOf<VaultDocument?>(null) }
            var pendingRelinkUri by rememberSaveable { mutableStateOf<String?>(null) }
            var sessionKey by remember { mutableStateOf<CharArray?>(null) }
            var baseline by remember { mutableStateOf<ByteArray?>(null) }
            var externalFormPickerActive by remember { mutableStateOf(false) }
            var saveJob by remember { mutableStateOf<Job?>(null) }
            val lockVault: () -> Unit = {
                vaultIdleTimer.stop()
                openJob?.cancel()
                saveJob?.cancel()
                sessionKey?.fill('\u0000')
                sessionKey = null
                baseline?.fill(0)
                baseline = null
                opened = null
                externalFormPickerActive = false
                graph.copyCredential.clearOwned()
            }
            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        fullAccessGranted = localFiles.permitted()
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            LaunchedEffect(externalFormPickerActive) {
                if (externalFormPickerActive) {
                    delay(60_000)
                    if (externalFormPickerActive && !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                        lockVault()
                    else externalFormPickerActive = false
                }
            }
            LaunchedEffect(opened, timeoutSeconds) {
                if (opened != null) {
                    while (true) {
                        delay(1000)
                        if (vaultIdleTimer.hasTimedOut(SystemClock.elapsedRealtime(), timeoutSeconds)) {
                            lockVault()
                            break
                        }
                    }
                }
            }
            DisposableEffect(opened) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP && opened != null &&
                        timeoutSeconds == 0 && !externalFormPickerActive) lockVault()
                    if (event == Lifecycle.Event.ON_RESUME && opened != null && !opening &&
                        vaultIdleTimer.hasTimedOut(SystemClock.elapsedRealtime(), timeoutSeconds)) lockVault()
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            val openVault: (VaultDocument, CharArray, Boolean) -> Unit = { document, key, fromBiometric ->
                openingStartedAtMillis = SystemClock.elapsedRealtime()
                opening = true
                unlockError = null
                openJob = scope.launch {
                    try {
                        if (document.uri.scheme != "file") throw SecurityException()
                        val (content, hash) = openLocalVault(VaultId(requireNotNull(document.uri.path)), key)
                        if (selected?.uri == document.uri) {
                            sessionKey?.fill('\u0000')
                            sessionKey = key.copyOf()
                            baseline = hash
                            selected = store.markOpened(document)
                            recent = store.recent()
                            vaultIdleTimer.start(SystemClock.elapsedRealtime())
                            opened = content
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: InvalidVaultKeyException) {
                        if (fromBiometric) {
                            runCatching { biometricKeys.remove(document.uri.toString()) }
                            biometricEnabled = false
                        }
                        unlockError = getString(R.string.unlock_error_key)
                    } catch (error: InvalidVaultFileException) {
                        unlockError = getString(R.string.unlock_error_file)
                    } catch (error: FileNotFoundException) {
                        unlockError = getString(R.string.unlock_error_access)
                    } catch (error: SecurityException) {
                        unlockError = getString(R.string.unlock_error_access)
                    } catch (error: Exception) {
                        unlockError = getString(R.string.unlock_error_file)
                    } finally {
                        key.fill('\u0000')
                        opening = false
                    }
                }
            }
            val openCloudVault: (CloudCopy, CharArray, Boolean) -> Unit = { copy, key, fromBiometric ->
                openingStartedAtMillis = SystemClock.elapsedRealtime()
                opening = true
                unlockError = null
                openJob = scope.launch {
                    try {
                        val (content, hash) = graph.cloudCopies.openVersioned(copy.id, key)
                        if (cloudSelected?.id == copy.id) {
                            sessionKey?.fill('\u0000')
                            sessionKey = key.copyOf()
                            baseline?.fill(0)
                            baseline = hash
                            cloudSelected = graph.cloudCopies.selected() ?: copy
                            vaultIdleTimer.start(SystemClock.elapsedRealtime())
                            opened = content
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: InvalidVaultKeyException) {
                        if (fromBiometric) {
                            runCatching { biometricKeys.remove(copy.id.biometricReference()) }
                            biometricEnabled = false
                        }
                        unlockError = getString(R.string.unlock_error_key)
                    } catch (_: Exception) {
                        unlockError = getString(R.string.unlock_error_file)
                    } finally {
                        key.fill('\u0000')
                        opening = false
                    }
                }
            }
            BackHandler(showSelector) {
                if (cloudCopiesOpen) cloudCopiesOpen = false
                else if (cloudBrowserOpen) cloudBrowserOpen = false
                else if (cloudAccountOpen) cloudAccountOpen = false
                else if (localPickerOpen) localPickerOpen = false else showSelector = false
                pendingRelinkUri = null
            }
            BackHandler(opened != null && !showSelector) { lockVault() }
            if (opened != null) {
                DisposableEffect(Unit) {
                    val alreadySecure = window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    onDispose { if (!alreadySecure) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
                }
            }
            val accessSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                fullAccessGranted = localFiles.permitted()
                val activate = activateFullAccessOnReturn
                activateFullAccessOnReturn = false
                if (activate || !fullAccessGranted && fileAccessMode == FileAccessMode.Full) {
                    fileAccessMode = if (fullAccessGranted) FileAccessMode.Full else FileAccessMode.Standard
                    settings.fileAccessMode = fileAccessMode
                }
                if (activate && fullAccessGranted && pendingRelinkUri != null && showSelector)
                    localPickerOpen = true
                if (activate && !fullAccessGranted) {
                    pendingRelinkUri = null
                    Toast.makeText(this@MainActivity,
                        getString(R.string.settings_access_denied), Toast.LENGTH_LONG).show()
                }
            }
            val manageFileAccess: (Boolean) -> Unit = { activate ->
                val target = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName"))
                try {
                    accessSettings.launch(target)
                    activateFullAccessOnReturn = activate
                    lockVault()
                } catch (_: ActivityNotFoundException) {
                    try {
                        accessSettings.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        activateFullAccessOnReturn = activate
                        lockVault()
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(this@MainActivity,
                            getString(R.string.settings_access_settings_unavailable), Toast.LENGTH_LONG).show()
                    }
                }
            }
            val requestFullAccess: () -> Unit = {
                if (localFiles.permitted()) {
                    settings.fileAccessMode = FileAccessMode.Full
                    fileAccessMode = FileAccessMode.Full
                    fullAccessGranted = true
                    if (showSelector) localPickerOpen = true
                } else manageFileAccess(true)
            }
            val darkTheme = when (appearance) {
                Appearance.Dark -> true
                Appearance.Light -> false
                Appearance.System -> isSystemInDarkTheme()
            }
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) SystemBarStyle.dark(Color.TRANSPARENT)
                        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (darkTheme) SystemBarStyle.dark(Color.TRANSPARENT)
                        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
            }
            CompositionLocalProvider(LocalCopyCredential provides graph.copyCredential) {
            VaultkTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
                if (showSelector) {
                    if (selectorDestination == VaultkDestination.Settings) {
                        SettingsScreen(
                            appearance = appearance,
                            onAppearanceChange = { settings.appearance = it; appearance = it },
                            biometricEnabled = biometricEnabled,
                            onBiometricClick = null,
                            timeoutSeconds = timeoutSeconds,
                            onTimeoutClick = { lockedTimeoutDialogOpen = true },
                            clipboardSeconds = clipboardSeconds,
                            onClipboardChange = { session.clipboardSeconds = it; clipboardSeconds = it },
                            searchNotes = searchNotes,
                            onSearchNotesChange = { settings.searchNotes = it; searchNotes = it },
                            showDetails = showDetails,
                            onShowDetailsChange = { settings.showDetails = it; showDetails = it },
                            modifier = Modifier.fillMaxSize(),
                            fileAccessGranted = fullAccessGranted,
                            onManageFileAccess = { manageFileAccess(false) },
                            cloudAutoSync = cloudAutoSync,
                            onCloudAutoSyncChange = { settings.cloudAutoSync = it; cloudAutoSync = it },
                            cloudAllowMetered = cloudAllowMetered,
                            onCloudAllowMeteredChange = { settings.cloudAllowMetered = it; cloudAllowMetered = it }
                        )
                    } else if (selectorDestination == VaultkDestination.Generator) {
                        PasswordGeneratorScreen(Modifier.fillMaxSize())
                    } else if (cloudCopiesOpen) CloudCopiesScreen(graph.cloudCopies,
                        selected = cloudSelected, onBack = { cloudCopiesOpen = false }, onSelect = { copy ->
                            graph.cloudCopies.select(copy.id)
                            cloudSelected = copy
                            lockVault()
                            cloudCopiesOpen = false
                            cloudAccountOpen = false
                            showSelector = false
                        }, onDeselect = {
                            lockVault()
                            graph.cloudCopies.select(null)
                            cloudSelected = null
                        }, synchronize = graph.synchronizeCloudCopy,
                        destination = selectorDestination,
                        onDestinationChange = { selectorDestination = it })
                    else if (cloudAccountOpen || cloudBrowserOpen) OneDriveScreen(
                        session = graph.cloudAccount,
                        copies = graph.cloudCopies,
                        host = this@MainActivity,
                        onBack = {
                            cloudAccountOpen = false
                            cloudBrowserOpen = false
                        },
                        onCopies = { cloudCopiesOpen = true },
                        onSelectCopy = { copy ->
                            cloudSelected = copy
                            graph.cloudCopies.select(copy.id)
                            selected = null
                            opened = null
                            unlockError = null
                            cloudCopiesOpen = false
                            cloudAccountOpen = false
                            cloudBrowserOpen = false
                            showSelector = false
                        },
                        destination = selectorDestination,
                        onDestinationChange = { selectorDestination = it })
                    else if (localPickerOpen) LocalVaultPickerScreen(graph.browseLocalVaults,
                         onBack = { pendingRelinkUri = null; localPickerOpen = false }, onSelect = { id ->
                             val file = File(id.value)
                            val uri = Uri.fromFile(file)
                             if (!localFiles.canRead(id)) {
                                pendingRelinkUri = null
                                pickerError = getString(R.string.open_error_unreadable)
                                localPickerOpen = false
                            } else {
                                val document = VaultDocument(uri.toString(), file.name, System.currentTimeMillis())
                                val relink = recent.firstOrNull { it.uri.toString() == pendingRelinkUri }
                                pendingRelinkUri = null
                                selected = if (relink != null) {
                                    store.relocate(relink.reference, document).also {
                                        if (relink.uri != uri) forgetReference(relink.uri)
                                    }
                                } else { store.select(document); document }
                                graph.cloudCopies.select(null)
                                cloudSelected = null
                                recent = store.recent()
                                opened = null
                                unlockError = null
                                pickerError = null
                                localPickerOpen = false
                                showSelector = false
                            }
                        }) else {
                        var persistentCloudCopies by remember { mutableStateOf<List<CloudCopy>>(emptyList()) }
                        var isOneDriveConnected by remember { mutableStateOf<Boolean?>(null) }
                        LaunchedEffect(cloudAccountOpen) {
                            persistentCloudCopies = withContext(Dispatchers.IO) {
                                runCatching { graph.cloudCopies.list() }.getOrDefault(emptyList())
                            }
                            graph.cloudAccount.current { result ->
                                isOneDriveConnected = result.getOrNull() != null
                            }
                        }
                        OpenVaultScreen(
                            recent = recent,
                            currentUri = selected?.uri,
                            unavailableRecent = unavailableRecent,
                            onUnavailableDismiss = { unavailableRecent = null },
                            onRemoveRecent = { document ->
                                store.remove(document.reference)
                                recent = store.recent()
                                if (selected?.uri == document.uri) {
                                    openJob?.cancel()
                                    selected = null
                                    unlockError = null
                                }
                                forgetReference(document.uri)
                            },
                            onRelocateRecent = { document ->
                                pendingRelinkUri = document.uri.toString()
                                unavailableRecent = null
                                if (localFiles.permitted()) localPickerOpen = true else requestFullAccess()
                            },
                            onBack = { pendingRelinkUri = null; showSelector = false },
                            fileAccessMode = fileAccessMode,
                            onDevice = {
                                pendingRelinkUri = null
                                if (localFiles.permitted()) localPickerOpen = true else requestFullAccess()
                            },
                            onOneDrive = { cloudAccountOpen = true },
                            onCloudCopies = { cloudCopiesOpen = true },
                            oneDriveConnected = isOneDriveConnected,
                            cloudCopies = persistentCloudCopies,
                            onCloudCopyClick = { copy ->
                                graph.cloudCopies.select(copy.id)
                                cloudSelected = copy
                                lockVault()
                                showSelector = false
                            },
                            destination = selectorDestination,
                            onDestinationChange = { selectorDestination = it },
                            onRecent = { document ->
                                val currentName = if (document.uri.scheme == "file" && store.hasReadPermission(document.reference))
                                    document.uri.path?.let { File(it).name } else null
                                if (currentName?.endsWith(".kdbx", ignoreCase = true) == true) {
                                    graph.cloudCopies.select(null)
                                    cloudSelected = null
                                    val refreshed = document.copy(name = currentName, selectedAt = System.currentTimeMillis())
                                    store.select(refreshed)
                                    selected = store.current()
                                    recent = store.recent()
                                    opened = null
                                    unlockError = null
                                    showSelector = false
                                } else {
                                    unavailableRecent = document
                                }
                            },
                            errorMessage = pickerError,
                            onErrorShown = { pickerError = null },
                        )
                    }
                } else if (opened != null) {
                    val isCloud = cloudSelected != null
                    VaultContentScreen(
                        content = opened!!,
                        fallbackName = cloudSelected?.name ?: selected?.name.orEmpty(),
                        onClose = lockVault,
                        editVault = graph.editVault,
                        onLock = lockVault,
                        timeoutSeconds = timeoutSeconds,
                        onTimeoutChange = {
                            session.timeoutSeconds = it
                            timeoutSeconds = it
                            vaultIdleTimer.start(SystemClock.elapsedRealtime())
                        },
                        appearance = appearance,
                        onAppearanceChange = { settings.appearance = it; appearance = it },
                        clipboardSeconds = clipboardSeconds,
                        onClipboardChange = { session.clipboardSeconds = it; clipboardSeconds = it },
                        searchNotes = searchNotes,
                        onSearchNotesChange = { settings.searchNotes = it; searchNotes = it },
                        showDetails = showDetails,
                        onShowDetailsChange = { settings.showDetails = it; showDetails = it },
                        onChangeVault = { lockVault(); showSelector = true },
                        onManageFileAccess = { manageFileAccess(false) },
                        fileAccessMode = fileAccessMode,
                        fileAccessGranted = fullAccessGranted,
                        onFileAccessChange = { mode ->
                            if (mode == FileAccessMode.Standard || localFiles.permitted()) {
                                settings.fileAccessMode = mode
                                fileAccessMode = mode
                                fullAccessGranted = localFiles.permitted()
                            } else requestFullAccess()
                        },
                        cloudAutoSync = cloudAutoSync,
                        onCloudAutoSyncChange = { settings.cloudAutoSync = it; cloudAutoSync = it },
                        cloudAllowMetered = cloudAllowMetered,
                        onCloudAllowMeteredChange = { settings.cloudAllowMetered = it; cloudAllowMetered = it },
                        onExternalFormPickerChange = { externalFormPickerActive = it },
                        isCloudVault = isCloud,
                        copy = cloudSelected,
                        synchronize = if (isCloud) graph.synchronizeCloudCopy else null,
                        isMeteredNetwork = isMeteredNetwork,
                        onCopyUpdated = { cloudSelected = graph.cloudCopies.selected() },
                        onRemoteDownloaded = {
                            val updated = graph.cloudCopies.selected()
                            cloudSelected = updated
                            val key = sessionKey?.copyOf()
                            if (updated != null && key != null) {
                                scope.launch {
                                    try {
                                        val (reloaded, hash) = graph.cloudCopies.openVersioned(updated.id, key)
                                        if (cloudSelected?.id == updated.id && opened != null) {
                                            baseline?.fill(0)
                                            baseline = hash
                                            opened = reloaded
                                        }
                                    } catch (_: Exception) {
                                        lockVault()
                                    } finally {
                                        key.fill('\u0000')
                                    }
                                }
                            } else {
                                key?.fill('\u0000')
                            }
                        },
                        onViewStatus = if (isCloud) {
                            {
                                lockVault()
                                showSelector = true
                                cloudCopiesOpen = true
                            }
                        } else null,
                        onUnlink = if (isCloud) {
                            {
                                lockVault()
                                graph.cloudCopies.select(null)
                                cloudSelected = null
                            }
                        } else null,
                        onCloudSave = if (isCloud) {
                            { changed, complete ->
                                val copy = cloudSelected
                                val original = opened
                                val key = sessionKey?.copyOf()
                                val hash = baseline?.copyOf()
                                if (copy == null || original == null || key == null || hash == null) {
                                    key?.fill('\u0000')
                                    hash?.fill(0)
                                    complete(getString(R.string.save_error_write))
                                } else {
                                    saveJob = scope.launch {
                                        try {
                                            val (saved, fingerprint) = graph.saveCloudCopyLocally(
                                                copy.id, hash, key, original, changed)
                                            baseline?.fill(0)
                                            baseline = fingerprint
                                            opened = saved
                                            cloudSelected = graph.cloudCopies.selected()
                                            complete(null)
                                        } catch (_: InvalidVaultKeyException) {
                                            complete(getString(R.string.unlock_error_key))
                                        } catch (_: VaultConflictException) {
                                            complete(getString(R.string.save_error_conflict))
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Exception) {
                                            complete(getString(R.string.save_error_write))
                                        } finally {
                                            key.fill('\u0000')
                                            hash.fill(0)
                                        }
                                    }
                                }
                            }
                        } else null,
                        onSave = if (!isCloud) {
                            { changed, progress, complete ->
                                val source = selected
                                val original = opened
                                val key = sessionKey?.copyOf()
                                val hash = baseline?.copyOf()
                                if (source?.uri?.scheme != "file" || !localFiles.canRead(VaultId(requireNotNull(source.uri.path))) ||
                                    original == null || key == null || hash == null) {
                                    key?.fill('\u0000')
                                    complete(getString(R.string.save_error_write))
                                } else {
                                    saveJob = scope.launch {
                                        try {
                                            val (saved, fingerprint) = saveLocalVault(
                                                VaultId(requireNotNull(source.uri.path)), hash, key, original, changed
                                            ) { update -> runOnUiThread { progress(update) } }
                                            baseline?.fill(0)
                                            baseline = fingerprint
                                            opened = saved
                                            complete(null)
                                        } catch (_: InvalidVaultKeyException) {
                                            complete(getString(R.string.unlock_error_key))
                                        } catch (_: VaultConflictException) {
                                            complete(getString(R.string.save_error_conflict))
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Exception) {
                                            complete(getString(R.string.save_error_write))
                                        } finally {
                                            key.fill('\u0000')
                                            hash.fill(0)
                                        }
                                    }
                                }
                            }
                        } else null,
                        biometricEnabled = biometricEnabled,
                        onEnableBiometric = if (isCloud) {
                            { key, complete ->
                                val copy = cloudSelected
                                if (copy == null || BiometricManager.from(this@MainActivity)
                                        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
                                    key.fill('\u0000')
                                    complete(getString(R.string.unlock_biometric_unavailable))
                                } else scope.launch {
                                    try {
                                        graph.cloudCopies.open(copy.id, key)
                                        val reference = copy.id.biometricReference()
                                        val cipher = biometricKeys.encryptionCipher(reference)
                                        promptBiometric(copy.name, cipher, { authenticated ->
                                            try {
                                                if (cloudSelected?.id != copy.id || opened == null) throw IllegalStateException()
                                                biometricKeys.store(reference, authenticated, key)
                                                biometricEnabled = true
                                                complete(null)
                                            } catch (_: Exception) {
                                                runCatching { biometricKeys.remove(reference) }
                                                complete(getString(R.string.unlock_biometric_error))
                                            } finally { key.fill('\u0000') }
                                        }, {
                                            key.fill('\u0000')
                                            complete(getString(R.string.unlock_biometric_error))
                                        })
                                    } catch (_: InvalidVaultKeyException) {
                                        key.fill('\u0000')
                                        complete(getString(R.string.unlock_error_key))
                                    } catch (_: Exception) {
                                        key.fill('\u0000')
                                        complete(getString(R.string.unlock_biometric_error))
                                    }
                                }
                            }
                        } else selected?.let { document ->
                            { key, complete ->
                                if (BiometricManager.from(this@MainActivity)
                                        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
                                    key.fill('\u0000')
                                    complete(getString(R.string.unlock_biometric_unavailable))
                                } else scope.launch {
                                    try {
                                        openLocalVault(VaultId(requireNotNull(document.uri.path)), key)
                                        val cipher = biometricKeys.encryptionCipher(document.uri.toString())
                                        promptBiometric(document.name, cipher, { authenticated ->
                                            try {
                                                if (selected?.uri != document.uri) throw IllegalStateException()
                                                biometricKeys.store(document.uri.toString(), authenticated, key)
                                                biometricEnabled = true
                                                complete(null)
                                            } catch (_: Exception) {
                                                runCatching { biometricKeys.remove(document.uri.toString()) }
                                                complete(getString(R.string.unlock_biometric_error))
                                            } finally { key.fill('\u0000') }
                                        }, {
                                            key.fill('\u0000')
                                            runCatching { biometricKeys.remove(document.uri.toString()) }
                                            complete(getString(R.string.unlock_biometric_error))
                                        })
                                    } catch (_: InvalidVaultKeyException) {
                                        key.fill('\u0000')
                                        complete(getString(R.string.unlock_error_key))
                                    } catch (_: Exception) {
                                        key.fill('\u0000')
                                        runCatching { biometricKeys.remove(document.uri.toString()) }
                                        complete(getString(R.string.unlock_biometric_error))
                                    }
                                }
                            }
                        },
                        onDisableBiometric = if (isCloud) {
                            {
                                cloudSelected?.let { runCatching { biometricKeys.remove(it.id.biometricReference()) } }
                                biometricEnabled = false
                                Toast.makeText(this@MainActivity, getString(R.string.content_biometric_disabled), Toast.LENGTH_SHORT).show()
                            }
                        } else selected?.let { document ->
                            {
                                runCatching { biometricKeys.remove(document.uri.toString()) }
                                biometricEnabled = false
                                Toast.makeText(this@MainActivity, getString(R.string.content_biometric_disabled), Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    UnlockVaultScreen(
                         vault = cloudSelected?.let {
                             SelectedVault(it.name, stringResource(R.string.cloud_origin),
                                 null, stringResource(R.string.vault_locked),
                                 biometricEnabled, it.id.biometricReference())
                         } ?: selected?.takeIf { it.uri.scheme == "file" }?.let {
                             SelectedVault(it.name, stringResource(R.string.vault_origin_local),
                                 it.openedAt?.let { time -> DateFormat.getDateTimeInstance().format(Date(time)) }
                                      ?: stringResource(R.string.vault_never_opened), stringResource(R.string.vault_locked),
                                  biometricEnabled, it.uri.toString())
                        },
                        loading = opening,
                        openingStartedAtMillis = openingStartedAtMillis,
                        errorMessage = unlockError,
                        onErrorDismiss = { unlockError = null },
                        onSelectVault = {
                            openJob?.cancel()
                            unlockError = null
                            showSelector = true
                        },
                           onUnlock = cloudSelected?.let { copy -> { key -> openCloudVault(copy, key, false) } }
                               ?: selected?.takeIf { it.uri.scheme == "file" }?.let { document -> { key -> openVault(document, key, false) } },
                            onBiometricUnlock = activeReference?.takeIf { biometricEnabled }?.let { reference -> {
                              if (!opening) {
                                  try {
                                      val cipher = biometricKeys.decryptionCipher(reference)
                                      promptBiometric(cloudSelected?.name ?: selected?.name.orEmpty(), cipher, { authenticated ->
                                          try {
                                              val key = biometricKeys.recover(reference, authenticated)
                                              val copy = cloudSelected
                                              val document = selected
                                              if (opened != null || activeReference != reference) key.fill('\u0000')
                                              else if (copy != null) openCloudVault(copy, key, true)
                                              else if (document?.uri?.scheme == "file") openVault(document, key, true)
                                              else key.fill('\u0000')
                                          } catch (_: Exception) {
                                              runCatching { biometricKeys.remove(reference) }
                                              biometricEnabled = false
                                              unlockError = getString(R.string.unlock_biometric_invalidated)
                                          }
                                      }, { unlockError = getString(R.string.unlock_biometric_error) })
                                  } catch (_: Exception) {
                                      runCatching { biometricKeys.remove(reference) }
                                     biometricEnabled = false
                                     unlockError = getString(R.string.unlock_biometric_invalidated)
                                 }
                             }
                          } },
                        settingsContent = { contentModifier ->
                            SettingsScreen(
                                appearance = appearance,
                                onAppearanceChange = { settings.appearance = it; appearance = it },
                                biometricEnabled = biometricEnabled,
                                onBiometricClick = null,
                                timeoutSeconds = timeoutSeconds,
                                onTimeoutClick = { lockedTimeoutDialogOpen = true },
                                clipboardSeconds = clipboardSeconds,
                                onClipboardChange = { session.clipboardSeconds = it; clipboardSeconds = it },
                                searchNotes = searchNotes,
                                onSearchNotesChange = { settings.searchNotes = it; searchNotes = it },
                                showDetails = showDetails,
                                onShowDetailsChange = { settings.showDetails = it; showDetails = it },
                                modifier = contentModifier,
                                fileAccessGranted = fullAccessGranted,
                                onManageFileAccess = { manageFileAccess(false) },
                                cloudAutoSync = cloudAutoSync,
                                onCloudAutoSyncChange = { settings.cloudAutoSync = it; cloudAutoSync = it },
                                cloudAllowMetered = cloudAllowMetered,
                                onCloudAllowMeteredChange = { settings.cloudAllowMetered = it; cloudAllowMetered = it },
                            )
                            if (lockedTimeoutDialogOpen) LockTimeoutDialog(timeoutSeconds,
                                { session.timeoutSeconds = it; timeoutSeconds = it }) {
                                lockedTimeoutDialogOpen = false
                            }
                        },
                    )
                }
                }
            }
            }
        }
    }

    private fun promptBiometric(name: String, cipher: Cipher, onSuccess: (Cipher) -> Unit,
        onError: () -> Unit) {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    result.cryptoObject?.cipher?.let(onSuccess) ?: onError()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onError() }
            })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_biometric_prompt_title))
            .setSubtitle(getString(R.string.unlock_biometric_prompt_subtitle, name))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(getString(R.string.groups_cancel))
            .build(), BiometricPrompt.CryptoObject(cipher))
    }

}
