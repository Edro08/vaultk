package com.edro08.vaultk

import android.content.ClipboardManager
import androidx.compose.runtime.CompositionLocalProvider
import com.edro08.vaultk.application.CopyCredential
import com.edro08.vaultk.data.AndroidCredentialClipboard
import com.edro08.vaultk.ui.LocalCopyCredential
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.edro08.vaultk.domain.Appearance
import com.edro08.vaultk.domain.FileAccessMode
import com.edro08.vaultk.domain.VaultContent
import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.domain.VaultGroup
import com.edro08.vaultk.ui.screen.vault.VaultContentScreen
import com.edro08.vaultk.ui.screen.storage.UnlockVaultScreen
import com.edro08.vaultk.ui.screen.storage.SelectedVault
import com.edro08.vaultk.ui.screen.settings.SettingsScreen
import com.edro08.vaultk.ui.theme.VaultkTheme
import org.junit.Rule
import org.junit.Test
import com.edro08.vaultk.data.KdbxVaultValueFactory
import com.edro08.vaultk.application.EditVault
import com.edro08.vaultk.ui.screen.settings.LockTimeoutDialog
import java.time.Instant

class VaultHomeTest {
    @get:Rule val compose = createComposeRule()
    private val editor = EditVault(KdbxVaultValueFactory())

    @Test fun compactSettingsKeepThemeAndFileAccessActions() {
        var appearance by mutableStateOf(Appearance.Dark)
        var access by mutableStateOf(FileAccessMode.Standard)
        var managed = false
        compose.setContent { VaultkTheme {
            SettingsScreen(appearance, onAppearanceChange = { appearance = it },
                biometricEnabled = false, onBiometricClick = null,
                timeoutSeconds = 300, onTimeoutClick = {},
                clipboardSeconds = 30, onClipboardChange = {},
                searchNotes = true, onSearchNotesChange = {},
                showDetails = true, onShowDetailsChange = {},
                onManageFileAccess = { managed = true })
        } }
        compose.onNodeWithTag("theme-Oscuro").assertIsSelected()
        compose.onNodeWithTag("theme-Claro").assertIsNotSelected().performClick()
        compose.runOnIdle { check(appearance == Appearance.Light) }
        compose.onNodeWithTag("theme-Claro").assertIsSelected()
        compose.onNodeWithTag("theme-Oscuro").assertIsNotSelected()
        compose.onNodeWithText("Acceso completo").performScrollTo().performClick()
        compose.runOnIdle { check(access == FileAccessMode.Full) }
        compose.onNodeWithText("Administrar permiso").performScrollTo().performClick()
        compose.runOnIdle { check(managed) }
    }

