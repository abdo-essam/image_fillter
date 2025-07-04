package com.ae.islamicimageviewer

// Android & TensorFlow Lite imports
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

// Tag used for logging
private const val TAG = "GenderDetectionModel"

// Internal class to perform gender detection using a TFLite model
internal class GenderDetectionModel(context: Context) {

    // Interpreter that runs the TFLite model
    private val interpreter: Interpreter

    // Image processor to resize and normalize the input image
    private val imageProcessor: ImageProcessor

    // Initialization block - runs when the object is created
    init {
        try {
            Log.d(TAG, "Loading model from assets")
            // Load the TFLite model from the assets folder
            val modelBuffer = FileUtil.loadMappedFile(context, "model_gender_nonq.tflite")

            Log.d(TAG, "Model loaded, creating interpreter")
            // Initialize the TFLite interpreter with the model buffer
            interpreter = Interpreter(modelBuffer)

            Log.d(TAG, "Interpreter created successfully")

            // Create the image processor
            imageProcessor = ImageProcessor.Builder()
                // Resize image to 128x128 pixels as expected by the model
                .add(ResizeOp(128, 128, ResizeOp.ResizeMethod.BILINEAR))
                // Normalize pixel values from [0, 255] to [0.0, 1.0]
                .add(NormalizeOp(0f, 255f))
                .build()
        } catch (e: Exception) {
            // Log and throw any error that occurs during initialization
            Log.e(TAG, "Failed to initialize model", e)
            throw e
        }
    }

    /**
     * This method takes a Bitmap image of a face and returns the predicted gender.
     * @param faceBitmap A cropped bitmap containing a single face.
     * @return GenderResult indicating if the face is female and the confidence.
     */
    fun detectGender(faceBitmap: Bitmap): GenderResult {
        try {
            Log.d(TAG, "Processing face bitmap: ${faceBitmap.width}x${faceBitmap.height}")

            // Convert bitmap to TensorImage and apply preprocessing
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(faceBitmap))

            // Output array to hold the prediction probabilities [Male, Female]
            val output = Array(1) { FloatArray(2) }

            // Run the model using the processed image
            interpreter.run(tensorImage.buffer, output)

            // Extract probabilities
            val maleProbability = output[0][0]
            val femaleProbability = output[0][1]

            Log.d(TAG, "Gender probabilities - Male: $maleProbability, Female: $femaleProbability")

            // Determine the gender with the higher confidence
            return GenderResult(
                isFemale = femaleProbability > maleProbability,
                confidence = maxOf(maleProbability, femaleProbability)
            )
        } catch (e: Exception) {
            // Log and rethrow any error during prediction
            Log.e(TAG, "Error detecting gender", e)
            throw e
        }
    }

    /**
     * Close the TFLite interpreter to free resources.
     */
    fun close() {
        try {
            interpreter.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreter", e)
        }
    }
}

/**
 * Data class representing the result of the gender detection.
 * @property isFemale True if the model predicts female, false for male.
 * @property confidence The model's confidence in its prediction (between 0.0 and 1.0).
 */
internal data class GenderResult(
    val isFemale: Boolean,
    val confidence: Float
)
