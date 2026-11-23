import {
  getAdaptiveDrawerWidth,
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
});
