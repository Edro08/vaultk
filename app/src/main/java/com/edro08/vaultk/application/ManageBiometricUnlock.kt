package com.edro08.vaultk.application

import com.edro08.vaultk.domain.BiometricCredentials

/** Authentication UI stays at the platform boundary; persistence goes through this use case. */
class ManageBiometricUnlock<C>(private val credentials: BiometricCredentials<C>) {
    fun enabled(reference: String): Boolean = credentials.enabled(reference)
    fun remove(reference: String) = credentials.remove(reference)
    fun encryptionCipher(reference: String): C = credentials.encryptionCipher(reference)
    fun store(reference: String, cipher: C, password: CharArray) =
        credentials.store(reference, cipher, password)
    fun decryptionCipher(reference: String): C = credentials.decryptionCipher(reference)
    fun recover(reference: String, cipher: C): CharArray = credentials.recover(reference, cipher)
}
