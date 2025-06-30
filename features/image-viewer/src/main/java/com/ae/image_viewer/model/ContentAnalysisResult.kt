package com.ae.image_viewer.model

sealed class ContentAnalysisResult {
    data object Appropriate : ContentAnalysisResult()
    data class Inappropriate(val reason: String) : ContentAnalysisResult()
    data class Error(val message: String) : ContentAnalysisResult()
}