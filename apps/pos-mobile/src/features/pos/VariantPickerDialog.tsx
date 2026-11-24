import React from 'react';
import { ScrollView, Text, View } from 'react-native';
import { Button, Dialog, Separator } from 'heroui-native';
import type { Product, UomOption } from '@/db/types';

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
  return (
    <Dialog isOpen={product !== null} onOpenChange={(open) => (!open ? onClose() : undefined)}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className="p-5 gap-3 max-w-md w-full self-center mx-4 max-h-[90%]">
          <Dialog.Title>{product?.name ?? 'Select Unit'}</Dialog.Title>
          <Dialog.Description>Choose packaging unit / UOM multiplier:</Dialog.Description>
          <ScrollView
            keyboardShouldPersistTaps="handled"
            showsVerticalScrollIndicator={false}
            contentContainerClassName="gap-2 py-1"
          >
            {product
              ? options.map((opt) => (
                  <Button
                    key={`${opt.name}-${opt.multiplier}`}
                    variant="secondary"
                    className="w-full"
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
                  </Button>
                ))
              : null}
          </ScrollView>
          <Separator />
          <Button variant="ghost" onPress={onClose}>
            <Button.Label>Cancel</Button.Label>
          </Button>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
