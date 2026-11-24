import React from 'react';
import { ScrollView, Text, View } from 'react-native';
import { Button, Dialog, Separator } from 'heroui-native';
import type { CheckoutReceipt } from '@/db/repositories/checkoutRepository';
import { formatMoney } from '@/domain/money';
import { useWindowLayout } from '@/layout/useWindowLayout';

interface ReceiptPreviewDialogProps {
  readonly receipt: CheckoutReceipt | null;
  readonly storeName: string;
  readonly currencySymbol: string;
  readonly onClose: () => void;
  readonly onPrint?: (receipt: CheckoutReceipt) => void;
}

export function ReceiptPreviewDialog({
  receipt,
  storeName,
  currencySymbol,
  onClose,
  onPrint,
}: ReceiptPreviewDialogProps) {
  const { dialogMaxWidthClass } = useWindowLayout();

  return (
    <Dialog isOpen={receipt !== null} onOpenChange={(open) => (!open ? onClose() : undefined)}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className={`p-5 gap-3 ${dialogMaxWidthClass} w-full self-center mx-4 max-h-[90%]`}>
          <Dialog.Title>Receipt #{receipt?.transaction.id}</Dialog.Title>
          <Dialog.Description>
            {storeName} • {receipt?.transaction.paymentMethod}
          </Dialog.Description>

          <Separator />

          <ScrollView
            keyboardShouldPersistTaps="handled"
            showsVerticalScrollIndicator={false}
            contentContainerClassName="gap-3 pb-1"
          >
            <View className="gap-1.5">
              {receipt?.items.map((item) => (
                <View key={item.id} className="flex-row justify-between gap-2">
                  <Text className="text-foreground text-xs flex-1" numberOfLines={1}>
                    {item.quantity}× {item.productName} ({item.uomName})
                  </Text>
                  <Text className="text-foreground text-xs font-semibold">
                    {formatMoney(item.price * item.quantity, currencySymbol)}
                  </Text>
                </View>
              ))}
            </View>

            <Separator />

            {receipt ? (
              <View className="gap-1">
                <View className="flex-row justify-between">
                  <Text className="text-foreground text-sm font-bold">Total</Text>
                  <Text className="text-foreground text-sm font-bold">
                    {formatMoney(receipt.transaction.totalAmount, currencySymbol)}
                  </Text>
                </View>
                <View className="flex-row justify-between">
                  <Text className="text-muted text-xs">Paid</Text>
                  <Text className="text-muted text-xs">
                    {formatMoney(receipt.transaction.cashPaid, currencySymbol)}
                  </Text>
                </View>
                <View className="flex-row justify-between">
                  <Text className="text-success text-xs font-semibold">Change</Text>
                  <Text className="text-success text-xs font-semibold">
                    {formatMoney(receipt.transaction.changeAmount, currencySymbol)}
                  </Text>
                </View>
              </View>
            ) : null}
          </ScrollView>

          <Separator />

          <View className="flex-row justify-end gap-2">
            {receipt && onPrint ? (
              <Button variant="secondary" onPress={() => onPrint(receipt)}>
                <Button.Label>Print Receipt</Button.Label>
              </Button>
            ) : null}
            <Button variant="primary" onPress={onClose}>
              <Button.Label>Done</Button.Label>
            </Button>
          </View>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
