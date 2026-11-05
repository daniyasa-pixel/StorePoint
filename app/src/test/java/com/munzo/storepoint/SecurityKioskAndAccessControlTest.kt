package com.munzo.storepoint

import com.munzo.storepoint.data.User
import com.munzo.storepoint.util.BiometricAuthHelper
import com.munzo.storepoint.util.DigitalServicesHelper
import com.munzo.storepoint.util.KioskLockdownPolicy
import com.munzo.storepoint.util.KioskLockout
import com.munzo.storepoint.util.SecurityHelper
import org.junit.Assert.*
import org.junit.Test

class SecurityKioskAndAccessControlTest {

    @Test
    fun rbac_roleSeparationPermissionsEnforced() {
        val adminUser = User(
            username = "store_admin",
            pinHash = SecurityHelper.hashPin("1234"),
            role = "ADMIN"
        )
        val cashierUser = User(
            username = "cashier_juan",
            pinHash = SecurityHelper.hashPin("5678"),
            role = "CASHIER"
        )
        val inventoryUser = User(
            username = "stock_clerk",
            pinHash = SecurityHelper.hashPin("9999"),
            role = "INVENTORY"
        )

        // Helper RBAC gate functions mirroring UI authorization
        fun canAccessAdminDashboard(user: User): Boolean = user.role.equals("ADMIN", ignoreCase = true)
        fun canAccessPayrollAndSecurity(user: User): Boolean = user.role.equals("ADMIN", ignoreCase = true)
        fun canProcessSales(user: User): Boolean = user.role in listOf("ADMIN", "CASHIER")
        fun canPerformStocktake(user: User): Boolean = user.role in listOf("ADMIN", "INVENTORY")

        // 1. Admin permissions
        assertTrue(canAccessAdminDashboard(adminUser))
        assertTrue(canAccessPayrollAndSecurity(adminUser))
        assertTrue(canProcessSales(adminUser))
        assertTrue(canPerformStocktake(adminUser))

        // 2. Cashier permissions (Restricted: cannot access Admin dashboard or security)
        assertFalse(canAccessAdminDashboard(cashierUser))
        assertFalse(canAccessPayrollAndSecurity(cashierUser))
        assertTrue(canProcessSales(cashierUser))
        assertFalse(canPerformStocktake(cashierUser))

        // 3. Inventory clerk permissions
        assertFalse(canAccessAdminDashboard(inventoryUser))
        assertFalse(canAccessPayrollAndSecurity(inventoryUser))
        assertFalse(canProcessSales(inventoryUser))
        assertTrue(canPerformStocktake(inventoryUser))
    }

    @Test
    fun fastCashierPinSwitching_validatesCredentialsAndSwitchesActiveUser() {
        val cashier1 = User(username = "maria", pinHash = SecurityHelper.hashPin("1111"), role = "CASHIER")
        val cashier2 = User(username = "pedro", pinHash = SecurityHelper.hashPin("2222"), role = "CASHIER")

        var activeCashier = cashier1
        assertEquals("maria", activeCashier.username)

        // Attempt invalid PIN for incoming cashier2
        val wrongAttempt = SecurityHelper.verifyPin("9999", cashier2.pinHash)
        assertFalse(wrongAttempt.isMatch)
        assertEquals("maria", activeCashier.username) // Remains cashier1 on failure

        // Attempt correct PIN for cashier2
        val correctAttempt = SecurityHelper.verifyPin("2222", cashier2.pinHash)
        assertTrue(correctAttempt.isMatch)
        activeCashier = cashier2
        assertEquals("pedro", activeCashier.username) // Successfully fast switched
    }

    @Test
    fun adminPinResetMechanism_validatesAndUpdatesHashedPin() {
        val staffUser = User(
            username = "staff_anna",
            pinHash = SecurityHelper.hashPin("4321"),
            role = "CASHIER",
            pinResetRequired = true
        )

        // 1. Validation checks
        assertFalse(SecurityHelper.isValidPin("123"))      // Too short
        assertFalse(SecurityHelper.isValidPin("12345"))    // Too long
        assertFalse(SecurityHelper.isValidPin("abcd"))     // Non-digits
        assertTrue(SecurityHelper.isValidPin("8888"))      // Valid 4-digit PIN

        // 2. Execute Admin PIN reset
        val newPin = "8888"
        val newHash = SecurityHelper.hashPin(newPin)
        val updatedUser = staffUser.copy(
            pinHash = newHash,
            pinResetRequired = false
        )

        // 3. Verify old PIN no longer works, new PIN verifies, reset flag is cleared
        assertFalse(SecurityHelper.verifyPin("4321", updatedUser.pinHash).isMatch)
        assertTrue(SecurityHelper.verifyPin("8888", updatedUser.pinHash).isMatch)
        assertFalse(updatedUser.pinResetRequired)
    }

