import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { FlatList, ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import {
  Alert,
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
  useToast,
} from 'heroui-native';
import { TactileButton } from '@/components/TactilePressable';
import { getRepositories } from '@/db/repositories';
import type { Category, Product } from '@/db/types';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { layoutTransition, staggerFadeUp } from '@/theme/motion';

export function AdminProductsTab() {
  const { toast } = useToast();
  const { containerPaddingClass, dialogMaxWidthClass } = useWindowLayout();
  const currency = useAppBootstrapStore((s) => s.currencySymbol);
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [showAdd, setShowAdd] = useState(false);

  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [cost, setCost] = useState('');
  const [stock, setStock] = useState('10');
  const [barcode, setBarcode] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [restockingId, setRestockingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [dialogError, setDialogError] = useState<string | null>(null);

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
    if (isSaving) return;
    setDialogError(null);
    if (!name.trim()) {
      setDialogError('Product name is required.');
      return;
    }
    const parsedPrice = price.trim() === '' ? 0 : Number(price.trim());
    const parsedCost = cost.trim() === '' ? 0 : Number(cost.trim());
    const parsedStock = stock.trim() === '' ? 0 : Number(stock.trim());
    if (!Number.isFinite(parsedPrice) || parsedPrice < 0) {
      setDialogError('Price must be a valid non-negative number.');
      return;
    }
    if (!Number.isFinite(parsedCost) || parsedCost < 0) {
      setDialogError('Cost must be a valid non-negative number.');
      return;
    }
    if (!Number.isInteger(parsedStock) || parsedStock < 0) {
      setDialogError('Opening stock must be a valid non-negative whole number.');
      return;
    }

    setIsSaving(true);
    const savedName = name.trim();
    try {
      const repos = await getRepositories();
      await repos.catalog.saveProduct({
        name: savedName,
        categoryId: categories[0]?.id ?? 1,
        price: parsedPrice,
        cost: parsedCost,
        stockCount: parsedStock,
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
      toast.show({
        label: 'Product saved',
        description: savedName,
        variant: 'success',
      });
    } catch (err) {
      setDialogError(err instanceof Error ? err.message : 'Failed to save product.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleQuickRestock = async (product: Product) => {
    if (restockingId !== null) return;
    setError(null);
    setRestockingId(product.id);
    try {
      const repos = await getRepositories();
      await repos.catalog.adjustStock({
        productId: product.id,
        delta: 10,
        actorUsername: activeUser?.username ?? 'admin',
        movementType: 'RESTOCK',
        notes: 'Quick +10 Admin Restock',
      });
      await load();
      toast.show({
        label: 'Restocked +10',
        description: `${product.name} (+10 ${product.baseUom})`,
        variant: 'success',
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to restock product.');
    } finally {
      setRestockingId(null);
    }
  };

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
    <View className={`flex-1 gap-3 ${containerPaddingClass}`}>
      <View className="flex-row items-center gap-2">
        <SearchField value={query} onChange={setQuery} className="flex-1">
          <SearchField.Group>
            <SearchField.SearchIcon />
            <SearchField.Input placeholder="Filter catalog by name or barcode..." />
            <SearchField.ClearButton />
          </SearchField.Group>
        </SearchField>
        <TactileButton
          feedbackVariant="stretch"
          variant="primary"
          onPress={() => setShowAdd(true)}
        >
          <Button.Label>+ Product</Button.Label>
        </TactileButton>
      </View>

      {error ? (
        <Alert status="danger">
          <Alert.Indicator />
          <Alert.Content>
            <Alert.Description>{error}</Alert.Description>
          </Alert.Content>
        </Alert>
      ) : null}

      <FlatList
        data={filtered}
        keyExtractor={(item) => String(item.id)}
        initialNumToRender={12}
        maxToRenderPerBatch={10}
        windowSize={7}
        removeClippedSubviews
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
              <TactileButton
                feedbackVariant="spring"
                variant="secondary"
                size="sm"
                isDisabled={restockingId !== null}
                onPress={() => void handleQuickRestock(item)}
              >
                <Button.Label>{restockingId === item.id ? '...' : '+10'}</Button.Label>
              </TactileButton>
            </Surface>
          </Animated.View>
        )}
      />

      <Dialog isOpen={showAdd} onOpenChange={setShowAdd}>
        <Dialog.Portal>
          <Dialog.Overlay />
          <Dialog.Content className={`p-5 gap-3 ${dialogMaxWidthClass} w-full self-center mx-4 max-h-[90%]`}>
            <Dialog.Title>Add Product</Dialog.Title>
            <ScrollView keyboardShouldPersistTaps="handled" contentContainerClassName="gap-3">
              {dialogError ? (
                <Alert status="danger">
                  <Alert.Indicator />
                  <Alert.Content>
                    <Alert.Description>{dialogError}</Alert.Description>
                  </Alert.Content>
                </Alert>
              ) : null}
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
              <Button variant="secondary" isDisabled={isSaving} onPress={() => setShowAdd(false)}>
                <Button.Label>Cancel</Button.Label>
              </Button>
              <TactileButton
                feedbackVariant="stretch"
                variant="primary"
                isDisabled={isSaving}
                onPress={() => void handleSave()}
              >
                <Button.Label>{isSaving ? 'Saving...' : 'Save Product'}</Button.Label>
              </TactileButton>
            </View>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog>
    </View>
  );
}
