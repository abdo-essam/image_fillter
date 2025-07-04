package com.ae.islamicimageviewer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.ae.islamicimageviewer.internal.FaceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ImageProcessor(private val context: Context) {
    private val faceDetector = FaceDetector()
    private val genderModel by lazy { GenderDetectionModel(context) }

    data class ProcessingResult(
        val shouldBlur: Boolean,
        val reason: String? = null
    )

    suspend fun processImage(bitmap: Bitmap): ProcessingResult = withContext(Dispatchers.Default) {
        try {
            Log.d("ImageProcessor", "Processing image of size: ${bitmap.width}x${bitmap.height}")

            val faces = faceDetector.detectFaces(bitmap)
            Log.d("ImageProcessor", "Detected ${faces.size} faces")

            var femaleCount = 0
            var totalFaces = faces.size

            for (face in faces) {
                try {
                    val faceBitmap = cropFace(bitmap, face)
                    val genderResult = genderModel.detectGender(faceBitmap)

                    Log.d("ImageProcessor", "Gender detection - Female: ${genderResult.isFemale}, Confidence: ${genderResult.confidence}")

                    if (genderResult.isFemale && genderResult.confidence > 0.6f) {
                        femaleCount++
                    }
                } catch (e: Exception) {
                    Log.e("ImageProcessor", "Error processing face", e)
                }
            }

            val shouldBlur = femaleCount > 0
            val reason = when {
                femaleCount > 0 -> "Detected $femaleCount female face(s)"
                totalFaces == 0 -> null
                else -> null
            }

            ProcessingResult(shouldBlur, reason)
        } catch (e: Exception) {
            Log.e("ImageProcessor", "Error in image processing", e)
            ProcessingResult(false, null)
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
            faceDetector.close()
            genderModel.close()
        } catch (e: Exception) {
            Log.e("ImageProcessor", "Error during cleanup", e)
        }
    }
}