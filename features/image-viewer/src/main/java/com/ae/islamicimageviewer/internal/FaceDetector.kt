package com.ae.islamicimageviewer.internal


import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


internal class FaceDetector {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .build()

    private val detector = FaceDetection.getClient(options)

    suspend fun detectFaces(bitmap: Bitmap): List<Face> = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                cont.resume(faces.map { face ->
                    Face(face.boundingBox)
                })
            }
            .addOnFailureListener { exception ->
                cont.resumeWithException(exception)
            }
    }

    fun close() {
        detector.close()
    }

    data class Face(val boundingBox: Rect)
}