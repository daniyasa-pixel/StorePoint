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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.munzo.storepoint.data.User
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.ExpressiveButton
import com.munzo.storepoint.ui.components.ExpressiveButtonSize
import com.munzo.storepoint.ui.components.ExpressiveButtonVariant
import com.munzo.storepoint.ui.enrollStaffFace
import com.munzo.storepoint.util.FaceBiometricHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffFaceRegistrationDialog(
    user: User,
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit,
    onEnrolled: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    var isLiveVerified by remember { mutableStateOf(false) }
    var livenessFeedback by remember { mutableStateOf("Position face in oval frame...") }
    var capturedFeatureVector by remember { mutableStateOf<FloatArray?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
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
                    Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Enroll Staff Face: @${user.username}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, enabled = !isSaving) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Look directly at the camera and blink once to verify liveness and enroll facial biometric templates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Camera viewfinder container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(
                            width = 2.dp,
                            color = if (isLiveVerified) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
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
                                    // Use front camera if available for kiosk selfie face enrollment
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
                                        if (mediaImage != null && !isSaving) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            detector.process(image)
                                                .addOnSuccessListener { faces ->
                                                    if (faces.isNotEmpty()) {
                                                        val primaryFace = faces[0]
                                                        val status = livenessTracker.evaluate(primaryFace)
                                                        livenessFeedback = status.message
                                                        if (status.isLive) {
                                                            isLiveVerified = true
                                                            capturedFeatureVector = FaceBiometricHelper.extractFeatureVector(primaryFace)
                                                        }
                                                    } else {
                                                        livenessFeedback = "No face in frame. Look toward camera."
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
                                        android.util.Log.e("FaceEnrollment", "Camera binding error", exc)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Oval Guide Overlay
                        Box(
                            modifier = Modifier
                                .size(width = 180.dp, height = 230.dp)
                                .clip(RoundedCornerShape(90.dp))
                                .border(
                                    width = 2.dp,
                                    color = if (isLiveVerified) Color(0xFF10B981) else Color.White.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(90.dp)
                                )
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Camera permission required", color = Color.White)
                        }
                    }
                }

                // Liveness Feedback Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLiveVerified) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiveVerified) Icons.Default.CheckCircle else Icons.Default.Face,
                            contentDescription = null,
                            tint = if (isLiveVerified) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = livenessFeedback,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isLiveVerified) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            ExpressiveButton(
                onClick = {
                    val vector = capturedFeatureVector
                    if (vector != null && isLiveVerified) {
                        isSaving = true
                        val templateHash = FaceBiometricHelper.hashTemplate(vector)
                        viewModel.enrollStaffFace(user.username, vector, templateHash) { success, msg ->
                            isSaving = false
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) {
                                onEnrolled()
                                onDismiss()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please complete liveness verification before saving.", Toast.LENGTH_SHORT).show()
                    }
                },
                label = if (isSaving) "Saving..." else "Confirm & Save Biometric",
                icon = Icons.Default.CameraAlt,
                enabled = isLiveVerified && capturedFeatureVector != null && !isSaving,
                variant = ExpressiveButtonVariant.FILLED,
                size = ExpressiveButtonSize.M
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}
