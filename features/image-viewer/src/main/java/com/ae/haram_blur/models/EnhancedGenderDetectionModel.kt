package com.ae.haram_blur.models

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.text.DecimalFormat
import androidx.core.graphics.createBitmap

private const val TAG = "GenderDetectionModel"

internal class EnhancedGenderDetectionModel(context: Context) {
    private val interpreter: Interpreter
    private val imageProcessor: ImageProcessor
    private val decimalFormat = DecimalFormat("#.####") // Format to 4 decimal places

    companion object {
        private const val MODEL_FILE = "GenderClass_06_03-20-08.tflite"
        private const val INPUT_SIZE = 224
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f

        // IMPORTANT: Based on your logs, it seems:
        // Index 0 = Female probability
        // Index 1 = Male probability
        private const val FEMALE_INDEX = 0
        private const val MALE_INDEX = 1

        // Confidence threshold for logging warnings
        private const val LOW_CONFIDENCE_THRESHOLD = 0.5f
    }

    init {
        try {
            Log.d(TAG, "Loading model from assets: $MODEL_FILE")
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)

            Log.d(TAG, "Model loaded, creating interpreter")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)

            Log.d(TAG, "Interpreter created successfully")

            val inputTensor = interpreter.getInputTensor(0)
            val outputTensor = interpreter.getOutputTensor(0)
            Log.d(TAG, "Input shape: ${inputTensor.shape().contentToString()}")
            Log.d(TAG, "Output shape: ${outputTensor.shape().contentToString()}")

            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model", e)
            throw e
        }
    }

    fun detectGender(faceBitmap: Bitmap): GenderResult {
        try {
            Log.d(TAG, "Processing face bitmap: ${faceBitmap.width}x${faceBitmap.height}")

            // Ensure bitmap is in correct format
            val rgbBitmap = ensureRgbBitmap(faceBitmap)

            // Process the image
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(rgbBitmap))

            // Prepare output array
            val output = Array(1) { FloatArray(2) }

            // Run inference
            interpreter.run(tensorImage.buffer, output)

            // Get probabilities with CORRECTED indices
            val femaleProbability = output[0][FEMALE_INDEX]
            val maleProbability = output[0][MALE_INDEX]

            // Format the values for better readability
            val formattedFemale = formatProbability(femaleProbability)
            val formattedMale = formatProbability(maleProbability)

            Log.d(TAG, "Raw output: [Female: $formattedFemale, Male: $formattedMale]")

            // Check if confidence is low
            val maxConfidence = maxOf(femaleProbability, maleProbability)
            if (maxConfidence < LOW_CONFIDENCE_THRESHOLD) {
                Log.w(TAG, "Low confidence prediction: ${(maxConfidence * 100).toInt()}%")
            }

            // Apply softmax to ensure probabilities sum to 1
            val expFemale = kotlin.math.exp(femaleProbability.toDouble())
            val expMale = kotlin.math.exp(maleProbability.toDouble())
            val sumExp = expFemale + expMale

            val normalizedFemaleProbability = (expFemale / sumExp).toFloat()
            val normalizedMaleProbability = (expMale / sumExp).toFloat()

            val isFemale = normalizedFemaleProbability > normalizedMaleProbability
            val confidence = if (isFemale) normalizedFemaleProbability else normalizedMaleProbability

            Log.d(TAG, "Prediction: ${if (isFemale) "Female" else "Male"} with ${(confidence * 100).toInt()}% confidence")

            return GenderResult(
                isFemale = isFemale,
                confidence = confidence
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting gender", e)
            throw e
        }
    }

    private fun ensureRgbBitmap(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.ARGB_8888) {
            val rgbBitmap = createBitmap(bitmap.width, bitmap.height)
            val canvas = android.graphics.Canvas(rgbBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            rgbBitmap
        } else {
            bitmap
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatProbability(probability: Float): String {
        return if (probability < 0.0001f) {
            String.format("%.2e", probability) // Scientific notation for very small numbers
        } else {
            decimalFormat.format(probability)
        }
    }

    fun close() {
        try {
            interpreter.close()
            Log.d(TAG, "Interpreter closed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreter", e)
        }
    }
}

internal data class GenderResult(
    val isFemale: Boolean,
    val confidence: Float
)
