package com.edro08.vaultk.data.security

import android.content.Context
import com.edro08.vaultk.domain.security.CredentialClipboardPort

class AndroidCredentialClipboard(private val context: Context) : CredentialClipboardPort {
    override fun copy(label: String, value: String, sensitive: Boolean) =
        CredentialClipboard.copy(context, label, value, sensitive)
    override fun clearOwned() = CredentialClipboard.clearOwned(context)
    override fun sweep() = CredentialClipboard.sweep(context)
}
