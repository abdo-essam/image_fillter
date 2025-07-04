package com.ae.image_viewer_3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ae.image_viewer_3.internal.ContentDetector
import com.ae.image_viewer_3.internal.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CustomImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
): Boolean {
    val context = LocalContext.current
    val contentDetector = remember { ContentDetector() }
    val imageProcessor = remember { ImageProcessor() }
    val TAG = "CustomImage"

    // State to hold the Painter (normal or blurred)
    var imagePainter by remember { mutableStateOf<Painter?>(null) }
    // State to track if the image is inappropriate
    var isInappropriate by remember { mutableStateOf(false) }

    // Load and process the image asynchronously
    LaunchedEffect(imageUrl) {
        Log.d(TAG, "Starting processing for $imageUrl")
        val bitmap = loadBitmap(context, imageUrl)
        if (bitmap != null) {
            // Check if content is inappropriate
            isInappropriate = contentDetector.isInappropriateContent(imageUrl, context)
            Log.d(TAG, "Detection result for $imageUrl: isInappropriate = $isInappropriate")
            // Apply blur if inappropriate, otherwise use the original bitmap
            imagePainter = if (isInappropriate) {
                Log.d(TAG, "Applying blur for $imageUrl")
                imageProcessor.applyBlur(bitmap)
            } else {
                Log.d(TAG, "No blur needed for $imageUrl")
                androidx.compose.ui.graphics.painter.BitmapPainter(bitmap.asImageBitmap())
            }
        } else {
            Log.e(TAG, "Bitmap loading failed for $imageUrl")
        }
    }

    // Display the image if Painter is available
    imagePainter?.let { painter ->
        Log.d(TAG, "Rendering image for $imageUrl, blurred: $isInappropriate")
        Image(
            painter = painter,
            contentDescription = "Custom Image",
            modifier = modifier,
            contentScale = contentScale
        )
    }

    return isInappropriate
}

/**
 * Helper function to load Bitmap from URL using Coil.
 */
private suspend fun loadBitmap(context: Context, imageUrl: String): Bitmap? {
    val TAG = "CustomImage"
    return withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader.Builder(context)
                .build()
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false) // Disable hardware acceleration to avoid compatibility issues
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                Log.d(TAG, "Image loaded successfully for $imageUrl")
                result.drawable.toSafeBitmap()
            } else {
                Log.e(TAG, "Coil result not successful for $imageUrl")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Exception loading bitmap for $imageUrl: ${e.message}")
            null
        }
    }
}

/**
 * Safely convert Drawable to Bitmap, handling hardware acceleration issues.
 */
private fun Drawable.toSafeBitmap(): Bitmap? {
    val TAG = "CustomImage"
    try {
        // If it's already a BitmapDrawable, extract the Bitmap directly if possible
        if (this is BitmapDrawable) {
            Log.d(TAG, "Drawable is BitmapDrawable, extracting bitmap")
            return this.bitmap
        }
        // Otherwise, create a software bitmap and draw onto it
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