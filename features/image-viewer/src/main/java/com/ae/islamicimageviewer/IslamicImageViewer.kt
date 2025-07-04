package com.ae.islamicimageviewer

// UI and Android dependencies
import android.util.Log
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

// Log tag for debugging
private const val TAG = "IslamicImageViewer"

/**
 * Custom image viewer that optionally applies a gender-based blur filter.
 *
 * @param url The image URL.
 * @param contentDescription Description for accessibility.
 * @param modifier Modifier to style the Composable.
 * @param contentScale How the image should scale inside its bounds.
 * @param onError Callback when any error occurs (loading or processing).
 * @param onImageFiltered Callback when image is filtered (e.g., contains female faces).
 * @param onImageApproved Callback when image passes without filtering.
 * @param enableFilter Enables or disables the gender-based filtering logic.
 */
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

    // State tracking image processing phase
    var processingState by remember(url) {
        mutableStateOf(ProcessingState.NOT_STARTED)
    }

    // Whether the image should be blurred based on detection
    var shouldBlur by remember(url) { mutableStateOf(false) }

    Log.d(TAG, "IslamicImageViewer composing for url: $url, state: $processingState")

    // Lazily initialize the ImageProcessor only once (if filter is enabled)
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
        } else null
    }

    // Cleanup resources when this composable leaves composition
    DisposableEffect(imageProcessor) {
        onDispose {
            Log.d(TAG, "Disposing ImageProcessor")
            imageProcessor?.cleanup()
        }
    }

    // UI container for image and progress indicator
    Box(modifier = modifier) {

        // Coil's AsyncImage loads the image asynchronously
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .allowHardware(false)
                .listener(
                    // Called when image loading starts
                    onStart = {
                        Log.d(TAG, "Image loading started: $url")
                    },

                    // Called when image is successfully loaded
                    onSuccess = { request, result ->
                        Log.d(TAG, "Image loaded successfully: $url")

                        // Process image only once and if filtering is enabled
                        if (processingState == ProcessingState.NOT_STARTED &&
                            enableFilter &&
                            imageProcessor != null
                        ) {
                            processingState = ProcessingState.PROCESSING

                            // Start coroutine to process image off the main thread
                            scope.launch {
                                try {
                                    Log.d(TAG, "Starting one-time processing for: $url")

                                    // Extract bitmap from the result drawable
                                    val bitmap = (result as? SuccessResult)?.drawable?.toBitmap()
                                    if (bitmap != null) {
                                        Log.d(TAG, "Bitmap obtained: ${bitmap.width}x${bitmap.height}")

                                        // Run gender detection in background thread
                                        val processingResult = withContext(Dispatchers.Default) {
                                            imageProcessor.processImage(bitmap)
                                        }

                                        shouldBlur = processingResult.shouldBlur
                                        processingState = ProcessingState.COMPLETED

                                        // Fire appropriate callback
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
                            // If filtering is disabled, mark processing as done immediately
                            processingState = ProcessingState.COMPLETED
                            onImageApproved?.invoke()
                        }
                    },

                    // Called if the image loading fails
                    onError = { request, error ->
                        Log.e(TAG, "Image loading failed: ${error.throwable.message}")
                        processingState = ProcessingState.ERROR
                        onError?.invoke(error.throwable)
                    }
                )
                .build(),

            // Content description for accessibility
            contentDescription = contentDescription,

            // How the image scales
            contentScale = contentScale,

            // Image modifier: fills the parent, applies blur if needed
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (shouldBlur && processingState == ProcessingState.COMPLETED) {
                        Modifier.blur(radius = 20.dp)
                    } else Modifier
                ),

            // Optional desaturation color filter if image is blurred
            colorFilter = if (shouldBlur && processingState == ProcessingState.COMPLETED) {
                ColorFilter.colorMatrix(
                    ColorMatrix().apply {
                        setToSaturation(0.3f)
                    }
                )
            } else null
        )

        // Show a loading spinner in the center while the image is being analyzed
        if (processingState == ProcessingState.PROCESSING) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// Enum representing the internal state of the image processing
private enum class ProcessingState {
    NOT_STARTED, // Initial state before any work
    PROCESSING,  // Actively detecting faces and gender
    COMPLETED,   // Processing completed (blur or approve)
    ERROR        // Error occurred during processing
}
