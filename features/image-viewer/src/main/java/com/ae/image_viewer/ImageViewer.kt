package com.ae.image_viewer


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import com.ae.image_viewer.internal.ImageViewerInternal

/**
 * A custom Image composable that automatically filters inappropriate content
 * based on Islamic cultural guidelines.
 */
@Composable
fun Image(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = null,
    onContentFiltered: ((reason: String) -> Unit)? = null
) {
    ImageViewerInternal(
        url = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
        onContentFiltered = onContentFiltered
    )
}