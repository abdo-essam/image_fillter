package com.ae.islamicimageviewer.internal

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import androidx.core.graphics.scale

class EnhancedMediaPipeFaceDetector(private val context: Context) {

    companion object {
        const val SHORT_RANGE_MODEL = "blaze_face_short_range.tflite"
        const val MIN_FACE_DETECTION_CONFIDENCE = 0.3f // Lower confidence for distant faces
        const val MIN_FACE_SUPPRESSION_THRESHOLD = 0.2f
        private const val TAG = "EnhancedFaceDetector"
    }

    private var faceDetector: FaceDetector? = null

    fun setupFaceDetector() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(SHORT_RANGE_MODEL)
            .build()

        val options = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinDetectionConfidence(MIN_FACE_DETECTION_CONFIDENCE)
            .setMinSuppressionThreshold(MIN_FACE_SUPPRESSION_THRESHOLD)
            .setRunningMode(RunningMode.IMAGE)
            .build()

        try {
            faceDetector = FaceDetector.createFromOptions(context, options)
            Log.d(TAG, "Face detector initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize: ${e.message}")
        }
    }

    fun detectFaces(originalBitmap: Bitmap): FaceDetectorResult? {
        if (faceDetector == null) {
            Log.e(TAG, "Face detector not initialized")
            return null
        }

        // Try multiple scales for distant faces
        val scales = listOf(1.0f, 1.5f, 2.0f, 3.0f)
        var bestResult: FaceDetectorResult? = null
        var maxDetections = 0

        for (scale in scales) {
            val scaledBitmap = if (scale == 1.0f) {
                originalBitmap
            } else {
                originalBitmap.scale(
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt()
                )
            }

            try {
                val mpImage = BitmapImageBuilder(scaledBitmap).build()
                val detectionResult = faceDetector?.detect(mpImage)

                val detectionCount = detectionResult?.detections()?.size ?: 0
                Log.d(TAG, "Scale $scale: detected $detectionCount faces")

                // Keep the result with most detections
                if (detectionCount > maxDetections) {
                    maxDetections = detectionCount
                    bestResult = detectionResult

                    // If we're using a scaled image, we need to adjust the coordinates
                    if (scale != 1.0f) {
                        // Note: MediaPipe doesn't allow modifying results directly
                        // So we'll handle coordinate scaling in the calling class
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error detecting at scale $scale: ${e.message}")
            }

            if (scale != 1.0f) {
                scaledBitmap.recycle()
            }
        }

        return bestResult
    }

    fun close() {
        faceDetector?.close()
        faceDetector = null
    }
}