package com.edro08.vaultk

import com.edro08.vaultk.application.security.CopyCredential
import com.edro08.vaultk.application.security.VaultSessionController
import com.edro08.vaultk.domain.security.CredentialClipboardPort
import com.edro08.vaultk.domain.vault.VaultContent
import com.edro08.vaultk.domain.vault.VaultGroup
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSessionControllerTest {
    private class FakeClipboard : CredentialClipboardPort {
        var cleared = false
        override fun copy(label: String, value: String, sensitive: Boolean) = Unit
        override fun clearOwned() { cleared = true }
    }

    private val fakeClipboard = FakeClipboard()
    private val copyCredential = CopyCredential(fakeClipboard)
    private val controller = VaultSessionController(copyCredential)

    private val sampleContent = VaultContent("TestVault", VaultGroup("root", "Root", emptyList(), emptyList()))
    private val sampleKey = "master-password-123".toCharArray()
    private val sampleFingerprint = byteArrayOf(1, 2, 3, 4, 5)

    @Test
    fun startOpensSessionAndProvidesKeyAndBaseline() {
        assertFalse(controller.isUnlocked)
        assertNull(controller.opened)

        controller.start(sampleContent, sampleKey, sampleFingerprint, nowElapsed = 1000L)

        assertTrue(controller.isUnlocked)
        assertEquals(sampleContent, controller.opened)

        var accessedKey: String? = null
        var accessedFingerprint: ByteArray? = null

        val result = controller.withKeyAndBaseline { key, hash ->
            accessedKey = String(key)
            accessedFingerprint = hash.copyOf()
            "success"
        }

        assertEquals("success", result)
        assertEquals("master-password-123", accessedKey)
        assertArrayEquals(sampleFingerprint, accessedFingerprint)
    }

    @Test
    fun lockSafelyWipesSessionAndClearsClipboard() {
        controller.start(sampleContent, sampleKey, sampleFingerprint, nowElapsed = 1000L)
        assertTrue(controller.isUnlocked)

        controller.lock()

        assertFalse(controller.isUnlocked)
        assertNull(controller.opened)
        assertTrue(fakeClipboard.cleared)

        val result = controller.withKeyAndBaseline { _, _ -> "should-not-run" }
        assertNull(result)
    }

    @Test
    fun updateContentUpdatesVaultAndFingerprint() {
        controller.start(sampleContent, sampleKey, sampleFingerprint, nowElapsed = 1000L)

        val updatedContent = sampleContent.copy(name = "UpdatedVault")
        val updatedFingerprint = byteArrayOf(9, 8, 7, 6)

        controller.updateContent(updatedContent, updatedFingerprint)

        assertEquals("UpdatedVault", controller.opened?.name)
        controller.withKeyAndBaseline { _, hash ->
            assertArrayEquals(updatedFingerprint, hash)
        }
    }

    @Test
    fun checkTimeoutLocksVaultWhenInactivityExceeded() {
        controller.start(sampleContent, sampleKey, sampleFingerprint, nowElapsed = 1000L)

        // 300 seconds timeout. At 1000 + 100_000ms = 100s, not timed out
        val notTimedOut = controller.checkTimeout(nowElapsed = 101_000L, timeoutSeconds = 300)
        assertFalse(notTimedOut)
        assertTrue(controller.isUnlocked)

        // At 1000 + 301_000ms = 301s, timed out
        val timedOut = controller.checkTimeout(nowElapsed = 302_000L, timeoutSeconds = 300)
        assertTrue(timedOut)
        assertFalse(controller.isUnlocked)
        assertNull(controller.opened)
    }

    @Test
    fun handleStopLocksImmediatelyWhenTimeoutIsZeroAndNoPicker() {
        controller.start(sampleContent, sampleKey, sampleFingerprint, nowElapsed = 1000L)

        // If external picker is active, do not lock
        val pickerActive = controller.handleStop(timeoutSeconds = 0, isExternalPickerActive = true)
        assertFalse(pickerActive)
        assertTrue(controller.isUnlocked)

        // If no picker and timeout is 0, lock immediately
        val locked = controller.handleStop(timeoutSeconds = 0, isExternalPickerActive = false)
        assertTrue(locked)
        assertFalse(controller.isUnlocked)
    }
}
