package com.edro08.vaultk

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertTrue
import com.edro08.vaultk.domain.VaultSaveProgress
import com.edro08.vaultk.domain.VaultSaveStage
import com.edro08.vaultk.ui.screen.storage.VaultSaveDialog
import com.edro08.vaultk.ui.theme.VaultkTheme
import org.junit.Rule
import org.junit.Test

class VaultSaveDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsElapsedAndStageWithoutBottomProgressBar() {
        var progress by mutableStateOf(VaultSaveProgress(VaultSaveStage.Preparing))
        val started = SystemClock.elapsedRealtime()
        compose.setContent { VaultkTheme { VaultSaveDialog(progress, started) } }
        compose.onNodeWithText("Guardando vault…").assertIsDisplayed()
        compose.onNodeWithText("Preparando y cifrando KDBX").assertIsDisplayed()
        compose.onNodeWithTag("vault-save-elapsed").assertIsDisplayed()
        val stageBounds = compose.onNodeWithTag("vault-save-stage").fetchSemanticsNode().boundsInRoot
        val elapsedBounds = compose.onNodeWithTag("vault-save-elapsed").fetchSemanticsNode().boundsInRoot
        assertTrue("El tiempo debe quedar debajo de la etapa", elapsedBounds.top >= stageBounds.bottom)
        assertTrue("El tiempo debe alinearse con la etapa", elapsedBounds.left == stageBounds.left)
        compose.onNodeWithTag("vault-save-progress").assertDoesNotExist()
        compose.onNodeWithText("64% del archivo").assertDoesNotExist()
        compose.runOnIdle { progress = VaultSaveProgress(VaultSaveStage.Writing, 0.64f) }
        compose.onNodeWithTag("vault-save-stage").assertTextEquals("Escribiendo archivo KDBX")
        compose.onNodeWithTag("vault-save-progress").assertDoesNotExist()
        compose.onNodeWithText("64% del archivo").assertDoesNotExist()
        compose.runOnIdle { progress = VaultSaveProgress(VaultSaveStage.Verifying) }
        compose.onNodeWithText("Verificando archivo temporal").assertIsDisplayed()
        compose.onNodeWithText("64% del archivo").assertDoesNotExist()
        compose.runOnIdle { progress = VaultSaveProgress(VaultSaveStage.Replacing) }
        compose.onNodeWithText("Reemplazando vault de forma segura").assertIsDisplayed()
        compose.onNodeWithTag("vault-save-elapsed").assertIsDisplayed()
    }
}
