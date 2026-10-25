package com.edro08.vaultk

import com.edro08.vaultk.data.KdbxReader
import com.edro08.vaultk.data.KdbxWriter
import com.edro08.vaultk.domain.InvalidVaultKeyException
import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.domain.VaultGroup
import com.edro08.vaultk.domain.insertEntry
import com.edro08.vaultk.domain.moveEntry
import com.edro08.vaultk.domain.removeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.linguafranca.pwdb.PropertyValue
import com.edro08.vaultk.data.KdbxVaultValueFactory
import org.linguafranca.pwdb.format.KdbxCredentials
import org.linguafranca.pwdb.format.KdbxHeader
import org.linguafranca.pwdb.format.KdbxSerializer
import org.linguafranca.pwdb.format.KdbxStreamFormat
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class KdbxWriterTest {
    @Test fun eachSaveRotatesCryptographicMaterialButPreservesKdfCost() {
        for (version in listOf(3, 4)) {
            val db = KdbxDatabase()
            db.rootGroup.addEntry("Antes")
            val source = ByteArrayOutputStream().also {
                db.save(KdbxStreamFormat(KdbxHeader(version)),
                    KdbxCredentials("llave".toByteArray()), it)
            }.toByteArray()
            val reader = KdbxReader()
            val original = reader.read(source.inputStream(), "llave".toCharArray())
            val changed = original.copy(root = original.root.copy(entries =
                original.root.entries.map { it.copy(title = "Después") }))
            val first = KdbxWriter(reader).prepare(source.inputStream(), "llave".toCharArray(),
                original, changed) { error("No attachment") }
            val next = reader.read(first.inputStream(), "llave".toCharArray())
            val second = KdbxWriter(reader).prepare(first.inputStream(), "llave".toCharArray(),
                next, next.copy(root = next.root.copy(entries =
                    next.root.entries.map { it.copy(title = "Último") }))) { error("No attachment") }
            val headers = listOf(source, first, second).map {
                KdbxSerializer.readOuterHeader(it.inputStream(), KdbxHeader())
            }
            headers.zipWithNext { before, after ->
                assertEquals(before.version, after.version)
                assertEquals(before.cipherUuid, after.cipherUuid)
                assertFalse(before.masterSeed.contentEquals(after.masterSeed))
                assertFalse(before.encryptionIv.contentEquals(after.encryptionIv))
                if (version == 3) {
                    assertEquals(before.transformRounds, after.transformRounds)
                    assertFalse(before.transformSeed.contentEquals(after.transformSeed))
                    assertFalse(before.innerRandomStreamKey.contentEquals(after.innerRandomStreamKey))
                } else {
                    val old = before.kdfParameters
                    val new = after.kdfParameters
                    for (parameter in listOf("I", "M", "P", "V", "$" + "UUID"))
                        assertArrayEquals(old.mustGet(parameter).asByteArray(), new.mustGet(parameter).asByteArray())
                    assertFalse(old.mustGet("S").asByteArray().contentEquals(new.mustGet("S").asByteArray()))
                }
            }
            assertEquals("Último", reader.read(second.inputStream(), "llave".toCharArray())
                .root.entries.single().title)
        }
    }

    @Test fun movesDuplicatesTrashesRestoresAndDeletesAcrossSaves() {
        for (version in listOf(3, 4)) {
            val db = KdbxDatabase()
            db.enableRecycleBin(true)
            db.recycleBin
            val sourceGroup = db.rootGroup.addGroup("Origen")
            db.rootGroup.addGroup("Destino")
            sourceGroup.addEntry("Principal").apply {
                setPropertyValue("Password", PropertyValue.SealedStore("secret".toCharArray()))
                setBinaryProperty("a.bin", byteArrayOf(7, 8))
            }
            sourceGroup.addEntry("Borrar")
            val bytes = ByteArrayOutputStream().also {
                db.save(KdbxStreamFormat(KdbxHeader(version)), KdbxCredentials("llave".toByteArray()), it)
            }.toByteArray()
            val reader = KdbxReader()
            val original = reader.read(bytes.inputStream(), "llave".toCharArray())
            val source = original.root.groups.single { it.name == "Origen" }
            val target = original.root.groups.single { it.name == "Destino" }
            val trash = original.recycleBinId!!
            val primary = source.entries.single { it.title == "Principal" }
            val removed = source.entries.single { it.title == "Borrar" }
            val changed = original.copy(root = original.root
                .moveEntry(source.id, target.id, primary.id)
                .moveEntry(source.id, trash, removed.id)
                .insertEntry(target.id, primary.copy(id = "draft:duplicate", title = "Copia")))
            val writer = KdbxWriter(reader)
            val saved = writer.prepare(bytes.inputStream(), "llave".toCharArray(), original, changed) {
                error("No SAF binary")
            }
            val opened = reader.read(saved.inputStream(), "llave".toCharArray())
            val moved = opened.root.groups.single { it.id == target.id }.entries
            assertEquals(2, moved.size)
            assertEquals(primary.id, moved.single { it.title == "Principal" }.id)
            assertTrue(moved.single { it.title == "Copia" }.id != primary.id)
            assertEquals("secret", moved.single { it.title == "Copia" }.password!!.valueAsString)
            assertTrue(moved.single { it.title == "Principal" }.attachments.single().bytes!!
                .contentEquals(byteArrayOf(7, 8)))
            assertEquals(removed.id, opened.root.groups.single { it.id == trash }.entries.single().id)

            val restored = opened.copy(root = opened.root
                .moveEntry(trash, source.id, removed.id)
                .removeEntry(target.id, primary.id))
            val savedAgain = writer.prepare(saved.inputStream(), "llave".toCharArray(), opened, restored) {
                error("No SAF binary")
            }
            val reopened = reader.read(savedAgain.inputStream(), "llave".toCharArray())
            assertEquals(removed.id, reopened.root.groups.single { it.id == source.id }.entries.single().id)
            assertEquals("Copia", reopened.root.groups.single { it.id == target.id }.entries.single().title)
            assertTrue(reopened.root.groups.single { it.id == trash }.entries.isEmpty())
            assertEquals(2, source.entries.size) // The original ciphertext was not modified.

            val permanentlyRemoved = opened.copy(root = opened.root.removeEntry(trash, removed.id))
            val purged = writer.prepare(saved.inputStream(), "llave".toCharArray(), opened,
                permanentlyRemoved) { error("No SAF binary") }
            val purgedSnapshot = reader.read(purged.inputStream(), "llave".toCharArray())
            assertTrue(purgedSnapshot.root.groups.single { it.id == trash }.entries.isEmpty())
            assertTrue(purgedSnapshot.root.groups.single { it.id == source.id }.entries.isEmpty())
        }
    }

    @Test fun opensEditsSavesAndReopensV3AndV4WithoutTouchingOriginal() {
        for (version in listOf(3, 4)) {
            val db = KdbxDatabase()
            db.name = "Vault"
            val group = db.rootGroup.addGroup("Servicios")
            group.addEntry("Mega").apply {
                username = "usuario"
                setPropertyValue("Password", PropertyValue.SealedStore("secreto".toCharArray()))
                setProperty("Region", "eu")
                setBinaryProperty("backup.txt", byteArrayOf(1, 2))
            }
            val source = ByteArrayOutputStream().also {
                db.save(KdbxStreamFormat(KdbxHeader(version)), KdbxCredentials("llave".toByteArray()), it)
            }.toByteArray()
            val reader = KdbxReader()
            val original = reader.read(source.inputStream(), "llave".toCharArray())
            val services = original.root.groups.single()
            val old = services.entries.single()
            val scannedOtp = "otpauth://totp/KeePassXC:ana?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=KeePassXC&digits=8&period=30"
            val edited = old.copy(title = "Mega nueva", notes = "nota editada", tags = listOf("Nube"),
                otp = KdbxVaultValueFactory().create(scannedOtp.toCharArray(), true),
                customFields = old.customFields + ("Region" to KdbxVaultValueFactory().create("us".toCharArray(), false)),
                attachments = emptyList())
            val updated = original.copy(root = original.root.copy(groups = listOf(services.copy(
                entries = listOf(edited, VaultEntry("draft:entry", "Nueva", "yo", "", "", null)),
                groups = listOf(VaultGroup("draft:group", "Equipo", emptyList(),
                    listOf(VaultEntry("draft:child", "Hija", "", "", "", null))))))))
            val encrypted = KdbxWriter(reader).prepare(ByteArrayInputStream(source), "llave".toCharArray(),
                original, updated) { error("Unexpected SAF attachment") }
            val reopened = reader.read(encrypted.inputStream(), "llave".toCharArray())
            val after = reopened.root.groups.single()
            assertEquals("Mega nueva", after.entries.first().title)
            assertEquals(old.id, after.entries.first().id)
            assertEquals("nota editada", after.entries.first().notes)
            assertEquals("secreto", after.entries.first().password!!.valueAsString)
            assertEquals("us", after.entries.first().customFields["Region"]!!.valueAsString)
            assertEquals(scannedOtp, after.entries.first().otp!!.valueAsString)
            assertTrue(after.entries.first().otp!!.isProtected)
            assertEquals(listOf("Nube"), after.entries.first().tags)
            assertTrue(after.entries.first().attachments.isEmpty())
            assertEquals("Nueva", after.entries.last().title)
            assertEquals("Hija", after.groups.single().entries.single().title)
            assertEquals("Mega", reader.read(source.inputStream(), "llave".toCharArray())
                .root.groups.single().entries.single().title)
            assertThrows(InvalidVaultKeyException::class.java) {
                reader.read(encrypted.inputStream(), "incorrecta".toCharArray())
            }
        }
    }
}
