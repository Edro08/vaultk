package com.edro08.vaultk

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.edro08.vaultk.domain.VaultGroup
import com.edro08.vaultk.ui.screen.GroupPickerSheet
import com.edro08.vaultk.ui.theme.VaultkTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GroupPickerLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun createGroupAndNoticeRemainInsideSheetWithManyGroups() {
        val children = (1..12).map { VaultGroup("group-$it", "Grupo $it", emptyList(), emptyList()) }
        val root = VaultGroup("root", "Raíz", children, emptyList())
        compose.setContent {
            VaultkTheme {
                GroupPickerSheet(root, root.id, onDismiss = {}, onSelect = {}, onCreate = { _, _ -> })
            }
        }

        val button = compose.onNodeWithText("Nuevo grupo").assertIsDisplayed().getUnclippedBoundsInRoot()
        val notice = compose.onNodeWithText("Guarda una copia KDBX después de crear un grupo para conservarlo.")
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val screen = compose.onAllNodes(isRoot()).get(1).getUnclippedBoundsInRoot()
        assertTrue("El aviso debe quedar bajo el botón", notice.top >= button.bottom)
        assertTrue("El pie debe quedar separado del borde de la ventana", notice.bottom <= screen.bottom - 16.dp)
    }
}
