import React, { useRef, useState } from 'react';
import { ScrollView, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import {
  Alert,
  BottomSheet,
  type BottomSheetRef,
  Button,
  Chip,
  Separator,
  Surface,
  useToast,
} from 'heroui-native';
import { AppTopBar } from '@/components/AppTopBar';
import { TactilePressable } from '@/components/TactilePressable';
import { APP_ROUTES } from '@/constants/routes';
import { AdminAboutTab } from '@/features/admin/AdminAboutTab';
import { AdminAnalyticsTab } from '@/features/admin/AdminAnalyticsTab';
import { AdminCalendarTab } from '@/features/admin/AdminCalendarTab';
import { AdminCategoriesTab } from '@/features/admin/AdminCategoriesTab';
import { AdminProductsTab } from '@/features/admin/AdminProductsTab';
import { AdminSecurityTab } from '@/features/admin/AdminSecurityTab';
import { AdminUsersTab } from '@/features/admin/AdminUsersTab';
import { BulkProductionDialog } from '@/features/production/BulkProductionDialog';
import { SuppliersAndPurchasesTab } from '@/features/suppliers/SuppliersAndPurchasesTab';
import { exportAndShareBackupSnapshot } from '@/backup/backupEngine';
import { exportAuditReportPdf } from '@/reports/pdfReportExporter';
import { useWindowLayout } from '@/layout/useWindowLayout';
import { enterFade } from '@/theme/motion';

type AdminTabId =
  | 'analytics'
  | 'products'
  | 'suppliers'
  | 'categories'
  | 'users'
  | 'calendar'
  | 'security'
  | 'about';

interface AdminTabSpec {
  readonly id: AdminTabId;
  readonly label: string;
  readonly shortLabel: string;
}

const ADMIN_TABS: readonly AdminTabSpec[] = [
  { id: 'analytics', label: 'Analytics', shortLabel: 'Analytics' },
  { id: 'products', label: 'Catalog Inventory', shortLabel: 'Catalog' },
  { id: 'suppliers', label: 'Suppliers & POs', shortLabel: 'Suppliers' },
  { id: 'categories', label: 'Categories', shortLabel: 'Categories' },
  { id: 'users', label: 'Staff Accounts', shortLabel: 'Staff' },
  { id: 'calendar', label: 'Payment Calendar', shortLabel: 'Calendar' },
  { id: 'security', label: 'Security', shortLabel: 'Security' },
  { id: 'about', label: 'About Information', shortLabel: 'About' },
];

const COMPACT_PRIMARY_IDS: readonly AdminTabId[] = ['analytics', 'products', 'suppliers', 'users'];
const COMPACT_MORE_IDS: readonly AdminTabId[] = ['categories', 'calendar', 'security', 'about'];

export default function AdminDashboardRoute() {
  const router = useRouter();
  const { toast } = useToast();
  const { isCompact, layout, containerPaddingClass } = useWindowLayout();
  const [activeTab, setActiveTab] = useState<AdminTabId>('products');
  const [showProduction, setShowProduction] = useState(false);
  const [exportError, setExportError] = useState<string | null>(null);
  const moreSheetRef = useRef<BottomSheetRef>(null);

  const activeSpec = ADMIN_TABS.find((t) => t.id === activeTab) ?? ADMIN_TABS[1]!;

  const handleExportBackup = async () => {
    setExportError(null);
    try {
      await exportAndShareBackupSnapshot();
      toast.show({
        label: 'Backup exported',
        description: 'Database backup snapshot is ready to share.',
        variant: 'success',
      });
    } catch (err) {
      setExportError(err instanceof Error ? err.message : 'Failed to export backup snapshot.');
    }
  };

  const handleExportPdf = async () => {
    setExportError(null);
    try {
      await exportAuditReportPdf();
      toast.show({
        label: 'Audit report exported',
        description: 'PDF audit report generated successfully.',
        variant: 'success',
      });
    } catch (err) {
      setExportError(err instanceof Error ? err.message : 'Failed to export audit report PDF.');
    }
  };

  const renderTab = () => {
    switch (activeTab) {
      case 'analytics':
        return <AdminAnalyticsTab />;
      case 'products':
        return <AdminProductsTab />;
      case 'suppliers':
        return <SuppliersAndPurchasesTab />;
      case 'categories':
        return <AdminCategoriesTab />;
      case 'users':
        return <AdminUsersTab />;
      case 'calendar':
        return <AdminCalendarTab />;
      case 'security':
        return (
          <AdminSecurityTab
            onExportBackup={() => {
              void handleExportBackup();
            }}
          />
        );
      case 'about':
        return <AdminAboutTab />;
    }
  };

  return (
    <SafeAreaView className="bg-background flex-1">
      <AppTopBar
        title="Admin Center"
        subtitle={`${activeSpec.label} • ${layout}`}
        onBack={() => router.replace(APP_ROUTES.CASHIER_POS)}
        rightActions={
          <>
            <Button
              variant="ghost"
              size="sm"
              onPress={() => {
                void handleExportPdf();
              }}
            >
              <Button.Label>PDF</Button.Label>
            </Button>
            <Button variant="tertiary" size="sm" onPress={() => setShowProduction(true)}>
              <Button.Label>BOM</Button.Label>
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onPress={() => router.push(APP_ROUTES.INVENTORY_PORTAL)}
            >
              <Button.Label>Inventory</Button.Label>
            </Button>
          </>
        }
      />

      {!isCompact ? (
        <Surface variant="default">
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerClassName="flex-row gap-2 px-4 py-2.5"
          >
            {ADMIN_TABS.map((tab) => (
              <Chip
                key={tab.id}
                color={activeTab === tab.id ? 'accent' : 'default'}
                variant={activeTab === tab.id ? 'primary' : 'soft'}
                onPress={() => setActiveTab(tab.id)}
              >
                <Chip.Label>{tab.label}</Chip.Label>
              </Chip>
            ))}
          </ScrollView>
          <Separator />
        </Surface>
      ) : null}

      <View className={`flex-1 ${containerPaddingClass} gap-3`}>
        {exportError ? (
          <Alert status="danger">
            <Alert.Indicator />
            <Alert.Content>
              <Alert.Description>{exportError}</Alert.Description>
            </Alert.Content>
            <Button variant="ghost" size="sm" onPress={() => setExportError(null)}>
              <Button.Label>Dismiss</Button.Label>
            </Button>
          </Alert>
        ) : null}
        <Animated.View key={activeTab} entering={enterFade} className="flex-1">
          {renderTab()}
        </Animated.View>
      </View>

      {isCompact ? (
        <Surface variant="default">
          <Separator />
          <View className="flex-row items-center justify-around px-2 py-2 gap-1">
            {COMPACT_PRIMARY_IDS.map((id) => {
              const tab = ADMIN_TABS.find((t) => t.id === id)!;
              const selected = activeTab === id;
              return (
                <TactilePressable
                  key={id}
                  feedbackVariant="spring"
                  className="flex-1"
                  onPress={() => setActiveTab(id)}
                  accessibilityLabel={tab.label}
                >
                  <View pointerEvents="none">
                    <Button
                      variant={selected ? 'primary' : 'ghost'}
                      size="sm"
                      className="w-full"
                    >
                      <Button.Label numberOfLines={1}>{tab.shortLabel}</Button.Label>
                    </Button>
                  </View>
                </TactilePressable>
              );
            })}
            <TactilePressable
              feedbackVariant="spring"
              className="flex-1"
              onPress={() => moreSheetRef.current?.present()}
              accessibilityLabel="More admin sections"
            >
              <View pointerEvents="none">
                <Button
                  variant={COMPACT_MORE_IDS.includes(activeTab) ? 'primary' : 'ghost'}
                  size="sm"
                  className="w-full"
                >
                  <Button.Label numberOfLines={1}>More</Button.Label>
                </Button>
              </View>
            </TactilePressable>
          </View>
        </Surface>
      ) : null}

      <BottomSheet ref={moreSheetRef}>
        <BottomSheet.Content className="p-4 gap-3">
          <BottomSheet.Header>
            <BottomSheet.Title>More Admin Sections</BottomSheet.Title>
          </BottomSheet.Header>
          <BottomSheet.Body className="gap-2 pt-2">
            {COMPACT_MORE_IDS.map((id) => {
              const tab = ADMIN_TABS.find((t) => t.id === id)!;
              return (
                <Button
                  key={id}
                  variant={activeTab === id ? 'primary' : 'secondary'}
                  className="w-full"
                  onPress={() => {
                    setActiveTab(id);
                    moreSheetRef.current?.dismiss();
                  }}
                >
                  <Button.Label>{tab.label}</Button.Label>
                </Button>
              );
            })}
          </BottomSheet.Body>
        </BottomSheet.Content>
      </BottomSheet>

      <BulkProductionDialog isOpen={showProduction} onOpenChange={setShowProduction} />
    </SafeAreaView>
  );
}
