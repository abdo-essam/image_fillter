package com.ae.islamicimageviewer


import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap

/**
 * Islamic-compliant Image composable that automatically detects and blurs
 * potentially inappropriate content based on Islamic guidelines.
 */
@Composable
internal fun IslamicImage(
    imageUrl: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    enableContentFiltering: Boolean = true,
    onContentFiltered: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val contentDetectionService = remember { ContentDetectionService() }
    val coroutineScope = rememberCoroutineScope()

    var isContentAppropriate by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(Size.ORIGINAL)
            .build()
    )

    // Monitor painter state and analyze content when image loads
    LaunchedEffect(painter.state) {
        if (painter.state is AsyncImagePainter.State.Success && enableContentFiltering) {
            coroutineScope.launch {
                try {
                    val bitmap = (painter.state as AsyncImagePainter.State.Success).result.drawable
                        .toBitmap()

                    val isAppropriate = contentDetectionService.isContentAppropriate(bitmap)
                    isContentAppropriate = isAppropriate
                    onContentFiltered?.invoke(!isAppropriate)
                } catch (e: Exception) {
                    // If analysis fails, assume content is appropriate
                    isContentAppropriate = true
                } finally {
                    isLoading = false
                }
            }
        } else if (!enableContentFiltering) {
            isContentAppropriate = true
            isLoading = false
        }
    }

    Box(modifier = modifier) {
        when {
            isLoading && enableContentFiltering -> {
                // Show loading indicator while analyzing content
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            isContentAppropriate == false -> {
                // Show blurred image with overlay
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 20.dp),
                    contentScale = contentScale
                )

                // Overlay text
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "محتوى محجوب\nContent Filtered",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            else -> {
                // Show normal image
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }
    }
}

/**
 * Extension function to convert Drawable to Bitmap.
 */
internal fun android.graphics.drawable.Drawable.toBitmap(): Bitmap {
    val bitmap = createBitmap(intrinsicWidth, intrinsicHeight)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}