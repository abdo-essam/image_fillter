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
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "IslamicImageViewer"

/**
 * A Composable that displays images with automatic content moderation
 * for Islamic cultural sensitivity. Images containing women will be
 * automatically blurred.
 */
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
    var isProcessing by remember { mutableStateOf(true) }

    val imageProcessor = remember {
        if (enableFilter) {
            Log.d(TAG, "Creating ImageProcessor")
            try {
                ImageProcessor(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create ImageProcessor", e)
                null
            }
        } else {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Disposing ImageProcessor")
            imageProcessor?.cleanup()
        }
    }

    Log.d(TAG, "Loading image: $model")

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(model)
            .crossfade(true)
            .allowHardware(false) // Disable hardware bitmaps for ML processing
            .build(),
        onSuccess = { state ->
            Log.d(TAG, "Image loaded successfully: $model")
            if (enableFilter && imageProcessor != null) {
                scope.launch(Dispatchers.Default) {
                    try {
                        isProcessing = true
                        Log.d(TAG, "Starting image processing for: $model")

                        val drawable = state.result.drawable
                        val bitmap = drawable.toBitmap()
                        Log.d(TAG, "Bitmap created: ${bitmap.width}x${bitmap.height}")

                        val result = imageProcessor.processImage(bitmap)
                        Log.d(TAG, "Processing result: shouldBlur=${result.shouldBlur}, reason=${result.reason}")

                        shouldBlur = result.shouldBlur

                        scope.launch(Dispatchers.Main) {
                            if (shouldBlur) {
                                onImageFiltered?.invoke(result.reason ?: "Content filtered")
                            } else {
                                onImageApproved?.invoke()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing image", e)
                        scope.launch(Dispatchers.Main) {
                            onError?.invoke(e)
                        }
                        shouldBlur = false
                    } finally {
                        isProcessing = false
                    }
                }
            } else {
                Log.d(TAG, "Filter disabled or processor null, approving image")
                isProcessing = false
                onImageApproved?.invoke()
            }
        },
        onError = { state ->
            Log.e(TAG, "Image loading error: ${state.result.throwable.message}")
            isProcessing = false
            onError?.invoke(state.result.throwable)
        }
    )

    Box(modifier = modifier) {
        // Capture the state to avoid smart cast issues
        when (val state = painter.state) {
            is AsyncImagePainter.State.Loading -> {
                Log.d(TAG, "State: Loading")
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is AsyncImagePainter.State.Success -> {
                Log.d(TAG, "State: Success, isProcessing=$isProcessing, shouldBlur=$shouldBlur")
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (shouldBlur && !isProcessing) {
                                Modifier.blur(radius = 20.dp)
                            } else {
                                Modifier
                            }
                        ),
                    contentScale = contentScale,
                    colorFilter = if (shouldBlur && !isProcessing) {
                        ColorFilter.colorMatrix(
                            ColorMatrix().apply {
                                setToSaturation(0.3f)
                            }
                        )
                    } else null
                )

                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            is AsyncImagePainter.State.Error -> {
                Log.e(TAG, "State: Error - ${state.result.throwable.message}")
                LaunchedEffect(state) {
                    onError?.invoke(state.result.throwable)
                }
            }
            is AsyncImagePainter.State.Empty -> {
                Log.d(TAG, "State: Empty")
            }
        }
    }
}