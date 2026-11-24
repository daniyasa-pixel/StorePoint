import React from 'react';
import { Text, View } from 'react-native';
import {
  Button,
  Card,
  Chip,
  Input,
  Label,
  Separator,
  Surface,
  Switch,
  TextField,
} from 'heroui-native';
import { TactileButton, TactilePressable } from '@/components/TactilePressable';
import type { StorePointRepositories } from '@/db/repositories';
import { DEFAULT_CURRENCY_SYMBOL } from '@/domain/money';
import { useWindowLayout } from '@/layout/useWindowLayout';

export const SETUP_CURRENCIES = [DEFAULT_CURRENCY_SYMBOL, '$', '€', '£', '¥'] as const;

export function parseNonNegativeSetupNumber(raw: string, label: string): number {
  const trimmed = raw.trim();
  if (!trimmed) return 0;
  const parsed = Number.parseFloat(trimmed);
  if (!Number.isFinite(parsed) || parsed < 0) {
    throw new Error(`${label} must be a valid non-negative number.`);
  }
  return parsed;
}

export async function seedStarterSampleCatalog(repos: StorePointRepositories): Promise<void> {
  const [bevId, snackId, pantryId, careId] = await Promise.all([
    repos.catalog.saveCategory({ name: 'Beverages', colorAccent: '#006FEE', iconBadge: 'cup', isArchived: false }),
    repos.catalog.saveCategory({ name: 'Snacks', colorAccent: '#F5A524', iconBadge: 'cookie', isArchived: false }),
    repos.catalog.saveCategory({ name: 'Pantry', colorAccent: '#17C964', iconBadge: 'basket', isArchived: false }),
    repos.catalog.saveCategory({ name: 'Personal Care', colorAccent: '#9353D3', iconBadge: 'sparkles', isArchived: false }),
  ]);
  const samples = [
    { name: 'Bottled Water 500ml', categoryId: bevId, price: 15, cost: 9, stockCount: 48, barcode: '480001001' },
    { name: 'Iced Coffee Can 240ml', categoryId: bevId, price: 38, cost: 26, stockCount: 24, barcode: '480001002' },
    { name: 'Salted Potato Crisps 60g', categoryId: snackId, price: 25, cost: 17, stockCount: 36, barcode: '480002001' },
    { name: 'Instant Pancit Canton', categoryId: pantryId, price: 18, cost: 13, stockCount: 60, barcode: '480003001' },
    { name: 'Bath Soap Bar 90g', categoryId: careId, price: 32, cost: 22, stockCount: 20, barcode: '480004001' },
  ] as const;
  for (const s of samples) {
    await repos.catalog.saveProduct({
      ...s, expirationDate: null, hasStick10s: false, priceStick10s: 0, hasStick20s: false, priceStick20s: 0,
      hasReam: false, priceReam: 0, hasMasterCase: false, priceMasterCase: 0, hasCustomUom: false,
      customUomName: 'Pack', customUomMultiplier: 1, customUomPrice: 0, barcode10s: '', barcode20s: '',
      barcodeReam: '', barcodeMasterCase: '', barcodeCustomUom: '', baseUom: 'pc', trackInventory: true, isArchived: false,
    });
  }
}

export interface SetupProfileFormValues {
  readonly storeName: string;
  readonly currencySymbol: string;
  readonly taxPercentage: string;
  readonly hasGCash: boolean;
  readonly hasMaya: boolean;
  readonly hasLoad: boolean;
  readonly gcashBalance: string;
  readonly smartLoadBalance: string;
  readonly globeLoadBalance: string;
  readonly loadServiceFee: string;
  readonly gcashServiceFee: string;
  readonly mayaServiceFee: string;
  readonly mayaBankFee: string;
  readonly seedStarterCatalog: boolean;
}

export interface SetupStepOneProfileProps {
  readonly values: SetupProfileFormValues;
  readonly onChange: <K extends keyof SetupProfileFormValues>(
    field: K,
    value: SetupProfileFormValues[K],
  ) => void;
  readonly onNext: () => void;
}

