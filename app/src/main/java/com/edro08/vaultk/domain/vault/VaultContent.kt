package com.edro08.vaultk.domain.vault

import java.time.Instant

/** Session snapshot. Never contains a master key; secrets remain in memory only while unlocked. */
data class VaultContent(val name: String, val root: VaultGroup, val recycleBinId: String? = null)

data class VaultGroup(
    val id: String,
    val name: String,
    val groups: List<VaultGroup>,
    val entries: List<VaultEntry>,
)

data class VaultEntry(
    val id: String,
    val title: String,
    val username: String,
    val url: String,
    val notes: String,
    val modified: Instant?,
    val password: VaultValue? = null,
    val otp: VaultValue? = null,
    val tags: List<String> = emptyList(),
    val created: Instant? = null,
    val expiresAt: Instant? = null,
    val customFields: Map<String, VaultValue> = emptyMap(),
    val attachments: List<VaultAttachment> = emptyList(),
)

/** Existing KDBX binary or a session-only SAF document; edits are not written to disk yet. */
data class VaultAttachment(
    val name: String,
    val uri: String? = null,
    val mimeType: String = "application/octet-stream",
    val bytes: ByteArray? = null,
)

class InvalidVaultKeyException(cause: Throwable) : Exception(cause)
class InvalidVaultFileException(cause: Throwable) : Exception(cause)
