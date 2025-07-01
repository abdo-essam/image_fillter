package com.ae.islamicimageviewer


import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.mlkit.vision.label.ImageLabeling


/**
 * Service responsible for detecting inappropriate content in images
 * based on Islamic cultural guidelines.
 */
internal class ContentDetectionService {

    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f)
            .build()
    )

    // Keywords that might indicate inappropriate content for Islamic culture
    private val inappropriateKeywords = setOf(
        "person", "people", "human", "man", "woman", "face", "body",
        "alcohol", "wine", "beer", "bottle", "drink",
        "underwear", "swimwear", "lingerie",
        "party", "nightclub", "bar",
        "gambling", "casino", "cards",
        "pig", "pork", "ham"
    )

    /**
     * Analyzes an image bitmap to determine if it contains appropriate content.
     *
     * @param bitmap The bitmap to analyze
     * @return true if content is appropriate, false if it should be filtered
     */
    suspend fun isContentAppropriate(bitmap: Bitmap): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)

            imageLabeler.process(image)
                .addOnSuccessListener { labels ->
                    val isAppropriate = labels.none { label ->
                        inappropriateKeywords.any { keyword ->
                            label.text.lowercase().contains(keyword.lowercase())
                        }
                    }
                    continuation.resume(isAppropriate)
                }
                .addOnFailureListener {
                    // If detection fails, assume content is appropriate
                    continuation.resume(true)
                }
        }
    }
}