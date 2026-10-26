package com.munzo.storepoint.util

import android.graphics.PointF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.sqrt

object FaceBiometricHelper {

    const val MATCH_DISTANCE_THRESHOLD = 0.28
    const val MATCH_COSINE_THRESHOLD = 0.92

    data class LivenessStatus(
        val isLive: Boolean,
        val blinkDetected: Boolean,
        val message: String
    )

    class LivenessTracker {
        private var wasEyesOpen = false
        private var blinkCount = 0
        private val yawHistory = mutableListOf<Float>()
        private val pitchHistory = mutableListOf<Float>()

        fun reset() {
            wasEyesOpen = false
            blinkCount = 0
            yawHistory.clear()
            pitchHistory.clear()
        }

        fun evaluate(face: Face): LivenessStatus {
            val leftEye = face.leftEyeOpenProbability ?: -1f
            val rightEye = face.rightEyeOpenProbability ?: -1f

            val isEyesOpen = leftEye > 0.65f && rightEye > 0.65f
            val isEyesClosed = leftEye in 0.0f..0.30f && rightEye in 0.0f..0.30f

            if (wasEyesOpen && isEyesClosed) {
                blinkCount++
                wasEyesOpen = false
            } else if (isEyesOpen) {
                wasEyesOpen = true
            }

            // Head pose variation
            yawHistory.add(face.headEulerAngleY)
            pitchHistory.add(face.headEulerAngleX)
            if (yawHistory.size > 10) yawHistory.removeAt(0)
            if (pitchHistory.size > 10) pitchHistory.removeAt(0)

            val yawStdDev = calculateStdDev(yawHistory)
            val pitchStdDev = calculateStdDev(pitchHistory)
            val hasMicroMovement = (yawStdDev > 0.8f || pitchStdDev > 0.8f) && yawHistory.size >= 5

            val isLive = blinkCount >= 1 || (hasMicroMovement && yawHistory.size >= 8)
            val msg = when {
                blinkCount >= 1 -> "Liveness verified (Blink confirmed)"
                hasMicroMovement -> "Liveness verified (Natural movement)"
                wasEyesOpen -> "Please blink once for liveness check"
                else -> "Hold face steady in camera view"
            }

            return LivenessStatus(
                isLive = isLive,
                blinkDetected = blinkCount >= 1,
                message = msg
            )
        }

        private fun calculateStdDev(values: List<Float>): Float {
            if (values.size < 2) return 0f
            val mean = values.average().toFloat()
            val sumSquaredDiff = values.sumOf { ((it - mean) * (it - mean)).toDouble() }
            return sqrt(sumSquaredDiff / values.size).toFloat()
        }
    }

