import React, { useEffect } from 'react';
import { Text, View, useWindowDimensions } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSequence,
  withTiming,
} from 'react-native-reanimated';
import { Button, Surface } from 'heroui-native';
import { isCompactLayout } from '@/layout/adaptiveLayout';
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
  const { width } = useWindowDimensions();
  const isCompact = isCompactLayout(width);
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

      <View className="gap-2.5">
        {KEYPAD_ROWS.map((row, rowIdx) => (
          <View key={rowIdx} className="flex-row gap-2.5">
            {row.map((key) => {
              const isAction = key === 'CLR' || key === '⌫';
              const keyLabel =
                key === 'CLR' ? 'Clear PIN' : key === '⌫' ? 'Backspace' : `Digit ${key}`;
              return (
                <Button
                  key={key}
                  variant={isAction ? 'tertiary' : 'secondary'}
                  size="lg"
                  isDisabled={isDisabled}
                  accessibilityLabel={keyLabel}
                  className={`flex-1 ${isCompact ? 'h-13' : 'h-14'}`}
                  onPress={() => handleKeyPress(key)}
                >
                  <Button.Label className="text-lg font-semibold">{key}</Button.Label>
                </Button>
              );
            })}
          </View>
        ))}
      </View>
    </Animated.View>
  );
}
