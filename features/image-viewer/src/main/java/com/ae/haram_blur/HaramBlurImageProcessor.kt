package com.ae.haram_blur

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.ae.islamicimageviewer.internal.FaceDetector
import com.ae.haram_blur.models.NsfwDetectionModel
import com.ae.haram_blur.models.EnhancedGenderDetectionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

private const val TAG = "HaramBlurProcessor"

class HaramBlurImageProcessor(
    private val context: Context,
    private val settings: BlurSettings = BlurSettings()
) {
    private val faceDetector = FaceDetector()
    private val genderModel by lazy { EnhancedGenderDetectionModel(context) }
    private val nsfwModel by lazy { NsfwDetectionModel(context) }

    data class BlurSettings(
        val blurFemales: Boolean = true,
        val blurMales: Boolean = false,
        val useNsfwDetection: Boolean = true,
        val nsfwThreshold: Float = 0.3f,
        val genderConfidenceThreshold: Float = 0.5f,
        val strictMode: Boolean = false // In strict mode, blur if unsure
    )

    data class ProcessingResult(
        val shouldBlur: Boolean,
        val reason: String? = null,
        val detectionDetails: DetectionDetails? = null
    )

    data class DetectionDetails(
        val facesDetected: Int = 0,
        val femalesDetected: Int = 0,
        val malesDetected: Int = 0,
        val nsfwScore: Float = 0f,
        val isNsfw: Boolean = false
    )

    suspend fun processImage(bitmap: Bitmap): ProcessingResult = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Processing image with HaramBlur algorithm")

            // Run NSFW detection and face detection in parallel
            val nsfwDeferred = async {
                if (settings.useNsfwDetection) {
                    nsfwModel.detectNsfw(bitmap)
                } else null
            }

            val facesDeferred = async {
                faceDetector.detectFaces(bitmap)
            }

            val nsfwResult = nsfwDeferred.await()
            val faces = facesDeferred.await()

            Log.d(TAG, "Detected ${faces.size} faces")

            // Check NSFW first
            if (nsfwResult != null && nsfwResult.isInappropriate) {
                Log.d(TAG, "NSFW content detected with score: ${nsfwResult.inappropriateScore}")
                return@withContext ProcessingResult(
                    shouldBlur = true,
                    reason = "Inappropriate content detected",
                    detectionDetails = DetectionDetails(
                        nsfwScore = nsfwResult.inappropriateScore,
                        isNsfw = true
                    )
                )
            }

            // Process faces for gender detection
            var femaleCount = 0
            var maleCount = 0
            var uncertainCount = 0

            for (face in faces) {
                try {
                    val faceBitmap = cropFace(bitmap, face)
                    val genderResult = genderModel.detectGender(faceBitmap)

                    when {
                        genderResult.confidence < settings.genderConfidenceThreshold -> {
                            uncertainCount++
                            if (settings.strictMode) {
                                // In strict mode, treat uncertain as female
                                femaleCount++
                            }
                        }
                        genderResult.isFemale -> femaleCount++
                        else -> maleCount++
                    }

                    Log.d(TAG, "Face gender: ${if (genderResult.isFemale) "Female" else "Male"}, " +
                            "Confidence: ${genderResult.confidence}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing face", e)
                    if (settings.strictMode) {
                        uncertainCount++
                        femaleCount++ // In strict mode, err on the side of caution
                    }
                }
            }

            // Determine if we should blur
            val shouldBlur = when {
                settings.blurFemales && femaleCount > 0 -> true
                settings.blurMales && maleCount > 0 -> true
                settings.strictMode && uncertainCount > 0 -> true
                else -> false
            }

            val reason = when {
                shouldBlur && femaleCount > 0 -> "Detected $femaleCount female face(s)"
                shouldBlur && maleCount > 0 -> "Detected $maleCount male face(s)"
                shouldBlur && settings.strictMode -> "Uncertain detection in strict mode"
                else -> null
            }

            ProcessingResult(
                shouldBlur = shouldBlur,
                reason = reason,
                detectionDetails = DetectionDetails(
                    facesDetected = faces.size,
                    femalesDetected = femaleCount,
                    malesDetected = maleCount,
                    nsfwScore = nsfwResult?.inappropriateScore ?: 0f,
                    isNsfw = nsfwResult?.isInappropriate == true
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in image processing", e)
            // In case of error, apply blur if in strict mode
            ProcessingResult(
                shouldBlur = settings.strictMode,
                reason = if (settings.strictMode) "Processing error in strict mode" else null
            )
        }
    }

    private fun cropFace(bitmap: Bitmap, face: FaceDetector.DetectedFace): Bitmap {
        val rect = face.boundingBox

        // Add padding to include more context around the face
        val padding = (rect.width() * 0.1).toInt()

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
            Log.d(TAG, "Cleaning up HaramBlur processor")
            faceDetector.close()
            genderModel.close()
            nsfwModel.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}