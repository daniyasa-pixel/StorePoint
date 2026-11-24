import { Uniwind } from 'uniwind';
import { create } from 'zustand';
import type { UserRole } from '@/constants/routes';
import { DEFAULT_CURRENCY_SYMBOL } from '@/domain/money';
import { sessionStateCache, type ThemeMode } from '@/storage/sessionStateCache';

export interface ActiveSessionUser {
  readonly id: number;
  readonly username: string;
  readonly fullName: string;
  readonly role: UserRole;
  readonly pinResetRequired: boolean;
}

export interface AppBootstrapState {
  readonly isDatabaseReady: boolean;
  readonly isOnboardingCompleted: boolean;
  readonly isSetupCompleted: boolean;
  readonly storeName: string;
  readonly currencySymbol: string;
  readonly activeUser: ActiveSessionUser | null;
  readonly isKioskModeActive: boolean;
  readonly isAlwaysOnEnabled: boolean;
  readonly themeMode: ThemeMode;

  setDatabaseReady: (ready: boolean) => void;
  completeOnboarding: () => void;
  completeSetup: (storeName: string, currencySymbol: string) => void;
  setActiveUser: (user: ActiveSessionUser | null) => void;
  setKioskModeActive: (active: boolean) => void;
  setAlwaysOnEnabled: (enabled: boolean) => void;
  setThemeMode: (mode: ThemeMode) => void;
  logout: () => void;
}

export const useAppBootstrapStore = create<AppBootstrapState>((set) => ({
  isDatabaseReady: false,
  isOnboardingCompleted: false,
  isSetupCompleted: false,
  storeName: 'StorePoint',
  currencySymbol: DEFAULT_CURRENCY_SYMBOL,
  activeUser: null,
  isKioskModeActive: false,
  isAlwaysOnEnabled: false,
  themeMode: 'system',

  setDatabaseReady: (ready) => set({ isDatabaseReady: ready }),
  completeOnboarding: () => set({ isOnboardingCompleted: true }),
  completeSetup: (storeName, currencySymbol) =>
    set({
      isSetupCompleted: true,
      storeName: storeName.trim() || 'StorePoint',
      currencySymbol: currencySymbol || DEFAULT_CURRENCY_SYMBOL,
    }),
  setActiveUser: (user) => set({ activeUser: user }),
  setKioskModeActive: (active) => set({ isKioskModeActive: active }),
  setAlwaysOnEnabled: (enabled) => set({ isAlwaysOnEnabled: enabled }),
  setThemeMode: (mode) => {
    set({ themeMode: mode });
    Uniwind.setTheme(mode);
    void sessionStateCache.setThemeMode(mode);
  },
  logout: () => set({ activeUser: null }),
}));
