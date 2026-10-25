package com.edro08.vaultk.application

/** Tracks an open session using monotonic timestamps supplied by the platform. */
class VaultIdleTimer {
    private var lastInteractionMillis: Long? = null

    fun start(nowMillis: Long) { lastInteractionMillis = nowMillis }
    fun interact(nowMillis: Long) {
        if (lastInteractionMillis != null) lastInteractionMillis = nowMillis
    }
    fun stop() { lastInteractionMillis = null }
    fun hasTimedOut(nowMillis: Long, timeoutSeconds: Int): Boolean =
        timeoutSeconds > 0 && lastInteractionMillis?.let {
            nowMillis - it >= timeoutSeconds * 1000L
        } == true
}
