package com.edro08.vaultk.application.security

import com.edro08.vaultk.domain.security.CredentialClipboardPort

class CopyCredential(private val clipboard: CredentialClipboardPort) {
    operator fun invoke(label: String, value: String, sensitive: Boolean = true) =
        clipboard.copy(label, value, sensitive)
    fun clearOwned() = clipboard.clearOwned()
}
