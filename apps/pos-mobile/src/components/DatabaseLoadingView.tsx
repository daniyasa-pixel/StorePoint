import React from 'react';
import { Text, View } from 'react-native';
import { Spinner, Surface } from 'heroui-native';
import { StorePointBrandBadge } from './StorePointBrandBadge';

interface DatabaseLoadingViewProps {
  readonly label?: string;
}

export function DatabaseLoadingView({
  label = 'Optimizing StorePoint Database...',
}: DatabaseLoadingViewProps) {
  return (
    <Surface variant="default" className="bg-background flex-1 items-center justify-center p-6">
      <View className="items-center gap-4">
        <StorePointBrandBadge size="lg" />
        <Spinner size="lg" />
        <Text className="text-foreground text-base font-semibold text-center">
          {label}
        </Text>
      </View>
    </Surface>
  );
}
