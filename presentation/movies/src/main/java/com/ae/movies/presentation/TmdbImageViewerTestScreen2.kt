package com.ae.movies.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ae.design_system.components.Text
import com.ae.design_system.theme.AppTheme
import com.ae.haram_blur.HaramBlurImageProcessor
import com.ae.haram_blur.HaramImageViewer
import com.ae.haram_blur.IslamicImageViewerConfig
import com.ae.movies.domain.model.ImageType
import com.ae.movies.domain.model.TestImage
import com.ae.movies.presentation.state.PaginationEvent
import com.ae.movies.viewmodel.TmdbTestViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmdbImageViewerTestScreen(
    viewModel: TmdbTestViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    // Detect when scrolled to bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisibleItem >= totalItems - 4 && // Load when 4 items from bottom
                    !uiState.isLoading &&
                    !uiState.endReached &&
                    uiState.error == null
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.onEvent(PaginationEvent.LoadNextPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        // Header
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "TMDB Islamic Filter Test",
                        style = AppTheme.typography.headlineMedium,
                        color = AppTheme.colors.title
                    )
                    Text(
                        text = "${uiState.items.size} images loaded (Page ${uiState.page})",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.subtitle
                    )
                }
            },
            actions = {
                if (!uiState.isRefreshing) {
                    IconButton(onClick = { viewModel.onEvent(PaginationEvent.Refresh) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AppTheme.colors.primary
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AppTheme.colors.background
            )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isRefreshing -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && uiState.items.isEmpty() -> {
                    ErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.onEvent(PaginationEvent.Refresh) }
                    )
                }

                uiState.items.isEmpty() && !uiState.isLoading -> {
                    EmptyState()
                }

                else -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(AppTheme.dimensions.spacingMedium),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spacingSmall),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spacingSmall)
                    ) {
                        items(
                            items = uiState.items,
                            key = { "${it.url}_${it.label}" }
                        ) { testImage ->
                            TmdbImageItem(
                                testImage = testImage,
                                onRemove = {
                                    viewModel.onEvent(PaginationEvent.RemoveItem(testImage))
                                }
                            )
                        }

                        // Loading indicator at bottom
                        if (uiState.isLoading) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        // Error at bottom
                        if (uiState.error != null && uiState.items.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = AppTheme.colors.error.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = uiState.error!!,
                                            style = AppTheme.typography.bodySmall,
                                            color = AppTheme.colors.error,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = { viewModel.onEvent(PaginationEvent.LoadNextPage) }
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }

                        // End reached message
                        if (uiState.endReached) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🎬 No more images to load",
                                        style = AppTheme.typography.bodyMedium,
                                        color = AppTheme.colors.subtitle
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Error: $error",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyState() {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TmdbImageItem(
    testImage: TestImage,
    onRemove: () -> Unit = {}
) {
    var imageLoadState by remember { mutableStateOf(ImageLoadState.LOADING) }
    var filterReason by remember { mutableStateOf<String?>(null) }
    var detectionDetails by remember { mutableStateOf<HaramBlurImageProcessor.DetectionDetails?>(null) }
    var hasResult by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Configure HaramBlur settings
    val imageConfig = remember {
        IslamicImageViewerConfig(
            enableFilter = true,
            blurStrength = 25f,
            allowClickToReveal = true,
            showBlurIcon = true,
            blurSettings = HaramBlurImageProcessor.BlurSettings(
                blurFemales = true,
                blurMales = false,
                useNsfwDetection = true,
                nsfwThreshold = 0.3f,
                genderConfidenceThreshold = 0.6f,
                strictMode = false
            )
        )
    }

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
            modifier = Modifier.fillMaxSize()
        ) {
            HaramImageViewer(
                model = testImage.url,
                contentDescription = testImage.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                config = imageConfig,
                onError = { error ->
                    if (!hasResult) {
                        imageLoadState = ImageLoadState.ERROR
                        filterReason = error.message ?: "Failed to load image"
                        hasResult = true
                    }
                },
                onImageFiltered = { reason, details ->
                    if (!hasResult) {
                        imageLoadState = ImageLoadState.FILTERED
                        filterReason = reason
                        detectionDetails = details
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

            // Type badge with menu
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                AssistChip(
                    onClick = { showMenu = true },
                    label = {
                        Text(
                            text = when (testImage.type) {
                                ImageType.MOVIE_POSTER -> "Movie"
                                ImageType.ACTOR_PROFILE -> "Actor"
                                ImageType.MOVIE_BACKDROP -> "Backdrop"
                            },
                            style = AppTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(24.dp)
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }

            // Enhanced status overlay with detection details
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

                    // Show enhanced filter status with details
                    when (imageLoadState) {
                        ImageLoadState.LOADING -> {
                            if (!hasResult) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                        ImageLoadState.FILTERED -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🚫 ${filterReason ?: "Filtered"}",
                                    style = AppTheme.typography.labelSmall,
                                    color = AppTheme.colors.error,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                // Show detection details if available
                                detectionDetails?.let { details ->
                                    Text(
                                        text = buildString {
                                            if (details.facesDetected > 0) {
                                                append("Faces: ${details.facesDetected}")
                                                if (details.femalesDetected > 0) {
                                                    append(" (${details.femalesDetected}F")
                                                }
                                                if (details.malesDetected > 0) {
                                                    if (details.femalesDetected > 0) append("/")
                                                    append("${details.malesDetected}M")
                                                }
                                                if (details.femalesDetected > 0 || details.malesDetected > 0) {
                                                    append(")")
                                                }
                                            }
                                            if (details.isNsfw) {
                                                if (details.facesDetected > 0) append(" | ")
                                                append("NSFW: ${(details.nsfwScore * 100).toInt()}%")
                                            }
                                        },
                                        style = AppTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = AppTheme.colors.subtitle,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                if (imageConfig.allowClickToReveal) {
                                    Text(
                                        text = "Tap to reveal",
                                        style = AppTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = AppTheme.colors.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
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