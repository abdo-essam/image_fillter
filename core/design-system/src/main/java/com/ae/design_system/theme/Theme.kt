package com.ae.design_system.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current

    val dimensions: Dimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette
    val typography = Typography
    val dimensions = Dimensions()

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTypography provides typography,
        LocalDimensions provides dimensions,
        content = content
    )
}

val LocalColors = staticCompositionLocalOf { DarkColorPalette }