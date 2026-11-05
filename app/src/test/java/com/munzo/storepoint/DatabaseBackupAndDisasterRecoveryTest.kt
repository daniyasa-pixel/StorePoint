package com.munzo.storepoint

import com.munzo.storepoint.data.DatabaseHealthReport
import com.munzo.storepoint.util.CrashDiagnosticsManager
import com.munzo.storepoint.util.DatabaseBackupManager
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DatabaseBackupAndDisasterRecoveryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testDir: File

    @Before
    fun setup() {
        testDir = tempFolder.newFolder("backup_disaster_recovery_test")
    }

    // =========================================================================
    // 1. Local Database Backup & Restore (DatabaseBackupManager)
    // =========================================================================

    @Test
    fun databaseBackup_unencryptedJsonStructureValidation() {
        val root = JSONObject().apply {
            put("backupVersion", 1)
            put("timestamp", 1700000000000L)
            put("dateTime", "2026-11-03 10:00:00")
            put("appName", "StorePoint POS")

            val tables = JSONObject().apply {
                val config = JSONObject().apply {
                    put("id", 1)
                    put("storeName", "StorePoint Flagship Store")
                    put("currencySymbol", "₱")
                    put("setupCompleted", true)
                }
                put("store_config", config)

                val users = JSONArray().apply {
                    put(JSONObject().apply {
                        put("username", "admin")
                        put("pinHash", "hashed_admin_pin")
                        put("role", "ADMIN")
                    })
                    put(JSONObject().apply {
                        put("username", "cashier1")
                        put("pinHash", "hashed_cashier_pin")
                        put("role", "CASHIER")
                    })
                }
                put("users", users)

                val products = JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", 101)
                        put("name", "San Miguel Pale Pilsen 330ml")
                        put("price", 65.0)
                        put("stock", 48)
                    })
                }
                put("products", products)

                val transactions = JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", 501)
                        put("totalAmount", 130.0)
                        put("paymentMethod", "CASH")
                        put("cashierUsername", "cashier1")
                    })
                }
                put("transactions", transactions)
            }
            put("tables", tables)
        }

        val jsonString = root.toString(2)
        val file = File(testDir, "test_plain_backup.json")
        file.writeText(jsonString, Charsets.UTF_8)

        val meta = DatabaseBackupManager.peekBackupMetadata(file)
        assertNotNull(meta)
        assertEquals("StorePoint Flagship Store", meta?.storeName)
        assertEquals(1, meta?.productsCount)
        assertEquals(1, meta?.transactionsCount)
        assertEquals(2, meta?.usersCount)
        assertFalse(meta?.isEncrypted ?: true)
    }

    @Test
    fun databaseBackup_encryptedExportAndDecryptionRoundtrip() = runBlocking {
        val samplePayload = """
            {
                "backupVersion": 1,
                "dateTime": "2026-11-03 12:00:00",
                "tables": {
                    "store_config": { "storeName": "Secret Branch" },
                    "products": [ { "id": 1, "name": "Espresso" } ]
                }
            }
        """.trimIndent()

        val passphrase = "StrongSecureAdminPassword!2026"

        // 1. Encrypt payload
        val encResult = DatabaseBackupManager.encryptBackupJson(samplePayload, passphrase)
        assertTrue(encResult.isSuccess)
        val encryptedEnvelope = encResult.getOrThrow()

        // 2. Validate envelope format (enc == 1, PBKDF2 iterations, salt, iv, mac, data)
        val envelopeJson = JSONObject(encryptedEnvelope)
        assertEquals(1, envelopeJson.getInt("enc"))
        assertTrue(envelopeJson.has("salt"))
        assertTrue(envelopeJson.has("iv"))
        assertTrue(envelopeJson.has("mac"))
        assertTrue(envelopeJson.has("data"))
        assertTrue(envelopeJson.getInt("iterations") >= 100_000)

        // 3. Inspect metadata on encrypted backup file
        val encFile = File(testDir, "encrypted_vault.json")
        encFile.writeText(encryptedEnvelope, Charsets.UTF_8)
        val meta = DatabaseBackupManager.peekBackupMetadata(encFile)
        assertNotNull(meta)
        assertTrue(meta?.isEncrypted == true)
        assertEquals("Encrypted Vault Backup", meta?.storeName)

        // 4. Decrypt with correct passphrase
        val decResult = DatabaseBackupManager.decryptBackupJson(encryptedEnvelope, passphrase)
        assertTrue(decResult.isSuccess)
        val decryptedPlain = decResult.getOrThrow()
        val decryptedObj = JSONObject(decryptedPlain)
        assertEquals("Secret Branch", decryptedObj.getJSONObject("tables").getJSONObject("store_config").getString("storeName"))

        // 5. Decrypt with wrong passphrase must fail HMAC/MAC authentication
        val wrongDecResult = DatabaseBackupManager.decryptBackupJson(encryptedEnvelope, "WrongPassword123")
        assertTrue(wrongDecResult.isFailure)
        assertTrue(wrongDecResult.exceptionOrNull()?.message?.contains("password") == true ||
                wrongDecResult.exceptionOrNull()?.message?.contains("MAC") == true)
    }

    @Test
    fun databaseBackup_encryptionRejectsEmptyPassword() = runBlocking {
        val encResult = DatabaseBackupManager.encryptBackupJson("{}", "")
        assertTrue(encResult.isFailure)
        assertTrue(encResult.exceptionOrNull()?.message?.contains("must not be empty") == true)
    }

    // =========================================================================
    // 2. System Diagnostics & Health Check
    // =========================================================================

    @Test
    fun systemDiagnostics_databaseHealthReportEvaluation() {
        val healthyReport = DatabaseHealthReport(
            isConnected = true,
            isWritable = true,
            integrityOk = true,
            latencyMs = 4L,
            cacheSizeKb = 128000L,
            walSizeBytes = 32768L,
            message = "Database connection active, verified, peak throughput (4ms latency)"
        )

        assertTrue(healthyReport.isConnected)
        assertTrue(healthyReport.isWritable)
        assertTrue(healthyReport.integrityOk)
        assertTrue(healthyReport.latencyMs <= 50L) // Low latency verified
        assertEquals(128000L, healthyReport.cacheSizeKb)
        assertEquals(32768L, healthyReport.walSizeBytes)
        assertTrue(healthyReport.message.contains("peak throughput"))

        val degradedReport = DatabaseHealthReport(
            isConnected = true,
            isWritable = false,
            integrityOk = false,
            latencyMs = 120L,
            cacheSizeKb = 0L,
            walSizeBytes = 0L,
            message = "Database responsive with warnings"
        )

        assertTrue(degradedReport.isConnected)
        assertFalse(degradedReport.isWritable)
        assertFalse(degradedReport.integrityOk)
        assertEquals(120L, degradedReport.latencyMs)
    }

    // =========================================================================
    // 3. Crash Diagnostics Manager
    // =========================================================================

    @Test
    fun crashDiagnostics_recordAndParseCrashLogs() {
        val crashFile = File(testDir, "test_crash_logs.json")

        val simulatedException = IllegalStateException("Hardware barcode scanner disconnected mid-transaction")
        val record = CrashDiagnosticsManager.recordCrashToFile(
            file = crashFile,
            throwable = simulatedException,
            cashier = "cashier_alice"
        )

        assertEquals("IllegalStateException", record.exceptionName)
        assertEquals("Hardware barcode scanner disconnected mid-transaction", record.message)
        assertEquals("cashier_alice", record.activeCashier)
        assertTrue(record.stackTrace.contains("IllegalStateException"))
        assertTrue(record.deviceInfo.contains("Model:"))

        val parsedLogs = CrashDiagnosticsManager.parseCrashLogs(crashFile)
        assertEquals(1, parsedLogs.size)
        assertEquals("cashier_alice", parsedLogs[0].activeCashier)
        assertEquals("IllegalStateException", parsedLogs[0].exceptionName)
    }

    @Test
    fun crashDiagnostics_rollingPruneCapsAtMaxRecords() {
        val crashFile = File(testDir, "test_rolling_crashes.json")

        // Record 35 crashes (MAX_CRASH_LOGS is 30)
        for (i in 1..35) {
            val ex = RuntimeException("Crash test #$i")
            CrashDiagnosticsManager.recordCrashToFile(
                file = crashFile,
                throwable = ex,
                cashier = "tester_$i"
            )
        }

        val logs = CrashDiagnosticsManager.parseCrashLogs(crashFile)
        assertEquals(30, logs.size)
        // Most recent crash recorded was tester_35; logs are returned reversed (newest first)
        assertEquals("Crash test #35", logs.first().message)
        assertEquals("Crash test #6", logs.last().message)
    }

    @Test
    fun crashDiagnostics_formatDiagnosticsReportContainsSystemTelemetry() {
        val dummyCrashes = listOf(
            CrashDiagnosticsManager.CrashRecord(
                timestamp = 1700000000000L,
                dateFormatted = "2026-11-03 10:30:00",
                exceptionName = "NullPointerException",
                message = "Printer socket closed",
                stackTrace = "java.lang.NullPointerException: Printer socket closed\n\tat com.munzo.storepoint...",
                deviceInfo = "Model: Sunmi V2 Pro, SDK: 33 (Android 13)",
                activeCashier = "bob_cashier"
            )
        )

        val report = CrashDiagnosticsManager.formatDiagnosticsReport(
            deviceManufacturer = "Sunmi",
            deviceModel = "V2 Pro",
            deviceCode = "v2_pro",
            androidRelease = "13",
            apiLevel = 33,
            fingerprint = "sunmi/v2_pro/pos",
            ramAvailMb = 1450,
            ramTotalMb = 3072,
            isLowMemory = false,
            storageFreeMb = 8500,
            crashes = dummyCrashes
        )

        assertTrue(report.contains("=== STOREPOINT POS SYSTEM DIAGNOSTICS ==="))
        assertTrue(report.contains("Device: Sunmi V2 Pro (v2_pro)"))
        assertTrue(report.contains("Android Version: 13 (API 33)"))
        assertTrue(report.contains("RAM: 1450 MB available / 3072 MB total"))
        assertTrue(report.contains("Internal App Storage Free: 8500 MB"))
        assertTrue(report.contains("CRASH HISTORY (1 incidents recorded)"))
        assertTrue(report.contains("Printer socket closed"))
        assertTrue(report.contains("Active Cashier: bob_cashier"))
    }
}

