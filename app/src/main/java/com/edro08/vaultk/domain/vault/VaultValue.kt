package com.edro08.vaultk.domain.vault

/** A session value. Implementations must not retain protected text as a String or plain char array. */
interface VaultValue {
    val isProtected: Boolean
    val valueAsChars: CharArray
    val valueAsString: String
        get() {
            val chars = valueAsChars
            return try { String(chars) } finally { chars.fill('\u0000') }
        }
}

fun interface VaultValueFactory {
    fun create(chars: CharArray, protected: Boolean): VaultValue
}
