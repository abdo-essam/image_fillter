package com.ae.islamicimageviewer

// Android imports
import android.content.Context
import android.graphics.Bitmap
import android.util.Log

// Internal components
import com.ae.islamicimageviewer.internal.FaceDetector

// Tag used for logging
private const val TAG = "ImageProcessor"

// Internal class responsible for detecting faces and gender classification
internal class ImageProcessor(context: Context) {

    // Face detection component (using ML Kit or similar)
    private val faceDetector: FaceDetector

    // Gender classification model (TensorFlow Lite)
    private val genderModel: GenderDetectionModel

    // Initialization block
    init {
        Log.d(TAG, "Initializing ImageProcessor")
        try {
            // Initialize face detector
            faceDetector = FaceDetector()
            Log.d(TAG, "FaceDetector created")

            // Initialize gender classification model
            genderModel = GenderDetectionModel(context)
            Log.d(TAG, "GenderDetectionModel created")

            Log.d(TAG, "ImageProcessor initialized successfully")
        } catch (e: Exception) {
            // Log and rethrow errors during initialization
            Log.e(TAG, "Failed to initialize ImageProcessor", e)
            throw e
        }
    }

    // Class to hold the result of image processing
    data class ProcessingResult(
        val shouldBlur: Boolean,  // Should the image be blurred?
        val reason: String? = null // Explanation for the decision
    )

    /**
     * Suspend function that processes the input image:
     * - Detects faces
     * - Classifies each face as male/female
     * - Decides whether the image should be blurred
     */
    suspend fun processImage(bitmap: Bitmap): ProcessingResult {
        return try {
            Log.d(TAG, "Processing image of size: ${bitmap.width}x${bitmap.height}")

            // Detect faces in the image
            val faces = faceDetector.detectFaces(bitmap)
            Log.d(TAG, "Detected ${faces.size} faces")

            var femaleCount = 0 // Number of female faces detected
            val totalFaces = faces.size

            // Iterate over each detected face
            for ((index, face) in faces.withIndex()) {
                try {
                    Log.d(TAG, "Processing face $index")

                    // Crop the face region from the original image
                    val faceBitmap = cropFace(bitmap, face)

                    // Predict gender for the cropped face
                    val genderResult = genderModel.detectGender(faceBitmap)

                    Log.d(TAG, "Face $index - Female: ${genderResult.isFemale}, Confidence: ${genderResult.confidence}")

                    // If predicted as female with high confidence, count it
                    if (genderResult.isFemale && genderResult.confidence > 0.6f) {
                        femaleCount++
                    }
                } catch (e: Exception) {
                    // Log error for this specific face, but continue processing others
                    Log.e(TAG, "Error processing face $index", e)
                }
            }

            // Determine final result: whether the image should be blurred or not
            val shouldBlur = femaleCount > 0

            // Reasoning for decision
            val reason = when {
                femaleCount > 0 -> "Detected $femaleCount female face(s)"
                totalFaces == 0 -> "No faces detected"
                else -> "All faces approved"
            }

            Log.d(TAG, "Processing complete: shouldBlur=$shouldBlur, reason=$reason")

            // Return the final result
            ProcessingResult(shouldBlur, reason)

        } catch (e: Exception) {
            // If any general error occurs, log it and return default result
            Log.e(TAG, "Error in image processing", e)
            ProcessingResult(false, "Processing error: ${e.message}")
        }
    }

    /**
     * Crop a face region from the original image using its bounding box.
     * Adds padding to the face for better context.
     */
    private fun cropFace(bitmap: Bitmap, face: FaceDetector.DetectedFace): Bitmap {
        val rect = face.boundingBox

        // Add 20% padding around the face
        val padding = (rect.width() * 0.2).toInt()

        // Calculate new bounds with padding, clamped to image dimensions
        val left = (rect.left - padding).coerceAtLeast(0)
        val top = (rect.top - padding).coerceAtLeast(0)
        val right = (rect.right + padding).coerceAtMost(bitmap.width)
        val bottom = (rect.bottom + padding).coerceAtMost(bitmap.height)

        val width = right - left
        val height = bottom - top

        // Return the cropped face region
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    /**
     * Cleanup resources when no longer needed (e.g. interpreter, detectors)
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up ImageProcessor")
            faceDetector.close()
            genderModel.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
