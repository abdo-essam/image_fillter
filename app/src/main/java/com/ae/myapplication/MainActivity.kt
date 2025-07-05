package com.ae.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ae.design_system.theme.AppTheme
import com.ae.movies.ImageViewerTestScreen
import com.ae.movies.presentation.TmdbImageViewerTestScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
             //  ImageViewerTestScreen()
                TmdbImageViewerTestScreen()
            }
        }
    }
}



