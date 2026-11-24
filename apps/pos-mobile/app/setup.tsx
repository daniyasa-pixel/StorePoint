import React, { useCallback, useEffect, useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Alert, Button, Card, Chip, Spinner, useToast } from 'heroui-native';
import {
  peekPersistentBackup,
  restoreFromPersistentBackup,
  type PersistentBackupSummary,
} from '@/backup/backupEngine';
import { AppTopBar } from '@/components/AppTopBar';
import { StorePointBrandBadge } from '@/components/StorePointBrandBadge';
import { TactileButton } from '@/components/TactilePressable';
import { ThemeModeSelector } from '@/components/ThemeModeToggle';
import { APP_ROUTES } from '@/constants/routes';
import { getRepositories } from '@/db/repositories';
import { DEFAULT_CURRENCY_SYMBOL } from '@/domain/money';
import {
  parseNonNegativeSetupNumber,
  seedStarterSampleCatalog,
  SetupStepOneProfile,
  type SetupProfileFormValues,
} from '@/features/setup/SetupStepOneProfile';
import { SetupStepTwoSecurity, type SetupSecurityFormValues } from '@/features/setup/SetupStepTwoSecurity';
import { useBackNavigationGuard } from '@/layout/useBackNavigationGuard';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { hashPin, isValidPin, isWeakPin, pinPolicyHint } from '@/security/securityHelper';
import { sessionStateCache } from '@/storage/sessionStateCache';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeUp, enterSlideRight, exitFadeUp, exitSlideLeft, layoutTransition } from '@/theme/motion';

