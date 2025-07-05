package com.ae.haram_blur.models

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp

private const val TAG = "EnhancedGenderModel"

internal class EnhancedGenderDetectionModel(context: Context) {
    private val interpreter: Interpreter
    private val imageProcessor: ImageProcessor

    init {
        try {
            Log.d(TAG, "Loading enhanced gender model")
            val modelBuffer = FileUtil.loadMappedFile(context, "gender_model.tflite")

            // Use GPU acceleration if available
            val options = Interpreter.Options().apply {
                try {
                    addDelegate(GpuDelegate())
                    Log.d(TAG, "GPU acceleration enabled")
                } catch (e: Exception) {
                    Log.d(TAG, "GPU acceleration not available, using CPU")
                }
            }

            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "Gender model loaded successfully")

            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(96, 96, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize gender model", e)
            throw e
        }
    }

    fun detectGender(faceBitmap: Bitmap): GenderResult {
        try {
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(faceBitmap))

            val output = Array(1) { FloatArray(2) }
            interpreter.run(tensorImage.buffer, output)

            val maleProbability = output[0][0]
            val femaleProbability = output[0][1]

            Log.d(TAG, "Gender probabilities - Male: $maleProbability, Female: $femaleProbability")

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
        interpreter.close()
    }
}

internal data class GenderResult(
    val isFemale: Boolean,
    val confidence: Float,
    val femaleProbability: Float,
    val maleProbability: Float
)