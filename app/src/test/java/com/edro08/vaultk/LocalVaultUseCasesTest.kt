package com.edro08.vaultk

import com.edro08.vaultk.domain.VaultRepository
import com.edro08.vaultk.domain.VaultId
import com.edro08.vaultk.application.OpenLocalVault
import com.edro08.vaultk.domain.OpenedVault
import com.edro08.vaultk.application.SaveLocalVault
import com.edro08.vaultk.domain.VaultSaveProgress
import com.edro08.vaultk.domain.VaultSaveStage
import com.edro08.vaultk.domain.VaultContent
import com.edro08.vaultk.domain.VaultGroup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LocalVaultUseCasesTest {
    private val content = VaultContent("test", VaultGroup("root", "Root", emptyList(), emptyList()))

    @Test fun opensThroughGatewayWithoutExposingInfrastructure() = runBlocking {
        val result = OpenedVault(content, byteArrayOf(1))
        val gateway = object : VaultRepository {
            override fun open(id: VaultId, key: CharArray): OpenedVault {
                assertEquals("/vault.kdbx", id.value)
                assertEquals("key", String(key))
                return result
            }
            override fun save(id: VaultId, expectedFingerprint: ByteArray, key: CharArray,
                original: VaultContent, changed: VaultContent,
                onProgress: (VaultSaveProgress) -> Unit): OpenedVault = error("Unexpected save")
        }
        assertSame(result, OpenLocalVault(gateway)(VaultId("/vault.kdbx"), "key".toCharArray()))
    }

    @Test fun saveEmitsPreparingAndReturnsVerifiedResult() = runBlocking {
        val result = OpenedVault(content, byteArrayOf(2))
        val stages = mutableListOf<VaultSaveStage>()
        val gateway = object : VaultRepository {
            override fun open(id: VaultId, key: CharArray): OpenedVault = error("Unexpected open")
            override fun save(id: VaultId, expectedFingerprint: ByteArray, key: CharArray,
                original: VaultContent, changed: VaultContent,
                onProgress: (VaultSaveProgress) -> Unit): OpenedVault {
                assertEquals("/vault.kdbx", id.value)
                assertEquals(1, expectedFingerprint.single().toInt())
                assertSame(content, original)
                assertSame(content, changed)
                onProgress(VaultSaveProgress(VaultSaveStage.Verifying))
                return result
            }
        }
        val saved = SaveLocalVault(gateway)(VaultId("/vault.kdbx"), byteArrayOf(1),
            "key".toCharArray(), content, content) { stages += it.stage }
        assertSame(result, saved)
        assertEquals(listOf(VaultSaveStage.Preparing, VaultSaveStage.Verifying), stages)
    }
}
