import React, { useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { Card, Chip, Separator, Surface } from 'heroui-native';
import { getRepositories } from '@/db/repositories';
import type { Product, StoreConfig, Transaction } from '@/db/types';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

export function AdminAnalyticsTab() {
  const currency = useAppBootstrapStore((s) => s.currencySymbol);
  const [txs, setTxs] = useState<Transaction[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [config, setConfig] = useState<StoreConfig | null>(null);

  useEffect(() => {
    void (async () => {
      const repos = await getRepositories();
      const [recent, allProds, cfg] = await Promise.all([
        repos.checkout.getRecentTransactions(200),
        repos.catalog.getProducts(false),
        repos.storeConfig.getConfig(),
      ]);
      setTxs(recent);
      setProducts(allProds);
      setConfig(cfg);
    })();
  }, []);

  const totalSales = txs.reduce((s, t) => s + t.totalAmount, 0);
  const lowStockCount = products.filter((p) => p.trackInventory && p.stockCount <= 5).length;
  const byMethod = txs.reduce<Record<string, number>>((acc, t) => {
    acc[t.paymentMethod] = (acc[t.paymentMethod] ?? 0) + t.totalAmount;
    return acc;
  }, {});

  return (
    <ScrollView contentContainerClassName="gap-4 pb-6">
      <View className="flex-row flex-wrap gap-3">
        <Card className="flex-1 min-w-36 p-4 gap-1">
          <Text className="text-muted text-xs">Gross Sales</Text>
          <Text className="text-accent text-xl font-bold">
            {currency}
            {totalSales.toFixed(2)}
          </Text>
        </Card>
        <Card className="flex-1 min-w-36 p-4 gap-1">
          <Text className="text-muted text-xs">Transactions</Text>
          <Text className="text-foreground text-xl font-bold">{txs.length}</Text>
        </Card>
        <Card className="flex-1 min-w-36 p-4 gap-1">
          <Text className="text-muted text-xs">Low Stock Alerts</Text>
          <Text className="text-warning text-xl font-bold">{lowStockCount}</Text>
        </Card>
      </View>

      {config ? (
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
      ) : null}

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
    </ScrollView>
  );
}
