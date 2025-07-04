package com.ae.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ae.design_system.components.Text
import com.ae.design_system.theme.AppTheme
import androidx.compose.ui.Alignment
import com.ae.islamicimageviewer.IslamicImageViewer

@Composable
fun ImageViewerTestScreen() {
    val testImages = listOf(
        TestImage("https://images.pexels.com/photos/1130626/pexels-photo-1130626.jpeg", "Female Portrait 4"),
        TestImage("https://images.unsplash.com/photo-1524504388940-b1c1722653e1", "Woman Photo"),
        TestImage("https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&h=300&fit=crop", "Woman Portrait"),
        TestImage("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=300&fit=crop", "Man Portrait"),
        TestImage("https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=400&h=300&fit=crop", "Group Photo"),
        TestImage("https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=400&h=300&fit=crop", "Fashion Model"),
        TestImage("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&h=300&fit=crop", "Female Portrait"),
        TestImage("https://images.pexels.com/photos/774909/pexels-photo-774909.jpeg", "Female Portrait 1"),
        TestImage("https://images.pexels.com/photos/220453/pexels-photo-220453.jpeg", "Male Portrait 1"),
        TestImage("https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg", "Female Portrait 2"),
        TestImage("https://images.pexels.com/photos/697509/pexels-photo-697509.jpeg", "Male Portrait 2"),
        TestImage("https://images.pexels.com/photos/91227/pexels-photo-91227.jpeg", "Male Portrait 3")

    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(AppTheme.dimensions.spacingMedium),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.dimensions.spacingMedium)
        ) {
            Text(
                text = "Islamic Content Filter Test",
                style = AppTheme.typography.headlineMedium,
                color = AppTheme.colors.title
            )
            Text(
                text = "Images with female faces will be automatically blurred",
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.subtitle,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

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
    var imageLoadState by remember { mutableStateOf(ImageLoadState.LOADING) }
    var filterReason by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppTheme.dimensions.cornerRadiusMedium)),
        shape = RoundedCornerShape(AppTheme.dimensions.cornerRadiusMedium)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.surface)
        ) {
            IslamicImageViewer(
                url = testImage.url,
                contentDescription = testImage.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { error ->
                    imageLoadState = ImageLoadState.ERROR
                    filterReason = error.message ?: "Failed to load image"
                },
                onImageFiltered = { reason ->
                    imageLoadState = ImageLoadState.FILTERED
                    filterReason = reason
                },
                onImageApproved = {
                    imageLoadState = ImageLoadState.SUCCESS
                }
            )

            // Label overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        AppTheme.colors.surface.copy(alpha = 0.95f)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = testImage.label,
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.title
                    )

                    // Show filter status
                    when (imageLoadState) {
                        ImageLoadState.LOADING -> {
                            Text(
                                text = "Processing...",
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.subtitle,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        ImageLoadState.FILTERED -> {
                            Text(
                                text = "🚫 ${filterReason ?: "Filtered"}",
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.error,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        ImageLoadState.SUCCESS -> {
                            Text(
                                text = "✓ Approved",
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        ImageLoadState.ERROR -> {
                            Text(
                                text = "❌ Error",
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.error,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class ImageLoadState {
    LOADING,
    SUCCESS,
    FILTERED,
    ERROR
}