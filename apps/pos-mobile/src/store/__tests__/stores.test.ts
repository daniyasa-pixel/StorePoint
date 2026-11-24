jest.mock('@/storage/sessionStateCache', () => ({
  sessionStateCache: {
    saveCartDraft: jest.fn().mockResolvedValue(undefined),
    loadCartDraft: jest.fn().mockResolvedValue([]),
    clearCartDraft: jest.fn().mockResolvedValue(undefined),
    setThemeMode: jest.fn().mockResolvedValue(undefined),
    getThemeMode: jest.fn().mockResolvedValue('system'),
  },
}));

import { Uniwind } from 'uniwind';
import type { Product, UomOption } from '@/db/types';
import { sessionStateCache } from '@/storage/sessionStateCache';
import { useAppBootstrapStore, type ActiveSessionUser } from '../appBootstrapStore';
import { usePosCartStore } from '../posCartStore';

function makeProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: 1,
    name: 'Bottled Water',
    categoryId: 1,
    price: 20,
    stockCount: 100,
    barcode: '480000000001',
    expirationDate: null,
    cost: 12,
    hasStick10s: false,
    priceStick10s: 0,
    hasStick20s: false,
    priceStick20s: 0,
    hasReam: false,
    priceReam: 0,
    hasMasterCase: false,
    priceMasterCase: 0,
    hasCustomUom: false,
    customUomName: '',
    customUomMultiplier: 1,
    customUomPrice: 0,
    barcode10s: '',
    barcode20s: '',
    barcodeReam: '',
    barcodeMasterCase: '',
    barcodeCustomUom: '',
    baseUom: 'pc',
    trackInventory: true,
    isArchived: false,
    ...overrides,
  };
}

describe('usePosCartStore', () => {
  beforeEach(() => {
    usePosCartStore.getState().clearCart();
    usePosCartStore.setState({ restoredNoticeCount: 0 });
  });

  afterEach(() => {
    usePosCartStore.getState().clearCart();
  });

  describe('addProduct', () => {
    it('adds a new cart line with default baseUom when no UOM option is provided', () => {
      // Arrange
      const product = makeProduct({ id: 10, price: 25, baseUom: 'bottle' });

      // Act
      usePosCartStore.getState().addProduct(product);

      // Assert
      const { lines } = usePosCartStore.getState();
      expect(lines).toHaveLength(1);
      expect(lines[0]).toMatchObject({
        product,
        quantity: 1,
        uomName: 'bottle',
        multiplier: 1,
        selectedPrice: 25,
        checked: true,
        itemDiscount: 0,
      });
    });

    it('increments quantity when adding an existing product with the same UOM', () => {
      // Arrange
      const product = makeProduct({ id: 11, price: 15, baseUom: 'pc' });
      usePosCartStore.getState().addProduct(product, undefined, 2);

      // Act
      usePosCartStore.getState().addProduct(product, undefined, 3);

      // Assert
      const { lines } = usePosCartStore.getState();
      expect(lines).toHaveLength(1);
      expect(lines[0]?.quantity).toBe(5);
    });

    it('keeps variant/UOM lines distinct for the same product by uomName', () => {
      // Arrange
      const product = makeProduct({ id: 12, price: 10, baseUom: 'stick' });
      const packUom: UomOption = { name: 'pack-10s', multiplier: 10, price: 95 };

      // Act
      usePosCartStore.getState().addProduct(product);
      usePosCartStore.getState().addProduct(product, packUom, 2);

      // Assert
      const { lines } = usePosCartStore.getState();
      expect(lines).toHaveLength(2);
      expect(lines[0]).toMatchObject({ uomName: 'stick', quantity: 1, selectedPrice: 10 });
      expect(lines[1]).toMatchObject({
        uomName: 'pack-10s',
        quantity: 2,
        multiplier: 10,
        selectedPrice: 95,
      });
    });
  });

  describe('updateQuantity', () => {
    it('increments and decrements quantity for a matching product and UOM', () => {
      // Arrange
      const product = makeProduct({ id: 20, baseUom: 'pc' });
      usePosCartStore.getState().addProduct(product, undefined, 2);

      // Act
      usePosCartStore.getState().updateQuantity(20, 'pc', 3);
      const afterIncrement = usePosCartStore.getState().lines[0]?.quantity;
      usePosCartStore.getState().updateQuantity(20, 'pc', -1);
      const afterDecrement = usePosCartStore.getState().lines[0]?.quantity;

      // Assert
      expect(afterIncrement).toBe(5);
      expect(afterDecrement).toBe(4);
    });

    it('removes the line when updated quantity drops to 0 or below', () => {
      // Arrange
      const product = makeProduct({ id: 21, baseUom: 'pc' });
      usePosCartStore.getState().addProduct(product, undefined, 2);

      // Act
      usePosCartStore.getState().updateQuantity(21, 'pc', -2);

      // Assert
      expect(usePosCartStore.getState().lines).toHaveLength(0);
    });
  });

  describe('toggleChecked', () => {
    it('toggles checked state only for the targeted line', () => {
      // Arrange
      const productA = makeProduct({ id: 30, baseUom: 'pc' });
      const productB = makeProduct({ id: 31, baseUom: 'pc' });
      usePosCartStore.getState().addProduct(productA);
      usePosCartStore.getState().addProduct(productB);

      // Act
      usePosCartStore.getState().toggleChecked(30, 'pc');

      // Assert
      const { lines } = usePosCartStore.getState();
      expect(lines.find((l) => l.product.id === 30)?.checked).toBe(false);
      expect(lines.find((l) => l.product.id === 31)?.checked).toBe(true);
    });
  });

  describe('setOrderDiscount and clearCart', () => {
    it('sets order discount and resets lines and discounts on clearCart', () => {
      // Arrange
      const product = makeProduct({ id: 40, price: 100 });
      usePosCartStore.getState().addProduct(product, undefined, 2);

      // Act
      usePosCartStore.getState().setOrderDiscount('PERCENTAGE', 10);
      const beforeClear = usePosCartStore.getState();
      usePosCartStore.getState().clearCart();
      const afterClear = usePosCartStore.getState();

      // Assert
      expect(beforeClear.orderDiscountType).toBe('PERCENTAGE');
      expect(beforeClear.orderDiscountValue).toBe(10);
      expect(afterClear.lines).toEqual([]);
      expect(afterClear.orderDiscountType).toBe('NONE');
      expect(afterClear.orderDiscountValue).toBe(0);
    });
  });
});

