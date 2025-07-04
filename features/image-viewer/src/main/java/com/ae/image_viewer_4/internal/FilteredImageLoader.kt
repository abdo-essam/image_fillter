package com.ae.image_viewer_4.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.ae.image_viewer_4.contentfilter.ContentDetector
import com.ae.image_viewer_4.contentfilter.IslamicContentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


internal class FilteredImageLoader(private val context: Context) {
    private val imageLoader = ImageLoader.Builder(context).build()
    private val contentDetector = ContentDetector()
    private val contentFilter = IslamicContentFilter()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun loadImage(url: String, callback: (Result) -> Unit) {
        scope.launch {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(Size.ORIGINAL)
                    .allowHardware(false)
                    .build()

                when (val result = imageLoader.execute(request)) {
                    is SuccessResult -> {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            processImage(bitmap, url, callback)
                        } else {
                            withContext(Dispatchers.Main) {
                                callback(Result.Error(Exception("Failed to extract bitmap")))
                            }
                        }
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            callback(Result.Error(Exception("Failed to load image")))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(Result.Error(e))
                }
            }
        }
    }

    private suspend fun processImage(bitmap: Bitmap, url: String, callback: (Result) -> Unit) {
        val analysis = contentDetector.analyzeImage(bitmap)

        when (val filterResult = contentFilter.shouldFilterContent(analysis)) {
            is IslamicContentFilter.FilterResult.Allowed -> {
                withContext(Dispatchers.Main) {
                    callback(Result.Success(url))
                }
            }
            is IslamicContentFilter.FilterResult.Filtered -> {
                withContext(Dispatchers.Main) {
                    callback(Result.Filtered(filterResult.reason, url))
                }
            }
        }
    }

    sealed class Result {
        data class Success(val url: String) : Result()
        data class Filtered(val reason: String, val url: String) : Result()
        data class Error(val exception: Throwable) : Result()
    }
}