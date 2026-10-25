package com.edro08.vaultk

import com.edro08.vaultk.data.KdbxReader
import com.edro08.vaultk.domain.InvalidVaultKeyException
import com.edro08.vaultk.domain.InvalidVaultFileException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.linguafranca.pwdb.format.KdbxCredentials
import org.linguafranca.pwdb.Entry
import org.linguafranca.pwdb.PropertyValue
import org.linguafranca.pwdb.format.KdbxHeader
import org.linguafranca.pwdb.format.KdbxStreamFormat
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class KdbxReaderTest {
    private val password = "test-master-key"

    @Test fun opensV4AndReadsNestedGroupsAndEntries() {
        val bytes = fixture(4)
        val content = KdbxReader().read(ByteArrayInputStream(bytes), password.toCharArray())
        assertEquals("Fixture", content.name)
        val group = content.root.groups.single()
        assertEquals("Servicios", group.name)
        assertEquals("Mega", group.entries.single().title)
        assertEquals("usuario", group.entries.single().username)
        assertEquals("https://example.com", group.entries.single().url)
        assertEquals("nota", group.entries.single().notes)
        val secret = group.entries.single().password!!
        assertTrue(secret.isProtected)
        val passwordChars = secret.valueAsChars
        try { assertEquals("clave-entrada", String(passwordChars)) } finally { passwordChars.fill('\u0000') }
        assertEquals("otpauth://totp/Test?secret=GEZDGNBV", group.entries.single().otp!!.valueAsString)
        assertEquals(listOf("Servicios", "Nube"), group.entries.single().tags)
        assertEquals("1234", group.entries.single().customFields["PIN"]!!.valueAsString)
        assertTrue(group.entries.single().customFields["PIN"]!!.isProtected)
        assertEquals("eu", group.entries.single().customFields["Region"]!!.valueAsString)
        assertTrue(!group.entries.single().customFields["Region"]!!.isProtected)
        assertEquals("backup.txt", group.entries.single().attachments.single().name)
        assertTrue(group.entries.single().attachments.single().bytes!!.contentEquals(byteArrayOf(1, 2, 3)))
        assertEquals("Anidado", group.groups.single().name)
    }

    @Test fun opensV3AndRejectsIncorrectKeys() {
        val bytes = fixture(3)
        assertEquals("Servicios", KdbxReader().read(ByteArrayInputStream(bytes), password.toCharArray()).root.groups.single().name)
        assertThrows(InvalidVaultKeyException::class.java) {
            KdbxReader().read(ByteArrayInputStream(bytes), "incorrecta".toCharArray())
        }
    }

    @Test fun rejectsIncorrectV4Key() {
        assertThrows(InvalidVaultKeyException::class.java) {
            KdbxReader().read(ByteArrayInputStream(fixture(4)), "incorrecta".toCharArray())
        }
    }

    @Test fun rejectsNonKdbxContent() {
        assertThrows(InvalidVaultFileException::class.java) {
            KdbxReader().read(ByteArrayInputStream(ByteArray(32)), password.toCharArray())
        }
    }

    private fun fixture(version: Int): ByteArray {
        val database = KdbxDatabase()
        database.name = "Fixture"
        val services = database.rootGroup.addGroup("Servicios")
        services.addEntry("Mega").apply {
            username = "usuario"
            url = "https://example.com"
            notes = "nota"
            addProperty(Entry.STANDARD_PROPERTY_NAME_PASSWORD, "clave-entrada")
            setProperty("otp", "otpauth://totp/Test?secret=GEZDGNBV")
            setPropertyValue("PIN", PropertyValue.SealedStore("1234".toCharArray()))
            setPropertyValue("Region", PropertyValue.BytesStore("eu".toCharArray()))
            setBinaryProperty("backup.txt", byteArrayOf(1, 2, 3))
            javaClass.getDeclaredField("tags").apply { isAccessible = true }.set(this, "Servicios;Nube")
        }
        services.addGroup("Anidado")
        return ByteArrayOutputStream().also { output ->
            database.save(KdbxStreamFormat(KdbxHeader(version)), KdbxCredentials(password.toByteArray()), output)
        }.toByteArray()
    }
}
