package com.ae.image_viewer_4

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ae.image_viewer_4.internal.FilteredImageLoader

@Composable
fun Image(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    strictMode: Boolean = true,
    allowClickToReveal: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null,
    onContentFiltered: ((reason: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val imageLoader = remember { FilteredImageLoader(context) }

    var imageState by remember(url) { mutableStateOf<ImageState>(ImageState.Loading) }
    var isRevealed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        imageState = ImageState.Loading
        isRevealed = false
        imageLoader.loadImage(url) { result ->
            imageState = when (result) {
                is FilteredImageLoader.Result.Success -> ImageState.Success(result.url)
                is FilteredImageLoader.Result.Filtered -> {
                    onContentFiltered?.invoke(result.reason)
                    ImageState.Filtered(result.reason, result.url)
                }

                is FilteredImageLoader.Result.Error -> ImageState.Error(result.exception)
            }
        }
    }

    Box(modifier = modifier) {
        when (val state = imageState) {
            is ImageState.Loading -> {
                placeholder?.invoke() ?: DefaultLoadingIndicator()
            }

            is ImageState.Success -> {
                AsyncImage(
                    model = state.url,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    onError = {
                        imageState = ImageState.Error(Exception("Failed to display image"))
                    }
                )
            }

            is ImageState.Filtered -> {
                if (isRevealed && allowClickToReveal) {
                    // Show the actual image with an overlay
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = state.originalUrl,
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = contentScale,
                        )

                        // Warning overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(
                                    Color.Red.copy(alpha = 0.9f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Filtered Content",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    FilteredContentPlaceholder(
                        reason = state.reason,
                        clickable = allowClickToReveal,
                        onRevealClick = { isRevealed = true }
                    )
                }
            }

            is ImageState.Error -> {
                error?.invoke() ?: DefaultErrorIndicator()
            }
        }
    }
}

@Composable
private fun DefaultLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FilteredContentPlaceholder(
    reason: String,
    clickable: Boolean = true,
    onRevealClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.2f))
            .then(
                if (clickable) Modifier.clickable { onRevealClick() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "Content filtered",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Content Filtered",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (clickable) {
                Text(
                    text = "Tap to reveal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun DefaultErrorIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Failed to load image",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

private sealed class ImageState {
    object Loading : ImageState()
    data class Success(val url: String) : ImageState()
    data class Filtered(val reason: String, val originalUrl: String) : ImageState()
    data class Error(val exception: Throwable) : ImageState()
}