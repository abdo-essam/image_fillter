package com.ae.design_system.components


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicText
import com.ae.design_system.theme.AppTheme

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.body,
    style: TextStyle = AppTheme.typography.bodyMedium,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    val mergedStyle = style.copy(
        color = color
    ).let { baseStyle ->
        if (textAlign != null) {
            baseStyle.copy(textAlign = textAlign)
        } else {
            baseStyle
        }
    }

    BasicText(
        text = text,
        modifier = modifier,
        style = mergedStyle,
        overflow = overflow,
        maxLines = maxLines
    )
}