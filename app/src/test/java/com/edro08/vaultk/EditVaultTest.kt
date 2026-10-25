package com.edro08.vaultk

import com.edro08.vaultk.application.CustomFieldDraft
import com.edro08.vaultk.application.EditVault
import com.edro08.vaultk.application.EntryFormData
import com.edro08.vaultk.data.KdbxVaultValueFactory
import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.domain.VaultGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EditVaultTest {
    private val time = Instant.parse("2026-01-02T00:00:00Z")
    private val editor = EditVault(KdbxVaultValueFactory(), now = { time }, newId = { "draft:new" })
    private val entry = VaultEntry("existing", "Before", "user", "", "", time,
        created = time.minusSeconds(60))
    private val root = VaultGroup("root", "Root", listOf(
        VaultGroup("source", "Source", emptyList(), listOf(entry)),
        VaultGroup("other", "Other", emptyList(), emptyList()),
        VaultGroup("bin", "Recycle Bin", emptyList(), emptyList())), emptyList())

    @Test fun upsertPreservesIdentityAndCreatesProtectedFields() {
        val form = EntryFormData("existing", "After", "user", "secret", "", "notes",
            emptyList(), null, "", "1234", listOf(CustomFieldDraft("private", "value", true)), emptyList())
        val result = editor.upsert(root, "source", form, entry)
        assertEquals("existing", result.entry.id)
        assertEquals("After", result.root.groups.first().entries.single().title)
        assertEquals(entry.created, result.entry.created)
        assertTrue(result.entry.password!!.isProtected)
        assertTrue(result.entry.customFields.getValue("private").isProtected)
        assertNotNull(result.entry.customFields["PIN"])
    }

    @Test fun duplicateMoveAndTrashLeaveOtherEntriesUntouched() {
        val duplicated = editor.duplicate(root, "source", entry)
        assertEquals("draft:new", duplicated.entry.id)
        assertEquals(2, duplicated.root.groups.first().entries.size)
        val moved = editor.move(root, "source", "other", entry.id)
        assertTrue(moved.groups.first().entries.isEmpty())
        assertEquals("existing", moved.groups[1].entries.single().id)
        val trashed = editor.delete(root, "source", entry.id, "bin")
        assertEquals("existing", trashed.groups[2].entries.single().id)
        assertTrue(editor.delete(trashed, "bin", entry.id, "bin").groups[2].entries.isEmpty())
    }

    @Test fun createGroupAndSearch() {
        val (updated, id) = editor.createGroup(root, "source", "New")
        assertEquals("draft:new", id)
        assertEquals("New", updated.groups.first().groups.single().name)
        assertEquals("existing", editor.search(root, "Before", false).single().entry.id)
    }
}
