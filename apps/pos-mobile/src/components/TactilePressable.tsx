import React, { useCallback } from 'react';
import type { AccessibilityRole, GestureResponderEvent } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSpring,
} from 'react-native-reanimated';
import {
  Button,
  PressableFeedback,
  type ButtonSize,
  type ButtonVariant,
} from 'heroui-native';
import { MOTION_SPRINGS } from '@/theme/motion';

export type TactileFeedbackVariant = 'spring' | 'stretch' | 'subtle';

export interface TactilePressableProps {
  readonly children: React.ReactNode;
  readonly feedbackVariant?: TactileFeedbackVariant;
  readonly onPress?: () => void;
  readonly onLongPress?: () => void;
  readonly isDisabled?: boolean;
  readonly fullWidth?: boolean;
  readonly className?: string;
  readonly pressableClassName?: string;
  readonly accessibilityLabel?: string;
  readonly accessibilityRole?: AccessibilityRole;
}

export interface TactileButtonProps {
  readonly children: React.ReactNode;
  readonly feedbackVariant?: TactileFeedbackVariant;
  readonly variant?: ButtonVariant;
  readonly size?: ButtonSize;
  readonly isDisabled?: boolean;
  readonly isIconOnly?: boolean;
  readonly fullWidth?: boolean;
  readonly onPress?: (event: GestureResponderEvent) => void;
  readonly onLongPress?: (event: GestureResponderEvent) => void;
  readonly onPressIn?: (event: GestureResponderEvent) => void;
  readonly onPressOut?: (event: GestureResponderEvent) => void;
  readonly className?: string;
  readonly containerClassName?: string;
  readonly accessibilityLabel?: string;
}

function joinClasses(...classes: Array<string | undefined | false>): string | undefined {
  const merged = classes.filter(Boolean).join(' ').trim();
  return merged.length > 0 ? merged : undefined;
}

function useTactileMotion(feedbackVariant: TactileFeedbackVariant, isDisabled: boolean) {
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

  return { animatedStyle, handlePressIn, handlePressOut };
}

/**
 * Tactile pressable wrapper combining HeroUI PressableFeedback highlight
 * with Reanimated spring / squash-and-stretch micro-interactions.
 * Use for custom cards, rows, and chips (never nest a HeroUI Button with pointerEvents="none";
 * use TactileButton for buttons instead).
 */
export function TactilePressable({
  children,
  feedbackVariant = 'spring',
  onPress,
  onLongPress,
  isDisabled = false,
  fullWidth = false,
  className,
  pressableClassName,
  accessibilityLabel,
  accessibilityRole = 'button',
}: TactilePressableProps) {
  const { animatedStyle, handlePressIn, handlePressOut } = useTactileMotion(
    feedbackVariant,
    isDisabled,
  );

  return (
    <Animated.View
      style={animatedStyle}
      className={joinClasses(fullWidth && 'w-full', className)}
    >
      <PressableFeedback
        isDisabled={isDisabled}
        onPress={onPress}
        onLongPress={onLongPress}
        onPressIn={handlePressIn}
        onPressOut={handlePressOut}
        accessibilityLabel={accessibilityLabel}
        accessibilityRole={accessibilityRole}
        className={joinClasses(fullWidth && 'w-full', pressableClassName)}
      >
        <PressableFeedback.Highlight />
        {children}
      </PressableFeedback>
    </Animated.View>
  );
}

/**
 * Tactile wrapper around HeroUI Button that attaches Reanimated spring/stretch
 * micro-interactions directly to Button's native pressable lifecycle without
 * nested PressableFeedback or pointerEvents="none" touch-blocking on Android Fabric.
 */
export function TactileButton({
  children,
  feedbackVariant = 'spring',
  variant,
  size,
  isDisabled = false,
  isIconOnly,
  fullWidth = false,
  onPress,
  onLongPress,
  onPressIn,
  onPressOut,
  className,
  containerClassName,
  accessibilityLabel,
}: TactileButtonProps) {
  const { animatedStyle, handlePressIn, handlePressOut } = useTactileMotion(
    feedbackVariant,
    isDisabled,
  );

  const composedPressIn = useCallback(
    (event: GestureResponderEvent) => {
      handlePressIn();
      onPressIn?.(event);
    },
    [handlePressIn, onPressIn],
  );

  const composedPressOut = useCallback(
    (event: GestureResponderEvent) => {
      handlePressOut();
      onPressOut?.(event);
    },
    [handlePressOut, onPressOut],
  );

  return (
    <Animated.View
      style={animatedStyle}
      className={joinClasses(fullWidth && 'w-full', containerClassName)}
    >
      <Button
        variant={variant}
        size={size}
        isDisabled={isDisabled}
        isIconOnly={isIconOnly}
        onPress={onPress}
        onLongPress={onLongPress}
        onPressIn={composedPressIn}
        onPressOut={composedPressOut}
        accessibilityLabel={accessibilityLabel}
        animation={{ scale: false }}
        className={joinClasses(fullWidth && 'w-full', className)}
      >
        {children}
      </Button>
    </Animated.View>
  );
}
