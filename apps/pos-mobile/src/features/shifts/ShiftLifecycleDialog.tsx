import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Alert, Button, Dialog, Input, Label, Separator, Spinner, Surface, TextField } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { CashierSession } from '@/db/types';
import {
  computeDenominationTotal,
  reconcileShiftDrawer,
  type DenominationCount,
} from '@/domain/cashDrawerCoordinator';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeUp, exitFadeUp } from '@/theme/motion';

interface ShiftLifecycleDialogProps {
  readonly isOpen: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly activeSession: CashierSession | null;
  readonly currencySymbol: string;
  readonly onSessionChanged: (session: CashierSession | null) => void;
}

const EMPTY_DENOMS: DenominationCount = {
  count1000: 0,
  count500: 0,
  count200: 0,
  count100: 0,
  count50: 0,
  count20: 0,
  countCoins: 0,
};

export function ShiftLifecycleDialog({
  isOpen,
  onOpenChange,
  activeSession,
  currencySymbol,
  onSessionChanged,
}: ShiftLifecycleDialogProps) {
  const { dialogMaxWidthClass } = useWindowLayout();
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const [startingFloat, setStartingFloat] = useState('500.00');
  const [denoms, setDenoms] = useState<DenominationCount>(EMPTY_DENOMS);
  const [discrepancyReason, setDiscrepancyReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const countedCash = computeDenominationTotal(denoms);

  const updateDenom = (key: keyof DenominationCount, raw: string) => {
    const parsed = key === 'countCoins' ? Number.parseFloat(raw) : Number.parseInt(raw, 10);
    setDenoms((prev) => ({ ...prev, [key]: Number.isFinite(parsed) && parsed >= 0 ? parsed : 0 }));
  };

  const handleOpenShift = async () => {
    setError(null);
    const floatVal = Number(startingFloat.trim());
    if (!startingFloat.trim() || !Number.isFinite(floatVal) || floatVal < 0) {
      setError('Starting cash float must be a valid non-negative number.');
      return;
    }
    setIsSaving(true);
    try {
      const repos = await getRepositories();
      const session = await repos.shifts.openShift(
        activeUser?.username ?? 'cashier',
        floatVal,
      );
      onSessionChanged(session);
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to open shift.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleCloseShift = async () => {
    if (!activeSession) return;
    setError(null);
    setIsSaving(true);
    try {
      const repos = await getRepositories();
      const [txs, drawerTxs] = await Promise.all([
        repos.checkout.getRecentTransactions(500),
        repos.shifts.getDrawerTransactions(activeSession.id),
      ]);
      const cashSales = txs
        .filter((t) => t.timestamp >= activeSession.startTime && t.paymentMethod === 'CASH')
        .reduce((sum, t) => sum + t.totalAmount, 0);
      const cashIn = drawerTxs.filter((d) => d.amount > 0).reduce((s, d) => s + d.amount, 0);
      const cashOut = drawerTxs
        .filter((d) => d.amount < 0)
        .reduce((s, d) => s + Math.abs(d.amount), 0);

      const rec = reconcileShiftDrawer({
        startingFloat: activeSession.startingCash,
        cashSales,
        cashIn,
        cashOut,
        denominations: denoms,
      });

      await repos.shifts.saveShiftReconciliation({
        sessionId: activeSession.id,
        cashierUsername: activeSession.cashierUsername,
        timestamp: Date.now(),
        ...denoms,
        totalCountedCash: rec.actualCountedCash,
        expectedCash: rec.expectedEndingCash,
        cashVariance: rec.cashVariance,
        countedGCash: 0,
        expectedGCash: 0,
        gcashVariance: 0,
        countedSmartLoad: 0,
        expectedSmartLoad: 0,
        smartLoadVariance: 0,
        countedGlobeLoad: 0,
        expectedGlobeLoad: 0,
        globeLoadVariance: 0,
        hasDiscrepancy: rec.hasDiscrepancy,
        managerOverrideApproved: !rec.requiresManagerOverride,
        managerUsername: activeUser?.role === 'ADMIN' ? activeUser.username : '',
        discrepancyReason: discrepancyReason.trim(),
        receiptPrinted: false,
      });
      await repos.shifts.closeShift(activeSession.id, rec.actualCountedCash);
      setDenoms(EMPTY_DENOMS);
      setDiscrepancyReason('');
      onSessionChanged(null);
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to close shift.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen={isOpen} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className={`p-5 gap-3 ${dialogMaxWidthClass} w-full self-center mx-4 max-h-[90%]`}>
          <Dialog.Title>
            {activeSession ? `Close Shift #${activeSession.id}` : 'Open Cashier Shift'}
          </Dialog.Title>

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

          {!activeSession ? (
            <>
              <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                className="shrink"
              >
                <ScrollView
                  keyboardShouldPersistTaps="handled"
                  showsVerticalScrollIndicator={false}
                  contentContainerClassName="gap-3 pb-1"
                >
                  <TextField>
                    <Label>Starting Cash Float ({currencySymbol})</Label>
                    <Input
                      value={startingFloat}
                      onChangeText={setStartingFloat}
                      keyboardType="decimal-pad"
                    />
                  </TextField>
                </ScrollView>
              </KeyboardAvoidingView>
              <Separator />
              <View className="flex-row justify-end gap-2">
                <Button variant="secondary" onPress={() => onOpenChange(false)} isDisabled={isSaving}>
                  <Button.Label>Cancel</Button.Label>
                </Button>
                <Button variant="primary" isDisabled={isSaving} onPress={handleOpenShift}>
                  {isSaving ? <Spinner size="sm" /> : null}
                  <Button.Label>{isSaving ? 'Opening...' : 'Start Shift'}</Button.Label>
                </Button>
              </View>
            </>
          ) : (
            <>
              <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                className="shrink"
              >
                <ScrollView
                  keyboardShouldPersistTaps="handled"
                  showsVerticalScrollIndicator={false}
                  contentContainerClassName="gap-2.5 pb-1"
                >
                  <Text className="text-muted text-xs">
                    Blind Denomination Count (enter physical bill & coin counts)
                  </Text>
                  {(
                    [
                      ['count1000', `${currencySymbol}1000 Bills`],
                      ['count500', `${currencySymbol}500 Bills`],
                      ['count200', `${currencySymbol}200 Bills`],
                      ['count100', `${currencySymbol}100 Bills`],
                      ['count50', `${currencySymbol}50 Bills`],
                      ['count20', `${currencySymbol}20 Bills`],
                      ['countCoins', `Coins Total (${currencySymbol})`],
                    ] as const
                  ).map(([field, label]) => (
                    <TextField key={field}>
                      <Label>{label}</Label>
                      <Input
                        value={denoms[field] ? String(denoms[field]) : ''}
                        onChangeText={(v) => updateDenom(field, v)}
                        keyboardType="decimal-pad"
                        placeholder="0"
                      />
                    </TextField>
                  ))}

                  <TextField>
                    <Label>Variance / Audit Note (Optional)</Label>
                    <Input
                      value={discrepancyReason}
                      onChangeText={setDiscrepancyReason}
                      placeholder="Notes for manager review"
                    />
                  </TextField>
                </ScrollView>
              </KeyboardAvoidingView>

              <Surface variant="secondary" className="rounded-xl px-3 py-2.5 flex-row items-center justify-between">
                <Text className="text-foreground text-xs font-semibold">Counted Cash Total</Text>
                <Text className="text-accent text-base font-bold">
                  {currencySymbol}
                  {countedCash.toFixed(2)}
                </Text>
              </Surface>

              <Separator />

              <View className="flex-row justify-end gap-2">
                <Button variant="secondary" onPress={() => onOpenChange(false)} isDisabled={isSaving}>
                  <Button.Label>Cancel</Button.Label>
                </Button>
                <Button variant="danger" isDisabled={isSaving} onPress={handleCloseShift}>
                  {isSaving ? <Spinner size="sm" /> : null}
                  <Button.Label>{isSaving ? 'Closing...' : 'Reconcile & Close'}</Button.Label>
                </Button>
              </View>
            </>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
