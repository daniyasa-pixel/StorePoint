package com.munzo.storepoint.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munzo.storepoint.ui.theme.HeroUITheme

enum class HeroColor {
    Primary,
    Success,
    Warning,
    Danger,
    Default
}

enum class HeroButtonVariant {
    Solid,
    Flat,
    Bordered,
    Light
}

enum class HeroSize {
    Sm,
    Md,
    Lg
}

enum class HeroChipVariant {
    Solid,
    Flat,
    Bordered,
    Dot
}

// =========================================================================
// 1. HeroButton (HeroUI v3 Anatomy)
// =========================================================================

@Composable
fun HeroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: HeroColor = HeroColor.Primary,
    variant: HeroButtonVariant = HeroButtonVariant.Solid,
    size: HeroSize = HeroSize.Md,
    enabled: Boolean = true,
    shape: Shape = HeroUITheme.radius.lg,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    text: String? = null,
    contentPadding: PaddingValues? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "hero_button_press_scale"
    )

    val c = HeroUITheme.colors

    val (bg, fg, border) = when (color) {
        HeroColor.Primary -> when (variant) {
            HeroButtonVariant.Solid -> Triple(c.accent, c.accentForeground, null)
            HeroButtonVariant.Flat -> Triple(c.accent.copy(alpha = 0.15f), c.accent, null)
            HeroButtonVariant.Bordered -> Triple(Color.Transparent, c.accent, BorderStroke(1.5.dp, c.accent))
            HeroButtonVariant.Light -> Triple(Color.Transparent, c.accent, null)
        }
        HeroColor.Success -> when (variant) {
            HeroButtonVariant.Solid -> Triple(c.success, c.successForeground, null)
            HeroButtonVariant.Flat -> Triple(c.success.copy(alpha = 0.15f), c.success, null)
            HeroButtonVariant.Bordered -> Triple(Color.Transparent, c.success, BorderStroke(1.5.dp, c.success))
            HeroButtonVariant.Light -> Triple(Color.Transparent, c.success, null)
        }
        HeroColor.Warning -> when (variant) {
            HeroButtonVariant.Solid -> Triple(c.warning, c.warningForeground, null)
            HeroButtonVariant.Flat -> Triple(c.warning.copy(alpha = 0.15f), c.warning, null)
            HeroButtonVariant.Bordered -> Triple(Color.Transparent, c.warning, BorderStroke(1.5.dp, c.warning))
            HeroButtonVariant.Light -> Triple(Color.Transparent, c.warning, null)
        }
        HeroColor.Danger -> when (variant) {
            HeroButtonVariant.Solid -> Triple(c.danger, c.dangerForeground, null)
            HeroButtonVariant.Flat -> Triple(c.danger.copy(alpha = 0.15f), c.danger, null)
            HeroButtonVariant.Bordered -> Triple(Color.Transparent, c.danger, BorderStroke(1.5.dp, c.danger))
            HeroButtonVariant.Light -> Triple(Color.Transparent, c.danger, null)
        }
        HeroColor.Default -> when (variant) {
            HeroButtonVariant.Solid -> Triple(c.defaultBtn, c.defaultForeground, null)
            HeroButtonVariant.Flat -> Triple(c.surfaceSecondary, c.surfaceForeground, null)
            HeroButtonVariant.Bordered -> Triple(Color.Transparent, c.foreground, BorderStroke(1.dp, c.border))
            HeroButtonVariant.Light -> Triple(Color.Transparent, c.foreground, null)
        }
    }

    val (height, hPadding, fontSize, iconSize) = when (size) {
        HeroSize.Sm -> Tuple4(34.dp, 12.dp, 12.sp, 14.dp)
        HeroSize.Md -> Tuple4(44.dp, 16.dp, 14.sp, 18.dp)
        HeroSize.Lg -> Tuple4(52.dp, 22.dp, 16.sp, 20.dp)
    }

    val finalBg = if (enabled) bg else bg.copy(alpha = 0.4f)
    val finalFg = if (enabled) fg else fg.copy(alpha = 0.4f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = finalBg,
        border = if (enabled) border else border?.copy(brush = androidx.compose.ui.graphics.SolidColor(c.border.copy(alpha = 0.3f))),
        interactionSource = interactionSource,
        modifier = modifier
            .height(height)
            .scale(scale)
    ) {
        Row(
            modifier = if (contentPadding != null) Modifier.padding(contentPadding) else Modifier.padding(horizontal = hPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (content != null) {
                content()
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = finalFg,
                        modifier = Modifier.size(iconSize)
                    )
                    if (!text.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                if (!text.isNullOrBlank()) {
                    Text(
                        text = text,
                        color = finalFg,
                        fontSize = fontSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (trailingIcon != null) {
                    if (!text.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = finalFg,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}

@Composable
fun HeroOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: HeroColor = HeroColor.Primary,
    size: HeroSize = HeroSize.Md,
    enabled: Boolean = true,
    shape: Shape = HeroUITheme.radius.lg,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    text: String? = null,
    contentPadding: PaddingValues? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    HeroButton(
        onClick = onClick,
        modifier = modifier,
        color = color,
        variant = HeroButtonVariant.Bordered,
        size = size,
        enabled = enabled,
        shape = shape,
        icon = icon,
        trailingIcon = trailingIcon,
        text = text,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun HeroTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: HeroColor = HeroColor.Primary,
    size: HeroSize = HeroSize.Md,
    enabled: Boolean = true,
    shape: Shape = HeroUITheme.radius.lg,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    text: String? = null,
    contentPadding: PaddingValues? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    HeroButton(
        onClick = onClick,
        modifier = modifier,
        color = color,
        variant = HeroButtonVariant.Light,
        size = size,
        enabled = enabled,
        shape = shape,
        icon = icon,
        trailingIcon = trailingIcon,
        text = text,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun HeroFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: HeroColor = HeroColor.Primary,
    size: HeroSize = HeroSize.Md,
    enabled: Boolean = true,
    shape: Shape = HeroUITheme.radius.lg,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    text: String? = null,
    contentPadding: PaddingValues? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    HeroButton(
        onClick = onClick,
        modifier = modifier,
        color = color,
        variant = HeroButtonVariant.Flat,
        size = size,
        enabled = enabled,
        shape = shape,
        icon = icon,
        trailingIcon = trailingIcon,
        text = text,
        contentPadding = contentPadding,
        content = content
    )
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

// =========================================================================
// 2. HeroCard Compound Component (HeroUI v3 Architecture)
// =========================================================================

@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    shape: Shape = HeroUITheme.radius.xl,
    backgroundColor: Color = HeroUITheme.colors.surface,
    borderColor: Color = HeroUITheme.colors.border,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 0.dp,
    colors: CardColors? = null,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clip(shape).clickable(onClick = onClick)
    } else {
        modifier
    }

    val finalBg = colors?.containerColor ?: backgroundColor
    val finalBorder = border ?: BorderStroke(borderWidth, borderColor)

    Surface(
        modifier = cardModifier,
        shape = shape,
        color = finalBg,
        tonalElevation = elevation,
        border = finalBorder
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    shape: Shape = HeroUITheme.radius.xl,
    backgroundColor: Color = HeroUITheme.colors.surface,
    borderColor: Color = HeroUITheme.colors.border,
    borderWidth: Dp = 1.dp,
    elevation: CardElevation,
    colors: CardColors? = null,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    HeroCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        elevation = 0.dp,
        colors = colors,
        border = border,
        onClick = onClick,
        content = content
    )
}

@Composable
fun HeroCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = HeroUITheme.colors.accent,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(HeroUITheme.radius.md)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HeroUITheme.colors.foreground
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = HeroUITheme.colors.muted
                    )
                }
            }
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(12.dp))
            action()
        }
    }
}

