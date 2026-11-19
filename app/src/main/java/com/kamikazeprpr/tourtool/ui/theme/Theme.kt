package com.kamikazeprpr.tourtool.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 配色：暖白底、深靛蓝主色、状态色只有绿/琥珀/红三档。
 * 对话框、卡片一律用白色，不要 Material3 默认的淡紫灰。
 */
private val TourColors = lightColorScheme(
    primary = DeepIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7F6),
    onPrimaryContainer = DeepIndigo,
    secondary = DeepIndigo,
    onSecondary = Color.White,
    background = WarmWhite,
    onBackground = TextMain,
    surface = Color.White,
    onSurface = TextMain,
    surfaceVariant = Color(0xFFF3F0E9),
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = Color(0xFF9A9A9A),
    outlineVariant = Color(0xFFD6D2C8),
    error = StateBad,
    onError = Color.White,
)

private val TourShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun TourToolTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TourColors,
        shapes = TourShapes,
        content = content,
    )
}
