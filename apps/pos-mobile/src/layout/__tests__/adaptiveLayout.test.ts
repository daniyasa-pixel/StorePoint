import {
  getAdaptiveDrawerWidth,
  getAdaptiveLayoutMetrics,
  getWindowLayout,
  isCompactLayout,
  resolveScanSearchCopy,
} from '../adaptiveLayout';
import { APP_ROUTES, resolveHomeRouteForRole } from '@/constants/routes';

describe('AdaptiveLayout contract', () => {
  it('classifies Compact (< 600dp), Medium (600-839dp), and Expanded (>= 840dp)', () => {
    expect(getWindowLayout(360)).toBe('Compact');
    expect(getWindowLayout(599)).toBe('Compact');
    expect(isCompactLayout(599)).toBe(true);

    expect(getWindowLayout(600)).toBe('Medium');
    expect(getWindowLayout(839)).toBe('Medium');
    expect(isCompactLayout(600)).toBe(false);

    expect(getWindowLayout(840)).toBe('Expanded');
    expect(getWindowLayout(1280)).toBe('Expanded');
  });

  it('caps adaptive drawer width at 85% up to 320dp', () => {
    expect(getAdaptiveDrawerWidth(320)).toBe(272);
    expect(getAdaptiveDrawerWidth(400)).toBe(320);
    expect(getAdaptiveDrawerWidth(1024)).toBe(320);
  });

  it('resolves ScanSearchField copy tiers without sibling field', () => {
    expect(resolveScanSearchCopy(420, false)).toEqual({
      label: 'Search Item / Barcode',
      placeholder: 'Scan or type a name, SKU, or barcode',
    });
    expect(resolveScanSearchCopy(300, false)).toEqual({
      label: 'Search Item',
      placeholder: 'Scan or type...',
    });
    expect(resolveScanSearchCopy(220, false)).toEqual({
      label: 'Search',
      placeholder: 'Scan...',
    });
    expect(resolveScanSearchCopy(180, false)).toEqual({
      label: 'Scan',
      placeholder: '',
    });
  });

  it('reserves sibling field width when Qty input shares the scanner row', () => {
    expect(resolveScanSearchCopy(420, true)).toEqual({
      label: 'Search Item',
      placeholder: 'Scan or type...',
    });
    expect(resolveScanSearchCopy(300, true)).toEqual({
      label: 'Scan',
      placeholder: '',
    });
  });

  it('routes INVENTORY role to Inventory Portal and CASHIER/ADMIN to Cashier POS', () => {
    expect(resolveHomeRouteForRole('INVENTORY')).toBe(APP_ROUTES.INVENTORY_PORTAL);
    expect(resolveHomeRouteForRole('CASHIER')).toBe(APP_ROUTES.CASHIER_POS);
    expect(resolveHomeRouteForRole('ADMIN')).toBe(APP_ROUTES.CASHIER_POS);
  });

  describe('getAdaptiveLayoutMetrics', () => {
    it('computes SmallPhone metrics in portrait', () => {
      expect(getAdaptiveLayoutMetrics(360, 640)).toEqual({
        windowLayout: 'Compact',
        sizeClass: 'SmallPhone',
        orientation: 'portrait',
        isShortHeight: false,
        useSplitPane: false,
        catalogColumns: 2,
        kpiColumns: 1,
        catalogFlex: 1,
        cartFlex: 1,
        containerPaddingClass: 'p-2.5',
        dialogMaxWidthClass: 'max-w-sm',
      });
    });

    it('computes Phone metrics in portrait and landscape with short height', () => {
      expect(getAdaptiveLayoutMetrics(412, 915)).toEqual({
        windowLayout: 'Compact',
        sizeClass: 'Phone',
        orientation: 'portrait',
        isShortHeight: false,
        useSplitPane: false,
        catalogColumns: 2,
        kpiColumns: 2,
        catalogFlex: 1,
        cartFlex: 1,
        containerPaddingClass: 'p-3',
        dialogMaxWidthClass: 'max-w-md',
      });

      expect(getAdaptiveLayoutMetrics(580, 360)).toEqual({
        windowLayout: 'Compact',
        sizeClass: 'Phone',
        orientation: 'landscape',
        isShortHeight: true,
        useSplitPane: false,
        catalogColumns: 2,
        kpiColumns: 2,
        catalogFlex: 1,
        cartFlex: 1,
        containerPaddingClass: 'p-3',
        dialogMaxWidthClass: 'max-w-md',
      });
    });

    it('computes Foldable metrics with and without split pane', () => {
      expect(getAdaptiveLayoutMetrics(720, 820)).toEqual({
        windowLayout: 'Medium',
        sizeClass: 'Foldable',
        orientation: 'portrait',
        isShortHeight: false,
        useSplitPane: true,
        catalogColumns: 2,
        kpiColumns: 2,
        catalogFlex: 1.2,
        cartFlex: 1,
        containerPaddingClass: 'p-4',
        dialogMaxWidthClass: 'max-w-md',
      });

      expect(getAdaptiveLayoutMetrics(720, 390)).toEqual({
        windowLayout: 'Medium',
        sizeClass: 'Foldable',
        orientation: 'landscape',
        isShortHeight: true,
        useSplitPane: false,
        catalogColumns: 3,
        kpiColumns: 2,
        catalogFlex: 1.2,
        cartFlex: 1,
        containerPaddingClass: 'p-4',
        dialogMaxWidthClass: 'max-w-md',
      });
    });

    it('computes Tablet metrics in portrait and landscape', () => {
      expect(getAdaptiveLayoutMetrics(900, 1200)).toEqual({
        windowLayout: 'Expanded',
        sizeClass: 'Tablet',
        orientation: 'portrait',
        isShortHeight: false,
        useSplitPane: true,
        catalogColumns: 3,
        kpiColumns: 3,
        catalogFlex: 1.5,
        cartFlex: 1,
        containerPaddingClass: 'p-4',
        dialogMaxWidthClass: 'max-w-lg',
      });

      expect(getAdaptiveLayoutMetrics(1024, 600)).toEqual({
        windowLayout: 'Expanded',
        sizeClass: 'Tablet',
        orientation: 'landscape',
        isShortHeight: false,
        useSplitPane: true,
        catalogColumns: 3,
        kpiColumns: 3,
        catalogFlex: 1.5,
        cartFlex: 1,
        containerPaddingClass: 'p-4',
        dialogMaxWidthClass: 'max-w-lg',
      });
    });

    it('computes LargeTerminal metrics in landscape', () => {
      expect(getAdaptiveLayoutMetrics(1280, 800)).toEqual({
        windowLayout: 'Expanded',
        sizeClass: 'LargeTerminal',
        orientation: 'landscape',
        isShortHeight: false,
        useSplitPane: true,
        catalogColumns: 4,
        kpiColumns: 4,
        catalogFlex: 1.5,
        cartFlex: 1,
        containerPaddingClass: 'p-6',
        dialogMaxWidthClass: 'max-w-xl',
      });
    });
  });
});
