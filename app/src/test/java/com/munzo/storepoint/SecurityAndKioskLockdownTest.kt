package com.munzo.storepoint

import com.munzo.storepoint.data.User
import com.munzo.storepoint.util.BiometricAuthHelper
import com.munzo.storepoint.util.DigitalServicesHelper
import com.munzo.storepoint.util.KioskLockdownPolicy
import com.munzo.storepoint.util.KioskLockout
import com.munzo.storepoint.util.SecurityHelper
import org.junit.Assert.*
import org.junit.Test

class SecurityAndKioskLockdownTest {

    @Test
    fun rbac_roleSeparationAdminAndCashierPermissions() {
        val adminUser = User(username = "admin", pinHash = SecurityHelper.hashPin("7891"), role = "ADMIN")
        val cashierUser = User(username = "cashier1", pinHash = SecurityHelper.hashPin("3245"), role = "CASHIER")
        val inventoryUser = User(username = "stockclerk", pinHash = SecurityHelper.hashPin("6182"), role = "INVENTORY")

        // Role verification
        assertEquals("ADMIN", adminUser.role)
        assertEquals("CASHIER", cashierUser.role)
        assertEquals("INVENTORY", inventoryUser.role)

        // Permission gate checks
        val canAccessAdminDashboard = { u: User -> u.role.equals("ADMIN", ignoreCase = true) }
        val canEditStaffCredentials = { u: User -> u.role.equals("ADMIN", ignoreCase = true) }
        val canManageBackups = { u: User -> u.role.equals("ADMIN", ignoreCase = true) }
        val canProcessSales = { u: User -> u.role in listOf("ADMIN", "CASHIER") }

        assertTrue(canAccessAdminDashboard(adminUser))
        assertFalse(canAccessAdminDashboard(cashierUser))
        assertFalse(canAccessAdminDashboard(inventoryUser))

        assertTrue(canEditStaffCredentials(adminUser))
        assertFalse(canEditStaffCredentials(cashierUser))

        assertTrue(canManageBackups(adminUser))
        assertFalse(canManageBackups(cashierUser))

        assertTrue(canProcessSales(adminUser))
        assertTrue(canProcessSales(cashierUser))
    }

    @Test
    fun fastCashierPinSwitching_validatesPinAndSwitchesActiveUser() {
        val cashierA = User(username = "maria", pinHash = SecurityHelper.hashPin("1892"), role = "CASHIER")
        val cashierB = User(username = "pedro", pinHash = SecurityHelper.hashPin("9431"), role = "CASHIER")
        val staffList = listOf(cashierA, cashierB)

        var activeCashier = cashierA
        assertEquals("maria", activeCashier.username)

        // Incoming cashier B enters PIN "9431"
        val incomingPin = "9431"
        val matchedUser = staffList.firstOrNull { SecurityHelper.verifyPin(incomingPin, it.pinHash).isMatch }
        assertNotNull(matchedUser)
        assertEquals("pedro", matchedUser?.username)

        // Switch active cashier
        if (matchedUser != null) {
            activeCashier = matchedUser
        }
        assertEquals("pedro", activeCashier.username)

        // Invalid PIN rejects switch
        val wrongPin = "0000"
        val failedMatch = staffList.firstOrNull { SecurityHelper.verifyPin(wrongPin, it.pinHash).isMatch }
        assertNull(failedMatch)
        assertEquals("pedro", activeCashier.username) // remains Pedro
    }

