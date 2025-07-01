package com.ae.design_system.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable

@Immutable
data class AppColors(
    val onSurface: Color,
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val background: Color,
    val backgroundLow: Color,
    val onPrimary: Color,
    val onPrimaryHint: Color,
    val iconBackground: Color,
    val iconBackgroundLow: Color,
    val title: Color,
    val body: Color,
    val hint: Color,
    val stroke: Color,
    val disable: Color,
    val error: Color,
    val warning: Color,
    val success: Color,
    val successVariant: Color,
    val isLight: Boolean
)

val DarkColorPalette = AppColors(
    onSurface = Color(0xFFFFFFFF),
    primary = Color(0xFFCE6A4F),
    primaryVariant = Color(0xFFFFBEAE),
    secondary = Color(0xFF4B0418),
    surface = Color(0xFF000000),
    surfaceHigh = Color(0xFF1D1D1D),
    background = Color(0xFF000000),
    backgroundLow = Color(0xFF0F0F0F),
    onPrimary = Color(0xFFFFFFFF),
    onPrimaryHint = Color(0xB3FFFFFF),
    iconBackground = Color(0xCC000000),
    iconBackgroundLow = Color(0x1FFFFFFF),
    title = Color(0xE6FFFFFF),
    body = Color(0x99FFFFFF),
    hint = Color(0x59FFFFFF),
    stroke = Color(0x14FFFFFF),
    disable = Color(0xFF1C1B1B),
    error = Color(0xFFF75562),
    warning = Color(0xFFCFC567),
    success = Color(0xFF167A4D),
    successVariant = Color(0xFF1D1F1E),
    isLight = false
)

val LightColorPalette = AppColors(
    onSurface = Color(0xFF131F1F),
    primary = Color(0xFFF77653),
    primaryVariant = Color(0xFFFFBEAE),
    secondary = Color(0xFF880718),
    surface = Color(0xFFFCF6F5),
    surfaceHigh = Color(0xFFFFFFFF),
    background = Color(0xFFFCF6F5),
    backgroundLow = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onPrimaryHint = Color(0x59FFFFFF),
    iconBackground = Color(0xCC000000),
    iconBackgroundLow = Color(0x1F000000),
    title = Color(0xE6131F1F),
    body = Color(0x99131F1F),
    hint = Color(0x59131F1F),
    stroke = Color(0x14131F1F),
    disable = Color(0xFFCC8FBC),
    error = Color(0xFFF75562),
    warning = Color(0xFFCFC74D),
    success = Color(0xFF167A4D),
    successVariant = Color(0xFFC4EBDC),
    isLight = true
)