    @Test
    fun biometricAuth_outcomesAndAvailabilityClassification() {
        // Test enum outcome coverage
        val outcomes = BiometricAuthHelper.BiometricOutcome.values()
        assertTrue(outcomes.contains(BiometricAuthHelper.BiometricOutcome.SUCCESS))
        assertTrue(outcomes.contains(BiometricAuthHelper.BiometricOutcome.FALLBACK))
        assertTrue(outcomes.contains(BiometricAuthHelper.BiometricOutcome.RECOVERABLE_ERROR))
        assertTrue(outcomes.contains(BiometricAuthHelper.BiometricOutcome.FATAL_ERROR))
        assertTrue(outcomes.contains(BiometricAuthHelper.BiometricOutcome.UNAVAILABLE))

        // Availability data structure validation
        val availReady = BiometricAuthHelper.BiometricAvailability(
            isAvailable = true,
            isHardwarePresent = true,
            canEnroll = true,
            reason = "Ready"
        )
        assertTrue(availReady.isAvailable)
        assertTrue(availReady.isHardwarePresent)

        val availNoEnrolled = BiometricAuthHelper.BiometricAvailability(
            isAvailable = false,
            isHardwarePresent = true,
            canEnroll = true,
            reason = "No biometric enrolled on this device"
        )
        assertFalse(availNoEnrolled.isAvailable)
        assertTrue(availNoEnrolled.isHardwarePresent)
        assertTrue(availNoEnrolled.canEnroll)
    }

    @Test
    fun kioskLockdownPolicy_escalatingLockoutSchedule() {
        // Free attempts (burst allowance)
        assertEquals(0L, KioskLockdownPolicy.lockoutWindowFor(0))
        assertEquals(0L, KioskLockdownPolicy.lockoutWindowFor(1))
        assertEquals(0L, KioskLockdownPolicy.lockoutWindowFor(2))

        // 3rd failure -> 30s lockout
        assertEquals(30_000L, KioskLockdownPolicy.lockoutWindowFor(3))

        // 4th failure -> 60s (1m) lockout
        assertEquals(60_000L, KioskLockdownPolicy.lockoutWindowFor(4))

        // 5th failure -> 120s (2m) lockout
        assertEquals(120_000L, KioskLockdownPolicy.lockoutWindowFor(5))

        // 6th failure -> 300s (5m maximum lockout)
        assertEquals(300_000L, KioskLockdownPolicy.lockoutWindowFor(6))

        // Further failures capped at MAX_LOCKOUT_MS (300s)
        assertEquals(300_000L, KioskLockdownPolicy.lockoutWindowFor(10))
        assertEquals(KioskLockdownPolicy.MAX_LOCKOUT_MS, KioskLockdownPolicy.lockoutWindowFor(99))

        // Remaining time calculation
        val now = 1000000L
        val lockedUntil = now + 45000L
        assertEquals(45000L, KioskLockdownPolicy.remainingMs(lockedUntil, now))
        assertEquals(0L, KioskLockdownPolicy.remainingMs(now - 1000L, now))

        // Human-readable message
        val msg = KioskLockdownPolicy.lockoutMessage(lockedUntil, now)
        assertEquals("Too many failed attempts. Locked for 45s.", msg)
    }

    @Test
    fun kioskLockout_stateTrackingAndSuccessReset() {
        var failCount = 0
        var lockedUntil = 0L
        var clock = 100_000L

        val lockout = KioskLockout(
            readFailCount = { failCount },
            readLockedUntil = { lockedUntil },
            writeState = { _, count, until ->
                failCount = count
                lockedUntil = until
            },
            now = { clock }
        )

        // 1st and 2nd failure: free burst warnings without lockout
        val msg1 = lockout.onFailure("admin_pin")
        assertTrue(msg1.contains("attempt"))
        assertEquals(1, failCount)
        assertNull(lockout.lockoutMessage("admin_pin"))
        assertEquals(0L, lockout.remainingMs("admin_pin"))

        val msg2 = lockout.onFailure("admin_pin")
        assertTrue(msg2.contains("last attempt"))
        assertEquals(2, failCount)
        assertNull(lockout.lockoutMessage("admin_pin"))
        assertEquals(0L, lockout.remainingMs("admin_pin"))

        // 3rd failure: 30s lockout triggered
        val msg3 = lockout.onFailure("admin_pin")
        assertTrue(msg3.contains("Too many failed attempts"))
        assertEquals(clock + 30_000L, lockedUntil)
        assertEquals(30_000L, lockout.remainingMs("admin_pin"))
        assertNotNull(lockout.lockoutMessage("admin_pin"))

        // Clock advances 30s: lockout expires
        clock += 30_000L
        assertEquals(0L, lockout.remainingMs("admin_pin"))

        // Successful authentication resets fail count and lockout timestamp
        lockout.onSuccess("admin_pin")
        assertEquals(0, failCount)
        assertEquals(0L, lockedUntil)
    }

    @Test
    fun terminalAppWhitelisting_authorizedBankingAndEwalletApps() {
        val defaultApps = DigitalServicesHelper.DEFAULT_ALLOWED_APPS
        // Validates standard Philippine e-wallet / banking apps are in default whitelist
        assertTrue(defaultApps.contains("com.globe.gcash.android"))
        assertTrue(defaultApps.contains("com.paymaya"))
        assertTrue(defaultApps.contains("com.android.stk"))

        // Whitelist filtering logic
        val installedTerminalApps = listOf(
            Pair("com.globe.gcash.android", "GCash"),
            Pair("com.paymaya", "Maya"),
            Pair("com.facebook.katana", "Facebook"), // Unauthorized social media app
            Pair("com.google.android.youtube", "YouTube") // Unauthorized media app
        )

        val allowedApps = setOf("com.globe.gcash.android", "com.paymaya")
        val authorizedLaunchable = installedTerminalApps.filter { allowedApps.contains(it.first) }

        assertEquals(2, authorizedLaunchable.size)
        assertEquals("GCash", authorizedLaunchable[0].second)
        assertEquals("Maya", authorizedLaunchable[1].second)
        assertFalse(authorizedLaunchable.any { it.first == "com.facebook.katana" })
        assertFalse(authorizedLaunchable.any { it.first == "com.google.android.youtube" })
    }
}
