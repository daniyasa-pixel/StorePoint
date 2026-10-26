package com.munzo.storepoint

import com.munzo.storepoint.data.TimeAttendanceRecord
import com.munzo.storepoint.data.User
import com.munzo.storepoint.util.FaceBiometricHelper
import com.munzo.storepoint.util.TimeTrackingAndPayrollHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeAttendanceAndPayrollTest {

    @Test
    fun computeWorkedAndOvertimeHours_withOneHourBreak() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 26, 8, 0, 0)
        val timeIn = cal.timeInMillis

        // 10 hours later (6:00 PM)
        cal.set(2026, Calendar.OCTOBER, 26, 18, 0, 0)
        val timeOut = cal.timeInMillis

        // 60 minutes break taken
        val breakMinutes = 60L

        val durationSummary = TimeTrackingAndPayrollHelper.computePayableAndOvertimeHours(
            timeIn = timeIn,
            timeOut = timeOut,
            breakMinutes = breakMinutes
        )

        // Total gross span = 10.0 hours.
        // Net worked = 10.0 - 1.0 = 9.0 hours.
        // Standard shift cap = 8.0 hours regular.
        // Overtime = 9.0 - 8.0 = 1.0 hour.
        assertEquals(8.0, durationSummary.regularHours, 0.01)
        assertEquals(1.0, durationSummary.overtimeHours, 0.01)
    }

    @Test
    fun computeWorkedHours_lessThanStandardShift_hasZeroOvertime() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 26, 8, 0, 0)
        val timeIn = cal.timeInMillis

        // 5 hours later (1:00 PM)
        cal.set(2026, Calendar.OCTOBER, 26, 13, 0, 0)
        val timeOut = cal.timeInMillis

        val durationSummary = TimeTrackingAndPayrollHelper.computePayableAndOvertimeHours(
            timeIn = timeIn,
            timeOut = timeOut,
            breakMinutes = 0L
        )

        assertEquals(5.0, durationSummary.regularHours, 0.01)
        assertEquals(0.0, durationSummary.overtimeHours, 0.01)
    }

    @Test
    fun computeTardiness_withTenMinuteGracePeriod() {
        // Scheduled shift start: 08:00 AM
        val scheduledHour = 8
        val scheduledMinute = 0

        // Case 1: Arrived at 08:05 AM (within 10-minute grace period) -> 0 minutes tardy
        val calOnTime = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 8, 5, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tardyGrace = TimeTrackingAndPayrollHelper.computeTardinessMinutes(
            timeIn = calOnTime.timeInMillis,
            shiftStartHour = scheduledHour,
            shiftStartMinute = scheduledMinute
        )
        assertEquals(0L, tardyGrace)

        // Case 2: Arrived at 08:10 AM (exactly on 10-minute grace limit) -> 0 minutes tardy
        val calExactGrace = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 8, 10, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tardyExact = TimeTrackingAndPayrollHelper.computeTardinessMinutes(
            timeIn = calExactGrace.timeInMillis,
            shiftStartHour = scheduledHour,
            shiftStartMinute = scheduledMinute
        )
        assertEquals(0L, tardyExact)

        // Case 3: Arrived at 08:35 AM (35 mins after start, past grace) -> 35 minutes tardy
        val calLate = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 8, 35, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tardyLate = TimeTrackingAndPayrollHelper.computeTardinessMinutes(
            timeIn = calLate.timeInMillis,
            shiftStartHour = scheduledHour,
            shiftStartMinute = scheduledMinute
        )
        assertEquals(35L, tardyLate)

        // Case 4: Arrived early at 07:50 AM -> 0 minutes tardy
        val calEarly = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 7, 50, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tardyEarly = TimeTrackingAndPayrollHelper.computeTardinessMinutes(
            timeIn = calEarly.timeInMillis,
            shiftStartHour = scheduledHour,
            shiftStartMinute = scheduledMinute
        )
        assertEquals(0L, tardyEarly)
    }

    @Test
    fun calculatePayroll_hourlyRateTier() {
        val baseRate = 75.00 // ₱75.00/hour
        val regularHours = 8.0
        val overtimeHours = 2.0
        val tardinessMinutes = 10L

        val result = TimeTrackingAndPayrollHelper.calculatePayrollForShift(
            rateTier = "HOURLY",
            baseRate = baseRate,
            regularHours = regularHours,
            overtimeHours = overtimeHours,
            tardinessMinutes = tardinessMinutes
        )

        // Regular Pay = 8.0 * 75.00 = 600.00
        // OT Pay = 2.0 * (75.00 * 1.25) = 2.0 * 93.75 = 187.50
        // Gross Pay = 600.00 + 187.50 = 787.50
        assertEquals(787.50, result.grossPay, 0.01)

        // Tardy Deduction: (10 / 60) * 75.00 = 12.50
        assertEquals(12.50, result.tardinessDeduction, 0.01)

        // Net Pay = 787.50 - 12.50 = 775.00
        assertEquals(775.00, result.netPay, 0.01)
    }

    @Test
    fun calculatePayroll_dailyRateTier() {
        val baseRate = 610.00 // Standard NCR daily minimum wage
        val regularHours = 8.0
        val overtimeHours = 0.0
        val tardinessMinutes = 0L

        val result = TimeTrackingAndPayrollHelper.calculatePayrollForShift(
            rateTier = "DAILY",
            baseRate = baseRate,
            regularHours = regularHours,
            overtimeHours = overtimeHours,
            tardinessMinutes = tardinessMinutes
        )

        assertEquals(610.00, result.grossPay, 0.01)
        assertEquals(0.00, result.tardinessDeduction, 0.01)
        assertEquals(610.00, result.netPay, 0.01)
    }

    @Test
    fun calculatePayroll_monthlyRateTier() {
        // ₱26,000.00/month. 208 working hours (26 days * 8 hours) -> ₱125.00/hr
        val baseRate = 26000.00
        val regularHours = 8.0
        val overtimeHours = 1.0
        val tardinessMinutes = 0L

        val result = TimeTrackingAndPayrollHelper.calculatePayrollForShift(
            rateTier = "MONTHLY",
            baseRate = baseRate,
            regularHours = regularHours,
            overtimeHours = overtimeHours,
            tardinessMinutes = tardinessMinutes
        )

        // Effective hourly rate: 26000 / 208 = 125.00
        // Regular pay: 8.0 * 125.00 = 1000.00
        // Overtime rate: 125.00 * 1.25 = 156.25
        // Gross: 1000.00 + 156.25 = 1156.25
        assertEquals(1156.25, result.grossPay, 0.01)
        assertEquals(1156.25, result.netPay, 0.01)
    }

    @Test
    fun generatePayrollCsv_containsFormattedHeaderAndRows() {
        val record = TimeAttendanceRecord(
            id = 1,
            username = "cashier_juan",
            timeIn = 1700000000000L,
            timeOut = 1700032400000L,
            totalBreakMinutes = 30L,
            authMethod = "FACE",
            rateTier = "HOURLY",
            baseRate = 80.00,
            grossPay = 720.00,
            deductions = 0.0,
            netPay = 720.00,
            notes = "Shift closed normally"
        )

        val csv = TimeTrackingAndPayrollHelper.generatePayrollCsv(
            records = listOf(record)
        )

        assertTrue(csv.contains("Log ID,Staff Username,Date,Rate Tier,Base Rate,Time In,Time Out,Break (mins),Regular Hrs,OT Hrs,Late (mins),Gross Pay,Deductions,Net Pay,Auth Method,Status,Notes"))
        assertTrue(csv.contains("cashier_juan"))
        assertTrue(csv.contains("HOURLY"))
        assertTrue(csv.contains("Shift closed normally"))
    }

    @Test
    fun facialBiometrics_distanceAndCosineSimilarity() {
        // Feature vector size is 8 normalized geometric values
        val registeredFeatures = floatArrayOf(0.12f, 0.45f, 0.33f, 0.78f, 0.55f, 0.23f, 0.89f, 0.41f)
        val serialized = FaceBiometricHelper.serializeVector(registeredFeatures)
        val deserialized = FaceBiometricHelper.deserializeVector(serialized)

        assertEquals(registeredFeatures.size, deserialized.size)
        for (i in registeredFeatures.indices) {
            assertEquals(registeredFeatures[i], deserialized[i], 0.001f)
        }

        // Same face with minor micro-variation
        val queryFeaturesSameFace = floatArrayOf(0.13f, 0.44f, 0.34f, 0.77f, 0.56f, 0.22f, 0.88f, 0.40f)

        val distance = FaceBiometricHelper.computeEuclideanDistance(queryFeaturesSameFace, registeredFeatures)
        val similarity = FaceBiometricHelper.computeCosineSimilarity(queryFeaturesSameFace, registeredFeatures)

        assertTrue("Distance should be small for similar face", distance < 0.10)
        assertTrue("Cosine similarity should be high (>0.98)", similarity > 0.98)

        val isMatch = FaceBiometricHelper.isFaceMatch(queryFeaturesSameFace, registeredFeatures)
        assertTrue("Matching face within threshold should verify", isMatch)

        // Distinctly different face
        val queryFeaturesDifferentFace = floatArrayOf(0.85f, 0.12f, 0.90f, 0.15f, 0.10f, 0.88f, 0.20f, 0.95f)
        val diffDistance = FaceBiometricHelper.computeEuclideanDistance(queryFeaturesDifferentFace, registeredFeatures)
        val isDifferentMatch = FaceBiometricHelper.isFaceMatch(queryFeaturesDifferentFace, registeredFeatures)

        assertTrue("Distance should be large for different face", diffDistance > 0.50)
        assertFalse("Different face vector should be rejected", isDifferentMatch)
    }

    @Test
    fun cryptographicHash_generatesDeterministicSha256() {
        val vector = floatArrayOf(0.12f, 0.45f, 0.33f, 0.78f, 0.55f, 0.23f, 0.89f, 0.41f)
        val hash1 = FaceBiometricHelper.hashTemplate(vector)
        val hash2 = FaceBiometricHelper.hashTemplate(vector)

        assertEquals("Hash must be deterministic", hash1, hash2)
        assertEquals("SHA-256 hex string must be 64 characters", 64, hash1.length)
        assertTrue("Hex string must be alphanumeric hex characters", hash1.all { it.isDigit() || it in 'a'..'f' })
    }
}
