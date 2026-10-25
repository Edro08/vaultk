package com.edro08.vaultk.application

import com.edro08.vaultk.domain.VaultDocument
import com.edro08.vaultk.domain.VaultSelection

class ManageRecentVaults(private val selection: VaultSelection) {
    fun current(): VaultDocument? = selection.current()
    fun recent(): List<VaultDocument> = selection.recent()
    fun select(document: VaultDocument) = selection.select(document)
    fun markOpened(document: VaultDocument): VaultDocument = selection.markOpened(document)
    fun relocate(oldReference: String, replacement: VaultDocument): VaultDocument =
        selection.relocate(oldReference, replacement)
    fun remove(reference: String) = selection.remove(reference)
    fun hasReadPermission(reference: String): Boolean = selection.hasReadPermission(reference)
}
