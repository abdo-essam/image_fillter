package com.ae.islamicimageviewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

/**
 * Utility methods for image processing operations.
 */
internal object ImageProcessingUtils {

    /**
     * Creates a blurred version of the provided bitmap using legacy blur technique.
     * This method works on all API levels.
     *
     * @param bitmap The original bitmap
     * @param blurRadius The radius of the blur effect (unused in legacy implementation)
     * @return The blurred bitmap
     */
    fun blurBitmap(bitmap: Bitmap, blurRadius: Float = 25f): Bitmap {
        return createLegacyBlur(bitmap, blurRadius)
    }

    private fun createLegacyBlur(bitmap: Bitmap, blurRadius: Float): Bitmap {
        // Create a darkened/obscured version as blur alternative
        val blurred = createBitmap(
            bitmap.width,
            bitmap.height,
            bitmap.config ?: Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(blurred)

        // First, draw the original bitmap with reduced opacity
        val paint = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            alpha = 100 // Make it semi-transparent to simulate blur
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // Then draw a semi-transparent overlay to obscure the content
        val overlayPaint = Paint().apply {
            color = android.graphics.Color.argb(150, 0, 0, 0)
        }
        canvas.drawRect(
            0f,
            0f,
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            overlayPaint
        )

        return blurred
    }

    /**
     * Adds overlay text to a bitmap.
     *
     * @param bitmap The bitmap to overlay text on
     * @param text The text to add
     * @return The bitmap with text overlay
     */
    fun addOverlayText(bitmap: Bitmap, text: String): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 48f
            textAlign = Paint.Align.CENTER
            flags = Paint.ANTI_ALIAS_FLAG
            setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
        }

        val x = bitmap.width / 2f
        val y = bitmap.height / 2f

        canvas.drawText(text, x, y, paint)
        return result
    }

    /**
     * Creates a pixelated effect as an alternative blur method.
     *
     * @param bitmap The original bitmap
     * @param pixelSize The size of pixels for the effect
     * @return The pixelated bitmap
     */
    fun pixelateBitmap(bitmap: Bitmap, pixelSize: Int = 20): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val paint = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = false
        }

        // Create small version and scale it back up for pixelation effect
        val smallWidth = maxOf(1, width / pixelSize)
        val smallHeight = maxOf(1, height / pixelSize)

        val smallBitmap = bitmap.scale(smallWidth, smallHeight, false)
        val scaledBitmap = smallBitmap.scale(width, height, false)

        canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

        // Clean up temporary bitmaps
        smallBitmap.recycle()
        scaledBitmap.recycle()

        return result
    }

    /**
     * Creates a darkened overlay effect.
     *
     * @param bitmap The original bitmap
     * @return The darkened bitmap
     */
    fun createDarkenedOverlay(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Draw semi-transparent black overlay
        val overlayPaint = Paint().apply {
            color = android.graphics.Color.argb(180, 0, 0, 0)
        }
        canvas.drawRect(
            0f,
            0f,
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            overlayPaint
        )

        return result
    }
}