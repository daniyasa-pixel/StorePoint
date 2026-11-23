import React from 'react';
import { Text, View } from 'react-native';

interface StorePointBrandBadgeProps {
  readonly size?: 'sm' | 'md' | 'lg';
}

const SIZE_CLASSES: Record<NonNullable<StorePointBrandBadgeProps['size']>, { box: string; text: string }> = {
  sm: { box: 'h-9 w-9 rounded-lg', text: 'text-sm' },
  md: { box: 'h-11 w-11 rounded-xl', text: 'text-base' },
  lg: { box: 'h-14 w-14 rounded-2xl', text: 'text-xl' },
};

export function StorePointBrandBadge({ size = 'sm' }: StorePointBrandBadgeProps) {
  const styles = SIZE_CLASSES[size];

  return (
    <View className={`bg-accent items-center justify-center ${styles.box}`}>
      <Text className={`text-accent-foreground font-bold tracking-tight ${styles.text}`}>
        SP
      </Text>
    </View>
  );
}
