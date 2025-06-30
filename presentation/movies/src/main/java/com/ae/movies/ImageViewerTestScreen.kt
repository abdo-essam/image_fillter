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
import com.ae.image_viewer.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush


@Composable
fun ImageViewerTestScreen() {
    val testImages = listOf(
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&h=300&fit=crop", // Woman
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=300&fit=crop", // Man
        "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=400&h=300&fit=crop", // Group
        "https://picsum.photos/400/300?random=1", // Random 1
        "https://picsum.photos/400/300?random=2", // Random 2
        "https://picsum.photos/400/300?random=3"  // Random 3
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        // Header
        Text(
            text = "Filter Test",
            style = AppTheme.typography.headlineMedium,
            color = AppTheme.colors.title,
            modifier = Modifier.padding(AppTheme.dimensions.spacingMedium)
        )

        // Image Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(AppTheme.dimensions.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spacingSmall)
        ) {
            items(testImages) { url ->
                GridImageItem(url = url)
            }
        }
    }
}

@Composable
private fun GridImageItem(url: String) {
    var filterReason by remember { mutableStateOf<String?>(null) }
    var showImage by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppTheme.dimensions.cornerRadiusMedium))
            .background(AppTheme.colors.surface)
            .clickable {
                if (filterReason != null) {
                    showImage = !showImage
                }
            }
    ) {
        if (filterReason != null && !showImage) {
            // Filtered state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AppTheme.colors.error.copy(alpha = 0.2f),
                                AppTheme.colors.error.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(AppTheme.dimensions.spacingMedium)
                ) {
                    // Icon with background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                AppTheme.colors.background,
                                RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🚫",
                            style = AppTheme.typography.titleLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(AppTheme.dimensions.spacingSmall))

                    // Parse and display info
                    val info = parseFilterReason(filterReason!!)

                    Text(
                        text = info.type,
                        style = AppTheme.typography.titleSmall,
                        color = AppTheme.colors.error
                    )

                    Text(
                        text = info.confidence,
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.error.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(AppTheme.dimensions.spacingSmall))

                    Text(
                        text = "Tap to view",
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.hint
                    )
                }
            }
        } else {
            // Show image
            Image(
                url = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onContentFiltered = { reason ->
                    filterReason = reason
                }
            )
        }

        // Status dot
        if (filterReason != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppTheme.dimensions.spacingSmall)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (showImage) AppTheme.colors.warning else AppTheme.colors.error,
                            RoundedCornerShape(8.dp)
                        )
                )
            }
        }
    }
}

// Data class for parsed info
data class FilterInfo(
    val type: String,
    val confidence: String,
    val details: List<String>
)

// Parse filter reason into structured data
private fun parseFilterReason(reason: String): FilterInfo {
    val type = when {
        reason.contains("Female", ignoreCase = true) -> "Female Detected"
        reason.contains("face", ignoreCase = true) -> "Face Detected"
        reason.contains("Person", ignoreCase = true) -> "Person Detected"
        reason.contains("alcohol", ignoreCase = true) -> "Alcohol"
        reason.contains("pork", ignoreCase = true) -> "Pork"
        else -> "Content Filtered"
    }

    // Extract all percentages
    val percentages = """$$(\d+)%$$""".toRegex()
        .findAll(reason)
        .map { it.groupValues[1].toInt() }
        .toList()

    val confidence = when {
        percentages.isEmpty() -> "High confidence"
        percentages.size == 1 -> "${percentages[0]}%"
        else -> "${percentages.maxOrNull()}% max"
    }

    // Extract detected items
    val details = reason.substringAfter(":")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    return FilterInfo(type, confidence, details)
}