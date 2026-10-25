package com.edro08.vaultk.data

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import java.util.UUID
import com.edro08.vaultk.domain.SecurityOptions

class SessionSecurity(context: Context) : com.edro08.vaultk.domain.SecurityPreferenceStore {
    private val preferences = context.getSharedPreferences("session_security", Context.MODE_PRIVATE)

    override var timeoutSeconds: Int
        get() = preferences.getInt("timeout_seconds", 300).takeIf { it in TIMEOUTS } ?: 300
        set(value) {
            require(value in TIMEOUTS)
            preferences.edit().putInt("timeout_seconds", value).apply()
        }

    override var clipboardSeconds: Int
        get() = preferences.getInt("clipboard_seconds", 30).takeIf { it in CLIPBOARD_TIMEOUTS } ?: 30
        set(value) {
            require(value in CLIPBOARD_TIMEOUTS)
            preferences.edit().putInt("clipboard_seconds", value).apply()
        }

    companion object {
        val TIMEOUTS = SecurityOptions.lockTimeoutSeconds
        val CLIPBOARD_TIMEOUTS = SecurityOptions.clipboardTimeoutSeconds
        fun hasTimedOut(lastInteractionMillis: Long, nowMillis: Long, timeoutSeconds: Int): Boolean =
            nowMillis - lastInteractionMillis >= timeoutSeconds * 1000L
    }
}

/** Clears only a clip this process installed; never replaces a newer clip from another app. */
object CredentialClipboard {
    private const val TOKEN = "com.edro08.vaultk.clip_token"
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var currentToken: String? = null

    fun copy(context: Context, label: String, value: String, sensitive: Boolean = true,
        clearAfterMillis: Long = SessionSecurity(context).clipboardSeconds * 1000L) {
        val manager = context.getSystemService(ClipboardManager::class.java)
        val token = UUID.randomUUID().toString()
        val clip = ClipData.newPlainText(label, value)
        clip.description.extras = PersistableBundle().apply {
            putString(TOKEN, token)
            if (sensitive) putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
        manager.setPrimaryClip(clip)
        pending?.let(handler::removeCallbacks)
        currentToken = token
        pending = Runnable { clearOwned(context) }.also { handler.postDelayed(it, clearAfterMillis) }
    }

    fun clearOwned(context: Context) {
        pending?.let(handler::removeCallbacks)
        pending = null
        val token = currentToken ?: return
        currentToken = null
        val manager = context.getSystemService(ClipboardManager::class.java)
        try {
            if (manager.primaryClip?.description?.extras?.getString(TOKEN) == token) manager.clearPrimaryClip()
        } catch (_: SecurityException) { /* Clipboard access may be unavailable when the app is backgrounded. */ }
    }
}
