import React, { useCallback, useEffect, useState } from 'react';
import { FlatList, ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import {
  Button,
  Card,
  Chip,
  Dialog,
  Input,
  Label,
  SearchField,
  Separator,
  Surface,
  TextField,
} from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { Category, Product } from '@/db/types';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { layoutTransition, staggerFadeUp } from '@/theme/motion';

export function AdminProductsTab() {
  const currency = useAppBootstrapStore((s) => s.currencySymbol);
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [query, setQuery] = useState('');
  const [showAdd, setShowAdd] = useState(false);

  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [cost, setCost] = useState('');
  const [stock, setStock] = useState('10');
  const [barcode, setBarcode] = useState('');

  const load = useCallback(async () => {
    const repos = await getRepositories();
    const [prods, cats] = await Promise.all([
      repos.catalog.getProducts(false),
      repos.catalog.getCategories(false),
    ]);
    setProducts(prods);
    setCategories(cats);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSave = async () => {
    if (!name.trim()) return;
    const repos = await getRepositories();
    await repos.catalog.saveProduct({
      name: name.trim(),
      categoryId: categories[0]?.id ?? 1,
      price: Number.parseFloat(price) || 0,
      cost: Number.parseFloat(cost) || 0,
      stockCount: Number.parseInt(stock, 10) || 0,
      barcode: barcode.trim(),
      expirationDate: null,
      hasStick10s: false,
      priceStick10s: 0,
      hasStick20s: false,
      priceStick20s: 0,
      hasReam: false,
      priceReam: 0,
      hasMasterCase: false,
      priceMasterCase: 0,
      hasCustomUom: false,
      customUomName: 'Pack',
      customUomMultiplier: 1,
      customUomPrice: 0,
      barcode10s: '',
      barcode20s: '',
      barcodeReam: '',
      barcodeMasterCase: '',
      barcodeCustomUom: '',
      baseUom: 'pc',
      trackInventory: true,
      isArchived: false,
    });
    setName('');
    setPrice('');
    setCost('');
    setStock('10');
    setBarcode('');
    setShowAdd(false);
    await load();
  };

  const handleQuickRestock = async (product: Product) => {
    const repos = await getRepositories();
    await repos.catalog.adjustStock({
      productId: product.id,
      delta: 10,
      actorUsername: activeUser?.username ?? 'admin',
      movementType: 'RESTOCK',
      notes: 'Quick +10 Admin Restock',
    });
    await load();
  };

  const filtered = products.filter(
    (p) =>
      !query.trim() ||
      p.name.toLowerCase().includes(query.trim().toLowerCase()) ||
      p.barcode.toLowerCase().includes(query.trim().toLowerCase()),
  );

  return (
    <View className="flex-1 gap-3">
      <View className="flex-row items-center gap-2">
        <SearchField value={query} onChange={setQuery} className="flex-1">
          <SearchField.Group>
            <SearchField.SearchIcon />
            <SearchField.Input placeholder="Filter catalog by name or barcode..." />
            <SearchField.ClearButton />
          </SearchField.Group>
        </SearchField>
        <Button variant="primary" onPress={() => setShowAdd(true)}>
          <Button.Label>+ Product</Button.Label>
        </Button>
      </View>

      <FlatList
        data={filtered}
        keyExtractor={(item) => String(item.id)}
        contentContainerClassName="gap-2 pb-6"
        ListEmptyComponent={
          <Card className="p-6 items-center">
            <Card.Title>Catalog is empty</Card.Title>
            <Card.Description>Tap + Product to add your first SKU.</Card.Description>
          </Card>
        }
        renderItem={({ item, index }) => (
          <Animated.View entering={staggerFadeUp(index)} layout={layoutTransition}>
            <Surface variant="default" className="rounded-xl p-3 border border-border flex-row items-center justify-between gap-2">
              <View className="flex-1 min-w-0">
                <Text numberOfLines={1} className="text-foreground text-sm font-semibold">{item.name}</Text>
                <Text numberOfLines={1} className="text-muted text-xs">
                  {currency}
                  {item.price.toFixed(2)} • Cost {currency}
                  {item.cost.toFixed(2)} {item.barcode ? `• ${item.barcode}` : ''}
                </Text>
              </View>
              <Chip
                size="sm"
                variant="soft"
                color={item.stockCount <= 5 ? 'warning' : 'success'}
              >
                <Chip.Label>{item.stockCount} {item.baseUom}</Chip.Label>
              </Chip>
              <Button variant="secondary" size="sm" onPress={() => void handleQuickRestock(item)}>
                <Button.Label>+10</Button.Label>
              </Button>
            </Surface>
          </Animated.View>
        )}
      />

      <Dialog isOpen={showAdd} onOpenChange={setShowAdd}>
        <Dialog.Portal>
          <Dialog.Overlay />
          <Dialog.Content className="p-5 gap-3 max-w-md w-full self-center mx-4 max-h-[90%]">
            <Dialog.Title>Add Product</Dialog.Title>
            <ScrollView keyboardShouldPersistTaps="handled" contentContainerClassName="gap-3">
              <TextField isRequired>
                <Label>Product Name</Label>
                <Input value={name} onChangeText={setName} placeholder="e.g. Bottled Water 500ml" />
              </TextField>
              <View className="flex-row gap-2">
                <TextField className="flex-1">
                  <Label>Price ({currency})</Label>
                  <Input value={price} onChangeText={setPrice} keyboardType="decimal-pad" placeholder="0.00" />
                </TextField>
                <TextField className="flex-1">
                  <Label>Cost ({currency})</Label>
                  <Input value={cost} onChangeText={setCost} keyboardType="decimal-pad" placeholder="0.00" />
                </TextField>
              </View>
              <View className="flex-row gap-2">
                <TextField className="flex-1">
                  <Label>Opening Stock</Label>
                  <Input value={stock} onChangeText={setStock} keyboardType="number-pad" />
                </TextField>
                <TextField className="flex-1">
                  <Label>Barcode</Label>
                  <Input value={barcode} onChangeText={setBarcode} placeholder="Optional" />
                </TextField>
              </View>
            </ScrollView>
            <Separator />
            <View className="flex-row justify-end gap-2">
              <Button variant="secondary" onPress={() => setShowAdd(false)}>
                <Button.Label>Cancel</Button.Label>
              </Button>
              <Button variant="primary" onPress={() => void handleSave()}>
                <Button.Label>Save Product</Button.Label>
              </Button>
            </View>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog>
    </View>
  );
}
