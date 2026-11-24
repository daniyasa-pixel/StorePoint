import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Alert, Button, Chip, Dialog, Input, Label, Separator, Spinner, TextField } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { CashierSession } from '@/db/types';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeUp, exitFadeUp } from '@/theme/motion';

interface DrawerCashInOutDialogProps {
  readonly isOpen: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly activeSession: CashierSession | null;
  readonly currencySymbol: string;
}

export function DrawerCashInOutDialog({
  isOpen,
  onOpenChange,
  activeSession,
  currencySymbol,
}: DrawerCashInOutDialogProps) {
  const { dialogMaxWidthClass } = useWindowLayout();
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const [direction, setDirection] = useState<'CASH_IN' | 'PAYOUT'>('PAYOUT');
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = async () => {
    setError(null);
    if (!activeSession) {
      setError('Open a shift before recording cash drawer movements.');
      return;
    }
    const val = Number.parseFloat(amount);
    if (!Number.isFinite(val) || val <= 0) {
      setError('Enter a valid positive amount.');
      return;
    }
    if (!reason.trim()) {
      setError('Provide a reason for audit logging.');
      return;
    }

    setIsSaving(true);
    try {
      const repos = await getRepositories();
      await repos.shifts.recordDrawerTransaction({
        sessionId: activeSession.id,
        cashierUsername: activeUser?.username ?? 'cashier',
        amount: direction === 'PAYOUT' ? -val : val,
        type: direction,
        reason: reason.trim(),
      });
      setAmount('');
      setReason('');
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to record drawer movement.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen={isOpen} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className={`p-5 gap-3 ${dialogMaxWidthClass} w-full self-center mx-4 max-h-[90%]`}>
          <Dialog.Title>Cash Drawer In / Out</Dialog.Title>

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

              <View className="flex-row flex-wrap gap-2">
                <Chip
                  color={direction === 'PAYOUT' ? 'danger' : 'default'}
                  variant={direction === 'PAYOUT' ? 'primary' : 'soft'}
                  onPress={() => setDirection('PAYOUT')}
                >
                  <Chip.Label>Cash Out / Payout</Chip.Label>
                </Chip>
                <Chip
                  color={direction === 'CASH_IN' ? 'success' : 'default'}
                  variant={direction === 'CASH_IN' ? 'primary' : 'soft'}
                  onPress={() => setDirection('CASH_IN')}
                >
                  <Chip.Label>Cash In / Top-Up</Chip.Label>
                </Chip>
              </View>

              <TextField>
                <Label>Amount ({currencySymbol})</Label>
                <Input value={amount} onChangeText={setAmount} keyboardType="decimal-pad" placeholder="0.00" />
              </TextField>

              <TextField>
                <Label>Reason</Label>
                <Input value={reason} onChangeText={setReason} placeholder="e.g. Ice delivery, petty cash, bank drop" />
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
              <Button.Label>{isSaving ? 'Saving...' : 'Record Movement'}</Button.Label>
            </Button>
          </View>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
