import React, { useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { Button, Card, Chip, Separator, Switch } from 'heroui-native';
import { checkDatabaseHealth, type DatabaseHealthReport } from '@/db/database';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

interface AdminSecurityTabProps {
  readonly onExportBackup?: () => void;
  readonly onTestPrint?: () => void;
}

export function AdminSecurityTab({ onExportBackup, onTestPrint }: AdminSecurityTabProps) {
  const isKioskActive = useAppBootstrapStore((s) => s.isKioskModeActive);
  const setKioskActive = useAppBootstrapStore((s) => s.setKioskModeActive);
  const isAlwaysOn = useAppBootstrapStore((s) => s.isAlwaysOnEnabled);
  const setAlwaysOn = useAppBootstrapStore((s) => s.setAlwaysOnEnabled);

  const [health, setHealth] = useState<DatabaseHealthReport | null>(null);

  const refreshHealth = async () => {
    setHealth(await checkDatabaseHealth());
  };

  useEffect(() => {
    void refreshHealth();
  }, []);

  return (
    <ScrollView contentContainerClassName="gap-4 pb-6">
      <Card className="p-4 gap-3">
        <Card.Title>Terminal Kiosk & Display Controls</Card.Title>
        <View className="flex-row items-center justify-between">
          <View className="flex-1 pr-3">
            <Text className="text-foreground text-sm font-medium">Kiosk Lockdown Mode</Text>
            <Text className="text-muted text-xs">
              Engages Android LockTask & hides system navigation bars on provisioned terminals.
            </Text>
          </View>
          <Switch isSelected={isKioskActive} onSelectedChange={setKioskActive} />
        </View>
        <Separator />
        <View className="flex-row items-center justify-between">
          <View className="flex-1 pr-3">
            <Text className="text-foreground text-sm font-medium">Keep Screen Always On</Text>
            <Text className="text-muted text-xs">Prevents counter terminal display sleep.</Text>
          </View>
          <Switch isSelected={isAlwaysOn} onSelectedChange={setAlwaysOn} />
        </View>
      </Card>

      <Card className="p-4 gap-3">
        <View className="flex-row items-center justify-between">
          <Card.Title>SQLite Engine & Maintenance</Card.Title>
          <Chip size="sm" variant="soft" color={health?.integrityOk ? 'success' : 'warning'}>
            <Chip.Label>{health?.integrityOk ? 'Healthy' : 'Checking'}</Chip.Label>
          </Chip>
        </View>
        <Text className="text-muted text-xs">
          {health ? health.message : 'Probing SQLite WAL integrity...'}
        </Text>
        <View className="flex-row flex-wrap gap-2 pt-1">
          <Button variant="secondary" size="sm" onPress={() => void refreshHealth()}>
            <Button.Label>Run Health Check</Button.Label>
          </Button>
          {onExportBackup ? (
            <Button variant="primary" size="sm" onPress={onExportBackup}>
              <Button.Label>Export JSON Backup</Button.Label>
            </Button>
          ) : null}
          {onTestPrint ? (
            <Button variant="outline" size="sm" onPress={onTestPrint}>
              <Button.Label>Test Thermal Printer</Button.Label>
            </Button>
          ) : null}
        </View>
      </Card>
    </ScrollView>
  );
}
