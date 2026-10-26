package com.example.detection

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset

enum class RecognitionStatus(val label: String) {
    SEARCHING("SEARCHING // POSITION FACE"),
    FACE_ACQUIRED("FACE ACQUIRED // LOCKING"),
    TRACKING("ANALYZING BIOMETRICS"),
    VERIFIED("FACE VERIFIED // UNLOCKED"),
    LOST("REALIGN FACE IN RETICLE")
}

data class RealtimeFaceData(
    val isFaceDetected: Boolean = false,
    val faceBounds: RectF? = null,
    val leftEyePos: Offset? = null,
    val rightEyePos: Offset? = null,
    val nosePos: Offset? = null,
    val mouthPos: Offset? = null,
    val headEulerY: Float = 0f,
    val headEulerZ: Float = 0f,
    val lockProgress: Float = 0f,
    val status: RecognitionStatus = RecognitionStatus.SEARCHING
)
