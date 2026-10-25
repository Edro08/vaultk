package com.edro08.vaultk.domain

interface CredentialClipboardPort {
    fun copy(label: String, value: String, sensitive: Boolean)
    fun clearOwned()
}
