package com.ae.image_viewer.internal


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ae.design_system.components.Text
import com.ae.design_system.theme.AppTheme

@Composable
internal fun ErrorPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(AppTheme.colors.surface)
            .clip(RoundedCornerShape(AppTheme.dimensions.cornerRadiusMedium)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(AppTheme.dimensions.spacingMedium)
        ) {
            // Error icon placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        AppTheme.colors.error.copy(alpha = 0.2f),
                        RoundedCornerShape(AppTheme.dimensions.cornerRadiusLarge)
                    )
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spacingMedium))

            Text(
                text = "Failed to load image",
                style = AppTheme.typography.titleMedium,
                color = AppTheme.colors.title
            )
        }
    }
}