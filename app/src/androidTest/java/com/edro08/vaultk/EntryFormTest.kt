package com.edro08.vaultk

import android.content.ClipboardManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import com.edro08.vaultk.domain.VaultAttachment
import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.application.EntryFormData
import com.edro08.vaultk.application.CopyCredential
import com.edro08.vaultk.data.AndroidCredentialClipboard
import com.edro08.vaultk.ui.LocalCopyCredential
import com.edro08.vaultk.ui.screen.vault.EntryDetailScreen
import com.edro08.vaultk.ui.screen.vault.EntryFormScreen
import com.edro08.vaultk.ui.screen.generator.PasswordGeneratorScreen
import com.edro08.vaultk.ui.theme.VaultkTheme
import org.junit.Rule
import org.junit.Test
import com.edro08.vaultk.data.KdbxVaultValueFactory
import java.time.Instant

class EntryFormTest {
    @get:Rule val compose = createComposeRule()

    @Test fun newEntryKeepsSecondarySectionsClosedUntilRequested() {
        compose.setContent { VaultkTheme { EntryFormScreen("Servicios", {}, {}) } }
        compose.onNodeWithText("El ✓ guarda la entrada solo en esta sesión. Para conservarla, guarda una copia KDBX.")
            .assertIsDisplayed()
        compose.onNodeWithText("Añadir campo personalizado").assertDoesNotExist()
        compose.onNodeWithText("Añadir adjunto").assertDoesNotExist()
        compose.onNodeWithText("Avanzado").performScrollTo().performClick()
        compose.onNodeWithText("Añadir campo personalizado").performScrollTo().assertIsDisplayed()
    }

