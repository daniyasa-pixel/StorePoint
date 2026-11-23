import React, { type ReactNode } from 'react';
import { Text, View } from 'react-native';
import { Button, Separator, Surface } from 'heroui-native';
import { StorePointBrandBadge } from './StorePointBrandBadge';

interface AppTopBarProps {
  readonly title: string;
  readonly subtitle?: string;
  readonly showBrandBadge?: boolean;
  readonly leftAction?: ReactNode;
  readonly rightActions?: ReactNode;
  readonly onBack?: () => void;
}

export function AppTopBar({
  title,
  subtitle,
  showBrandBadge = true,
  leftAction,
  rightActions,
  onBack,
}: AppTopBarProps) {
  return (
    <Surface variant="default" className="w-full">
      <View className="flex-row items-center justify-between px-4 py-3 gap-3">
        <View className="flex-row items-center gap-3 flex-1 min-w-0">
          {onBack ? (
            <Button variant="ghost" size="sm" onPress={onBack}>
              <Button.Label>← Back</Button.Label>
            </Button>
          ) : (
            leftAction
          )}
          {showBrandBadge ? <StorePointBrandBadge size="sm" /> : null}
          <View className="flex-1 min-w-0">
            <Text className="text-foreground text-lg font-bold" numberOfLines={1}>
              {title}
            </Text>
            {subtitle ? (
              <Text className="text-muted text-xs" numberOfLines={1}>
                {subtitle}
              </Text>
            ) : null}
          </View>
        </View>
        {rightActions ? (
          <View className="flex-row items-center gap-2">{rightActions}</View>
        ) : null}
      </View>
      <Separator />
    </Surface>
  );
}
