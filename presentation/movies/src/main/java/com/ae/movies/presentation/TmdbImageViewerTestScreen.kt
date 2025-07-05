package com.ae.movies.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ae.design_system.components.Text
import com.ae.design_system.theme.AppTheme
import com.ae.islamicimageviewer.IslamicImageViewer
import com.ae.movies.domain.model.TestImage
import com.ae.movies.domain.model.ImageType
import com.ae.movies.viewmodel.TmdbTestViewModel
import org.koin.androidx.compose.koinViewModel


@Composable
fun TmdbImageViewerTestScreen(
    viewModel: TmdbTestViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.dimensions.spacingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TMDB Islamic Content Filter Test",
                        style = AppTheme.typography.headlineMedium,
                        color = AppTheme.colors.title
                    )
                    Text(
                        text = "Testing with real movie posters and actor profiles",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.subtitle,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (!uiState.isLoading) {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AppTheme.colors.primary
                        )
                    }
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.error,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.images.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No images found",
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.subtitle
                    )
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(AppTheme.dimensions.spacingMedium),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spacingSmall),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spacingSmall)
                ) {
                    items(uiState.images) { testImage ->
                        TmdbImageItem(testImage = testImage)
                    }
                }
            }
        }
    }
}

@Composable
private fun TmdbImageItem(testImage: TestImage) {
    var imageLoadState by remember { mutableStateOf(ImageLoadState.LOADING) }
    var filterReason by remember { mutableStateOf<String?>(null) }
    var hasResult by remember { mutableStateOf(false) }

    val backgroundColor = when (testImage.type) {
        ImageType.MOVIE_POSTER -> AppTheme.colors.primary.copy(alpha = 0.1f)
        ImageType.ACTOR_PROFILE -> AppTheme.colors.secondary.copy(alpha = 0.1f)
        ImageType.MOVIE_BACKDROP -> AppTheme.colors.tertiary.copy(alpha = 0.1f)
    }

    Card(
        modifier = Modifier
            .aspectRatio(if (testImage.type == ImageType.MOVIE_BACKDROP) 16f/9f else 2f/3f)
            .clip(RoundedCornerShape(AppTheme.dimensions.cornerRadiusMedium)),
        shape = RoundedCornerShape(AppTheme.dimensions.cornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            IslamicImageViewer(
                url = testImage.url,
                contentDescription = testImage.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { error ->
                    if (!hasResult) {
                        imageLoadState = ImageLoadState.ERROR
                        filterReason = error.message ?: "Failed to load image"
                        hasResult = true
                    }
                },
                onImageFiltered = { reason ->
                    if (!hasResult) {
                        imageLoadState = ImageLoadState.FILTERED
                        filterReason = reason
                        hasResult = true
                    }
                },
                onImageApproved = {
                    if (!hasResult) {
                        imageLoadState = ImageLoadState.SUCCESS
                        hasResult = true
                    }
                }
            )

            // Type badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        color = AppTheme.colors.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (testImage.type) {
                        ImageType.MOVIE_POSTER -> "Movie"
                        ImageType.ACTOR_PROFILE -> "Actor"
                        ImageType.MOVIE_BACKDROP -> "Backdrop"
                    },
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onSurface
                )
            }

            // Label and status overlay
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
                        color = AppTheme.colors.title,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )

                    // Show filter status
                    when (imageLoadState) {
                        ImageLoadState.LOADING -> {
                            if (!hasResult) {
                                Text(
                                    text = "Processing...",
                                    style = AppTheme.typography.labelSmall,
                                    color = AppTheme.colors.subtitle,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
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
                                color = AppTheme.colors.success,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        ImageLoadState.ERROR -> {
                            Text(
                                text = "❌ ${filterReason ?: "Error"}",
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.error,
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 1
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