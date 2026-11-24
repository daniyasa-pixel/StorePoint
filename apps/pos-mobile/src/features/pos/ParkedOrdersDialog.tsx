import React, { useCallback, useEffect, useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import { Button, Dialog, Input, Label, Separator, Surface, TextField } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { ParkedTransaction, ParkedTransactionItem, Product } from '@/db/types';
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
  const lines = usePosCartStore((s) => s.lines);
  const clearCart = usePosCartStore((s) => s.clearCart);
  const addProduct = usePosCartStore((s) => s.addProduct);

  const [note, setNote] = useState('');
  const [parked, setParked] = useState<
    Array<{ readonly header: ParkedTransaction; readonly items: readonly ParkedTransactionItem[] }>
  >([]);

  const loadParked = useCallback(async () => {
    const repos = await getRepositories();
    setParked(await repos.checkout.getParkedTransactions());
  }, []);

  useEffect(() => {
    if (isOpen) {
      void loadParked();
    }
  }, [isOpen, loadParked]);

  const handleParkCurrent = async () => {
    if (lines.length === 0) return;
    const repos = await getRepositories();
    await repos.checkout.parkCart(note || 'Held Order', lines);
    clearCart();
    setNote('');
    await loadParked();
  };

  const handleResume = async (
    entry: { readonly header: ParkedTransaction; readonly items: readonly ParkedTransactionItem[] },
  ) => {
    const byId = new Map(products.map((p) => [p.id, p]));
    clearCart();
    for (const item of entry.items) {
      const prod = byId.get(item.productId);
      if (prod) {
        addProduct(prod, { name: prod.baseUom || 'pc', multiplier: 1, price: item.price }, item.quantity);
      }
    }
    const repos = await getRepositories();
    await repos.checkout.deleteParkedTransaction(entry.header.id);
    onOpenChange(false);
  };

  return (
    <Dialog isOpen={isOpen} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className="p-5 gap-3 max-w-md w-full self-center mx-4 max-h-[90%]">
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
              {lines.length > 0 ? (
                <View className="gap-2">
                  <TextField>
                    <Label>Hold Note</Label>
                    <Input value={note} onChangeText={setNote} placeholder="e.g. Table 3 / Customer getting cash" />
                  </TextField>
                  <Button variant="primary" onPress={handleParkCurrent}>
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
                      <Button variant="secondary" size="sm" onPress={() => handleResume(entry)}>
                        <Button.Label>Resume</Button.Label>
                      </Button>
                    </Surface>
                  ))
                )}
              </View>
            </ScrollView>
          </KeyboardAvoidingView>

          <Separator />

          <Button variant="ghost" onPress={() => onOpenChange(false)}>
            <Button.Label>Close</Button.Label>
          </Button>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
