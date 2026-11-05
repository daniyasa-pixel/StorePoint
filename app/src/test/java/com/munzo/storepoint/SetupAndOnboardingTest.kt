package com.munzo.storepoint

import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.User
import com.munzo.storepoint.util.AppUpdateInfo
import com.munzo.storepoint.util.AppUpdateManager
import com.munzo.storepoint.util.SecurityHelper
import com.munzo.storepoint.util.UpdateDownloadState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SetupAndOnboardingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // =========================================================================
    // 1. Store Setup Wizard Initialization
    // =========================================================================

    @Test
    fun storeSetupWizard_defaultFinancialsAndCurrencies() {
        val defaultConfig = StoreConfig(
            id = 1,
            storeName = "StorePoint Sari-Sari Mart",
            currencySymbol = "₱", // Philippine Peso default
            taxPercentage = 12.0, // Standard 12% PH VAT
            setupCompleted = true,
            hasGCash = true,
            hasMaya = true,
            hasLoad = true,
            smartLoadBalance = 5000.0,
            globeLoadBalance = 5000.0,
            loadServiceFee = 2.0,
            gcashServiceFee = 10.0,
            mayaServiceFee = 10.0,
            mayaBankFee = 15.0
        )

        assertEquals("₱", defaultConfig.currencySymbol)
        assertEquals(12.0, defaultConfig.taxPercentage, 0.0)
        assertTrue(defaultConfig.setupCompleted)
        assertTrue(defaultConfig.hasGCash)
        assertTrue(defaultConfig.hasMaya)
        assertTrue(defaultConfig.hasLoad)
        assertEquals(2.0, defaultConfig.loadServiceFee, 0.0)
        assertEquals(10.0, defaultConfig.gcashServiceFee, 0.0)
        assertEquals(10.0, defaultConfig.mayaServiceFee, 0.0)
        assertEquals(15.0, defaultConfig.mayaBankFee, 0.0)
    }

    @Test
    fun storeSetupWizard_adminCredentialsAndPinValidation() {
        val weakPins = listOf("1234", "0000", "1111", "9999", "4321")
        for (pin in weakPins) {
            assertTrue("Weak PIN $pin should be flagged as weak", SecurityHelper.isWeakPin(pin))
        }

        val strongPin = "7492"
        assertTrue(SecurityHelper.isValidPin(strongPin))
        assertFalse(SecurityHelper.isWeakPin(strongPin))

        // Create initial administrator account under the 4-digit PIN standard
        val hashedPin = SecurityHelper.hashPin(strongPin)
        val adminUser = User(
            username = "store_admin",
            pinHash = hashedPin,
            role = "ADMIN",
            biometricEnrolled = false,
            pinResetRequired = false
        )

        assertEquals("store_admin", adminUser.username)
        assertEquals("ADMIN", adminUser.role)
        assertFalse(adminUser.pinResetRequired)
        assertTrue(SecurityHelper.verifyPin(strongPin, adminUser.pinHash).isMatch)
        assertFalse(SecurityHelper.verifyPin("9999", adminUser.pinHash).isMatch)
    }

    @Test
    fun storeSetupWizard_receiptHeaderAndBrandingDetails() {
        val testTin = "987-654-321-000"
        val testAddress = "Unit 102 Taft Avenue, Pasay City, Metro Manila"
        val testHeader = "Official Sales Invoice / Cash Voucher"
        val testFooter = "Thank you for shopping! Maraming Salamat Po!"
        val printBarcode = true

        val sampleConfig = StoreConfig(
            storeName = "Metro Manila Sari-Sari Express",
            currencySymbol = "₱",
            taxPercentage = 12.0,
            setupCompleted = true
        )

        val receiptBytes = com.munzo.storepoint.util.EscPosHelper.buildReceiptEscPos(
            storeConfig = sampleConfig,
            transaction = com.munzo.storepoint.data.Transaction(
                id = 101,
                timestamp = 1700000000000L,
                cashierUsername = "admin",
                subtotal = 100.0,
                taxAmount = 12.0,
                totalAmount = 112.0,
                paymentMethod = "CASH",
                cashPaid = 120.0,
                changeAmount = 8.0
            ),
            items = listOf(
                com.munzo.storepoint.data.TransactionItem(
                    id = 1,
                    transactionId = 101,
                    productId = 1,
                    productName = "Product A",
                    price = 100.0,
                    quantity = 1
                )
            ),
            is80mm = false,
            customHeader = testHeader,
            customFooter = testFooter,
            storeTin = testTin,
            storeAddress = testAddress,
            printBarcode = printBarcode
        )

        val receiptText = String(receiptBytes, Charsets.US_ASCII)
        assertTrue(receiptText.contains("Metro Manila Sari-Sari Express"))
        assertTrue(receiptText.contains(testTin))
        assertTrue(receiptText.contains(testAddress))
        assertTrue(receiptText.contains(testHeader))
        assertTrue(receiptText.contains(testFooter))
    }

    // =========================================================================
    // 2. Over-the-Air App Updates (AppUpdateManager)
    // =========================================================================

    @Test
    fun otaUpdates_semverComparisonLogic() {
        // Increments
        assertTrue(AppUpdateManager.isVersionNewer("v2.0.0", "1.0.1.10"))
        assertTrue(AppUpdateManager.isVersionNewer("1.1.0", "1.0.1.10"))
        assertTrue(AppUpdateManager.isVersionNewer("1.0.2", "1.0.1.10"))
        assertTrue(AppUpdateManager.isVersionNewer("1.0.1.11", "1.0.1.10"))

        // Same or older versions
        assertFalse(AppUpdateManager.isVersionNewer("1.0.1.10", "1.0.1.10"))
        assertFalse(AppUpdateManager.isVersionNewer("v1.0.1.10", "1.0.1.10"))
        assertFalse(AppUpdateManager.isVersionNewer("1.0.0", "1.0.1.10"))
        assertFalse(AppUpdateManager.isVersionNewer("v0.9.9", "1.0.1.10"))
    }

    @Test
    fun otaUpdates_parseGitHubReleasePayload() {
        val releasePayload = JSONObject().apply {
            put("tag_name", "v2.0.0")
            put("name", "StorePoint POS v2.0.0 Major Release")
            put("body", "• Enhanced Thermal Printing\n• Kiosk Lockdown Improvements")
            put("published_at", "2026-11-03T00:00:00Z")

            val assets = JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "StorePoint-v2.0.0-signed-release.apk")
                    put("browser_download_url", "https://github.com/uznom/StorePoint/releases/download/v2.0.0/StorePoint-v2.0.0.apk")
                    put("size", 28500000L)
                })
                put(JSONObject().apply {
                    put("name", "StorePoint-v2.0.0.apk.sha256")
                    put("browser_download_url", "https://github.com/uznom/StorePoint/releases/download/v2.0.0/StorePoint-v2.0.0.apk.sha256")
                    put("size", 65L)
                })
            }
            put("assets", assets)
        }

        val parsedInfo = AppUpdateManager.parseReleaseJson(
            json = releasePayload,
            currentVersion = "1.0.1.10",
            explicitChecksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )

        assertEquals("v2.0.0", parsedInfo.latestVersionTag)
        assertEquals("StorePoint POS v2.0.0 Major Release", parsedInfo.releaseName)
        assertTrue(parsedInfo.isNewer)
        assertEquals(28500000L, parsedInfo.assetSize)
        assertEquals("https://github.com/uznom/StorePoint/releases/download/v2.0.0/StorePoint-v2.0.0.apk", parsedInfo.downloadUrl)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", parsedInfo.sha256Checksum)
        assertTrue(parsedInfo.releaseNotes.contains("Enhanced Thermal Printing"))
    }

    @Test
    fun otaUpdates_fileSha256ChecksumVerification() {
        val testFile = File(tempFolder.root, "test_payload.bin")
        testFile.writeText("StorePoint Standalone Kiosk OTA APK Verification", Charsets.UTF_8)

        val computedSha256 = AppUpdateManager.sha256HexOfFile(testFile)
        assertNotNull(computedSha256)
        assertEquals(64, computedSha256.length) // Standard 64-char hex string

        // Verify repeatability
        val secondHash = AppUpdateManager.sha256HexOfFile(testFile)
        assertEquals(computedSha256, secondHash)
    }

    @Test
    fun otaUpdates_downloadStateRepresentation() {
        val idleState = UpdateDownloadState.Idle
        assertTrue(idleState is UpdateDownloadState.Idle)

        val downloadingState = UpdateDownloadState.Downloading(
            progress = 0.65f,
            downloadedBytes = 16384000L,
            totalBytes = 25000000L
        )
        assertEquals(0.65f, downloadingState.progress, 0.001f)
        assertEquals(16384000L, downloadingState.downloadedBytes)

        val dummyFile = File(tempFolder.root, "StorePoint-update.apk")
        val readyState = UpdateDownloadState.ReadyToInstall(dummyFile)
        assertEquals(dummyFile, readyState.apkFile)

        val errorState = UpdateDownloadState.Error("HTTP 404: Release binary asset not found")
        assertEquals("HTTP 404: Release binary asset not found", errorState.message)
    }
}

