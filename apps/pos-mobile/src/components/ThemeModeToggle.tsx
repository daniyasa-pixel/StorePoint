import React, { useCallback } from 'react';
import { View } from 'react-native';
import { Button, Chip, type ChipSize } from 'heroui-native';
import { useUniwind } from 'uniwind';
import type { ThemeMode } from '@/storage/sessionStateCache';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { TactileButton } from './TactilePressable';

export interface ThemeModeToggleProps {
  readonly compact?: boolean;
  readonly className?: string;
}

export interface ThemeModeSelectorProps {
  readonly value?: ThemeMode;
  readonly onChange?: (mode: ThemeMode) => void;
  readonly size?: ChipSize;
  readonly className?: string;
}

const THEME_OPTIONS: ReadonlyArray<{
  readonly mode: ThemeMode;
  readonly label: string;
  readonly accessibilityLabel: string;
}> = [
  { mode: 'system', label: 'System', accessibilityLabel: 'Use system theme' },
  { mode: 'light', label: '☀ Light', accessibilityLabel: 'Use light theme' },
  { mode: 'dark', label: '☾ Dark', accessibilityLabel: 'Use dark theme' },
];

/**
 * 1-tap Light / Dark toggle button for top bars and headers.
 */
export function ThemeModeToggle({ compact = false, className }: ThemeModeToggleProps) {
  const { theme } = useUniwind();
  const setThemeMode = useAppBootstrapStore((s) => s.setThemeMode);
  const isDark = theme === 'dark';

  const handleToggle = useCallback(() => {
    setThemeMode(isDark ? 'light' : 'dark');
  }, [isDark, setThemeMode]);

  const nextLabel = isDark ? '☀ Light' : '☾ Dark';
  const compactIcon = isDark ? '☀' : '☾';
  const a11yLabel = isDark ? 'Switch to light mode' : 'Switch to dark mode';

  return (
    <TactileButton
      variant="ghost"
      size="sm"
      isIconOnly={compact}
      feedbackVariant="spring"
      accessibilityLabel={a11yLabel}
      className={className}
      onPress={handleToggle}
    >
      <Button.Label>{compact ? compactIcon : nextLabel}</Button.Label>
    </TactileButton>
  );
}

/**
 * 3-option HeroUI Chip selector (System / Light / Dark) for Setup and Admin Settings.
 */
export function ThemeModeSelector({
  value,
  onChange,
  size = 'md',
  className,
}: ThemeModeSelectorProps) {
  const storeMode = useAppBootstrapStore((s) => s.themeMode);
  const setStoreMode = useAppBootstrapStore((s) => s.setThemeMode);
  const activeMode = value ?? storeMode;

  const handleSelect = useCallback(
    (mode: ThemeMode) => {
      if (onChange) {
        onChange(mode);
      } else {
        setStoreMode(mode);
      }
    },
    [onChange, setStoreMode],
  );

  return (
    <View className={`flex-row flex-wrap items-center gap-2 ${className ?? ''}`.trim()}>
      {THEME_OPTIONS.map((option) => {
        const isSelected = activeMode === option.mode;
        return (
          <Chip
            key={option.mode}
            size={size}
            color={isSelected ? 'accent' : 'default'}
            variant={isSelected ? 'primary' : 'soft'}
            accessibilityRole="button"
            accessibilityState={{ selected: isSelected }}
            accessibilityLabel={option.accessibilityLabel}
            onPress={() => handleSelect(option.mode)}
          >
            <Chip.Label>{option.label}</Chip.Label>
          </Chip>
        );
      })}
    </View>
  );
}
