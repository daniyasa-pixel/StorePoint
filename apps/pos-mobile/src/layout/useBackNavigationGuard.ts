import { useEffect, useRef } from 'react';
import { BackHandler, Platform } from 'react-native';
import { useToast } from 'heroui-native';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

export const DOUBLE_BACK_EXIT_WINDOW_MS = 2_000;

export interface BackNavigationGuardOptions {
  /**
   * Custom step/drawer/tab back handler. Return `true` when the press was consumed.
   */
  readonly onBack?: () => boolean;
  /**
   * Blocks app exit unconditionally when `true` (or automatically when Kiosk Mode is active).
   */
  readonly blockExit?: boolean;
  /**
   * Requires two back swipes/presses within 2000ms to exit StorePoint when not in Kiosk Mode.
   */
  readonly requireDoubleBackToExit?: boolean;
  /**
   * Optional flag to disable the guard listener dynamically.
   * @default true
   */
  readonly enabled?: boolean;
}

export type BackGuardDecision =
  | { readonly handled: true; readonly action: 'custom' | 'kiosk-blocked' | 'blocked' | 'prompt-double-back' | 'exit-app' }
  | { readonly handled: false; readonly action: 'passthrough' };

export interface EvaluateBackPressInput {
  readonly onBack?: () => boolean;
  readonly isKioskModeActive: boolean;
  readonly blockExit: boolean;
  readonly requireDoubleBackToExit: boolean;
  readonly lastBackPressAt: number;
  readonly now: number;
}

export function evaluateBackPress({
  onBack,
  isKioskModeActive,
  blockExit,
  requireDoubleBackToExit,
  lastBackPressAt,
  now,
}: EvaluateBackPressInput): {
  readonly decision: BackGuardDecision;
  readonly nextLastBackPressAt: number;
} {
  if (onBack?.()) {
    return {
      decision: { handled: true, action: 'custom' },
      nextLastBackPressAt: lastBackPressAt,
    };
  }

  if (isKioskModeActive) {
    return {
      decision: { handled: true, action: 'kiosk-blocked' },
      nextLastBackPressAt: 0,
    };
  }

  if (blockExit) {
    return {
      decision: { handled: true, action: 'blocked' },
      nextLastBackPressAt: 0,
    };
  }

  if (requireDoubleBackToExit) {
    if (lastBackPressAt > 0 && now - lastBackPressAt < DOUBLE_BACK_EXIT_WINDOW_MS) {
      return {
        decision: { handled: true, action: 'exit-app' },
        nextLastBackPressAt: 0,
      };
    }
    return {
      decision: { handled: true, action: 'prompt-double-back' },
      nextLastBackPressAt: now,
    };
  }

  return {
    decision: { handled: false, action: 'passthrough' },
    nextLastBackPressAt: lastBackPressAt,
  };
}

/**
 * Android hardware Back & edge-swipe gesture guard.
 * Coordinates step-back / tab-back callbacks, Kiosk Lockdown suppression,
 * and double-swipe-to-exit confirmation to prevent accidental POS exits.
 */
export function useBackNavigationGuard({
  onBack,
  blockExit = false,
  requireDoubleBackToExit = false,
  enabled = true,
}: BackNavigationGuardOptions = {}): void {
  const isKioskModeActive = useAppBootstrapStore((s) => s.isKioskModeActive);
  const { toast } = useToast();
  const lastBackPressRef = useRef(0);
  const latestRef = useRef({
    onBack,
    blockExit,
    requireDoubleBackToExit,
    isKioskModeActive,
    toast,
  });

  latestRef.current = {
    onBack,
    blockExit,
    requireDoubleBackToExit,
    isKioskModeActive,
    toast,
  };

  useEffect(() => {
    if (!enabled || Platform.OS !== 'android') return;

    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      const current = latestRef.current;
      const { decision, nextLastBackPressAt } = evaluateBackPress({
        onBack: current.onBack,
        isKioskModeActive: current.isKioskModeActive,
        blockExit: current.blockExit,
        requireDoubleBackToExit: current.requireDoubleBackToExit,
        lastBackPressAt: lastBackPressRef.current,
        now: Date.now(),
      });

      lastBackPressRef.current = nextLastBackPressAt;

      if (decision.action === 'kiosk-blocked') {
        current.toast.show({
          label: 'Kiosk Lockdown active',
          variant: 'warning',
        });
        return true;
      }

      if (decision.action === 'prompt-double-back') {
        current.toast.show({
          label: 'Swipe back again to exit StorePoint',
          variant: 'default',
        });
        return true;
      }

      if (decision.action === 'exit-app') {
        BackHandler.exitApp();
        return true;
      }

      return decision.handled;
    });

    return () => {
      subscription.remove();
    };
  }, [enabled]);
}
