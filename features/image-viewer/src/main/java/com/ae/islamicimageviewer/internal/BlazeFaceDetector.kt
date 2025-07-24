package com.ae.islamicimageviewer.internal

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

class MediaPipeFaceDetector(private val context: Context) {

    companion object {
        // Model names
        const val SHORT_RANGE_MODEL = "blaze_face_short_range.tflite"
        const val FULL_RANGE_MODEL = "blaze_face_full_range.tflite"
        const val NUM_FACES = 10
        const val MIN_FACE_DETECTION_CONFIDENCE = 0.5f
        const val MIN_FACE_SUPPRESSION_THRESHOLD = 0.3f
        private const val TAG = "MediaPipeFaceDetector"
    }

    private var faceDetector: FaceDetector? = null
    private var runningMode = RunningMode.IMAGE

    fun setupFaceDetector(
        modelName: String = SHORT_RANGE_MODEL,
        runningMode: RunningMode = RunningMode.IMAGE,
        resultListener: ((FaceDetectorResult, MPImage) -> Unit)? = null
    ) {
        this.runningMode = runningMode

        val baseOptionsBuilder = BaseOptions.builder()
            .setModelAssetPath(modelName)

        val optionsBuilder = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setMinDetectionConfidence(MIN_FACE_DETECTION_CONFIDENCE)
            .setMinSuppressionThreshold(MIN_FACE_SUPPRESSION_THRESHOLD)
            .setRunningMode(runningMode)

        // For live stream mode
        if (runningMode == RunningMode.LIVE_STREAM) {
            if (resultListener != null) {
                optionsBuilder.setResultListener { result, image ->
                    resultListener.invoke(result, image)
                }
            }
        }

        try {
            faceDetector = FaceDetector.createFromOptions(context, optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize: ${e.message}")
        }
    }

    // For static images
    fun detectFaces(bitmap: Bitmap): FaceDetectorResult? {
        if (faceDetector == null) {
            Log.e(TAG, "Face detector not initialized")
            return null
        }

        if (runningMode != RunningMode.IMAGE) {
            throw IllegalStateException("Wrong running mode. Use IMAGE mode.")
        }

        val mpImage = BitmapImageBuilder(bitmap).build()
        return faceDetector?.detect(mpImage)
    }

    // For video processing
    fun detectForVideo(bitmap: Bitmap, frameTimestamp: Long): FaceDetectorResult? {
        if (runningMode != RunningMode.VIDEO) {
            throw IllegalStateException("Wrong running mode. Use VIDEO mode.")
        }

        val mpImage = BitmapImageBuilder(bitmap).build()
        return faceDetector?.detectForVideo(mpImage, frameTimestamp)
    }

    // For live stream
    fun detectAsync(bitmap: Bitmap, frameTimestamp: Long) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalStateException("Wrong running mode. Use LIVE_STREAM mode.")
        }

        val mpImage = BitmapImageBuilder(bitmap).build()
        faceDetector?.detectAsync(mpImage, frameTimestamp)
    }

    fun close() {
        faceDetector?.close()
    }
}