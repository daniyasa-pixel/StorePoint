package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munzo.storepoint.data.TimeAttendanceRecord
import com.munzo.storepoint.data.User
import com.munzo.storepoint.ui.*
import com.munzo.storepoint.ui.components.ExpressiveButton
import com.munzo.storepoint.ui.components.ExpressiveButtonSize
import com.munzo.storepoint.ui.components.ExpressiveButtonVariant
import com.munzo.storepoint.util.TimeTrackingAndPayrollHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceTab(
    viewModel: StorePointViewModel
) {
    val context = LocalContext.current
    val storeConfig by viewModel.storeConfig.collectAsState()
    val attendanceRecords by viewModel.allAttendanceRecords.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    val curr = storeConfig?.currencySymbol ?: "$"

    var showClockDialog by remember { mutableStateOf(false) }
    var editingUserForRates by remember { mutableStateOf<User?>(null) }
    var userToEnrollFace by remember { mutableStateOf<User?>(null) }

    // Summary Computations
    val totalGross = remember(attendanceRecords) { attendanceRecords.sumOf { it.grossPay } }
    val totalDeductions = remember(attendanceRecords) { attendanceRecords.sumOf { it.deductions } }
    val totalNetPay = remember(attendanceRecords) { attendanceRecords.sumOf { it.netPay } }
    val totalLateMinutes = remember(attendanceRecords) { attendanceRecords.sumOf { it.tardinessMinutes } }
    val totalOtMinutes = remember(attendanceRecords) { attendanceRecords.sumOf { it.overtimeMinutes } }
    val activeStaffCount = remember(attendanceRecords) { attendanceRecords.count { it.status == "ACTIVE" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Staff Attendance & Payroll Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Biometric Time Tracking • Late & Overtime Calculations • Multi-tier Payroll",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.exportAttendanceCsv(context) }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV")
                }

                OutlinedButton(onClick = { viewModel.exportAttendancePdf(context) }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export PDF")
                }

                ExpressiveButton(
                    onClick = { showClockDialog = true },
                    label = "Time Clock",
                    icon = Icons.Default.AccessTime,
                    variant = ExpressiveButtonVariant.FILLED,
                    size = ExpressiveButtonSize.M
                )
            }
        }

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PayrollMetricCard(
                title = "Total Net Payable",
                value = "$curr${String.format(Locale.US, "%.2f", totalNetPay)}",
                subtitle = "Gross: $curr${String.format(Locale.US, "%.2f", totalGross)}",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            PayrollMetricCard(
                title = "Late Deductions",
                value = "$curr${String.format(Locale.US, "%.2f", totalDeductions)}",
                subtitle = "$totalLateMinutes total late mins",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )

            PayrollMetricCard(
                title = "Overtime Accumulated",
                value = "${String.format(Locale.US, "%.1f", totalOtMinutes / 60.0)} hrs",
                subtitle = "1.25x premium applied",
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )

            PayrollMetricCard(
                title = "On Clock Today",
                value = "$activeStaffCount staff",
                subtitle = "Currently active",
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider()

        // Content Tabs / Lists
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: Staff Rate Tiers & Shift Schedules
            item {
                Text(
                    text = "Staff Rate Tiers & Scheduled Shifts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(allUsers) { user ->
                StaffRateRow(
                    user = user,
                    curr = curr,
                    onEdit = { editingUserForRates = user },
                    onEnrollFace = { userToEnrollFace = user }
                )
            }

            // Section 2: Attendance Records Log
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attendance Records Log (${attendanceRecords.size} records)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (attendanceRecords.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No time attendance records recorded yet.", fontWeight = FontWeight.Medium)
                            Text("Staff can time in using the facial recognition scanner or fallback PIN.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                items(attendanceRecords) { record ->
                    AttendanceRecordCard(record = record, curr = curr)
                }
            }
        }
    }

    if (showClockDialog) {
        StaffAttendanceDialog(
            viewModel = viewModel,
            onDismiss = { showClockDialog = false }
        )
    }

    editingUserForRates?.let { user ->
        StaffRateEditDialog(
            user = user,
            viewModel = viewModel,
            onDismiss = { editingUserForRates = null }
        )
    }

    userToEnrollFace?.let { user ->
        StaffFaceRegistrationDialog(
            user = user,
            viewModel = viewModel,
            onDismiss = { userToEnrollFace = null },
            onEnrolled = { userToEnrollFace = null }
        )
    }
}

