package com.edro08.vaultk

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsProperties
import com.edro08.vaultk.ui.screen.storage.SelectedVault
import com.edro08.vaultk.ui.screen.storage.UnlockVaultScreen
import com.edro08.vaultk.ui.screen.storage.formatOpeningElapsed
import com.edro08.vaultk.ui.theme.VaultkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UnlockElapsedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun elapsedTimeUsesTenthsAndKeepsCountingPastOneMinute() {
        assertEquals("00:00.0", formatOpeningElapsed(0))
        assertEquals("00:04.8", formatOpeningElapsed(4_899))
        assertEquals("00:59.9", formatOpeningElapsed(59_999))
        assertEquals("01:00.0", formatOpeningElapsed(60_000))
        assertEquals("02:03.4", formatOpeningElapsed(123_499))
    }

    @Test fun counterAppearsOnlyDuringOpeningAndResetsOnRetry() {
        var loading by mutableStateOf(false)
        var started by mutableLongStateOf(0L)
        var error by mutableStateOf<String?>(null)
        compose.setContent { VaultkTheme {
            UnlockVaultScreen(SelectedVault("Nuevo.kdbx", "Dispositivo", null, "Bloqueado"),
                onSelectVault = {}, loading = loading, openingStartedAtMillis = started,
                errorMessage = error)
        } }
        compose.onNodeWithTag("unlock-elapsed").assertDoesNotExist()
        compose.runOnIdle { started = SystemClock.elapsedRealtime(); loading = true }
        compose.onNodeWithTag("unlock-elapsed").assertIsDisplayed()
        compose.onNodeWithText("Tiempo transcurrido").assertIsDisplayed()
        val firstValue = compose.onNodeWithTag("unlock-elapsed").fetchSemanticsNode()
            .config[SemanticsProperties.Text].first().text
        compose.waitUntil(3_000) {
            compose.onNodeWithTag("unlock-elapsed").fetchSemanticsNode()
                .config[SemanticsProperties.Text].first().text != firstValue
        }
        compose.runOnIdle { loading = false; error = "No se pudo abrir" }
        compose.onNodeWithTag("unlock-elapsed").assertDoesNotExist()
        compose.onNodeWithText("No se pudo abrir").assertIsDisplayed()
        compose.runOnIdle { started = SystemClock.elapsedRealtime() + 1_000; error = null; loading = true }
        compose.onNodeWithTag("unlock-elapsed").assertIsDisplayed()
        compose.onNodeWithText("00:00.0").assertIsDisplayed()
    }
}
