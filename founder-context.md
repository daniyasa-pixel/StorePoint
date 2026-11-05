# Founder Context — StorePoint POS

**Last Updated:** 2026-11-03  
**Stage:** Pre-Release Commercial POS Kiosk (Production-Ready Codebase)  
**Target Market:** Small-to-Medium Retailers, Sari-Sari Stores, Convenience Marts, and Micro-Merchants (Philippines / Emerging Markets)

---

## 1. Company & Product Vision
StorePoint is an offline-first commercial point-of-sale (POS) terminal application designed to run on standalone Android hardware (tablets, handheld POS devices like Sunmi/iMin, and commercial retail terminals) without requiring persistent cloud connections, subscriptions, or Google Play Services.

### Core Value Proposition
- **100% Offline Reliability**: Operates continuously during power outages, spotty cellular reception, or internet blackouts.
- **Philippine Retail Specialization**: Native support for Philippine Peso (`₱`), 12% Value-Added Tax (VAT), official TIN/receipt formatting, and digital financial services retailing (GCash Cash In/Out, Maya transfers, and prepaid telco e-loading for Smart, TNT, Globe, TM).
- **Hardened Kiosk Security**: Dedicated POS kiosk mode locking out unauthorized apps, preventing app uninstallation, and enforcing role-based PIN and biometric authentication.
- **Hardware Agnostic**: Prints to standard 58mm and 80mm ESC/POS thermal receipt printers over USB OTG and Bluetooth, with RJ11 cash drawer kick integration.

---

## 2. Core Pillars & Capabilities Shipped
1. **POS Cashier Terminal**: Camera barcode scanner, instant SKU lookup, multi-tender split payments (Cash, GCash, Maya, Debit), cart parking/resumption, line discounts, and customer charge accounts.
2. **Shift Handover & Drawer Balancing**: Blind cash float counts, denomination breakdown, expected vs actual cash discrepancy calculations, supervisor biometric override, and printed shift reconciliation slips.
3. **Repackaging & Production BOM Engine**: Bulk sack-to-retail pack conversion (e.g., 50kg rice sack into 1kg retail bags), automatic raw material deduction, finished goods stock crediting, and exact COGS valuation.
4. **Digital Services Hub & Wallet Ledgers**: Integrated portals for GCash cash-in/out, Maya bank transfers, and prepaid load sales with persistent double-entry audit ledgers, service charge tiers, and automated 2% load retailer rebates.
5. **Inventory Management, Stocktake & Price Checker**: Multi-Unit of Measure (kg, liters, packs, bottles, pieces, cans), rapid customer-facing price checker scanner, physical count stocktake audit adjustments, and stock receiving with COGS updates.
6. **Product Catalog & Variant Management**: Full product CRUD, category styling with color badges, and multi-variant support (sizes, packaging types, flavors) with independent barcodes, stock levels, and pricing.
7. **Suppliers, Purchasing & Accounts Payable**: Vendor directory, credit payment terms (COD, 7-day, 15-day, 30-day), itemized purchase orders with delivery tracking, and store overhead payment calendar (rent, electricity, utilities).
8. **Analytics & Financial Reporting**: Daily gross sales, net profit, basket sizes, sales trend visualizations, digital fee tuning, and print-ready PDF/CSV export engine.
9. **Hardware Integration & ESC/POS Thermal Printing**: 58mm/80mm receipt generation, Code 128 barcode printing, custom TIN/receipt branding, RJ11 cash drawer kick pulses, and petty cash cash-in/out drawer slips.
10. **Security, Access Control & Kiosk Lockdown**: Strict Admin vs Cashier RBAC, fast cashier PIN switching, Admin PIN reset tool, biometric fingerprint supervisor overrides, full-screen kiosk pinning, and allowed terminal package whitelisting.
11. **Database Backup & Disaster Recovery**: Encrypted/unencrypted full JSON database exports with PBKDF2-HMAC-SHA256 and AES-256-CBC, persistent internal snapshots, SQLite PRAGMA integrity validation, and crash diagnostics manager.
12. **Setup, Onboarding & OTA Updates**: First-run setup wizard, 12% VAT and ₱ defaults, and built-in GitHub Releases OTA updater with SHA-256 binary verification.

---

## 3. Current Operating Metrics
- **Automated Test Coverage**: 203 unit tests across 26 test classes (100% pass rate, 0 failures).
- **Database Architecture**: SQLite 3.x managed via Room 2.6 with Write-Ahead Logging (WAL) and 128MB cache.
- **Hardware Integration**: Tested for USB OTG ESC/POS printers and Bluetooth thermal printers.
- **Security Baseline**: 4-digit PIN policy with progressive backoff lockout, SHA-256 salted hashes, and Android Device Administrator integration.

---

## 4. Current Bottleneck
**Field Pilot Validation & Hardware Bench Testing**: All 12 functional pillars are code-complete and unit-tested in software. The critical constraint is now real-world validation across physical handheld POS devices (e.g., Sunmi V2s, iMin D4) in live retail environments under varied thermal paper widths and USB peripheral configurations.

