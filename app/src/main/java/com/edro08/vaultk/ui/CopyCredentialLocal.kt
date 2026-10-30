package com.edro08.vaultk.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.edro08.vaultk.application.security.CopyCredential

/** Provided by the composition root, not constructed inside a screen. */
val LocalCopyCredential = staticCompositionLocalOf<CopyCredential?> { null }
