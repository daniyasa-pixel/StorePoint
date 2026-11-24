import { LAYOUT_BREAKPOINTS } from '@/theme/tokens';

/**
 * Single-source-of-truth adaptive layout contract for StorePoint POS.
 * Direct TypeScript port of Kotlin ui/layout/AdaptiveLayout.kt.
 *
 * Every screen derives responsive layout from WindowLayout instead of
 * ad-hoc width checks.
 */
export type WindowLayout = 'Compact' | 'Medium' | 'Expanded';

export type DeviceSizeClass =
  | 'SmallPhone'
  | 'Phone'
  | 'Foldable'
  | 'Tablet'
  | 'LargeTerminal';

export type LayoutOrientation = 'portrait' | 'landscape';

export interface AdaptiveLayoutMetrics {
  readonly windowLayout: WindowLayout;
  readonly sizeClass: DeviceSizeClass;
  readonly orientation: LayoutOrientation;
  readonly isShortHeight: boolean;
  readonly useSplitPane: boolean;
  readonly catalogColumns: number;
  readonly kpiColumns: number;
  readonly catalogFlex: number;
  readonly cartFlex: number;
  readonly containerPaddingClass: string;
  readonly dialogMaxWidthClass: string;
}

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

function getDeviceSizeClass(widthDp: number): DeviceSizeClass {
  if (widthDp < 380) return 'SmallPhone';
  if (widthDp < 600) return 'Phone';
  if (widthDp < 840) return 'Foldable';
  if (widthDp < 1200) return 'Tablet';
  return 'LargeTerminal';
}

export function getAdaptiveLayoutMetrics(
  widthDp: number,
  heightDp: number,
): AdaptiveLayoutMetrics {
  const windowLayout = getWindowLayout(widthDp);
  const sizeClass = getDeviceSizeClass(widthDp);
  const orientation: LayoutOrientation = widthDp > heightDp ? 'landscape' : 'portrait';
  const isShortHeight = heightDp < 520;
  const useSplitPane = widthDp >= 600 && heightDp >= 420;

  let catalogColumns: number;
  switch (sizeClass) {
    case 'SmallPhone':
    case 'Phone':
      catalogColumns = 2;
      break;
    case 'Foldable':
      catalogColumns = useSplitPane ? 2 : 3;
      break;
    case 'Tablet':
      catalogColumns = 3;
      break;
    case 'LargeTerminal':
      catalogColumns = 4;
      break;
  }

  let kpiColumns: number;
  switch (sizeClass) {
    case 'SmallPhone':
      kpiColumns = 1;
      break;
    case 'Phone':
    case 'Foldable':
      kpiColumns = 2;
      break;
    case 'Tablet':
      kpiColumns = 3;
      break;
    case 'LargeTerminal':
      kpiColumns = 4;
      break;
  }

  let catalogFlex: number;
  let cartFlex: number;
  if (windowLayout === 'Expanded' || sizeClass === 'LargeTerminal') {
    catalogFlex = 1.5;
    cartFlex = 1;
  } else if (windowLayout === 'Medium') {
    catalogFlex = 1.2;
    cartFlex = 1;
  } else {
    catalogFlex = 1;
    cartFlex = 1;
  }

  let containerPaddingClass: string;
  switch (sizeClass) {
    case 'SmallPhone':
      containerPaddingClass = 'p-2.5';
      break;
    case 'Phone':
      containerPaddingClass = 'p-3';
      break;
    case 'Foldable':
    case 'Tablet':
      containerPaddingClass = 'p-4';
      break;
    case 'LargeTerminal':
      containerPaddingClass = 'p-6';
      break;
  }

  let dialogMaxWidthClass: string;
  switch (sizeClass) {
    case 'SmallPhone':
      dialogMaxWidthClass = 'max-w-sm';
      break;
    case 'Phone':
    case 'Foldable':
      dialogMaxWidthClass = 'max-w-md';
      break;
    case 'Tablet':
      dialogMaxWidthClass = 'max-w-lg';
      break;
    case 'LargeTerminal':
      dialogMaxWidthClass = 'max-w-xl';
      break;
  }

  return {
    windowLayout,
    sizeClass,
    orientation,
    isShortHeight,
    useSplitPane,
    catalogColumns,
    kpiColumns,
    catalogFlex,
    cartFlex,
    containerPaddingClass,
    dialogMaxWidthClass,
  };
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
