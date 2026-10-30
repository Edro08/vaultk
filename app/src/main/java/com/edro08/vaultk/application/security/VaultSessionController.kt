package com.edro08.vaultk.application.security

import com.edro08.vaultk.domain.vault.VaultContent

/**
 * Coordinates in-memory vault session lifecycle, volatile master key retention,
 * deterministic memory wiping, and inactivity auto-lock.
 */
class VaultSessionController(
    private val copyCredential: CopyCredential,
    val idleTimer: VaultIdleTimer = VaultIdleTimer()
) {
    var opened: VaultContent? = null
        private set

    private var sessionKey: CharArray? = null
    private var baseline: ByteArray? = null

    val isUnlocked: Boolean
        get() = opened != null

    /**
     * Starts an authenticated session. Securely clones the master key and fingerprint,
     * starts the inactivity timer, and sets the active vault content.
     */
    fun start(content: VaultContent, key: CharArray, fingerprint: ByteArray, nowElapsed: Long) {
        lock()
        sessionKey = key.copyOf()
        baseline = fingerprint.copyOf()
        idleTimer.start(nowElapsed)
        opened = content
    }

    /**
     * Updates content and baseline after a successful local save or remote reload.
     */
    fun updateContent(content: VaultContent, newFingerprint: ByteArray) {
        baseline?.fill(0)
        baseline = newFingerprint.copyOf()
        opened = content
    }

    /**
     * Safely closes the session: wipes secrets and fingerprints from memory with zeros,
     * clears owned clipboard credentials, and stops the idle timer.
     */
    fun lock() {
        idleTimer.stop()
        sessionKey?.fill('\u0000')
        sessionKey = null
        baseline?.fill(0)
        baseline = null
        opened = null
        copyCredential.clearOwned()
    }

    /** Resets the inactivity timer to the current timestamp. */
    fun touch(nowElapsed: Long) {
        if (isUnlocked) {
            idleTimer.start(nowElapsed)
        }
    }

    /**
     * Checks if the session has expired due to inactivity. If timed out, locks the vault.
     * @return true if the session timed out and was locked.
     */
    fun checkTimeout(nowElapsed: Long, timeoutSeconds: Int): Boolean {
        if (isUnlocked && idleTimer.hasTimedOut(nowElapsed, timeoutSeconds)) {
            lock()
            return true
        }
        return false
    }

    /**
     * Handles app moving to background / ON_STOP.
     * If timeoutSeconds == 0 (immediate lock) and no external picker is active, locks the vault.
     * @return true if locked.
     */
    fun handleStop(timeoutSeconds: Int, isExternalPickerActive: Boolean): Boolean {
        if (isUnlocked && timeoutSeconds == 0 && !isExternalPickerActive) {
            lock()
            return true
        }
        return false
    }

    /**
     * Executes a block with a safe temporary clone of the master key.
     * Guarantees zero-filling of the clone immediately in a finally block.
     */
    fun <T> withKey(block: (CharArray) -> T): T? {
        val key = sessionKey?.copyOf() ?: return null
        return try {
            block(key)
        } finally {
            key.fill('\u0000')
        }
    }

    /**
     * Executes a block with temporary clones of both master key and fingerprint baseline.
     * Guarantees zero-filling of both clones immediately in a finally block.
     */
    fun <T> withKeyAndBaseline(block: (CharArray, ByteArray) -> T): T? {
        val key = sessionKey?.copyOf() ?: return null
        val hash = baseline?.copyOf()
        if (hash == null) {
            key.fill('\u0000')
            return null
        }
        return try {
            block(key, hash)
        } finally {
            key.fill('\u0000')
            hash.fill(0)
        }
    }
}
