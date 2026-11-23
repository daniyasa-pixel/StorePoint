import React, { useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import {
  Alert,
  Button,
  Card,
  Chip,
  Input,
  Label,
  Switch,
  TextField,
} from 'heroui-native';
import { AppTopBar } from '@/components/AppTopBar';
import { PinPadEntry } from '@/components/PinPadEntry';
import { APP_ROUTES } from '@/constants/routes';
import { getRepositories } from '@/db/repositories';
import { hashPin, isValidPin, isWeakPin, pinPolicyHint } from '@/security/securityHelper';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

const CURRENCIES = ['₱', '$', '€', '£', '¥'] as const;

export default function SetupRoute() {
  const router = useRouter();
  const [step, setStep] = useState<1 | 2>(1);
  const [storeName, setStoreName] = useState('');
  const [currencySymbol, setCurrencySymbol] = useState<string>('₱');
  const [taxPercentage, setTaxPercentage] = useState('12.0');
  const [hasGCash, setHasGCash] = useState(true);
  const [hasMaya, setHasMaya] = useState(true);
  const [hasLoad, setHasLoad] = useState(true);

  const [adminUsername, setAdminUsername] = useState('admin');
  const [adminPin, setAdminPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const handleNextStep = () => {
    if (!storeName.trim()) {
      setError('Store name is required.');
      return;
    }
    setError(null);
    setStep(2);
  };

  const handleCompleteSetup = async () => {
    if (!adminUsername.trim()) {
      setError('Admin username is required.');
      return;
    }
    if (!isValidPin(adminPin)) {
      setError('Admin PIN must be exactly 4 digits.');
      return;
    }
    if (isWeakPin(adminPin)) {
      setError(`PIN is too easy to guess. ${pinPolicyHint()}`);
      return;
    }

    setIsSaving(true);
    setError(null);
    try {
      const repos = await getRepositories();
      const parsedTax = Number.parseFloat(taxPercentage);
      await repos.storeConfig.saveConfig({
        id: 1,
        storeName: storeName.trim(),
        currencySymbol,
        taxPercentage: Number.isFinite(parsedTax) ? parsedTax : 12.0,
        setupCompleted: true,
        gcashBalance: 0,
        hasGCash,
        hasMaya,
        hasLoad,
        smartLoadBalance: 0,
        globeLoadBalance: 0,
        loadServiceFee: 2.0,
        gcashServiceFee: 10.0,
        mayaServiceFee: 10.0,
        mayaBankFee: 15.0,
      });

      await repos.users.saveUser({
        username: adminUsername.trim(),
        pinHash: hashPin(adminPin),
        role: 'ADMIN',
        barcodeId: '',
        biometricEnrolled: false,
        pinResetRequired: false,
      });

      const existingCats = await repos.catalog.getCategories(true);
      if (existingCats.length === 0) {
        await repos.catalog.saveCategory({
          name: 'General',
          colorAccent: '#006FEE',
          iconBadge: 'store',
          isArchived: false,
        });
      }

      useAppBootstrapStore.getState().completeSetup(storeName.trim(), currencySymbol);
      router.replace(APP_ROUTES.LOGIN);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save setup.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <SafeAreaView className="bg-background flex-1">
      <AppTopBar
        title="Store Setup"
        subtitle={`Step ${step} of 2 — ${step === 1 ? 'Store Profile & Services' : 'Admin Account Security'}`}
        rightActions={
          <Chip color="accent" variant="soft" size="sm">
            <Chip.Label>{step}/2</Chip.Label>
          </Chip>
        }
      />

      <ScrollView contentContainerClassName="p-5 pb-10">
        <View className="w-full max-w-xl self-center gap-4">
          {error ? (
            <Alert status="danger">
              <Alert.Indicator />
              <Alert.Content>
                <Alert.Title>Check Setup Input</Alert.Title>
                <Alert.Description>{error}</Alert.Description>
              </Alert.Content>
            </Alert>
          ) : null}

          {step === 1 ? (
            <Card>
              <Card.Header>
                <Card.Title>Store Profile & Currency</Card.Title>
                <Card.Description>
                  Configure your store identity, VAT rate, and digital services.
                </Card.Description>
              </Card.Header>
              <Card.Body className="gap-4 pt-4">
                <TextField isRequired>
                  <Label>Store Name</Label>
                  <Input
                    value={storeName}
                    onChangeText={setStoreName}
                    placeholder="e.g. Munzo Sari-Sari & Mini Mart"
                  />
                </TextField>

                <View className="gap-2">
                  <Text className="text-foreground text-sm font-medium">Currency Symbol</Text>
                  <View className="flex-row flex-wrap gap-2">
                    {CURRENCIES.map((symbol) => (
                      <Chip
                        key={symbol}
                        color={currencySymbol === symbol ? 'accent' : 'default'}
                        variant={currencySymbol === symbol ? 'primary' : 'soft'}
                        onPress={() => setCurrencySymbol(symbol)}
                      >
                        <Chip.Label>{symbol}</Chip.Label>
                      </Chip>
                    ))}
                  </View>
                </View>

                <TextField>
                  <Label>VAT Percentage (%)</Label>
                  <Input
                    value={taxPercentage}
                    onChangeText={setTaxPercentage}
                    keyboardType="decimal-pad"
                    placeholder="12.0"
                  />
                </TextField>

                <View className="gap-3 pt-1">
                  <View className="flex-row items-center justify-between">
                    <Text className="text-foreground text-sm">Enable GCash Services</Text>
                    <Switch isSelected={hasGCash} onSelectedChange={setHasGCash} />
                  </View>
                  <View className="flex-row items-center justify-between">
                    <Text className="text-foreground text-sm">Enable Maya Services</Text>
                    <Switch isSelected={hasMaya} onSelectedChange={setHasMaya} />
                  </View>
                  <View className="flex-row items-center justify-between">
                    <Text className="text-foreground text-sm">Enable Prepaid Load</Text>
                    <Switch isSelected={hasLoad} onSelectedChange={setHasLoad} />
                  </View>
                </View>
              </Card.Body>
              <Card.Footer className="pt-4">
                <Button variant="primary" size="lg" className="w-full" onPress={handleNextStep}>
                  <Button.Label>Next: Admin Security</Button.Label>
                </Button>
              </Card.Footer>
            </Card>
          ) : (
            <Card>
              <Card.Header>
                <Card.Title>Create Owner / Admin PIN</Card.Title>
                <Card.Description>{pinPolicyHint()}</Card.Description>
              </Card.Header>
              <Card.Body className="gap-4 pt-4">
                <TextField isRequired>
                  <Label>Admin Username</Label>
                  <Input
                    value={adminUsername}
                    onChangeText={setAdminUsername}
                    autoCapitalize="none"
                    placeholder="admin"
                  />
                </TextField>

                <PinPadEntry
                  pin={adminPin}
                  onPinChange={(next) => {
                    setAdminPin(next);
                    setError(null);
                  }}
                  isDisabled={isSaving}
                  helperText="Enter a strong 4-digit Admin PIN"
                />
              </Card.Body>
              <Card.Footer className="flex-row justify-between gap-3 pt-4">
                <Button variant="secondary" onPress={() => setStep(1)} isDisabled={isSaving}>
                  <Button.Label>Back</Button.Label>
                </Button>
                <Button
                  variant="primary"
                  className="flex-1"
                  onPress={handleCompleteSetup}
                  isDisabled={isSaving || adminPin.length < 4}
                >
                  <Button.Label>{isSaving ? 'Saving...' : 'Complete Setup'}</Button.Label>
                </Button>
              </Card.Footer>
            </Card>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
