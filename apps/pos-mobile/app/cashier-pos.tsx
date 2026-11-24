import React, { useEffect, useState } from 'react';
import { ScrollView, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Alert, Button, Chip, Tabs } from 'heroui-native';
import { AppTopBar } from '@/components/AppTopBar';
import { APP_ROUTES } from '@/constants/routes';
import { getRepositories } from '@/db/repositories';
import type { CheckoutReceipt } from '@/db/repositories/checkoutRepository';
import type { CashierSession, Product, UomOption } from '@/db/types';
import { DigitalServicesDialog } from '@/features/digital/DigitalServicesDialog';
import { ParkedOrdersDialog } from '@/features/pos/ParkedOrdersDialog';
import { PaymentCheckoutDialog } from '@/features/pos/PaymentCheckoutDialog';
import { PosCartPane } from '@/features/pos/PosCartPane';
import { PosCatalogPane } from '@/features/pos/PosCatalogPane';
import { ReceiptPreviewDialog } from '@/features/pos/ReceiptPreviewDialog';
import { buildUomOptionsForProduct, usePosCatalog } from '@/features/pos/usePosCatalog';
import { VariantPickerDialog } from '@/features/pos/VariantPickerDialog';
import { DrawerCashInOutDialog } from '@/features/shifts/DrawerCashInOutDialog';
import { ReturnRefundDialog } from '@/features/shifts/ReturnRefundDialog';
import { ShiftLifecycleDialog } from '@/features/shifts/ShiftLifecycleDialog';
import { BarcodeScannerModal } from '@/hardware/BarcodeScannerModal';
import { getWindowLayout } from '@/layout/adaptiveLayout';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { usePosCartStore } from '@/store/posCartStore';
import { enterFadeDown, exitFadeUp, layoutTransition } from '@/theme/motion';

