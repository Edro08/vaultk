package com.edro08.vaultk

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edro08.vaultk.data.kdbx.KdbxReader
import com.edro08.vaultk.data.kdbx.KdbxWriter
import com.edro08.vaultk.data.kdbx.LocalVaultTransaction
import com.edro08.vaultk.data.kdbx.VaultKdbxHeader
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.linguafranca.pwdb.format.KdbxCredentials
import org.linguafranca.pwdb.format.KdbxStreamFormat
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class Argon2NativeTest {
    @Test fun savesIsolatedHighCostVaultAndMeasuresFullSave() {
        val header = VaultKdbxHeader(4)
        header.kdfParameters.putULong("I", 98)
        header.kdfParameters.putUInt("P", 4)
        val database = KdbxDatabase().apply { rootGroup.addEntry("Before") }
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "argon2-native-benchmark.kdbx")
        val key = "benchmark-only".toCharArray()
        try {
            val source = ByteArrayOutputStream().also {
                database.save(KdbxStreamFormat(header), KdbxCredentials("benchmark-only".toByteArray()), it)
            }.toByteArray()
            file.writeBytes(source)
            val reader = KdbxReader()
            val content = reader.read(file.inputStream(), key)
            val changed = content.copy(root = content.root.copy(entries =
                content.root.entries.map { it.copy(title = "After") }))
            val started = System.nanoTime()
            val replacement = file.inputStream().use {
                KdbxWriter(reader).prepare(it, key, content, changed) { error("No attachment") }
            }
            val prepared = System.nanoTime()
            val transaction = LocalVaultTransaction(reader)
            val saved = transaction.commit(file, transaction.fingerprint(file), replacement, key).first
            val done = System.nanoTime()
            assertEquals("After", saved.root.entries.single().title)
            assertEquals("After", reader.read(file.inputStream(), key).root.entries.single().title)
            Log.i("VaultkSaveTiming", "nativeFullSave prepare=${(prepared - started) / 1_000_000}ms, " +
                "commit=${(done - prepared) / 1_000_000}ms")
        } finally {
            header.clearDerivedKey()
            key.fill('\u0000')
            file.delete()
        }
    }

    @Test fun nativeArgon2dMatchesKdbxReferenceAndMeasuresVaultCost() {
        val input = ByteArray(32) { it.toByte() }
        val salt = ByteArray(32) { (it + 32).toByte() }
        val native = Argon2Kt()
        val reference = Argon2BytesGenerator().apply {
            init(Argon2Parameters.Builder(Argon2Parameters.ARGON2_d)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(2).withMemoryAsKB(65536).withParallelism(4)
                .withSalt(salt).build())
        }
        val expected = ByteArray(32)
        reference.generateBytes(input, expected, 0, expected.size)
        val actual = native.hash(Argon2Mode.ARGON2_D, input, salt,
            tCostInIterations = 2, mCostInKibibyte = 65536, parallelism = 4,
            hashLengthInBytes = 32, version = Argon2Version.V13).rawHashAsByteArray()
        assertArrayEquals(expected, actual)
        val started = System.nanoTime()
        val result = native.hash(Argon2Mode.ARGON2_D, input, salt,
            tCostInIterations = 98, mCostInKibibyte = 65536, parallelism = 4,
            hashLengthInBytes = 32, version = Argon2Version.V13)
        Log.i("VaultkSaveTiming", "nativeArgon2_98=${(System.nanoTime() - started) / 1_000_000}ms")
        result.rawHashAsByteArray().fill(0)
        expected.fill(0)
        actual.fill(0)
        input.fill(0)
        salt.fill(0)
    }
}