    @Test fun detailHidesProtectedCustomFieldsUntilExplicitlyRevealed() {
        val entry = VaultEntry("existing", "Mega", "", "", "", null,
            customFields = mapOf("PIN" to KdbxVaultValueFactory().create("1234".toCharArray(), true)))
        compose.setContent { VaultkTheme { EntryDetailScreen(entry) {} } }
        compose.onNodeWithText("PIN").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1234").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mostrar PIN").performClick()
        compose.onNodeWithText("1234").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ocultar PIN").performClick()
        compose.onNodeWithText("1234").assertDoesNotExist()
    }

    @Test fun detailActionsSitBesideValuesInsteadOfBelowThem() {
        val entry = VaultEntry("existing", "ChatGPT", "admin", "https://chatgpt.com", "- This account Chat GPT", null,
            password = KdbxVaultValueFactory().create("secret".toCharArray(), true))
        compose.setContent { VaultkTheme { EntryDetailScreen(entry) {} } }
        val username = compose.onNodeWithText("admin").fetchSemanticsNode().boundsInRoot
        val copy = compose.onNodeWithContentDescription("Copiar usuario").fetchSemanticsNode().boundsInRoot
        check(copy.left > username.right && copy.top < username.bottom)
        val url = compose.onNodeWithText("https://chatgpt.com").fetchSemanticsNode().boundsInRoot
        val open = compose.onNodeWithContentDescription("Abrir URL").fetchSemanticsNode().boundsInRoot
        check(open.left > url.right && open.top < url.bottom)
        compose.onNodeWithText("Copiar UUID").performScrollTo().assertIsDisplayed()
    }

    @Test fun detailNotesCanBeCopied() {
        val notes = "Nota de prueba con varias líneas\nSegunda línea"
        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(ClipboardManager::class.java)
        try {
            val entry = VaultEntry("existing", "Cuenta", "", "", notes, null)
            compose.setContent { ClipboardTestTheme { EntryDetailScreen(entry) {} } }
            compose.onNodeWithContentDescription("Copiar notas").performScrollTo().performClick()
            compose.runOnIdle { check(clipboard.primaryClip!!.getItemAt(0).text.toString() == notes) }
        } finally { clipboard.clearPrimaryClip() }
    }

    @Test fun notesAcceptTwoThousandCharacters() {
        val notes = longNotes()
        var saved: EntryFormData? = null
        compose.setContent { VaultkTheme { EntryFormScreen("Servicios", { saved = it }, {}) } }
        compose.onNodeWithText("Título").performTextInput("Cuenta")
        compose.onNodeWithText("Notas").performScrollTo().performTextInput(notes)
        compose.onNodeWithText("2000/2000").assertExists()
        compose.onNodeWithText("Notas").performTextReplacement(notes + "x")
        compose.onNodeWithText("2000/2000").assertExists()
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.runOnIdle { check(saved?.notes == notes) }
    }

    @Test fun longNotesRemainAccessibleAndDetailScrollsPastThem() {
        val notes = longNotes()
        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(ClipboardManager::class.java)
        try {
            val entry = VaultEntry("existing", "Cuenta", "", "", notes, null)
            compose.setContent { ClipboardTestTheme { EntryDetailScreen(entry) {} } }
            compose.onNodeWithText(notes).assertExists()
            compose.onNodeWithText("Copiar UUID").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Copiar notas").performScrollTo().performClick()
            compose.runOnIdle { check(clipboard.primaryClip!!.getItemAt(0).text.toString() == notes) }
        } finally { clipboard.clearPrimaryClip() }
    }

    private fun longNotes() =
        ("Una nota larga para comprobar el desplazamiento.\n".repeat(50)).take(1989) + "Fin de nota"

    @Test fun generatorLengthPresetsAndExactInputStayInSync() {
        compose.setContent { VaultkTheme { PasswordGeneratorScreen() } }
        compose.onNodeWithTag("generator-preset-32").performClick()
        compose.onNodeWithTag("generator-length").assertTextEquals("32")
        compose.onNodeWithTag("generator-length").performTextReplacement("27")
        compose.onNodeWithTag("generator-length").assertTextEquals("27")
        compose.onNodeWithTag("generator-preset-64").performClick()
        compose.onNodeWithTag("generator-length").assertTextEquals("64")
        compose.onNodeWithTag("generator-length").performTextReplacement("128")
        compose.onNodeWithTag("generator-length").assertTextEquals("128")
        compose.onNodeWithTag("generator-slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        compose.onNodeWithTag("generator-length").assertTextEquals("32")
    }

    @Test fun acceptsOtpAuthUriWithQrParametersAndRejectsHotp() {
        val qr = "otpauth://totp/KeePassXC:ana?secret=GEZDGNBVGY3TQOJQ&issuer=KeePassXC&digits=8&period=30"
        var saved: EntryFormData? = null
        compose.setContent { VaultkTheme { EntryFormScreen("Servicios", { saved = it }, {}) } }
        compose.onNodeWithText("Título").performTextInput("Cuenta QR")
        compose.onNodeWithText("Avanzado").performScrollTo().performClick()
        compose.onNodeWithText("Escanear QR TOTP").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Clave TOTP u otpauth://").performTextInput(qr.replace("totp/", "hotp/"))
        compose.onNodeWithText("Introduce una clave Base32 o un enlace TOTP válido.").assertIsDisplayed()
        compose.onNodeWithText("Clave TOTP u otpauth://").performTextReplacement(qr)
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.runOnIdle { check(saved?.otp == qr) }
    }

    @Test fun generatorConfigurationRegenerationAndCopyDoNotChangeFormUntilUse() {
        var saved: EntryFormData? = null
        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(ClipboardManager::class.java)
        try {
            compose.setContent { ClipboardTestTheme { EntryFormScreen("Servicios", { saved = it }, {}) } }
            compose.onNodeWithText("Título").performTextInput("Nueva cuenta")
            compose.onNodeWithText("Generar").performClick()
            compose.onNodeWithText("Generar").assertIsDisplayed()
            compose.onNodeWithText("Usar contraseña").assertIsDisplayed().assertIsNotEnabled()
            compose.onNodeWithText("Contraseña generada").assertDoesNotExist()
            compose.onNodeWithTag("generator-length").performTextReplacement("12")
            compose.onNodeWithText("Mayúsculas (A–Z)").performClick()
            compose.onNodeWithText("Minúsculas (a–z)").performClick()
            compose.onNodeWithText("Símbolos (!@#$%&*…)").performClick()
            compose.onNodeWithText("Generar").performScrollTo().performClick()
            compose.onNodeWithText("Regenerar").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Copiar").performScrollTo().performClick()
            var first = ""
            compose.runOnIdle {
                first = clipboard.primaryClip!!.getItemAt(0).text.toString()
                check(first.matches(Regex("[0-9]{12}")))
            }
            compose.onNodeWithText("Regenerar").performClick()
            compose.onNodeWithText("Copiar").performClick()
            compose.runOnIdle {
                val second = clipboard.primaryClip!!.getItemAt(0).text.toString()
                check(second.matches(Regex("[0-9]{12}")) && second != first)
            }
            compose.onNodeWithContentDescription("Volver").performClick()
            compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
            compose.runOnIdle { check(saved?.password == "") }
        } finally { clipboard.clearPrimaryClip() }
    }

    @Test fun generatorUsedInEditorOnlyChangesPasswordOnFormSave() {
        val entry = VaultEntry("existing", "Cuenta", "ana", "", "", null,
            password = KdbxVaultValueFactory().create("anterior".toCharArray(), true))
        var saved: EntryFormData? = null
        compose.setContent { VaultkTheme { EntryFormScreen("Servicios", { saved = it }, {}, entry) } }
        compose.onNodeWithText("Generar").performClick()
        compose.onNodeWithText("Generar").performClick()
        compose.onNodeWithText("Usar contraseña").performClick()
        compose.runOnIdle { check(saved == null) }
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.runOnIdle {
            check(saved?.id == "existing")
            check(saved?.password?.length == 20 && saved?.password != "anterior")
        }
    }

    @Test fun newEntryPasswordClearRequiresConfirmation() {
        var saved: EntryFormData? = null
        compose.setContent { VaultkTheme { EntryFormScreen("Servicios", { saved = it }, {}) } }
        compose.onNodeWithText("Título").performTextInput("Cuenta")
        compose.onNodeWithText("Contraseña").performTextInput("clave-segura")
        compose.onNodeWithText("Limpiar").performClick()
        compose.onNodeWithText("¿Limpiar contraseña?").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Limpiar").performClick()
        compose.onNodeWithText("¿Limpiar contraseña?").assertIsDisplayed()
        compose.onNodeWithText("Sí, limpiar").performClick()
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.runOnIdle { check(saved?.password == "") }
    }

    @Test fun editingPasswordCanBeClearedWithoutChangingOriginalUntilSave() {
        val entry = VaultEntry("existing", "Cuenta", "", "", "", null,
            password = KdbxVaultValueFactory().create("anterior".toCharArray(), true))
        var saved: EntryFormData? = null
        compose.setContent { VaultkTheme { EntryFormScreen("Servicios", { saved = it }, {}, entry) } }
        compose.onNodeWithText("Limpiar").performClick()
        compose.onNodeWithText("Sí, limpiar").performClick()
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.runOnIdle { check(saved?.password == "" && entry.password?.valueAsChars?.concatToString() == "anterior") }
    }

    @Test fun savesAdvancedValuesOnlyToSessionCallback() {
        var saved: EntryFormData? = null
        compose.setContent { VaultkTheme { EntryFormScreen("Servicios", { saved = it }, {}) } }
        compose.onNodeWithText("Título").performTextInput("Nueva cuenta")
        compose.onNodeWithText("General").performClick()
        compose.onNodeWithText("Avanzado").performScrollTo().performClick()
        compose.onNodeWithText("Añadir campo personalizado").performScrollTo().performClick()
        compose.onNodeWithText("Nombre del campo").performTextInput("Token")
        compose.onNodeWithText("Valor").performTextInput("secreto")
        compose.onNodeWithText("Ocultar valor").performClick()
        compose.onNodeWithText("Añadir").performClick()
        compose.onNodeWithText("Token").assertIsDisplayed()
        compose.onNodeWithText("Clave TOTP u otpauth://").performScrollTo()
            .performTextInput("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        compose.onNodeWithText("PIN").performScrollTo().performTextInput("1234")
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.runOnIdle {
            check(saved?.title == "Nueva cuenta")
            check(saved?.pin == "1234")
            check(saved?.fields?.single()?.isProtected == true)
            check(saved?.otp?.startsWith("otpauth://totp/") == true)
        }
    }

    @Test fun warnsBeforeAbandoningFilledForm() {
        var left = false
        compose.setContent { VaultkTheme { EntryFormScreen("Servicios", {}, { left = true }) } }
        compose.onNodeWithText("Título").performTextInput("Nueva cuenta")
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("¿Descartar la nueva entrada?").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { check(!left) }
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Descartar formulario").performClick()
        compose.runOnIdle { check(left) }
    }

    @Test fun editorPrefillsAndUpdatesExistingFieldsWithoutChangingIdentity() {
        val created = Instant.parse("2025-03-01T00:00:00Z")
        val expiry = Instant.parse("2027-02-02T09:00:00Z")
        val entry = VaultEntry("existing", "Mega", "old-user", "https://example.org", "nota", created,
            password = KdbxVaultValueFactory().create("old-password".toCharArray(), true),
            otp = KdbxVaultValueFactory().create("otpauth://totp/Test?secret=GEZDGNBVGY3TQOJQ".toCharArray(), true),
            tags = listOf("Nube"), created = created, expiresAt = expiry,
            customFields = mapOf("Token" to KdbxVaultValueFactory().create("secret".toCharArray(), true),
                "PIN" to KdbxVaultValueFactory().create("1234".toCharArray(), true)),
            attachments = listOf(VaultAttachment("backup.txt", bytes = byteArrayOf(1, 2))))
        var saved: EntryFormData? = null
        compose.setContent { VaultkTheme { EntryFormScreen("Servicios", { saved = it }, {}, entry) } }
        compose.onNodeWithText("Editar entrada").assertIsDisplayed()
        compose.onNodeWithText("Título").performTextReplacement("Mega actualizada")
        compose.onNodeWithText("General").performClick()
        compose.onNodeWithText("Token").performScrollTo().performClick()
        compose.onNodeWithText("Valor").performTextReplacement("otro secreto")
        compose.onNodeWithText("Actualizar").performClick()
        compose.onNodeWithText("backup.txt").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Guardar entrada en el vault").performClick()
        compose.runOnIdle {
            check(saved?.id == "existing")
            check(saved?.password == "old-password")
            check(saved?.otp == "otpauth://totp/Test?secret=GEZDGNBVGY3TQOJQ")
            check(saved?.tags == listOf("Nube"))
            check(saved?.expiresAt == expiry)
            check(saved?.pin == "1234")
            check(saved?.fields?.single() == com.edro08.vaultk.application.CustomFieldDraft("Token", "otro secreto", true))
            check(saved?.attachments?.single()?.bytes?.contentEquals(byteArrayOf(1, 2)) == true)
        }
    }
}

@Composable
private fun ClipboardTestTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    CompositionLocalProvider(LocalCopyCredential provides CopyCredential(AndroidCredentialClipboard(context))) {
        VaultkTheme { content() }
    }
}
