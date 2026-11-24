import React, { useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Alert, Card, Chip, Separator, Spinner } from 'heroui-native';
import { AppTopBar } from '@/components/AppTopBar';
import { checkDatabaseHealth, type DatabaseHealthReport } from '@/db/database';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeUp } from '@/theme/motion';

export default function AboutRoute() {
  const router = useRouter();
  const storeName = useAppBootstrapStore((s) => s.storeName);
  const [health, setHealth] = useState<DatabaseHealthReport | null>(null);

  useEffect(() => {
    void checkDatabaseHealth().then(setHealth);
  }, []);

  return (
    <SafeAreaView className="bg-background flex-1">
      <AppTopBar
        title="About StorePoint"
        subtitle="Version 1.0.2.0 • Android APK"
        onBack={() => router.back()}
      />
      <ScrollView contentContainerClassName="p-5 gap-4">
        <Animated.View entering={enterFadeUp} className="w-full max-w-xl self-center">
          <Card className="w-full">
            <Card.Header>
              <Card.Title>{storeName}</Card.Title>
              <Card.Description>
                Offline-first Android POS built with Expo, TypeScript, HeroUI Native (v3), and Uniwind.
              </Card.Description>
            </Card.Header>
            <Card.Body className="gap-3 pt-3">
              <View className="flex-row flex-wrap gap-2">
                <Chip color="accent" variant="soft" size="sm">
                  <Chip.Label>HeroUI Native v3</Chip.Label>
                </Chip>
                <Chip color="success" variant="soft" size="sm">
                  <Chip.Label>Offline SQLite v19</Chip.Label>
                </Chip>
                <Chip color="default" variant="soft" size="sm">
                  <Chip.Label>Android APK</Chip.Label>
                </Chip>
              </View>
              <Separator />
              {health === null ? (
                <View className="flex-row items-center gap-2 py-1">
                  <Spinner size="sm" />
                  <Text className="text-muted text-xs">Running SQLite integrity check...</Text>
                </View>
              ) : (
                <Alert
                  status={
                    health.isConnected && health.isWritable && health.integrityOk
                      ? 'success'
                      : 'danger'
                  }
                >
                  <Alert.Indicator />
                  <Alert.Content>
                    <Alert.Title>SQLite Engine Health</Alert.Title>
                    <Alert.Description>{health.message}</Alert.Description>
                  </Alert.Content>
                </Alert>
              )}
            </Card.Body>
          </Card>
        </Animated.View>
      </ScrollView>
    </SafeAreaView>
  );
}