const DIGITAL_SERVICE_TILES: ReadonlyArray<{
  readonly key: 'hasGCash' | 'hasMaya' | 'hasLoad';
  readonly title: string;
  readonly description: string;
}> = [
  {
    key: 'hasGCash',
    title: 'GCash Retailing',
    description: 'Cash-in, cash-out, and e-wallet ledger tracking with automatic service fees.',
  },
  {
    key: 'hasMaya',
    title: 'Maya Services',
    description: 'Maya wallet transfers, QR payments, and InstaPay bank transfer surcharges.',
  },
  {
    key: 'hasLoad',
    title: 'Prepaid Mobile Load',
    description: 'Smart, TNT, DITO, Globe & TM e-load retailing with dedicated wallet balances.',
  },
];

export function SetupStepOneProfile({ values, onChange, onNext }: SetupStepOneProfileProps) {
  const { isExpanded } = useWindowLayout();
  const anyDigitalEnabled = values.hasGCash || values.hasMaya || values.hasLoad;
  const showWalletBalances = values.hasGCash || values.hasLoad;

  return (
    <Card>
      <Card.Header>
        <Card.Title>1. Store Profile & Money</Card.Title>
        <Card.Description>
          Configure your store identity, operational currency, VAT, and digital retail wallets.
        </Card.Description>
      </Card.Header>

      <Card.Body className="gap-4 pt-4">
        <View className={isExpanded ? 'flex-row items-start gap-4' : 'gap-4'}>
          <View className={isExpanded ? 'flex-1' : 'w-full'}>
            <TextField isRequired>
              <Label>Store / Business Name</Label>
              <Input
                value={values.storeName}
                onChangeText={(v) => onChange('storeName', v)}
                placeholder="e.g. Munzo Sari-Sari & Mini Mart"
              />
            </TextField>
          </View>

          <View className={isExpanded ? 'flex-1 gap-2' : 'gap-2'}>
            <Label>Operational Currency</Label>
            <View className="flex-row flex-wrap gap-2">
              {SETUP_CURRENCIES.map((symbol) => {
                const selected = values.currencySymbol === symbol;
                return (
                  <Chip
                    key={symbol}
                    size="md"
                    color={selected ? 'accent' : 'default'}
                    variant={selected ? 'primary' : 'soft'}
                    accessibilityRole="button"
                    accessibilityState={{ selected }}
                    accessibilityLabel={`Select currency ${symbol}`}
                    onPress={() => onChange('currencySymbol', symbol)}
                  >
                    <Chip.Label>{symbol}</Chip.Label>
                  </Chip>
                );
              })}
            </View>
          </View>
        </View>

        <View className="gap-1.5">
          <TextField>
            <Label>Value-Added Tax (%)</Label>
            <Input
              value={values.taxPercentage}
              onChangeText={(v) => onChange('taxPercentage', v)}
              keyboardType="decimal-pad"
              placeholder="12.0"
            />
          </TextField>
          <Text className="text-muted text-xs">
            Extracted inclusively on receipts (default 12.0% Philippine VAT; set 0 for non-VAT stores).
          </Text>
        </View>

        <Separator />

        <View className="gap-2.5">
          <View>
            <Text className="text-foreground text-sm font-semibold">
              Digital Services Retailing
            </Text>
            <Text className="text-muted text-xs">
              Toggle the e-wallet and prepaid load services your counter offers.
            </Text>
          </View>

          {DIGITAL_SERVICE_TILES.map((tile) => {
            const isSelected = values[tile.key];
            return (
              <Surface
                key={tile.key}
                variant="secondary"
                className={`rounded-xl p-3.5 flex-row items-center justify-between gap-3 border ${
                  isSelected ? 'border-accent bg-accent/5' : 'border-border/60'
                }`}
              >
                <TactilePressable
                  feedbackVariant="subtle"
                  className="flex-1"
                  accessibilityRole="button"
                  accessibilityLabel={`Toggle ${tile.title}`}
                  onPress={() => onChange(tile.key, !isSelected)}
                >
                  <View className="gap-0.5">
                    <Text className="text-foreground text-sm font-semibold">{tile.title}</Text>
                    <Text className="text-muted text-xs leading-4">{tile.description}</Text>
                  </View>
                </TactilePressable>
                <Switch
                  isSelected={isSelected}
                  onSelectedChange={(next) => onChange(tile.key, next)}
                />
              </Surface>
            );
          })}
        </View>

        {showWalletBalances ? (
          <Surface variant="secondary" className="rounded-xl p-3.5 gap-3 border border-border/60">
            <View>
              <Text className="text-foreground text-sm font-semibold">Initial Wallet Balances</Text>
              <Text className="text-muted text-xs">
                Starting e-wallet float ({values.currencySymbol}) recorded for shift tracking.
              </Text>
            </View>
            <View className={isExpanded ? 'flex-row gap-3' : 'gap-3'}>
              {values.hasGCash ? (
                <View className={isExpanded ? 'flex-1' : 'w-full'}>
                  <TextField>
                    <Label>GCash Balance ({values.currencySymbol})</Label>
                    <Input
                      value={values.gcashBalance}
                      onChangeText={(v) => onChange('gcashBalance', v)}
                      keyboardType="decimal-pad"
                      placeholder="0.00"
                    />
                  </TextField>
                </View>
              ) : null}
              {values.hasLoad ? (
                <>
                  <View className={isExpanded ? 'flex-1' : 'w-full'}>
                    <TextField>
                      <Label>Smart / TNT Load ({values.currencySymbol})</Label>
                      <Input
                        value={values.smartLoadBalance}
                        onChangeText={(v) => onChange('smartLoadBalance', v)}
                        keyboardType="decimal-pad"
                        placeholder="0.00"
                      />
                    </TextField>
                  </View>
                  <View className={isExpanded ? 'flex-1' : 'w-full'}>
                    <TextField>
                      <Label>Globe / TM Load ({values.currencySymbol})</Label>
                      <Input
                        value={values.globeLoadBalance}
                        onChangeText={(v) => onChange('globeLoadBalance', v)}
                        keyboardType="decimal-pad"
                        placeholder="0.00"
                      />
                    </TextField>
                  </View>
                </>
              ) : null}
            </View>
          </Surface>
        ) : null}

        {anyDigitalEnabled ? (
          <Surface variant="secondary" className="rounded-xl p-3.5 gap-3 border border-border/60">
            <View>
              <Text className="text-foreground text-sm font-semibold">Customer Service Fee Tuning</Text>
              <Text className="text-muted text-xs">
                Default convenience fees ({values.currencySymbol}) added per digital transaction.
              </Text>
            </View>
            <View className="flex-row flex-wrap gap-3">
              {(
                [
                  { show: values.hasLoad, key: 'loadServiceFee', label: 'Load Fee', placeholder: '2.00' },
                  { show: values.hasGCash, key: 'gcashServiceFee', label: 'GCash Fee', placeholder: '10.00' },
                  { show: values.hasMaya, key: 'mayaServiceFee', label: 'Maya Wallet Fee', placeholder: '10.00' },
                  { show: values.hasMaya, key: 'mayaBankFee', label: 'Maya Bank Fee', placeholder: '15.00' },
                ] as const
              )
                .filter((item) => item.show)
                .map(({ key, label, placeholder }) => (
                  <View key={key} className="min-w-[130px] flex-1">
                    <TextField>
                      <Label>{label}</Label>
                      <Input
                        value={values[key]}
                        onChangeText={(v) => onChange(key, v)}
                        keyboardType="decimal-pad"
                        placeholder={placeholder}
                      />
                    </TextField>
                  </View>
                ))}
            </View>
          </Surface>
        ) : null}

        <Surface variant="secondary" className="rounded-xl p-3.5 border border-border/60">
          <View className="flex-row items-center justify-between gap-3">
            <View className="flex-1 gap-0.5">
              <Text className="text-foreground text-sm font-semibold">
                Seed Starter Sample Catalog
              </Text>
              <Text className="text-muted text-xs leading-4">
                Adds Beverages, Snacks, Pantry & Personal Care with starter items so your POS works immediately.
              </Text>
            </View>
            <Switch
              isSelected={values.seedStarterCatalog}
              onSelectedChange={(next) => onChange('seedStarterCatalog', next)}
            />
          </View>
        </Surface>
      </Card.Body>

      <Card.Footer className="pt-4">
        <TactileButton feedbackVariant="stretch" variant="primary" size="lg" fullWidth onPress={onNext}>
          <Button.Label>Next: Security & Admin →</Button.Label>
        </TactileButton>
      </Card.Footer>
    </Card>
  );
}
