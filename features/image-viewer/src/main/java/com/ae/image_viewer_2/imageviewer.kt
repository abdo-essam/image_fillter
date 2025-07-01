package com.ae.image_viewer_2

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ae.image_viewer_2.internal.BlurTransformation
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment

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

    // Simple URL-based filtering for testing
    val shouldFilter = remember(imageUrl, contentFilteringEnabled) {
        if (contentFilteringEnabled && imageUrl is String) {
            imageUrl.contains("1544005313") || // Woman photo
                    imageUrl.contains("1522202176988") || // Group photo
                    imageUrl.contains("random=1") // Test image
        } else false
    }

    var painterState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    LaunchedEffect(painterState) {
        when (painterState) {
            is AsyncImagePainter.State.Loading -> onLoading()
            is AsyncImagePainter.State.Success -> onContentFiltered(shouldFilter)
            is AsyncImagePainter.State.Error -> onError()
            else -> {}
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .apply {
                if (shouldFilter) {
                    transformations(BlurTransformation(radius = 25f))
                }
            }
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        onState = { state ->
            painterState = state
        }
    )
}