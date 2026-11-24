import React, { useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Card, Chip, Separator, Spinner, Surface } from 'heroui-native';
import { TactilePressable } from '@/components/TactilePressable';
import { getRepositories } from '@/db/repositories';
import type { Product, StoreConfig, Transaction } from '@/db/types';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFadeUp, staggerFadeUp } from '@/theme/motion';

export function AdminAnalyticsTab() {
  const currency = useAppBootstrapStore((s) => s.currencySymbol);
  const { kpiColumns, containerPaddingClass } = useWindowLayout();
  const [txs, setTxs] = useState<Transaction[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [config, setConfig] = useState<StoreConfig | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const repos = await getRepositories();
        const [recent, allProds, cfg] = await Promise.all([
          repos.checkout.getRecentTransactions(200),
          repos.catalog.getProducts(false),
          repos.storeConfig.getConfig(),
        ]);
        if (!active) return;
        setTxs(recent);
        setProducts(allProds);
        setConfig(cfg);
      } catch {
        // Ignore load errors when analytics view fails to query local DB
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center py-12">
        <Spinner size="md" />
      </View>
    );
  }

  const totalSales = txs.reduce((s, t) => s + t.totalAmount, 0);
  const lowStockCount = products.filter((p) => p.trackInventory && p.stockCount <= 5).length;
  const byMethod = txs.reduce<Record<string, number>>((acc, t) => {
    acc[t.paymentMethod] = (acc[t.paymentMethod] ?? 0) + t.totalAmount;
    return acc;
  }, {});

  const kpis = [
    {
      label: 'Gross Sales',
      value: `${currency}${totalSales.toFixed(2)}`,
      valueClass: 'text-accent text-xl font-bold',
    },
    {
      label: 'Transactions',
      value: String(txs.length),
      valueClass: 'text-foreground text-xl font-bold',
    },
    {
      label: 'Low Stock Alerts',
      value: String(lowStockCount),
      valueClass: 'text-warning text-xl font-bold',
    },
  ];

  const kpiColClass =
    kpiColumns === 1
      ? 'w-full'
      : kpiColumns === 2
        ? 'w-[48%]'
        : kpiColumns === 3
          ? 'w-[31%]'
          : 'w-[23%]';

  return (
    <ScrollView contentContainerClassName={`gap-4 pb-6 ${containerPaddingClass}`}>
      <View className="flex-row flex-wrap gap-3">
        {kpis.map((kpi, i) => (
          <Animated.View key={kpi.label} entering={staggerFadeUp(i)} className={kpiColClass}>
            <TactilePressable feedbackVariant="subtle">
              <Card className="p-4 gap-1">
                <Text className="text-muted text-xs">{kpi.label}</Text>
                <Text className={kpi.valueClass}>{kpi.value}</Text>
              </Card>
            </TactilePressable>
          </Animated.View>
        ))}
      </View>

      {config ? (
        <Animated.View entering={enterFadeUp}>
          <Card className="p-4 gap-2">
            <Card.Title>E-Wallet & Load Floats</Card.Title>
            <View className="flex-row flex-wrap gap-2 pt-1">
              <Chip color="accent" variant="soft" size="sm">
                <Chip.Label>
                  GCash: {currency}
                  {config.gcashBalance.toFixed(2)}
                </Chip.Label>
              </Chip>
              <Chip color="success" variant="soft" size="sm">
                <Chip.Label>
                  Smart: {currency}
                  {config.smartLoadBalance.toFixed(2)}
                </Chip.Label>
              </Chip>
              <Chip color="warning" variant="soft" size="sm">
                <Chip.Label>
                  Globe: {currency}
                  {config.globeLoadBalance.toFixed(2)}
                </Chip.Label>
              </Chip>
            </View>
          </Card>
        </Animated.View>
      ) : null}

      <Animated.View entering={enterFadeUp}>
        <Card className="p-4 gap-2">
          <Card.Title>Payment Method Split</Card.Title>
          <Separator />
          {Object.keys(byMethod).length === 0 ? (
            <Text className="text-muted text-xs py-2">No sales recorded yet.</Text>
          ) : (
            Object.entries(byMethod).map(([method, amount]) => (
              <Surface
                key={method}
                variant="secondary"
                className="rounded-lg px-3 py-2 flex-row justify-between"
              >
                <Text className="text-foreground text-sm font-medium">{method}</Text>
                <Text className="text-foreground text-sm font-bold">
                  {currency}
                  {amount.toFixed(2)}
                </Text>
              </Surface>
            ))
          )}
        </Card>
      </Animated.View>
    </ScrollView>
  );
}
