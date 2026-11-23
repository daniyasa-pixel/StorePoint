import type { UserRole } from '@/constants/routes';

export interface StoreConfig {
  readonly id: number;
  readonly storeName: string;
  readonly currencySymbol: string;
  readonly taxPercentage: number;
  readonly setupCompleted: boolean;
  readonly gcashBalance: number;
  readonly hasGCash: boolean;
  readonly hasMaya: boolean;
  readonly hasLoad: boolean;
  readonly smartLoadBalance: number;
  readonly globeLoadBalance: number;
  readonly loadServiceFee: number;
  readonly gcashServiceFee: number;
  readonly mayaServiceFee: number;
  readonly mayaBankFee: number;
}

export interface User {
  readonly username: string;
  readonly pinHash: string;
  readonly role: UserRole;
  readonly barcodeId: string;
  readonly biometricEnrolled: boolean;
  readonly pinResetRequired: boolean;
}

export interface Category {
  readonly id: number;
  readonly name: string;
  readonly colorAccent: string;
  readonly iconBadge: string;
  readonly isArchived: boolean;
}

export interface Product {
  readonly id: number;
  readonly name: string;
  readonly categoryId: number;
  readonly price: number;
  readonly stockCount: number;
  readonly barcode: string;
  readonly expirationDate: string | null;
  readonly cost: number;
  readonly hasStick10s: boolean;
  readonly priceStick10s: number;
  readonly hasStick20s: boolean;
  readonly priceStick20s: number;
  readonly hasReam: boolean;
  readonly priceReam: number;
  readonly hasMasterCase: boolean;
  readonly priceMasterCase: number;
  readonly hasCustomUom: boolean;
  readonly customUomName: string;
  readonly customUomMultiplier: number;
  readonly customUomPrice: number;
  readonly barcode10s: string;
  readonly barcode20s: string;
  readonly barcodeReam: string;
  readonly barcodeMasterCase: string;
  readonly barcodeCustomUom: string;
  readonly baseUom: string;
  readonly trackInventory: boolean;
  readonly isArchived: boolean;
}

export interface ProductVariant {
  readonly id: number;
  readonly productId: number;
  readonly variantName: string;
  readonly uomName: string;
  readonly multiplier: number;
  readonly price: number;
  readonly cost: number;
  readonly barcode: string;
  readonly isDefault: boolean;
  readonly stockCount: number;
}

export interface UomOption {
  readonly name: string;
  readonly multiplier: number;
  readonly price: number;
}

export interface CartItemDetails {
  readonly product: Product;
  readonly quantity: number;
  readonly uomName: string;
  readonly multiplier: number;
  readonly selectedPrice: number;
}

export interface CashierSession {
  readonly id: number;
  readonly cashierUsername: string;
  readonly startTime: number;
  readonly endTime: number | null;
  readonly startingCash: number;
  readonly endingCash: number | null;
  readonly status: 'ACTIVE' | 'CLOSED';
}

export interface DrawerTransaction {
  readonly id: number;
  readonly sessionId: number;
  readonly timestamp: number;
  readonly cashierUsername: string;
  readonly amount: number;
  readonly type: string;
  readonly reason: string;
}

export interface WalletLedgerEntry {
  readonly id: number;
  readonly walletType: 'GCASH' | 'SMART' | 'GLOBE';
  readonly timestamp: number;
  readonly actorUsername: string;
  readonly type: string;
  readonly delta: number;
  readonly balanceAfter: number;
  readonly reference: string;
  readonly notes: string;
}

export interface Transaction {
  readonly id: number;
  readonly timestamp: number;
  readonly cashierUsername: string;
  readonly subtotal: number;
  readonly taxAmount: number;
  readonly totalAmount: number;
  readonly paymentMethod: string;
  readonly cashPaid: number;
  readonly changeAmount: number;
  readonly status: 'COMPLETED' | 'PARTIALLY_REFUNDED' | 'REFUNDED';
  readonly customerName: string;
}

export interface TransactionItem {
  readonly id: number;
  readonly transactionId: number;
  readonly productId: number;
  readonly productName: string;
  readonly price: number;
  readonly quantity: number;
  readonly cost: number;
  readonly uomName: string;
}

export interface ReturnTransaction {
  readonly id: number;
  readonly originalTransactionId: number;
  readonly timestamp: number;
  readonly cashierUsername: string;
  readonly totalRefundAmount: number;
  readonly refundMethod: string;
  readonly returnReason: string;
  readonly customerName: string;
  readonly notes: string;
}

export interface ReturnItem {
  readonly id: number;
  readonly returnTransactionId: number;
  readonly productId: number;
  readonly productName: string;
  readonly quantity: number;
  readonly unitPrice: number;
  readonly refundAmount: number;
  readonly uomName: string;
  readonly restockToInventory: boolean;
}

