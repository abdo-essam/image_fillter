package com.ae.islamicimageviewer


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * Public API for Islamic Image Viewer Library
 */
object IslamicImageViewer {

    /**
     * Creates an Islamic-compliant image viewer that automatically filters
     * potentially inappropriate content.
     *
     * @param imageUrl The URL or resource identifier for the image
     * @param modifier Modifier to be applied to the image
     * @param contentDescription Content description for accessibility
     * @param contentScale How the image should be scaled
     * @param enableContentFiltering Whether to enable automatic content filtering
     * @param onContentFiltered Callback when content is filtered (true when filtered)
     */
    @Composable
    fun Image(
        imageUrl: Any?,
        modifier: Modifier = Modifier,
        contentDescription: String? = null,
        contentScale: ContentScale = ContentScale.Crop,
        enableContentFiltering: Boolean = true,
        onContentFiltered: ((Boolean) -> Unit)? = null
    ) {
        IslamicImage(
            imageUrl = imageUrl,
            modifier = modifier,
            contentDescription = contentDescription,
            contentScale = contentScale,
            enableContentFiltering = enableContentFiltering,
            onContentFiltered = onContentFiltered
        )
    }
}