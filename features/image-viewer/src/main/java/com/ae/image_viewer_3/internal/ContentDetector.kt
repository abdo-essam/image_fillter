package com.ae.image_viewer_3.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer

class ContentDetector {
    private var interpreter: Interpreter? = null
    private val inputSize = 128 // Gender model input size is 128x128 as per README
    private var isInitialized = false
    private val TAG = "ContentDetector"

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            Log.d(TAG, "Attempting to load model from assets...")
            val modelFile = FileUtil.loadMappedFile(context, "GenderClass_06_03-20-08.tflite")
            interpreter = Interpreter(modelFile)
            isInitialized = true
            Log.d(TAG, "Model initialized successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            isInitialized = false
            Log.e(TAG, "Model initialization failed: ${e.message}")
        }
    }

    suspend fun isInappropriateContent(imageUrl: String, context: Context): Boolean {
        // Reinitialize if somehow not initialized
        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "Model not initialized, attempting reinitialization for $imageUrl")
            initialize(context)
            if (!isInitialized || interpreter == null) {
                Log.e(TAG, "Reinitialization failed or model still not initialized, returning false for $imageUrl")
                return false
            }
        }

        Log.d(TAG, "Loading image from URL: $imageUrl")
        val bitmap = withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val loader = ImageLoader.Builder(context).build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    Log.d(TAG, "Image loaded successfully for $imageUrl")
                    result.drawable.toSafeBitmap()
                } else {
                    Log.e(TAG, "Image loading failed for $imageUrl")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Exception during image loading for $imageUrl: ${e.message}")
                null
            }
        } ?: return false

        Log.d(TAG, "Preprocessing image for model inference...")
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val resizeOp = ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR)
        val processedImage = resizeOp.apply(tensorImage)

        Log.d(TAG, "Running model inference for $imageUrl")
        val inputBuffer = processedImage.buffer
        val outputBuffer = ByteBuffer.allocateDirect(4 * 2)
        try {
            interpreter?.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()
            val scores = FloatArray(2)
            outputBuffer.asFloatBuffer().get(scores)
            val score0 = scores[0]
            val score1 = scores[1]
            Log.d(TAG, "Model output for $imageUrl - Score[0]: $score0, Score[1]: $score1")
            val femaleScore = score1 // Assuming index 1 is female
            val threshold = 0.3f // Lowered for sensitivity
            val isFemale = femaleScore > threshold
            Log.d(TAG, "Threshold: $threshold, Detected as female (Score[1]): $isFemale for $imageUrl")
            return isFemale
        } catch (e: Exception) {
            Log.e(TAG, "Inference error for $imageUrl: ${e.message}")
            return false
        }
    }

    fun close() {
        interpreter?.close()
        isInitialized = false
        Log.d(TAG, "Model resources closed.")
    }
}

// Safely convert Drawable to Bitmap (same as previous)
fun Drawable.toSafeBitmap(): Bitmap? {
    val TAG = "ContentDetector"
    try {
        if (this is android.graphics.drawable.BitmapDrawable) {
            Log.d(TAG, "Drawable is BitmapDrawable, extracting bitmap")
            return this.bitmap
        }
        Log.d(TAG, "Creating software bitmap for drawable")
        val bitmap = Bitmap.createBitmap(
            intrinsicWidth.coerceAtLeast(1),
            intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        Log.d(TAG, "Successfully converted drawable to bitmap")
        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e(TAG, "Failed to convert drawable to bitmap: ${e.message}")
        return null
    }
}