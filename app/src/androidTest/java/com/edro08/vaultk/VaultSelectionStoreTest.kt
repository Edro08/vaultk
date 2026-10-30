package com.edro08.vaultk

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.edro08.vaultk.domain.settings.VaultDocument
import com.edro08.vaultk.application.vault.ManageRecentVaults
import com.edro08.vaultk.data.settings.VaultSelectionStore
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSelectionStoreTest {
    @Test fun unavailableDocumentRemainsVisibleAndCanBeRelocatedOrRemoved() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val isolated = "vault_selection_test_${UUID.randomUUID()}"
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
                base.getSharedPreferences(isolated, mode)
        }
        try {
            val store = ManageRecentVaults(VaultSelectionStore(context))
            val old = VaultDocument("content://missing.provider/vault.kdbx",
                "Personal.kdbx", 100, 80)
            store.select(old)
            assertEquals(old, store.recent().single()) // No persisted URI grant.
            assertEquals(old, VaultSelectionStore(context).current())
            val moved = store.relocate(old.reference, VaultDocument("content://new.provider/vault.kdbx",
                "Renombrado.kdbx", 200))
            assertNull(moved.openedAt)
            assertEquals(listOf(moved), VaultSelectionStore(context).recent())
            assertEquals(moved, store.current())
            val opened = store.markOpened(moved)
            assertTrue(opened.openedAt!! > 80)
            assertEquals(opened.openedAt, store.recent().single().openedAt)
            store.remove(moved.reference)
            assertTrue(store.recent().isEmpty())
            assertNull(VaultSelectionStore(context).current())
        } finally { base.deleteSharedPreferences(isolated) }
    }

    @Test fun removingAnotherRecentKeepsCurrent() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val isolated = "vault_selection_test_${UUID.randomUUID()}"
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
                base.getSharedPreferences(isolated, mode)
        }
        try {
            val store = ManageRecentVaults(VaultSelectionStore(context))
            val previous = VaultDocument("file:///storage/emulated/0/Documents/A.kdbx", "A.kdbx", 1)
            val current = VaultDocument("file:///storage/emulated/0/Download/B.kdbx", "B.kdbx", 2)
            store.select(previous)
            store.select(current)
            store.remove(previous.reference)
            assertEquals(listOf(current), store.recent())
            assertEquals(current, store.current())
        } finally { base.deleteSharedPreferences(isolated) }
    }
}
