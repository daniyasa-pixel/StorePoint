import React, { useCallback, useEffect, useState } from 'react';
import { FlatList, Text, View } from 'react-native';
import { Button, Input, Surface } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { Product } from '@/db/types';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

export function InventoryStocktakeTab() {
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const [products, setProducts] = useState<Product[]>([]);
  const [counts, setCounts] = useState<Record<number, string>>({});

  const load = useCallback(async () => {
    const repos = await getRepositories();
    setProducts(await repos.catalog.getProducts(false));
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleApplyVariance = async (product: Product) => {
    const raw = counts[product.id];
    if (raw === undefined || raw.trim() === '') return;
    const physical = Number.parseInt(raw, 10);
    if (!Number.isFinite(physical) || physical < 0) return;

    const delta = physical - product.stockCount;
    if (delta === 0) return;

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
  };

  return (
    <FlatList
      data={products}
      keyExtractor={(p) => String(p.id)}
      contentContainerClassName="gap-2 pb-6"
      ListEmptyComponent={
        <Text className="text-muted text-xs text-center py-6">No products in catalog.</Text>
      }
      renderItem={({ item }) => (
        <Surface variant="default" className="rounded-xl p-3 border border-border flex-row items-center gap-2">
          <View className="flex-1">
            <Text className="text-foreground text-sm font-semibold">{item.name}</Text>
            <Text className="text-muted text-xs">
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
          <Button variant="secondary" size="sm" onPress={() => void handleApplyVariance(item)}>
            <Button.Label>Apply</Button.Label>
          </Button>
        </Surface>
      )}
    />
  );
}
