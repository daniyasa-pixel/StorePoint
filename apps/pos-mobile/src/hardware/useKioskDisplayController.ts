import { useEffect } from 'react';
import { BackHandler, Platform } from 'react-native';
import { activateKeepAwakeAsync, deactivateKeepAwake } from 'expo-keep-awake';
import * as NavigationBar from 'expo-navigation-bar';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

const KEEP_AWAKE_TAG = 'storepoint-pos-terminal';

/**
 * Coordinates Android-only kiosk display flags:
 * 1. Keep screen awake when enabled or while kiosk mode is active.
 * 2. Hide Android system navigation bar in kiosk mode.
 * 3. Suppress hardware Back button in kiosk mode.
 */
export function useKioskDisplayController(): void {
  const isKioskActive = useAppBootstrapStore((s) => s.isKioskModeActive);
  const isAlwaysOn = useAppBootstrapStore((s) => s.isAlwaysOnEnabled);

  useEffect(() => {
    if (isAlwaysOn || isKioskActive) {
      void activateKeepAwakeAsync(KEEP_AWAKE_TAG);
    } else {
      void deactivateKeepAwake(KEEP_AWAKE_TAG);
    }
    return () => {
      void deactivateKeepAwake(KEEP_AWAKE_TAG);
    };
  }, [isAlwaysOn, isKioskActive]);

  useEffect(() => {
    if (Platform.OS !== 'android') return;
    NavigationBar.setVisibilityAsync(isKioskActive ? 'hidden' : 'visible').catch(() => {
      // Ignored on Android edge-to-edge devices where system bar visibility is managed by OS
    });
  }, [isKioskActive]);

  useEffect(() => {
    if (!isKioskActive) return;
    const sub = BackHandler.addEventListener('hardwareBackPress', () => true);
    return () => sub.remove();
  }, [isKioskActive]);
}
