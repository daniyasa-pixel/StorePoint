import React, { type ReactNode } from 'react';
import { Text, View, useWindowDimensions } from 'react-native';
import Animated from 'react-native-reanimated';
import { Button, Separator, Surface } from 'heroui-native';
import { isCompactLayout } from '@/layout/adaptiveLayout';
import { enterFade, exitFade, layoutTransition } from '@/theme/motion';
import { StorePointBrandBadge } from './StorePointBrandBadge';
import { TactileButton } from './TactilePressable';
import { ThemeModeToggle } from './ThemeModeToggle';

export interface AppTopBarProps {
  readonly title: string;
  readonly subtitle?: string;
  readonly showBrandBadge?: boolean;
  readonly showThemeToggle?: boolean;
  readonly leftAction?: ReactNode;
  readonly rightActions?: ReactNode;
  readonly onBack?: () => void;
}

export function AppTopBar({
  title,
  subtitle,
  showBrandBadge = true,
  showThemeToggle = true,
  leftAction,
  rightActions,
  onBack,
}: AppTopBarProps) {
  const { width } = useWindowDimensions();
  const isCompact = isCompactLayout(width);
  const shouldShowBadge = showBrandBadge && !(isCompact && onBack && rightActions);
  const hasRightArea = showThemeToggle || Boolean(rightActions);

  return (
    <Surface variant="default" className="w-full">
      <View
        className={`flex-row items-center justify-between ${
          isCompact ? 'px-3 py-2.5 gap-2' : 'px-4 py-3 gap-3'
        }`}
      >
        <View className="flex-row items-center gap-2.5 flex-1 min-w-0">
          {onBack ? (
            <TactileButton
              variant="ghost"
              size="sm"
              isIconOnly={isCompact}
              accessibilityLabel="Go back"
              containerClassName="shrink-0"
              onPress={onBack}
            >
              <Button.Label>{isCompact ? '←' : '← Back'}</Button.Label>
            </TactileButton>
          ) : (
            leftAction
          )}
          {shouldShowBadge ? <StorePointBrandBadge size="sm" /> : null}
          <Animated.View layout={layoutTransition} className="flex-1 min-w-0">
            <Text
              className={`text-foreground font-bold ${isCompact ? 'text-base' : 'text-lg'}`}
              numberOfLines={1}
            >
              {title}
            </Text>
            {subtitle ? (
              <Animated.Text
                key={subtitle}
                entering={enterFade}
                exiting={exitFade}
                className="text-muted text-xs"
                numberOfLines={1}
              >
                {subtitle}
              </Animated.Text>
            ) : null}
          </Animated.View>
        </View>
        {hasRightArea ? (
          <View className="flex-row items-center gap-1.5 shrink-0">
            {showThemeToggle ? <ThemeModeToggle compact /> : null}
            {rightActions}
          </View>
        ) : null}
      </View>
      <Separator />
    </Surface>
  );
}
