package com.ae.islamicimageviewer.internal

// Import necessary Android and ML Kit classes
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Internal class to encapsulate face detection logic
internal class FaceDetector {

    // Build face detector options: accurate mode (more precise) and no contour detection (for performance)
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // Use more accurate detection (slower)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)              // Disable face contour detection for performance
        .build()

    // Create the face detector client using the defined options
    private val detector = FaceDetection.getClient(options)

    /**
     * Asynchronously detects faces in a given bitmap.
     * Returns a list of detected faces, each with only the bounding box info.
     */
    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace> = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0) // Convert bitmap to ML Kit input image

        // Start face detection
        detector.process(image)
            .addOnSuccessListener { faces ->
                // Map the result to our simplified face model (bounding boxes only)
                val results = faces.map { face ->
                    DetectedFace(face.boundingBox)
                }
                cont.resume(results) // Resume coroutine with result
            }
            .addOnFailureListener { exception ->
                cont.resumeWithException(exception) // Resume coroutine with exception if detection fails
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
