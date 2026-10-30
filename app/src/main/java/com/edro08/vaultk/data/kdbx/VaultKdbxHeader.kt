package com.edro08.vaultk.data.kdbx

import android.os.Build
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import org.linguafranca.pwdb.format.KdbxHeader
import java.security.MessageDigest
import java.security.SecureRandom
import java.nio.ByteBuffer
import java.util.UUID

/** KeePassJava2 asks for the same expensive KDF result several times per KDBX operation. */
internal class VaultKdbxHeader : KdbxHeader {
    constructor() : super()
    constructor(version: Int) : super(version)

    private var cachedDigest: ByteArray? = null
    private var cachedTransformation: ByteArray? = null

    override fun getTransformedKeyDigest(digest: ByteArray): ByteArray {
        val previous = cachedDigest
        val transformed = cachedTransformation
        if (previous != null && transformed != null && MessageDigest.isEqual(previous, digest))
            return transformed.copyOf()
        clearDerivedKey()
        val result = if (version >= 4 && Build.VERSION.SDK_INT > 0 &&
            kdfParameters.mustGet("\$UUID").asUuid() == ARGON2D_UUID) {
            deriveNativeArgon2d(digest)
        } else super.getTransformedKeyDigest(digest)
        cachedDigest = digest.copyOf()
        cachedTransformation = result.copyOf()
        return result
    }

    fun clearDerivedKey() {
        cachedDigest?.fill(0)
        cachedTransformation?.fill(0)
        cachedDigest = null
        cachedTransformation = null
    }

    private fun deriveNativeArgon2d(digest: ByteArray): ByteArray {
        val parameters = kdfParameters
        val version = when (parameters.mustGet("V").asInteger()) {
            0x10 -> Argon2Version.V10
            0x13 -> Argon2Version.V13
            else -> error("Unsupported Argon2 version")
        }
        val iterations = Math.toIntExact(parameters.mustGet("I").asLong())
        val memoryBytes = parameters.mustGet("M").asLong()
        require(memoryBytes > 0 && memoryBytes % 1024L == 0L)
        val memoryKiB = Math.toIntExact(memoryBytes / 1024L)
        val parallelism = parameters.mustGet("P").asInteger()
        val result = Argon2Kt().hash(Argon2Mode.ARGON2_D, digest,
            parameters.mustGet("S").asByteArray(),
            tCostInIterations = iterations, mCostInKibibyte = memoryKiB,
            parallelism = parallelism, hashLengthInBytes = 32, version = version)
        return try {
            result.rawHashAsByteArray()
        } finally {
            result.rawHash.wipe()
            result.encodedOutput.wipe()
        }
    }

    private fun ByteBuffer.wipe() {
        clear()
        while (hasRemaining()) put(0.toByte())
    }

    /** Preserve KDF hardness and format, but never reuse salts, master seed or cipher IV. */
    fun rotateForSave() {
        clearDerivedKey()
        val random = SecureRandom()
        fun fresh(length: Int) = ByteArray(length).also(random::nextBytes)
        masterSeed = fresh(masterSeed.size)
        encryptionIv = fresh(encryptionIv.size)
        innerRandomStreamKey = fresh(innerRandomStreamKey.size)
        if (version < 4) {
            transformSeed = fresh(transformSeed.size)
            streamStartBytes = fresh(streamStartBytes.size)
        } else {
            val salt = kdfParameters.mustGet("S").asByteArray()
            kdfParameters.putByteArray("S", fresh(salt.size))
        }
    }

    private companion object {
        val ARGON2D_UUID: UUID = UUID.fromString("EF636DDF-8C29-444B-91F7-A9A403E30A0C")
    }
}
