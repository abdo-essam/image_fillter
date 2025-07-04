package com.ae.islamicimageviewer

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "IslamicImageViewer"

@Composable
fun IslamicImageViewer(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onError: ((Throwable) -> Unit)? = null,
    onImageFiltered: ((reason: String) -> Unit)? = null,
    onImageApproved: (() -> Unit)? = null,
    enableFilter: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var shouldBlur by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var imageLoaded by remember { mutableStateOf(false) }

    Log.d(TAG, "IslamicImageViewer composing for model: $model")

    val imageProcessor = remember {
        if (enableFilter) {
            try {
                Log.d(TAG, "Creating ImageProcessor")
                ImageProcessor(context).also {
                    Log.d(TAG, "ImageProcessor created successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create ImageProcessor", e)
                onError?.invoke(e)
                null
            }
        } else {
            null
        }
    }

    DisposableEffect(imageProcessor) {
        onDispose {
            Log.d(TAG, "Disposing ImageProcessor")
            imageProcessor?.cleanup()
        }
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .allowHardware(false)
                .listener(
                    onStart = {
                        Log.d(TAG, "Image loading started: $model")
                    },
                    onSuccess = { request, result ->
                        Log.d(TAG, "Image loaded successfully: $model")
                        imageLoaded = true

                        if (enableFilter && imageProcessor != null) {
                            scope.launch {
                                try {
                                    isProcessing = true
                                    Log.d(TAG, "Starting processing")

                                    // Get bitmap from result
                                    val bitmap = (result as? SuccessResult)?.drawable?.toBitmap()
                                    if (bitmap != null) {
                                        Log.d(TAG, "Bitmap obtained: ${bitmap.width}x${bitmap.height}")

                                        val processingResult = withContext(Dispatchers.Default) {
                                            imageProcessor.processImage(bitmap)
                                        }

                                        Log.d(TAG, "Processing complete: shouldBlur=${processingResult.shouldBlur}")
                                        shouldBlur = processingResult.shouldBlur

                                        if (shouldBlur) {
                                            onImageFiltered?.invoke(processingResult.reason ?: "Content filtered")
                                        } else {
                                            onImageApproved?.invoke()
                                        }
                                    } else {
                                        Log.e(TAG, "Failed to get bitmap from drawable")
                                        onError?.invoke(Exception("Failed to process image"))
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error during processing", e)
                                    onError?.invoke(e)
                                } finally {
                                    isProcessing = false
                                }
                            }
                        } else {
                            Log.d(TAG, "Filter disabled or processor null")
                            onImageApproved?.invoke()
                        }
                    },
                    onError = { request, error ->
                        Log.e(TAG, "Image loading failed: ${error.throwable.message}")
                        onError?.invoke(error.throwable)
                    }
                )
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (shouldBlur && !isProcessing && imageLoaded) {
                        Modifier.blur(radius = 20.dp)
                    } else {
                        Modifier
                    }
                ),
            colorFilter = if (shouldBlur && !isProcessing && imageLoaded) {
                ColorFilter.colorMatrix(
                    ColorMatrix().apply {
                        setToSaturation(0.3f)
                    }
                )
            } else null
        )

        if (isProcessing || !imageLoaded) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}