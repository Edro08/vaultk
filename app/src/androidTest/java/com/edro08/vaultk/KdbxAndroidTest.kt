package com.edro08.vaultk

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.edro08.vaultk.data.kdbx.KdbxReader
import com.edro08.vaultk.data.kdbx.KdbxWriter
import com.edro08.vaultk.domain.vault.VaultAttachment
import com.edro08.vaultk.domain.vault.InvalidVaultKeyException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.linguafranca.pwdb.format.KdbxCredentials
import org.linguafranca.pwdb.Entry
import org.linguafranca.pwdb.format.KdbxHeader
import org.linguafranca.pwdb.format.KdbxStreamFormat
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class KdbxAndroidTest {
    @Test fun opensV3AndV4OnAndroidAndRejectsWrongKey() {
        for (version in listOf(3, 4)) {
            val database = KdbxDatabase()
            database.rootGroup.addGroup("Android").addEntry("Ejemplo").apply {
                addProperty(Entry.STANDARD_PROPERTY_NAME_PASSWORD, "clave-del-ejemplo")
                setProperty("otp", "otpauth://totp/Test?secret=GEZDGNBV")
            }
            val bytes = ByteArrayOutputStream().also {
                database.save(KdbxStreamFormat(KdbxHeader(version)), KdbxCredentials("prueba".toByteArray()), it)
            }.toByteArray()
            val content = KdbxReader().read(ByteArrayInputStream(bytes), "prueba".toCharArray())
            assertEquals("Ejemplo", content.root.groups.single().entries.single().title)
            val secret = content.root.groups.single().entries.single().password!!
            assertTrue(secret.isProtected)
            val chars = secret.valueAsChars
            try { assertEquals("clave-del-ejemplo", String(chars)) } finally { chars.fill('\u0000') }
            val edited = content.copy(root = content.root.copy(groups = listOf(
                content.root.groups.single().let { group -> group.copy(entries = listOf(
                    group.entries.single().copy(title = "Editada", attachments = listOf(
                        VaultAttachment("backup.txt", "content://vaultk-test/attachment")))
                )) }
            )))
            val saved = KdbxWriter().prepare(bytes.inputStream(), "prueba".toCharArray(), content, edited) {
                byteArrayOf(4, 5, 6).inputStream()
            }
            assertEquals("Editada", KdbxReader().read(saved.inputStream(), "prueba".toCharArray())
                .root.groups.single().entries.single().title)
            assertTrue(KdbxReader().read(saved.inputStream(), "prueba".toCharArray())
                .root.groups.single().entries.single().attachments.single().bytes!!.contentEquals(byteArrayOf(4, 5, 6)))
            assertEquals("Ejemplo", KdbxReader().read(bytes.inputStream(), "prueba".toCharArray())
                .root.groups.single().entries.single().title)
            assertThrows(InvalidVaultKeyException::class.java) {
                KdbxReader().read(ByteArrayInputStream(bytes), "incorrecta".toCharArray())
            }
        }
    }
}
