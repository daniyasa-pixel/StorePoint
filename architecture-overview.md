# Architecture Overview — StorePoint POS

**Author:** Architecture Reviewer Agent  
**Date:** 2026-11-03  
**Status:** Validated Architecture  

---

## 1. Recommended Architecture & System Design
StorePoint employs an **Offline-First Single-Process MVVM Architecture** optimized for high reliability, instant UI responsiveness, and strict zero-cloud dependency:

```
┌────────────────────────────────────────────────────────┐
│           Jetpack Compose UI Layer (M3 Expressive)     │
│   CashierPOSScreen   AdminDashboard   SetupScreen      │
│   InventoryPortal    SuppliersScreen  ShiftDialogs     │
└───────────────────────────▲────────────────────────────┘
                            │ (StateFlow, Immutable UI state)
┌───────────────────────────┴────────────────────────────┐
│                    StorePointViewModel                  │
│       StorePointViewModelCart / Inventory / Hardware    │
│       StorePointViewModelReconciliation / Purchases    │
└─────────────▲──────────────────────────────▲───────────┘
              │                              │
┌─────────────┴──────────────┐ ┌─────────────┴───────────┐
│     StorePointRepository   │ │   Utility Engines       │
│ Room DAOs & Transact Blocks│ │ EscPosHelper, PdfExport │
│ CacheSize, WAL Checkpoints │ │ BiometricAuthHelper     │
└─────────────▲──────────────┘ │ DatabaseBackupManager   │
              │                └─────────────────────────┘
┌─────────────┴──────────────┐
│  SQLite 3.x (Room DB v18)  │
│  WAL Mode, In-Memory Temp  │
│  Foreign Keys Active       │
└────────────────────────────┘
```

### Architectural Tenets
1. **Unconditional Local Persistence**: Every financial transaction, drawer movement, inventory deduct, and bill schedule writes synchronously to SQLite before UI confirmation is dispatched.
2. **Modularized ViewModel Extensions**: Core ViewModel logic is partitioned across focused domain files (`StorePointViewModelCart.kt`, `StorePointViewModelInventory.kt`, `StorePointViewModelHardware.kt`, `StorePointViewModelReconciliation.kt`, `StorePointViewModelPurchases.kt`, `StorePointViewModelBackup.kt`, `StorePointViewModelUpdates.kt`), keeping state predictable while avoiding monolithic class complexity.
3. **Hardware Isolation**: Low-level hardware drivers (USB OTG endpoints, Bluetooth serial RFCOMM sockets, CameraX barcode analyzers) are decoupled behind clean utility helpers (`UsbPrinterHelper`, `EscPosHelper`, `BiometricAuthHelper`), allowing pure unit testing of byte buffers and protocols.

---

## 2. Key Components & Responsibilities
- **`AppDatabase` & DAOs ([`AppDatabase.kt`](file:///home/jeffs/projects/StorePoint/app/src/main/java/com/munzo/storepoint/data/AppDatabase.kt))**:
  - Version 18 schema managing 17 relational entities.
  - WAL journal mode with automated 1,000-page autocheckpoint and 128MB cache size for fast sub-5ms transaction writes.
  - Foreign key enforcement with indexed relationship columns (`transaction_items`, `drawer_transactions`, `parked_transaction_items`).
- **`EscPosHelper` & Printing Engine ([`EscPosHelper.kt`](file:///home/jeffs/projects/StorePoint/app/src/main/java/com/munzo/storepoint/util/EscPosHelper.kt))**:
  - Generates raw binary ESC/POS command buffers for 58mm (32 chars/line) and 80mm (48 chars/line) thermal heads.
  - Code 128 / Code 39 barcode formatting (`GS k 73`), pulse generation for drawer kick (`ESC p 0 25 250`), paper cutting (`GS V 66 0`), and dynamic font sizing.
- **`DatabaseBackupManager` ([`DatabaseBackupManager.kt`](file:///home/jeffs/projects/StorePoint/app/src/main/java/com/munzo/storepoint/util/DatabaseBackupManager.kt))**:
  - Full relational JSON schema serialization and deserialization.
  - Optional PBKDF2-HMAC-SHA256 (120,000 iterations) + AES-256-CBC envelope encryption with HMAC-SHA256 integrity tag for data-at-rest protection.
  - Silent persistent auto-snapshot vault (`storepoint_vault_latest.json`) preserving transactional state across uninstalls.
- **`KioskLockdownPolicy` & Security ([`KioskLockdownPolicy.kt`](file:///home/jeffs/projects/StorePoint/app/src/main/java/com/munzo/storepoint/util/KioskLockdownPolicy.kt))**:
  - Progressive exponential backoff PIN lockout policy.
  - Device Administrator integration blocking package uninstall, app pinning (`startLockTask()`), and allowed application whitelisting.

---

## 3. Main Technical Risks
1. **Bluetooth Socket Dropouts on Mobile Thermal Printers**: Cheap 58mm Bluetooth printers frequently drop RFCOMM connections when idle for power saving. `EscPosHelper` must gracefully reconnect without blocking the main Compose UI thread or hanging the cashier checkout dialog.
2. **Large JSON Serialization Memory Spikes**: On 1GB RAM Android Go devices, exporting 50,000+ historical transactions into a single in-memory JSON string could risk an `OutOfMemoryError`. Chunked streaming or SQLite file-level backup (`VACUUM INTO`) should be planned for high-volume stores.
3. **USB OTG Device Permission Revocation**: Android may revoke USB device permissions if an OTG cable is jostled on the counter. The app should cache USB vendor/product IDs and auto-re-prompt on peripheral reconnect.

---

## 4. What Can Be Deferred Safely (Post-Pilot Roadmap)
- **Multi-Terminal LAN Sync**: Multi-register peer-to-peer sync via Wi-Fi/mDNS can be deferred until after single-terminal retail pilots succeed.
- **Cloud Analytics Dashboard**: Web portal for remote owners can be deferred; local PDF/CSV export and Quick Share file transfers fully satisfy merchant reporting needs today.
- **Automated SQLite Vacuuming**: Full database VACUUM can be deferred to an automated monthly maintenance job.

---

## 5. Build-Order & QA Implications
- Future modifications to database entities MUST include automated Room migration scripts tested against version 18 schema (`app/schemas/com.munzo.storepoint.data.AppDatabase/18.json`).
- Changes to `EscPosHelper` must preserve exact character line widths (32 chars for 58mm, 48 chars for 80mm) to prevent text wrapping on physical thermal paper.

