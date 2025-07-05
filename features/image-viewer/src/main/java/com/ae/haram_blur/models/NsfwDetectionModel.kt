package com.ae.haram_blur.models

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

private const val TAG = "NsfwDetectionModel"

internal class NsfwDetectionModel(context: Context) {
    private val interpreter: Interpreter
    private val imageProcessor: ImageProcessor

    // NSFW categories
    enum class Category(val index: Int) {
        DRAWING(0),
        HENTAI(1),
        NEUTRAL(2),
        PORN(3),
        SEXY(4)
    }

    init {
        try {
            Log.d(TAG, "Loading NSFW model from assets")
            val modelBuffer = FileUtil.loadMappedFile(context, "nsfw_model.tflite")
            interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "NSFW model loaded successfully")

            // NSFW.js model expects 224x224 images
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NSFW model", e)
            throw e
        }
    }

    fun detectNsfw(bitmap: Bitmap): NsfwResult {
        try {
            Log.d(TAG, "Processing image for NSFW detection")
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

            // Run inference
            val output = Array(1) { FloatArray(5) } // 5 categories
            interpreter.run(tensorImage.buffer, output)

            // Get probabilities for each category
            val probabilities = output[0]
            val results = mutableMapOf<Category, Float>()

            Category.entries.forEach { category ->
                results[category] = probabilities[category.index]
            }

            // Calculate if content is inappropriate
            val inappropriateScore = results[Category.PORN]!! +
                    results[Category.SEXY]!! +
                    results[Category.HENTAI]!!

            Log.d(TAG, "NSFW detection results: $results")

            return NsfwResult(
                isInappropriate = inappropriateScore > 0.3f,
                inappropriateScore = inappropriateScore,
                categoryScores = results
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting NSFW content", e)
            throw e
        }
    }

    fun close() {
        try {
            interpreter.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreter", e)
        }
    }
}

internal data class NsfwResult(
    val isInappropriate: Boolean,
    val inappropriateScore: Float,
    val categoryScores: Map<NsfwDetectionModel.Category, Float>
)