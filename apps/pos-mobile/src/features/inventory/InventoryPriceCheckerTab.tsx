import React, { useEffect, useMemo, useState } from 'react';
import { FlatList, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Card, Chip, SearchField, Surface } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { Product } from '@/db/types';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { layoutTransition, staggerFadeUp } from '@/theme/motion';

export function InventoryPriceCheckerTab() {
  const currency = useAppBootstrapStore((s) => s.currencySymbol);
  const [products, setProducts] = useState<Product[]>([]);
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');

  useEffect(() => {
    void getRepositories()
      .then((r) => r.catalog.getProducts(false))
      .then(setProducts);
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 150);
    return () => clearTimeout(timer);
  }, [query]);

  const filtered = useMemo(() => {
    const q = debouncedQuery.trim().toLowerCase();
    if (!q) return products;
    return products.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        p.barcode.toLowerCase().includes(q),
    );
  }, [products, debouncedQuery]);

  return (
    <View className="flex-1 gap-3">
      <SearchField value={query} onChange={setQuery}>
        <SearchField.Group>
          <SearchField.SearchIcon />
          <SearchField.Input placeholder="Scan or type barcode / product name..." />
          <SearchField.ClearButton />
        </SearchField.Group>
      </SearchField>

      <FlatList
        data={filtered}
        keyExtractor={(p) => String(p.id)}
        initialNumToRender={12}
        maxToRenderPerBatch={10}
        windowSize={7}
        removeClippedSubviews
        contentContainerClassName="gap-2 pb-6"
        ListEmptyComponent={
          <Card className="p-6 items-center">
            <Card.Title>No matching item</Card.Title>
          </Card>
        }
        renderItem={({ item, index }) => (
          <Animated.View entering={staggerFadeUp(index)} layout={layoutTransition}>
            <Surface variant="default" className="rounded-xl p-4 border border-border flex-row justify-between items-center gap-2">
              <View className="flex-1 min-w-0 pr-3">
                <Text numberOfLines={2} className="text-foreground text-base font-bold">{item.name}</Text>
                <Text numberOfLines={1} className="text-muted text-xs">
                  Barcode: {item.barcode || 'None'} • Stock: {item.stockCount} {item.baseUom}
                </Text>
              </View>
              <Chip color="accent" variant="primary" size="md">
                <Chip.Label>
                  {currency}
                  {item.price.toFixed(2)}
                </Chip.Label>
              </Chip>
            </Surface>
          </Animated.View>
        )}
      />
    </View>
  );
}
