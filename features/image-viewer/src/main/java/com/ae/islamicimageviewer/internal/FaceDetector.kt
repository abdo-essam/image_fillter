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

private const val TAG = "FaceDetector"

internal class FaceDetector(private val context: Context) {

    private val blazeFaceDetector = BlazeFaceDetector(context)

    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace> {
        return try {
            val blazeFaceResults = blazeFaceDetector.detectFaces(bitmap)

            // Convert BlazeFace results to FaceDetector.DetectedFace
            blazeFaceResults.map { blazeFace ->
                DetectedFace(
                    boundingBox = blazeFace.boundingBox
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting faces", e)
            emptyList()
        }
    }

    fun close() {
        blazeFaceDetector.close()
    }

    data class DetectedFace(val boundingBox: Rect)
}
