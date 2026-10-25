package com.edro08.vaultk.domain

import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Standard otpauth://totp/ URI as stored in KeePassXC's `otp` property. */
class Totp private constructor(private val key: ByteArray, private val algorithm: String,
    private val digits: Int, val period: Int) {
    fun codeAt(epochSeconds: Long): String {
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(key, algorithm))
        val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(Math.floorDiv(epochSeconds, period.toLong())).array())
        val offset = hash.last().toInt() and 0x0f
        val value = ((hash[offset].toInt() and 0x7f) shl 24) or
            ((hash[offset + 1].toInt() and 0xff) shl 16) or
            ((hash[offset + 2].toInt() and 0xff) shl 8) or
            (hash[offset + 3].toInt() and 0xff)
        return (value % POWERS[digits]).toString().padStart(digits, '0')
    }

    fun secondsLeft(epochSeconds: Long): Int = period - Math.floorMod(epochSeconds, period.toLong()).toInt()

    fun clear() = key.fill(0)

    companion object {
        private val POWERS = intArrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000)

        /** Canonical KeePassXC `otp` value for a manual Base32 key or a scanned otpauth URI. */
        fun credentialUri(raw: String): String? {
            val text = raw.trim()
            if (text.isEmpty()) return null
            val uri = if (text.startsWith("otpauth://", ignoreCase = true)) text else {
                val secret = text.filterNot(Char::isWhitespace).uppercase(Locale.ROOT)
                if (secret.any { it !in 'A'..'Z' && it !in '2'..'7' && it != '=' }) return null
                "otpauth://totp/?secret=$secret"
            }
            return uri.takeIf { parse(it)?.let { otp -> otp.clear(); true } == true }
        }

        fun parse(raw: String): Totp? = runCatching {
            val uri = URI(raw)
            if (!uri.scheme.equals("otpauth", ignoreCase = true) ||
                !uri.host.equals("totp", ignoreCase = true)) return null
            val parameters = uri.rawQuery?.split('&')?.mapNotNull { item ->
                val separator = item.indexOf('=')
                if (separator <= 0) null else {
                    URLDecoder.decode(item.substring(0, separator), StandardCharsets.UTF_8.name()).lowercase(Locale.ROOT) to
                        URLDecoder.decode(item.substring(separator + 1), StandardCharsets.UTF_8.name())
                }
            }?.toMap() ?: return null
            val algorithm = when (parameters["algorithm"]?.uppercase(Locale.ROOT) ?: "SHA1") {
                "SHA1" -> "HmacSHA1"
                "SHA256" -> "HmacSHA256"
                "SHA512" -> "HmacSHA512"
                else -> return null
            }
            val digits = parameters["digits"]?.toIntOrNull() ?: if ("digits" in parameters) return null else 6
            val period = parameters["period"]?.toIntOrNull() ?: if ("period" in parameters) return null else 30
            if (digits !in 6..8 || period !in 15..120) return null
            val key = decodeBase32(parameters["secret"] ?: return null) ?: return null
            Totp(key, algorithm, digits, period)
        }.getOrNull()

        private fun decodeBase32(secret: String): ByteArray? {
            val clean = secret.uppercase(Locale.ROOT).filterNot { it.isWhitespace() }.trimEnd('=')
            if (clean.isEmpty()) return null
            val output = ArrayList<Byte>()
            var buffer = 0
            var bits = 0
            for (char in clean) {
                val digit = when (char) {
                    in 'A'..'Z' -> char - 'A'
                    in '2'..'7' -> char - '2' + 26
                    else -> return null
                }
                buffer = (buffer shl 5) or digit
                bits += 5
                if (bits >= 8) {
                    bits -= 8
                    output.add((buffer shr bits).toByte())
                    buffer = buffer and ((1 shl bits) - 1)
                }
            }
            return output.takeIf { it.isNotEmpty() }?.toByteArray()
        }
    }
}
