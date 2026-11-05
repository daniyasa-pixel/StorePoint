package com.munzo.storepoint.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * HeroUI v3 Design Tokens & Color Palettes for StorePoint POS.
 * Directly translated from official HeroUI v3 design system variables:
 * - Accent: Electric Royal Blue (oklch 62% ~ #006FEE)
 * - Success: Vivid Emerald Green (oklch 73% ~ #17C964)
 * - Warning: Warm Golden Amber (oklch 82% ~ #F5A524)
 * - Danger: Vivid Rose Crimson (oklch 59% ~ #F31260)
 * - Layered Surface Tiers: Background, Surface, SurfaceSecondary, SurfaceTertiary
 */
data class HeroUIColors(
    val background: Color,
    val foreground: Color,
    val surface: Color,
    val surfaceForeground: Color,
    val surfaceSecondary: Color,
    val surfaceTertiary: Color,
    val overlay: Color,
    val border: Color,
    val separator: Color,
    val fieldBackground: Color,
    val fieldBorder: Color,
    val muted: Color,
    val accent: Color,
    val accentForeground: Color,
    val success: Color,
    val successForeground: Color,
    val warning: Color,
    val warningForeground: Color,
    val danger: Color,
    val dangerForeground: Color,
    val defaultBtn: Color,
    val defaultForeground: Color,
    val isDark: Boolean
)

val HeroUILightColors = HeroUIColors(
    background = Color(0xFFF9FAFB),
    foreground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    surfaceForeground = Color(0xFF111827),
    surfaceSecondary = Color(0xFFF3F4F6),
    surfaceTertiary = Color(0xFFE5E7EB),
    overlay = Color(0xFFFFFFFF),
    border = Color(0xFFE5E7EB),
    separator = Color(0xFFE5E7EB),
    fieldBackground = Color(0xFFF3F4F6),
    fieldBorder = Color(0xFFD1D5DB),
    muted = Color(0xFF6B7280),
    accent = Color(0xFF006FEE),
    accentForeground = Color(0xFFFFFFFF),
    success = Color(0xFF17C964),
    successForeground = Color(0xFF03220E),
    warning = Color(0xFFF5A524),
    warningForeground = Color(0xFF2E1901),
    danger = Color(0xFFF31260),
    dangerForeground = Color(0xFFFFFFFF),
    defaultBtn = Color(0xFFE4E4E7),
    defaultForeground = Color(0xFF18181B),
    isDark = false
)

val HeroUIDarkColors = HeroUIColors(
    background = Color(0xFF0C0C0E),
    foreground = Color(0xFFF4F4F5),
    surface = Color(0xFF18181B),
    surfaceForeground = Color(0xFFF4F4F5),
    surfaceSecondary = Color(0xFF27272A),
    surfaceTertiary = Color(0xFF3F3F46),
    overlay = Color(0xFF1E1E22),
    border = Color(0xFF2E2E33),
    separator = Color(0xFF27272A),
    fieldBackground = Color(0xFF202024),
    fieldBorder = Color(0xFF3F3F46),
    muted = Color(0xFFA1A1AA),
    accent = Color(0xFF006FEE),
    accentForeground = Color(0xFFFFFFFF),
    success = Color(0xFF17C964),
    successForeground = Color(0xFF03220E),
    warning = Color(0xFFF5A524),
    warningForeground = Color(0xFF2E1901),
    danger = Color(0xFFF31260),
    dangerForeground = Color(0xFFFFFFFF),
    defaultBtn = Color(0xFF27272A),
    defaultForeground = Color(0xFFF4F4F5),
    isDark = true
)

object HeroUIRadius {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(20.dp)
    val xxl = RoundedCornerShape(24.dp)
    val full = RoundedCornerShape(9999.dp)
}

val LocalHeroUIColors = staticCompositionLocalOf { HeroUILightColors }

object HeroUITheme {
    val colors: HeroUIColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHeroUIColors.current

    val radius = HeroUIRadius
}

@Composable
fun HeroUIThemeProvider(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) HeroUIDarkColors else HeroUILightColors
    CompositionLocalProvider(LocalHeroUIColors provides colors) {
        content()
    }
}

