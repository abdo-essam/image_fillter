package com.ae.image_viewer_2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.ae.image_viewer_2.internal.BlurTransformation
import com.ae.image_viewer_2.internal.ContentAnalyzer
import kotlinx.coroutines.launch

@Composable
fun FilteredImage(
    imageUrl: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    onLoading: () -> Unit = {},
    onError: () -> Unit = {},
    onContentFiltered: (Boolean) -> Unit = {},
    contentFilteringEnabled: Boolean = true
) {
    val context = LocalContext.current
    val contentAnalyzer = remember { ContentAnalyzer(context) }
    val scope = rememberCoroutineScope()

    var shouldBlur by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }
    var hasAnalyzed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            contentAnalyzer.cleanup()
        }
    }

    Box(
        modifier = modifier.clickable {
            if (shouldBlur) {
                showOriginal = !showOriginal
            }
        }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .apply {
                    if (shouldBlur && !showOriginal) {
                        transformations(BlurTransformation(radius = 25f))
                    }
                }
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            onState = { state ->
                when (state) {
                    is AsyncImagePainter.State.Loading -> onLoading()
                    is AsyncImagePainter.State.Success -> {
                        if (contentFilteringEnabled && !hasAnalyzed) {
                            hasAnalyzed = true
                            scope.launch {
                                contentAnalyzer.analyzeImage(state.result.drawable) { hasInappropriateContent ->
                                    shouldBlur = hasInappropriateContent
                                    onContentFiltered(hasInappropriateContent)
                                }
                            }
                        }
                    }
                    is AsyncImagePainter.State.Error -> {
                        onError()
                        onContentFiltered(false)
                    }
                    else -> {}
                }
            }
        )
    }
}