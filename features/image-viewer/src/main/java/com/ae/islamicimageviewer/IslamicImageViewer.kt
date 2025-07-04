package com.ae.islamicimageviewer

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
import kotlinx.coroutines.launch

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
        if (enableFilter) ImageProcessor(context) else null
    }

    DisposableEffect(Unit) {
        onDispose {
            imageProcessor?.cleanup()
        }
    }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(model)
            .crossfade(true)
            .build(),
        onSuccess = { state ->
            if (enableFilter) {
                scope.launch {
                    try {
                        isProcessing = true
                        val drawable = state.result.drawable
                        val bitmap = drawable.toBitmap()

                        val result = imageProcessor?.processImage(bitmap)
                        shouldBlur = result?.shouldBlur ?: false

                        if (shouldBlur) {
                            onImageFiltered?.invoke(result?.reason ?: "Content filtered")
                        } else {
                            onImageApproved?.invoke()
                        }
                    } catch (e: Exception) {
                        onError?.invoke(e)
                        shouldBlur = false
                    } finally {
                        isProcessing = false
                    }
                }
            } else {
                isProcessing = false
                onImageApproved?.invoke()
            }
        },
        onError = { state ->
            isProcessing = false
            onError?.invoke(state.result.throwable)
        }
    )

    Box(modifier = modifier) {
        // Capture the state to avoid smart cast issues
        when (val state = painter.state) {
            is AsyncImagePainter.State.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is AsyncImagePainter.State.Success -> {
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
                // Now we can safely access the error
                LaunchedEffect(state) {
                    onError?.invoke(state.result.throwable)
                }
            }
            is AsyncImagePainter.State.Empty -> {
                // Empty state - nothing to show
            }
        }
    }
}