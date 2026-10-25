package com.edro08.vaultk.data

import android.content.Context
import com.edro08.vaultk.domain.CredentialClipboardPort

class AndroidCredentialClipboard(private val context: Context) : CredentialClipboardPort {
    override fun copy(label: String, value: String, sensitive: Boolean) =
        CredentialClipboard.copy(context, label, value, sensitive)
    override fun clearOwned() = CredentialClipboard.clearOwned(context)
}
