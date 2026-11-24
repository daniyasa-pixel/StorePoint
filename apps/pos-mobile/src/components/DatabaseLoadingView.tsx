import React, { useEffect } from 'react';
import { Text } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Animated, {
  Easing,
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withSequence,
  withTiming,
} from 'react-native-reanimated';
import { Spinner, Surface } from 'heroui-native';
import { enterFade, exitFade } from '@/theme/motion';
import { StorePointBrandBadge } from './StorePointBrandBadge';

interface DatabaseLoadingViewProps {
  readonly label?: string;
}

export function DatabaseLoadingView({
  label = 'Optimizing StorePoint Database...',
}: DatabaseLoadingViewProps) {
  const pulse = useSharedValue(1);

  useEffect(() => {
    pulse.value = withRepeat(
      withSequence(
        withTiming(1.06, { duration: 900, easing: Easing.inOut(Easing.ease) }),
        withTiming(1, { duration: 900, easing: Easing.inOut(Easing.ease) }),
      ),
      -1,
      true,
    );
  }, [pulse]);

  const badgeStyle = useAnimatedStyle(() => ({
    transform: [{ scale: pulse.value }],
  }));

  return (
    <SafeAreaView className="flex-1 bg-background">
      <Animated.View
        entering={enterFade}
        exiting={exitFade}
        className="flex-1"
      >
        <Surface variant="default" className="bg-background flex-1 items-center justify-center p-6">
          <Animated.View className="items-center gap-4">
            <Animated.View style={badgeStyle}>
              <StorePointBrandBadge size="lg" />
            </Animated.View>
            <Spinner size="lg" />
            <Text className="text-foreground text-base font-semibold text-center">
              {label}
            </Text>
          </Animated.View>
        </Surface>
      </Animated.View>
    </SafeAreaView>
  );
}
