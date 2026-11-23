import React from 'react';
import { Text, View } from 'react-native';
import { Button, Surface } from 'heroui-native';
import { PIN_LENGTH } from '@/security/securityHelper';

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
    <View className="w-full gap-4">
      <View className="flex-row items-center justify-center gap-3 py-1">
        {Array.from({ length: maxLength }).map((_, idx) => {
          const filled = idx < pin.length;
          return (
            <Surface
              key={idx}
              variant={filled ? 'default' : 'secondary'}
              className={`h-12 w-12 rounded-xl items-center justify-center border ${
                errorMessage
                  ? 'border-danger'
                  : filled
                    ? 'border-accent bg-accent/10'
                    : 'border-border'
              }`}
            >
              <Text className="text-foreground text-xl font-bold">{filled ? '●' : ''}</Text>
            </Surface>
          );
        })}
      </View>

      {errorMessage ? (
        <Text className="text-danger text-xs text-center font-medium">{errorMessage}</Text>
      ) : helperText ? (
        <Text className="text-muted text-xs text-center">{helperText}</Text>
      ) : null}

      <View className="gap-2.5">
        {KEYPAD_ROWS.map((row, rowIdx) => (
          <View key={rowIdx} className="flex-row gap-2.5">
            {row.map((key) => {
              const isAction = key === 'CLR' || key === '⌫';
              return (
                <Button
                  key={key}
                  variant={isAction ? 'tertiary' : 'secondary'}
                  size="lg"
                  isDisabled={isDisabled}
                  className="flex-1 h-14"
                  onPress={() => handleKeyPress(key)}
                >
                  <Button.Label className="text-lg font-semibold">{key}</Button.Label>
                </Button>
              );
            })}
          </View>
        ))}
      </View>
    </View>
  );
}
