/**
 * StorePoint SQLite Schema v19 — 100% column, FK, and index parity with Kotlin Room AppDatabase v19.
 * All 22 relational tables + hot-path POS & analytics indices.
 */
export const SCHEMA_VERSION = 19;

export const SCHEMA_V19_STATEMENTS: readonly string[] = [
  `CREATE TABLE IF NOT EXISTS store_config (
    id INTEGER PRIMARY KEY NOT NULL,
    storeName TEXT NOT NULL,
    currencySymbol TEXT NOT NULL,
    taxPercentage REAL NOT NULL DEFAULT 12.0,
    setupCompleted INTEGER NOT NULL DEFAULT 0,
    gcashBalance REAL NOT NULL DEFAULT 0.0,
    hasGCash INTEGER NOT NULL DEFAULT 1,
    hasMaya INTEGER NOT NULL DEFAULT 1,
    hasLoad INTEGER NOT NULL DEFAULT 1,
    smartLoadBalance REAL NOT NULL DEFAULT 0.0,
    globeLoadBalance REAL NOT NULL DEFAULT 0.0,
    loadServiceFee REAL NOT NULL DEFAULT 2.0,
    gcashServiceFee REAL NOT NULL DEFAULT 10.0,
    mayaServiceFee REAL NOT NULL DEFAULT 10.0,
    mayaBankFee REAL NOT NULL DEFAULT 15.0
  );`,

  `CREATE TABLE IF NOT EXISTS user_accounts (
    username TEXT PRIMARY KEY NOT NULL,
    pinHash TEXT NOT NULL,
    role TEXT NOT NULL,
    barcodeId TEXT NOT NULL DEFAULT '',
    biometricEnrolled INTEGER NOT NULL DEFAULT 0,
    pinResetRequired INTEGER NOT NULL DEFAULT 0
  );`,

  `CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    colorAccent TEXT NOT NULL DEFAULT '',
    iconBadge TEXT NOT NULL DEFAULT '',
    isArchived INTEGER NOT NULL DEFAULT 0
  );`,

  `CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    categoryId INTEGER NOT NULL,
    price REAL NOT NULL,
    stockCount INTEGER NOT NULL,
    barcode TEXT NOT NULL,
    expirationDate TEXT,
    cost REAL NOT NULL DEFAULT 0.0,
    hasStick10s INTEGER NOT NULL DEFAULT 0,
    priceStick10s REAL NOT NULL DEFAULT 0.0,
    hasStick20s INTEGER NOT NULL DEFAULT 0,
    priceStick20s REAL NOT NULL DEFAULT 0.0,
    hasReam INTEGER NOT NULL DEFAULT 0,
    priceReam REAL NOT NULL DEFAULT 0.0,
    hasMasterCase INTEGER NOT NULL DEFAULT 0,
    priceMasterCase REAL NOT NULL DEFAULT 0.0,
    hasCustomUom INTEGER NOT NULL DEFAULT 0,
    customUomName TEXT NOT NULL DEFAULT 'Pack',
    customUomMultiplier INTEGER NOT NULL DEFAULT 1,
    customUomPrice REAL NOT NULL DEFAULT 0.0,
    barcode10s TEXT NOT NULL DEFAULT '',
    barcode20s TEXT NOT NULL DEFAULT '',
    barcodeReam TEXT NOT NULL DEFAULT '',
    barcodeMasterCase TEXT NOT NULL DEFAULT '',
    barcodeCustomUom TEXT NOT NULL DEFAULT '',
    baseUom TEXT NOT NULL DEFAULT 'pc',
    trackInventory INTEGER NOT NULL DEFAULT 1,
    isArchived INTEGER NOT NULL DEFAULT 0
  );`,
  `CREATE INDEX IF NOT EXISTS index_products_categoryId ON products(categoryId);`,
  `CREATE INDEX IF NOT EXISTS index_products_barcode ON products(barcode);`,
  `CREATE INDEX IF NOT EXISTS index_products_name ON products(name);`,
  `CREATE INDEX IF NOT EXISTS index_products_stockCount ON products(stockCount);`,
  `CREATE INDEX IF NOT EXISTS index_products_isArchived_name ON products(isArchived, name);`,

  `CREATE TABLE IF NOT EXISTS product_variants (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    productId INTEGER NOT NULL,
    variantName TEXT NOT NULL,
    uomName TEXT NOT NULL DEFAULT 'pc',
    multiplier INTEGER NOT NULL DEFAULT 1,
    price REAL NOT NULL,
    cost REAL NOT NULL DEFAULT 0.0,
    barcode TEXT NOT NULL DEFAULT '',
    isDefault INTEGER NOT NULL DEFAULT 0,
    stockCount INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(productId) REFERENCES products(id) ON DELETE CASCADE
  );`,
  `CREATE INDEX IF NOT EXISTS index_product_variants_productId ON product_variants(productId);`,
  `CREATE INDEX IF NOT EXISTS index_product_variants_barcode ON product_variants(barcode);`,

  `CREATE TABLE IF NOT EXISTS cashier_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    cashierUsername TEXT NOT NULL,
    startTime INTEGER NOT NULL,
    endTime INTEGER,
    startingCash REAL NOT NULL,
    endingCash REAL,
    status TEXT NOT NULL DEFAULT 'ACTIVE'
  );`,

  `CREATE TABLE IF NOT EXISTS drawer_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    sessionId INTEGER NOT NULL,
    timestamp INTEGER NOT NULL,
    cashierUsername TEXT NOT NULL,
    amount REAL NOT NULL,
    type TEXT NOT NULL,
    reason TEXT NOT NULL,
    FOREIGN KEY(sessionId) REFERENCES cashier_sessions(id) ON DELETE CASCADE
  );`,
  `CREATE INDEX IF NOT EXISTS index_drawer_transactions_sessionId ON drawer_transactions(sessionId);`,

  `CREATE TABLE IF NOT EXISTS wallet_ledger (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    walletType TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    actorUsername TEXT NOT NULL,
    type TEXT NOT NULL,
    delta REAL NOT NULL,
    balanceAfter REAL NOT NULL,
    reference TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT ''
  );`,
  `CREATE INDEX IF NOT EXISTS index_wallet_ledger_walletType ON wallet_ledger(walletType);`,
  `CREATE INDEX IF NOT EXISTS index_wallet_ledger_timestamp ON wallet_ledger(timestamp);`,

  `CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    timestamp INTEGER NOT NULL,
    cashierUsername TEXT NOT NULL,
    subtotal REAL NOT NULL,
    taxAmount REAL NOT NULL,
    totalAmount REAL NOT NULL,
    paymentMethod TEXT NOT NULL,
    cashPaid REAL NOT NULL,
    changeAmount REAL NOT NULL,
    status TEXT NOT NULL DEFAULT 'COMPLETED',
    customerName TEXT NOT NULL DEFAULT ''
  );`,
  `CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions(timestamp);`,
  `CREATE INDEX IF NOT EXISTS index_transactions_cashierUsername ON transactions(cashierUsername);`,
  `CREATE INDEX IF NOT EXISTS index_transactions_paymentMethod_timestamp ON transactions(paymentMethod, timestamp);`,

  `CREATE TABLE IF NOT EXISTS transaction_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    transactionId INTEGER NOT NULL,
    productId INTEGER NOT NULL,
    productName TEXT NOT NULL,
    price REAL NOT NULL,
    quantity INTEGER NOT NULL,
    cost REAL NOT NULL DEFAULT 0.0,
    uomName TEXT NOT NULL DEFAULT 'pc',
    FOREIGN KEY(transactionId) REFERENCES transactions(id) ON DELETE CASCADE
  );`,
  `CREATE INDEX IF NOT EXISTS index_transaction_items_transactionId ON transaction_items(transactionId);`,
  `CREATE INDEX IF NOT EXISTS index_transaction_items_productId ON transaction_items(productId);`,
  `CREATE INDEX IF NOT EXISTS index_transaction_items_productId_transactionId ON transaction_items(productId, transactionId);`,

  `CREATE TABLE IF NOT EXISTS return_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    originalTransactionId INTEGER NOT NULL,
    timestamp INTEGER NOT NULL,
    cashierUsername TEXT NOT NULL,
    totalRefundAmount REAL NOT NULL,
    refundMethod TEXT NOT NULL DEFAULT 'CASH',
    returnReason TEXT NOT NULL DEFAULT 'Defective / Spoiled',
    customerName TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    FOREIGN KEY(originalTransactionId) REFERENCES transactions(id) ON DELETE CASCADE
  );`,
  `CREATE INDEX IF NOT EXISTS index_return_transactions_originalTransactionId ON return_transactions(originalTransactionId);`,

  `CREATE TABLE IF NOT EXISTS return_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    returnTransactionId INTEGER NOT NULL,
    productId INTEGER NOT NULL,
    productName TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    unitPrice REAL NOT NULL,
    refundAmount REAL NOT NULL,
    uomName TEXT NOT NULL DEFAULT 'pc',
    restockToInventory INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY(returnTransactionId) REFERENCES return_transactions(id) ON DELETE CASCADE
  );`,
  `CREATE INDEX IF NOT EXISTS index_return_items_returnTransactionId ON return_items(returnTransactionId);`,
  `CREATE INDEX IF NOT EXISTS index_return_items_productId ON return_items(productId);`,

  `CREATE TABLE IF NOT EXISTS suppliers (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    contactPerson TEXT NOT NULL DEFAULT '',
    phone TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL DEFAULT '',
    address TEXT NOT NULL DEFAULT '',
    deliverySchedule TEXT NOT NULL DEFAULT '',
    paymentTerms TEXT NOT NULL DEFAULT 'Cash on Delivery',
    notes TEXT NOT NULL DEFAULT ''
  );`,

  `CREATE TABLE IF NOT EXISTS purchase_orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    poNumber TEXT NOT NULL,
    supplierId INTEGER NOT NULL,
    supplierName TEXT NOT NULL,
    orderDate INTEGER NOT NULL,
    expectedDeliveryDate INTEGER,
    receivedDate INTEGER,
    totalCost REAL NOT NULL,
    status TEXT NOT NULL DEFAULT 'ORDERED',
    paymentStatus TEXT NOT NULL DEFAULT 'UNPAID',
    notes TEXT NOT NULL DEFAULT '',
    FOREIGN KEY(supplierId) REFERENCES suppliers(id) ON DELETE RESTRICT
  );`,
  `CREATE INDEX IF NOT EXISTS index_purchase_orders_supplierId ON purchase_orders(supplierId);`,

  `CREATE TABLE IF NOT EXISTS purchase_order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    purchaseOrderId INTEGER NOT NULL,
    productId INTEGER NOT NULL,
    productName TEXT NOT NULL,
    quantityOrdered INTEGER NOT NULL,
    quantityReceived INTEGER NOT NULL DEFAULT 0,
    unitCost REAL NOT NULL,
    totalCost REAL NOT NULL,
    uomName TEXT NOT NULL DEFAULT 'pc',
    FOREIGN KEY(purchaseOrderId) REFERENCES purchase_orders(id) ON DELETE CASCADE
  );`,
  `CREATE INDEX IF NOT EXISTS index_purchase_order_items_purchaseOrderId ON purchase_order_items(purchaseOrderId);`,
  `CREATE INDEX IF NOT EXISTS index_purchase_order_items_productId ON purchase_order_items(productId);`,

  `CREATE TABLE IF NOT EXISTS parked_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    timestamp INTEGER NOT NULL,
    note TEXT NOT NULL
  );`,

  `CREATE TABLE IF NOT EXISTS parked_transaction_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    parkedTransactionId INTEGER NOT NULL,
    productId INTEGER NOT NULL,
    productName TEXT NOT NULL,
    price REAL NOT NULL,
    quantity INTEGER NOT NULL,
    FOREIGN KEY(parkedTransactionId) REFERENCES parked_transactions(id) ON DELETE CASCADE
  );`,
  `CREATE INDEX IF NOT EXISTS index_parked_transaction_items_parkedTransactionId ON parked_transaction_items(parkedTransactionId);`,
  `CREATE INDEX IF NOT EXISTS index_parked_transaction_items_productId ON parked_transaction_items(productId);`,

  `CREATE TABLE IF NOT EXISTS payment_schedules (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    title TEXT NOT NULL,
    amount REAL NOT NULL,
    dueDate TEXT NOT NULL,
    category TEXT NOT NULL,
    priority TEXT NOT NULL,
    isPaid INTEGER NOT NULL DEFAULT 0,
    paymentMethod TEXT NOT NULL DEFAULT 'CASH'
  );`,

  `CREATE TABLE IF NOT EXISTS shift_reconciliations (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    sessionId INTEGER NOT NULL,
    cashierUsername TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    count1000 INTEGER NOT NULL DEFAULT 0,
    count500 INTEGER NOT NULL DEFAULT 0,
    count200 INTEGER NOT NULL DEFAULT 0,
    count100 INTEGER NOT NULL DEFAULT 0,
    count50 INTEGER NOT NULL DEFAULT 0,
    count20 INTEGER NOT NULL DEFAULT 0,
    countCoins REAL NOT NULL DEFAULT 0.0,
    totalCountedCash REAL NOT NULL DEFAULT 0.0,
    expectedCash REAL NOT NULL DEFAULT 0.0,
    cashVariance REAL NOT NULL DEFAULT 0.0,
    countedGCash REAL NOT NULL DEFAULT 0.0,
    expectedGCash REAL NOT NULL DEFAULT 0.0,
    gcashVariance REAL NOT NULL DEFAULT 0.0,
    countedSmartLoad REAL NOT NULL DEFAULT 0.0,
    expectedSmartLoad REAL NOT NULL DEFAULT 0.0,
    smartLoadVariance REAL NOT NULL DEFAULT 0.0,
    countedGlobeLoad REAL NOT NULL DEFAULT 0.0,
    expectedGlobeLoad REAL NOT NULL DEFAULT 0.0,
    globeLoadVariance REAL NOT NULL DEFAULT 0.0,
    hasDiscrepancy INTEGER NOT NULL DEFAULT 0,
    managerOverrideApproved INTEGER NOT NULL DEFAULT 0,
    managerUsername TEXT NOT NULL DEFAULT '',
    discrepancyReason TEXT NOT NULL DEFAULT '',
    receiptPrinted INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(sessionId) REFERENCES cashier_sessions(id) ON DELETE CASCADE
  );`,
  `CREATE INDEX IF NOT EXISTS index_shift_reconciliations_sessionId ON shift_reconciliations(sessionId);`,
  `CREATE INDEX IF NOT EXISTS index_shift_reconciliations_timestamp ON shift_reconciliations(timestamp);`,

  `CREATE TABLE IF NOT EXISTS bill_of_materials (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    rawMaterialProductId INTEGER NOT NULL,
    targetRetailProductId INTEGER NOT NULL,
    rawQuantityNeeded REAL NOT NULL,
    producedRetailYield REAL NOT NULL,
    wasteLossPercentage REAL NOT NULL DEFAULT 0.0,
    notes TEXT NOT NULL DEFAULT '',
    FOREIGN KEY(rawMaterialProductId) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY(targetRetailProductId) REFERENCES products(id) ON DELETE CASCADE
  );`,
  `CREATE INDEX IF NOT EXISTS index_bill_of_materials_rawMaterialProductId ON bill_of_materials(rawMaterialProductId);`,
  `CREATE INDEX IF NOT EXISTS index_bill_of_materials_targetRetailProductId ON bill_of_materials(targetRetailProductId);`,

  `CREATE TABLE IF NOT EXISTS production_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    productionCode TEXT NOT NULL,
    rawMaterialProductId INTEGER NOT NULL,
    rawMaterialName TEXT NOT NULL,
    rawMaterialQtyUsed REAL NOT NULL,
    rawMaterialUnitCost REAL NOT NULL,
    targetRetailProductId INTEGER NOT NULL,
    targetRetailProductName TEXT NOT NULL,
    targetRetailQtyProduced REAL NOT NULL,
    derivedRetailUnitCost REAL NOT NULL,
    retailSellingPrice REAL NOT NULL,
    wasteLossQty REAL NOT NULL DEFAULT 0.0,
    operatorUsername TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    notes TEXT NOT NULL DEFAULT ''
  );`,
  `CREATE INDEX IF NOT EXISTS index_production_runs_productionCode ON production_runs(productionCode);`,
  `CREATE INDEX IF NOT EXISTS index_production_runs_rawMaterialProductId ON production_runs(rawMaterialProductId);`,
  `CREATE INDEX IF NOT EXISTS index_production_runs_targetRetailProductId ON production_runs(targetRetailProductId);`,
  `CREATE INDEX IF NOT EXISTS index_production_runs_timestamp ON production_runs(timestamp);`,

  `CREATE TABLE IF NOT EXISTS inventory_movements (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    productId INTEGER NOT NULL,
    productName TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    actorUsername TEXT NOT NULL,
    movementType TEXT NOT NULL,
    quantityDelta REAL NOT NULL,
    stockBefore INTEGER NOT NULL,
    stockAfter INTEGER NOT NULL,
    reference TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT ''
  );`,
  `CREATE INDEX IF NOT EXISTS index_inventory_movements_productId ON inventory_movements(productId);`,
  `CREATE INDEX IF NOT EXISTS index_inventory_movements_timestamp ON inventory_movements(timestamp);`,
];
