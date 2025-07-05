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
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

private const val TAG = "NsfwDetectionModel"

internal class NsfwDetectionModel(context: Context) {
    private val interpreter: Interpreter
    private val imageProcessor: ImageProcessor
    private val inputImageWidth: Int
    private val inputImageHeight: Int

    // NSFW categories
    enum class Category(val index: Int, val label: String) {
        DRAWING(0, "drawings"),
        HENTAI(1, "hentai"),
        NEUTRAL(2, "neutral"),
        PORN(3, "porn"),
        SEXY(4, "sexy")
    }

    init {
        try {
            Log.d(TAG, "Loading NSFW model from assets")
            val modelBuffer = FileUtil.loadMappedFile(context, "nsfw_model.tflite")

            // Initialize interpreter with more memory
            val options = Interpreter.Options().apply {
                numThreads = 4
                // Allocate more memory for the interpreter
                useNNAPI = false // Disable NNAPI to avoid compatibility issues
            }

            interpreter = Interpreter(modelBuffer, options)

            // Get input tensor information
            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()

            Log.d(TAG, "Input tensor shape: ${inputShape.contentToString()}")
            Log.d(TAG, "Input tensor data type: ${inputTensor.dataType()}")

            // Verify output tensor
            val outputTensor = interpreter.getOutputTensor(0)
            Log.d(TAG, "Output tensor shape: ${outputTensor.shape().contentToString()}")
            Log.d(TAG, "Output tensor data type: ${outputTensor.dataType()}")

            inputImageHeight = inputShape[1]
            inputImageWidth = inputShape[2]

            Log.d(TAG, "NSFW model loaded - Input size: ${inputImageWidth}x${inputImageHeight}")

            // Image processor for preprocessing
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f)) // Normalize to [0, 1]
                .build()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NSFW model", e)
            throw e
        }
    }

    fun detectNsfw(bitmap: Bitmap): NsfwResult {
        try {
            Log.d(TAG, "Processing image for NSFW detection")

            // Process the image
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

            // Create output buffer - ensure it matches the model's output shape
            val outputShape = interpreter.getOutputTensor(0).shape()
            val outputDataType = interpreter.getOutputTensor(0).dataType()

            // The model outputs [1, 5] - one batch, 5 categories
            val outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType)

            // Run inference
            interpreter.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            // Get probabilities
            val probabilities = outputBuffer.floatArray
            val results = mutableMapOf<Category, Float>()

            // Map probabilities to categories
            Category.entries.forEach { category ->
                if (category.index < probabilities.size) {
                    results[category] = probabilities[category.index]
                }
            }

            // Calculate inappropriate score
            val inappropriateScore = (results[Category.PORN] ?: 0f) +
                    (results[Category.SEXY] ?: 0f) +
                    (results[Category.HENTAI] ?: 0f)

            Log.d(TAG, "NSFW detection results: $results")

            return NsfwResult(
                isInappropriate = inappropriateScore > 0.3f,
                inappropriateScore = inappropriateScore,
                categoryScores = results
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error detecting NSFW content", e)
            // Return safe default on error
            return NsfwResult(
                isInappropriate = false,
                inappropriateScore = 0f,
                categoryScores = emptyMap()
            )
        }
    }

    fun close() {
        try {
            interpreter.close()
            Log.d(TAG, "NSFW model closed")
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