import React, { useEffect, useState } from 'react';
import { ScrollView, Text, useWindowDimensions, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Button, Card, Chip, PressableFeedback, Spinner } from 'heroui-native';
import { PinPadEntry } from '@/components/PinPadEntry';
import { StorePointBrandBadge } from '@/components/StorePointBrandBadge';
import { APP_ROUTES, resolveHomeRouteForRole } from '@/constants/routes';
import { KioskEmergencyUnlockDialog } from '@/features/auth/KioskEmergencyUnlockDialog';
import { useAuthFlow } from '@/features/auth/useAuthFlow';
import { getWindowLayout } from '@/layout/adaptiveLayout';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeDown, enterFadeUp, layoutTransition } from '@/theme/motion';

export default function LoginRoute() {
  const router = useRouter();
  const { width } = useWindowDimensions();
  const isExpanded = getWindowLayout(width) === 'Expanded';
  const storeName = useAppBootstrapStore((s) => s.storeName);
  const { users, biometricAvailability, loginWithPin, loginWithBiometric } = useAuthFlow();

  const [selectedUsername, setSelectedUsername] = useState('');
  const [pin, setPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [logoTapCount, setLogoTapCount] = useState(0);
  const [showEmergencyUnlock, setShowEmergencyUnlock] = useState(false);

  useEffect(() => {
    if (!selectedUsername && users.length > 0 && users[0]) {
      setSelectedUsername(users[0].username);
    }
  }, [selectedUsername, users]);

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

  const staffSelector =
    users.length > 0 ? (
      <View className="flex-row flex-wrap gap-2 justify-center">
        {users.map((u) => (
          <Chip
            key={u.username}
            color={selectedUsername === u.username ? 'accent' : 'default'}
            variant={selectedUsername === u.username ? 'primary' : 'soft'}
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
        ))}
      </View>
    ) : null;

  return (
    <SafeAreaView className="bg-background flex-1">
      <ScrollView contentContainerClassName="flex-grow justify-center p-5">
        <View
          className={
            isExpanded
              ? 'w-full max-w-3xl self-center flex-row items-center gap-6'
              : 'w-full max-w-md self-center gap-5'
          }
        >
          <Animated.View
            entering={enterFadeDown}
            className={isExpanded ? 'flex-1 items-center gap-4' : 'items-center gap-2'}
          >
            <PressableFeedback
              onPress={handleLogoTap}
              accessibilityRole="button"
              accessibilityLabel="StorePoint logo"
            >
              <StorePointBrandBadge size="lg" />
            </PressableFeedback>
            <Text className="text-foreground text-2xl font-bold text-center">{storeName}</Text>
            <Text className="text-muted text-xs text-center">
              Select staff account and enter 4-digit PIN
            </Text>
            {isExpanded ? staffSelector : null}
          </Animated.View>

          <Animated.View entering={enterFadeUp} className={isExpanded ? 'flex-1' : 'w-full'}>
            <Card>
              <Card.Body>
                <Animated.View layout={layoutTransition} className="gap-4">
                  {!isExpanded ? staffSelector : null}

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
                    <Button
                      variant="secondary"
                      size="md"
                      className="w-full"
                      onPress={handleBiometric}
                      isDisabled={isSubmitting}
                    >
                      <Button.Label>Sign In with Fingerprint / Biometrics</Button.Label>
                    </Button>
                  ) : null}
                </Animated.View>
              </Card.Body>
              <Card.Footer className="flex-row justify-between pt-3">
                <Button variant="ghost" size="sm" onPress={() => router.push(APP_ROUTES.PIN_RESET)}>
                  <Button.Label>Reset PIN</Button.Label>
                </Button>
                <Button variant="ghost" size="sm" onPress={() => router.push(APP_ROUTES.ABOUT)}>
                  <Button.Label>About</Button.Label>
                </Button>
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
