package com.ae.image_viewer_3.internal



import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.graphics.createBitmap

class ImageProcessor {
    /**
     * Applies a blur effect to the provided Bitmap.
     * @param originalBitmap The original Bitmap to blur.
     * @return A Painter with the blurred Bitmap.
     */
    fun applyBlur(originalBitmap: Bitmap): Painter {
        return try {
            // Create a new Bitmap for the blurred result
            val blurredBitmap = createBitmap(originalBitmap.width, originalBitmap.height)
            val canvas = Canvas(blurredBitmap)
            val paint = Paint().apply {
                maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
            }
            // Draw the original bitmap onto the new bitmap with blur effect
            canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
            BitmapPainter(blurredBitmap.asImageBitmap())
        } catch (e: Exception) {
            e.printStackTrace()
            BitmapPainter(originalBitmap.asImageBitmap())
        }
    }
}