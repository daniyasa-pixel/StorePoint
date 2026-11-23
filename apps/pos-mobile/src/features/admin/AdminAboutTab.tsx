import React from 'react';
import { Text, View } from 'react-native';
import { Card, Chip } from 'heroui-native';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

export function AdminAboutTab() {
  const storeName = useAppBootstrapStore((s) => s.storeName);

  return (
    <Card className="p-5 gap-3">
      <Card.Title>{storeName}</Card.Title>
      <Card.Description>
        StorePoint POS Mobile v1.0.1.10 — Android APK Edition built with Expo, TypeScript,
        HeroUI Native (v3), Uniwind, and offline SQLite v19.
      </Card.Description>
      <View className="flex-row flex-wrap gap-2 pt-1">
        <Chip size="sm" color="accent" variant="soft">
          <Chip.Label>100% HeroUI Native</Chip.Label>
        </Chip>
        <Chip size="sm" color="success" variant="soft">
          <Chip.Label>Android APK Only</Chip.Label>
        </Chip>
        <Chip size="sm" color="default" variant="soft">
          <Chip.Label>Room v19 Parity</Chip.Label>
        </Chip>
      </View>
      <Text className="text-muted text-xs pt-2">
        Zero cloud dependency. All financial records remain on-device.
      </Text>
    </Card>
  );
}
