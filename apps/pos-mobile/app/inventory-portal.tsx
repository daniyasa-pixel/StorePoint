import React, { useState } from 'react';
import { View } from 'react-native';
import Animated from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Button, Tabs } from 'heroui-native';
import { AppTopBar } from '@/components/AppTopBar';
import { TactileButton } from '@/components/TactilePressable';
import { APP_ROUTES } from '@/constants/routes';
import { InventoryPriceCheckerTab } from '@/features/inventory/InventoryPriceCheckerTab';
import { InventoryQuickShareTab } from '@/features/inventory/InventoryQuickShareTab';
import { InventoryStocktakeTab } from '@/features/inventory/InventoryStocktakeTab';
import { BulkProductionDialog } from '@/features/production/BulkProductionDialog';
import { exportAndShareBackupSnapshot } from '@/backup/backupEngine';
import { useBackNavigationGuard } from '@/layout/useBackNavigationGuard';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { enterFade } from '@/theme/motion';

export default function InventoryPortalRoute() {
  const router = useRouter();
  const { containerPaddingClass } = useWindowLayout();
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const storeName = useAppBootstrapStore((s) => s.storeName);
  const logout = useAppBootstrapStore((s) => s.logout);
  const [activeTab, setActiveTab] = useState('price-checker');
  const [showProduction, setShowProduction] = useState(false);

  const isAdmin = activeUser?.role === 'ADMIN';

  useBackNavigationGuard({
    requireDoubleBackToExit: true,
    onBack: () => {
      if (showProduction) {
        setShowProduction(false);
        return true;
      }
      if (activeTab !== 'price-checker') {
        setActiveTab('price-checker');
        return true;
      }
      if (router.canGoBack()) {
        router.back();
        return true;
      }
      return false;
    },
  });

  const handleLogout = () => {
    logout();
    router.replace(APP_ROUTES.LOGIN);
  };

  return (
    <SafeAreaView className="bg-background flex-1">
      <AppTopBar
        title={storeName}
        subtitle="Inventory & Audit Console"
        onBack={
          isAdmin
            ? () => {
                if (router.canGoBack()) {
                  router.back();
                } else {
                  router.replace(APP_ROUTES.ADMIN_DASHBOARD);
                }
              }
            : undefined
        }
        rightActions={
          <>
            <TactileButton
              feedbackVariant="spring"
              variant="secondary"
              size="sm"
              onPress={() => setShowProduction(true)}
            >
              <Button.Label>Repack BOM</Button.Label>
            </TactileButton>
            <TactileButton
              feedbackVariant="spring"
              variant="danger-soft"
              size="sm"
              onPress={handleLogout}
            >
              <Button.Label>Logout</Button.Label>
            </TactileButton>
          </>
        }
      />

      <View className={`flex-1 ${containerPaddingClass}`}>
        <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 gap-3">
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
            <Animated.View entering={enterFade} className="flex-1">
              <InventoryPriceCheckerTab />
            </Animated.View>
          </Tabs.Content>

          <Tabs.Content value="stocktake" className="flex-1">
            <Animated.View entering={enterFade} className="flex-1">
              <InventoryStocktakeTab />
            </Animated.View>
          </Tabs.Content>

          <Tabs.Content value="quick-share" className="flex-1">
            <Animated.View entering={enterFade} className="flex-1">
              <InventoryQuickShareTab
                onExportCatalog={() => {
                  void exportAndShareBackupSnapshot();
                }}
              />
            </Animated.View>
          </Tabs.Content>
        </Tabs>
      </View>

      <BulkProductionDialog isOpen={showProduction} onOpenChange={setShowProduction} />
    </SafeAreaView>
  );
}
