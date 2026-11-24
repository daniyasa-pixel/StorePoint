import '../src/global.css';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { Alert, Button, HeroUINativeProvider } from 'heroui-native';
import { useUniwind } from 'uniwind';
import { DatabaseLoadingView } from '@/components/DatabaseLoadingView';
import { getRepositories } from '@/db/repositories';
import { useKioskDisplayController } from '@/hardware/useKioskDisplayController';
import { sessionStateCache } from '@/storage/sessionStateCache';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

export default function RootLayout() {
  const isDatabaseReady = useAppBootstrapStore((state) => state.isDatabaseReady);
  const [bootstrapError, setBootstrapError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);
  const { theme } = useUniwind();
  useKioskDisplayController();

  useEffect(() => {
    let active = true;

    (async () => {
      setBootstrapError(null);
      try {
        const [repos, onboardingDone, savedThemeMode] = await Promise.all([
          getRepositories(),
          sessionStateCache.isOnboardingCompleted(),
          sessionStateCache.getThemeMode(),
        ]);
        const config = await repos.storeConfig.getConfig();
        if (!active) return;

        const store = useAppBootstrapStore.getState();
        store.setThemeMode(savedThemeMode);
        if (onboardingDone) {
          store.completeOnboarding();
        }
        if (config?.setupCompleted) {
          store.completeSetup(config.storeName, config.currencySymbol);
        }
        store.setDatabaseReady(true);
      } catch (error) {
        console.error('StorePoint database bootstrap failed:', error);
        if (active) {
          setBootstrapError(
            error instanceof Error ? error.message : 'Failed to initialize local database.',
          );
        }
      }
    })();

    return () => {
      active = false;
    };
  }, [retryCount]);

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
          <StatusBar style={theme === 'dark' ? 'light' : 'dark'} />
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
                {bootstrapError ? (
                  <View className="flex-1 bg-background items-center justify-center p-6 gap-4">
                    <Alert status="danger" className="max-w-md w-full">
                      <Alert.Indicator />
                      <Alert.Content>
                        <Alert.Title>Database Initialization Failed</Alert.Title>
                        <Alert.Description>{bootstrapError}</Alert.Description>
                      </Alert.Content>
                    </Alert>
                    <Button variant="primary" onPress={() => setRetryCount((c) => c + 1)}>
                      <Button.Label>Retry</Button.Label>
                    </Button>
                  </View>
                ) : (
                  <DatabaseLoadingView />
                )}
              </View>
            ) : null}
          </View>
        </HeroUINativeProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
