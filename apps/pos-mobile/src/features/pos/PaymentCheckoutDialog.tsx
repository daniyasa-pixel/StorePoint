import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Alert, Button, Chip, Dialog, Input, Label, Separator, Spinner, TextField } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { CheckoutReceipt } from '@/db/repositories/checkoutRepository';
import {
  computeCheckoutTotals,
  type DiscountType,
} from '@/domain/posCheckoutCoordinator';
import { sessionStateCache } from '@/storage/sessionStateCache';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { usePosCartStore } from '@/store/posCartStore';
import { enterFadeUp, exitFadeUp } from '@/theme/motion';

interface PaymentCheckoutDialogProps {
  readonly isOpen: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly currencySymbol: string;
  readonly vatRate: number;
  readonly onCompleted: (receipt: CheckoutReceipt) => void;
}

const PAYMENT_METHODS = ['CASH', 'GCASH', 'MAYA', 'CARD'] as const;
const DISCOUNT_TYPES: readonly DiscountType[] = ['NONE', 'PERCENTAGE', 'FIXED', 'SENIOR_PWD'];

export function PaymentCheckoutDialog({
  isOpen,
  onOpenChange,
  currencySymbol,
  vatRate,
  onCompleted,
}: PaymentCheckoutDialogProps) {
  const lines = usePosCartStore((s) => s.lines);
  const orderDiscountType = usePosCartStore((s) => s.orderDiscountType);
  const orderDiscountValue = usePosCartStore((s) => s.orderDiscountValue);
  const setOrderDiscount = usePosCartStore((s) => s.setOrderDiscount);
  const clearCheckedLines = usePosCartStore((s) => s.clearCheckedLines);
  const activeUser = useAppBootstrapStore((s) => s.activeUser);

  const [paymentMethod, setPaymentMethod] = useState<string>('CASH');
  const [cashTendered, setCashTendered] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const activeItems = lines
    .filter((l) => l.checked)
    .map((l) => ({
      ...l,
      selectedPrice: Math.max(0, l.selectedPrice - l.itemDiscount),
    }));

  const totals = computeCheckoutTotals(
    activeItems,
    orderDiscountType,
    orderDiscountValue,
    true,
    vatRate,
  );

  const paid =
    paymentMethod === 'CASH'
      ? Number.parseFloat(cashTendered) || 0
      : totals.netTotal;
  const change = Math.max(0, paid - totals.netTotal);

  const handleConfirm = async () => {
    setError(null);
    if (paid < totals.netTotal) {
      setError(`Tendered amount must be at least ${currencySymbol}${totals.netTotal.toFixed(2)}.`);
      return;
    }

    setIsProcessing(true);
    try {
      const repos = await getRepositories();
      const receipt = await repos.checkout.checkout({
        items: activeItems,
        cashierUsername: activeUser?.username ?? 'cashier',
        paymentMethod,
        cashPaid: paid,
        customerName: customerName.trim(),
        discountType: orderDiscountType,
        discountValue: orderDiscountValue,
        vatRate,
      });
      await sessionStateCache.setLastPaymentMethod(paymentMethod);
      clearCheckedLines();
      setCashTendered('');
      setCustomerName('');
      onOpenChange(false);
      onCompleted(receipt);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Checkout failed.');
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <Dialog isOpen={isOpen} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className="p-5 gap-3 max-w-md w-full self-center mx-4 max-h-[90%]">
          <Dialog.Title>Checkout Settlement</Dialog.Title>
          <Dialog.Description>
            Net Total: {currencySymbol}
            {totals.netTotal.toFixed(2)} (VAT {currencySymbol}
            {totals.vatAmount.toFixed(2)})
          </Dialog.Description>

          <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            className="shrink"
          >
            <ScrollView
              keyboardShouldPersistTaps="handled"
              showsVerticalScrollIndicator={false}
              contentContainerClassName="gap-4 pb-1"
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

              <View className="gap-2">
                <Text className="text-foreground text-xs font-semibold">Payment Method</Text>
                <View className="flex-row flex-wrap gap-2">
                  {PAYMENT_METHODS.map((m) => (
                    <Chip
                      key={m}
                      color={paymentMethod === m ? 'accent' : 'default'}
                      variant={paymentMethod === m ? 'primary' : 'soft'}
                      onPress={() => setPaymentMethod(m)}
                    >
                      <Chip.Label>{m}</Chip.Label>
                    </Chip>
                  ))}
                </View>
              </View>

              <View className="gap-2">
                <Text className="text-foreground text-xs font-semibold">Discount</Text>
                <View className="flex-row flex-wrap gap-2">
                  {DISCOUNT_TYPES.map((d) => (
                    <Chip
                      key={d}
                      size="sm"
                      color={orderDiscountType === d ? 'accent' : 'default'}
                      variant={orderDiscountType === d ? 'primary' : 'soft'}
                      onPress={() => setOrderDiscount(d, d === 'PERCENTAGE' ? 10 : 0)}
                    >
                      <Chip.Label>{d}</Chip.Label>
                    </Chip>
                  ))}
                </View>
              </View>

              {paymentMethod === 'CASH' ? (
                <View className="gap-2">
                  <TextField>
                    <Label>Cash Tendered ({currencySymbol})</Label>
                    <Input
                      value={cashTendered}
                      onChangeText={setCashTendered}
                      keyboardType="decimal-pad"
                      placeholder={totals.netTotal.toFixed(2)}
                    />
                  </TextField>
                  <View className="flex-row gap-2">
                    <Button
                      variant="tertiary"
                      size="sm"
                      className="flex-1"
                      onPress={() => setCashTendered(totals.netTotal.toFixed(2))}
                    >
                      <Button.Label>Exact</Button.Label>
                    </Button>
                    <Button
                      variant="tertiary"
                      size="sm"
                      className="flex-1"
                      onPress={() => setCashTendered('500')}
                    >
                      <Button.Label>{currencySymbol}500</Button.Label>
                    </Button>
                    <Button
                      variant="tertiary"
                      size="sm"
                      className="flex-1"
                      onPress={() => setCashTendered('1000')}
                    >
                      <Button.Label>{currencySymbol}1000</Button.Label>
                    </Button>
                  </View>
                  <Text className="text-success text-sm font-bold">
                    Change: {currencySymbol}
                    {change.toFixed(2)}
                  </Text>
                </View>
              ) : null}

              <TextField>
                <Label>Customer Name (Optional)</Label>
                <Input value={customerName} onChangeText={setCustomerName} placeholder="Walk-in" />
              </TextField>
            </ScrollView>
          </KeyboardAvoidingView>

          <Separator />

          <View className="flex-row justify-end gap-2">
            <Button variant="secondary" onPress={() => onOpenChange(false)} isDisabled={isProcessing}>
              <Button.Label>Cancel</Button.Label>
            </Button>
            <Button variant="primary" onPress={handleConfirm} isDisabled={isProcessing}>
              {isProcessing ? <Spinner size="sm" /> : null}
              <Button.Label>{isProcessing ? 'Processing...' : 'Complete Sale'}</Button.Label>
            </Button>
          </View>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
