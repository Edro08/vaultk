package com.edro08.vaultk.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.edro08.vaultk.application.CopyCredential

/** Provided by the composition root, not constructed inside a screen. */
val LocalCopyCredential = staticCompositionLocalOf<CopyCredential?> { null }
