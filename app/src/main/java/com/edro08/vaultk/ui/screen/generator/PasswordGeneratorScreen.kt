package com.edro08.vaultk.ui.screen.generator

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edro08.vaultk.R
import com.edro08.vaultk.domain.PasswordGenerator
import com.edro08.vaultk.domain.PasswordOptions
import com.edro08.vaultk.ui.LocalCopyCredential
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.component.VaultkIconTile
import com.edro08.vaultk.ui.theme.LocalVaultkCustomColors
import com.edro08.vaultk.ui.theme.VaultkSpacing
import kotlin.math.log2
import kotlin.math.roundToInt

private val lengthMarks = listOf(4, 8, 12, 20, 32, 64, 128)

private fun sliderPosition(length: Int): Float {
    val segment = lengthMarks.indexOfLast { it <= length }.coerceIn(0, lengthMarks.lastIndex - 1)
    return segment + (length - lengthMarks[segment]).toFloat() /
        (lengthMarks[segment + 1] - lengthMarks[segment])
}

private fun sliderLength(position: Float): Int {
    val segment = position.toInt().coerceIn(0, lengthMarks.lastIndex - 1)
    return (lengthMarks[segment] + (position - segment) *
        (lengthMarks[segment + 1] - lengthMarks[segment])).roundToInt()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(modifier: Modifier = Modifier, onUse: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    val copyCredential = LocalCopyCredential.current
    val customColors = LocalVaultkCustomColors.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    var lengthText by remember { mutableStateOf(PasswordOptions().length.toString()) }
    var isSliding by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf(PasswordOptions()) }
    var typesExpanded by remember { mutableStateOf(true) }
    var generated by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(true) }

    val useButtonRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(generated) {
        if (generated != null && onUse != null) useButtonRequester.bringIntoView()
    }

    val length = lengthText.toIntOrNull()
    val valid = length != null && length in PasswordGenerator.MIN_LENGTH..PasswordGenerator.MAX_LENGTH &&
        (options.uppercase || options.lowercase || options.digits || options.symbols)

    fun update(newOptions: PasswordOptions) {
        options = newOptions
        if (generated != null && valid) {
            generated = PasswordGenerator.generate(newOptions.copy(length = length ?: newOptions.length))
        }
    }

    fun copy(password: String) {
        requireNotNull(copyCredential)(context.getString(R.string.generator_clip_label), password)
        Toast.makeText(context, context.getString(R.string.generator_copied), Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VaultkSpacing.normal),
        verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
    ) {
        // Card 1: Longitud con escala de regla y puntos alineados
        VaultkCard {
            Column(
                modifier = Modifier.padding(VaultkSpacing.normal),
                verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
            ) {
                // Cabecera de longitud con caja de valor a la derecha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                ) {
                    VaultkIconTile(Icons.Default.Straighten, null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSliding) stringResource(R.string.generator_length_value, lengthText)
                            else stringResource(R.string.generator_length),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.generator_length_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = lengthText,
                        onValueChange = {
                            if (it.length <= 3 && it.all(Char::isDigit)) {
                                lengthText = it
                                val parsed = it.toIntOrNull()
                                if (parsed != null && parsed in PasswordGenerator.MIN_LENGTH..PasswordGenerator.MAX_LENGTH && generated != null && valid) {
                                    generated = PasswordGenerator.generate(options.copy(length = parsed))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = length == null || length !in PasswordGenerator.MIN_LENGTH..PasswordGenerator.MAX_LENGTH,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(76.dp)
                            .testTag("generator-length")
                    )
                }

                // Slider con puntos exactamente sobre cada tick y burbuja flotante en el thumb
                Slider(
                    value = sliderPosition((length ?: PasswordGenerator.MIN_LENGTH)
                        .coerceIn(PasswordGenerator.MIN_LENGTH, PasswordGenerator.MAX_LENGTH)),
                    onValueChange = {
                        isSliding = true
                        val newLen = sliderLength(it)
                        lengthText = newLen.toString()
                        if (generated != null && valid) {
                            generated = PasswordGenerator.generate(options.copy(length = newLen))
                        }
                    },
                    onValueChangeFinished = { isSliding = false },
                    valueRange = 0f..lengthMarks.lastIndex.toFloat(),
                    thumb = {
                        Box(
                            modifier = Modifier.size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(unbounded = true)
                                    .offset(y = (-26).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = lengthText,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    },
                    track = { state ->
                        val activeTick = MaterialTheme.colorScheme.onPrimary
                        val inactiveTick = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        val density = LocalDensity.current
                        val thumbRadiusPx = with(density) { 10.dp.toPx() }

                        Box(Modifier.fillMaxWidth()) {
                            SliderDefaults.Track(sliderState = state)
                            Canvas(Modifier.matchParentSize()) {
                                val trackStart = thumbRadiusPx
                                val trackEnd = size.width - thumbRadiusPx
                                val trackLength = (trackEnd - trackStart).coerceAtLeast(0f)

                                for (index in 0..lengthMarks.lastIndex) {
                                    val markFraction = index.toFloat() / lengthMarks.lastIndex
                                    val x = if (isRtl) trackEnd - trackLength * markFraction else trackStart + trackLength * markFraction
                                    val isPassed = if (isRtl) (1f - markFraction) <= state.coercedValueAsFraction else markFraction <= state.coercedValueAsFraction

                                    drawCircle(
                                        color = if (isPassed) activeTick else inactiveTick,
                                        radius = 2.5.dp.toPx(),
                                        center = Offset(x, center.y)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .testTag("generator-slider")
                )

                // Escala de regla inferior: ticks verticales | y números directamente alineados bajo los puntos
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    val totalWidth = maxWidth
                    val thumbRadius = 10.dp
                    val availableWidth = totalWidth - thumbRadius * 2

                    lengthMarks.forEachIndexed { index, mark ->
                        val markFraction = index.toFloat() / lengthMarks.lastIndex
                        val isSelected = length == mark
                        val centerOffset = if (isRtl) {
                            thumbRadius + availableWidth * (1f - markFraction)
                        } else {
                            thumbRadius + availableWidth * markFraction
                        }

                        Box(
                            modifier = Modifier
                                .offset(x = centerOffset - 20.dp)
                                .width(40.dp)
                                .clickable {
                                    lengthText = mark.toString()
                                    if (generated != null && valid) {
                                        generated = PasswordGenerator.generate(options.copy(length = mark))
                                    }
                                }
                                .testTag("generator-preset-$mark"),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(6.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = mark.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (length == null || length !in PasswordGenerator.MIN_LENGTH..PasswordGenerator.MAX_LENGTH) {
                    Text(
                        text = stringResource(
                            R.string.generator_length_error,
                            PasswordGenerator.MIN_LENGTH,
                            PasswordGenerator.MAX_LENGTH
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Card 2: Tipos de caracteres (Colapsable)
        VaultkCard {
            Column(Modifier.padding(vertical = VaultkSpacing.small)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { typesExpanded = !typesExpanded }
                        .padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                ) {
                    VaultkIconTile(Icons.Default.Settings, null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.generator_types),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.generator_types_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (typesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(if (typesExpanded) R.string.form_collapse else R.string.form_expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (typesExpanded) {
                    GeneratorOption(
                        label = stringResource(R.string.generator_uppercase),
                        checked = options.uppercase
                    ) { update(options.copy(uppercase = it)) }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = VaultkSpacing.normal),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    GeneratorOption(
                        label = stringResource(R.string.generator_lowercase),
                        checked = options.lowercase
                    ) { update(options.copy(lowercase = it)) }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = VaultkSpacing.normal),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    GeneratorOption(
                        label = stringResource(R.string.generator_digits),
                        checked = options.digits
                    ) { update(options.copy(digits = it)) }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = VaultkSpacing.normal),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    GeneratorOption(
                        label = stringResource(R.string.generator_symbols),
                        checked = options.symbols
                    ) { update(options.copy(symbols = it)) }

                    if (!options.uppercase && !options.lowercase && !options.digits && !options.symbols) {
                        Text(
                            text = stringResource(R.string.generator_classes_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(
                                horizontal = VaultkSpacing.normal,
                                vertical = VaultkSpacing.small
                            )
                        )
                    }
                }
            }
        }

        // Card 3: Contraseña generada (Abajo, antes de los botones)
        generated?.let { password ->
            val alphabetSize = (if (options.uppercase) 26 else 0) +
                (if (options.lowercase) 26 else 0) + (if (options.digits) 10 else 0) +
                (if (options.symbols) 13 else 0)
            val bits = (password.length * log2(alphabetSize.toDouble().coerceAtLeast(1.0))).roundToInt()
            val level = when {
                bits < 40 -> 1
                bits < 60 -> 2
                bits < 80 -> 3
                bits < 100 -> 4
                else -> 5
            }
            val strengthColor = when {
                level <= 1 -> MaterialTheme.colorScheme.error
                level <= 3 -> Color(0xFFFBBF24)
                level == 4 -> Color(0xFF34D399)
                else -> Color(0xFF10B981)
            }
            val strengthText = stringResource(when {
                level <= 1 -> R.string.generator_weak
                level <= 3 -> R.string.generator_moderate
                level == 4 -> R.string.generator_strong
                else -> R.string.generator_secure
            })

            VaultkCard {
                Column(
                    modifier = Modifier.padding(VaultkSpacing.normal),
                    verticalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)
                    ) {
                        VaultkIconTile(Icons.Default.Key, null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.generator_preview),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$bits bits · $strengthText",
                                style = MaterialTheme.typography.bodySmall,
                                color = strengthColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(if (visible) R.string.common_hide_password else R.string.common_show_password),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Password Box with Monospace Display
                    Surface(
                        color = customColors.inputBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, customColors.inputBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (visible) {
                            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                            val digitColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                            val symbolColor = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                            val upperColor = if (isDark) Color.White else Color(0xFF09090B)
                            val lowerColor = if (isDark) Color(0xFFD4D4D8) else Color(0xFF4B5563)

                            val styledText = remember(password, isDark) {
                                buildAnnotatedString {
                                    password.forEach { ch ->
                                        when {
                                            ch.isDigit() -> withStyle(SpanStyle(color = digitColor)) { append(ch) }
                                            !ch.isLetterOrDigit() -> withStyle(SpanStyle(color = symbolColor)) { append(ch) }
                                            ch.isUpperCase() -> withStyle(SpanStyle(color = upperColor, fontWeight = FontWeight.SemiBold)) { append(ch) }
                                            else -> withStyle(SpanStyle(color = lowerColor)) { append(ch) }
                                        }
                                    }
                                }
                            }
                            Text(
                                text = styledText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 1.25f
                                ),
                                modifier = Modifier.padding(VaultkSpacing.normal)
                            )
                        } else {
                            Text(
                                text = "•".repeat(password.length.coerceAtMost(32)),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(VaultkSpacing.normal)
                            )
                        }
                    }

                    // Strength Meter Bar (5 segments)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)
                    ) {
                        repeat(5) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .background(
                                        if (index < level) strengthColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons Row: Generar / Regenerar & Copiar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.small)
        ) {
            OutlinedButton(
                enabled = valid,
                onClick = {
                    generated = PasswordGenerator.generate(options.copy(length = length!!))
                    visible = true
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.padding(end = VaultkSpacing.small)
                )
                Text(
                    text = stringResource(
                        if (generated == null) R.string.generator_generate else R.string.generator_regenerate
                    )
                )
            }
            if (generated != null) {
                Button(
                    onClick = { generated?.let(::copy) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.padding(end = VaultkSpacing.small)
                    )
                    Text(text = stringResource(R.string.generator_copy))
                }
            }
        }

        if (onUse != null) {
            Button(
                enabled = generated != null,
                onClick = { generated?.let(onUse) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .bringIntoViewRequester(useButtonRequester),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    modifier = Modifier.padding(end = VaultkSpacing.small)
                )
                Text(text = stringResource(R.string.generator_use))
            }
        }
    }
}

@Composable
private fun GeneratorOption(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = VaultkSpacing.normal, vertical = VaultkSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}
