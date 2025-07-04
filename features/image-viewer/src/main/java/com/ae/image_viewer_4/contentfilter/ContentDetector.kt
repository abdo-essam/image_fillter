package com.ae.image_viewer_4.contentfilter

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

internal class ContentDetector {
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.1f) // Detect smaller faces
            .build()
    )

    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.3f) // Lower threshold to catch more labels
            .build()
    )

    suspend fun analyzeImage(bitmap: Bitmap): ContentAnalysis {
        val image = InputImage.fromBitmap(bitmap, 0)

        val faces = try {
            faceDetector.process(image).await()
        } catch (e: Exception) {
            emptyList()
        }

        val labels = try {
            imageLabeler.process(image).await()
        } catch (e: Exception) {
            emptyList()
        }

        // Add manual analysis for common patterns
        val enhancedLabels = labels.map {
            ImageLabel(it.text, it.confidence)
        }.toMutableList()

        // If we detect a face but no gender labels, add a generic person label
        if (faces.isNotEmpty() && !labels.any {
                it.text.lowercase().let { text ->
                    text.contains("man") || text.contains("woman") ||
                            text.contains("person") || text.contains("male") ||
                            text.contains("female")
                }
            }) {
            enhancedLabels.add(ImageLabel("Person", 0.8f))
        }

        return ContentAnalysis(
            faces = faces,
            labels = enhancedLabels,
            bitmap = bitmap
        )
    }

    data class ContentAnalysis(
        val faces: List<Face>,
        val labels: List<ImageLabel>,
        val bitmap: Bitmap
    )

    data class ImageLabel(
        val text: String,
        val confidence: Float
    )
}