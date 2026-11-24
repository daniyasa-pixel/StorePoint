import React, { useCallback } from 'react';
import { FlatList, ScrollView, Text, View } from 'react-native';
import { Button, Card, Chip, PressableFeedback, SearchField } from 'heroui-native';
import type { Category, Product } from '@/db/types';
import { resolveScanSearchCopy } from '@/layout/adaptiveLayout';
import { useWindowLayout } from '@/layout/useWindowLayout';

interface PosCatalogPaneProps {
  readonly categories: readonly Category[];
  readonly products: readonly Product[];
  readonly selectedCategoryId: number | null;
  readonly onSelectCategory: (id: number | null) => void;
  readonly searchQuery: string;
  readonly onSearchChange: (q: string) => void;
  readonly currencySymbol: string;
  readonly onSelectProduct: (product: Product) => void;
  readonly onOpenScanner: () => void;
}

const catalogKeyExtractor = (item: Product) => String(item.id);

export const PosCatalogPane = React.memo(function PosCatalogPane({
  categories,
  products,
  selectedCategoryId,
  onSelectCategory,
  searchQuery,
  onSearchChange,
  currencySymbol,
  onSelectProduct,
  onOpenScanner,
}: PosCatalogPaneProps) {
  const { width, isExpanded } = useWindowLayout();
  const searchCopy = resolveScanSearchCopy(width, false);
  const numColumns = isExpanded ? 3 : 2;

  const renderProduct = useCallback(
    ({ item }: { item: Product }) => {
      const lowStock = item.trackInventory && item.stockCount <= 5;
      const outOfStock = item.trackInventory && item.stockCount <= 0;
      return (
        <View className="flex-1">
          <PressableFeedback
            className="flex-1"
            isDisabled={outOfStock}
            accessibilityRole="button"
            accessibilityLabel={`${item.name}, ${currencySymbol}${item.price.toFixed(2)}, ${
              item.trackInventory ? `${item.stockCount} ${item.baseUom} in stock` : 'Service'
            }`}
            onPress={() => onSelectProduct(item)}
          >
            <Card className="flex-1 p-3 gap-2">
              <View className="flex-row items-start justify-between gap-2">
                <Text
                  className="text-foreground text-sm font-semibold flex-1"
                  numberOfLines={2}
                >
                  {item.name}
                </Text>
                <Chip
                  size="sm"
                  variant="soft"
                  color={outOfStock ? 'danger' : lowStock ? 'warning' : 'success'}
                >
                  <Chip.Label>
                    {item.trackInventory ? `${item.stockCount} ${item.baseUom}` : 'Service'}
                  </Chip.Label>
                </Chip>
              </View>
              <Text className="text-accent text-base font-bold">
                {currencySymbol}
                {item.price.toFixed(2)}
              </Text>
            </Card>
          </PressableFeedback>
        </View>
      );
    },
    [currencySymbol, onSelectProduct],
  );

  return (
    <View className="flex-1 gap-3">
      <View className="flex-row items-center gap-2">
        <SearchField value={searchQuery} onChange={onSearchChange} className="flex-1">
          <SearchField.Group>
            <SearchField.SearchIcon />
            <SearchField.Input placeholder={searchCopy.placeholder || searchCopy.label} />
            <SearchField.ClearButton />
          </SearchField.Group>
        </SearchField>
        <Button variant="secondary" size="md" onPress={onOpenScanner}>
          <Button.Label>Scan</Button.Label>
        </Button>
      </View>

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        className="shrink-0 grow-0"
        contentContainerClassName="flex-row gap-2 py-1"
      >
        <Chip
          color={selectedCategoryId === null ? 'accent' : 'default'}
          variant={selectedCategoryId === null ? 'primary' : 'soft'}
          onPress={() => onSelectCategory(null)}
        >
          <Chip.Label>All</Chip.Label>
        </Chip>
        {categories.map((cat) => (
          <Chip
            key={cat.id}
            color={selectedCategoryId === cat.id ? 'accent' : 'default'}
            variant={selectedCategoryId === cat.id ? 'primary' : 'soft'}
            onPress={() => onSelectCategory(cat.id)}
          >
            <Chip.Label>{cat.name}</Chip.Label>
          </Chip>
        ))}
      </ScrollView>

      <FlatList
        key={numColumns}
        data={products}
        numColumns={numColumns}
        keyExtractor={catalogKeyExtractor}
        initialNumToRender={12}
        maxToRenderPerBatch={10}
        windowSize={7}
        removeClippedSubviews
        columnWrapperClassName="gap-3"
        contentContainerClassName="gap-3 pb-6"
        ListEmptyComponent={
          <Card className="p-6 items-center">
            <Card.Title>No matching products</Card.Title>
            <Card.Description>
              Add items in Admin Center → Catalog Inventory or adjust your filter.
            </Card.Description>
          </Card>
        }
        renderItem={renderProduct}
      />
    </View>
  );
});
