package com.munzo.storepoint.ui.screens

import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.munzo.storepoint.data.TimeAttendanceRecord
import com.munzo.storepoint.data.User
import com.munzo.storepoint.ui.*
import com.munzo.storepoint.ui.components.ExpressiveButton
import com.munzo.storepoint.ui.components.ExpressiveButtonSize
import com.munzo.storepoint.ui.components.ExpressiveButtonVariant
import com.munzo.storepoint.util.FaceBiometricHelper
import com.munzo.storepoint.util.SecurityHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAttendanceDialog(
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val credentials by viewModel.allActiveFaceCredentials.collectAsState()
    val allAttendance by viewModel.allAttendanceRecords.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Face Scan, 1 = PIN Fallback
    var identifiedUser by remember { mutableStateOf<User?>(null) }
    var activeShiftRecord by remember { mutableStateOf<TimeAttendanceRecord?>(null) }
    var actionInProgress by remember { mutableStateOf(false) }

    // Fallback PIN state
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Camera and Face Biometric State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val livenessTracker = remember { FaceBiometricHelper.LivenessTracker() }
    var livenessFeedback by remember { mutableStateOf("Looking for staff face...") }
    var isLiveVerified by remember { mutableStateOf(false) }

    // Update active shift record when identified user changes
    LaunchedEffect(identifiedUser, allAttendance) {
        val user = identifiedUser
        if (user != null) {
            activeShiftRecord = allAttendance.firstOrNull {
                it.username.equals(user.username, ignoreCase = true) && it.status == "ACTIVE"
            }
        } else {
            activeShiftRecord = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = { if (!actionInProgress) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Staff Attendance & Time Clock", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onDismiss, enabled = !actionInProgress) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab Selector: Face Biometric vs PIN Fallback
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Face Biometric") },
                        icon = { Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Fallback PIN") },
                        icon = { Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                if (selectedTab == 0) {
                    // Face Scan Mode
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(
                                width = 2.dp,
                                color = if (identifiedUser != null) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCameraPermission) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }

                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build()
                                        val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        } else {
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        }

                                        val detector = FaceDetection.getClient(FaceBiometricHelper.createDetectorOptions())

                                        val analysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null && !actionInProgress && identifiedUser == null) {
                                                val image = InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.imageInfo.rotationDegrees
                                                )
                                                detector.process(image)
                                                    .addOnSuccessListener { faces ->
                                                        if (faces.isNotEmpty()) {
                                                            val face = faces[0]
                                                            val status = livenessTracker.evaluate(face)
                                                            livenessFeedback = status.message
                                                            isLiveVerified = status.isLive

                                                            val capturedVector = FaceBiometricHelper.extractFeatureVector(face)

                                                            // Match against registered staff
                                                            var bestMatchUser: User? = null
                                                            for (cred in credentials) {
                                                                val enrolledVector = FaceBiometricHelper.deserializeVector(cred.faceLandmarkPoints)
                                                                if (enrolledVector.isNotEmpty() && FaceBiometricHelper.isFaceMatch(capturedVector, enrolledVector)) {
                                                                    bestMatchUser = allUsers.firstOrNull { it.username.equals(cred.username, ignoreCase = true) }
                                                                    break
                                                                }
                                                            }

                                                            if (bestMatchUser != null) {
                                                                identifiedUser = bestMatchUser
                                                                livenessFeedback = "Identified: @${bestMatchUser.username}"
                                                            }
                                                        } else {
                                                            livenessFeedback = "Position face in camera view"
                                                        }
                                                    }
                                                    .addOnCompleteListener {
                                                        imageProxy.close()
                                                    }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }

                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                cameraSelector,
                                                preview,
                                                analysis
                                            )
                                            preview.setSurfaceProvider(previewView.surfaceProvider)
                                        } catch (exc: Exception) {
                                            android.util.Log.e("FaceAttendance", "Camera binding failed", exc)
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))

                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Oval overlay
                            Box(
                                modifier = Modifier
                                    .size(width = 160.dp, height = 210.dp)
                                    .clip(RoundedCornerShape(80.dp))
                                    .border(
                                        width = 2.dp,
                                        color = if (identifiedUser != null) Color(0xFF10B981) else Color.White.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(80.dp)
                                    )
                            )
                        } else {
                            Text("Camera permission required", color = Color.White)
                        }
                    }

                    // Feedback Bar
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (identifiedUser != null) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (identifiedUser != null) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (identifiedUser != null) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = livenessFeedback,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (identifiedUser != null) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                } else {
                    // PIN Fallback Mode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Enter your 4-digit Staff PIN:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    enteredPin = it
                                    pinError = null
                                    if (it.length == 4) {
                                        // Attempt PIN resolution
                                        val matched = allUsers.firstOrNull { u -> SecurityHelper.verifyPin(it, u.pinHash).isMatch }
                                        if (matched != null) {
                                            identifiedUser = matched
                                        } else {
                                            pinError = "Invalid PIN. No staff account matched."
                                            identifiedUser = null
                                        }
                                    }
                                }
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            isError = pinError != null,
                            label = { Text("4-Digit PIN") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        pinError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Staff Identification & Shift Action Panel
                identifiedUser?.let { user ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "@${user.username} (${user.role})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Tier: ${user.rateTier} @ ₱${String.format(Locale.US, "%.2f", user.baseRate)} | Shift: %02d:%02d".format(user.shiftStartHour, user.shiftStartMinute),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = if (activeShiftRecord != null) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                                ) {
                                    Text(
                                        text = if (activeShiftRecord != null) "CLOCKED IN" else "OFF CLOCK",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            activeShiftRecord?.let { rec ->
                                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                Text(
                                    text = "Started shift at ${timeFormat.format(Date(rec.timeIn))}" +
                                            if (rec.breakStart != null) " (Currently on break)" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val currentRecord = activeShiftRecord
                                if (currentRecord == null) {
                                    // Not clocked in -> Clock In
                                    ExpressiveButton(
                                        onClick = {
                                            actionInProgress = true
                                            val auth = if (selectedTab == 0) "FACE" else "FALLBACK_PIN"
                                            viewModel.clockInStaff(user.username, auth) { rec, msg ->
                                                actionInProgress = false
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                if (rec != null) {
                                                    onDismiss()
                                                }
                                            }
                                        },
                                        label = "Time In (Clock In)",
                                        icon = Icons.Default.Login,
                                        variant = ExpressiveButtonVariant.FILLED,
                                        size = ExpressiveButtonSize.M,
                                        enabled = !actionInProgress,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    // Clocked in -> Clock Out & Break Controls
                                    val isOnBreak = currentRecord.breakStart != null
                                    OutlinedButton(
                                        onClick = {
                                            actionInProgress = true
                                            viewModel.toggleStaffBreak(currentRecord.id, !isOnBreak) { ok, msg ->
                                                actionInProgress = false
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = !actionInProgress,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isOnBreak) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isOnBreak) "End Break" else "Start Break")
                                    }

                                    ExpressiveButton(
                                        onClick = {
                                            actionInProgress = true
                                            viewModel.clockOutStaff(currentRecord.id) { rec, msg ->
                                                actionInProgress = false
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                if (rec != null) {
                                                    onDismiss()
                                                }
                                            }
                                        },
                                        label = "Time Out",
                                        icon = Icons.Default.Logout,
                                        variant = ExpressiveButtonVariant.FILLED,
                                        size = ExpressiveButtonSize.M,
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                        enabled = !actionInProgress,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    identifiedUser = null
                                    livenessTracker.reset()
                                    enteredPin = ""
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Switch Staff Member", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !actionInProgress) {
                Text("Close")
            }
        }
    )
}
