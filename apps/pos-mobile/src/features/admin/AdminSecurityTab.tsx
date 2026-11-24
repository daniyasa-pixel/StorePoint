import React, { useCallback, useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import {
  Alert,
  Button,
  Card,
  Chip,
  Input,
  Label,
  Separator,
  Spinner,
  Surface,
  Switch,
  TextField,
  useToast,
} from 'heroui-native';
import { TactileButton } from '@/components/TactilePressable';
import { ThemeModeSelector } from '@/components/ThemeModeToggle';
import { checkDatabaseHealth, type DatabaseHealthReport } from '@/db/database';
import { getRepositories } from '@/db/repositories';
import type { StoreConfig } from '@/db/types';
import { DEFAULT_CURRENCY_SYMBOL } from '@/domain/money';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeUp } from '@/theme/motion';

interface AdminSecurityTabProps {
  readonly onExportBackup?: () => void;
  readonly onTestPrint?: () => void;
}

export function AdminSecurityTab({ onExportBackup, onTestPrint }: AdminSecurityTabProps) {
  const { toast } = useToast();
  const isKioskActive = useAppBootstrapStore((s) => s.isKioskModeActive);
  const setKioskActive = useAppBootstrapStore((s) => s.setKioskModeActive);
  const isAlwaysOn = useAppBootstrapStore((s) => s.isAlwaysOnEnabled);
  const setAlwaysOn = useAppBootstrapStore((s) => s.setAlwaysOnEnabled);
  const currencySymbol = useAppBootstrapStore((s) => s.currencySymbol);
  const completeSetup = useAppBootstrapStore((s) => s.completeSetup);

  const [health, setHealth] = useState<DatabaseHealthReport | null>(null);
  const [isCheckingHealth, setIsCheckingHealth] = useState(true);

  const [existingConfig, setExistingConfig] = useState<StoreConfig | null>(null);
  const [storeName, setStoreName] = useState('');
  const [taxPercentage, setTaxPercentage] = useState('12.0');
  const [hasGCash, setHasGCash] = useState(true);
  const [hasMaya, setHasMaya] = useState(true);
  const [hasLoad, setHasLoad] = useState(true);
  const [loadServiceFee, setLoadServiceFee] = useState('2.00');
  const [gcashServiceFee, setGcashServiceFee] = useState('10.00');
  const [mayaServiceFee, setMayaServiceFee] = useState('10.00');
  const [mayaBankFee, setMayaBankFee] = useState('15.00');
  const [isLoadingConfig, setIsLoadingConfig] = useState(true);
  const [isSavingConfig, setIsSavingConfig] = useState(false);
  const [configError, setConfigError] = useState<string | null>(null);

  const refreshHealth = useCallback(async () => {
    setIsCheckingHealth(true);
    try {
      setHealth(await checkDatabaseHealth());
    } finally {
      setIsCheckingHealth(false);
    }
  }, []);

  const loadStoreConfig = useCallback(async () => {
    setIsLoadingConfig(true);
    setConfigError(null);
    try {
      const repos = await getRepositories();
      const cfg = await repos.storeConfig.getConfig();
      if (cfg) {
        setExistingConfig(cfg);
        setStoreName(cfg.storeName);
        setTaxPercentage(String(cfg.taxPercentage));
        setHasGCash(cfg.hasGCash);
        setHasMaya(cfg.hasMaya);
        setHasLoad(cfg.hasLoad);
        setLoadServiceFee(cfg.loadServiceFee.toFixed(2));
        setGcashServiceFee(cfg.gcashServiceFee.toFixed(2));
        setMayaServiceFee(cfg.mayaServiceFee.toFixed(2));
        setMayaBankFee(cfg.mayaBankFee.toFixed(2));
      } else {
        const fallbackName = useAppBootstrapStore.getState().storeName;
        setStoreName(fallbackName);
      }
    } catch (err) {
      setConfigError(err instanceof Error ? err.message : 'Failed to load store configuration.');
    } finally {
      setIsLoadingConfig(false);
    }
  }, []);

  useEffect(() => {
    void refreshHealth();
    void loadStoreConfig();
  }, [refreshHealth, loadStoreConfig]);

  const handleSaveStoreConfig = async () => {
    if (isSavingConfig) return;
    setConfigError(null);

    const trimmedName = storeName.trim();
    if (!trimmedName) {
      setConfigError('Store name is required.');
      return;
    }

    const parsedVat = Number.parseFloat(taxPercentage.trim());
    if (!Number.isFinite(parsedVat) || parsedVat < 0 || parsedVat > 100) {
      setConfigError('VAT % must be a valid number between 0 and 100.');
      return;
    }

    const parsedLoadFee = Number.parseFloat(loadServiceFee.trim());
    const parsedGcashFee = Number.parseFloat(gcashServiceFee.trim());
    const parsedMayaFee = Number.parseFloat(mayaServiceFee.trim());
    const parsedMayaBankFee = Number.parseFloat(mayaBankFee.trim());

    if (
      !Number.isFinite(parsedLoadFee) ||
      parsedLoadFee < 0 ||
      !Number.isFinite(parsedGcashFee) ||
      parsedGcashFee < 0 ||
      !Number.isFinite(parsedMayaFee) ||
      parsedMayaFee < 0 ||
      !Number.isFinite(parsedMayaBankFee) ||
      parsedMayaBankFee < 0
    ) {
      setConfigError('All service fees must be valid non-negative numbers.');
      return;
    }

    setIsSavingConfig(true);
    try {
      const repos = await getRepositories();
      const latest = (await repos.storeConfig.getConfig()) ?? existingConfig;
      const resolvedCurrency = latest?.currencySymbol ?? currencySymbol ?? DEFAULT_CURRENCY_SYMBOL;

      const updated: StoreConfig = {
        id: latest?.id ?? 1,
        storeName: trimmedName,
        currencySymbol: resolvedCurrency,
        taxPercentage: parsedVat,
        setupCompleted: latest?.setupCompleted ?? true,
        gcashBalance: latest?.gcashBalance ?? 0,
        hasGCash,
        hasMaya,
        hasLoad,
        smartLoadBalance: latest?.smartLoadBalance ?? 0,
        globeLoadBalance: latest?.globeLoadBalance ?? 0,
        loadServiceFee: parsedLoadFee,
        gcashServiceFee: parsedGcashFee,
        mayaServiceFee: parsedMayaFee,
        mayaBankFee: parsedMayaBankFee,
      };

      await repos.storeConfig.saveConfig(updated);
      setExistingConfig(updated);
      completeSetup(trimmedName, resolvedCurrency);

      toast.show({
        label: 'Store settings saved',
        description: `${trimmedName} profile & service fees updated.`,
        variant: 'success',
      });
    } catch (err) {
      setConfigError(err instanceof Error ? err.message : 'Failed to save store configuration.');
    } finally {
      setIsSavingConfig(false);
    }
  };

  return (
    <ScrollView keyboardShouldPersistTaps="handled" contentContainerClassName="gap-4 pb-6">
      <Animated.View entering={enterFadeUp}>
        <Card className="p-4 gap-3">
          <Card.Title>Appearance & Theme</Card.Title>
          <Text className="text-muted text-xs">
            Choose System, Light, or Dark mode across POS, Admin, and Inventory terminals.
          </Text>
          <ThemeModeSelector />
        </Card>
      </Animated.View>

      <Animated.View entering={enterFadeUp}>
        <Card className="p-4 gap-3">
          <View className="flex-row items-center justify-between gap-2">
            <Card.Title>Store Profile & Service Fees</Card.Title>
            {isLoadingConfig ? <Spinner size="sm" /> : null}
          </View>
          <Text className="text-muted text-xs">
            Update store identity, VAT rate, enabled e-wallet services, and default service fees.
          </Text>

          {configError ? (
            <Alert status="danger">
              <Alert.Indicator />
              <Alert.Content>
                <Alert.Description>{configError}</Alert.Description>
              </Alert.Content>
            </Alert>
          ) : null}

          <View className="flex-row gap-2">
            <TextField className="flex-2" isRequired>
              <Label>Store Name</Label>
              <Input
                value={storeName}
                onChangeText={setStoreName}
                placeholder="StorePoint Mini Mart"
              />
            </TextField>
            <TextField className="flex-1">
              <Label>VAT (%)</Label>
              <Input
                value={taxPercentage}
                onChangeText={setTaxPercentage}
                keyboardType="decimal-pad"
                placeholder="12.0"
              />
            </TextField>
          </View>

          <Surface variant="secondary" className="rounded-xl p-3 gap-2.5">
            <View className="flex-row items-center justify-between">
              <Text className="text-foreground text-sm font-medium">Enable GCash Services</Text>
              <Switch isSelected={hasGCash} onSelectedChange={setHasGCash} />
            </View>
            <Separator />
            <View className="flex-row items-center justify-between">
              <Text className="text-foreground text-sm font-medium">Enable Maya Services</Text>
              <Switch isSelected={hasMaya} onSelectedChange={setHasMaya} />
            </View>
            <Separator />
            <View className="flex-row items-center justify-between">
              <Text className="text-foreground text-sm font-medium">Enable Prepaid Load</Text>
              <Switch isSelected={hasLoad} onSelectedChange={setHasLoad} />
            </View>
          </Surface>

          <View className="flex-row gap-2">
            <TextField className="flex-1">
              <Label>Load Fee ({currencySymbol})</Label>
              <Input
                value={loadServiceFee}
                onChangeText={setLoadServiceFee}
                keyboardType="decimal-pad"
                placeholder="2.00"
              />
            </TextField>
            <TextField className="flex-1">
              <Label>GCash Fee ({currencySymbol})</Label>
              <Input
                value={gcashServiceFee}
                onChangeText={setGcashServiceFee}
                keyboardType="decimal-pad"
                placeholder="10.00"
              />
            </TextField>
          </View>

          <View className="flex-row gap-2">
            <TextField className="flex-1">
              <Label>Maya Fee ({currencySymbol})</Label>
              <Input
                value={mayaServiceFee}
                onChangeText={setMayaServiceFee}
                keyboardType="decimal-pad"
                placeholder="10.00"
              />
            </TextField>
            <TextField className="flex-1">
              <Label>Maya Bank Fee ({currencySymbol})</Label>
              <Input
                value={mayaBankFee}
                onChangeText={setMayaBankFee}
                keyboardType="decimal-pad"
                placeholder="15.00"
              />
            </TextField>
          </View>

          <View className="flex-row justify-end pt-1">
            <TactileButton
              feedbackVariant="stretch"
              variant="primary"
              size="sm"
              isDisabled={isSavingConfig || isLoadingConfig}
              onPress={() => void handleSaveStoreConfig()}
            >
              {isSavingConfig ? <Spinner size="sm" /> : null}
              <Button.Label>{isSavingConfig ? 'Saving...' : 'Save Store Settings'}</Button.Label>
            </TactileButton>
          </View>
        </Card>
      </Animated.View>

      <Animated.View entering={enterFadeUp}>
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
      </Animated.View>

      <Animated.View entering={enterFadeUp}>
        <Card className="p-4 gap-3">
          <View className="flex-row items-center justify-between gap-2">
            <Card.Title>SQLite Engine & Maintenance</Card.Title>
            <View className="flex-row items-center gap-2">
              {isCheckingHealth ? <Spinner size="sm" /> : null}
              <Chip size="sm" variant="soft" color={health?.integrityOk ? 'success' : 'warning'}>
                <Chip.Label>
                  {isCheckingHealth ? 'Checking' : health?.integrityOk ? 'Healthy' : 'Attention'}
                </Chip.Label>
              </Chip>
            </View>
          </View>
          <Text className="text-muted text-xs">
            {health ? health.message : 'Probing SQLite WAL integrity...'}
          </Text>
          <View className="flex-row flex-wrap gap-2 pt-1">
            <TactileButton
              variant="secondary"
              size="sm"
              isDisabled={isCheckingHealth}
              onPress={() => void refreshHealth()}
            >
              <Button.Label>Run Health Check</Button.Label>
            </TactileButton>
            {onExportBackup ? (
              <TactileButton variant="primary" size="sm" onPress={onExportBackup}>
                <Button.Label>Export JSON Backup</Button.Label>
              </TactileButton>
            ) : null}
            {onTestPrint ? (
              <TactileButton variant="outline" size="sm" onPress={onTestPrint}>
                <Button.Label>Test Thermal Printer</Button.Label>
              </TactileButton>
            ) : null}
          </View>
        </Card>
      </Animated.View>
    </ScrollView>
  );
}
