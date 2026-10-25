package com.edro08.vaultk

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.edro08.vaultk.data.LocalVaultFiles
import com.edro08.vaultk.application.BrowseLocalVaults
import com.edro08.vaultk.data.KdbxReader
import com.edro08.vaultk.ui.screen.LocalVaultPickerScreen
import com.edro08.vaultk.ui.theme.VaultkTheme
import java.io.File
import org.linguafranca.pwdb.format.KdbxCredentials
import org.linguafranca.pwdb.format.KdbxHeader
import org.linguafranca.pwdb.format.KdbxStreamFormat
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocalVaultFilesTest {
    @get:Rule val compose = createComposeRule()

    @Test fun browserOnlyListsVaultsAndCanFindThemInNestedFolders() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "local-vault-picker-test").apply { deleteRecursively(); mkdirs() }
        try {
            val documents = File(root, "Documents").apply { mkdirs() }
            File(root, "Download").mkdirs()
            File(documents, "Personal.KDBX").writeText("demo")
            File(documents, "otro.txt").writeText("not a vault")
            val files = LocalVaultFiles(root) { true }
            var picked: File? = null
            compose.setContent { VaultkTheme {
                LocalVaultPickerScreen(BrowseLocalVaults(files), onBack = {}, onSelect = { picked = File(it.value) })
            } }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("local-Documents").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("local-Documents").performClick()
            compose.onNodeWithTag("local-Personal.KDBX").assertIsDisplayed()
            compose.onNodeWithTag("local-otro.txt").assertDoesNotExist()
            compose.onNodeWithTag("local-Personal.KDBX").performClick()
            compose.runOnIdle { assertEquals(File(documents, "Personal.KDBX").canonicalFile, picked?.canonicalFile) }
            compose.onNodeWithText("Buscar archivos KDBX…").performTextInput("personal")
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("local-Personal.KDBX").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("local-Personal.KDBX").assertIsDisplayed()
        } finally { root.deleteRecursively() }
    }

    @Test fun directReadsRequireGrantAndConfinePathsToSharedRoot() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "local-vault-files-test").apply { deleteRecursively(); mkdirs() }
        val outside = File(context.cacheDir, "external.kdbx")
        try {
            val folder = File(root, "Vaults").apply { mkdirs() }
            val vault = File(folder, "Edro.kdbx").apply { writeText("encrypted") }
            File(folder, "bad.txt").writeText("other")
            outside.writeText("outside")
            var granted = false
            val files = LocalVaultFiles(root) { granted }
            assertFalse(files.canRead(Uri.fromFile(vault)))
            assertTrue(runCatching { files.directory(folder) }.isFailure)
            granted = true
            assertEquals(listOf(vault.canonicalFile), files.directory(folder).second.map { it.canonicalFile })
            assertEquals(listOf(vault.canonicalFile), files.search("edro").map { it.canonicalFile })
            assertEquals("encrypted", files.open(Uri.fromFile(vault)).bufferedReader().use { it.readText() })
            assertFalse(files.canRead(Uri.fromFile(File(folder, "bad.txt"))))
            assertFalse(files.canRead(Uri.fromFile(outside)))
            assertFalse(files.withinRoot(File(folder, "../../external.kdbx")))
            assertTrue(runCatching { files.open(Uri.fromFile(outside)) }.isFailure)
        } finally { root.deleteRecursively(); outside.delete() }
    }

    @Test fun opensEncryptedVaultSelectedByLocalPath() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "local-vault-open-test").apply { deleteRecursively(); mkdirs() }
        try {
            val folder = File(root, "Vaults").apply { mkdirs() }
            val vault = File(folder, "Personal.kdbx")
            val database = KdbxDatabase()
            database.rootGroup.addGroup("Local").addEntry("Entrada local")
            vault.outputStream().use { database.save(KdbxStreamFormat(KdbxHeader(4)),
                KdbxCredentials("maestra".toByteArray()), it) }
            val files = LocalVaultFiles(root) { true }
            val chosen = files.directory(folder).second.single()
            val key = "maestra".toCharArray()
            val opened = try { files.open(Uri.fromFile(chosen)).use { KdbxReader().read(it, key) } }
                finally { key.fill('\u0000') }
            assertEquals("Entrada local", opened.root.groups.single().entries.single().title)
        } finally { root.deleteRecursively() }
    }
}
