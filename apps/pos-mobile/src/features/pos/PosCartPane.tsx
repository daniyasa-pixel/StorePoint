import React from 'react';
import { FlatList, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Button, Card, Checkbox, Chip, Separator, Surface } from 'heroui-native';
import { computeCheckoutTotals } from '@/domain/posCheckoutCoordinator';
import { usePosCartStore } from '@/store/posCartStore';
import { enterSlideRight, exitSlideLeft, layoutTransition } from '@/theme/motion';

interface PosCartPaneProps {
  readonly currencySymbol: string;
  readonly vatRate: number;
  readonly onOpenPayment: () => void;
  readonly onOpenParked: () => void;
}

export function PosCartPane({
  currencySymbol,
  vatRate,
  onOpenPayment,
  onOpenParked,
}: PosCartPaneProps) {
  const lines = usePosCartStore((s) => s.lines);
  const orderDiscountType = usePosCartStore((s) => s.orderDiscountType);
  const orderDiscountValue = usePosCartStore((s) => s.orderDiscountValue);
  const updateQuantity = usePosCartStore((s) => s.updateQuantity);
  const toggleChecked = usePosCartStore((s) => s.toggleChecked);
  const clearCart = usePosCartStore((s) => s.clearCart);

  const activeLines = lines
    .filter((l) => l.checked)
    .map((l) => ({
      ...l,
      selectedPrice: Math.max(0, l.selectedPrice - l.itemDiscount),
    }));

  const totals = computeCheckoutTotals(
    activeLines,
    orderDiscountType,
    orderDiscountValue,
    true,
    vatRate,
  );

  return (
    <Card className="flex-1 p-3 gap-3">
      <View className="flex-row items-center justify-between">
        <View className="flex-row items-center gap-2">
          <Text className="text-foreground text-base font-bold">Active Cart</Text>
          <Chip size="sm" color="accent" variant="soft">
            <Chip.Label>{activeLines.length} selected</Chip.Label>
          </Chip>
        </View>
        <View className="flex-row gap-2">
          <Button variant="secondary" size="sm" onPress={onOpenParked}>
            <Button.Label>Hold</Button.Label>
          </Button>
          {lines.length > 0 ? (
            <Button variant="danger-soft" size="sm" onPress={clearCart}>
              <Button.Label>Clear</Button.Label>
            </Button>
          ) : null}
        </View>
      </View>

      <Separator />

      <FlatList
        className="flex-1"
        data={lines}
        keyExtractor={(item) => `${item.product.id}-${item.uomName}`}
        contentContainerClassName="gap-2 pb-2"
        ListEmptyComponent={
          <View className="py-10 items-center">
            <Text className="text-muted text-sm">Tap any product or scan a barcode to start</Text>
          </View>
        }
        renderItem={({ item }) => {
          const unitNet = Math.max(0, item.selectedPrice - item.itemDiscount);
          const lineTotal = unitNet * item.quantity;
          return (
            <Animated.View
              entering={enterSlideRight}
              exiting={exitSlideLeft}
              layout={layoutTransition}
            >
              <Surface variant="secondary" className="rounded-xl p-2.5 flex-row items-center gap-2">
                <Checkbox
                  isSelected={item.checked}
                  onSelectedChange={() => toggleChecked(item.product.id, item.uomName)}
                />
                <View className="flex-1 min-w-0">
                  <Text className="text-foreground text-sm font-semibold" numberOfLines={1}>
                    {item.product.name}
                  </Text>
                  <Text className="text-muted text-xs" numberOfLines={1}>
                    {item.uomName} • {currencySymbol}
                    {unitNet.toFixed(2)}
                  </Text>
                </View>
                <View className="flex-row items-center gap-1.5">
                  <Button
                    variant="tertiary"
                    size="sm"
                    onPress={() => updateQuantity(item.product.id, item.uomName, -1)}
                  >
                    <Button.Label>−</Button.Label>
                  </Button>
                  <Text className="text-foreground text-sm font-bold min-w-6 text-center">
                    {item.quantity}
                  </Text>
                  <Button
                    variant="tertiary"
                    size="sm"
                    onPress={() => updateQuantity(item.product.id, item.uomName, 1)}
                  >
                    <Button.Label>+</Button.Label>
                  </Button>
                </View>
                <Text className="text-foreground text-sm font-bold min-w-16 text-right">
                  {currencySymbol}
                  {lineTotal.toFixed(2)}
                </Text>
              </Surface>
            </Animated.View>
          );
        }}
      />

      <Separator />

      <View className="gap-1">
        <View className="flex-row justify-between">
          <Text className="text-muted text-xs">Subtotal</Text>
          <Text className="text-foreground text-xs font-medium">
            {currencySymbol}
            {totals.subtotal.toFixed(2)}
          </Text>
        </View>
        <View className="flex-row justify-between">
          <Text className="text-muted text-xs">VAT ({vatRate}%)</Text>
          <Text className="text-foreground text-xs font-medium">
            {currencySymbol}
            {totals.vatAmount.toFixed(2)}
          </Text>
        </View>
        <View className="flex-row justify-between pt-1">
          <Text className="text-foreground text-base font-bold">Total Due</Text>
          <Text className="text-accent text-lg font-bold">
            {currencySymbol}
            {totals.netTotal.toFixed(2)}
          </Text>
        </View>
      </View>

      <Button
        variant="primary"
        size="lg"
        className="w-full"
        isDisabled={activeLines.length === 0}
        onPress={onOpenPayment}
      >
        <Button.Label>
          Charge {currencySymbol}
          {totals.netTotal.toFixed(2)}
        </Button.Label>
      </Button>
    </Card>
  );
}
