package com.ae.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ae.design_system.components.Text
import com.ae.design_system.theme.AppTheme
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import com.ae.image_viewer_2.FilteredImage
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun ImageViewerTestScreen() {
    val testImages = listOf(
        TestImage("https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&h=300&fit=crop", "Woman"),
        TestImage("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=300&fit=crop", "Man"),
        TestImage("https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=400&h=300&fit=crop", "Group"),
        TestImage("https://picsum.photos/400/300?random=1", "Random 1"),
        TestImage("https://picsum.photos/400/300?random=2", "Random 2"),
        TestImage("https://picsum.photos/400/300?random=3", "Random 3")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        Text(
            text = "Filter Test",
            style = AppTheme.typography.headlineMedium,
            color = AppTheme.colors.title,
            modifier = Modifier.padding(AppTheme.dimensions.spacingMedium)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(AppTheme.dimensions.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spacingSmall)
        ) {
            items(testImages) { testImage ->
                GridImageItem(testImage = testImage)
            }
        }
    }
}

data class TestImage(val url: String, val label: String)

@Composable
private fun GridImageItem(testImage: TestImage) {
    var imageState by remember { mutableStateOf(ImageLoadState.LOADING) }
    var isFiltered by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppTheme.dimensions.cornerRadiusMedium))
            .background(AppTheme.colors.surface)
            .clickable {
                if (isFiltered) {
                    showOriginal = !showOriginal
                }
            }
    ) {
        // Always show the image
        FilteredImage(
            imageUrl = testImage.url,
            contentDescription = testImage.label,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            contentFilteringEnabled = !showOriginal,
            onLoading = {
                imageState = ImageLoadState.LOADING
            },
            onError = {
                imageState = ImageLoadState.ERROR
            },
            onContentFiltered = { filtered ->
                imageState = ImageLoadState.SUCCESS
                isFiltered = filtered
            }
        )

        // Loading overlay
        if (imageState == ImageLoadState.LOADING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AppTheme.colors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Error overlay
        if (imageState == ImageLoadState.ERROR) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "❌ Failed",
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.error
                )
            }
        }

        // Filtered overlay (only when filtered and not showing original)
        if (imageState == ImageLoadState.SUCCESS && isFiltered && !showOriginal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        AppTheme.colors.error.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(AppTheme.dimensions.spacingSmall)
                ) {
                    Text(
                        text = "🚫",
                        style = AppTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Tap to view",
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.onSurface
                    )
                }
            }
        }

        // Label
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(AppTheme.dimensions.spacingSmall)
                .background(
                    AppTheme.colors.surface.copy(alpha = 0.9f),
                    RoundedCornerShape(AppTheme.dimensions.cornerRadiusSmall)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = testImage.label,
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.onSurface
            )
        }

        // Status indicator
        if (imageState == ImageLoadState.SUCCESS) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppTheme.dimensions.spacingSmall)
                    .size(12.dp)
                    .background(
                        when {
                            !isFiltered -> AppTheme.colors.success
                            showOriginal -> AppTheme.colors.warning
                            else -> AppTheme.colors.error
                        },
                        RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

enum class ImageLoadState {
    LOADING,
    SUCCESS,
    ERROR
}