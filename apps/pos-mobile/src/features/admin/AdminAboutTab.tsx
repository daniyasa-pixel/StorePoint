import React from 'react';
import { ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Card, Chip } from 'heroui-native';
import { APP_VERSION } from '@/constants/appInfo';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeUp } from '@/theme/motion';

export function AdminAboutTab() {
  const storeName = useAppBootstrapStore((s) => s.storeName);

  return (
    <ScrollView contentContainerClassName="pb-6">
      <Animated.View entering={enterFadeUp}>
        <Card className="p-5 gap-3">
          <Card.Title>{storeName}</Card.Title>
          <Card.Description>
            StorePoint POS Mobile v{APP_VERSION} — Android APK Edition built with Expo, TypeScript,
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
      </Animated.View>
    </ScrollView>
  );
}
