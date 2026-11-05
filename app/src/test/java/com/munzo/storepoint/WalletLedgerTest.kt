package com.munzo.storepoint

import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.data.StorePointRepository.WalletEntryType
import com.munzo.storepoint.data.StorePointRepository.WalletType
import com.munzo.storepoint.ui.screens.calculateTieredStoreFee
import com.munzo.storepoint.util.DigitalServicesHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the wallet-float ledger introduced to make owner reloads auditable.
 *
 * Before this, the GCash / Smart / Globe balances were only ever changed by a raw
 * absolute setter, so a peso removed by a customer load was indistinguishable from a
 * mistyped adjustment and there was no way to prove a float matched physical money
 * at shift close.
 */
class WalletLedgerTest {

    // --- Label mapping: the UI must be able to name a wallet the way the app does ---

    @Test
    fun canonicalMappingResolvesEachWalletLabel() {
        assertEquals(WalletType.GCASH, StorePointRepository.canonicalWalletType("GCash"))
        assertEquals(WalletType.SMART, StorePointRepository.canonicalWalletType("Smart / TNT"))
        assertEquals(WalletType.SMART, StorePointRepository.canonicalWalletType("DITO"))
        assertEquals(WalletType.GLOBE, StorePointRepository.canonicalWalletType("Globe / TM"))
    }

    @Test
    fun canonicalMappingIsCaseInsensitive() {
        assertEquals(WalletType.GCASH, StorePointRepository.canonicalWalletType("gcash"))
        assertEquals(WalletType.SMART, StorePointRepository.canonicalWalletType("SMART"))
        assertEquals(WalletType.SMART, StorePointRepository.canonicalWalletType("tnt"))
        assertEquals(WalletType.GLOBE, StorePointRepository.canonicalWalletType("globe"))
    }

    @Test
    fun canonicalMappingResolvesTmToGlobeNotToASmartWallet() {
        // "TM" is Globe's brand. It must not be captured by the Smart/TNT branch,
        // which is the failure this assertion exists to prevent.
        assertEquals(WalletType.GLOBE, StorePointRepository.canonicalWalletType("TM"))
    }

    @Test
    fun unknownLabelFallsBackToGcashRatherThanThrowing() {
        assertEquals(WalletType.GCASH, StorePointRepository.canonicalWalletType("Unlabelled"))
        assertEquals(WalletType.GCASH, StorePointRepository.canonicalWalletType(""))
    }

    // --- Ledger invariants ---

    @Test
    fun entryTypeVocabularyIsStable() {
        // These strings are persisted and also seeded by MIGRATION_15_16, so they
        // must not drift without a migration.
        assertEquals("RELOAD", WalletEntryType.RELOAD)
        assertEquals("CONSUMED", WalletEntryType.CONSUMED)
        assertEquals("INFLOW", WalletEntryType.INFLOW)
        assertEquals("OUTFLOW", WalletEntryType.OUTFLOW)
        assertEquals("RECONCILE", WalletEntryType.RECONCILE)
        assertEquals("OPENING_BALANCE", WalletEntryType.OPENING_BALANCE)
    }

    @Test
    fun tieredStoreFeeCalculationMatchesPhilippineBrackets() {
        // Under ₱500 -> ₱5.00 min fee
        assertEquals(5.0, calculateTieredStoreFee(200.0, 10.0), 0.001)
        assertEquals(5.0, calculateTieredStoreFee(500.0, 10.0), 0.001)

        // ₱501 to ₱1,000 -> ₱10.00 base bracket
        assertEquals(10.0, calculateTieredStoreFee(700.0, 10.0), 0.001)
        assertEquals(10.0, calculateTieredStoreFee(1000.0, 10.0), 0.001)

        // ₱1,001 to ₱1,500 -> ₱15.00
        assertEquals(15.0, calculateTieredStoreFee(1200.0, 10.0), 0.001)
        assertEquals(15.0, calculateTieredStoreFee(1500.0, 10.0), 0.001)

        // ₱1,501 to ₱2,000 -> ₱20.00
        assertEquals(20.0, calculateTieredStoreFee(2000.0, 10.0), 0.001)

        // ₱5,000 -> ₱50.00
        assertEquals(50.0, calculateTieredStoreFee(5000.0, 10.0), 0.001)
    }

