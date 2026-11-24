import React, { useEffect } from 'react';
import { Text, View } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSequence,
  withTiming,
} from 'react-native-reanimated';
import { Surface } from 'heroui-native';
import { TactilePressable } from '@/components/TactilePressable';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { PIN_LENGTH } from '@/security/securityHelper';
import { enterFade, enterPop, exitFade, layoutTransition } from '@/theme/motion';

interface PinPadEntryProps {
  readonly pin: string;
  readonly onPinChange: (nextPin: string) => void;
  readonly maxLength?: number;
  readonly isDisabled?: boolean;
  readonly errorMessage?: string | null;
  readonly helperText?: string;
}

const KEYPAD_ROWS: readonly (readonly string[])[] = [
  ['1', '2', '3'],
  ['4', '5', '6'],
  ['7', '8', '9'],
  ['CLR', '0', '⌫'],
];

/**
 * On-screen numeric POS PIN keypad built exclusively with HeroUI Native.
 * Deliberately avoids the Android soft IME so checkout/auth context stays
 * visible and protected from IME autofill or shoulder-surfing.
 */
export function PinPadEntry({
  pin,
  onPinChange,
  maxLength = PIN_LENGTH,
  isDisabled = false,
  errorMessage,
  helperText,
}: PinPadEntryProps) {
  const { isCompact, isShortHeight, sizeClass } = useWindowLayout();
  const isDense = isShortHeight || sizeClass === 'SmallPhone';
  const keyHeightClass = isDense ? 'h-11' : isCompact ? 'h-13' : 'h-14';
  const shakeX = useSharedValue(0);

  useEffect(() => {
    if (!errorMessage) return;
    shakeX.value = withSequence(
      withTiming(-8, { duration: 45 }),
      withTiming(8, { duration: 45 }),
      withTiming(-6, { duration: 45 }),
      withTiming(6, { duration: 45 }),
      withTiming(0, { duration: 45 }),
    );
  }, [errorMessage, shakeX]);

  const dotsAnimatedStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: shakeX.value }],
  }));

  const handleKeyPress = (key: string) => {
    if (isDisabled) return;
    if (key === 'CLR') {
      onPinChange('');
      return;
    }
    if (key === '⌫') {
      onPinChange(pin.slice(0, -1));
      return;
    }
    if (pin.length < maxLength) {
      onPinChange(`${pin}${key}`);
    }
  };

  return (
    <Animated.View layout={layoutTransition} className="w-full gap-4">
      <Animated.View
        style={dotsAnimatedStyle}
        accessibilityLabel={`${pin.length} of ${maxLength} digits entered`}
        className="flex-row items-center justify-center gap-3 py-1"
      >
        {Array.from({ length: maxLength }).map((_, idx) => {
          const filled = idx < pin.length;
          return (
            <Surface
              key={idx}
              variant={filled ? 'default' : 'secondary'}
              className={`${
                isCompact ? 'h-11 w-11' : 'h-12 w-12'
              } rounded-xl items-center justify-center border ${
                errorMessage
                  ? 'border-danger bg-danger/10'
                  : filled
                    ? 'border-accent bg-accent/10'
                    : 'border-border'
              }`}
            >
              {filled ? (
                <Animated.Text
                  key={`dot-${idx}`}
                  entering={enterPop}
                  className="text-foreground text-xl font-bold"
                >
                  ●
                </Animated.Text>
              ) : null}
            </Surface>
          );
        })}
      </Animated.View>

      {errorMessage ? (
        <Animated.Text
          key={errorMessage}
          entering={enterFade}
          exiting={exitFade}
          className="text-danger text-xs text-center font-medium"
        >
          {errorMessage}
        </Animated.Text>
      ) : helperText ? (
        <Text className="text-muted text-xs text-center">{helperText}</Text>
      ) : null}

      <View className={isDense ? 'gap-2' : 'gap-2.5'}>
        {KEYPAD_ROWS.map((row, rowIdx) => (
          <View key={rowIdx} className={isDense ? 'flex-row gap-2' : 'flex-row gap-2.5'}>
            {row.map((key) => {
              const isAction = key === 'CLR' || key === '⌫';
              const keyLabel =
                key === 'CLR' ? 'Clear PIN' : key === '⌫' ? 'Backspace' : `Digit ${key}`;
              return (
                <TactilePressable
                  key={key}
                  feedbackVariant="stretch"
                  isDisabled={isDisabled}
                  accessibilityLabel={keyLabel}
                  accessibilityRole="button"
                  className="flex-1"
                  onPress={() => handleKeyPress(key)}
                >
                  <Surface
                    variant={isAction ? 'tertiary' : 'secondary'}
                    className={`w-full ${keyHeightClass} rounded-xl items-center justify-center border border-border/60 ${
                      isDisabled ? 'opacity-50' : ''
                    }`}
                  >
                    <Text className="text-foreground text-lg font-semibold">{key}</Text>
                  </Surface>
                </TactilePressable>
              );
            })}
          </View>
        ))}
      </View>
    </Animated.View>
  );
}
