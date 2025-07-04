package com.ae.islamicimageviewer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.ae.islamicimageviewer.internal.FaceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ImageProcessor"

internal class ImageProcessor(private val context: Context) {
    private val faceDetector: FaceDetector
    private val genderModel: GenderDetectionModel

    init {
        Log.d(TAG, "Initializing ImageProcessor")
        try {
            faceDetector = FaceDetector()
            Log.d(TAG, "FaceDetector created")

            genderModel = GenderDetectionModel(context)
            Log.d(TAG, "GenderDetectionModel created")

            Log.d(TAG, "ImageProcessor initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ImageProcessor", e)
            throw e
        }
    }

    data class ProcessingResult(
        val shouldBlur: Boolean,
        val reason: String? = null
    )

    suspend fun processImage(bitmap: Bitmap): ProcessingResult {
        return try {
            Log.d(TAG, "Processing image of size: ${bitmap.width}x${bitmap.height}")

            val faces = faceDetector.detectFaces(bitmap)
            Log.d(TAG, "Detected ${faces.size} faces")

            var femaleCount = 0
            var totalFaces = faces.size

            for ((index, face) in faces.withIndex()) {
                try {
                    Log.d(TAG, "Processing face $index")
                    val faceBitmap = cropFace(bitmap, face)
                    val genderResult = genderModel.detectGender(faceBitmap)

                    Log.d(TAG, "Face $index - Female: ${genderResult.isFemale}, Confidence: ${genderResult.confidence}")

                    if (genderResult.isFemale && genderResult.confidence > 0.6f) {
                        femaleCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing face $index", e)
                }
            }

            val shouldBlur = femaleCount > 0
            val reason = when {
                femaleCount > 0 -> "Detected $femaleCount female face(s)"
                totalFaces == 0 -> "No faces detected"
                else -> "All faces approved"
            }

            Log.d(TAG, "Processing complete: shouldBlur=$shouldBlur, reason=$reason")
            ProcessingResult(shouldBlur, reason)
        } catch (e: Exception) {
            Log.e(TAG, "Error in image processing", e)
            ProcessingResult(false, "Processing error: ${e.message}")
        }
    }

    private fun cropFace(bitmap: Bitmap, face: FaceDetector.Face): Bitmap {
        val rect = face.boundingBox

        // Add some padding to the face crop
        val padding = (rect.width() * 0.2).toInt()

        val left = (rect.left - padding).coerceAtLeast(0)
        val top = (rect.top - padding).coerceAtLeast(0)
        val right = (rect.right + padding).coerceAtMost(bitmap.width)
        val bottom = (rect.bottom + padding).coerceAtMost(bitmap.height)

        val width = right - left
        val height = bottom - top

        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

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