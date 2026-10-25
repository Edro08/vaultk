package com.edro08.vaultk

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.edro08.vaultk.domain.VaultDocument
import com.edro08.vaultk.ui.uri
import com.edro08.vaultk.domain.FileAccessMode
import com.edro08.vaultk.ui.screen.OpenVaultScreen
import com.edro08.vaultk.ui.theme.VaultkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class OpenVaultScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun deviceUsesLocalBrowserInBothAccessModes() {
        var mode by mutableStateOf(FileAccessMode.Standard)
        var opened = 0
        compose.setContent { VaultkTheme {
            OpenVaultScreen(emptyList(), {}, { opened++ }, {}, fileAccessMode = mode)
        } }
        compose.onNodeWithText("Acceso completo · Almacenamiento compartido").assertDoesNotExist()
        compose.onNodeWithText("Abrir un archivo .kdbx con el selector de Android.").assertDoesNotExist()
        compose.onNodeWithText("Explorar carpetas del almacenamiento compartido.").assertIsDisplayed()
        compose.onNodeWithText("Dispositivo").performClick()
        compose.runOnIdle { mode = FileAccessMode.Full }
        compose.onNodeWithText("Explorar carpetas del almacenamiento compartido.").assertIsDisplayed()
        compose.onNodeWithText("Dispositivo").performClick()
        compose.runOnIdle { assertEquals(2, opened) }
    }

    @Test fun recentDisplaysOriginLastOpeningAndExplicitRemoveOrRelocateActions() {
        val document = VaultDocument("content://provider/Personal.kdbx", "Personal.kdbx", 100, 200)
        var recent by mutableStateOf(listOf(document))
        var relocated: VaultDocument? = null
        compose.setContent { VaultkTheme {
            OpenVaultScreen(recent, {}, {}, {}, currentUri = document.uri,
                onRelocateRecent = { relocated = it }, onRemoveRecent = { recent = emptyList() })
        } }
        compose.onNodeWithText("Personal.kdbx").assertIsDisplayed()
        compose.onNodeWithText("Documento · Selector de Android").assertIsDisplayed()
        compose.onNodeWithText("Última apertura:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Vault actual").assertIsDisplayed()
        compose.onNodeWithContentDescription("Opciones de Personal.kdbx").performClick()
        compose.onNodeWithText("Localizar de nuevo").performClick()
        compose.runOnIdle { assertEquals(document, relocated) }
        compose.onNodeWithContentDescription("Opciones de Personal.kdbx").performClick()
        compose.onNodeWithText("Quitar de recientes").performClick()
        compose.onNodeWithText("El archivo no se borrará.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Personal.kdbx").assertIsDisplayed()
        compose.onNodeWithContentDescription("Opciones de Personal.kdbx").performClick()
        compose.onNodeWithText("Quitar de recientes").performClick()
        compose.onNodeWithText("Quitar de recientes").performClick()
        compose.onNodeWithText("Sin vaults recientes").assertIsDisplayed()
    }

    @Test fun unavailableRecentOffersRelocationWithoutRemovingRecord() {
        val document = VaultDocument("file:///storage/emulated/0/Documents/Movido.kdbx",
            "Movido.kdbx", 10)
        var missing by mutableStateOf<VaultDocument?>(document)
        var relocated: VaultDocument? = null
        compose.setContent { VaultkTheme {
            OpenVaultScreen(listOf(document), {}, {}, {}, unavailableRecent = missing,
                onUnavailableDismiss = { missing = null }, onRelocateRecent = { relocated = it })
        } }
        compose.onNodeWithText("Vault no disponible").assertIsDisplayed()
        compose.onNodeWithText("Localizar de nuevo").performClick()
        compose.runOnIdle { assertEquals(document, relocated); assertNull(missing) }
        compose.onNodeWithText("Movido.kdbx").assertIsDisplayed()
    }
}
