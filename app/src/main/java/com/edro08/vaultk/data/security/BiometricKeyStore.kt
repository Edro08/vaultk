package com.edro08.vaultk.data.security

import android.content.Context
import com.edro08.vaultk.domain.security.BiometricCredentials
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The KDBX password is only persisted encrypted by a per-document, biometric-bound Keystore key. */
class BiometricKeyStore(context: Context) : BiometricCredentials<Cipher> {
    private val preferences = context.getSharedPreferences("biometric_keys", Context.MODE_PRIVATE)
    private val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun alias(reference: String): String = "vaultk-biometric-" + MessageDigest.getInstance("SHA-256")
        .digest(reference.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    override fun enabled(reference: String): Boolean = preferences.contains(alias(reference))

    override fun remove(reference: String) {
        val name = alias(reference)
        preferences.edit().remove(name).commit()
        keystore.deleteEntry(name)
    }

    override fun encryptionCipher(reference: String): Cipher {
        val name = alias(reference)
        val key = (keystore.getKey(name, null) as? SecretKey) ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(name,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                    .setInvalidatedByBiometricEnrollment(true)
                    .build())
            }.generateKey()
        return Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    override fun store(reference: String, cipher: Cipher, password: CharArray) {
        val byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password))
        val plain = ByteArray(byteBuffer.remaining())
        byteBuffer.get(plain)
        if (byteBuffer.hasArray()) byteBuffer.array().fill(0)
        try {
            val encrypted = cipher.doFinal(plain)
            try {
                val value = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(encrypted, Base64.NO_WRAP)
                check(preferences.edit().putString(alias(reference), value).commit())
            } finally { encrypted.fill(0) }
        } finally { plain.fill(0) }
    }

    override fun decryptionCipher(reference: String): Cipher {
        val value = preferences.getString(alias(reference), null) ?: throw IllegalStateException("Missing credential")
        val iv = Base64.decode(value.substringBefore(':'), Base64.NO_WRAP)
        require(iv.size == 12)
        val key = keystore.getKey(alias(reference), null) as? SecretKey ?: throw IllegalStateException("Missing key")
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
    }

    override fun recover(reference: String, cipher: Cipher): CharArray {
        val value = preferences.getString(alias(reference), null) ?: throw IllegalStateException("Missing credential")
        val encrypted = Base64.decode(value.substringAfter(':'), Base64.NO_WRAP)
        return try {
            val plain = cipher.doFinal(encrypted)
            try {
                val charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(plain))
                val chars = CharArray(charBuffer.remaining())
                charBuffer.get(chars)
                if (charBuffer.hasArray()) charBuffer.array().fill('\u0000')
                chars
            } finally { plain.fill(0) }
        } finally { encrypted.fill(0) }
    }
}
