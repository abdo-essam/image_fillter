package com.ae.image_viewer.internal

import android.graphics.Bitmap
import android.util.Log
import com.ae.image_viewer.model.ContentAnalysisResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

internal class ContentFilter {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.3f)
            .build()
    )

    // Face detector with classification
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.1f) // Detect smaller faces
            .enableTracking() // Enable face tracking
            .build()
    )

    private val femaleIndicators = setOf(
        "woman", "women", "girl", "female", "lady", "ladies",
        "dress", "skirt", "blouse", "makeup", "lipstick",
        "earrings", "jewelry", "handbag",
         "long hair", "hairstyle", "ponytail", "braid"
    )

    // Add male indicators to explicitly identify males
    private val maleIndicators = setOf(
        "man", "men", "boy", "male", "gentleman", "guy",
        "beard", "mustache", "facial hair", "suit", "tie",
        "businessman", "father", "brother"
    )

    private val inappropriateKeywords = setOf(
        "alcohol", "wine", "beer", "liquor", "cocktail",
        "pork", "bacon", "ham", "pig",
        "gambling", "casino", "betting",
        "bikini", "swimsuit", "underwear", "lingerie",
        "nightclub", "bar", "pub"
    )

    suspend fun analyzeImage(bitmap: Bitmap): ContentAnalysisResult = coroutineScope {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // Run face detection and label detection in parallel
            val facesDeferred = async {
                try {
                    faceDetector.process(inputImage).await()
                } catch (e: Exception) {
                    Log.e("ContentFilter", "Face detection failed", e)
                    emptyList<Face>()
                }
            }

            val labelsDeferred = async {
                try {
                    labeler.process(inputImage).await()
                } catch (e: Exception) {
                    Log.e("ContentFilter", "Label detection failed", e)
                    emptyList()
                }
            }

            val faces = facesDeferred.await()
            val labels = labelsDeferred.await()

            // Log results
            Log.d("ContentFilter", "=== Analysis Results ===")
            Log.d("ContentFilter", "Faces detected: ${faces.size}")
            Log.d("ContentFilter", "Labels detected: ${labels.size}")

            // Log labels
            labels.forEach { label ->
                Log.d("ContentFilter", "Label: ${label.text} (${(label.confidence * 100).toInt()}%)")
            }

            // Female detection logic
            val femaleDetectionResult = detectFemalePresence(faces, labels)

            if (femaleDetectionResult.isDetected) {
                Log.d("ContentFilter", "Female detected: ${femaleDetectionResult.reason}")
                return@coroutineScope ContentAnalysisResult.Inappropriate(
                    reason = femaleDetectionResult.reason
                )
            }

            // Check for other inappropriate content
            val detectedInappropriateContent = labels
                .filter { label ->
                    inappropriateKeywords.any { keyword ->
                        label.text.contains(keyword, ignoreCase = true)
                    }
                }
                .map { label -> "${label.text} (${(label.confidence * 100).toInt()}%)" }

            if (detectedInappropriateContent.isNotEmpty()) {
                ContentAnalysisResult.Inappropriate(
                    reason = "Content contains: ${detectedInappropriateContent.joinToString(", ")}"
                )
            } else {
                ContentAnalysisResult.Appropriate
            }

        } catch (e: Exception) {
            Log.e("ContentFilter", "Analysis failed", e)
            ContentAnalysisResult.Error(e.message ?: "Analysis failed")
        }
    }

    private fun detectFemalePresence(
        faces: List<Face>,
        labels: List<com.google.mlkit.vision.label.ImageLabel>
    ): FemaleDetectionResult {
        val reasons = mutableListOf<String>()
        var confidenceScore = 0f

        // Check for explicit male indicators first
        val hasMaleLabels = labels.any { label ->
            maleIndicators.any { indicator ->
                label.text.contains(indicator, ignoreCase = true) && label.confidence > 0.5f
            }
        }

        // Check for female indicators
        val hasFemaleLabels = labels.any { label ->
            femaleIndicators.any { indicator ->
                label.text.contains(indicator, ignoreCase = true) && label.confidence > 0.1f
            }
        }

        // If male indicators are found and no female indicators, don't filter
        if (hasMaleLabels && !hasFemaleLabels) {
            Log.d("ContentFilter", "Male detected, not filtering")
            return FemaleDetectionResult(
                isDetected = false,
                confidence = 0f,
                reason = "Male detected"
            )
        }

        // If female indicators are found, filter
        if (hasFemaleLabels) {
            confidenceScore = 0.9f
            val femaleLabels = labels.filter { label ->
                femaleIndicators.any { indicator ->
                    label.text.contains(indicator, ignoreCase = true)
                }
            }.map { "${it.text} (${(it.confidence * 100).toInt()}%)" }
            reasons.add("Female indicators: ${femaleLabels.joinToString(", ")}")
        }

        // If faces detected but no clear gender indicators
        else if (faces.isNotEmpty() && !hasMaleLabels) {
            // Only filter if we're uncertain (no male indicators found)
            Log.d("ContentFilter", "Face detected with no clear gender indicators")

            // Check for person/people labels
            val hasPersonLabel = labels.any {
                it.text.equals("person", ignoreCase = true) ||
                        it.text.equals("people", ignoreCase = true)
            }

            // Look for context clues that might indicate gender
            val hasNeutralClothing = labels.any { label ->
                listOf("shirt", "jacket", "clothing", "outfit").any {
                    label.text.contains(it, ignoreCase = true)
                }
            }

            // If we have faces but no gender indicators and no male-specific items, be cautious
            if (hasPersonLabel && !hasNeutralClothing) {
                confidenceScore = 0.7f
                reasons.add("${faces.size} face(s) detected without male indicators")
            } else {
                // Don't filter if we're unsure
                confidenceScore = 0.3f // Below threshold
            }
        }

        // Decision threshold
        val isDetected = confidenceScore >= 0.6f

        return FemaleDetectionResult(
            isDetected = isDetected,
            confidence = confidenceScore,
            reason = if (isDetected) {
                "Female presence detected: ${reasons.joinToString("; ")}"
            } else {
                "No female presence detected"
            }
        )
    }

    // Clean up resources
    fun close() {
        try {
            faceDetector.close()
        } catch (e: Exception) {
            Log.e("ContentFilter", "Error closing face detector", e)
        }
    }
}

data class FemaleDetectionResult(
    val isDetected: Boolean,
    val confidence: Float,
    val reason: String
)