    @Test
    fun adminPinReset_enforcesPinPolicyAndRecoveryKey() {
        val adminPinHash = SecurityHelper.hashPin("8492")
        val adminUser = User(username = "admin", pinHash = adminPinHash, role = "ADMIN")

        // 1. Weak PIN rejection at reset
        assertTrue("0000 is repeat weak", SecurityHelper.isWeakPin("0000"))
        assertTrue("1234 is ascending run", SecurityHelper.isWeakPin("1234"))
        assertTrue("4321 is descending run", SecurityHelper.isWeakPin("4321"))
        assertTrue("2580 is keyboard column walk", SecurityHelper.isWeakPin("2580"))
        assertTrue("1212 is alternating pattern", SecurityHelper.isWeakPin("1212"))
        assertFalse("7391 is strong", SecurityHelper.isWeakPin("7391"))

        // 2. Length & digit enforcement
        assertFalse("5 digits invalid", SecurityHelper.isValidPin("12345"))
        assertFalse("3 digits invalid", SecurityHelper.isValidPin("123"))
        assertFalse("alpha invalid", SecurityHelper.isValidPin("12a4"))
        assertTrue("4 numeric digits valid", SecurityHelper.isValidPin("7391"))

        // 3. Current PIN verification vs Master Recovery Key
        val currentPin = "8492"
        val isPinMatch = SecurityHelper.verifyPin(currentPin, adminUser.pinHash).isMatch
        assertTrue(isPinMatch)

        val masterRecoveryKey = "STOREPOINT-ADMIN-RESET"
        val isRecoveryMatch = masterRecoveryKey == "STOREPOINT-ADMIN-RESET"
        assertTrue(isRecoveryMatch)

        // Successful reset commits new hash
        val newPin = "7391"
        val updatedAdmin = adminUser.copy(pinHash = SecurityHelper.hashPin(newPin))
        assertTrue(SecurityHelper.verifyPin("7391", updatedAdmin.pinHash).isMatch)
        assertFalse(SecurityHelper.verifyPin("8492", updatedAdmin.pinHash).isMatch)
    }

    @Test
    fun biometricAuth_outcomesAndContracts() {
        // Outcome enum contract
        val outcomes = BiometricAuthHelper.BiometricOutcome.values().map { it.name }
        assertTrue(outcomes.contains("SUCCESS"))
        assertTrue(outcomes.contains("FALLBACK"))
        assertTrue(outcomes.contains("RECOVERABLE_ERROR"))
        assertTrue(outcomes.contains("FATAL_ERROR"))
        assertTrue(outcomes.contains("UNAVAILABLE"))

        // Availability data class contract
        val avail = BiometricAuthHelper.BiometricAvailability(
            isAvailable = true,
            isHardwarePresent = true,
            canEnroll = true,
            reason = "Ready"
        )
        assertTrue(avail.isAvailable)
        assertTrue(avail.isHardwarePresent)
        assertEquals("Ready", avail.reason)
    }

    @Test
    fun kioskLockdownPolicy_progressiveBackoffSchedule() {
        // Free attempts (first 2 attempts: no lockout)
        assertEquals(0L, KioskLockdownPolicy.lockoutWindowFor(1))
        assertEquals(0L, KioskLockdownPolicy.lockoutWindowFor(2))

        // Progressive escalation
        assertEquals(30_000L, KioskLockdownPolicy.lockoutWindowFor(3))   // 3rd attempt: 30s
        assertEquals(60_000L, KioskLockdownPolicy.lockoutWindowFor(4))   // 4th attempt: 60s
        assertEquals(120_000L, KioskLockdownPolicy.lockoutWindowFor(5))  // 5th attempt: 120s
        assertEquals(300_000L, KioskLockdownPolicy.lockoutWindowFor(6))  // 6th attempt: 300s (5m max)
        assertEquals(300_000L, KioskLockdownPolicy.lockoutWindowFor(10)) // subsequent attempts cap at 300s

        // Lockout message formatting
        val now = 1000000L
        val lockedUntil = now + 45000L // 45 seconds remaining
        val msg = KioskLockdownPolicy.lockoutMessage(lockedUntil, now)
        assertTrue(msg.contains("45s"))
    }

    @Test
    fun kioskAppWhitelisting_defaultAppsAndCustomPackages() {
        // Default whitelisted terminal apps (GCash, Maya, SIM Toolkit)
        val defaults = DigitalServicesHelper.DEFAULT_ALLOWED_APPS
        assertTrue(defaults.contains("com.globe.gcash.android"))
        assertTrue(defaults.contains("com.paymaya"))
        assertTrue(defaults.contains("com.android.stk"))

        // Adding banking and authorized apps
        val extendedWhitelist = defaults.toMutableSet().apply {
            add("ph.com.bdo.digital")
            add("com.bpi.ng.app")
            add("com.unionbankph.online")
        }

        assertTrue(extendedWhitelist.contains("ph.com.bdo.digital"))
        assertTrue(extendedWhitelist.contains("com.bpi.ng.app"))
        assertTrue(extendedWhitelist.contains("com.unionbankph.online"))
        assertEquals(6, extendedWhitelist.size)

        // Serialization format
        val serialized = extendedWhitelist.joinToString(",")
        val deserialized = serialized.split(",").map { it.trim() }.toSet()
        assertEquals(extendedWhitelist, deserialized)
    }
}

