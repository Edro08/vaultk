package com.edro08.vaultk.data

import com.edro08.vaultk.domain.InvalidVaultFileException
import com.edro08.vaultk.domain.InvalidVaultKeyException
import com.edro08.vaultk.domain.VaultContent
import com.edro08.vaultk.domain.VaultAttachment
import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.domain.VaultGroup
import com.edro08.vaultk.domain.VaultValue
import com.edro08.vaultk.domain.groupsAndSelf
import org.linguafranca.pwdb.Group
import org.linguafranca.pwdb.Entry
import org.linguafranca.pwdb.format.KdbxCredentials
import org.linguafranca.pwdb.format.KdbxStreamFormat
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase
import java.io.InputStream
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets

/** Parse only; never save to or modify the SAF document. Call from a background dispatcher. */
class KdbxReader {
    fun read(input: InputStream, password: CharArray): VaultContent {
        val database = load(input, password)
        val root = database.rootGroup.snapshot()
        // getRecycleBin() can CREATE a group if the file only declares it enabled;
        // expose the bin only when it was actually present in the loaded snapshot.
        val binId = database.recycleBin?.uuid?.toString()?.takeIf { id ->
            root.groupsAndSelf().any { it.id == id }
        }
        return VaultContent(database.name.orEmpty(), root, binId)
    }

    fun load(input: InputStream, password: CharArray): KdbxDatabase {
        val encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password))
        val bytes = ByteArray(encoded.remaining())
        encoded.get(bytes)
        if (encoded.hasArray()) encoded.array().fill(0)
        val header = VaultKdbxHeader()
        try {
            val credentials = KdbxCredentials(bytes)
            val database = try {
                KdbxDatabase.load(KdbxStreamFormat(header), credentials, input)
            } catch (error: IllegalStateException) {
                val message = error.message.orEmpty()
                if (message.contains("Inconsistent stream start bytes") || message.contains("Header HMAC does not match")) {
                    throw InvalidVaultKeyException(error)
                }
                throw InvalidVaultFileException(error)
            }
            return database
        } finally {
            header.clearDerivedKey()
            bytes.fill(0)
        }
    }

    private fun Group.snapshot(): VaultGroup = VaultGroup(
        id = uuid.toString(),
        name = name.orEmpty(),
        groups = groups.map { it.snapshot() },
        entries = entries.map { entry ->
            VaultEntry(
                id = entry.uuid.toString(),
                title = entry.title.orEmpty(),
                username = entry.username.orEmpty(),
                url = entry.url.orEmpty(),
                notes = entry.notes.orEmpty(),
                modified = entry.lastModificationTime?.toInstant(),
                password = entry.protectedValue(Entry.STANDARD_PROPERTY_NAME_PASSWORD),
                otp = entry.protectedValue("otp"),
                tags = entry.kdbxTags(),
                created = entry.creationTime?.toInstant(),
                expiresAt = if (entry.expires) entry.expiryTime?.toInstant() else null,
                customFields = entry.propertyNames.filterNot { name ->
                    name in setOf("Title", "UserName", "Password", "URL", "Notes", "otp", "Tags")
                }.associateWith { name ->
                    val value = entry.getPropertyValue(name)
                    val chars = value.valueAsChars
                    try {
                        KdbxVaultValueFactory().create(chars, value.isProtected)
                    } finally { chars.fill('\u0000') }
                },
                attachments = entry.binaryPropertyNames.map { name ->
                    VaultAttachment(name, bytes = entry.getBinaryProperty(name))
                },
            )
        },
    )

    private fun Entry.protectedValue(name: String): VaultValue? {
        val value = getPropertyValue(name) ?: return null
        val chars = value.valueAsChars
        return try {
            when {
                chars.isEmpty() -> null
                value.isProtected -> KdbxVaultValue(value)
                else -> KdbxVaultValueFactory().create(chars, true)
            }
        } finally { chars.fill('\u0000') }
    }

    private fun Entry.kdbxTags(): List<String> {
        // KeePassJava2 maps the KDBX <Tags> element but exposes no public accessor.
        val raw = runCatching {
            javaClass.getDeclaredField("tags").apply { isAccessible = true }.get(this) as? String
        }.getOrNull() ?: getProperty("Tags").orEmpty()
        return raw.split(';').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
