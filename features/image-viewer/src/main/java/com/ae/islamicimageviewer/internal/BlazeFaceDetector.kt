package com.ae.islamicimageviewer.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlin.math.max
import kotlin.math.min

internal class BlazeFaceDetector(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val imageProcessor: ImageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(127.5f, 127.5f)) // Normalize to [-1, 1]
        .build()

    companion object {
        private const val TAG = "BlazeFaceDetector"

        // Model file
        private const val MODEL_FILE = "blaze_face_short_range.tflite"

        // BlazeFace model parameters
        private const val INPUT_SIZE = 128
        private const val NUM_BOXES = 896
        private const val NUM_COORDS = 16

        // Detection parameters
        private const val SCORE_THRESHOLD = 0.5f
        private const val IOU_THRESHOLD = 0.3f
    }

    init {

        loadModel()
    }

    private fun loadModel() {
        try {
            Log.d(TAG, "Loading model: $MODEL_FILE")
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(false) // Disable NNAPI for better compatibility
            }
            interpreter = Interpreter(modelBuffer, options)

            // Log model details
            interpreter?.let { interp ->
                Log.d(TAG, "Model loaded successfully")
                Log.d(TAG, "Input tensors: ${interp.inputTensorCount}")
                Log.d(TAG, "Output tensors: ${interp.outputTensorCount}")

                // Log input details
                for (i in 0 until interp.inputTensorCount) {
                    val tensor = interp.getInputTensor(i)
                    Log.d(TAG, "Input $i: shape=${tensor.shape().contentToString()}, dtype=${tensor.dataType()}")
                }

                // Log output details
                for (i in 0 until interp.outputTensorCount) {
                    val tensor = interp.getOutputTensor(i)
                    Log.d(TAG, "Output $i: shape=${tensor.shape().contentToString()}, dtype=${tensor.dataType()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            throw RuntimeException("Failed to load BlazeFace model", e)
        }
    }

    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace> {
        val interpreter = this.interpreter ?: run {
            Log.e(TAG, "Interpreter not initialized")
            return emptyList()
        }

        return try {
            Log.d(TAG, "Processing image: ${bitmap.width}x${bitmap.height}")

            // Prepare input
            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)
            val processedImage = imageProcessor.process(tensorImage)
            val inputBuffer = processedImage.buffer

            // Prepare outputs with correct shapes
            val outputBoxes = Array(1) { Array(NUM_BOXES) { FloatArray(NUM_COORDS) } }
            val outputScores = Array(1) { Array(NUM_BOXES) { FloatArray(1) } }

            // Run inference
            val startTime = System.currentTimeMillis()
            interpreter.runForMultipleInputsOutputs(
                arrayOf(inputBuffer),
                mapOf(0 to outputBoxes, 1 to outputScores)
            )
            val inferenceTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Inference completed in ${inferenceTime}ms")

            // Process detections
            val detections = processDetections(
                outputBoxes[0],
                outputScores[0],
                bitmap.width,
                bitmap.height
            )

            Log.d(TAG, "Found ${detections.size} face(s)")
            detections
        } catch (e: Exception) {
            Log.e(TAG, "Error during face detection", e)
            emptyList()
        }
    }

    private fun processDetections(
        boxes: Array<FloatArray>,
        scores: Array<FloatArray>,
        imageWidth: Int,
        imageHeight: Int
    ): List<DetectedFace> {
        val detections = mutableListOf<RawDetection>()
        var validDetections = 0

        // Collect valid detections
        for (i in scores.indices) {
            val score = scores[i][0]
            if (score >= SCORE_THRESHOLD) {
                validDetections++
                val box = boxes[i]
                // BlazeFace outputs: [xCenter, yCenter, width, height, ...keypoints...]
                val xCenter = box[0]
                val yCenter = box[1]
                val width = box[2]
                val height = box[3]

                detections.add(
                    RawDetection(
                        score = score,
                        xCenter = xCenter,
                        yCenter = yCenter,
                        width = width,
                        height = height
                    )
                )
            }
        }

        Log.d(TAG, "Valid detections before NMS: $validDetections")

        // Apply NMS
        val nmsResults = nonMaxSuppression(detections)
        Log.d(TAG, "Detections after NMS: ${nmsResults.size}")

        // Convert to screen coordinates
        return nmsResults.map { detection ->
            val halfWidth = detection.width * 0.5f
            val halfHeight = detection.height * 0.5f

            // Convert normalized coordinates to pixel coordinates
            val left = max(0, ((detection.xCenter - halfWidth) * imageWidth).toInt())
            val top = max(0, ((detection.yCenter - halfHeight) * imageHeight).toInt())
            val right = min(imageWidth, ((detection.xCenter + halfWidth) * imageWidth).toInt())
            val bottom = min(imageHeight, ((detection.yCenter + halfHeight) * imageHeight).toInt())

            // Add a small padding to include more face area
            val padding = ((right - left) * 0.1).toInt()
            val paddedLeft = max(0, left - padding)
            val paddedTop = max(0, top - padding)
            val paddedRight = min(imageWidth, right + padding)
            val paddedBottom = min(imageHeight, bottom + padding)

            DetectedFace(
                boundingBox = Rect(paddedLeft, paddedTop, paddedRight, paddedBottom),
                confidence = detection.score
            )
        }
    }

    private fun nonMaxSuppression(detections: List<RawDetection>): List<RawDetection> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.score }
        val selected = mutableListOf<RawDetection>()
        val active = BooleanArray(sorted.size) { true }

        for (i in sorted.indices) {
            if (!active[i]) continue

            selected.add(sorted[i])

            for (j in i + 1 until sorted.size) {
                if (!active[j]) continue

                val iou = calculateIoU(sorted[i], sorted[j])
                if (iou > IOU_THRESHOLD) {
                    active[j] = false
                }
            }
        }

        return selected
    }

    private fun calculateIoU(a: RawDetection, b: RawDetection): Float {
        val aLeft = a.xCenter - a.width * 0.5f
        val aRight = a.xCenter + a.width * 0.5f
        val aTop = a.yCenter - a.height * 0.5f
        val aBottom = a.yCenter + a.height * 0.5f

        val bLeft = b.xCenter - b.width * 0.5f
        val bRight = b.xCenter + b.width * 0.5f
        val bTop = b.yCenter - b.height * 0.5f
        val bBottom = b.yCenter + b.height * 0.5f

        val interLeft = max(aLeft, bLeft)
        val interTop = max(aTop, bTop)
        val interRight = min(aRight, bRight)
        val interBottom = min(aBottom, bBottom)

        val interArea = max(0f, interRight - interLeft) * max(0f, interBottom - interTop)

        if (interArea <= 0) return 0f

        val aArea = a.width * a.height
        val bArea = b.width * b.height
        val unionArea = aArea + bArea - interArea

        return if (unionArea > 0) interArea / unionArea else 0f
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            Log.d(TAG, "BlazeFace detector closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreter", e)
        }
    }

    data class DetectedFace(
        val boundingBox: Rect,
        val confidence: Float = 0f
    )

    private data class RawDetection(
        val score: Float,
        val xCenter: Float,
        val yCenter: Float,
        val width: Float,
        val height: Float
    )
}