describe('useAppBootstrapStore', () => {
  const initialState = {
    isDatabaseReady: false,
    isOnboardingCompleted: false,
    isSetupCompleted: false,
    storeName: 'StorePoint',
    currencySymbol: '₱',
    activeUser: null,
    isKioskModeActive: false,
    isAlwaysOnEnabled: false,
    themeMode: 'system' as const,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    useAppBootstrapStore.setState(initialState);
  });

  it('marks onboarding completed', () => {
    // Arrange
    expect(useAppBootstrapStore.getState().isOnboardingCompleted).toBe(false);

    // Act
    useAppBootstrapStore.getState().completeOnboarding();

    // Assert
    expect(useAppBootstrapStore.getState().isOnboardingCompleted).toBe(true);
  });

  it('completes setup with trimmed store name and currency symbol defaults', () => {
    // Arrange & Act
    useAppBootstrapStore.getState().completeSetup('  Sari-Sari Hub  ', '$');

    // Assert
    expect(useAppBootstrapStore.getState()).toMatchObject({
      isSetupCompleted: true,
      storeName: 'Sari-Sari Hub',
      currencySymbol: '$',
    });

    // Act: blank values fall back to defaults
    useAppBootstrapStore.getState().completeSetup('   ', '');

    // Assert
    expect(useAppBootstrapStore.getState()).toMatchObject({
      isSetupCompleted: true,
      storeName: 'StorePoint',
      currencySymbol: '₱',
    });
  });

  it('sets active user, toggles kiosk mode, and clears active user on logout', () => {
    // Arrange
    const user: ActiveSessionUser = {
      id: 7,
      username: 'cashier1',
      fullName: 'Maria Santos',
      role: 'CASHIER',
      pinResetRequired: false,
    };

    // Act
    useAppBootstrapStore.getState().setActiveUser(user);
    useAppBootstrapStore.getState().setKioskModeActive(true);

    // Assert
    expect(useAppBootstrapStore.getState().activeUser).toEqual(user);
    expect(useAppBootstrapStore.getState().isKioskModeActive).toBe(true);

    // Act
    useAppBootstrapStore.getState().logout();

    // Assert
    expect(useAppBootstrapStore.getState().activeUser).toBeNull();
  });

  it('updates themeMode, syncs Uniwind.setTheme, and persists via sessionStateCache', () => {
    // Arrange
    expect(useAppBootstrapStore.getState().themeMode).toBe('system');

    // Act
    useAppBootstrapStore.getState().setThemeMode('dark');

    // Assert
    expect(useAppBootstrapStore.getState().themeMode).toBe('dark');
    expect(Uniwind.setTheme).toHaveBeenCalledWith('dark');
    expect(sessionStateCache.setThemeMode).toHaveBeenCalledWith('dark');
  });
});