export interface Supplier {
  readonly id: number;
  readonly name: string;
  readonly contactPerson: string;
  readonly phone: string;
  readonly email: string;
  readonly address: string;
  readonly deliverySchedule: string;
  readonly paymentTerms: string;
  readonly notes: string;
}

export interface PurchaseOrder {
  readonly id: number;
  readonly poNumber: string;
  readonly supplierId: number;
  readonly supplierName: string;
  readonly orderDate: number;
  readonly expectedDeliveryDate: number | null;
  readonly receivedDate: number | null;
  readonly totalCost: number;
  readonly status: 'ORDERED' | 'RECEIVED' | 'CANCELLED';
  readonly paymentStatus: 'UNPAID' | 'PAID';
  readonly notes: string;
}

export interface PurchaseOrderItem {
  readonly id: number;
  readonly purchaseOrderId: number;
  readonly productId: number;
  readonly productName: string;
  readonly quantityOrdered: number;
  readonly quantityReceived: number;
  readonly unitCost: number;
  readonly totalCost: number;
  readonly uomName: string;
}

export interface ParkedTransaction {
  readonly id: number;
  readonly timestamp: number;
  readonly note: string;
}

export interface ParkedTransactionItem {
  readonly id: number;
  readonly parkedTransactionId: number;
  readonly productId: number;
  readonly productName: string;
  readonly price: number;
  readonly quantity: number;
}

export interface PaymentSchedule {
  readonly id: number;
  readonly title: string;
  readonly amount: number;
  readonly dueDate: string;
  readonly category: string;
  readonly priority: 'High' | 'Medium' | 'Low';
  readonly isPaid: boolean;
  readonly paymentMethod: string;
}

export interface ShiftReconciliation {
  readonly id: number;
  readonly sessionId: number;
  readonly cashierUsername: string;
  readonly timestamp: number;
  readonly count1000: number;
  readonly count500: number;
  readonly count200: number;
  readonly count100: number;
  readonly count50: number;
  readonly count20: number;
  readonly countCoins: number;
  readonly totalCountedCash: number;
  readonly expectedCash: number;
  readonly cashVariance: number;
  readonly countedGCash: number;
  readonly expectedGCash: number;
  readonly gcashVariance: number;
  readonly countedSmartLoad: number;
  readonly expectedSmartLoad: number;
  readonly smartLoadVariance: number;
  readonly countedGlobeLoad: number;
  readonly expectedGlobeLoad: number;
  readonly globeLoadVariance: number;
  readonly hasDiscrepancy: boolean;
  readonly managerOverrideApproved: boolean;
  readonly managerUsername: string;
  readonly discrepancyReason: string;
  readonly receiptPrinted: boolean;
}

export interface BillOfMaterials {
  readonly id: number;
  readonly rawMaterialProductId: number;
  readonly targetRetailProductId: number;
  readonly rawQuantityNeeded: number;
  readonly producedRetailYield: number;
  readonly wasteLossPercentage: number;
  readonly notes: string;
}

export interface ProductionLogEntity {
  readonly id: number;
  readonly productionCode: string;
  readonly rawMaterialProductId: number;
  readonly rawMaterialName: string;
  readonly rawMaterialQtyUsed: number;
  readonly rawMaterialUnitCost: number;
  readonly targetRetailProductId: number;
  readonly targetRetailProductName: string;
  readonly targetRetailQtyProduced: number;
  readonly derivedRetailUnitCost: number;
  readonly retailSellingPrice: number;
  readonly wasteLossQty: number;
  readonly operatorUsername: string;
  readonly timestamp: number;
  readonly notes: string;
}

export interface InventoryMovementEntity {
  readonly id: number;
  readonly productId: number;
  readonly productName: string;
  readonly timestamp: number;
  readonly actorUsername: string;
  readonly movementType: string;
  readonly quantityDelta: number;
  readonly stockBefore: number;
  readonly stockAfter: number;
  readonly reference: string;
  readonly notes: string;
}

export type SqlBindValue = string | number | null;

export interface SqlRunResult {
  readonly lastInsertRowId: number;
  readonly changes: number;
}

export interface SqlExecutor {
  execAsync: (sql: string) => Promise<void>;
  runAsync: (sql: string, params?: readonly SqlBindValue[]) => Promise<SqlRunResult>;
  getFirstAsync: <T>(sql: string, params?: readonly SqlBindValue[]) => Promise<T | null>;
  getAllAsync: <T>(sql: string, params?: readonly SqlBindValue[]) => Promise<T[]>;
  withTransactionAsync: <T>(task: () => Promise<T>) => Promise<T>;
}
