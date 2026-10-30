package com.edro08.vaultk

import android.accessibilityservice.AccessibilityService
import android.os.Environment
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FileAccessFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun safModeOpensAndroidDocumentPickerFromRealActivity() {
        compose.onNodeWithTag("navigation-settings").performClick()
        compose.onNodeWithText("SAF").performScrollTo().performClick()
        compose.onNodeWithTag("navigation-vault").performClick()
        compose.onNodeWithText("Seleccionar vault").performScrollTo().performClick()
        compose.onNodeWithText("Dispositivo").performClick()
        awaitExternalScreen("documentsui")
    }

    @Test fun fullModeOpensPermissionSettingsOrLocalBrowserFromRealActivity() {
        val permissionGranted = Environment.isExternalStorageManager()
        compose.onNodeWithTag("navigation-settings").performClick()
        compose.onNodeWithText("Acceso completo").performScrollTo().performClick()
        if (permissionGranted) {
            compose.onNodeWithTag("navigation-vault").performClick()
            compose.onNodeWithText("Seleccionar vault").performScrollTo().performClick()
            compose.onNodeWithText("Dispositivo").performClick()
            compose.onNodeWithText("Buscar archivos KDBX…").assertIsDisplayed()
        } else awaitExternalScreen("settings")
    }

    private fun awaitExternalScreen(packagePart: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        try {
            compose.waitUntil(10_000) {
                automation.rootInActiveWindow?.packageName?.toString()
                    ?.contains(packagePart, ignoreCase = true) == true
            }
            assertTrue(automation.rootInActiveWindow?.packageName?.toString()
                ?.contains(packagePart, ignoreCase = true) == true)
        } finally {
            automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }
    }
}
