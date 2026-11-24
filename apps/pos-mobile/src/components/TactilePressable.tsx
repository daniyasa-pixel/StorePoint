import React, { useCallback } from 'react';
import type { AccessibilityRole } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSpring,
} from 'react-native-reanimated';
import { PressableFeedback } from 'heroui-native';
import { MOTION_SPRINGS } from '@/theme/motion';

export type TactileFeedbackVariant = 'spring' | 'stretch' | 'subtle';

export interface TactilePressableProps {
  readonly children: React.ReactNode;
  readonly feedbackVariant?: TactileFeedbackVariant;
  readonly onPress?: () => void;
  readonly onLongPress?: () => void;
  readonly isDisabled?: boolean;
  readonly className?: string;
  readonly accessibilityLabel?: string;
  readonly accessibilityRole?: AccessibilityRole;
}

/**
 * Tactile pressable wrapper combining HeroUI PressableFeedback highlight
 * with Reanimated spring / squash-and-stretch micro-interactions.
 */
export function TactilePressable({
  children,
  feedbackVariant = 'spring',
  onPress,
  onLongPress,
  isDisabled = false,
  className,
  accessibilityLabel,
  accessibilityRole = 'button',
}: TactilePressableProps) {
  const scaleX = useSharedValue(1);
  const scaleY = useSharedValue(1);

  const handlePressIn = useCallback(() => {
    if (isDisabled) return;
    if (feedbackVariant === 'stretch') {
      scaleX.value = withSpring(1.03, MOTION_SPRINGS.tactile);
      scaleY.value = withSpring(0.94, MOTION_SPRINGS.tactile);
      return;
    }
    const targetScale = feedbackVariant === 'subtle' ? 0.985 : 0.96;
    scaleX.value = withSpring(targetScale, MOTION_SPRINGS.tactile);
    scaleY.value = withSpring(targetScale, MOTION_SPRINGS.tactile);
  }, [feedbackVariant, isDisabled, scaleX, scaleY]);

  const handlePressOut = useCallback(() => {
    const releaseSpring =
      feedbackVariant === 'stretch' ? MOTION_SPRINGS.stretch : MOTION_SPRINGS.bouncy;
    scaleX.value = withSpring(1, releaseSpring);
    scaleY.value = withSpring(1, releaseSpring);
  }, [feedbackVariant, scaleX, scaleY]);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scaleX: scaleX.value }, { scaleY: scaleY.value }],
  }));

  return (
    <Animated.View style={animatedStyle} className={className}>
      <PressableFeedback
        isDisabled={isDisabled}
        onPress={onPress}
        onLongPress={onLongPress}
        onPressIn={handlePressIn}
        onPressOut={handlePressOut}
        accessibilityLabel={accessibilityLabel}
        accessibilityRole={accessibilityRole}
        className="w-full"
      >
        <PressableFeedback.Highlight />
        {children}
      </PressableFeedback>
    </Animated.View>
  );
}