@Composable
fun HeroCardBody(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
    ) {
        content()
    }
}

@Composable
fun HeroCardFooter(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        content()
    }
}

// =========================================================================
// 3. HeroChip (HeroUI v3 Pill Tags)
// =========================================================================

@Composable
fun HeroChip(
    text: String,
    modifier: Modifier = Modifier,
    color: HeroColor = HeroColor.Primary,
    variant: HeroChipVariant = HeroChipVariant.Flat,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val c = HeroUITheme.colors
    val (bg, fg, border) = when (color) {
        HeroColor.Primary -> when (variant) {
            HeroChipVariant.Solid -> Triple(c.accent, c.accentForeground, null)
            HeroChipVariant.Flat -> Triple(c.accent.copy(alpha = 0.14f), c.accent, null)
            HeroChipVariant.Bordered -> Triple(Color.Transparent, c.accent, BorderStroke(1.dp, c.accent))
            HeroChipVariant.Dot -> Triple(c.surfaceSecondary, c.foreground, null)
        }
        HeroColor.Success -> when (variant) {
            HeroChipVariant.Solid -> Triple(c.success, c.successForeground, null)
            HeroChipVariant.Flat -> Triple(c.success.copy(alpha = 0.14f), c.success, null)
            HeroChipVariant.Bordered -> Triple(Color.Transparent, c.success, BorderStroke(1.dp, c.success))
            HeroChipVariant.Dot -> Triple(c.surfaceSecondary, c.foreground, null)
        }
        HeroColor.Warning -> when (variant) {
            HeroChipVariant.Solid -> Triple(c.warning, c.warningForeground, null)
            HeroChipVariant.Flat -> Triple(c.warning.copy(alpha = 0.14f), c.warning, null)
            HeroChipVariant.Bordered -> Triple(Color.Transparent, c.warning, BorderStroke(1.dp, c.warning))
            HeroChipVariant.Dot -> Triple(c.surfaceSecondary, c.foreground, null)
        }
        HeroColor.Danger -> when (variant) {
            HeroChipVariant.Solid -> Triple(c.danger, c.dangerForeground, null)
            HeroChipVariant.Flat -> Triple(c.danger.copy(alpha = 0.14f), c.danger, null)
            HeroChipVariant.Bordered -> Triple(Color.Transparent, c.danger, BorderStroke(1.dp, c.danger))
            HeroChipVariant.Dot -> Triple(c.surfaceSecondary, c.foreground, null)
        }
        HeroColor.Default -> when (variant) {
            HeroChipVariant.Solid -> Triple(c.defaultBtn, c.defaultForeground, null)
            HeroChipVariant.Flat -> Triple(c.surfaceSecondary, c.muted, null)
            HeroChipVariant.Bordered -> Triple(Color.Transparent, c.muted, BorderStroke(1.dp, c.border))
            HeroChipVariant.Dot -> Triple(c.surfaceSecondary, c.foreground, null)
        }
    }

    val chipModifier = if (onClick != null) {
        modifier.clip(HeroUITheme.radius.full).clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = chipModifier,
        shape = HeroUITheme.radius.full,
        color = bg,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (variant == HeroChipVariant.Dot) {
                val dotColor = when (color) {
                    HeroColor.Primary -> c.accent
                    HeroColor.Success -> c.success
                    HeroColor.Warning -> c.warning
                    HeroColor.Danger -> c.danger
                    HeroColor.Default -> c.muted
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = fg,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun HeroFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    color: HeroColor = HeroColor.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    HeroChip(
        text = label,
        modifier = modifier,
        color = if (selected) color else HeroColor.Default,
        variant = if (selected) HeroChipVariant.Solid else HeroChipVariant.Flat,
        icon = icon,
        onClick = if (enabled) onClick else null
    )
}

@Composable
fun HeroFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: HeroColor = HeroColor.Primary,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    label: @Composable () -> Unit
) {
    val c = HeroUITheme.colors
    val (bg, fg, border) = if (selected) {
        when (color) {
            HeroColor.Primary -> Triple(c.accent, c.accentForeground, null)
            HeroColor.Success -> Triple(c.success, c.successForeground, null)
            HeroColor.Warning -> Triple(c.warning, c.warningForeground, null)
            HeroColor.Danger -> Triple(c.danger, c.dangerForeground, null)
            HeroColor.Default -> Triple(c.defaultBtn, c.defaultForeground, null)
        }
    } else {
        Triple(c.surfaceSecondary, c.muted, null)
    }

    val chipModifier = if (enabled) {
        modifier.clip(HeroUITheme.radius.full).clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = chipModifier,
        shape = HeroUITheme.radius.full,
        color = if (enabled) bg else bg.copy(alpha = 0.5f),
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            CompositionLocalProvider(
                LocalContentColor provides fg
            ) {
                label()
            }
        }
    }
}

// =========================================================================
// 4. HeroSegmentedTabs (HeroUI v3 Sliding Pills)
// =========================================================================

@Composable
fun <T> HeroSegmentedTabs(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    iconProvider: ((T) -> ImageVector)? = null
) {
    val c = HeroUITheme.colors
    Surface(
        modifier = modifier,
        shape = HeroUITheme.radius.xl,
        color = c.surfaceSecondary,
        border = BorderStroke(1.dp, c.border)
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val bg by animateColorAsState(
                    targetValue = if (isSelected) c.surface else Color.Transparent,
                    label = "tab_bg"
                )
                val fg by animateColorAsState(
                    targetValue = if (isSelected) c.accent else c.muted,
                    label = "tab_fg"
                )

                Surface(
                    onClick = { onItemSelected(item) },
                    shape = HeroUITheme.radius.lg,
                    color = bg,
                    border = if (isSelected) BorderStroke(1.dp, c.border) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (iconProvider != null) {
                            Icon(
                                imageVector = iconProvider(item),
                                contentDescription = null,
                                tint = fg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = labelProvider(item),
                            color = fg,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 5. Responsive Screen Layout Classes
// =========================================================================

enum class HeroWindowSize {
    Compact,   // Phones, portrait screens (< 600dp)
    Medium,    // Foldables, small tablets, vertical registers (600dp - 839dp)
    Expanded   // Desktop, tablets in landscape, large commercial registers (>= 840dp)
}

@Composable
fun rememberHeroWindowSize(): HeroWindowSize {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp
    return remember(widthDp) {
        when {
            widthDp < 600 -> HeroWindowSize.Compact
            widthDp < 840 -> HeroWindowSize.Medium
            else -> HeroWindowSize.Expanded
        }
    }
}

// =========================================================================
// 6. HeroInput / HeroTextField (HeroUI v3 Inputs)
// =========================================================================

enum class HeroInputVariant {
    Bordered,
    Flat,
    Faded
}

@Composable
fun HeroInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    helperText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    variant: HeroInputVariant = HeroInputVariant.Bordered,
    shape: Shape = HeroUITheme.radius.lg
) {
    HeroInput(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { Text(it, style = MaterialTheme.typography.bodySmall, color = if (isError) HeroUITheme.colors.danger else HeroUITheme.colors.muted) } },
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyMedium, color = HeroUITheme.colors.muted.copy(alpha = 0.6f)) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        isError = isError,
        supportingText = (errorMessage ?: helperText)?.let {
            { Text(it, style = MaterialTheme.typography.labelSmall, color = if (isError) HeroUITheme.colors.danger else HeroUITheme.colors.muted) }
        },
        singleLine = singleLine,
        maxLines = maxLines,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        variant = variant,
        shape = shape
    )
}

@Composable
fun HeroInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    variant: HeroInputVariant = HeroInputVariant.Bordered,
    shape: Shape = HeroUITheme.radius.lg
) {
    val c = HeroUITheme.colors
    val containerBg = when (variant) {
        HeroInputVariant.Bordered -> c.surface
        HeroInputVariant.Flat -> c.surfaceSecondary
        HeroInputVariant.Faded -> c.surfaceSecondary.copy(alpha = 0.5f)
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        isError = isError,
        supportingText = supportingText,
        singleLine = singleLine,
        maxLines = maxLines,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = containerBg,
            unfocusedContainerColor = containerBg,
            disabledContainerColor = containerBg.copy(alpha = 0.4f),
            focusedBorderColor = if (isError) c.danger else c.accent,
            unfocusedBorderColor = if (isError) c.danger else c.border,
            errorBorderColor = c.danger,
            focusedTextColor = c.foreground,
            unfocusedTextColor = c.foreground,
            cursorColor = c.accent
        )
    )
}

// =========================================================================
// 7. HeroSwitch (HeroUI v3 Animated Switches)
// =========================================================================

@Composable
fun HeroSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    description: String? = null,
    color: HeroColor = HeroColor.Primary,
    enabled: Boolean = true
) {
    val c = HeroUITheme.colors
    val activeTrackColor = when (color) {
        HeroColor.Primary -> c.accent
        HeroColor.Success -> c.success
        HeroColor.Warning -> c.warning
        HeroColor.Danger -> c.danger
        HeroColor.Default -> c.foreground
    }

    Row(
        modifier = modifier
            .clip(HeroUITheme.radius.md)
            .clickable(enabled = enabled && onCheckedChange != null) {
                onCheckedChange?.invoke(!checked)
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (!label.isNullOrBlank()) {
            Column(modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) c.foreground else c.muted
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.muted
                    )
                }
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeTrackColor,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = c.muted,
                uncheckedTrackColor = c.surfaceSecondary,
                uncheckedBorderColor = c.border
            )
        )
    }
}

