package com.ae.islamicimageviewer


import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp


internal class GenderDetectionModel(context: Context) {
    private val interpreter: Interpreter
    private val imageProcessor: ImageProcessor

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, "model_gender_nonq.tflite")
        interpreter = Interpreter(modelBuffer)

        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(128, 128, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()
    }

    fun detectGender(faceBitmap: Bitmap): GenderResult {
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(faceBitmap))

        val output = Array(1) { FloatArray(2) }
        interpreter.run(tensorImage.buffer, output)

        val maleProbability = output[0][0]
        val femaleProbability = output[0][1]

        return GenderResult(
            isFemale = femaleProbability > maleProbability,
            confidence = maxOf(maleProbability, femaleProbability)
        )
    }

    fun close() {
        interpreter.close()
    }
}

internal data class GenderResult(
    val isFemale: Boolean,
    val confidence: Float
)