@Composable
private fun PayrollMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun StaffRateRow(
    user: User,
    curr: String,
    onEdit: () -> Unit,
    onEnrollFace: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(user.username.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("@${user.username}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ) {
                            Text(user.role, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        if (user.faceEnrolled) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Text("Face Enrolled", color = Color(0xFF10B981), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(
                        text = "Rate: ${user.rateTier} ($curr${String.format(Locale.US, "%.2f", user.baseRate)}) • Scheduled Start: %02d:%02d".format(user.shiftStartHour, user.shiftStartMinute),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEnrollFace) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = "Enroll Face Biometrics",
                        tint = if (user.faceEnrolled) Color(0xFF10B981) else MaterialTheme.colorScheme.tertiary
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Rates", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AttendanceRecordCard(
    record: TimeAttendanceRecord,
    curr: String
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    val isCompleted = record.status == "COMPLETED"
    val duration = TimeTrackingAndPayrollHelper.computePayableAndOvertimeHours(
        timeIn = record.timeIn,
        timeOut = record.timeOut ?: System.currentTimeMillis(),
        breakMinutes = record.totalBreakMinutes,
        standardShiftHours = record.scheduledShiftHours
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "@${record.username}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF10B981).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isCompleted) "COMPLETED" else "ACTIVE SHIFT",
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF10B981),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = record.authMethod,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = dateFormat.format(Date(record.timeIn)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Time In: ${timeFormat.format(Date(record.timeIn))}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = record.timeOut?.let { "Time Out: ${timeFormat.format(Date(it))}" } ?: "In Progress...",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (record.timeOut != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Duration: ${String.format(Locale.US, "%.1f", duration.totalWorkedHours)}h (${String.format(Locale.US, "%.1f", duration.regularHours)}h reg / ${String.format(Locale.US, "%.1f", duration.overtimeHours)}h ot)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (record.tardinessMinutes > 0) {
                        Text(
                            text = "Tardy: ${record.tardinessMinutes} mins (-$curr${String.format(Locale.US, "%.2f", record.deductions)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rate: ${record.rateTier} ($curr${String.format(Locale.US, "%.2f", record.baseRate)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Text(
                    text = "Net Pay: $curr${String.format(Locale.US, "%.2f", record.netPay)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StaffRateEditDialog(
    user: User,
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var rateTier by remember { mutableStateOf(user.rateTier) }
    var baseRateStr by remember { mutableStateOf(String.format(Locale.US, "%.2f", user.baseRate)) }
    var startHourStr by remember { mutableStateOf(user.shiftStartHour.toString()) }
    var startMinStr by remember { mutableStateOf("%02d".format(user.shiftStartMinute)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payroll Settings: @${user.username}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Rate Tier:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HOURLY", "DAILY", "MONTHLY").forEach { tier ->
                        FilterChip(
                            selected = rateTier == tier,
                            onClick = { rateTier = tier },
                            label = { Text(tier) }
                        )
                    }
                }

                OutlinedTextField(
                    value = baseRateStr,
                    onValueChange = { baseRateStr = it },
                    label = { Text("Base Rate (₱)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startHourStr,
                        onValueChange = { startHourStr = it },
                        label = { Text("Shift Hour (0-23)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = startMinStr,
                        onValueChange = { startMinStr = it },
                        label = { Text("Minute (0-59)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            ExpressiveButton(
                onClick = {
                    val rate = baseRateStr.toDoubleOrNull() ?: 0.0
                    val hour = startHourStr.toIntOrNull()?.coerceIn(0, 23) ?: 8
                    val min = startMinStr.toIntOrNull()?.coerceIn(0, 59) ?: 0

                    viewModel.updateStaffRateTier(user.username, rateTier, rate, hour, min) { ok ->
                        if (ok) {
                            Toast.makeText(context, "Payroll settings updated for @${user.username}", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                },
                label = "Save Settings",
                variant = ExpressiveButtonVariant.FILLED,
                size = ExpressiveButtonSize.M
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

