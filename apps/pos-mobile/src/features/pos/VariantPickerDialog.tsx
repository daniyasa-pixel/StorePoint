import React from 'react';
import { ScrollView, Text, View } from 'react-native';
import { Button, Dialog, Separator } from 'heroui-native';
import { TactileButton } from '@/components/TactilePressable';
import type { Product, UomOption } from '@/db/types';
import { useWindowLayout } from '@/layout/useWindowLayout';

interface VariantPickerDialogProps {
  readonly product: Product | null;
  readonly options: readonly UomOption[];
  readonly currencySymbol: string;
  readonly onSelect: (product: Product, option: UomOption) => void;
  readonly onClose: () => void;
}

export function VariantPickerDialog({
  product,
  options,
  currencySymbol,
  onSelect,
  onClose,
}: VariantPickerDialogProps) {
  const { dialogMaxWidthClass } = useWindowLayout();

  return (
    <Dialog isOpen={product !== null} onOpenChange={(open) => (!open ? onClose() : undefined)}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className={`p-5 gap-3 ${dialogMaxWidthClass} w-full self-center mx-4 max-h-[90%]`}>
          <Dialog.Title>{product?.name ?? 'Select Unit'}</Dialog.Title>
          <Dialog.Description>Choose packaging unit / UOM multiplier:</Dialog.Description>
          <ScrollView
            keyboardShouldPersistTaps="handled"
            showsVerticalScrollIndicator={false}
            contentContainerClassName="gap-2 py-1"
          >
            {product
              ? options.map((opt) => (
                  <TactileButton
                    key={`${opt.name}-${opt.multiplier}`}
                    feedbackVariant="stretch"
                    variant="secondary"
                    fullWidth
                    accessibilityLabel={`${opt.name} times ${opt.multiplier}, ${currencySymbol}${opt.price.toFixed(2)}`}
                    onPress={() => {
                      onSelect(product, opt);
                      onClose();
                    }}
                  >
                    <View className="flex-1 flex-row items-center justify-between gap-2">
                      <Text className="text-foreground text-sm font-medium" numberOfLines={1}>
                        {opt.name} (×{opt.multiplier})
                      </Text>
                      <Text className="text-accent text-sm font-bold">
                        {currencySymbol}
                        {opt.price.toFixed(2)}
                      </Text>
                    </View>
                  </TactileButton>
                ))
              : null}
          </ScrollView>
          <Separator />
          <TactileButton variant="ghost" onPress={onClose}>
            <Button.Label>Cancel</Button.Label>
          </TactileButton>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
