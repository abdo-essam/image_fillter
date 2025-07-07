package com.ae.islamicimageviewer.internal

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class FaceDetector {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.1f) // Detect smaller faces
        .enableTracking() // Enable face tracking
        .build()

    private val detector = FaceDetection.getClient(options)

    /**
     * Asynchronously detects faces in a given bitmap.
     * Returns a list of detected faces, each with only the bounding box info.
     */
    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace> = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                val results = faces.map { face ->
                    DetectedFace(face.boundingBox)
                }
                cont.resume(results)
            }
            .addOnFailureListener { exception ->
                cont.resumeWithException(exception)
            }
    }

    /**
     * Clean up the detector when no longer needed.
     */
    fun close() {
        detector.close()
    }

    /**
     * Our custom simplified Face data class — only contains the bounding box of the face.
     */
    data class DetectedFace(val boundingBox: Rect)
}
