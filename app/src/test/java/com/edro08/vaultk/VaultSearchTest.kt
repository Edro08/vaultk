package com.edro08.vaultk

import com.edro08.vaultk.domain.VaultEntry
import com.edro08.vaultk.domain.VaultGroup
import com.edro08.vaultk.domain.searchEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.edro08.vaultk.data.KdbxVaultValueFactory

class VaultSearchTest {
    private val nested = VaultEntry("nested", "Correo", "ana", "https://mail.example.org",
        "Cuenta de trabajo", null,
        password = KdbxVaultValueFactory().create("contraseña-oculta".toCharArray(), true),
        tags = listOf("Personal"),
        customFields = mapOf(
            "Región" to KdbxVaultValueFactory().create("Europa".toCharArray(), false),
            "PIN" to KdbxVaultValueFactory().create("123456".toCharArray(), true),
        ))
    private val root = VaultGroup("root", "Raíz", listOf(
        VaultGroup("work", "Trabajo", listOf(
            VaultGroup("nestedGroup", "Servicios", emptyList(), listOf(nested))
        ), emptyList()),
        VaultGroup("other", "Otros", emptyList(),
            listOf(VaultEntry("otherEntry", "Otra cuenta", "luis", "", "", null))),
    ), emptyList())

    @Test fun searchesEveryGroupAndFieldRegardlessOfSelectedGroup() {
        for (query in listOf("correo", "ANA", "mail.example", "trabajo", "personal", "región", "EUROPA")) {
            val result = root.searchEntries(query).single()
            assertEquals("nested", result.entry.id)
            assertEquals("nestedGroup", result.groupId)
            assertEquals(listOf("Raíz", "Trabajo", "Servicios"), result.groupPath)
        }
        assertEquals("nested", root.searchEntries("ana europa").single().entry.id)
        assertEquals("otherEntry", root.searchEntries("otra").single().entry.id)
    }

    @Test fun excludesProtectedValuesAndSecretsAndDoesNotShowAllOnEmptyQuery() {
        for (query in listOf("contraseña-oculta", "123456", "  ", "ana inexistente")) {
            assertTrue(root.searchEntries(query).isEmpty())
        }
        assertEquals("nested", root.searchEntries("PIN").single().entry.id)
    }
}
