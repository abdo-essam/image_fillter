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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "IslamicImageViewer"

@Composable
fun IslamicImageViewer(
    url: String,
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

    // State to track processing status
    var processingState by remember(url) {
        mutableStateOf(ProcessingState.NOT_STARTED)
    }
    var shouldBlur by remember(url) { mutableStateOf(false) }

    Log.d(TAG, "IslamicImageViewer composing for url: $url, state: $processingState")

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
                .data(url)
                .crossfade(true)
                .allowHardware(false)
                .listener(
                    onStart = {
                        Log.d(TAG, "Image loading started: $url")
                    },
                    onSuccess = { request, result ->
                        Log.d(TAG, "Image loaded successfully: $url")

                        // Only process if we haven't processed this image yet
                        if (processingState == ProcessingState.NOT_STARTED &&
                            enableFilter &&
                            imageProcessor != null) {

                            processingState = ProcessingState.PROCESSING

                            scope.launch {
                                try {
                                    Log.d(TAG, "Starting one-time processing for: $url")

                                    // Get bitmap from result
                                    val bitmap = (result as? SuccessResult)?.drawable?.toBitmap()
                                    if (bitmap != null) {
                                        Log.d(TAG, "Bitmap obtained: ${bitmap.width}x${bitmap.height}")

                                        val processingResult = withContext(Dispatchers.Default) {
                                            imageProcessor.processImage(bitmap)
                                        }

                                        Log.d(TAG, "Processing complete: shouldBlur=${processingResult.shouldBlur}")
                                        shouldBlur = processingResult.shouldBlur

                                        // Mark as completed
                                        processingState = ProcessingState.COMPLETED

                                        // Call appropriate callback
                                        if (shouldBlur) {
                                            onImageFiltered?.invoke(processingResult.reason ?: "Content filtered")
                                        } else {
                                            onImageApproved?.invoke()
                                        }
                                    } else {
                                        Log.e(TAG, "Failed to get bitmap from drawable")
                                        processingState = ProcessingState.ERROR
                                        onError?.invoke(Exception("Failed to process image"))
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error during processing", e)
                                    processingState = ProcessingState.ERROR
                                    onError?.invoke(e)
                                }
                            }
                        } else if (!enableFilter) {
                            // If filter is disabled, mark as completed immediately
                            processingState = ProcessingState.COMPLETED
                            onImageApproved?.invoke()
                        }
                    },
                    onError = { request, error ->
                        Log.e(TAG, "Image loading failed: ${error.throwable.message}")
                        processingState = ProcessingState.ERROR
                        onError?.invoke(error.throwable)
                    }
                )
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (shouldBlur && processingState == ProcessingState.COMPLETED) {
                        Modifier.blur(radius = 20.dp)
                    } else {
                        Modifier
                    }
                ),
            colorFilter = if (shouldBlur && processingState == ProcessingState.COMPLETED) {
                ColorFilter.colorMatrix(
                    ColorMatrix().apply {
                        setToSaturation(0.3f)
                    }
                )
            } else null
        )

        // Show loading indicator only while processing
        if (processingState == ProcessingState.PROCESSING) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private enum class ProcessingState {
    NOT_STARTED,
    PROCESSING,
    COMPLETED,
    ERROR
}