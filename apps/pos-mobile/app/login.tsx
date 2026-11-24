import React, { useEffect, useState } from 'react';
import { Linking, ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Button, Card, Chip, Spinner } from 'heroui-native';
import { PinPadEntry } from '@/components/PinPadEntry';
import { StorePointBrandBadge } from '@/components/StorePointBrandBadge';
import { TactileButton, TactilePressable } from '@/components/TactilePressable';
import { ThemeModeToggle } from '@/components/ThemeModeToggle';
import { APP_ROUTES, resolveHomeRouteForRole } from '@/constants/routes';
import { KioskEmergencyUnlockDialog } from '@/features/auth/KioskEmergencyUnlockDialog';
import { useAuthFlow } from '@/features/auth/useAuthFlow';
import { useBackNavigationGuard } from '@/layout/useBackNavigationGuard';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { sessionStateCache } from '@/storage/sessionStateCache';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeDown, enterFadeUp, layoutTransition } from '@/theme/motion';

export default function LoginRoute() {
  const router = useRouter();
  const { useSplitPane, containerPaddingClass, dialogMaxWidthClass, isShortHeight } =
    useWindowLayout();
  const storeName = useAppBootstrapStore((s) => s.storeName);
  const { users, biometricAvailability, loginWithPin, loginWithBiometric } = useAuthFlow();

  const [selectedUsername, setSelectedUsername] = useState('');
  const [pin, setPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [adminPhone, setAdminPhone] = useState('');

  const [logoTapCount, setLogoTapCount] = useState(0);
  const [showEmergencyUnlock, setShowEmergencyUnlock] = useState(false);

  useBackNavigationGuard({ requireDoubleBackToExit: true });

  useEffect(() => {
    if (!selectedUsername && users.length > 0 && users[0]) {
      setSelectedUsername(users[0].username);
    }
  }, [selectedUsername, users]);

  useEffect(() => {
    void sessionStateCache.getAdminContact().then((contact) => {
      setAdminPhone(contact.phone.trim());
    });
  }, []);

  const handleLogoTap = () => {
    const next = logoTapCount + 1;
    if (next >= 5) {
      setLogoTapCount(0);
      setShowEmergencyUnlock(true);
    } else {
      setLogoTapCount(next);
    }
  };

  const handlePinSignIn = async (candidatePin: string) => {
    setIsSubmitting(true);
    setError(null);
    try {
      const outcome = await loginWithPin(selectedUsername, candidatePin);
      if (outcome.requiresPinReset) {
        router.push(APP_ROUTES.PIN_RESET);
        return;
      }
      if (!outcome.ok || !outcome.user) {
        setError(outcome.error ?? 'Sign-in failed.');
        setPin('');
        return;
      }
      router.replace(resolveHomeRouteForRole(outcome.user.role));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handlePinChange = (next: string) => {
    setPin(next);
    setError(null);
    if (next.length === 4) {
      void handlePinSignIn(next);
    }
  };

  const handleBiometric = async () => {
    setError(null);
    const outcome = await loginWithBiometric();
    if (outcome.requiresPinReset) {
      router.push(APP_ROUTES.PIN_RESET);
      return;
    }
    if (!outcome.ok || !outcome.user) {
      setError(outcome.error ?? 'Biometric sign-in failed.');
      return;
    }
    router.replace(resolveHomeRouteForRole(outcome.user.role));
  };

  const handleCallAdmin = () => {
    if (!adminPhone) return;
    void Linking.openURL(`tel:${adminPhone}`);
  };

  const staffSelector =
    users.length > 0 ? (
      <View className="flex-row flex-wrap gap-2 justify-center">
        {users.map((u) => {
          const isSelected = selectedUsername === u.username;
          return (
            <Chip
              key={u.username}
              color={isSelected ? 'accent' : 'default'}
              variant={isSelected ? 'primary' : 'soft'}
              accessibilityRole="button"
              accessibilityState={{ selected: isSelected }}
              onPress={() => {
                setSelectedUsername(u.username);
                setPin('');
                setError(null);
              }}
            >
              <Chip.Label>
                {u.username} ({u.role})
              </Chip.Label>
            </Chip>
          );
        })}
      </View>
    ) : null;

  return (
    <SafeAreaView className="bg-background flex-1">
      <View className="w-full flex-row items-center justify-end px-4 pt-2">
        <ThemeModeToggle />
      </View>

      <ScrollView contentContainerClassName={`flex-grow justify-center ${containerPaddingClass}`}>
        <View
          className={
            useSplitPane
              ? 'w-full max-w-3xl self-center flex-row items-center gap-6'
              : `w-full ${dialogMaxWidthClass} self-center ${isShortHeight ? 'gap-3' : 'gap-5'}`
          }
        >
          <Animated.View
            entering={enterFadeDown}
            className={useSplitPane ? 'flex-1 items-center gap-4' : 'items-center gap-2'}
          >
            <TactilePressable
              feedbackVariant="spring"
              onPress={handleLogoTap}
              accessibilityRole="button"
              accessibilityLabel="StorePoint logo"
            >
              <StorePointBrandBadge size={isShortHeight ? 'md' : 'lg'} />
            </TactilePressable>
            <Text className="text-foreground text-2xl font-bold text-center">{storeName}</Text>
            <Text className="text-muted text-xs text-center">
              Select staff account and enter 4-digit PIN
            </Text>
            {useSplitPane ? staffSelector : null}
          </Animated.View>

          <Animated.View entering={enterFadeUp} className={useSplitPane ? 'flex-1' : 'w-full'}>
            <Card>
              <Card.Body>
                <Animated.View layout={layoutTransition} className="gap-4">
                  {!useSplitPane ? staffSelector : null}

                  <PinPadEntry
                    pin={pin}
                    onPinChange={handlePinChange}
                    isDisabled={isSubmitting}
                    errorMessage={error}
                  />

                  {isSubmitting ? (
                    <View className="flex-row items-center justify-center gap-2">
                      <Spinner size="sm" />
                      <Text className="text-muted text-xs">Verifying PIN...</Text>
                    </View>
                  ) : null}

                  {biometricAvailability === 'AVAILABLE' ? (
                    <TactileButton
                      feedbackVariant="stretch"
                      variant="secondary"
                      size="md"
                      fullWidth
                      isDisabled={isSubmitting}
                      onPress={() => void handleBiometric()}
                    >
                      <Button.Label>Sign In with Fingerprint / Biometrics</Button.Label>
                    </TactileButton>
                  ) : null}
                </Animated.View>
              </Card.Body>
              <Card.Footer className="flex-row flex-wrap items-center justify-between gap-2 pt-3">
                <TactileButton
                  variant="ghost"
                  size="sm"
                  onPress={() => router.push(APP_ROUTES.PIN_RESET)}
                >
                  <Button.Label>Reset PIN</Button.Label>
                </TactileButton>
                {adminPhone ? (
                  <TactileButton variant="ghost" size="sm" onPress={handleCallAdmin}>
                    <Button.Label>Call Admin</Button.Label>
                  </TactileButton>
                ) : null}
                <TactileButton
                  variant="ghost"
                  size="sm"
                  onPress={() => router.push(APP_ROUTES.ABOUT)}
                >
                  <Button.Label>About</Button.Label>
                </TactileButton>
              </Card.Footer>
            </Card>
          </Animated.View>
        </View>
      </ScrollView>

      <KioskEmergencyUnlockDialog
        isOpen={showEmergencyUnlock}
        onOpenChange={setShowEmergencyUnlock}
      />
    </SafeAreaView>
  );
}
