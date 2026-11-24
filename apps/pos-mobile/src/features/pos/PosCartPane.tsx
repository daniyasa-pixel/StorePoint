import React, { useCallback, useEffect, useMemo } from 'react';
import { Alert, FlatList, Text, View } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSequence,
  withSpring,
} from 'react-native-reanimated';
import { Button, Card, Checkbox, Chip, Separator, Surface } from 'heroui-native';
import { TactileButton } from '@/components/TactilePressable';
import { formatMoney } from '@/domain/money';
import { computeCheckoutTotals } from '@/domain/posCheckoutCoordinator';
import { type PosCartLine, usePosCartStore } from '@/store/posCartStore';
import { enterSlideRight, exitSlideLeft, layoutTransition, MOTION_SPRINGS } from '@/theme/motion';

interface PosCartPaneProps {
  readonly currencySymbol: string;
  readonly vatRate: number;
  readonly onOpenPayment: () => void;
  readonly onOpenParked: () => void;
}

const cartKeyExtractor = (item: PosCartLine) => `${item.product.id}-${item.uomName}`;

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

  const activeLines = useMemo(
    () =>
      lines
        .filter((l) => l.checked)
        .map((l) => ({
          ...l,
          selectedPrice: Math.max(0, l.selectedPrice - l.itemDiscount),
        })),
    [lines],
  );

  const totals = useMemo(
    () =>
      computeCheckoutTotals(
        activeLines,
        orderDiscountType,
        orderDiscountValue,
        true,
        vatRate,
      ),
    [activeLines, orderDiscountType, orderDiscountValue, vatRate],
  );

  const totalScale = useSharedValue(1);
  useEffect(() => {
    totalScale.value = withSequence(
      withSpring(1.08, MOTION_SPRINGS.tactile),
      withSpring(1, MOTION_SPRINGS.bouncy),
    );
  }, [totals.netTotal, totalScale]);

  const totalAnimatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: totalScale.value }],
  }));

  const renderCartItem = useCallback(
    ({ item }: { item: PosCartLine }) => {
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
              accessibilityLabel={`Select ${item.product.name}`}
              onSelectedChange={() => toggleChecked(item.product.id, item.uomName)}
            />
            <View className="flex-1 min-w-0">
              <Text className="text-foreground text-sm font-semibold" numberOfLines={1}>
                {item.product.name}
              </Text>
              <Text className="text-muted text-xs" numberOfLines={1}>
                {item.uomName} • {formatMoney(unitNet, currencySymbol)}
              </Text>
            </View>
            <View className="flex-row items-center gap-1.5">
              <TactileButton
                feedbackVariant="spring"
                variant="tertiary"
                size="sm"
                accessibilityLabel={`Decrease quantity of ${item.product.name}`}
                onPress={() => updateQuantity(item.product.id, item.uomName, -1)}
              >
                <Button.Label>−</Button.Label>
              </TactileButton>
              <Text className="text-foreground text-sm font-bold min-w-6 text-center">
                {item.quantity}
              </Text>
              <TactileButton
                feedbackVariant="spring"
                variant="tertiary"
                size="sm"
                accessibilityLabel={`Increase quantity of ${item.product.name}`}
                onPress={() => updateQuantity(item.product.id, item.uomName, 1)}
              >
                <Button.Label>+</Button.Label>
              </TactileButton>
            </View>
            <Text className="text-foreground text-sm font-bold min-w-16 text-right">
              {formatMoney(lineTotal, currencySymbol)}
            </Text>
          </Surface>
        </Animated.View>
      );
    },
    [currencySymbol, toggleChecked, updateQuantity],
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
            <Button
              variant="danger-soft"
              size="sm"
              onPress={() =>
                Alert.alert('Clear Cart', 'Remove all items from the current cart?', [
                  { text: 'Cancel', style: 'cancel' },
                  { text: 'Clear', style: 'destructive', onPress: clearCart },
                ])
              }
            >
              <Button.Label>Clear</Button.Label>
            </Button>
          ) : null}
        </View>
      </View>

      <Separator />

      <FlatList
        className="flex-1"
        data={lines}
        keyExtractor={cartKeyExtractor}
        initialNumToRender={12}
        maxToRenderPerBatch={10}
        windowSize={7}
        removeClippedSubviews
        contentContainerClassName="gap-2 pb-2"
        ListEmptyComponent={
          <View className="py-10 items-center">
            <Text className="text-muted text-sm">Tap any product or scan a barcode to start</Text>
          </View>
        }
        renderItem={renderCartItem}
      />

      <Separator />

      <View className="gap-1">
        <View className="flex-row justify-between">
          <Text className="text-muted text-xs">Subtotal</Text>
          <Text className="text-foreground text-xs font-medium">
            {formatMoney(totals.subtotal, currencySymbol)}
          </Text>
        </View>
        <View className="flex-row justify-between">
          <Text className="text-muted text-xs">VAT ({vatRate}%)</Text>
          <Text className="text-foreground text-xs font-medium">
            {formatMoney(totals.vatAmount, currencySymbol)}
          </Text>
        </View>
        <View className="flex-row justify-between pt-1">
          <Text className="text-foreground text-base font-bold">Total Due</Text>
          <Animated.Text
            className="text-accent text-lg font-bold"
            style={totalAnimatedStyle}
          >
            {formatMoney(totals.netTotal, currencySymbol)}
          </Animated.Text>
        </View>
      </View>

      <TactileButton
        feedbackVariant="stretch"
        variant="primary"
        size="lg"
        fullWidth
        isDisabled={activeLines.length === 0}
        accessibilityLabel={`Charge ${formatMoney(totals.netTotal, currencySymbol)}`}
        onPress={onOpenPayment}
      >
        <Button.Label>Charge {formatMoney(totals.netTotal, currencySymbol)}</Button.Label>
      </TactileButton>
    </Card>
  );
}
