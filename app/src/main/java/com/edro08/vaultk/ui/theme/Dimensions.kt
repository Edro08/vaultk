package com.edro08.vaultk.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object VaultkSpacing {
    val tiny = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val normal = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
}

val VaultkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
