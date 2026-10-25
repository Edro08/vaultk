package com.edro08.vaultk.application

import com.edro08.vaultk.domain.VaultAttachment
import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.domain.VaultGroup
import com.edro08.vaultk.domain.VaultSearchResult
import com.edro08.vaultk.domain.VaultValue
import com.edro08.vaultk.domain.VaultValueFactory
import com.edro08.vaultk.domain.insertEntry
import com.edro08.vaultk.domain.isInside
import com.edro08.vaultk.domain.moveEntry
import com.edro08.vaultk.domain.removeEntry
import com.edro08.vaultk.domain.replaceEntry
import com.edro08.vaultk.domain.searchEntries
import com.edro08.vaultk.domain.withChild
import java.time.Instant
import java.util.UUID

/** Values sent by the entry form; converting them to vault entries is an application action. */
data class EntryFormData(
    val id: String,
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val tags: List<String>,
    val expiresAt: Instant?,
    val otp: String,
    val pin: String,
    val fields: List<CustomFieldDraft>,
    val attachments: List<VaultAttachment>,
)

data class CustomFieldDraft(val name: String, val value: String, val isProtected: Boolean)

data class EntryEditResult(val root: VaultGroup, val entry: VaultEntry)

class EditVault(private val values: VaultValueFactory,
    private val now: () -> Instant = Instant::now,
    private val newId: () -> String = { "draft:${UUID.randomUUID()}" }) {
    fun search(root: VaultGroup, query: String, includeNotes: Boolean): List<VaultSearchResult> =
        root.searchEntries(query, includeNotes)

    fun upsert(root: VaultGroup, groupId: String, form: EntryFormData,
        previous: VaultEntry?): EntryEditResult {
        fun protected(value: String): VaultValue? {
            if (value.isEmpty()) return null
            val chars = value.toCharArray()
            return try { values.create(chars, true) } finally { chars.fill('\u0000') }
        }
        val fields = form.fields.associate { field ->
            val chars = field.value.toCharArray()
            field.name to try {
                values.create(chars, field.isProtected)
            } finally { chars.fill('\u0000') }
        }.toMutableMap()
        protected(form.pin)?.let { fields["PIN"] = it }
        val time = now()
        val entry = VaultEntry(form.id, form.title, form.username, form.url, form.notes, time,
            protected(form.password), protected(form.otp), form.tags, previous?.created ?: time,
            form.expiresAt, fields, form.attachments)
        return EntryEditResult(if (previous == null) root.insertEntry(groupId, entry)
            else root.replaceEntry(groupId, entry), entry)
    }

    fun duplicate(root: VaultGroup, groupId: String, entry: VaultEntry): EntryEditResult {
        val time = now()
        val copy = entry.copy(id = newId(), created = time, modified = time)
        return EntryEditResult(root.insertEntry(groupId, copy), copy)
    }

    fun createGroup(root: VaultGroup, parentId: String, name: String): Pair<VaultGroup, String> {
        val id = newId()
        return root.withChild(parentId, VaultGroup(id, name, emptyList(), emptyList())) to id
    }

    fun move(root: VaultGroup, sourceId: String, destinationId: String, entryId: String): VaultGroup =
        root.moveEntry(sourceId, destinationId, entryId)

    fun delete(root: VaultGroup, groupId: String, entryId: String, recycleBinId: String?): VaultGroup =
        if (recycleBinId != null && !root.isInside(groupId, recycleBinId))
            root.moveEntry(groupId, recycleBinId, entryId)
        else root.removeEntry(groupId, entryId)
}
