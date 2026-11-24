import React, { useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Alert, Button, Chip, Dialog, Separator, Spinner, Surface, Switch } from 'heroui-native';
import { TactilePressable } from '@/components/TactilePressable';
import { getRepositories } from '@/db/repositories';
import type { Transaction, TransactionItem } from '@/db/types';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeUp, enterSlideRight, exitFadeUp, exitSlideLeft } from '@/theme/motion';

interface ReturnRefundDialogProps {
  readonly isOpen: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly currencySymbol: string;
  readonly onRefundCompleted: () => void;
}

const REASONS = ['Defective / Spoiled', 'Expired', 'Wrong Item', 'Customer Changed Mind'] as const;

export function ReturnRefundDialog({
  isOpen,
  onOpenChange,
  currencySymbol,
  onRefundCompleted,
}: ReturnRefundDialogProps) {
  const { dialogMaxWidthClass } = useWindowLayout();
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const [recentTxs, setRecentTxs] = useState<Transaction[]>([]);
  const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);
  const [txItems, setTxItems] = useState<TransactionItem[]>([]);
  const [reason, setReason] = useState<string>(REASONS[0]);
  const [restock, setRestock] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    let active = true;
    setSelectedTx(null);
    setTxItems([]);
    setError(null);
    void getRepositories()
      .then((repos) => repos.checkout.getRecentTransactions(15))
      .then((txs) => {
        if (active) setRecentTxs(txs);
      })
      .catch(() => {
        // Ignore load errors if dialog unmounts or DB query fails
      });
    return () => {
      active = false;
    };
  }, [isOpen]);

  const handleSelectTx = async (tx: Transaction) => {
    const repos = await getRepositories();
    setSelectedTx(tx);
    setTxItems(await repos.checkout.getTransactionItems(tx.id));
  };

  const handleRefundAll = async () => {
    if (!selectedTx || txItems.length === 0) return;
    setIsProcessing(true);
    setError(null);
    try {
      const repos = await getRepositories();
      await repos.returns.processReturn({
        originalTransactionId: selectedTx.id,
        cashierUsername: activeUser?.username ?? 'cashier',
        refundMethod: 'CASH',
        returnReason: reason,
        items: txItems.map((i) => ({
          productId: i.productId,
          productName: i.productName,
          quantity: i.quantity,
          unitPrice: i.price,
          refundAmount: i.price * i.quantity,
          uomName: i.uomName,
          restockToInventory: restock,
        })),
      });
      onRefundCompleted();
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Return failed.');
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <Dialog isOpen={isOpen} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className={`p-5 gap-3 ${dialogMaxWidthClass} w-full self-center mx-4 max-h-[90%]`}>
          <Dialog.Title>Returns & Refunds</Dialog.Title>

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

          {!selectedTx ? (
            <Animated.View
              key="return-step-1"
              entering={enterSlideRight}
              exiting={exitSlideLeft}
              className="shrink"
            >
              <ScrollView
                keyboardShouldPersistTaps="handled"
                showsVerticalScrollIndicator={false}
                contentContainerClassName="gap-2 pb-1"
              >
                {recentTxs.length === 0 ? (
                  <Text className="text-muted text-xs text-center py-4">No recent sales found.</Text>
                ) : (
                  recentTxs.map((tx) => (
                    <TactilePressable
                      key={tx.id}
                      feedbackVariant="stretch"
                      className="w-full"
                      accessibilityLabel={`Select receipt #${tx.id}, ${currencySymbol}${tx.totalAmount.toFixed(2)}`}
                      onPress={() => void handleSelectTx(tx)}
                    >
                      <Surface
                        variant="secondary"
                        className="rounded-xl p-3 flex-row items-center justify-between gap-2"
                      >
                        <View className="flex-1 min-w-0">
                          <Text className="text-foreground text-sm font-semibold" numberOfLines={1}>
                            Receipt #{tx.id} • {currencySymbol}
                            {tx.totalAmount.toFixed(2)}
                          </Text>
                          <Text className="text-muted text-xs" numberOfLines={1}>
                            {tx.paymentMethod} • {tx.status}
                          </Text>
                        </View>
                        <View pointerEvents="none">
                          <Button variant="secondary" size="sm">
                            <Button.Label>Select</Button.Label>
                          </Button>
                        </View>
                      </Surface>
                    </TactilePressable>
                  ))
                )}
              </ScrollView>
            </Animated.View>
          ) : (
            <Animated.View
              key="return-step-2"
              entering={enterSlideRight}
              exiting={exitSlideLeft}
              className="shrink"
            >
              <ScrollView
                keyboardShouldPersistTaps="handled"
                showsVerticalScrollIndicator={false}
                contentContainerClassName="gap-3 pb-1"
              >
                <Text className="text-foreground text-sm font-semibold">
                  Refunding Receipt #{selectedTx.id} ({txItems.length} item(s))
                </Text>
                <View className="flex-row flex-wrap gap-1.5">
                  {REASONS.map((r) => (
                    <Chip
                      key={r}
                      size="sm"
                      color={reason === r ? 'accent' : 'default'}
                      variant={reason === r ? 'primary' : 'soft'}
                      onPress={() => setReason(r)}
                    >
                      <Chip.Label>{r}</Chip.Label>
                    </Chip>
                  ))}
                </View>
                <View className="flex-row items-center justify-between">
                  <Text className="text-foreground text-sm">Restock items to inventory</Text>
                  <Switch isSelected={restock} onSelectedChange={setRestock} />
                </View>
              </ScrollView>
            </Animated.View>
          )}

          <Separator />

          {!selectedTx ? (
            <Button variant="ghost" onPress={() => onOpenChange(false)}>
              <Button.Label>Close</Button.Label>
            </Button>
          ) : (
            <View className="flex-row justify-end gap-2">
              <Button variant="secondary" onPress={() => setSelectedTx(null)} isDisabled={isProcessing}>
                <Button.Label>Back</Button.Label>
              </Button>
              <Button variant="danger" isDisabled={isProcessing} onPress={handleRefundAll}>
                {isProcessing ? <Spinner size="sm" /> : null}
                <Button.Label>{isProcessing ? 'Refunding...' : 'Confirm Refund'}</Button.Label>
              </Button>
            </View>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
