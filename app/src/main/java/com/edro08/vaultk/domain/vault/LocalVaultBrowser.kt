package com.edro08.vaultk.domain.vault

data class LocalVaultItem(val id: VaultId, val name: String, val parent: VaultId?)
data class LocalVaultListing(val directories: List<LocalVaultItem>, val vaults: List<LocalVaultItem>)

interface LocalVaultBrowser {
    fun permitted(): Boolean
    fun canRead(id: VaultId): Boolean
    val root: VaultId
    fun parentOf(directory: VaultId): VaultId
    fun list(directory: VaultId): LocalVaultListing
    fun find(query: String): List<LocalVaultItem>
}