    /**
     * Standard ML Kit Face Detector options configured for fast on-device inference,
     * landmark detection, and classification (eyes open / blink detection).
     */
    fun createDetectorOptions(): FaceDetectorOptions {
        return FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.20f)
            .enableTracking()
            .build()
    }

    /**
     * Extracts a normalized, scale-invariant 8-dimensional geometric face feature vector from ML Kit landmarks.
     */
    fun extractFeatureVector(face: Face): FloatArray {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
        val mouthL = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mouthR = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position

        val boxWidth = face.boundingBox.width().toFloat().coerceAtLeast(1f)
        val boxHeight = face.boundingBox.height().toFloat().coerceAtLeast(1f)
        val aspectRatio = boxHeight / boxWidth

        if (leftEye == null || rightEye == null || nose == null) {
            // Fallback estimation using bounding box proportions
            return floatArrayOf(
                aspectRatio,
                0.5f,
                0.8f,
                0.4f,
                1.0f,
                1.0f,
                face.headEulerAngleX / 45f,
                face.headEulerAngleY / 45f
            )
        }

        val interEyeDist = hypot(rightEye.x - leftEye.x, rightEye.y - leftEye.y).coerceAtLeast(1f)
        val midEyeX = (leftEye.x + rightEye.x) / 2f
        val midEyeY = (leftEye.y + rightEye.y) / 2f

        val eyeToNoseDist = hypot(nose.x - midEyeX, nose.y - midEyeY) / interEyeDist

        val midMouthX = if (mouthL != null && mouthR != null) (mouthL.x + mouthR.x) / 2f else nose.x
        val midMouthY = if (mouthL != null && mouthR != null) (mouthL.y + mouthR.y) / 2f else nose.y + interEyeDist * 0.7f
        val eyeToMouthDist = hypot(midMouthX - midEyeX, midMouthY - midEyeY) / interEyeDist

        val mouthWidth = if (mouthL != null && mouthR != null) hypot(mouthR.x - mouthL.x, mouthR.y - mouthL.y) / interEyeDist else 0.75f

        val noseToChin = if (mouthBottom != null) hypot(mouthBottom.x - nose.x, mouthBottom.y - nose.y) / interEyeDist else 0.55f

        val dLeftEyeNose = hypot(nose.x - leftEye.x, nose.y - leftEye.y)
        val dRightEyeNose = hypot(nose.x - rightEye.x, nose.y - rightEye.y)
        val eyeNoseSymmetry = (dLeftEyeNose / dRightEyeNose.coerceAtLeast(0.1f)).coerceIn(0.5f, 2.0f)

        val mouthNoseSymmetry = if (mouthL != null && mouthR != null) {
            val dL = hypot(mouthL.x - nose.x, mouthL.y - nose.y)
            val dR = hypot(mouthR.x - nose.x, mouthR.y - nose.y)
            (dL / dR.coerceAtLeast(0.1f)).coerceIn(0.5f, 2.0f)
        } else {
            1.0f
        }

        return floatArrayOf(
            aspectRatio,
            eyeToNoseDist,
            eyeToMouthDist,
            mouthWidth,
            noseToChin,
            eyeNoseSymmetry,
            mouthNoseSymmetry,
            interEyeDist / boxWidth
        )
    }

    /**
     * Calculates Euclidean distance between two feature vectors.
     */
    fun computeEuclideanDistance(v1: FloatArray, v2: FloatArray): Double {
        if (v1.size != v2.size || v1.isEmpty()) return Double.MAX_VALUE
        var sum = 0.0
        for (i in v1.indices) {
            val diff = (v1[i] - v2[i]).toDouble()
            sum += diff * diff
        }
        return sqrt(sum)
    }

    /**
     * Calculates Cosine Similarity between two feature vectors.
     */
    fun computeCosineSimilarity(v1: FloatArray, v2: FloatArray): Double {
        if (v1.size != v2.size || v1.isEmpty()) return 0.0
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        if (denom <= 0.0) return 0.0
        return dot / denom
    }

    /**
     * Determines if a live captured face vector matches a stored enrollment vector.
     */
    fun isFaceMatch(
        captured: FloatArray,
        enrolled: FloatArray,
        distanceThreshold: Double = MATCH_DISTANCE_THRESHOLD,
        cosineThreshold: Double = MATCH_COSINE_THRESHOLD
    ): Boolean {
        val distance = computeEuclideanDistance(captured, enrolled)
        val similarity = computeCosineSimilarity(captured, enrolled)
        return distance <= distanceThreshold || similarity >= cosineThreshold
    }

    /**
     * Serializes a vector to a comma-separated string for persistence.
     */
    fun serializeVector(vector: FloatArray): String {
        return vector.joinToString(",") { String.format(Locale.US, "%.4f", it) }
    }

    /**
     * Deserializes a comma-separated vector string back to FloatArray.
     */
    fun deserializeVector(serialized: String): FloatArray {
        if (serialized.isBlank()) return FloatArray(0)
        return try {
            serialized.split(",").map { it.trim().toFloat() }.toFloatArray()
        } catch (_: Exception) {
            FloatArray(0)
        }
    }

    /**
     * Generates a secure SHA-256 hash of the quantized facial biometric template.
     */
    fun hashTemplate(vector: FloatArray): String {
        val quantized = serializeVector(vector)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(quantized.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
