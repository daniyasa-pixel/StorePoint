import React, { useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Button, Card, Chip } from 'heroui-native';
import { StorePointBrandBadge } from '@/components/StorePointBrandBadge';
import { TactilePressable } from '@/components/TactilePressable';
import { APP_ROUTES } from '@/constants/routes';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { sessionStateCache } from '@/storage/sessionStateCache';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterSlideRight, exitSlideLeft, layoutTransition } from '@/theme/motion';

const SLIDES = [
  {
    badge: 'Offline-First POS',
    title: 'Zero-Cloud Cash Register',
    description:
      'Every sale, multi-UOM cart, shift reconciliation, and inventory update writes atomically to local SQLite.',
  },
  {
    badge: 'Inventory & BOM',
    title: 'Multi-UOM & Repackaging Engine',
    description:
      'Sell by piece, pack, box, or ream with automatic stock deduction, supplier POs, and bulk BOM repackaging.',
  },
  {
    badge: 'Terminal Security',
    title: 'Kiosk Lockdown & Audit Trail',
    description:
      '4-digit PBKDF2 PINs, biometric sign-in, escalating lockout protection, and encrypted disaster-recovery backups.',
  },
] as const;

export default function OnboardingRoute() {
  const router = useRouter();
  const { containerPaddingClass, dialogMaxWidthClass, isShortHeight } = useWindowLayout();
  const [slideIndex, setSlideIndex] = useState(0);
  const current = SLIDES[slideIndex] ?? SLIDES[0];

  const handleFinish = async () => {
    await sessionStateCache.setOnboardingCompleted(true);
    useAppBootstrapStore.getState().completeOnboarding();
    router.replace(APP_ROUTES.SETUP);
  };

  return (
    <SafeAreaView className="bg-background flex-1">
      <ScrollView contentContainerClassName={`flex-grow justify-center ${containerPaddingClass}`}>
        <View className={`w-full ${dialogMaxWidthClass} self-center ${isShortHeight ? 'gap-4' : 'gap-6'}`}>
          <View className="items-center gap-3">
            <StorePointBrandBadge size={isShortHeight ? 'md' : 'lg'} />
            <Animated.View
              key={slideIndex}
              entering={enterSlideRight}
              exiting={exitSlideLeft}
              className="items-center gap-3"
            >
              <Chip color="accent" variant="soft" size="sm">
                <Chip.Label>{current.badge}</Chip.Label>
              </Chip>
              <Text className="text-foreground text-2xl font-bold text-center">
                {current.title}
              </Text>
              <Text className="text-muted text-sm text-center leading-5">
                {current.description}
              </Text>
            </Animated.View>
          </View>

          <View className="flex-row justify-center gap-2">
            {SLIDES.map((_, idx) => (
              <TactilePressable
                key={idx}
                feedbackVariant="spring"
                onPress={() => setSlideIndex(idx)}
                accessibilityRole="button"
                accessibilityLabel={`Go to slide ${idx + 1}`}
                className="p-1"
              >
                <Animated.View
                  layout={layoutTransition}
                  className={`h-2 rounded-full ${
                    idx === slideIndex ? 'w-8 bg-accent' : 'w-2 bg-border'
                  }`}
                />
              </TactilePressable>
            ))}
          </View>

          <Card>
            <Card.Body className="gap-3">
              {slideIndex < SLIDES.length - 1 ? (
                <TactilePressable
                  feedbackVariant="stretch"
                  className="w-full"
                  onPress={() => setSlideIndex((i) => i + 1)}
                >
                  <View pointerEvents="none">
                    <Button variant="primary" size="lg" className="w-full">
                      <Button.Label>Next</Button.Label>
                    </Button>
                  </View>
                </TactilePressable>
              ) : (
                <TactilePressable
                  feedbackVariant="stretch"
                  className="w-full"
                  onPress={handleFinish}
                >
                  <View pointerEvents="none">
                    <Button variant="primary" size="lg" className="w-full">
                      <Button.Label>Start Store Setup</Button.Label>
                    </Button>
                  </View>
                </TactilePressable>
              )}
              <Button variant="ghost" size="sm" className="w-full" onPress={handleFinish}>
                <Button.Label>Skip Intro</Button.Label>
              </Button>
            </Card.Body>
          </Card>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