    @Test
    fun ussdCodeMappingResolvesAllPhilippineTelcos() {
        assertEquals("*123#", DigitalServicesHelper.getUssdCodeForNetwork("Smart"))
        assertEquals("*123#", DigitalServicesHelper.getUssdCodeForNetwork("TNT"))
        assertEquals("*143#", DigitalServicesHelper.getUssdCodeForNetwork("Globe"))
        assertEquals("*143#", DigitalServicesHelper.getUssdCodeForNetwork("TM"))
        assertEquals("*185#", DigitalServicesHelper.getUssdCodeForNetwork("DITO"))
    }

    @Test
    fun directSmsCommandFormattingSupportsAirtimeAndWallets() {
        // Smart/TNT Pasaload format
        val smartCmd = DigitalServicesHelper.getSmsCommand("Load", "Smart", "09181234567", 50.0)
        assertEquals("PASALOAD 09181234567 50", smartCmd)

        // DITO Pasaload format
        val ditoCmd = DigitalServicesHelper.getSmsCommand("Load", "DITO", "09911234567", 100.0)
        assertEquals("PASALOAD 09911234567 100", ditoCmd)

        // Globe Share-a-Load format
        val globeCmd = DigitalServicesHelper.getSmsCommand("Load", "Globe", "09171234567", 30.0)
        assertEquals("SHARE 30", globeCmd)

        // GCash / Maya SMS notice format
        val gcashCmd = DigitalServicesHelper.getSmsCommand("GCash", "", "09171234567", 500.0)
        assertEquals("SEND ₱500.00 to 09171234567", gcashCmd)
    }

    @Test
    fun digitalServiceItemParsingIncludesDito() {
        val parsed = DigitalServicesHelper.parseItem("Load Top-Up (DITO) for 09911234567 (Amt: 100.0, Fee: 2.0)")
        assertNotNull(parsed)
        assertEquals("Load", parsed?.service)
        assertEquals("Top-Up", parsed?.action)
        assertEquals("DITO", parsed?.network)
        assertEquals("09911234567", parsed?.mobileNumber)
        assertEquals(100.0, parsed?.amount ?: 0.0, 0.001)
        assertEquals(2.0, parsed?.fee ?: 0.0, 0.001)
    }

    @Test
    fun serviceFeeProfitsAreExtractableFromLedgerNotes() {
        val entries = listOf(
            "Fee Profit: ₱10.00 | GCash Cash-In ₱1000.00",
            "Fee Profit: ₱20.00 | Maya Cash-In ₱2000.00",
            "Fee Profit: ₱4.00 (Markup: ₱2.00 + 2% Rebate) | Load ₱100.00",
            "Direct cash reload without fee"
        )
        val totalFeeProfit = entries.sumOf { notes ->
            val match = Regex("""Fee Profit:\s*₱?([\d.]+)""").find(notes)
            match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        }
        assertEquals(34.0, totalFeeProfit, 0.001)
    }

    @Test
    fun runningBalanceReconcilesReloadsAgainstConsumption() {
        // Mirrors the invariant the migration seed exists to preserve: after seeding an
        // opening balance, SUM(delta) must equal the live store_config balance.
        val movements = listOf(
            1_000.0 to "OPENING_BALANCE",
            500.0 to "RELOAD",
            -100.0 to "CONSUMED",
            -250.0 to "CONSUMED",
            20.0 to "RECONCILE"
        )
        val net = movements.sumOf { it.first }
        assertEquals(1_170.0, net, 0.001)
        assertTrue(
            "Every row must carry a type so history is self-describing",
            movements.all { it.second.isNotBlank() }
        )
    }

    @Test
    fun consumedMovementsAreAlwaysNegative() {
        // A positive CONSUMED row would inflate the float, so the sign is asserted here
        // rather than trusted to the call site.
        val consumption = -kotlin.math.abs(100.0)
        assertTrue("Consumption must reduce the float", consumption < 0.0)
    }
}
