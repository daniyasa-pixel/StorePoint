import React, { useState } from 'react';
import { View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Button, Tabs } from 'heroui-native';
import { AppTopBar } from '@/components/AppTopBar';
import { APP_ROUTES } from '@/constants/routes';
import { InventoryPriceCheckerTab } from '@/features/inventory/InventoryPriceCheckerTab';
import { InventoryQuickShareTab } from '@/features/inventory/InventoryQuickShareTab';
import { InventoryStocktakeTab } from '@/features/inventory/InventoryStocktakeTab';
import { BulkProductionDialog } from '@/features/production/BulkProductionDialog';
import { exportAndShareBackupSnapshot } from '@/backup/backupEngine';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

export default function InventoryPortalRoute() {
  const router = useRouter();
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const storeName = useAppBootstrapStore((s) => s.storeName);
  const logout = useAppBootstrapStore((s) => s.logout);
  const [selectedTab, setSelectedTab] = useState('price-checker');
  const [showProduction, setShowProduction] = useState(false);

  const isAdmin = activeUser?.role === 'ADMIN';

  const handleLogout = () => {
    logout();
    router.replace(APP_ROUTES.LOGIN);
  };

  return (
    <SafeAreaView className="bg-background flex-1">
      <AppTopBar
        title={storeName}
        subtitle="Inventory & Audit Console"
        onBack={isAdmin ? () => router.back() : undefined}
        rightActions={
          <>
            <Button variant="secondary" size="sm" onPress={() => setShowProduction(true)}>
              <Button.Label>Repack BOM</Button.Label>
            </Button>
            <Button variant="danger-soft" size="sm" onPress={handleLogout}>
              <Button.Label>Logout</Button.Label>
            </Button>
          </>
        }
      />

      <View className="flex-1 p-4">
        <Tabs value={selectedTab} onValueChange={setSelectedTab} className="flex-1 gap-3">
          <Tabs.List className="w-full">
            <Tabs.Indicator />
            <Tabs.Trigger value="price-checker" className="flex-1">
              <Tabs.Label>Price Checker</Tabs.Label>
            </Tabs.Trigger>
            <Tabs.Trigger value="stocktake" className="flex-1">
              <Tabs.Label>Stocktake</Tabs.Label>
            </Tabs.Trigger>
            <Tabs.Trigger value="quick-share" className="flex-1">
              <Tabs.Label>Quick Share</Tabs.Label>
            </Tabs.Trigger>
          </Tabs.List>

          <Tabs.Content value="price-checker" className="flex-1">
            <InventoryPriceCheckerTab />
          </Tabs.Content>

          <Tabs.Content value="stocktake" className="flex-1">
            <InventoryStocktakeTab />
          </Tabs.Content>

          <Tabs.Content value="quick-share" className="flex-1">
            <InventoryQuickShareTab
              onExportCatalog={() => {
                void exportAndShareBackupSnapshot();
              }}
            />
          </Tabs.Content>
        </Tabs>
      </View>

      <BulkProductionDialog isOpen={showProduction} onOpenChange={setShowProduction} />
    </SafeAreaView>
  );
}
