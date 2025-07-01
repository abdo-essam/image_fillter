package com.ae.image_viewer_2.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal class ContentAnalyzer(private val context: Context) {

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.1f)
            .enableTracking()
            .build()
    )

    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.6f)
            .build()
    )

    fun analyzeImage(drawable: Drawable?, onResult: (Boolean) -> Unit) {
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: run {
            onResult(false)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hasInappropriateContent = detectInappropriateContent(bitmap)

                CoroutineScope(Dispatchers.Main).launch {
                    onResult(hasInappropriateContent)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onResult(false)
                }
            }
        }
    }

    private suspend fun detectInappropriateContent(bitmap: Bitmap): Boolean {
        val image = InputImage.fromBitmap(bitmap, 0)

        return try {
            // Check 1: Face Detection
            val faces = faceDetector.process(image).await()

            // Analyze faces for gender indicators
            val hasPotentiallyFemaleFace = faces.any { face ->
                analyzeFaceForGender(face)
            }

            if (hasPotentiallyFemaleFace) {
                return true
            }

            // Check 2: Image Labels
            val labels = imageLabeler.process(image).await()

            // Log detected labels for debugging
            labels.forEach { label ->
                android.util.Log.d("ContentAnalyzer", "Label: ${label.text}, Confidence: ${label.confidence}")
            }

            // Check for inappropriate labels
            labels.any { label ->
                val labelText = label.text.lowercase()
                inappropriateLabels.any { inappropriate ->
                    labelText.contains(inappropriate) && label.confidence > 0.6f
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("ContentAnalyzer", "Error analyzing image", e)
            false
        }
    }

    private fun analyzeFaceForGender(face: Face): Boolean {
        // Analyze facial characteristics that might indicate gender
        // Note: This is a simplified heuristic approach

        // Check smile probability (studies show women tend to smile more in photos)
        val highSmileProbability = face.smilingProbability?.let { it > 0.7f } ?: false

        // Check eye open probability (makeup might affect eye detection)
        val eyeCharacteristics = (face.leftEyeOpenProbability ?: 0f) + (face.rightEyeOpenProbability ?: 0f) / 2

        // Face tracking ID presence (longer hair might affect tracking)
        val hasTrackingId = face.trackingId != null

        // Combine heuristics (this is not 100% accurate but provides a baseline)
        // In production, use a proper gender classification model
        return highSmileProbability || (eyeCharacteristics > 0.8f && hasTrackingId)
    }

    fun cleanup() {
        try {
            faceDetector.close()
            imageLabeler.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    companion object {
        private val inappropriateLabels = setOf(
            "woman", "women", "girl", "lady", "female", "her", "she",
            "dress", "skirt", "bikini", "swimsuit", "lingerie",
            "breast", "chest", "cleavage", "skin", "bare",
            "model", "fashion", "beauty", "makeup", "lipstick",
            "hair", "long hair", "blonde", "brunette",
            "person" // Will need additional checks if person is detected
        )
    }
}