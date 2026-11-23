/**
 * StorePoint POS Design System Tokens — Parity with Kotlin HeroUITheme.kt.
 * Used for native APIs (StatusBar, SVG charts, icon tints) where Tailwind classes
 * cannot be passed directly. UI layout/styling MUST use Uniwind + HeroUI Native.
 */
export const HERO_COLORS = {
  light: {
    background: '#F9FAFB',
    foreground: '#111827',
    surface: '#FFFFFF',
    surfaceSecondary: '#F3F4F6',
    surfaceTertiary: '#E5E7EB',
    border: '#E5E7EB',
    muted: '#6B7280',
    accent: '#006FEE',
    accentForeground: '#FFFFFF',
    success: '#17C964',
    successForeground: '#03220E',
    warning: '#F5A524',
    warningForeground: '#2E1901',
    danger: '#F31260',
    dangerForeground: '#FFFFFF',
  },
  dark: {
    background: '#0C0C0E',
    foreground: '#F4F4F5',
    surface: '#18181B',
    surfaceSecondary: '#27272A',
    surfaceTertiary: '#3F3F46',
    border: '#2E2E33',
    muted: '#A1A1AA',
    accent: '#006FEE',
    accentForeground: '#FFFFFF',
    success: '#17C964',
    successForeground: '#03220E',
    warning: '#F5A524',
    warningForeground: '#2E1901',
    danger: '#F31260',
    dangerForeground: '#FFFFFF',
  },
  brand: {
    gcash: '#007DFE',
    maya: '#00B86B',
    load: '#F59E0B',
  },
} as const;

export const HERO_RADIUS = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  full: 9999,
} as const;

export const LAYOUT_BREAKPOINTS = {
  COMPACT_MAX: 600,
  MEDIUM_MAX: 840,
  DRAWER_WIDTH_RATIO: 0.85,
  DRAWER_MAX_WIDTH: 320,
} as const;
