package com.ae.islamicimageviewer.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "FaceDetector"

internal class FaceDetector(context: Context) {

    private val blazeFaceDetector = MediaPipeFaceDetector(context)

    init {
        // Initialize the detector with default settings
        blazeFaceDetector.setupFaceDetector()
    }

    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace> = withContext(Dispatchers.Default) {
        try {
            val result = blazeFaceDetector.detectFaces(bitmap)

            // Convert MediaPipe results to FaceDetector.DetectedFace
            result?.detections()?.map { detection ->
                val boundingBox = detection.boundingBox()
                DetectedFace(
                    boundingBox = Rect(
                        boundingBox.left.toInt(),
                        boundingBox.top.toInt(),
                        boundingBox.right.toInt(),
                        boundingBox.bottom.toInt()
                    )
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting faces", e)
            emptyList()
        }
    }

    fun close() {
        blazeFaceDetector.close()
    }

    data class DetectedFace(val boundingBox: Rect)
}