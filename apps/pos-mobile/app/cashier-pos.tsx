import React, { useEffect, useState } from 'react';
import { Alert as RNAlert, ScrollView, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Alert, Button, Chip, Tabs, useToast } from 'heroui-native';
import { AppTopBar } from '@/components/AppTopBar';
import { TactileButton } from '@/components/TactilePressable';
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
import { buildEscPosReceiptBytes } from '@/hardware/escPosFormatter';
import { useBackNavigationGuard } from '@/layout/useBackNavigationGuard';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';
import { usePosCartStore } from '@/store/posCartStore';
import { enterFadeDown, exitFadeUp, layoutTransition } from '@/theme/motion';

export default function CashierPosRoute() {
  const router = useRouter();
  const { toast } = useToast();
  const { useSplitPane, catalogFlex, cartFlex, containerPaddingClass } = useWindowLayout();
  const activeUser = useAppBootstrapStore((s) => s.activeUser);
  const storeName = useAppBootstrapStore((s) => s.storeName);
  const currencySymbol = useAppBootstrapStore((s) => s.currencySymbol);
  const logout = useAppBootstrapStore((s) => s.logout);

  const lineCount = usePosCartStore((s) => s.lines.length);
  const addProduct = usePosCartStore((s) => s.addProduct);
  const restoredNoticeCount = usePosCartStore((s) => s.restoredNoticeCount);
  const acknowledgeRestoreNotice = usePosCartStore((s) => s.acknowledgeRestoreNotice);

  const catalog = usePosCatalog();

  const [activeSession, setActiveSession] = useState<CashierSession | null>(null);
  const [vatRate, setVatRate] = useState(12.0);
  const [mobileTab, setMobileTab] = useState('catalog');
  const [variantProduct, setVariantProduct] = useState<{
    product: Product;
    options: UomOption[];
  } | null>(null);
  const [showCheckout, setShowCheckout] = useState(false);
  const [showParked, setShowParked] = useState(false);
  const [showShiftDialog, setShowShiftDialog] = useState(false);
  const [showCashInOut, setShowCashInOut] = useState(false);
  const [showReturns, setShowReturns] = useState(false);
  const [showDigital, setShowDigital] = useState(false);
  const [showScanner, setShowScanner] = useState(false);
  const [completedReceipt, setCompletedReceipt] = useState<CheckoutReceipt | null>(null);
  const [posError, setPosError] = useState<string | null>(null);

  useEffect(() => {
    void getRepositories().then(async (repos) => {
      const [session, config] = await Promise.all([
        repos.shifts.getActiveSession(activeUser?.username),
        repos.storeConfig.getConfig(),
      ]);
      setActiveSession(session);
      if (config && Number.isFinite(config.taxPercentage)) {
        setVatRate(config.taxPercentage);
      }
    });
  }, [activeUser?.username]);

  useBackNavigationGuard({
    requireDoubleBackToExit: true,
    onBack: () => {
      if (showScanner) {
        setShowScanner(false);
        return true;
      }
      if (completedReceipt) {
        setCompletedReceipt(null);
        return true;
      }
      if (showCheckout) {
        setShowCheckout(false);
        return true;
      }
      if (variantProduct) {
        setVariantProduct(null);
        return true;
      }
      if (showParked) {
        setShowParked(false);
        return true;
      }
      if (showReturns) {
        setShowReturns(false);
        return true;
      }
      if (showDigital) {
        setShowDigital(false);
        return true;
      }
      if (showCashInOut) {
        setShowCashInOut(false);
        return true;
      }
      if (showShiftDialog) {
        setShowShiftDialog(false);
        return true;
      }
      if (!useSplitPane && mobileTab === 'cart') {
        setMobileTab('catalog');
        return true;
      }
      return false;
    },
  });

  const handlePrintReceipt = (receipt: CheckoutReceipt) => {
    const payload = buildEscPosReceiptBytes(
      receipt,
      storeName,
      currencySymbol,
      58,
      receipt.transaction.paymentMethod === 'CASH',
    );
    toast.show({
      label: 'No Bluetooth printer paired',
      description: `ESC/POS payload ready (${payload.byteLength} bytes). Pair a 58mm/80mm thermal printer in Android Bluetooth settings.`,
      variant: 'warning',
    });
  };

  const handleSelectProduct = async (product: Product) => {
    setPosError(null);
    try {
      const repos = await getRepositories();
      const variants = await repos.catalog.getVariantsForProduct(product.id);
      const options = buildUomOptionsForProduct(product, variants);
      if (options.length > 1) {
        setVariantProduct({ product, options });
      } else {
        addProduct(product, options[0]);
        toast.show({
          label: `Added ${product.name}`,
          variant: 'default',
          duration: 1500,
        });
      }
    } catch (err) {
      setPosError(err instanceof Error ? err.message : `Failed to load options for ${product.name}.`);
    }
  };

  const handleBarcodeScanned = async (code: string) => {
    setPosError(null);
    try {
      const repos = await getRepositories();
      const found = await repos.catalog.findProductByBarcode(code);
      if (found) {
        await handleSelectProduct(found);
      } else {
        const message = `No product found for barcode "${code}".`;
        setPosError(message);
        toast.show({
          label: 'Barcode not found',
          description: message,
          variant: 'warning',
        });
      }
    } catch (err) {
      setPosError(err instanceof Error ? err.message : 'Barcode lookup failed.');
    }
  };

  const performLogout = () => {
    logout();
    router.replace(APP_ROUTES.LOGIN);
  };

  const handleLogout = () => {
    if (lineCount > 0) {
      RNAlert.alert('Logout', 'You have items in the cart. Logout anyway?', [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Logout', style: 'destructive', onPress: performLogout },
      ]);
      return;
    }
    performLogout();
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
      vatRate={vatRate}
      onOpenPayment={() => setShowCheckout(true)}
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
              <TactileButton
                variant="secondary"
                size="sm"
                onPress={() => router.push(APP_ROUTES.ADMIN_DASHBOARD)}
              >
                <Button.Label>Admin</Button.Label>
              </TactileButton>
            ) : null}
            <TactileButton variant="danger-soft" size="sm" onPress={handleLogout}>
              <Button.Label>Logout</Button.Label>
            </TactileButton>
          </>
        }
      />

      <View className="px-4 pt-2">
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerClassName="flex-row gap-2">
          <Chip
            size="sm"
            color={activeSession ? 'success' : 'warning'}
            variant="soft"
            accessibilityRole="button"
            accessibilityLabel={activeSession ? 'Close Shift' : 'Open Shift'}
            onPress={() => setShowShiftDialog(true)}
          >
            <Chip.Label>{activeSession ? 'Close Shift' : 'Open Shift'}</Chip.Label>
          </Chip>
          <Chip
            size="sm"
            color="default"
            variant="soft"
            accessibilityRole="button"
            accessibilityLabel="Cash In/Out"
            onPress={() => setShowCashInOut(true)}
          >
            <Chip.Label>Cash In/Out</Chip.Label>
          </Chip>
          <Chip
            size="sm"
            color="default"
            variant="soft"
            accessibilityRole="button"
            accessibilityLabel="Returns"
            onPress={() => setShowReturns(true)}
          >
            <Chip.Label>Returns</Chip.Label>
          </Chip>
          <Chip
            size="sm"
            color="accent"
            variant="soft"
            accessibilityRole="button"
            accessibilityLabel="Digital / Load"
            onPress={() => setShowDigital(true)}
          >
            <Chip.Label>Digital / Load</Chip.Label>
          </Chip>
        </ScrollView>
      </View>

      <View className={`flex-1 ${containerPaddingClass} gap-3`}>
        {posError ? (
          <Animated.View
            entering={enterFadeDown}
            exiting={exitFadeUp}
            layout={layoutTransition}
          >
            <Alert status="danger">
              <Alert.Indicator />
              <Alert.Content>
                <Alert.Description>{posError}</Alert.Description>
              </Alert.Content>
              <Button variant="ghost" size="sm" onPress={() => setPosError(null)}>
                <Button.Label>Dismiss</Button.Label>
              </Button>
            </Alert>
          </Animated.View>
        ) : null}

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

        {useSplitPane ? (
          <View className="flex-1 flex-row gap-4">
            <View style={{ flex: catalogFlex }}>{catalogPane}</View>
            <View style={{ flex: cartFlex }}>{cartPane}</View>
          </View>
        ) : (
          <Tabs value={mobileTab} onValueChange={setMobileTab} className="flex-1 gap-3">
            <Tabs.List className="w-full">
              <Tabs.Indicator />
              <Tabs.Trigger value="catalog" className="flex-1">
                <Tabs.Label>Catalog</Tabs.Label>
              </Tabs.Trigger>
              <Tabs.Trigger value="cart" className="flex-1">
                <Tabs.Label>Cart ({lineCount})</Tabs.Label>
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
        product={variantProduct?.product ?? null}
        options={variantProduct?.options ?? []}
        currencySymbol={currencySymbol}
        onSelect={(p, opt) => {
          addProduct(p, opt);
          toast.show({
            label: `Added ${p.name} (${opt.name})`,
            variant: 'default',
            duration: 1500,
          });
        }}
        onClose={() => setVariantProduct(null)}
      />

      <PaymentCheckoutDialog
        isOpen={showCheckout}
        onOpenChange={setShowCheckout}
        currencySymbol={currencySymbol}
        vatRate={vatRate}
        onCompleted={(receipt) => {
          setCompletedReceipt(receipt);
          void catalog.refreshCatalog();
        }}
      />

      <ParkedOrdersDialog
        isOpen={showParked}
        onOpenChange={setShowParked}
        products={catalog.products}
      />

      <ReceiptPreviewDialog
        receipt={completedReceipt}
        storeName={storeName}
        currencySymbol={currencySymbol}
        onClose={() => setCompletedReceipt(null)}
        onPrint={handlePrintReceipt}
      />

      <ShiftLifecycleDialog
        isOpen={showShiftDialog}
        onOpenChange={setShowShiftDialog}
        activeSession={activeSession}
        currencySymbol={currencySymbol}
        onSessionChanged={setActiveSession}
      />

      <DrawerCashInOutDialog
        isOpen={showCashInOut}
        onOpenChange={setShowCashInOut}
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
          void handleBarcodeScanned(code);
        }}
      />
    </SafeAreaView>
  );
}
