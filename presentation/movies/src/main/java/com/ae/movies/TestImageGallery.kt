/*
package com.ae.movies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ae.image_viewer_3.CustomImage
import com.ae.image_viewer_3.internal.ContentDetector

data class TestImage(val url: String, val label: String)

@Composable
fun TestImageGallery() {
    val testImages = listOf(
        TestImage("https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&h=300&fit=crop", "Woman"),
        TestImage("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=300&fit=crop", "Man"),
        TestImage("https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=400&h=300&fit=crop", "Group"),
        TestImage("https://picsum.photos/400/300?random=1", "Random 1"),
        TestImage("https://picsum.photos/400/300?random=2", "Random 2"),
        TestImage("https://picsum.photos/400/300?random=3", "Random 3"),
        TestImage("https://images.pexels.com/photos/774909/pexels-photo-774909.jpeg", "Test 1"),
        TestImage("https://images.pexels.com/photos/220453/pexels-photo-220453.jpeg", "Test 2"),
        TestImage("https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg", "Test 3"),
        TestImage("https://images.pexels.com/photos/91227/pexels-photo-91227.jpeg", "Test 4"),
        TestImage("https://images.pexels.com/photos/1130626/pexels-photo-1130626.jpeg", "Test 5"),
        TestImage("https://images.pexels.com/photos/697509/pexels-photo-697509.jpeg", "Test 6")
    )

    val blurredStates = remember { mutableStateListOf<Boolean>() }
    if (blurredStates.size != testImages.size) {
        blurredStates.clear()
        blurredStates.addAll(List(testImages.size) { false })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(testImages.withIndex().toList()) { (index, image) ->
            Column {
                Text(
                    text = "Image: ${image.label}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val isBlurred = customImage(
                    imageUrl = image.url,
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                )
                if (blurredStates[index] != isBlurred) {
                    blurredStates[index] = isBlurred
                }
                Text(
                    text = if (blurredStates[index]) "Status: Blurred (Inappropriate Content Detected)" else "Status: Not Blurred",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}*/
