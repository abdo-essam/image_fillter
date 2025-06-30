package com.ae.image_viewer.internal

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.ae.image_viewer.model.ContentAnalysisResult
import kotlinx.coroutines.launch

@Composable
internal fun ImageViewerInternal(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    colorFilter: ColorFilter?,
    onContentFiltered: ((reason: String) -> Unit)?
) {
    val context = LocalContext.current
    val contentFilter = remember { ContentFilter() }
    var analysisComplete by remember { mutableStateOf(false) }
    var isContentAppropriate by remember { mutableStateOf<Boolean?>(null) }
    var filterReason by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Log.d("ImageViewerInternal", "Loading image: $url")

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .listener(
                onSuccess = { _, result ->
                    if (!analysisComplete) {
                        scope.launch {
                            try {
                                Log.d("ImageViewerInternal", "Image loaded successfully, starting analysis")
                                val bitmap = result.drawable.toBitmap()

                                when (val analysisResult = contentFilter.analyzeImage(bitmap)) {
                                    ContentAnalysisResult.Appropriate -> {
                                        isContentAppropriate = true
                                        filterReason = null
                                        Log.d("ImageViewerInternal", "Content approved")
                                    }
                                    is ContentAnalysisResult.Inappropriate -> {
                                        isContentAppropriate = false
                                        filterReason = analysisResult.reason
                                        onContentFiltered?.invoke(analysisResult.reason)
                                        Log.d("ImageViewerInternal", "Content filtered: ${analysisResult.reason}")
                                    }
                                    is ContentAnalysisResult.Error -> {
                                        // On error, show the image
                                        isContentAppropriate = true
                                        filterReason = null
                                        Log.e("ImageViewerInternal", "Analysis error: ${analysisResult.message}")
                                    }
                                }
                                analysisComplete = true
                            } catch (e: Exception) {
                                Log.e("ImageViewerInternal", "Exception during analysis", e)
                                isContentAppropriate = true
                                analysisComplete = true
                            }
                        }
                    }
                }
            )
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter
    ) {
        when (val state = painter.state) {
            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFCF6F5)),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            is AsyncImagePainter.State.Success -> {
                // Wait for analysis to complete
                if (!analysisComplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFCF6F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                } else {
                    // Always show the actual image content
                    // The parent component will handle showing/hiding based on filter status
                    SubcomposeAsyncImageContent()
                }
            }
            is AsyncImagePainter.State.Error -> {
                ErrorPlaceholder(
                    modifier = Modifier.fillMaxSize()
                )
            }
            AsyncImagePainter.State.Empty -> {
                // Handle empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFCF6F5))
                )
            }
        }
    }
}