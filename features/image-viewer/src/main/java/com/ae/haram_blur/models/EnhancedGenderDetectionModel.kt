package com.ae.haram_blur.models

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp


private const val TAG = "GenderDetectionModel"

internal class EnhancedGenderDetectionModel(context: Context) {
    private val interpreter: Interpreter
    private val imageProcessor: ImageProcessor

    init {
        try {
            Log.d(TAG, "Loading gender model")
            val modelBuffer = FileUtil.loadMappedFile(context, "model_gender_nonq.tflite")

            // Simple CPU-only options
            val options = Interpreter.Options().apply {
                numThreads = 4
                useNNAPI = false // Explicitly disable NNAPI
            }

            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "Gender model loaded successfully (CPU mode)")

            // Assuming the model expects 128x128 images based on previous implementation
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(128, 128, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize gender model", e)
            throw e
        }
    }

    fun detectGender(faceBitmap: Bitmap): GenderResult {
        try {
            val startTime = System.currentTimeMillis()
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(faceBitmap))

            val output = Array(1) { FloatArray(2) }
            interpreter.run(tensorImage.buffer, output)

            val inferenceTime = System.currentTimeMillis() - startTime

            val maleProbability = output[0][0]
            val femaleProbability = output[0][1]

            Log.d(TAG, "Gender detection completed in ${inferenceTime}ms - Male: %.3f, Female: %.3f"
                .format(maleProbability, femaleProbability))

            return GenderResult(
                isFemale = femaleProbability > maleProbability,
                confidence = maxOf(maleProbability, femaleProbability),
                femaleProbability = femaleProbability,
                maleProbability = maleProbability
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting gender", e)
            throw e
        }
    }

    fun close() {
        try {
            interpreter.close()
            Log.d(TAG, "Gender model closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing model", e)
        }
    }
}

internal data class GenderResult(
    val isFemale: Boolean,
    val confidence: Float,
    val femaleProbability: Float,
    val maleProbability: Float
)