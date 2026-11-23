import AsyncStorage from '@react-native-async-storage/async-storage';

const KEYS = {
  ONBOARDING_DONE: 'sp_onboarding_done',
  CART_DRAFT: 'sp_cart_draft',
  LAST_PAYMENT_METHOD: 'sp_last_payment_method',
  LAST_POS_TAB: 'sp_last_pos_tab',
  LAST_ADMIN_TAB: 'sp_last_admin_tab',
  LAST_SEARCH_QUERY: 'sp_last_search_query',
  DID_RESTORE_CART: 'sp_did_restore_cart',
} as const;

export interface CartDraftLine {
  readonly productId: number;
  readonly quantity: number;
  readonly uomName: string;
  readonly multiplier: number;
  readonly selectedPrice: number;
  readonly discount: number;
}

export const sessionStateCache = {
  async isOnboardingCompleted(): Promise<boolean> {
    const raw = await AsyncStorage.getItem(KEYS.ONBOARDING_DONE);
    return raw === 'true';
  },

  async setOnboardingCompleted(done: boolean): Promise<void> {
    await AsyncStorage.setItem(KEYS.ONBOARDING_DONE, done ? 'true' : 'false');
  },

  async saveCartDraft(lines: readonly CartDraftLine[]): Promise<void> {
    if (lines.length === 0) {
      await AsyncStorage.removeItem(KEYS.CART_DRAFT);
      return;
    }
    await AsyncStorage.setItem(KEYS.CART_DRAFT, JSON.stringify(lines));
  },

  async loadCartDraft(): Promise<CartDraftLine[]> {
    const raw = await AsyncStorage.getItem(KEYS.CART_DRAFT);
    if (!raw) return [];
    try {
      const parsed: unknown = JSON.parse(raw);
      return Array.isArray(parsed) ? (parsed as CartDraftLine[]) : [];
    } catch {
      return [];
    }
  },

  async clearCartDraft(): Promise<void> {
    await AsyncStorage.removeItem(KEYS.CART_DRAFT);
  },

  async setLastPaymentMethod(method: string): Promise<void> {
    await AsyncStorage.setItem(KEYS.LAST_PAYMENT_METHOD, method);
  },

  async getLastPaymentMethod(): Promise<string> {
    return (await AsyncStorage.getItem(KEYS.LAST_PAYMENT_METHOD)) ?? 'CASH';
  },

  async setLastAdminTab(tab: string): Promise<void> {
    await AsyncStorage.setItem(KEYS.LAST_ADMIN_TAB, tab);
  },

  async getLastAdminTab(): Promise<string> {
    return (await AsyncStorage.getItem(KEYS.LAST_ADMIN_TAB)) ?? 'products';
  },
};
