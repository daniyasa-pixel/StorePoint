import { useCallback, useEffect, useMemo, useState } from 'react';
import { getRepositories } from '@/db/repositories';
import type { Category, Product, ProductVariant, UomOption } from '@/db/types';
import { usePosCartStore } from '@/store/posCartStore';

export function buildUomOptionsForProduct(
  product: Product,
  variants: readonly ProductVariant[],
): UomOption[] {
  const options: UomOption[] = [
    { name: product.baseUom || 'pc', multiplier: 1, price: product.price },
  ];

  if (product.hasStick10s && product.priceStick10s > 0) {
    options.push({ name: '10s Pack', multiplier: 10, price: product.priceStick10s });
  }
  if (product.hasStick20s && product.priceStick20s > 0) {
    options.push({ name: '20s Pack', multiplier: 20, price: product.priceStick20s });
  }
  if (product.hasReam && product.priceReam > 0) {
    options.push({ name: 'Ream', multiplier: 200, price: product.priceReam });
  }
  if (product.hasMasterCase && product.priceMasterCase > 0) {
    options.push({ name: 'Master Case', multiplier: 1000, price: product.priceMasterCase });
  }
  if (product.hasCustomUom && product.customUomPrice > 0) {
    options.push({
      name: product.customUomName || 'Pack',
      multiplier: Math.max(1, product.customUomMultiplier),
      price: product.customUomPrice,
    });
  }

  for (const v of variants) {
    options.push({
      name: v.variantName,
      multiplier: Math.max(1, v.multiplier),
      price: v.price,
    });
  }

  return options;
}

export function usePosCatalog() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const handle = setTimeout(() => setDebouncedQuery(searchQuery.trim().toLowerCase()), 140);
    return () => clearTimeout(handle);
  }, [searchQuery]);

  const refreshCatalog = useCallback(async () => {
    setIsLoading(true);
    try {
      const repos = await getRepositories();
      const [cats, prods] = await Promise.all([
        repos.catalog.getCategories(false),
        repos.catalog.getProducts(false),
      ]);
      setCategories(cats);
      setProducts(prods);
      await usePosCartStore.getState().restoreFromDraft(prods);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshCatalog();
  }, [refreshCatalog]);

  const filteredProducts = useMemo(() => {
    return products.filter((p) => {
      if (selectedCategoryId !== null && p.categoryId !== selectedCategoryId) {
        return false;
      }
      if (!debouncedQuery) return true;
      return (
        p.name.toLowerCase().includes(debouncedQuery) ||
        p.barcode.toLowerCase().includes(debouncedQuery)
      );
    });
  }, [products, selectedCategoryId, debouncedQuery]);

  return {
    categories,
    products,
    filteredProducts,
    selectedCategoryId,
    setSelectedCategoryId,
    searchQuery,
    setSearchQuery,
    isLoading,
    refreshCatalog,
  };
}
