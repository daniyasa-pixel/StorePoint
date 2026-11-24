import '../src/global.css';
import React, { useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { HeroUINativeProvider } from 'heroui-native';
import { DatabaseLoadingView } from '@/components/DatabaseLoadingView';
import { getRepositories } from '@/db/repositories';
import { useKioskDisplayController } from '@/hardware/useKioskDisplayController';
import { sessionStateCache } from '@/storage/sessionStateCache';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

export default function RootLayout() {
  const isDatabaseReady = useAppBootstrapStore((state) => state.isDatabaseReady);
  useKioskDisplayController();

  useEffect(() => {
    let active = true;

    (async () => {
      try {
        const [repos, onboardingDone] = await Promise.all([
          getRepositories(),
          sessionStateCache.isOnboardingCompleted(),
        ]);
        const config = await repos.storeConfig.getConfig();
        if (!active) return;

        const store = useAppBootstrapStore.getState();
        if (onboardingDone) {
          store.completeOnboarding();
        }
        if (config?.setupCompleted) {
          store.completeSetup(config.storeName, config.currencySymbol);
        }
      } catch (error) {
        console.error('StorePoint database bootstrap failed:', error);
      } finally {
        if (active) {
          useAppBootstrapStore.getState().setDatabaseReady(true);
        }
      }
    })();

    return () => {
      active = false;
    };
  }, []);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <HeroUINativeProvider
          config={{
            textProps: {
              maxFontSizeMultiplier: 1.35,
            },
          }}
        >
          <StatusBar style="auto" />
          <View style={{ flex: 1 }}>
            <Stack
              screenOptions={{
                headerShown: false,
                animation: 'fade',
                animationDuration: 200,
              }}
            >
              <Stack.Screen name="index" />
              <Stack.Screen name="onboarding" />
              <Stack.Screen name="setup" />
              <Stack.Screen name="login" />
              <Stack.Screen name="pin-reset" />
              <Stack.Screen name="about" />
              <Stack.Screen name="cashier-pos" />
              <Stack.Screen name="admin-dashboard" />
              <Stack.Screen name="inventory-portal" />
            </Stack>
            {!isDatabaseReady ? (
              <View style={StyleSheet.absoluteFill}>
                <DatabaseLoadingView />
              </View>
            ) : null}
          </View>
        </HeroUINativeProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
