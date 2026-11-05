# Recommended Next Step — StorePoint POS

**Author:** Co-Founder Operating Loop  
**Date:** 2026-11-03  

---

## 1. The Bottleneck
**Physical Hardware Bench Testing & Field Merchant Pilot Validation**  
All 12 software pillars are implemented, architected, and validated with 203 passing unit tests. Further code additions in isolation yield diminishing returns; the bottleneck is proving real-world hardware reliability (thermal printing, Bluetooth reconnects, USB OTG cash drawer kicks) and cashier ergonomics on physical retail counters.

---

## 2. Chosen Action: Controlled 3-Store Field Pilot
Execute a structured 7-day shadow pilot across 3 diverse retail environments:
1. **Sari-Sari Store**: Fast-moving consumer goods, frequent small-denomination cash transactions, mobile load retailing, and camera barcode scanning.
2. **Mini-Grocery / Convenience Mart**: Multi-item basket checkouts, product variants, thermal 80mm receipts, and RJ11 cash drawer kick integration.
3. **Wholesale / Bulk Repackager**: Bulk rice/sugar repackaging via the BOM engine, supplier purchase order receiving, and store overhead bill tracking.

---

## 3. Why This Beats the Obvious Alternative
- **Alternative (Building More Software Features / Cloud Sync)**: Adding cloud syncing, web portals, or loyalty systems before merchant pilot validation risks building complex infrastructure on untested assumptions. Merchants care primarily about speed at checkout, accurate drawer reconciliation, and zero crashes during power outages.
- **Why Pilot Wins**: Field validation surfaces real hardware quirks (thermal paper jams, Bluetooth sleep timeouts, dim lighting barcode reads) that cannot be simulated in test runners, while providing undeniable proof of product-market fit.

---

## 4. Concrete Next Steps
1. **Prepare 3 Test POS Devices**:
   - Install StorePoint v1.0.1.10 APK on 3 dedicated Android POS terminals/tablets.
   - Activate Device Administrator to secure Kiosk Mode.
   - Pair 58mm Bluetooth and 80mm USB OTG thermal receipt printers with RJ11 cash drawers.
2. **Execute Onboarding Setup**:
   - Run the first-run Setup Wizard on each device: enter store name, default currency (`₱`), 12% VAT, and configure digital service balances.
3. **Run 7-Day Pilot & Shadow Shifts**:
   - Shadow cashiers during morning opening float counts, peak rush checkouts, GCash/Load retailing, and end-of-shift blind cash reconciliation.
   - Monitor `crash_logs.json` and database health latency via the Admin Security Tab.