// =========================================================================
// 8. HeroCheckbox (HeroUI v3 Checkboxes)
// =========================================================================

@Composable
fun HeroCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    color: HeroColor = HeroColor.Primary,
    enabled: Boolean = true
) {
    val c = HeroUITheme.colors
    val activeColor = when (color) {
        HeroColor.Primary -> c.accent
        HeroColor.Success -> c.success
        HeroColor.Warning -> c.warning
        HeroColor.Danger -> c.danger
        HeroColor.Default -> c.foreground
    }

    Row(
        modifier = modifier
            .clip(HeroUITheme.radius.md)
            .clickable(enabled = enabled && onCheckedChange != null) {
                onCheckedChange?.invoke(!checked)
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = activeColor,
                uncheckedColor = c.border,
                checkmarkColor = Color.White
            )
        )
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) c.foreground else c.muted
            )
        }
    }
}

// =========================================================================
// 9. HeroAlert (HeroUI v3 Alert Callouts)
// =========================================================================

@Composable
fun HeroAlert(
    description: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    color: HeroColor = HeroColor.Primary,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null
) {
    val c = HeroUITheme.colors
    val statusColor: Color = when (color) {
        HeroColor.Primary -> c.accent
        HeroColor.Success -> c.success
        HeroColor.Warning -> c.warning
        HeroColor.Danger -> c.danger
        HeroColor.Default -> c.muted
    }
    val defaultIcon: ImageVector = when (color) {
        HeroColor.Primary -> Icons.Default.Info
        HeroColor.Success -> Icons.Default.CheckCircle
        HeroColor.Warning -> Icons.Default.Warning
        HeroColor.Danger -> Icons.Default.Error
        HeroColor.Default -> Icons.Default.Info
    }

    Surface(
        shape = HeroUITheme.radius.lg,
        color = statusColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.28f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon ?: defaultIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.foreground
                )
            }
            if (action != null) {
                action()
            }
        }
    }
}

// =========================================================================
// 10. HeroDivider (HeroUI v3 Micro-Border Separators)
// =========================================================================

@Composable
fun HeroDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = HeroUITheme.colors.border
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color
    )
}

// =========================================================================
// 11. HeroSpinner (HeroUI v3 Circular Spinners)
// =========================================================================

@Composable
fun HeroSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.5.dp,
    color: HeroColor = HeroColor.Primary
) {
    val c = HeroUITheme.colors
    val spinnerColor = when (color) {
        HeroColor.Primary -> c.accent
        HeroColor.Success -> c.success
        HeroColor.Warning -> c.warning
        HeroColor.Danger -> c.danger
        HeroColor.Default -> c.foreground
    }
    CircularProgressIndicator(
        modifier = modifier.size(size),
        strokeWidth = strokeWidth,
        color = spinnerColor
    )
}

// =========================================================================
// 12. HeroModal (HeroUI v3 Modals)
// =========================================================================

@Composable
fun HeroModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = HeroUITheme.radius.xl,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = HeroUITheme.colors.surface,
            border = BorderStroke(1.dp, HeroUITheme.colors.border),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                content()
            }
        }
    }
}

