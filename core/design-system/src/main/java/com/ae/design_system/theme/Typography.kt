package com.ae.design_system.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ae.design_system.R

// Define IBM Sans Plex font family
val IBMSansPlexFamily = FontFamily(
    Font(R.font.ibm_sans_plex_regular, FontWeight.Normal),
    Font(R.font.ibm_sans_plex_medium, FontWeight.Medium),
    Font(R.font.ibm_sans_plex_semibold, FontWeight.SemiBold)
)

@Immutable
data class AppTypography(
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

val Typography = AppTypography(
    headlineLarge = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 42.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    titleSmall = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelMedium = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelSmall = TextStyle(
        fontFamily = IBMSansPlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp
    )
)

val LocalTypography = staticCompositionLocalOf { Typography }