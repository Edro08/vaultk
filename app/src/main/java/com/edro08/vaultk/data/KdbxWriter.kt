package com.edro08.vaultk.data

import com.edro08.vaultk.domain.VaultContent
import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.domain.VaultGroup
import com.edro08.vaultk.domain.VaultValue
import org.linguafranca.pwdb.Entry
import org.linguafranca.pwdb.Group
import org.linguafranca.pwdb.PropertyValue
import org.linguafranca.pwdb.format.KdbxStreamFormat
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.Date

/** Produces a complete encrypted replacement; the caller commits it through LocalVaultTransaction. */
class KdbxWriter(private val reader: KdbxReader = KdbxReader()) {
    fun prepare(input: InputStream, key: CharArray, original: VaultContent,
        changed: VaultContent, onTiming: (String, Long) -> Unit = { _, _ -> },
        attachmentInput: (android.net.Uri) -> InputStream): ByteArray {
        val loadStarted = System.nanoTime()
        val database = reader.load(input, key)
        onTiming("readOriginal", (System.nanoTime() - loadStarted) / 1_000_000)
        require(database.rootGroup.uuid.toString() == original.root.id) { "Vault changed since unlock" }
        val initial = original.root.allEntries().associateBy { it.id }
        require(changed.root.id == original.root.id && changed.recycleBinId == original.recycleBinId)
        val groups = mutableMapOf<String, Group>()
        fun applyGroups(source: VaultGroup, target: Group) {
            groups[source.id] = target
            source.groups.forEach { child ->
                val existing = target.groups.firstOrNull { it.uuid.toString() == child.id }
                val group = existing?.also {
                    if (it.name != child.name) {
                        it.name = child.name
                    }
                } ?: run {
                    require(child.id.startsWith("draft:")) { "Missing original group" }
                    target.addGroup(child.name)
                }
                applyGroups(child, group)
            }
        }
        if (changed.root.name.isNotBlank() && database.rootGroup.name != changed.root.name) {
            database.rootGroup.name = changed.root.name
        }
        applyGroups(changed.root, database.rootGroup)
        val desired = mutableMapOf<String, Pair<Group, VaultEntry>>()
        fun collect(source: VaultGroup) {
            source.entries.forEach { item ->
                require(desired.put(item.id, groups.getValue(source.id) to item) == null) {
                    "Duplicate entry UUID"
                }
            }
            source.groups.forEach(::collect)
        }
        collect(changed.root)
        fun originalEntries(group: Group): List<Pair<Group, Entry>> =
            group.entries.map { group to it } + group.groups.flatMap(::originalEntries)
        // A missing entry is an explicit permanent deletion. Moves retain the original
        // KDBX object (UUID, icon, history, timestamps and other metadata).
        val existing = originalEntries(database.rootGroup).associateBy { it.second.uuid.toString() }
        existing.forEach { (id, location) ->
            if (id !in desired) location.first.removeEntry(location.second)
        }
        desired.forEach { (id, pair) ->
            val (target, item) = pair
            val old = existing[id]
            if (old == null && !id.startsWith("draft:")) error("Missing original entry")
            val dest = if (old == null) target.addEntry(item.title) else {
                if (old.first !== target) {
                    // KeePass uses this UUID as the suggested restore location in the Recycle Bin.
                    if (target.uuid.toString() == original.recycleBinId) {
                        old.second.javaClass.getDeclaredField("previousParentGroup")
                            .apply { isAccessible = true }.set(old.second, old.first.uuid)
                    }
                    old.first.removeEntry(old.second)
                    target.addEntry(old.second)
                }
                old.second
            }
            if (old == null || initial[id] !== item) updateEntry(dest, item, attachmentInput)
        }
        val output = ByteArrayOutputStream()
        // Keep the original KDF cost and format, but use fresh salts/IVs for each save.
        val header = (database.streamFormat as KdbxStreamFormat).streamConfiguration as VaultKdbxHeader
        header.rotateForSave()
        val encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(key))
        val keyBytes = ByteArray(encoded.remaining())
        encoded.get(keyBytes)
        if (encoded.hasArray()) encoded.array().fill(0)
        try {
            val credentials = org.linguafranca.pwdb.format.KdbxCredentials(keyBytes)
            val writeStarted = System.nanoTime()
            database.save(credentials, output)
            onTiming("encryptNew", (System.nanoTime() - writeStarted) / 1_000_000)
        } finally {
            header.clearDerivedKey()
            keyBytes.fill(0)
        }
        // LocalVaultTransaction decrypts the staged bytes before replacing the original.
        // Decrypting this in-memory copy here would repeat the vault's expensive KDF.
        return output.toByteArray()
    }

    private fun updateEntry(dest: Entry, source: VaultEntry,
        attachmentInput: (android.net.Uri) -> InputStream) {
        dest.title = source.title
        dest.username = source.username
        dest.url = source.url
        dest.notes = source.notes
        fun put(name: String, value: VaultValue?) {
            if (value == null) dest.removeProperty(name) else dest.setPropertyValue(name, value.toPropertyValue())
        }
        // KeePassJava2 requires standard fields to exist even when blank.
        dest.setPropertyValue("Password", source.password?.toPropertyValue() ?: PropertyValue.SealedStore(charArrayOf()))
        put("otp", source.otp)
        val reserved = setOf("Title", "UserName", "Password", "URL", "Notes", "otp", "Tags")
        dest.propertyNames.filter { it !in reserved && it !in source.customFields }
            .forEach { dest.removeProperty(it) }
        source.customFields.forEach { (name, value) -> put(name, value) }
        dest.setExpires(source.expiresAt != null)
        source.expiresAt?.let { dest.expiryTime = Date.from(it) }
        // KeePassJava2 parses <Tags> but does not provide a setter.
        dest.javaClass.getDeclaredField("tags").apply { isAccessible = true }
            .set(dest, source.tags.joinToString(";"))
        dest.binaryPropertyNames.filterNot { name -> source.attachments.any { it.name == name } }
            .forEach { dest.removeBinaryProperty(it) }
        source.attachments.forEach { attachment ->
            val binary = attachment.bytes ?: attachment.uri?.let { attachmentInput(android.net.Uri.parse(it)).use(InputStream::readBytes) }
                ?: error("Unavailable attachment")
            dest.setBinaryProperty(attachment.name, binary)
        }
    }

    private fun VaultGroup.allEntries(): List<VaultEntry> = entries + groups.flatMap { it.allEntries() }
}
