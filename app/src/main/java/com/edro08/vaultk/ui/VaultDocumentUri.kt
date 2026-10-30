package com.edro08.vaultk.ui

import android.net.Uri
import com.edro08.vaultk.domain.settings.VaultDocument

/** Android presentation boundary for persisted document references. */
val VaultDocument.uri: Uri get() = Uri.parse(reference)
