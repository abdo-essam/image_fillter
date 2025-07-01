package com.ae.image_viewer_2.internal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import coil.size.Size
import coil.transform.Transformation

internal class BlurTransformation(
    private val radius: Float = 25f
) : Transformation {

    override val cacheKey: String = "${BlurTransformation::class.java.name}-$radius"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Use RenderEffect for API 31+
                blurWithRenderEffect(input)
            }
            else -> {
                // Use stack blur algorithm for older versions
                stackBlur(input, radius.toInt())
            }
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private fun blurWithRenderEffect(input: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)

        val renderNode = android.graphics.RenderNode("blur")
        renderNode.setPosition(0, 0, input.width, input.height)

        val canvas = renderNode.beginRecording(input.width, input.height)
        canvas.drawBitmap(input, 0f, 0f, null)
        renderNode.endRecording()

        val renderEffect = android.graphics.RenderEffect.createBlurEffect(
            radius, radius,
            android.graphics.Shader.TileMode.CLAMP
        )
        renderNode.setRenderEffect(renderEffect)

        val outputCanvas = Canvas(output)
        outputCanvas.drawRenderNode(renderNode)

        return output
    }

    /**
     * Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
     * Simplified implementation for fallback
     */
    private fun stackBlur(source: Bitmap, radius: Int): Bitmap {
        if (radius < 1) {
            return source
        }

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)

        // Simple box blur as fallback
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        // Create a smaller bitmap for blur effect
        val scale = 0.4f
        val scaledWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (source.height * scale).toInt().coerceAtLeast(1)

        val smallBitmap = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        val blurredBitmap = Bitmap.createScaledBitmap(smallBitmap, source.width, source.height, true)

        val canvas = Canvas(output)
        canvas.drawBitmap(blurredBitmap, 0f, 0f, paint)

        // Apply multiple passes for stronger blur
        val tempBitmap = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempBitmap)

        paint.alpha = 180
        for (i in 0..2) {
            tempCanvas.drawBitmap(output, 0f, 0f, paint)
            canvas.drawBitmap(tempBitmap, 0f, 0f, paint)
        }

        smallBitmap.recycle()
        blurredBitmap.recycle()
        tempBitmap.recycle()

        return output
    }
}