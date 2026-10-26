package com.example.detection

import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

class RealtimeFaceAnalyzer(
    private val onFaceDataUpdate: (RealtimeFaceData) -> Unit
) : ImageAnalysis.Analyzer {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    private var stableFrameCount = 0
    private val requiredFramesToVerify = 24 // ~0.8-1.0 sec at 24-30 fps for snappy recognition
    private var verifiedTimestamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Dimensions of rotated image frame
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val imgWidth = if (isRotated) imageProxy.height.toFloat() else imageProxy.width.toFloat()
        val imgHeight = if (isRotated) imageProxy.width.toFloat() else imageProxy.height.toFloat()

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val primaryFace = faces[0]
                    handleFaceDetected(primaryFace, imgWidth, imgHeight)
                } else {
                    handleFaceLost()
                }
            }
            .addOnFailureListener {
                handleFaceLost()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleFaceDetected(face: Face, imgWidth: Float, imgHeight: Float) {
        val now = System.currentTimeMillis()
        if (verifiedTimestamp > 0L) {
            if (now - verifiedTimestamp < 2000L) {
                // Keep showing verified status during cooldown
                return
            } else {
                verifiedTimestamp = 0L
                stableFrameCount = 0
            }
        }

        stableFrameCount++

        val progress = (stableFrameCount.toFloat() / requiredFramesToVerify).coerceIn(0f, 1f)
        val status = when {
            progress >= 1f -> {
                verifiedTimestamp = now
                RecognitionStatus.VERIFIED
            }
            progress > 0.25f -> RecognitionStatus.TRACKING
            else -> RecognitionStatus.FACE_ACQUIRED
        }

        // Normalize bounding box coordinates to 0f..1f (mirror horizontally for front camera)
        val box = face.boundingBox
        val normLeft = (1f - (box.right.toFloat() / imgWidth)).coerceIn(0f, 1f)
        val normRight = (1f - (box.left.toFloat() / imgWidth)).coerceIn(0f, 1f)
        val normTop = (box.top.toFloat() / imgHeight).coerceIn(0f, 1f)
        val normBottom = (box.bottom.toFloat() / imgHeight).coerceIn(0f, 1f)

        val normalizedBounds = RectF(normLeft, normTop, normRight, normBottom)

        // Extract and normalize facial landmarks
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position?.let {
            Offset(1f - (it.x / imgWidth), it.y / imgHeight)
        }
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.let {
            Offset(1f - (it.x / imgWidth), it.y / imgHeight)
        }
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position?.let {
            Offset(1f - (it.x / imgWidth), it.y / imgHeight)
        }
        val mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position?.let {
            Offset(1f - (it.x / imgWidth), it.y / imgHeight)
        }

        val data = RealtimeFaceData(
            isFaceDetected = true,
            faceBounds = normalizedBounds,
            leftEyePos = leftEye,
            rightEyePos = rightEye,
            nosePos = nose,
            mouthPos = mouth,
            headEulerY = face.headEulerAngleY,
            headEulerZ = face.headEulerAngleZ,
            lockProgress = progress,
            status = status
        )

        onFaceDataUpdate(data)
    }

    private fun handleFaceLost() {
        if (stableFrameCount > 0) {
            stableFrameCount = kotlin.math.max(0, stableFrameCount - 2)
        }

        val data = RealtimeFaceData(
            isFaceDetected = false,
            faceBounds = null,
            lockProgress = (stableFrameCount.toFloat() / requiredFramesToVerify).coerceIn(0f, 1f),
            status = if (stableFrameCount > 0) RecognitionStatus.LOST else RecognitionStatus.SEARCHING
        )
        onFaceDataUpdate(data)
    }
}
