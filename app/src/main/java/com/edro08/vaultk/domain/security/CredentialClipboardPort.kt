package com.edro08.vaultk.domain.security

interface CredentialClipboardPort {
    fun copy(label: String, value: String, sensitive: Boolean)
    fun clearOwned()
}
