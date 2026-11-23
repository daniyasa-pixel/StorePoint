import { LAYOUT_BREAKPOINTS } from '@/theme/tokens';

/**
 * Single-source-of-truth adaptive layout contract for StorePoint POS.
 * Direct TypeScript port of Kotlin ui/layout/AdaptiveLayout.kt.
 *
 * Every screen derives responsive layout from WindowLayout instead of
 * ad-hoc width checks.
 */
export type WindowLayout = 'Compact' | 'Medium' | 'Expanded';

export interface ScanSearchCopy {
  readonly label: string;
  readonly placeholder: string;
}

export const SCAN_SEARCH_METRICS = {
  FIELD_CHROME_RESERVE: 56,
  TRAILING_SLOT_WIDTH: 48,
  SIBLING_RESERVE: 92,
} as const;

export function getWindowLayout(widthDp: number): WindowLayout {
  if (widthDp < LAYOUT_BREAKPOINTS.COMPACT_MAX) {
    return 'Compact';
  }
  if (widthDp < LAYOUT_BREAKPOINTS.MEDIUM_MAX) {
    return 'Medium';
  }
  return 'Expanded';
}

export function isCompactLayout(widthDp: number): boolean {
  return getWindowLayout(widthDp) === 'Compact';
}

export function getAdaptiveDrawerWidth(screenWidthDp: number): number {
  return Math.min(
    Math.round(screenWidthDp * LAYOUT_BREAKPOINTS.DRAWER_WIDTH_RATIO),
    LAYOUT_BREAKPOINTS.DRAWER_MAX_WIDTH,
  );
}

/**
 * Resolves width-aware scan/search field copy across phones, 7" tablets,
 * and 10" POS terminals without truncating labels or placeholders.
 */
export function resolveScanSearchCopy(
  availableWidthDp: number,
  hasSiblingField = false,
): ScanSearchCopy {
  const reserved =
    SCAN_SEARCH_METRICS.FIELD_CHROME_RESERVE +
    SCAN_SEARCH_METRICS.TRAILING_SLOT_WIDTH +
    (hasSiblingField ? SCAN_SEARCH_METRICS.SIBLING_RESERVE : 0);

  const textWidth = availableWidthDp - reserved;

  if (textWidth >= 300) {
    return {
      label: 'Search Item / Barcode',
      placeholder: 'Scan or type a name, SKU, or barcode',
    };
  }
  if (textWidth >= 190) {
    return {
      label: 'Search Item',
      placeholder: 'Scan or type...',
    };
  }
  if (textWidth >= 110) {
    return {
      label: 'Search',
      placeholder: 'Scan...',
    };
  }
  return {
    label: 'Scan',
    placeholder: '',
  };
}