    @Test fun generatorIsAvailableBetweenVaultAndSettingsWhileLocked() {
        compose.setContent { VaultkTheme {
            UnlockVaultScreen(null, onSelectVault = {})
        } }
        compose.onNodeWithTag("navigation-generator").performClick()
        compose.onNodeWithText("Longitud").assertIsDisplayed()
        compose.onNodeWithTag("generator-slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(5f) }
        compose.onNodeWithTag("generator-length").assertTextEquals("64")
        compose.onNodeWithText("Generar").performClick()
        compose.onNodeWithText("Contraseña generada").assertIsDisplayed()
        compose.onNodeWithText("Usar contraseña").assertDoesNotExist()
        compose.onNodeWithTag("navigation-settings").performClick()
        compose.onNodeWithTag("navigation-vault").performClick()
    }

    @Test fun generatorTabCanUsePasswordInNewEntry() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(), emptyList()))
        compose.setContent { VaultkTheme { VaultContentScreen(content, "Personal.kdbx", {}, editVault = editor) } }
        compose.onNodeWithTag("navigation-generator").performClick()
        compose.onNodeWithText("Usar contraseña").assertIsNotEnabled()
        compose.onNodeWithText("Generar").performClick()
        compose.onNodeWithText("Segura").assertIsDisplayed()
        compose.onNodeWithText("Usar contraseña").performClick()
        compose.onNodeWithText("Nueva entrada").assertIsDisplayed()
        compose.onNodeWithText("Título").performTextInput("Cuenta generada")
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.onNodeWithText("Contraseña").assertIsDisplayed()
    }

    @Test fun entryScreensKeepNavigationAndUnsavedFormAsksBeforeChangingTab() {
        val item = VaultEntry("entry", "Cuenta", "ana", "", "", null)
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(), listOf(item)))
        compose.setContent { VaultkTheme { VaultContentScreen(content, "Personal.kdbx", {}, editVault = editor) } }
        compose.onNodeWithText("Cuenta").performClick()
        compose.onNodeWithTag("navigation-generator").assertIsDisplayed()
        compose.onNodeWithTag("navigation-settings").performClick()
        compose.onNodeWithText("Apariencia general de la aplicación.").assertIsDisplayed()
        compose.onNodeWithTag("navigation-vault").performClick()
        compose.onNodeWithContentDescription("Nueva entrada").performClick()
        compose.onNodeWithTag("navigation-generator").assertIsDisplayed()
        compose.onNodeWithText("Título").performTextInput("Borrador")
        compose.onNodeWithText("Borrador").assertIsDisplayed()
        compose.onNodeWithTag("navigation-settings").performClick()
        compose.onNodeWithText("¿Descartar la nueva entrada?").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Borrador").assertIsDisplayed()
    }

    @Test fun lockedVaultHasBottomNavigationAndEditableSettingsWithoutBiometricEnrollment() {
        var appearance by mutableStateOf(Appearance.Dark)
        compose.setContent { VaultkTheme {
            UnlockVaultScreen(SelectedVault("Personal.kdbx", "Dispositivo", null, "Bloqueado"),
                onSelectVault = {}, onUnlock = { it.fill('\u0000') }, settingsContent = { modifier ->
                    SettingsScreen(appearance,
                        onAppearanceChange = { appearance = it }, biometricEnabled = false,
                        onBiometricClick = null, timeoutSeconds = 300, onTimeoutClick = {},
                        clipboardSeconds = 30, onClipboardChange = {}, searchNotes = true,
                        onSearchNotesChange = {}, showDetails = true, onShowDetailsChange = {},
                        modifier = modifier)
                })
        } }
        compose.onNodeWithContentDescription("Más opciones").assertDoesNotExist()
        compose.onNodeWithTag("unlock-manual").assertIsNotEnabled()
        compose.onNodeWithTag("navigation-settings").performClick()
        compose.onNodeWithText("Desbloqueo biométrico").assertDoesNotExist()
        compose.onNodeWithText("Cambiar vault").assertDoesNotExist()
        compose.onNodeWithText("Apariencia general de la aplicación.").assertIsDisplayed()
        compose.onNodeWithText("Claro").performClick()
        compose.runOnIdle { check(appearance == Appearance.Light) }
        compose.onNodeWithTag("navigation-vault").performClick()
        compose.onNodeWithTag("unlock-manual").assertIsNotEnabled()
    }

    @Test fun selectedVaultIsOneNavigableRowWithStatus() {
        var selectorOpened = false
        compose.setContent { VaultkTheme {
            UnlockVaultScreen(SelectedVault("Vaultk-Test.kdbx", "Dispositivo · Local", "Nunca", "Bloqueado"),
                onSelectVault = { selectorOpened = true }, onUnlock = { it.fill('\u0000') })
        } }
        compose.onNodeWithTag("selected-vault-row").assertIsDisplayed().performClick()
        compose.runOnIdle { check(selectorOpened) }
        compose.onNodeWithText("Seleccionar vault").assertDoesNotExist()
        compose.onNodeWithText("Vaultk-Test.kdbx").assertIsDisplayed()
    }

    @Test fun switchingVaultsWithSameNameClearsMasterKeyInput() {
        var selectedUri by mutableStateOf("content://device/one")
        compose.setContent { VaultkTheme {
            UnlockVaultScreen(SelectedVault("Personal.kdbx", "Dispositivo", null, "Bloqueado",
                identity = selectedUri), onSelectVault = {}, onUnlock = { it.fill('\u0000') })
        } }
        compose.onNodeWithText("Introduce la llave maestra").performTextInput("clave temporal")
        compose.onNodeWithTag("unlock-manual").assertIsEnabled()
        compose.runOnIdle { selectedUri = "content://device/two" }
        compose.onNodeWithTag("unlock-manual").assertIsNotEnabled()
    }

    @Test fun fileAccessChoicesAndPermissionActionAreVisibleInSettings() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(), emptyList()))
        var mode by mutableStateOf(FileAccessMode.Standard)
        var manageRequested = false
        compose.setContent { VaultkTheme {
            VaultContentScreen(content, "Personal.kdbx", {}, fileAccessMode = mode,
                fileAccessGranted = true, onFileAccessChange = { mode = it },
                onManageFileAccess = { manageRequested = true }, editVault = editor)
        } }
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Acceso a archivos").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("SAF").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Acceso completo").performScrollTo().performClick()
        compose.runOnIdle { check(mode == FileAccessMode.Full) }
        compose.onNodeWithText("Estado: Activo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Administrar permiso").performScrollTo().performClick()
        compose.runOnIdle { check(manageRequested && mode == FileAccessMode.Full) }
        compose.onNodeWithText("SAF").performScrollTo().performClick()
        compose.runOnIdle { check(mode == FileAccessMode.Standard) }
    }

    @Test fun managingFilePermissionConfirmsUnsavedVaultChanges() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(), emptyList()))
        var manageRequested = false
        compose.setContent { VaultkTheme {
            VaultContentScreen(content, "Personal.kdbx", {}, onManageFileAccess = { manageRequested = true }, editVault = editor)
        } }
        compose.onNodeWithContentDescription("Nueva entrada").performClick()
        compose.onNodeWithText("Título").performTextInput("Temporal")
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithTag("navigation-settings").performClick()
        compose.onNodeWithText("Administrar permiso").performScrollTo().performClick()
        compose.onNodeWithText("Al abrir los ajustes de permisos se bloqueará el vault y se perderán los cambios sin guardar. El archivo KDBX no se ha modificado.").assertIsDisplayed()
        compose.runOnIdle { check(!manageRequested) }
        compose.onNodeWithText("Seguir en el vault").performClick()
        compose.runOnIdle { check(!manageRequested) }
        compose.onNodeWithText("Administrar permiso").performClick()
        compose.onNodeWithText("Descartar y cerrar").performClick()
        compose.runOnIdle { check(manageRequested) }
    }

    @Test fun manualLockAsksBeforeDiscardingEdits() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(), emptyList()))
        var locked = false
        compose.setContent { VaultkTheme {
            VaultContentScreen(content, "Personal.kdbx", {}, onLock = { locked = true }, editVault = editor)
        } }
        compose.onNodeWithContentDescription("Nueva entrada").performClick()
        compose.onNodeWithText("Título").performTextInput("Temporal")
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Bloquear vault").performClick()
        compose.onNodeWithText("Los cambios sin guardar se perderán al bloquear el vault. El archivo KDBX no se ha modificado.").assertIsDisplayed()
        compose.runOnIdle { check(!locked) }
        compose.onNodeWithText("Seguir en el vault").performClick()
        compose.runOnIdle { check(!locked) }
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Bloquear vault").performClick()
        compose.onNodeWithText("Descartar y cerrar").performClick()
        compose.runOnIdle { check(locked) }
    }

    @Test fun lockTimeoutCanBeChangedFromSettings() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(), emptyList()))
        var timeout = 300
        compose.setContent { VaultkTheme {
            VaultContentScreen(content, "Personal.kdbx", {}, timeoutSeconds = timeout,
                onTimeoutChange = { timeout = it }, editVault = editor)
        } }
        compose.onNodeWithTag("navigation-settings").performClick()
        compose.onNodeWithText("Bloqueo automático").performClick()
        compose.onNodeWithText("2 min").assertDoesNotExist()
        compose.onNodeWithText("4 min").assertDoesNotExist()
        compose.onNodeWithTag("lock-timeout-180").performClick()
        compose.runOnIdle { check(timeout == 180) }
    }

    @Test fun lockTimeoutDescriptionFollowsSelectedChoice() {
        var timeout by mutableStateOf(300)
        var dialog by mutableStateOf(false)
        compose.setContent { VaultkTheme {
            SettingsScreen(Appearance.Dark, onAppearanceChange = {}, biometricEnabled = false,
                onBiometricClick = null, timeoutSeconds = timeout, onTimeoutClick = { dialog = true },
                clipboardSeconds = 30, onClipboardChange = {}, searchNotes = true,
                onSearchNotesChange = {}, showDetails = true, onShowDetailsChange = {})
            if (dialog) LockTimeoutDialog(
                timeout,
                { timeout = it }, { dialog = false })
        } }
        compose.onNodeWithText("Se bloquea tras 5 minutos de inactividad.").assertIsDisplayed()
        compose.onNodeWithText("Bloqueo automático").performClick()
        compose.onNodeWithTag("lock-timeout-0").performClick()
        compose.runOnIdle { check(timeout == 0) }
        compose.onNodeWithText("Se bloquea al salir de la aplicación.").assertIsDisplayed()
        compose.onNodeWithText("Bloqueo automático").performClick()
        compose.onNodeWithTag("lock-timeout-30").performClick()
        compose.onNodeWithText("Se bloquea tras 30 segundos de inactividad.").assertIsDisplayed()
        compose.onNodeWithText("Bloqueo automático").performClick()
        compose.onNodeWithTag("lock-timeout-60").performClick()
        compose.onNodeWithText("Se bloquea tras 1 minuto de inactividad.").assertIsDisplayed()
    }

    @Test fun clipboardTimeoutUsesTheSameChoicePatternWithoutSavingOnCancel() {
        var clipboard by mutableStateOf(30)
        compose.setContent { VaultkTheme {
            SettingsScreen(Appearance.Dark, onAppearanceChange = {}, biometricEnabled = false,
                onBiometricClick = null, timeoutSeconds = 300, onTimeoutClick = {},
                clipboardSeconds = clipboard, onClipboardChange = { clipboard = it },
                searchNotes = true, onSearchNotesChange = {},
                showDetails = true, onShowDetailsChange = {})
        } }
        compose.onNodeWithText("Limpiar portapapeles").performClick()
        compose.onNodeWithTag("time-choice-30 s").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { check(clipboard == 30) }
        compose.onNodeWithText("Limpiar portapapeles").performClick()
        compose.onNodeWithTag("time-choice-60 s").performClick()
        compose.runOnIdle { check(clipboard == 60) }
    }

    @Test fun biometricEnrollmentRequiresMasterKey() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(), emptyList()))
        var requested: String? = null
        compose.setContent {
            VaultkTheme {
                VaultContentScreen(content, "Personal.kdbx", {}, biometricEnabled = false,
                    onEnableBiometric = { key, complete ->
                        requested = String(key)
                        key.fill('\u0000')
                        complete(null)
                    }, editVault = editor)
            }
        }
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Activar desbloqueo biométrico").performClick()
        compose.onNodeWithText("Introduce la llave maestra").performTextInput("prueba-maestra")
        compose.onNodeWithTag("biometric-enable-confirm").performClick()
        compose.runOnIdle { check(requested == "prueba-maestra") }
    }

    @Test fun biometricProtectionCanBeDisabled() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(), emptyList()))
        var disabled = false
        compose.setContent {
            VaultkTheme {
                VaultContentScreen(content, "Personal.kdbx", {}, biometricEnabled = true,
                    onDisableBiometric = { disabled = true }, editVault = editor)
            }
        }
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Desactivar desbloqueo biométrico").performClick()
        compose.runOnIdle { check(disabled) }
    }

    @Test fun biometricUnlockAlwaysKeepsMasterKeyAlternative() {
        var biometricRequests = 0
        var manualKey: String? = null
        compose.setContent {
            VaultkTheme {
                UnlockVaultScreen(SelectedVault("Personal.kdbx", "Dispositivo", null, "Bloqueado", true),
                    onSelectVault = {}, onUnlock = { key -> manualKey = String(key); key.fill('\u0000') },
                    onBiometricUnlock = { biometricRequests++ })
            }
        }
        compose.onNodeWithText("Desbloquear con huella").performClick()
        compose.onNodeWithText("Introduce la llave maestra").performTextInput("alternativa")
        compose.onNodeWithTag("unlock-manual").performClick()
        compose.runOnIdle { check(biometricRequests == 1 && manualKey == "alternativa") }
    }

    @Test fun searchesAcrossGroupsAndOpensMatchingEntryDirectly() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", listOf(
            VaultGroup("work", "Trabajo", emptyList(), listOf(
                VaultEntry("mail", "Correo", "ana", "https://mail.example.org", "", null,
                    tags = listOf("Servicios"),
                    customFields = mapOf("Región" to KdbxVaultValueFactory().create("Europa".toCharArray(), false))))),
            VaultGroup("other", "Otros", emptyList(), listOf(
                VaultEntry("other-entry", "Otra entrada", "luis", "", "", null))),
        ), emptyList()))
        compose.setContent { VaultkTheme { VaultContentScreen(content, "Personal.kdbx", {}, editVault = editor) } }

        compose.onNodeWithContentDescription("Seleccionar grupo").performClick()
        compose.onNodeWithTag("group-enter-other").performClick()
        compose.onNodeWithTag("group-use-current").performClick()
        compose.onNodeWithText("Correo").assertDoesNotExist()
        compose.onNodeWithText("Buscar en el vault…").performTextInput("EUROPA")
        compose.onNodeWithText("Correo").assertIsDisplayed().performClick()
        compose.onNodeWithText("https://mail.example.org").assertIsDisplayed()
        compose.onNodeWithText("Trabajo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Correo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Borrar búsqueda").performClick()
        compose.onNodeWithText("Correo").assertIsDisplayed()
        compose.onNodeWithText("Otra entrada").assertDoesNotExist()
    }

    @Test fun selectsNestedGroupAndOpensEntryWithoutLeavingSession() {
        val entry = VaultEntry("entry", "Mega", "usuario", "https://example.org", "Una nota", null)
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", listOf(
            VaultGroup("work", "Trabajo", listOf(
                VaultGroup("services", "Servicios", emptyList(), listOf(entry))
            ), emptyList())
        ), emptyList()))
        compose.setContent { VaultkTheme { VaultContentScreen(content, "Personal.kdbx", {}, editVault = editor) } }

        compose.onNodeWithText("Mega").assertDoesNotExist()
        compose.onNodeWithContentDescription("Seleccionar grupo").performClick()
        compose.onNodeWithTag("group-enter-work").performClick()
        compose.onNodeWithTag("group-enter-services").performClick()
        compose.onNodeWithTag("group-use-current").performClick()
        compose.onNodeWithText("Mega").assertIsDisplayed().performClick()
        compose.onNodeWithText("Una nota").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Mega").assertIsDisplayed()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Tema").assertIsDisplayed()
        compose.onNode(hasText("Vault") and hasClickAction()).performClick()
        compose.onNodeWithText("Mega").assertIsDisplayed()
    }

    @Test fun settingsChangeSearchAndAppearanceWithoutLosingVaultNavigation() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(),
            listOf(VaultEntry("entry", "Correo", "ana", "", "claveunica", null))))
        var appearance by mutableStateOf(Appearance.Dark)
        var searchNotes by mutableStateOf(true)
        compose.setContent { VaultkTheme(darkTheme = appearance == Appearance.Dark) {
            VaultContentScreen(content, "Personal.kdbx", {}, appearance = appearance,
                onAppearanceChange = { appearance = it }, searchNotes = searchNotes,
                onSearchNotesChange = { searchNotes = it }, editVault = editor)
        } }
        compose.onNodeWithText("Buscar en el vault…").performTextInput("claveunica")
        compose.onNodeWithText("Correo").assertIsDisplayed()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Claro").performScrollTo().performClick()
        compose.onNodeWithTag("settings-list").performScrollToIndex(3)
        compose.onNodeWithTag("setting-switch-row-Buscar en notas").performScrollTo().performClick()
        compose.runOnIdle { check(appearance == Appearance.Light && !searchNotes) }
        compose.onNode(hasText("Vault") and hasClickAction()).performClick()
        compose.onNodeWithText("No se encontraron entradas.").assertIsDisplayed()
    }

    @Test fun changeVaultConfirmsDiscardingUnsavedEntry() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(), emptyList()))
        var changed = false
        compose.setContent { VaultkTheme {
            VaultContentScreen(content, "Personal.kdbx", {},
                onChangeVault = { changed = true }, editVault = editor)
        } }
        compose.onNodeWithContentDescription("Nueva entrada").performClick()
        compose.onNodeWithText("Título").performTextInput("Temporal")
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Cambiar vault").performClick()
        compose.onNodeWithText("Los cambios sin guardar se perderán al cambiar de vault. El archivo KDBX actual no se ha modificado.").assertIsDisplayed()
        compose.runOnIdle { check(!changed) }
        compose.onNodeWithText("Seguir en el vault").performClick()
        compose.runOnIdle { check(!changed) }
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Cambiar vault").performClick()
        compose.onNodeWithText("Descartar y cerrar").performClick()
        compose.runOnIdle { check(changed) }
    }

    @Test fun searchesGroupsAndWarnsBeforeDiscardingTemporaryGroup() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", listOf(
            VaultGroup("trash", "Papelera", emptyList(), emptyList()),
            VaultGroup("work", "Trabajo", listOf(
                VaultGroup("nested", "Anidado", emptyList(), emptyList())
            ), emptyList())
        ), emptyList()))
        var closed = false
        compose.setContent { VaultkTheme { VaultContentScreen(content, "Personal.kdbx", { closed = true }, editVault = editor) } }

        compose.onNodeWithContentDescription("Seleccionar grupo").performClick()
        compose.onNodeWithText("Buscar grupo").performTextInput("Papelera")
        compose.onNodeWithTag("group-select-trash").assertIsDisplayed().performClick()
        compose.onNodeWithText("Papelera").assertIsDisplayed()
        compose.onNodeWithContentDescription("Seleccionar grupo").performClick()
        compose.onNodeWithText("Buscar grupo").performTextInput("Anidado")
        compose.onNodeWithTag("group-select-nested").assertIsDisplayed().performClick()
        compose.onNodeWithText("Anidado").assertIsDisplayed()

        compose.onNodeWithContentDescription("Seleccionar grupo").performClick()
        compose.onNodeWithText("Nuevo grupo").performClick()
        compose.onNodeWithText("Nombre del grupo").performTextInput("Temporal")
        compose.onNodeWithText("Crear grupo").performClick()
        compose.onNodeWithText("Hay grupos nuevos sin guardar. Guarda una copia KDBX para conservarlos.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Cerrar vault").performClick()
        compose.onNodeWithText("Seguir en el vault").performClick()
        compose.runOnIdle { check(!closed) }
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Cerrar vault").performClick()
        compose.onNodeWithText("Descartar y cerrar").performClick()
        compose.runOnIdle { check(closed) }
    }

    @Test fun detailKeepsPasswordHiddenUntilRequestedAndCopiesSensitiveValue() {
        val password = "contraseña-de-prueba"
        val entry = VaultEntry("entry", "Mega", "usuario", "https://example.org", "Una nota", null,
            password = KdbxVaultValueFactory().create(password.toCharArray(), true),
            otp = KdbxVaultValueFactory().create("otpauth://totp/Test?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ".toCharArray(), true),
            tags = listOf("Servicios"))
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(), listOf(entry)))
        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(ClipboardManager::class.java)
        try {
            compose.setContent {
                CompositionLocalProvider(LocalCopyCredential provides CopyCredential(
                    AndroidCredentialClipboard(InstrumentationRegistry.getInstrumentation().targetContext))) {
                    VaultkTheme { VaultContentScreen(content, "Personal.kdbx", {}, editVault = editor) }
                }
            }
            compose.onNodeWithText("Mega").performClick()
            compose.onNodeWithText(password).assertDoesNotExist()
            compose.onNodeWithText("••••••••••••").assertIsDisplayed()
            compose.onNodeWithContentDescription("Mostrar contraseña").performClick()
            compose.onNodeWithText(password).assertIsDisplayed()
            compose.onNodeWithContentDescription("Ocultar contraseña").performClick()
            compose.onNodeWithText(password).assertDoesNotExist()
            compose.onNodeWithTag("detail-copy-password").performClick()
            compose.runOnIdle {
                check(clipboard.primaryClip?.getItemAt(0)?.text?.toString() == password)
            }
            compose.onNodeWithText("TOTP").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("detail-totp-code").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("detail-totp-remaining").assertIsDisplayed()
            compose.onNodeWithTag("detail-copy-totp").performClick()
            compose.runOnIdle {
                check(clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.matches(Regex("[0-9]{6}")) == true)
            }
            compose.onNodeWithText("Servicios").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Más opciones").performClick()
            compose.onNodeWithText("Eliminar entrada").performClick()
            compose.onNodeWithText("¿Eliminar esta entrada?").assertIsDisplayed()
            compose.onNodeWithText("Cancelar").performClick()
        } finally {
            clipboard.clearPrimaryClip()
        }
    }

    @Test fun createsEntryInSessionAndConfirmsDiscardOnVaultClose() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", listOf(
            VaultGroup("services", "Servicios", emptyList(), emptyList())
        ), emptyList()))
        var closed = false
        compose.setContent { VaultkTheme { VaultContentScreen(content, "Personal.kdbx", { closed = true }, editVault = editor) } }

        compose.onNodeWithContentDescription("Seleccionar grupo").performClick()
        compose.onNodeWithTag("group-enter-services").performClick()
        compose.onNodeWithTag("group-use-current").performClick()
        compose.onNodeWithContentDescription("Nueva entrada").performClick()
        compose.onNodeWithText("Nueva entrada").assertIsDisplayed()
        compose.onNodeWithText("Servicios").assertIsDisplayed()
        compose.onNodeWithText("La nueva entrada permanecerá en esta sesión hasta que guardes una copia KDBX.")
            .assertIsDisplayed()
        compose.onNodeWithText("Título").performTextInput("Cuenta nueva")
        compose.onNodeWithText("Generar contraseña").performClick()
        compose.onNodeWithText("Generar").performClick()
        compose.onNodeWithText("Usar contraseña").performClick()
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.onNodeWithText("Cuenta nueva").assertIsDisplayed()
        compose.onNodeWithText("••••••••••••").assertIsDisplayed()
        compose.onNodeWithContentDescription("Editar").performClick()
        compose.onNodeWithText("Título").performTextReplacement("Cuenta actualizada")
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.onNodeWithText("Cuenta actualizada").assertIsDisplayed()
        compose.onNodeWithText("••••••••••••").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Cuenta nueva").assertDoesNotExist()
        compose.onNodeWithText("Cuenta actualizada").assertIsDisplayed()
        compose.onNodeWithText("Temporal").assertIsDisplayed()
        compose.onNodeWithText("Cambios sin guardar", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Cerrar vault").performClick()
        compose.onNodeWithText("¿Descartar cambios temporales?").assertIsDisplayed()
        compose.onNodeWithText("Descartar y cerrar").performClick()
        compose.runOnIdle { check(closed) }
    }

    @Test fun editsExistingEntryInPlaceAndWarnsOnClose() {
        val created = Instant.parse("2025-03-01T00:00:00Z")
        val original = VaultEntry("existing", "Mega", "usuario", "", "", created,
            password = KdbxVaultValueFactory().create("clave vieja".toCharArray(), true), created = created)
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(), listOf(original)))
        var closed = false
        compose.setContent { VaultkTheme { VaultContentScreen(content, "Personal.kdbx", { closed = true }, editVault = editor) } }
        compose.onNodeWithText("Mega").performClick()
        compose.onNodeWithContentDescription("Editar").performClick()
        compose.onNodeWithText("Título").performTextReplacement("Mega nueva")
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("¿Descartar los cambios de la entrada?").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.onNodeWithText("Mega nueva").assertIsDisplayed()
        compose.onNodeWithText("••••••••••••").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Mega nueva").assertIsDisplayed()
        compose.onNodeWithText("Mega").assertDoesNotExist()
        compose.onNodeWithText("Cambios sin guardar", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Cerrar vault").performClick()
        compose.onNodeWithText("¿Descartar cambios temporales?").assertIsDisplayed()
        compose.onNodeWithText("Descartar y cerrar").performClick()
        compose.runOnIdle { check(closed) }
    }

    @Test fun failedDirectSaveRetainsSessionEditsAndShowsFailure() {
        val original = VaultContent("Personal", VaultGroup("root", "Raíz", emptyList(),
            listOf(VaultEntry("existing", "Antes", "", "", "", null))))
        var received: VaultContent? = null
        compose.setContent { VaultkTheme { VaultContentScreen(original, "Personal.kdbx", {},
            onSave = { changes, _, complete ->
                received = changes
                complete("No se pudo guardar")
            }, editVault = editor) } }
        compose.onNodeWithText("Antes").performClick()
        compose.onNodeWithContentDescription("Editar").performClick()
        compose.onNodeWithText("Título").performTextReplacement("Después")
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.runOnIdle {
            check(received?.root?.entries?.single()?.title == "Después")
        }
        compose.onNodeWithText("Después").assertIsDisplayed()
        compose.onNodeWithText("Cambios sin guardar", substring = true).assertIsDisplayed()
    }

    @Test fun duplicatesMovesTrashesAndRestoresWithFailedDirectSaves() {
        val content = VaultContent("Personal", VaultGroup("root", "Raíz", listOf(
            VaultGroup("from", "Origen", emptyList(),
                listOf(VaultEntry("original", "Mega", "user", "", "", null))),
            VaultGroup("to", "Destino", emptyList(), emptyList()),
            VaultGroup("trash", "Papelera", emptyList(), emptyList()),
        ), emptyList()), recycleBinId = "trash")
        var saved: VaultContent? = null
        compose.setContent { VaultkTheme { VaultContentScreen(content, "Personal.kdbx", {},
            onSave = { changes, _, complete ->
                saved = changes
                complete("Fallo de prueba")
            }, editVault = editor) } }

        compose.onNodeWithContentDescription("Seleccionar grupo").performClick()
        compose.onNodeWithTag("group-enter-from").performClick()
        compose.onNodeWithTag("group-use-current").performClick()
        compose.onNodeWithText("Mega").performClick()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Duplicar").performClick()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Mover").performClick()
        compose.onNodeWithText("Buscar grupo").performTextInput("Destino")
        compose.onNodeWithTag("group-select-to").performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Mega").assertIsDisplayed()

        compose.onNodeWithContentDescription("Seleccionar grupo").performClick()
        compose.onNodeWithText("Buscar grupo").performTextInput("Origen")
        compose.onNodeWithTag("group-enter-from").performClick()
        compose.onNodeWithTag("group-use-current").performClick()
        compose.onNodeWithText("Mega").performClick()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Eliminar entrada").performClick()
        compose.onNodeWithText("La entrada pasará a Papelera", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Eliminar").performClick()

        compose.onNodeWithContentDescription("Seleccionar grupo").performClick()
        compose.onNodeWithText("Buscar grupo").performTextInput("Papelera")
        compose.onNodeWithTag("group-select-trash").performClick()
        compose.onNodeWithText("Mega").performClick()
        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onNodeWithText("Restaurar en otro grupo").performClick()
        compose.onNodeWithText("Buscar grupo").performTextInput("Destino")
        compose.onNodeWithTag("group-select-to").performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Cambios sin guardar", substring = true).assertIsDisplayed()
        compose.runOnIdle {
            val entries = saved!!.root.groups.single { it.id == "to" }.entries
            check(entries.size == 2 && entries.any { it.id == "original" })
            check(entries.any { it.id.startsWith("draft:") })
            check(saved!!.root.groups.single { it.id == "trash" }.entries.isEmpty())
        }
    }
}
