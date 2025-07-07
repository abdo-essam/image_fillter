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
import android.util.Log
import androidx.core.graphics.createBitmap


internal class ContentAnalyzer(private val context: Context) {

    companion object {
        private const val TAG = "ContentAnalyzer"

        // Confidence thresholds
        private const val FACE_CONFIDENCE_THRESHOLD = 0.7f
        private const val LABEL_CONFIDENCE_THRESHOLD = 0.6f

        // Labels that might indicate inappropriate content
        private val INAPPROPRIATE_LABELS = setOf(
            // Direct indicators
            "woman", "girl", "lady", "female",
            // Clothing
            "dress", "skirt", "bikini", "swimsuit", "lingerie", "bra",
            // Body parts
            "skin", "chest", "leg", "thigh", "shoulder", "neck",
            // Activities/contexts
            "dance", "model", "fashion", "beauty", "makeup",
            // Hair (often a gender indicator)
            "hair", "long hair", "blonde", "brunette", "hairstyle"
        )

        // Labels that need additional context
        private val CONTEXT_LABELS = setOf(
            "person", "people", "human", "face", "portrait"
        )
    }

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.1f)
            .build()
    )

    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()
    )

    fun analyzeImage(drawable: Drawable?, onResult: (Boolean) -> Unit) {
        val bitmap = drawable?.toBitmap() ?: run {
            onResult(false)
            return
        }

        analyzeImageBitmap(bitmap, onResult)
    }

    private fun analyzeImageBitmap(bitmap: Bitmap, onResult: (Boolean) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        // Run both face detection and label detection in parallel
        val faceTask = faceDetector.process(image)
        val labelTask = imageLabeler.process(image)

        faceTask.continueWithTask { faceResult ->
            labelTask.continueWith { labelResult ->
                try {
                    val faces = faceResult.result
                    val labels = labelResult.result

                    // Log detection results
                    Log.d(TAG, "Detected ${faces?.size ?: 0} faces")
                    labels?.forEach { label ->
                        Log.d(TAG, "Label: ${label.text} (${(label.confidence * 100).toInt()}%)")
                    }

                    // Analyze results
                    val hasInappropriateContent = analyzeResults(faces, labels)
                    onResult(hasInappropriateContent)

                } catch (e: Exception) {
                    Log.e(TAG, "Error analyzing image", e)
                    onResult(false)
                }
            }
        }
    }

    private fun analyzeResults(faces: List<Face>?, labels: List<com.google.mlkit.vision.label.ImageLabel>?): Boolean {
        // Check 1: Direct inappropriate labels
        labels?.forEach { label ->
            if (label.confidence >= LABEL_CONFIDENCE_THRESHOLD) {
                val labelLower = label.text.lowercase()
                if (INAPPROPRIATE_LABELS.any { inappropriate ->
                        labelLower.contains(inappropriate)
                    }) {
                    Log.d(TAG, "Inappropriate label detected: ${label.text}")
                    return true
                }
            }
        }

        // Check 2: Face analysis with gender prediction
        if (!faces.isNullOrEmpty()) {
            val hasPotentiallyFemaleFace = faces.any { face ->
                val genderScore = analyzeFaceGender(face, labels)
                Log.d(TAG, "Face gender score: $genderScore")
                genderScore > 0.6f
            }

            if (hasPotentiallyFemaleFace) {
                Log.d(TAG, "Female face characteristics detected")
                return true
            }
        }

        // Check 3: Context analysis - if "person" is detected, analyze other context
        val hasPerson = labels?.any { label ->
            CONTEXT_LABELS.contains(label.text.lowercase()) && label.confidence > 0.7f
        } ?: false

        if (hasPerson) {
            // Check for combinations that might indicate female presence
            val contextScore = analyzeContext(labels)
            if (contextScore > 0.7f) {
                Log.d(TAG, "Context analysis indicates filtering needed")
                return true
            }
        }

        return false
    }

    private fun analyzeFaceGender(face: Face, labels: List<com.google.mlkit.vision.label.ImageLabel>?): Float {
        var score = 0f
        var factors = 0

        // Factor 1: Smile probability (studies show women smile more in photos)
        face.smilingProbability?.let { smileProb ->
            if (smileProb > 0.8f) {
                score += 0.3f
                factors++
            }
        }

        // Factor 2: Face shape analysis
        val faceShape = analyzeFaceShape(face)
        score += faceShape * 0.4f
        factors++

        // Factor 3: Landmark analysis
        val landmarkScore = analyzeLandmarks(face)
        score += landmarkScore * 0.3f
        factors++

        // Factor 4: Check if hair-related labels are present
        val hasHairLabels = labels?.any { label ->
            val text = label.text.lowercase()
            (text.contains("hair") || text.contains("hairstyle")) && label.confidence > 0.6f
        } ?: false

        if (hasHairLabels) {
            score += 0.2f
        }

        return if (factors > 0) score else 0f
    }

    private fun analyzeFaceShape(face: Face): Float {
        // Analyze face bounds for shape characteristics
        val bounds = face.boundingBox
        val aspectRatio = bounds.height().toFloat() / bounds.width().toFloat()

        // Female faces tend to be slightly more oval/round
        return when {
            aspectRatio in 1.2f..1.4f -> 0.7f
            aspectRatio in 1.15f..1.45f -> 0.5f
            else -> 0.3f
        }
    }

    private fun analyzeLandmarks(face: Face): Float {
        var score = 0f

        // Get key landmarks
        val leftEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
        val nose = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE)
        val mouth = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_BOTTOM)

        if (leftEye != null && rightEye != null && nose != null && mouth != null) {
            // Calculate facial proportions
            val eyeDistance = distance(leftEye.position, rightEye.position)
            val noseToMouth = distance(nose.position, mouth.position)

            // Female faces often have different proportions
            val ratio = noseToMouth / eyeDistance
            if (ratio in 0.6f..0.8f) {
                score += 0.5f
            }
        }

        // Check eye characteristics
        val leftEyeOpen = face.leftEyeOpenProbability ?: 0f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 0f

        // Both eyes fully open might indicate makeup/posed photo
        if (leftEyeOpen > 0.9f && rightEyeOpen > 0.9f) {
            score += 0.3f
        }

        return score.coerceIn(0f, 1f)
    }

    private fun distance(p1: android.graphics.PointF, p2: android.graphics.PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun analyzeContext(labels: List<com.google.mlkit.vision.label.ImageLabel>?): Float {
        var score = 0f

        labels?.forEach { label ->
            val text = label.text.lowercase()
            val confidence = label.confidence

            // Check for clothing items
            if (text in setOf("clothing", "dress", "fashion", "outfit")) {
                score += confidence * 0.3f
            }

            // Check for beauty/cosmetics context
            if (text in setOf("beauty", "cosmetics", "makeup", "lipstick")) {
                score += confidence * 0.4f
            }

            // Check for jewelry
            if (text in setOf("jewelry", "earring", "necklace")) {
                score += confidence * 0.2f
            }

            // Check for indoor/portrait settings
            if (text in setOf("portrait", "selfie", "indoor")) {
                score += confidence * 0.1f
            }
        }

        return score.coerceIn(0f, 1f)
    }

    fun cleanup() {
        try {
            faceDetector.close()
            imageLabeler.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

// Extension function to convert Drawable to Bitmap
private fun Drawable.toBitmap(): Bitmap? {
    return when (this) {
        is BitmapDrawable -> bitmap
        else -> {
            val bitmap =
                createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1))
            val canvas = android.graphics.Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bitmap
        }
    }
}