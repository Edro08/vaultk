package com.edro08.vaultk

import android.content.ClipData
import android.content.ClipboardManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.edro08.vaultk.data.security.CredentialClipboard
import com.edro08.vaultk.data.security.SessionSecurity
import com.edro08.vaultk.application.security.VaultIdleTimer
import com.edro08.vaultk.data.settings.AppSettings
import com.edro08.vaultk.domain.settings.Appearance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSecurityTest {
    @Test fun settingsPersistBetweenInstancesAndRejectInvalidClipboardDeadline() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val session = SessionSecurity(context)
        val settings = AppSettings(context)
        val oldClipboard = session.clipboardSeconds
        val oldAppearance = settings.appearance
        val oldSearch = settings.searchNotes
        val oldDetails = settings.showDetails
        try {
            session.clipboardSeconds = 60
            settings.appearance = Appearance.System
            settings.searchNotes = false
            settings.showDetails = false
            assertEquals(60, SessionSecurity(context).clipboardSeconds)
            assertEquals(Appearance.System, AppSettings(context).appearance)
            assertFalse(AppSettings(context).searchNotes)
            assertFalse(AppSettings(context).showDetails)
            assertTrue(runCatching { session.clipboardSeconds = 0 }.isFailure)
        } finally {
            session.clipboardSeconds = oldClipboard
            settings.appearance = oldAppearance
            settings.searchNotes = oldSearch
            settings.showDetails = oldDetails
        }
    }

    @Test fun timeoutIsPersistedAndRestrictedToValidChoices() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = SessionSecurity(context)
        val previous = settings.timeoutSeconds
        try {
            assertEquals(listOf(0, 30, 60, 180, 300), SessionSecurity.TIMEOUTS)
            settings.timeoutSeconds = 0
            assertEquals(0, SessionSecurity(context).timeoutSeconds)
            settings.timeoutSeconds = 30
            assertEquals(30, SessionSecurity(context).timeoutSeconds)
            assertFalse(SessionSecurity.hasTimedOut(10_000, 39_999, 30))
            assertTrue(SessionSecurity.hasTimedOut(10_000, 40_000, 30))
            assertTrue(runCatching { settings.timeoutSeconds = 120 }.isFailure)
            assertTrue(runCatching { settings.timeoutSeconds = 900 }.isFailure)
            context.getSharedPreferences("session_security", android.content.Context.MODE_PRIVATE)
                .edit().putInt("timeout_seconds", 900).commit()
            assertEquals(300, SessionSecurity(context).timeoutSeconds)
        } finally { settings.timeoutSeconds = previous }
    }

    @Test fun idleTimeoutStartsAfterOpeningNotWhileWaitingForUnlock() {
        val timer = VaultIdleTimer()
        timer.interact(1_000)
        assertFalse(timer.hasTimedOut(91_000, 30))
        timer.start(91_000) // Simulate an unlock that took longer than both short intervals.
        assertFalse(timer.hasTimedOut(91_000, 30))
        assertFalse(timer.hasTimedOut(120_999, 30))
        assertTrue(timer.hasTimedOut(121_000, 30))
        timer.interact(121_000)
        assertFalse(timer.hasTimedOut(180_999, 60))
        assertTrue(timer.hasTimedOut(181_000, 60))
        timer.stop()
        assertFalse(timer.hasTimedOut(300_000, 30))
    }

    @Test fun clipboardClearsOnlyItsOwnLatestCopy() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario -> scenario.onActivity { context ->
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            CredentialClipboard.copy(context, "test", "secret", clearAfterMillis = 5_000)
            assertEquals("secret", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
            clipboard.setPrimaryClip(ClipData.newPlainText("externo", "valor ajeno"))
            CredentialClipboard.clearOwned(context)
            assertEquals("valor ajeno", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
            CredentialClipboard.copy(context, "test", "otro secreto", clearAfterMillis = 5_000)
            CredentialClipboard.clearOwned(context)
            assertTrue(clipboard.primaryClip?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.text?.toString() != "otro secreto")
        } }
    }

    @Test fun clipboardAutomaticallyClearsCopiedCredential() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { context ->
                CredentialClipboard.copy(context, "test", "temporal", clearAfterMillis = 100)
            }
            var cleared = false
            repeat(30) {
                Thread.sleep(100)
                scenario.onActivity { context ->
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    cleared = clipboard.primaryClip?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)?.text?.toString() != "temporal"
                }
                if (cleared) return@use
            }
            assertTrue(cleared)
        }
    }
}
