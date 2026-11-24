import React, { useEffect, useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Alert, Button, Chip, Dialog, Input, Label, Separator, Spinner, TextField } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { Product } from '@/db/types';
import { analyzeCostAndMargin, computeDerivedUnitCost } from '@/domain/productionBomCoordinator';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeUp, exitFadeUp } from '@/theme/motion';

interface BulkProductionDialogProps {
  readonly isOpen: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly onCompleted?: () => void;
}

export function BulkProductionDialog({
  isOpen,
  onOpenChange,
  onCompleted,
}: BulkProductionDialogProps) {
  const currency = useAppBootstrapStore((s) => s.currencySymbol);
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const [products, setProducts] = useState<Product[]>([]);
  const [rawId, setRawId] = useState<number | null>(null);
  const [targetId, setTargetId] = useState<number | null>(null);
  const [rawQty, setRawQty] = useState('1');
  const [yieldQty, setYieldQty] = useState('10');
  const [scrapPct, setScrapPct] = useState('0');
  const [error, setError] = useState<string | null>(null);
  const [isRunning, setIsRunning] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    void getRepositories()
      .then((r) => r.catalog.getProducts(false))
      .then((list) => {
        setProducts(list);
        if (list[0]) setRawId(list[0].id);
        if (list[1]) setTargetId(list[1].id);
      });
  }, [isOpen]);

  const rawProd = products.find((p) => p.id === rawId) ?? null;
  const targetProd = products.find((p) => p.id === targetId) ?? null;
  const derivedCost = rawProd
    ? computeDerivedUnitCost(
        rawProd.cost,
        Number.parseFloat(rawQty) || 0,
        Number.parseFloat(yieldQty) || 0,
        Number.parseFloat(scrapPct) || 0,
      )
    : 0;
  const margin = targetProd ? analyzeCostAndMargin(derivedCost, targetProd.price) : null;

  const handleRun = async () => {
    setError(null);
    if (!rawId || !targetId || rawId === targetId) {
      setError('Select distinct raw material and target retail products.');
      return;
    }
    setIsRunning(true);
    try {
      const repos = await getRepositories();
      await repos.production.executeProductionRun({
        rawMaterialProductId: rawId,
        targetRetailProductId: targetId,
        rawQtyUsed: Number.parseFloat(rawQty) || 0,
        producedQty: Number.parseFloat(yieldQty) || 0,
        wasteLossPercentage: Number.parseFloat(scrapPct) || 0,
        operatorUsername: activeUser?.username ?? 'admin',
      });
      onCompleted?.();
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Production run failed.');
    } finally {
      setIsRunning(false);
    }
  };

  return (
    <Dialog isOpen={isOpen} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className="p-5 gap-3 max-w-md w-full self-center mx-4 max-h-[90%]">
          <Dialog.Title>Bulk Repackaging & BOM Production</Dialog.Title>

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
                <Animated.View entering={enterFadeUp} exiting={exitFadeUp}>
                  <Alert status="danger">
                    <Alert.Indicator />
                    <Alert.Content>
                      <Alert.Description>{error}</Alert.Description>
                    </Alert.Content>
                  </Alert>
                </Animated.View>
              ) : null}

              <View className="gap-1.5">
                <Text className="text-foreground text-xs font-semibold">Raw Material</Text>
                <View className="flex-row flex-wrap gap-1.5">
                  {products.slice(0, 6).map((p) => (
                    <Chip
                      key={`raw-${p.id}`}
                      size="sm"
                      color={rawId === p.id ? 'accent' : 'default'}
                      variant={rawId === p.id ? 'primary' : 'soft'}
                      onPress={() => setRawId(p.id)}
                    >
                      <Chip.Label>{p.name} ({p.stockCount})</Chip.Label>
                    </Chip>
                  ))}
                </View>
              </View>

              <View className="gap-1.5">
                <Text className="text-foreground text-xs font-semibold">Target Retail Product</Text>
                <View className="flex-row flex-wrap gap-1.5">
                  {products.slice(0, 6).map((p) => (
                    <Chip
                      key={`target-${p.id}`}
                      size="sm"
                      color={targetId === p.id ? 'success' : 'default'}
                      variant={targetId === p.id ? 'primary' : 'soft'}
                      onPress={() => setTargetId(p.id)}
                    >
                      <Chip.Label>{p.name}</Chip.Label>
                    </Chip>
                  ))}
                </View>
              </View>

              <View className="flex-row gap-2">
                <TextField className="flex-1">
                  <Label>Raw Used</Label>
                  <Input value={rawQty} onChangeText={setRawQty} keyboardType="decimal-pad" />
                </TextField>
                <TextField className="flex-1">
                  <Label>Yield Qty</Label>
                  <Input value={yieldQty} onChangeText={setYieldQty} keyboardType="decimal-pad" />
                </TextField>
                <TextField className="flex-1">
                  <Label>Waste %</Label>
                  <Input value={scrapPct} onChangeText={setScrapPct} keyboardType="decimal-pad" />
                </TextField>
              </View>

              {margin ? (
                <Text className="text-muted text-xs">
                  Derived Cost: {currency}
                  {margin.derivedUnitCost.toFixed(2)} • Margin: {margin.marginPercentage.toFixed(1)}%
                </Text>
              ) : null}
            </ScrollView>
          </KeyboardAvoidingView>

          <Separator />

          <View className="flex-row justify-end gap-2">
            <Button variant="secondary" onPress={() => onOpenChange(false)} isDisabled={isRunning}>
              <Button.Label>Cancel</Button.Label>
            </Button>
            <Button variant="primary" isDisabled={isRunning} onPress={() => void handleRun()}>
              {isRunning ? <Spinner size="sm" /> : null}
              <Button.Label>{isRunning ? 'Running...' : 'Execute Repack'}</Button.Label>
            </Button>
          </View>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  );
}
