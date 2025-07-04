package com.ae.islamicimageviewer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

private const val TAG = "GenderDetectionModel"

internal class GenderDetectionModel(context: Context) {
    private val interpreter: Interpreter
    private val imageProcessor: ImageProcessor

    init {
        try {
            Log.d(TAG, "Loading model from assets")
            val modelBuffer = FileUtil.loadMappedFile(context, "model_gender_nonq.tflite")
            Log.d(TAG, "Model loaded, creating interpreter")
            interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "Interpreter created successfully")

            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(128, 128, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model", e)
            throw e
        }
    }

    fun detectGender(faceBitmap: Bitmap): GenderResult {
        try {
            Log.d(TAG, "Processing face bitmap: ${faceBitmap.width}x${faceBitmap.height}")
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(faceBitmap))

            val output = Array(1) { FloatArray(2) }
            interpreter.run(tensorImage.buffer, output)

            val maleProbability = output[0][0]
            val femaleProbability = output[0][1]

            Log.d(TAG, "Gender probabilities - Male: $maleProbability, Female: $femaleProbability")

            return GenderResult(
                isFemale = femaleProbability > maleProbability,
                confidence = maxOf(maleProbability, femaleProbability)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting gender", e)
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

internal data class GenderResult(
    val isFemale: Boolean,
    val confidence: Float
)