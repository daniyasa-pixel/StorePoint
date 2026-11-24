import React, { useCallback, useEffect, useState } from 'react';
import { Alert as RNAlert, KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import { Alert, Button, Dialog, Input, Label, Separator, Surface, TextField, useToast } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { ParkedTransaction, ParkedTransactionItem, Product } from '@/db/types';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { usePosCartStore } from '@/store/posCartStore';

interface ParkedOrdersDialogProps {
  readonly isOpen: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly products: readonly Product[];
}

export function ParkedOrdersDialog({
  isOpen,
  onOpenChange,
  products,
}: ParkedOrdersDialogProps) {
  const { toast } = useToast();
  const { dialogMaxWidthClass } = useWindowLayout();
  const lines = usePosCartStore((s) => s.lines);
  const clearCart = usePosCartStore((s) => s.clearCart);
  const addProduct = usePosCartStore((s) => s.addProduct);

  const [note, setNote] = useState('');
  const [isBusy, setIsBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [parked, setParked] = useState<
    Array<{ readonly header: ParkedTransaction; readonly items: readonly ParkedTransactionItem[] }>
  >([]);

  const loadParked = useCallback(async () => {
    const repos = await getRepositories();
    setParked(await repos.checkout.getParkedTransactions());
  }, []);

  useEffect(() => {
    if (!isOpen) return;
    let active = true;
    setError(null);
    void getRepositories()
      .then((repos) => repos.checkout.getParkedTransactions())
      .then((list) => {
        if (active) setParked(list);
      })
      .catch(() => {
        // Ignore load errors if dialog unmounts or DB query fails
      });
    return () => {
      active = false;
    };
  }, [isOpen]);

  const handleParkCurrent = async () => {
    if (lines.length === 0 || isBusy) return;
    setError(null);
    setIsBusy(true);
    try {
      const repos = await getRepositories();
      const label = note.trim() || 'Held Order';
      await repos.checkout.parkCart(label, lines);
      clearCart();
      setNote('');
      await loadParked();
      toast.show({
        label: 'Order parked',
        description: label,
        variant: 'success',
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to park current order.');
    } finally {
      setIsBusy(false);
    }
  };

  const executeResume = async (
    entry: { readonly header: ParkedTransaction; readonly items: readonly ParkedTransactionItem[] },
  ) => {
    if (isBusy) return;
    setError(null);
    setIsBusy(true);
    try {
      const repos = await getRepositories();
      await repos.checkout.deleteParkedTransaction(entry.header.id);
      const byId = new Map(products.map((p) => [p.id, p]));
      clearCart();
      for (const item of entry.items) {
        const prod = byId.get(item.productId);
        if (prod) {
          addProduct(prod, { name: prod.baseUom || 'pc', multiplier: 1, price: item.price }, item.quantity);
        }
      }
      toast.show({
        label: 'Order resumed',
        description: entry.header.note,
        variant: 'accent',
      });
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to resume parked order.');
    } finally {
      setIsBusy(false);
    }
  };

  const handleResume = (
    entry: { readonly header: ParkedTransaction; readonly items: readonly ParkedTransactionItem[] },
  ) => {
    if (isBusy) return;
    if (lines.length > 0) {
      RNAlert.alert(
        'Resume Parked Order',
        'Resuming this parked order will replace the items currently in your cart. Continue?',
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Resume', style: 'destructive', onPress: () => void executeResume(entry) },
        ],
      );
      return;
    }
    void executeResume(entry);
  };

  return (
    <Dialog isOpen={isOpen} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className={`p-5 gap-3 ${dialogMaxWidthClass} w-full self-center mx-4 max-h-[90%]`}>
          <Dialog.Title>Hold / Parked Orders</Dialog.Title>

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
                <Alert status="danger">
                  <Alert.Indicator />
                  <Alert.Content>
                    <Alert.Description>{error}</Alert.Description>
                  </Alert.Content>
                </Alert>
              ) : null}
              {lines.length > 0 ? (
                <View className="gap-2">
                  <TextField>
                    <Label>Hold Note</Label>
                    <Input value={note} onChangeText={setNote} placeholder="e.g. Table 3 / Customer getting cash" />
                  </TextField>
                  <Button variant="primary" isDisabled={isBusy} onPress={() => void handleParkCurrent()}>
                    <Button.Label>Hold Current Cart ({lines.length} items)</Button.Label>
                  </Button>
                </View>
              ) : null}

              <Separator />

              <View className="gap-2">
                {parked.length === 0 ? (
                  <Text className="text-muted text-xs text-center py-3">No parked orders.</Text>
                ) : (
                  parked.map((entry) => (
                    <Surface
                      key={entry.header.id}
                      variant="secondary"
                      className="rounded-xl p-3 flex-row items-center justify-between gap-2"
                    >
                      <View className="flex-1 min-w-0">
                        <Text className="text-foreground text-sm font-semibold" numberOfLines={1}>
                          {entry.header.note}
                        </Text>
                        <Text className="text-muted text-xs">{entry.items.length} line item(s)</Text>
                      </View>
                      <Button variant="secondary" size="sm" isDisabled={isBusy} onPress={() => void handleResume(entry)}>
                        <Button.Label>Resume</Button.Label>
                      </Button>
                    </Surface>
                  ))
                )}
              </View>
            </ScrollView>
          </KeyboardAvoidingView>

          <Separator />

          <Button variant="ghost" isDisabled={isBusy} onPress={() => onOpenChange(false)}>
            <Button.Label>Close</Button.Label>
          </Button>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
