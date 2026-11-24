import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Alert, Button, Chip, Dialog, Input, Label, Separator, Spinner, TextField } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import { WALLET_ENTRY_TYPES, type WalletType } from '@/domain/money';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeUp, exitFadeUp } from '@/theme/motion';

interface DigitalServicesDialogProps {
  readonly isOpen: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly currencySymbol: string;
}

const WALLETS: readonly { readonly id: WalletType; readonly label: string }[] = [
  { id: 'GCASH', label: 'GCash' },
  { id: 'SMART', label: 'Smart / TNT' },
  { id: 'GLOBE', label: 'Globe / TM' },
];

export function DigitalServicesDialog({
  isOpen,
  onOpenChange,
  currencySymbol,
}: DigitalServicesDialogProps) {
  const { dialogMaxWidthClass } = useWindowLayout();
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const [walletType, setWalletType] = useState<WalletType>('GCASH');
  const [mode, setMode] = useState<'CONSUME' | 'RELOAD'>('CONSUME');
  const [amount, setAmount] = useState('');
  const [reference, setReference] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = async () => {
    setError(null);
    const val = Number.parseFloat(amount);
    if (!Number.isFinite(val) || val <= 0) {
      setError('Enter a valid positive amount.');
      return;
    }

    setIsSaving(true);
    try {
      const repos = await getRepositories();
      await repos.storeConfig.applyWalletDelta({
        walletType,
        delta: mode === 'CONSUME' ? -val : val,
        actorUsername: activeUser?.username ?? 'cashier',
        entryType: mode === 'CONSUME' ? WALLET_ENTRY_TYPES.CONSUMED : WALLET_ENTRY_TYPES.RELOAD,
        reference: reference.trim(),
        notes: mode === 'CONSUME' ? 'POS Digital Service' : 'Wallet Float Reload',
      });
      setAmount('');
      setReference('');
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Wallet update failed.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen={isOpen} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className={`p-5 gap-3 ${dialogMaxWidthClass} w-full self-center mx-4 max-h-[90%]`}>
          <Dialog.Title>Digital Services & E-Wallet</Dialog.Title>

          <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            className="shrink"
          >
            <ScrollView
              keyboardShouldPersistTaps="handled"
              showsVerticalScrollIndicator={false}
              contentContainerClassName="gap-3 pb-1"
            >
              {error ? (
                <Animated.View entering={enterFadeUp} exiting={exitFadeUp}>
                  <Alert status="danger">
                    <Alert.Indicator />
                    <Alert.Content>
                      <Alert.Description>{error}</Alert.Description>
                    </Alert.Content>
                  </Alert>
                </Animated.View>
              ) : null}

              <View className="gap-1.5">
                <Text className="text-foreground text-xs font-semibold">Wallet</Text>
                <View className="flex-row flex-wrap gap-2">
                  {WALLETS.map((w) => (
                    <Chip
                      key={w.id}
                      color={walletType === w.id ? 'accent' : 'default'}
                      variant={walletType === w.id ? 'primary' : 'soft'}
                      onPress={() => setWalletType(w.id)}
                    >
                      <Chip.Label>{w.label}</Chip.Label>
                    </Chip>
                  ))}
                </View>
              </View>

              <View className="flex-row flex-wrap gap-2">
                <Chip
                  color={mode === 'CONSUME' ? 'warning' : 'default'}
                  variant={mode === 'CONSUME' ? 'primary' : 'soft'}
                  onPress={() => setMode('CONSUME')}
                >
                  <Chip.Label>Customer Load / Cash-In</Chip.Label>
                </Chip>
                <Chip
                  color={mode === 'RELOAD' ? 'success' : 'default'}
                  variant={mode === 'RELOAD' ? 'primary' : 'soft'}
                  onPress={() => setMode('RELOAD')}
                >
                  <Chip.Label>Reload Float</Chip.Label>
                </Chip>
              </View>

              <TextField>
                <Label>Amount ({currencySymbol})</Label>
                <Input value={amount} onChangeText={setAmount} keyboardType="decimal-pad" placeholder="0.00" />
              </TextField>

              <TextField>
                <Label>Reference / Mobile Number</Label>
                <Input value={reference} onChangeText={setReference} placeholder="09XX-XXX-XXXX / Ref #" />
              </TextField>
            </ScrollView>
          </KeyboardAvoidingView>

          <Separator />

          <View className="flex-row justify-end gap-2">
            <Button variant="secondary" onPress={() => onOpenChange(false)} isDisabled={isSaving}>
              <Button.Label>Cancel</Button.Label>
            </Button>
            <Button variant="primary" isDisabled={isSaving} onPress={handleSubmit}>
              {isSaving ? <Spinner size="sm" /> : null}
              <Button.Label>{isSaving ? 'Saving...' : 'Submit'}</Button.Label>
            </Button>
          </View>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
