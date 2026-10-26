package com.munzo.storepoint.util

import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.data.TimeAttendanceRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeTrackingAndPayrollHelper {

    data class ShiftDurationSummary(
        val totalWorkedHours: Double,
        val regularHours: Double,
        val overtimeHours: Double,
        val breakMinutes: Long
    )

    data class PayrollCalculationResult(
        val grossPay: Double,
        val overtimePay: Double,
        val tardinessDeduction: Double,
        val netPay: Double,
        val effectiveHourlyRate: Double
    )

    /**
     * Calculates tardiness in minutes for a given clock-in timestamp relative to the staff's scheduled shift start.
     * Enforces a configurable grace period (default 10 minutes).
     */
    fun computeTardinessMinutes(
        timeIn: Long,
        shiftStartHour: Int = 8,
        shiftStartMinute: Int = 0,
        gracePeriodMinutes: Int = 10
    ): Long {
        if (timeIn <= 0) return 0L
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeIn
            set(Calendar.HOUR_OF_DAY, shiftStartHour)
            set(Calendar.MINUTE, shiftStartMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val scheduledStart = cal.timeInMillis
        val diffMs = timeIn - scheduledStart
        if (diffMs <= gracePeriodMinutes * 60L * 1000L) {
            return 0L
        }
        return (diffMs / (60L * 1000L)).coerceAtLeast(0L)
    }

    /**
     * Calculates worked duration, deducting total break duration and segmenting into regular vs. overtime hours.
     */
    fun computePayableAndOvertimeHours(
        timeIn: Long,
        timeOut: Long,
        breakMinutes: Long = 0L,
        standardShiftHours: Double = 8.0
    ): ShiftDurationSummary {
        if (timeOut <= timeIn) {
            return ShiftDurationSummary(0.0, 0.0, 0.0, breakMinutes)
        }
        val grossDurationMs = timeOut - timeIn
        val breakDurationMs = (breakMinutes.coerceAtLeast(0L)) * 60L * 1000L
        val netDurationMs = (grossDurationMs - breakDurationMs).coerceAtLeast(0L)
        val rawWorkedHours = netDurationMs.toDouble() / (3600L * 1000L).toDouble()
        val totalWorkedHours = StorePointRepository.roundMoney(rawWorkedHours)

        val regularHours = StorePointRepository.roundMoney(totalWorkedHours.coerceAtMost(standardShiftHours))
        val overtimeHours = StorePointRepository.roundMoney((totalWorkedHours - standardShiftHours).coerceAtLeast(0.0))

        return ShiftDurationSummary(
            totalWorkedHours = totalWorkedHours,
            regularHours = regularHours,
            overtimeHours = overtimeHours,
            breakMinutes = breakMinutes
        )
    }

    /**
     * Accumulates gross pay, overtime premium (1.25x), tardiness deductions, and net payable compensation
     * across HOURLY, DAILY, and MONTHLY rate tiers.
     */
    fun calculatePayrollForShift(
        rateTier: String,
        baseRate: Double,
        regularHours: Double,
        overtimeHours: Double,
        tardinessMinutes: Long
    ): PayrollCalculationResult {
        val safeBaseRate = if (baseRate.isNaN() || baseRate < 0.0) 0.0 else baseRate
        val normTier = rateTier.uppercase().trim()

        val effectiveHourlyRate: Double = when (normTier) {
            "DAILY" -> StorePointRepository.roundMoney(safeBaseRate / 8.0)
            "MONTHLY" -> StorePointRepository.roundMoney(safeBaseRate / 208.0) // 26 working days * 8 hours
            else -> StorePointRepository.roundMoney(safeBaseRate) // HOURLY
        }

        val regularPay: Double = when (normTier) {
            "DAILY" -> StorePointRepository.roundMoney((regularHours / 8.0) * safeBaseRate)
            else -> StorePointRepository.roundMoney(regularHours * effectiveHourlyRate)
        }

        val overtimeMultiplier = 1.25
        val overtimePay = StorePointRepository.roundMoney(overtimeHours * effectiveHourlyRate * overtimeMultiplier)
        val grossPay = StorePointRepository.roundMoney(regularPay + overtimePay)

        val tardinessDeduction = StorePointRepository.roundMoney(
            (tardinessMinutes.toDouble() / 60.0) * effectiveHourlyRate
        )

        val netPay = StorePointRepository.roundMoney((grossPay - tardinessDeduction).coerceAtLeast(0.0))

        return PayrollCalculationResult(
            grossPay = grossPay,
            overtimePay = overtimePay,
            tardinessDeduction = tardinessDeduction,
            netPay = netPay,
            effectiveHourlyRate = effectiveHourlyRate
        )
    }

    /**
     * Formats a complete CSV export string for staff time attendance and payroll records.
     */
    fun generatePayrollCsv(records: List<TimeAttendanceRecord>): String {
        val sb = StringBuilder()
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        sb.append("Log ID,Staff Username,Date,Rate Tier,Base Rate,Time In,Time Out,Break (mins),Regular Hrs,OT Hrs,Late (mins),Gross Pay,Deductions,Net Pay,Auth Method,Status,Notes\n")

        for (rec in records) {
            val dateStr = dateFormat.format(Date(rec.timeIn))
            val timeInStr = timeFormat.format(Date(rec.timeIn))
            val timeOutStr = rec.timeOut?.let { timeFormat.format(Date(it)) } ?: "IN PROGRESS"

            val duration = computePayableAndOvertimeHours(
                timeIn = rec.timeIn,
                timeOut = rec.timeOut ?: rec.timeIn,
                breakMinutes = rec.totalBreakMinutes,
                standardShiftHours = rec.scheduledShiftHours
            )

            val cleanNotes = rec.notes.replace("\"", "\"\"").replace("\n", " ")

            sb.append("${rec.id},")
            sb.append("\"${rec.username}\",")
            sb.append("\"$dateStr\",")
            sb.append("${rec.rateTier},")
            sb.append(String.format(Locale.US, "%.2f,", rec.baseRate))
            sb.append("\"$timeInStr\",")
            sb.append("\"$timeOutStr\",")
            sb.append("${rec.totalBreakMinutes},")
            sb.append(String.format(Locale.US, "%.2f,", duration.regularHours))
            sb.append(String.format(Locale.US, "%.2f,", duration.overtimeHours))
            sb.append("${rec.tardinessMinutes},")
            sb.append(String.format(Locale.US, "%.2f,", rec.grossPay))
            sb.append(String.format(Locale.US, "%.2f,", rec.deductions))
            sb.append(String.format(Locale.US, "%.2f,", rec.netPay))
            sb.append("${rec.authMethod},")
            sb.append("${rec.status},")
            sb.append("\"$cleanNotes\"\n")
        }
        return sb.toString()
    }
}
