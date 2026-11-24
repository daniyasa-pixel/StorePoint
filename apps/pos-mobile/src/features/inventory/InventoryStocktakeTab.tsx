import React, { useCallback, useEffect, useState } from 'react';
import { FlatList, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Alert, Button, Input, Surface, useToast } from 'heroui-native';
import { TactileButton } from '@/components/TactilePressable';
import { getRepositories } from '@/db/repositories';
import type { Product } from '@/db/types';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { layoutTransition, staggerFadeUp } from '@/theme/motion';

export function InventoryStocktakeTab() {
  const { toast } = useToast();
  const { containerPaddingClass } = useWindowLayout();
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const [products, setProducts] = useState<Product[]>([]);
  const [counts, setCounts] = useState<Record<number, string>>({});
  const [applyingId, setApplyingId] = useState<number | null>(null);
  const [feedback, setFeedback] = useState<{ status: 'danger' | 'accent'; message: string } | null>(null);

  const load = useCallback(async () => {
    const repos = await getRepositories();
    setProducts(await repos.catalog.getProducts(false));
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleApplyVariance = async (product: Product) => {
    if (applyingId !== null) return;
    setFeedback(null);
    const raw = counts[product.id];
    if (raw === undefined || raw.trim() === '') {
      setFeedback({ status: 'danger', message: `Enter a physical count for ${product.name}.` });
      return;
    }
    const physical = Number(raw.trim());
    if (!Number.isInteger(physical) || physical < 0) {
      setFeedback({ status: 'danger', message: `Invalid count for ${product.name}. Enter a non-negative whole number.` });
      return;
    }

    const delta = physical - product.stockCount;
    if (delta === 0) {
      setFeedback({ status: 'accent', message: `No variance for ${product.name} — physical count matches system stock (${product.stockCount}).` });
      return;
    }

    setApplyingId(product.id);
    try {
      const repos = await getRepositories();
      await repos.catalog.adjustStock({
        productId: product.id,
        delta,
        actorUsername: activeUser?.username ?? 'inventory',
        movementType: 'STOCKTAKE_AUDIT',
        notes: `Physical count reconciled to ${physical}`,
      });
      setCounts((prev) => {
        const next = { ...prev };
        delete next[product.id];
        return next;
      });
      await load();
      toast.show({
        label: 'Stocktake applied',
        description: `${product.name} reconciled to ${physical} ${product.baseUom} (${delta > 0 ? `+${delta}` : delta})`,
        variant: 'success',
      });
    } catch (err) {
      setFeedback({
        status: 'danger',
        message: err instanceof Error ? err.message : `Failed to apply variance for ${product.name}.`,
      });
    } finally {
      setApplyingId(null);
    }
  };

  return (
    <View className={`flex-1 gap-3 ${containerPaddingClass}`}>
      {feedback ? (
        <Alert status={feedback.status}>
          <Alert.Indicator />
          <Alert.Content>
            <Alert.Description>{feedback.message}</Alert.Description>
          </Alert.Content>
        </Alert>
      ) : null}
      <FlatList
        data={products}
        keyExtractor={(p) => String(p.id)}
        keyboardShouldPersistTaps="handled"
        contentContainerClassName="gap-2 pb-6"
        ListEmptyComponent={
          <Text className="text-muted text-xs text-center py-6">No products in catalog.</Text>
        }
        renderItem={({ item, index }) => (
          <Animated.View entering={staggerFadeUp(index)} layout={layoutTransition}>
            <Surface variant="default" className="rounded-xl p-3 border border-border flex-row items-center gap-2">
              <View className="flex-1 min-w-0">
                <Text numberOfLines={2} className="text-foreground text-sm font-semibold">{item.name}</Text>
                <Text numberOfLines={1} className="text-muted text-xs">
                  System: {item.stockCount} {item.baseUom}
                </Text>
              </View>
              <View className="w-20">
                <Input
                  value={counts[item.id] ?? ''}
                  onChangeText={(v) => setCounts((prev) => ({ ...prev, [item.id]: v }))}
                  keyboardType="number-pad"
                  placeholder={String(item.stockCount)}
                />
              </View>
              <TactileButton
                feedbackVariant="stretch"
                variant="secondary"
                size="sm"
                isDisabled={applyingId !== null}
                onPress={() => void handleApplyVariance(item)}
              >
                <Button.Label>{applyingId === item.id ? '...' : 'Apply'}</Button.Label>
              </TactileButton>
            </Surface>
          </Animated.View>
        )}
      />
    </View>
  );
}
