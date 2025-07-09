package com.ae.haram_blur

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ae.islamicimageviewer.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "IslamicImageViewer"

/**
 * Configuration for the Islamic Image Viewer
 */
data class IslamicImageViewerConfig(
    val enableFilter: Boolean = true,
    val blurStrength: Float = 20f,
    val allowClickToReveal: Boolean = false,
    val showBlurIcon: Boolean = true,
    val showFaceBoxes: Boolean = false,
    val faceBoxColors: FaceBoxColors = FaceBoxColors(),
    val blurSettings: HaramBlurImageProcessor.BlurSettings = HaramBlurImageProcessor.BlurSettings()
)

data class FaceBoxColors(
    val female: Color = Color.Red,
    val male: Color = Color.Blue,
    val uncertain: Color = Color.Yellow,
    val strokeWidth: Float = 3f
)

/**
 * A Composable that displays images with automatic content moderation
 * for Islamic cultural sensitivity using HaramBlur-style detection.
 */
@Composable
fun HaramImageViewer(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    config: IslamicImageViewerConfig = IslamicImageViewerConfig(),
    onError: ((Throwable) -> Unit)? = null,
    onImageFiltered: ((reason: String, details: HaramBlurImageProcessor.DetectionDetails?) -> Unit)? = null,
    onImageApproved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State management
    var processingState by remember(model) {
        mutableStateOf(ProcessingState.NOT_STARTED)
    }
    var shouldBlur by remember(model) { mutableStateOf(false) }
    var blurReason by remember(model) { mutableStateOf<String?>(null) }
    var detectionDetails by remember(model) { mutableStateOf<HaramBlurImageProcessor.DetectionDetails?>(null) }
    var isRevealed by remember(model) { mutableStateOf(false) }

    // Add state for image size
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    // Animated blur value for smooth transitions
    val animatedBlurRadius by animateFloatAsState(
        targetValue = if (shouldBlur && !isRevealed) config.blurStrength else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "blur_animation"
    )

    Log.d(TAG, "IslamicImageViewer composing for model: $model, state: $processingState")

    val imageProcessor = remember(config.blurSettings) {
        if (config.enableFilter) {
            try {
                Log.d(TAG, "Creating HaramBlur processor")
                HaramBlurImageProcessor(context, config.blurSettings).also {
                    Log.d(TAG, "HaramBlur processor created successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create HaramBlur processor", e)
                onError?.invoke(e)
                null
            }
        } else {
            null
        }
    }

    DisposableEffect(imageProcessor) {
        onDispose {
            Log.d(TAG, "Disposing HaramBlur processor")
            imageProcessor?.cleanup()
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                viewSize = coordinates.size
            }
            .then(
                if (config.allowClickToReveal && shouldBlur) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isRevealed = !isRevealed
                    }
                } else {
                    Modifier
                }
            )
    ) {
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

                        // Get image dimensions
                        val bitmap = result.drawable.toBitmap()
                        bitmap?.let {
                            imageSize = IntSize(it.width, it.height)
                        }

                        if (processingState == ProcessingState.NOT_STARTED &&
                            config.enableFilter &&
                            imageProcessor != null) {

                            processingState = ProcessingState.PROCESSING

                            scope.launch {
                                try {
                                    Log.d(TAG, "Starting HaramBlur processing for: $model")

                                    if (bitmap != null) {
                                        Log.d(TAG, "Bitmap obtained: ${bitmap.width}x${bitmap.height}")

                                        val processingResult = withContext(Dispatchers.Default) {
                                            imageProcessor.processImage(bitmap)
                                        }

                                        Log.d(TAG, "Processing complete: shouldBlur=${processingResult.shouldBlur}")
                                        shouldBlur = processingResult.shouldBlur
                                        blurReason = processingResult.reason
                                        detectionDetails = processingResult.detectionDetails

                                        processingState = ProcessingState.COMPLETED

                                        if (shouldBlur) {
                                            onImageFiltered?.invoke(
                                                processingResult.reason ?: "Content filtered",
                                                processingResult.detectionDetails
                                            )
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
                        } else if (!config.enableFilter) {
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
                    if (animatedBlurRadius > 0f) {
                        Modifier.blur(radius = animatedBlurRadius.dp)
                    } else {
                        Modifier
                    }
                ),
            colorFilter = if (shouldBlur && !isRevealed && processingState == ProcessingState.COMPLETED) {
                ColorFilter.colorMatrix(
                    ColorMatrix().apply {
                        setToSaturation(0.3f) // Reduce saturation for blurred images
                    }
                )
            } else null
        )

        // Add face box overlay
        if (config.showFaceBoxes &&
            detectionDetails != null &&
            detectionDetails!!.faceBoundingBoxes.isNotEmpty() &&
            imageSize != IntSize.Zero &&
            processingState == ProcessingState.COMPLETED) {

            FaceBoxOverlay(
                faceInfoList = detectionDetails!!.faceBoundingBoxes,
                imageSize = imageSize,
                viewSize = viewSize,
                colors = config.faceBoxColors,
                modifier = Modifier.matchParentSize()
            )
        }

        // Show loading indicator while processing
        if (processingState == ProcessingState.PROCESSING) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Show blur icon overlay
        if (config.showBlurIcon && shouldBlur && !isRevealed &&
            processingState == ProcessingState.COMPLETED) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_secure),
                    contentDescription = "Content blurred",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun FaceBoxOverlay(
    faceInfoList: List<HaramBlurImageProcessor.FaceInfo>,
    imageSize: IntSize,
    viewSize: IntSize,
    colors: FaceBoxColors,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (imageSize.width == 0 || imageSize.height == 0) return@Canvas

        // Calculate scale factors
        val scaleX = size.width / imageSize.width
        val scaleY = size.height / imageSize.height

        faceInfoList.forEach { faceInfo ->
            val rect = faceInfo.boundingBox

            // Scale the rectangle coordinates to match the displayed image size
            val scaledLeft = rect.left * scaleX
            val scaledTop = rect.top * scaleY
            val scaledRight = rect.right * scaleX
            val scaledBottom = rect.bottom * scaleY

            val color = when (faceInfo.gender) {
                HaramBlurImageProcessor.Gender.FEMALE -> colors.female
                HaramBlurImageProcessor.Gender.MALE -> colors.male
                HaramBlurImageProcessor.Gender.UNCERTAIN -> colors.uncertain
            }

            drawRect(
                color = color,
                topLeft = Offset(scaledLeft, scaledTop),
                size = Size(
                    scaledRight - scaledLeft,
                    scaledBottom - scaledTop
                ),
                style = Stroke(
                    width = colors.strokeWidth.dp.toPx()
                )
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