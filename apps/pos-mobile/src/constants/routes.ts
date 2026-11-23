/**
 * Canonical route paths mirroring Kotlin MainActivity.kt NavHost.
 */
export const APP_ROUTES = {
  ONBOARDING: '/onboarding',
  SETUP: '/setup',
  LOGIN: '/login',
  PIN_RESET: '/pin-reset',
  ABOUT: '/about',
  CASHIER_POS: '/cashier-pos',
  ADMIN_DASHBOARD: '/admin-dashboard',
  INVENTORY_PORTAL: '/inventory-portal',
} as const;

export type AppRoute = (typeof APP_ROUTES)[keyof typeof APP_ROUTES];

export type UserRole = 'ADMIN' | 'CASHIER' | 'INVENTORY';

export function resolveHomeRouteForRole(role: UserRole): AppRoute {
  return role === 'INVENTORY'
    ? APP_ROUTES.INVENTORY_PORTAL
    : APP_ROUTES.CASHIER_POS;
}
