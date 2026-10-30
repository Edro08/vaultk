package com.edro08.vaultk.data.settings

import android.content.Context
import android.net.Uri
import com.edro08.vaultk.domain.settings.VaultDocument
import com.edro08.vaultk.domain.settings.VaultSelection
import org.json.JSONArray
import org.json.JSONObject

/** Only document references and display metadata are saved here, never credentials. */
class VaultSelectionStore(context: Context) : VaultSelection {
    private val resolver = context.contentResolver
    private val preferences = context.getSharedPreferences("vault_selection", Context.MODE_PRIVATE)

    override fun current(): VaultDocument? {
        val selected = preferences.getString("current_uri", null) ?: return null
        return recent().firstOrNull { it.reference == selected }
    }

    override fun recent(): List<VaultDocument> {
        val array = runCatching { JSONArray(preferences.getString("recent", "[]")) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                val item = array.getJSONObject(index)
                VaultDocument(item.getString("uri"), item.getString("name"), item.getLong("selectedAt"),
                    item.optLong("openedAt").takeIf { it > 0 })
            }.getOrNull()?.takeIf {
                Uri.parse(it.reference).scheme in listOf("content", "file") && it.name.endsWith(".kdbx", ignoreCase = true)
            }
        }
    }

    override fun select(document: VaultDocument) {
        val previous = recent()
        val updated = document.copy(openedAt = document.openedAt ?: previous.firstOrNull { it.reference == document.reference }?.openedAt)
        val records = (listOf(updated) + previous.filterNot { it.reference == document.reference }).take(10)
        persist(records, updated.reference)
    }

    override fun markOpened(document: VaultDocument): VaultDocument {
        val now = System.currentTimeMillis()
        return document.copy(selectedAt = now, openedAt = now).also(::select)
    }

    /** Replacing a moved document does not transfer a URI-bound biometric key. */
    override fun relocate(oldReference: String, replacement: VaultDocument): VaultDocument {
        val previous = recent()
        val old = previous.firstOrNull { it.reference == oldReference }
        // A new URI has not yet been opened or verified as the same vault.
        val updated = replacement.copy(openedAt = old?.openedAt?.takeIf { oldReference == replacement.reference })
        persist((listOf(updated) + previous.filterNot { it.reference == oldReference || it.reference == updated.reference }).take(10),
            updated.reference)
        return updated
    }

    override fun remove(reference: String) {
        val currentUri = preferences.getString("current_uri", null)
        persist(recent().filterNot { it.reference == reference },
            currentUri?.takeUnless { it == reference })
    }

    private fun persist(records: List<VaultDocument>, currentUri: String?) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(JSONObject().put("uri", record.reference).put("name", record.name)
                .put("selectedAt", record.selectedAt).apply { record.openedAt?.let { put("openedAt", it) } })
        }
        preferences.edit().putString("recent", array.toString())
            .putString("current_uri", currentUri?.toString()).apply()
    }

    override fun hasReadPermission(reference: String): Boolean {
        val uri = Uri.parse(reference)
        return if (uri.scheme == "file") LocalVaultFiles().canRead(uri)
        else resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
    }
}
