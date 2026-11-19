package com.kamikazeprpr.tourtool.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TourColors = lightColorScheme(
    primary = DeepIndigo,
    onPrimary = Color.White,
    secondary = DeepIndigo,
    onSecondary = Color.White,
    background = WarmWhite,
    onBackground = TextMain,
    surface = WarmWhite,
    onSurface = TextMain,
    error = StateBad,
)

@Composable
fun TourToolTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TourColors, content = content)
}
