package com.munzo.storepoint.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.munzo.storepoint.data.TimeAttendanceRecord
import com.munzo.storepoint.util.PdfExportUtil
import com.munzo.storepoint.util.TimeTrackingAndPayrollHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

fun StorePointViewModel.enrollStaffFace(
    username: String,
    vector: FloatArray,
    templateHash: String,
    onComplete: (Boolean, String) -> Unit
) {
    viewModelScope.launch {
        try {
            repository.registerFaceCredential(
                username = username,
                vector = vector,
                templateHash = templateHash,
                antiSpoofingProfile = "BLINK_AND_HEAD_POSE"
            )
            onComplete(true, "Face biometric enrolled successfully for @$username")
        } catch (e: Exception) {
            onComplete(false, "Failed to enroll face biometric: ${e.localizedMessage}")
        }
    }
}

fun StorePointViewModel.deleteStaffFace(
    username: String,
    onComplete: (Boolean) -> Unit
) {
    viewModelScope.launch {
        try {
            repository.deleteFaceCredential(username)
            onComplete(true)
        } catch (_: Exception) {
            onComplete(false)
        }
    }
}

fun StorePointViewModel.clockInStaff(
    username: String,
    authMethod: String = "FACE",
    notes: String = "",
    onComplete: (TimeAttendanceRecord?, String) -> Unit
) {
    viewModelScope.launch {
        try {
            val activeRec = repository.getActiveAttendanceForUser(username)
            if (activeRec != null) {
                onComplete(null, "@$username is already clocked in since ${formatTime(activeRec.timeIn)}.")
                return@launch
            }
            val record = repository.recordTimeIn(username, authMethod, notes)
            val msg = if (record.tardinessMinutes > 0) {
                "Time In recorded for @$username (${record.tardinessMinutes}m tardiness detected)"
            } else {
                "Time In recorded for @$username (On time)"
            }
            onComplete(record, msg)
        } catch (e: Exception) {
            onComplete(null, "Clock In error: ${e.localizedMessage}")
        }
    }
}

fun StorePointViewModel.clockOutStaff(
    recordId: Int,
    notes: String = "",
    onComplete: (TimeAttendanceRecord?, String) -> Unit
) {
    viewModelScope.launch {
        try {
            val updated = repository.recordTimeOut(recordId, notes)
            if (updated != null) {
                val dur = TimeTrackingAndPayrollHelper.computePayableAndOvertimeHours(
                    updated.timeIn,
                    updated.timeOut ?: System.currentTimeMillis(),
                    updated.totalBreakMinutes,
                    updated.scheduledShiftHours
                )
                onComplete(updated, "Shift closed for @${updated.username}. Total: ${dur.totalWorkedHours}h (OT: ${dur.overtimeHours}h)")
            } else {
                onComplete(null, "Could not find active record #$recordId to clock out.")
            }
        } catch (e: Exception) {
            onComplete(null, "Clock Out error: ${e.localizedMessage}")
        }
    }
}

fun StorePointViewModel.toggleStaffBreak(
    recordId: Int,
    isStarting: Boolean,
    onComplete: (Boolean, String) -> Unit
) {
    viewModelScope.launch {
        try {
            if (isStarting) {
                repository.recordBreakStart(recordId)
                onComplete(true, "Break interval started.")
            } else {
                repository.recordBreakEnd(recordId)
                onComplete(true, "Break concluded. Shift resumed.")
            }
        } catch (e: Exception) {
            onComplete(false, "Break error: ${e.localizedMessage}")
        }
    }
}

fun StorePointViewModel.updateStaffRateTier(
    username: String,
    rateTier: String,
    baseRate: Double,
    shiftStartHour: Int,
    shiftStartMinute: Int,
    onComplete: (Boolean) -> Unit = {}
) {
    viewModelScope.launch {
        try {
            repository.updateStaffPayrollConfig(username, rateTier, baseRate, shiftStartHour, shiftStartMinute)
            onComplete(true)
        } catch (_: Exception) {
            onComplete(false)
        }
    }
}

fun StorePointViewModel.exportAttendanceCsv(context: Context) {
    viewModelScope.launch {
        try {
            val records = repository.getAllAttendanceRecordsSync()
            if (records.isEmpty()) {
                Toast.makeText(context, "No attendance records to export.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val csvText = TimeTrackingAndPayrollHelper.generatePayrollCsv(records)
            val file = File(context.cacheDir, "storepoint_payroll_ledger_${System.currentTimeMillis()}.csv")
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { fos ->
                    fos.write(csvText.toByteArray(Charsets.UTF_8))
                }
            }
            PdfExportUtil.shareFile(context, file, mimeType = "text/csv")
        } catch (e: Exception) {
            Toast.makeText(context, "CSV export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

fun StorePointViewModel.exportAttendancePdf(context: Context, periodTitle: String = "All Records") {
    viewModelScope.launch {
        try {
            val records = repository.getAllAttendanceRecordsSync()
            val config = repository.getStoreConfigSync()
            if (records.isEmpty()) {
                Toast.makeText(context, "No attendance records to export.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (config != null) {
                PdfExportUtil.exportPayrollLedgerPdf(context, config, records, periodTitle)
            } else {
                Toast.makeText(context, "Store configuration not found.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "PDF export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

private fun formatTime(millis: Long): String {
    return java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(millis))
}
