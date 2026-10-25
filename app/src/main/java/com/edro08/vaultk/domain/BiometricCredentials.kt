package com.edro08.vaultk.domain

/** C is an opaque platform authentication operation. References are stable document URI strings. */
interface BiometricCredentials<C> {
    fun enabled(reference: String): Boolean
    fun remove(reference: String)
    fun encryptionCipher(reference: String): C
    fun store(reference: String, cipher: C, password: CharArray)
    fun decryptionCipher(reference: String): C
    fun recover(reference: String, cipher: C): CharArray
}
