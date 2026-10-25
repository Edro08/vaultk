package com.edro08.vaultk.domain

import java.security.SecureRandom

data class PasswordOptions(
    val length: Int = 20,
    val uppercase: Boolean = true,
    val lowercase: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
)

/** Each enabled character class is guaranteed to occur at least once. */
object PasswordGenerator {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 128
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#\$%&*?_-+=."
    private val random = SecureRandom()

    fun generate(options: PasswordOptions): String {
        require(options.length in MIN_LENGTH..MAX_LENGTH)
        val pools = buildList {
            if (options.uppercase) add(UPPERCASE)
            if (options.lowercase) add(LOWERCASE)
            if (options.digits) add(DIGITS)
            if (options.symbols) add(SYMBOLS)
        }
        require(pools.isNotEmpty())
        val alphabet = pools.joinToString("")
        val chars = CharArray(options.length) { index ->
            val pool = if (index < pools.size) pools[index] else alphabet
            pool[random.nextInt(pool.length)]
        }
        for (index in chars.lastIndex downTo 1) {
            val swap = random.nextInt(index + 1)
            val temp = chars[index]
            chars[index] = chars[swap]
            chars[swap] = temp
        }
        return try { String(chars) } finally { chars.fill('\u0000') }
    }
}