export default function CashierPosRoute() {
  const router = useRouter();
  const { width } = useWindowLayout();
  const windowLayout = getWindowLayout(width);
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const storeName = useAppBootstrapStore((s) => s.storeName);
  const currencySymbol = useAppBootstrapStore((s) => s.currencySymbol);
  const logout = useAppBootstrapStore((s) => s.logout);

  const lines = usePosCartStore((s) => s.lines);
  const addProduct = usePosCartStore((s) => s.addProduct);
  const restoredNoticeCount = usePosCartStore((s) => s.restoredNoticeCount);
  const acknowledgeRestoreNotice = usePosCartStore((s) => s.acknowledgeRestoreNotice);

  const catalog = usePosCatalog();

  const [activeSession, setActiveSession] = useState<CashierSession | null>(null);
  const [portraitTab, setPortraitTab] = useState('catalog');
  const [variantTarget, setVariantTarget] = useState<{
    product: Product;
    options: UomOption[];
  } | null>(null);
  const [showPayment, setShowPayment] = useState(false);
  const [showParked, setShowParked] = useState(false);
  const [showShift, setShowShift] = useState(false);
  const [showDrawer, setShowDrawer] = useState(false);
  const [showReturns, setShowReturns] = useState(false);
  const [showDigital, setShowDigital] = useState(false);
  const [showScanner, setShowScanner] = useState(false);
  const [lastReceipt, setLastReceipt] = useState<CheckoutReceipt | null>(null);

  useEffect(() => {
    void getRepositories()
      .then((repos) => repos.shifts.getActiveSession(activeUser?.username))
      .then(setActiveSession);
  }, [activeUser?.username]);

  const handleSelectProduct = async (product: Product) => {
    const repos = await getRepositories();
    const variants = await repos.catalog.getVariantsForProduct(product.id);
    const options = buildUomOptionsForProduct(product, variants);
    if (options.length > 1) {
      setVariantTarget({ product, options });
    } else {
      addProduct(product, options[0]);
    }
  };

  const handleLogout = () => {
    logout();
    router.replace(APP_ROUTES.LOGIN);
  };

  const catalogPane = (
    <PosCatalogPane
      categories={catalog.categories}
      products={catalog.filteredProducts}
      selectedCategoryId={catalog.selectedCategoryId}
      onSelectCategory={catalog.setSelectedCategoryId}
      searchQuery={catalog.searchQuery}
      onSearchChange={catalog.setSearchQuery}
      currencySymbol={currencySymbol}
      onSelectProduct={(p) => {
        void handleSelectProduct(p);
      }}
      onOpenScanner={() => setShowScanner(true)}
    />
  );

  const cartPane = (
    <PosCartPane
      currencySymbol={currencySymbol}
      vatRate={12.0}
      onOpenPayment={() => setShowPayment(true)}
      onOpenParked={() => setShowParked(true)}
    />
  );

  return (
    <SafeAreaView className="bg-background flex-1">
      <AppTopBar
        title={storeName}
        subtitle={`POS • ${activeUser?.username ?? 'Guest'} • ${activeSession ? `Shift #${activeSession.id}` : 'No Shift'}`}
        rightActions={
          <>
            {activeUser?.role === 'ADMIN' ? (
              <Button
                variant="secondary"
                size="sm"
                onPress={() => router.push(APP_ROUTES.ADMIN_DASHBOARD)}
              >
                <Button.Label>Admin</Button.Label>
              </Button>
            ) : null}
            <Button variant="danger-soft" size="sm" onPress={handleLogout}>
              <Button.Label>Logout</Button.Label>
            </Button>
          </>
        }
      />

      <View className="px-4 pt-2">
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerClassName="flex-row gap-2">
          <Chip
            size="sm"
            color={activeSession ? 'success' : 'warning'}
            variant="soft"
            onPress={() => setShowShift(true)}
          >
            <Chip.Label>{activeSession ? 'Close Shift' : 'Open Shift'}</Chip.Label>
          </Chip>
          <Chip size="sm" color="default" variant="soft" onPress={() => setShowDrawer(true)}>
            <Chip.Label>Cash In/Out</Chip.Label>
          </Chip>
          <Chip size="sm" color="default" variant="soft" onPress={() => setShowReturns(true)}>
            <Chip.Label>Returns</Chip.Label>
          </Chip>
          <Chip size="sm" color="accent" variant="soft" onPress={() => setShowDigital(true)}>
            <Chip.Label>Digital / Load</Chip.Label>
          </Chip>
        </ScrollView>
      </View>

      <View className="flex-1 p-4 gap-3">
        {restoredNoticeCount > 0 ? (
          <Animated.View
            entering={enterFadeDown}
            exiting={exitFadeUp}
            layout={layoutTransition}
          >
            <Alert status="accent">
              <Alert.Indicator />
              <Alert.Content>
                <Alert.Title>Restored your basket ({restoredNoticeCount} item(s))</Alert.Title>
              </Alert.Content>
              <Button variant="ghost" size="sm" onPress={acknowledgeRestoreNotice}>
                <Button.Label>OK</Button.Label>
              </Button>
            </Alert>
          </Animated.View>
        ) : null}

        {windowLayout !== 'Compact' ? (
          <View className="flex-1 flex-row gap-4">
            <View className={windowLayout === 'Expanded' ? 'flex-[1.5]' : 'flex-[1.2]'}>
              {catalogPane}
            </View>
            <View className="flex-1">{cartPane}</View>
          </View>
        ) : (
          <Tabs value={portraitTab} onValueChange={setPortraitTab} className="flex-1 gap-3">
            <Tabs.List className="w-full">
              <Tabs.Indicator />
              <Tabs.Trigger value="catalog" className="flex-1">
                <Tabs.Label>Catalog</Tabs.Label>
              </Tabs.Trigger>
              <Tabs.Trigger value="cart" className="flex-1">
                <Tabs.Label>Cart ({lines.length})</Tabs.Label>
              </Tabs.Trigger>
            </Tabs.List>
            <Tabs.Content value="catalog" className="flex-1">
              {catalogPane}
            </Tabs.Content>
            <Tabs.Content value="cart" className="flex-1">
              {cartPane}
            </Tabs.Content>
          </Tabs>
        )}
      </View>

      <VariantPickerDialog
        product={variantTarget?.product ?? null}
        options={variantTarget?.options ?? []}
        currencySymbol={currencySymbol}
        onSelect={(p, opt) => addProduct(p, opt)}
        onClose={() => setVariantTarget(null)}
      />

      <PaymentCheckoutDialog
        isOpen={showPayment}
        onOpenChange={setShowPayment}
        currencySymbol={currencySymbol}
        vatRate={12.0}
        onCompleted={(receipt) => {
          setLastReceipt(receipt);
          void catalog.refreshCatalog();
        }}
      />

      <ParkedOrdersDialog
        isOpen={showParked}
        onOpenChange={setShowParked}
        products={catalog.products}
      />

      <ReceiptPreviewDialog
        receipt={lastReceipt}
        storeName={storeName}
        currencySymbol={currencySymbol}
        onClose={() => setLastReceipt(null)}
      />

      <ShiftLifecycleDialog
        isOpen={showShift}
        onOpenChange={setShowShift}
        activeSession={activeSession}
        currencySymbol={currencySymbol}
        onSessionChanged={setActiveSession}
      />

      <DrawerCashInOutDialog
        isOpen={showDrawer}
        onOpenChange={setShowDrawer}
        activeSession={activeSession}
        currencySymbol={currencySymbol}
      />

      <ReturnRefundDialog
        isOpen={showReturns}
        onOpenChange={setShowReturns}
        currencySymbol={currencySymbol}
        onRefundCompleted={() => {
          void catalog.refreshCatalog();
        }}
      />

      <DigitalServicesDialog
        isOpen={showDigital}
        onOpenChange={setShowDigital}
        currencySymbol={currencySymbol}
      />

      <BarcodeScannerModal
        isOpen={showScanner}
        onOpenChange={setShowScanner}
        onBarcodeScanned={(code) => {
          void getRepositories()
            .then((r) => r.catalog.findProductByBarcode(code))
            .then((found) => {
              if (found) {
                void handleSelectProduct(found);
              }
            });
        }}
      />
    </SafeAreaView>
  );
}
