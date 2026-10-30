package com.edro08.vaultk.application.vault

import com.edro08.vaultk.domain.vault.LocalVaultBrowser
import com.edro08.vaultk.domain.vault.LocalVaultItem
import com.edro08.vaultk.domain.vault.LocalVaultListing
import com.edro08.vaultk.domain.vault.VaultId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrowseLocalVaults(private val browser: LocalVaultBrowser) {
    fun permitted(): Boolean = browser.permitted()
    fun canRead(id: VaultId): Boolean = browser.canRead(id)
    val root: VaultId get() = browser.root
    fun parentOf(directory: VaultId): VaultId = browser.parentOf(directory)
    suspend fun list(directory: VaultId, query: String): LocalVaultListing = withContext(Dispatchers.IO) {
        if (query.isBlank()) browser.list(directory)
        else LocalVaultListing(emptyList(), browser.find(query.trim()))
    }
}