export default function SetupRoute() {
  const router = useRouter();
  const { toast } = useToast();
  const { containerPaddingClass, dialogMaxWidthClass, isShortHeight } = useWindowLayout();

  const [step, setStep] = useState<1 | 2>(1);
  const [profile, setProfile] = useState<SetupProfileFormValues>({
    storeName: '', currencySymbol: DEFAULT_CURRENCY_SYMBOL, taxPercentage: '12.0',
    hasGCash: true, hasMaya: true, hasLoad: true,
    gcashBalance: '0.00', smartLoadBalance: '0.00', globeLoadBalance: '0.00',
    loadServiceFee: '2.00', gcashServiceFee: '10.00', mayaServiceFee: '10.00', mayaBankFee: '15.00',
    seedStarterCatalog: true,
  });
  const [security, setSecurity] = useState<SetupSecurityFormValues>({
    adminName: '', adminPhone: '', adminUsername: 'admin', adminPin: '', confirmPin: '',
  });

  const [backupSummary, setBackupSummary] = useState<PersistentBackupSummary | null>(null);
  const [isRestoringBackup, setIsRestoringBackup] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    void peekPersistentBackup().then(setBackupSummary);
  }, []);

  const handleBackAction = useCallback((): boolean => {
    setError(null);
    if (step === 2) {
      setStep(1);
      return true;
    }
    router.replace(APP_ROUTES.ONBOARDING);
    return true;
  }, [router, step]);

  useBackNavigationGuard({ onBack: handleBackAction });

  const updateProfile = useCallback(
    <K extends keyof SetupProfileFormValues>(field: K, value: SetupProfileFormValues[K]) => {
      setProfile((prev) => ({ ...prev, [field]: value }));
      setError(null);
    },
    [],
  );

  const updateSecurity = useCallback(
    <K extends keyof SetupSecurityFormValues>(field: K, value: SetupSecurityFormValues[K]) => {
      setSecurity((prev) => ({ ...prev, [field]: value }));
      setError(null);
    },
    [],
  );

  const validateStepOne = (): boolean => {
    if (!profile.storeName.trim()) {
      setError('Store / Business Name is required before continuing.');
      return false;
    }
    try {
      parseNonNegativeSetupNumber(profile.taxPercentage, 'VAT percentage');
      parseNonNegativeSetupNumber(profile.gcashBalance, 'GCash balance');
      parseNonNegativeSetupNumber(profile.smartLoadBalance, 'Smart load balance');
      parseNonNegativeSetupNumber(profile.globeLoadBalance, 'Globe load balance');
      parseNonNegativeSetupNumber(profile.loadServiceFee, 'Load service fee');
      parseNonNegativeSetupNumber(profile.gcashServiceFee, 'GCash service fee');
      parseNonNegativeSetupNumber(profile.mayaServiceFee, 'Maya service fee');
      parseNonNegativeSetupNumber(profile.mayaBankFee, 'Maya bank fee');
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid numeric field in Store Profile.');
      return false;
    }
  };

  const handleSelectStep = (target: 1 | 2) => {
    if (target === 2 && !validateStepOne()) return;
    setError(null);
    setStep(target);
  };

  const handleRestoreBackup = async () => {
    setIsRestoringBackup(true);
    setError(null);
    try {
      const outcome = await restoreFromPersistentBackup();
      if (!outcome.ok) {
        setError(outcome.message);
        return;
      }
      const repos = await getRepositories();
      const users = await repos.users.getAllUsers();
      toast.show({ label: 'Backup Restored', description: outcome.message, variant: 'success' });
      if (users.length > 0) {
        router.replace(APP_ROUTES.LOGIN);
      } else {
        setProfile((p) => ({ ...p, storeName: outcome.storeName, currencySymbol: outcome.currencySymbol, seedStarterCatalog: false }));
        setBackupSummary(null);
        setStep(2);
      }
    } finally {
      setIsRestoringBackup(false);
    }
  };

  const handleCompleteSetup = async () => {
    if (!validateStepOne()) {
      setStep(1);
      return;
    }
    const cleanUsername = security.adminUsername.trim();
    if (!cleanUsername) {
      setError('Admin username is required.');
      return;
    }
    if (!isValidPin(security.adminPin) || isWeakPin(security.adminPin)) {
      setError(`Choose a stronger 4-digit PIN. ${pinPolicyHint()}`);
      return;
    }
    if (security.adminPin !== security.confirmPin) {
      setError('Master PIN and Confirm PIN do not match.');
      return;
    }

    setIsSaving(true);
    setError(null);
    try {
      const repos = await getRepositories();
      const cleanStoreName = profile.storeName.trim();

      await repos.storeConfig.saveConfig({
        id: 1,
        storeName: cleanStoreName,
        currencySymbol: profile.currencySymbol,
        taxPercentage: parseNonNegativeSetupNumber(profile.taxPercentage, 'VAT percentage'),
        setupCompleted: true,
        gcashBalance: profile.hasGCash ? parseNonNegativeSetupNumber(profile.gcashBalance, 'GCash balance') : 0,
        hasGCash: profile.hasGCash,
        hasMaya: profile.hasMaya,
        hasLoad: profile.hasLoad,
        smartLoadBalance: profile.hasLoad ? parseNonNegativeSetupNumber(profile.smartLoadBalance, 'Smart load balance') : 0,
        globeLoadBalance: profile.hasLoad ? parseNonNegativeSetupNumber(profile.globeLoadBalance, 'Globe load balance') : 0,
        loadServiceFee: parseNonNegativeSetupNumber(profile.loadServiceFee, 'Load service fee'),
        gcashServiceFee: parseNonNegativeSetupNumber(profile.gcashServiceFee, 'GCash service fee'),
        mayaServiceFee: parseNonNegativeSetupNumber(profile.mayaServiceFee, 'Maya service fee'),
        mayaBankFee: parseNonNegativeSetupNumber(profile.mayaBankFee, 'Maya bank fee'),
      });

      await repos.users.saveUser({
        username: cleanUsername,
        pinHash: hashPin(security.adminPin),
        role: 'ADMIN',
        barcodeId: '',
        biometricEnrolled: false,
        pinResetRequired: false,
      });

      await sessionStateCache.saveAdminContact(
        security.adminName.trim() || cleanUsername,
        security.adminPhone.trim(),
      );

      const [existingCats, existingProducts] = await Promise.all([
        repos.catalog.getCategories(true),
        repos.catalog.getProducts(true),
      ]);

      if (profile.seedStarterCatalog && existingProducts.length === 0 && existingCats.length === 0) {
        await seedStarterSampleCatalog(repos);
      } else if (existingCats.length === 0) {
        await repos.catalog.saveCategory({
          name: 'General',
          colorAccent: '#006FEE',
          iconBadge: 'store',
          isArchived: false,
        });
      }

      const store = useAppBootstrapStore.getState();
      store.completeSetup(cleanStoreName, profile.currencySymbol);
      store.setActiveUser({
        id: 1,
        username: cleanUsername,
        fullName: security.adminName.trim() || cleanUsername,
        role: 'ADMIN',
        pinResetRequired: false,
      });

      toast.show({ label: 'Store setup complete! Welcome to StorePoint.', variant: 'success' });
      router.replace(APP_ROUTES.ADMIN_DASHBOARD);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save store setup.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <SafeAreaView className="bg-background flex-1">
      <AppTopBar
        title="Store Setup"
        subtitle={`Step ${step} of 2 — ${step === 1 ? 'Profile & Money' : 'Security & Admin'}`}
        onBack={handleBackAction}
        rightActions={
          <Chip color="accent" variant="soft" size="sm">
            <Chip.Label>{step}/2</Chip.Label>
          </Chip>
        }
      />

      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} className="flex-1">
        <ScrollView contentContainerClassName={`${containerPaddingClass} pb-10`} keyboardShouldPersistTaps="handled">
          <Animated.View
            layout={layoutTransition}
            className={`w-full ${dialogMaxWidthClass} self-center ${isShortHeight ? 'gap-3' : 'gap-4'}`}
          >
            <View className="items-center gap-2 pt-1">
              <StorePointBrandBadge size={isShortHeight ? 'sm' : 'md'} />
              <Text className="text-foreground text-xl font-bold text-center">Welcome to StorePoint POS</Text>
              <Text className="text-muted text-xs text-center">
                Set up your offline cash register, retail wallets, and owner credentials in under a minute.
              </Text>
            </View>

            <Card>
              <Card.Body className="flex-row items-center justify-between gap-3 flex-wrap py-3">
                <View className="gap-0.5">
                  <Text className="text-foreground text-sm font-semibold">Appearance & Theme</Text>
                  <Text className="text-muted text-xs">Choose comfortable counter lighting</Text>
                </View>
                <ThemeModeSelector size="sm" />
              </Card.Body>
            </Card>

            <View className="flex-row items-center justify-center gap-2 flex-wrap">
              {(
                [
                  [1, '1. Profile & Money'],
                  [2, '2. Security & Admin'],
                ] as const
              ).map(([targetStep, label]) => (
                <Chip
                  key={targetStep}
                  size="md"
                  color={step === targetStep ? 'accent' : 'default'}
                  variant={step === targetStep ? 'primary' : 'soft'}
                  accessibilityRole="tab"
                  accessibilityState={{ selected: step === targetStep }}
                  onPress={() => handleSelectStep(targetStep)}
                >
                  <Chip.Label>{label}</Chip.Label>
                </Chip>
              ))}
            </View>

            {backupSummary ? (
              <Animated.View entering={enterFadeUp} exiting={exitFadeUp}>
                <Card className="border border-accent/50">
                  <Card.Header>
                    <Card.Title>Previous Backup Found</Card.Title>
                    <Card.Description>
                      {backupSummary.storeName} • {backupSummary.productsCount} products •{' '}
                      {backupSummary.transactionsCount} transactions
                    </Card.Description>
                  </Card.Header>
                  <Card.Footer className="flex-row items-center gap-2 pt-3">
                    <TactileButton variant="primary" size="sm" isDisabled={isRestoringBackup} onPress={() => void handleRestoreBackup()}>
                      {isRestoringBackup ? <Spinner size="sm" /> : null}
                      <Button.Label>{isRestoringBackup ? 'Restoring...' : 'Restore Store Now'}</Button.Label>
                    </TactileButton>
                    <TactileButton variant="ghost" size="sm" isDisabled={isRestoringBackup} onPress={() => setBackupSummary(null)}>
                      <Button.Label>Dismiss</Button.Label>
                    </TactileButton>
                  </Card.Footer>
                </Card>
              </Animated.View>
            ) : null}

            {error ? (
              <Animated.View entering={enterFadeUp} exiting={exitFadeUp}>
                <Alert status="danger">
                  <Alert.Indicator />
                  <Alert.Content>
                    <Alert.Title>Check Setup Input</Alert.Title>
                    <Alert.Description>{error}</Alert.Description>
                  </Alert.Content>
                </Alert>
              </Animated.View>
            ) : null}

            <Animated.View key={step} entering={enterSlideRight} exiting={exitSlideLeft}>
              {step === 1 ? (
                <SetupStepOneProfile
                  values={profile}
                  onChange={updateProfile}
                  onNext={() => handleSelectStep(2)}
                />
              ) : (
                <SetupStepTwoSecurity
                  values={security}
                  isSaving={isSaving}
                  onChange={updateSecurity}
                  onBack={() => handleSelectStep(1)}
                  onComplete={() => void handleCompleteSetup()}
                />
              )}
            </Animated.View>
          </Animated.View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
