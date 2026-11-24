const mockStorage = new Map<string, string>();

jest.mock('@react-native-async-storage/async-storage', () => ({
  __esModule: true,
  default: {
    getItem: jest.fn((key: string) => Promise.resolve(mockStorage.get(key) ?? null)),
    setItem: jest.fn((key: string, value: string) => {
      mockStorage.set(key, value);
      return Promise.resolve();
    }),
    removeItem: jest.fn((key: string) => {
      mockStorage.delete(key);
      return Promise.resolve();
    }),
  },
}));

import AsyncStorage from '@react-native-async-storage/async-storage';
import { sessionStateCache, type CartDraftLine } from '../sessionStateCache';

describe('sessionStateCache', () => {
  beforeEach(() => {
    mockStorage.clear();
    jest.clearAllMocks();
  });

  describe('cart draft persistence', () => {
    it('saves and loads cart draft lines', async () => {
      // Arrange
      const draft: CartDraftLine[] = [
        {
          productId: 1,
          quantity: 3,
          uomName: 'pc',
          multiplier: 1,
          selectedPrice: 25,
          discount: 2,
        },
        {
          productId: 2,
          quantity: 1,
          uomName: 'pack-10s',
          multiplier: 10,
          selectedPrice: 95,
          discount: 0,
        },
      ];

      // Act
      await sessionStateCache.saveCartDraft(draft);
      const loaded = await sessionStateCache.loadCartDraft();

      // Assert
      expect(AsyncStorage.setItem).toHaveBeenCalledWith('sp_cart_draft', JSON.stringify(draft));
      expect(loaded).toEqual(draft);
    });

    it('removes stored cart draft when saving an empty array', async () => {
      // Arrange
      await sessionStateCache.saveCartDraft([
        {
          productId: 5,
          quantity: 1,
          uomName: 'pc',
          multiplier: 1,
          selectedPrice: 10,
          discount: 0,
        },
      ]);

      // Act
      await sessionStateCache.saveCartDraft([]);
      const loaded = await sessionStateCache.loadCartDraft();

      // Assert
      expect(AsyncStorage.removeItem).toHaveBeenCalledWith('sp_cart_draft');
      expect(loaded).toEqual([]);
    });

    it('falls back gracefully to an empty array on malformed JSON', async () => {
      // Arrange
      mockStorage.set('sp_cart_draft', '{invalid-json');

      // Act
      const loaded = await sessionStateCache.loadCartDraft();

      // Assert
      expect(loaded).toEqual([]);
    });

    it('falls back gracefully to an empty array when stored JSON is not an array', async () => {
      // Arrange
      mockStorage.set('sp_cart_draft', JSON.stringify({ productId: 1 }));

      // Act
      const loaded = await sessionStateCache.loadCartDraft();

      // Assert
      expect(loaded).toEqual([]);
    });
  });

  describe('onboarding completed flag', () => {
    it('returns false when unset and persists true/false transitions', async () => {
      // Arrange & Act
      const initial = await sessionStateCache.isOnboardingCompleted();
      await sessionStateCache.setOnboardingCompleted(true);
      const afterTrue = await sessionStateCache.isOnboardingCompleted();
      await sessionStateCache.setOnboardingCompleted(false);
      const afterFalse = await sessionStateCache.isOnboardingCompleted();

      // Assert
      expect(initial).toBe(false);
      expect(afterTrue).toBe(true);
      expect(afterFalse).toBe(false);
    });
  });

  describe('theme mode persistence', () => {
    it('defaults to system when unset or invalid and persists light/dark/system', async () => {
      // Arrange & Act
      const initial = await sessionStateCache.getThemeMode();
      await sessionStateCache.setThemeMode('dark');
      const afterDark = await sessionStateCache.getThemeMode();
      await sessionStateCache.setThemeMode('light');
      const afterLight = await sessionStateCache.getThemeMode();
      mockStorage.set('sp_theme_mode', 'unexpected-value');
      const afterInvalid = await sessionStateCache.getThemeMode();

      // Assert
      expect(initial).toBe('system');
      expect(afterDark).toBe('dark');
      expect(afterLight).toBe('light');
      expect(afterInvalid).toBe('system');
    });
  });

  describe('admin contact persistence', () => {
    it('saves trimmed admin contact info and falls back safely when unset or malformed', async () => {
      // Arrange & Act
      const initial = await sessionStateCache.getAdminContact();
      await sessionStateCache.saveAdminContact('  Maria Santos  ', '  +63 917 555 0199  ');
      const saved = await sessionStateCache.getAdminContact();
      mockStorage.set('sp_admin_contact', '{corrupt-json');
      const corrupted = await sessionStateCache.getAdminContact();

      // Assert
      expect(initial).toEqual({ name: '', phone: '' });
      expect(saved).toEqual({ name: 'Maria Santos', phone: '+63 917 555 0199' });
      expect(corrupted).toEqual({ name: '', phone: '' });
    });
  });
});
