import React from 'react';
import { View } from 'react-native';
import { Button, Dialog } from 'heroui-native';
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
        <Dialog.Content className="p-5 gap-3 max-w-md w-full self-center">
          <Dialog.Title>{product?.name ?? 'Select Unit'}</Dialog.Title>
          <Dialog.Description>Choose packaging unit / UOM multiplier:</Dialog.Description>
          <View className="gap-2 pt-1">
            {product
              ? options.map((opt) => (
                  <Button
                    key={`${opt.name}-${opt.multiplier}`}
                    variant="secondary"
                    className="w-full justify-between"
                    onPress={() => {
                      onSelect(product, opt);
                      onClose();
                    }}
                  >
                    <Button.Label>
                      {opt.name} (×{opt.multiplier}) — {currencySymbol}
                      {opt.price.toFixed(2)}
                    </Button.Label>
                  </Button>
                ))
              : null}
          </View>
          <Button variant="ghost" onPress={onClose}>
            <Button.Label>Cancel</Button.Label>
          </Button>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
