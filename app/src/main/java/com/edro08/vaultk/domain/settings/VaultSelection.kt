package com.edro08.vaultk.domain.settings

/** Reference and display metadata only; never credentials or platform objects. */
data class VaultDocument(val reference: String, val name: String, val selectedAt: Long,
    val openedAt: Long? = null)

interface VaultSelection {
    fun current(): VaultDocument?
    fun recent(): List<VaultDocument>
    fun select(document: VaultDocument)
    fun markOpened(document: VaultDocument): VaultDocument
    fun relocate(oldReference: String, replacement: VaultDocument): VaultDocument
    fun remove(reference: String)
    fun hasReadPermission(reference: String): Boolean
}
