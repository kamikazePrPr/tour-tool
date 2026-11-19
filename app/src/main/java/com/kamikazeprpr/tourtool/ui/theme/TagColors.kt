package com.kamikazeprpr.tourtool.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 标签配色：每种标签一个固定底色 + 深一号的文字色。
 *
 * 取的是柔和低饱和的奶油调，跟整体画风一致；同一个 colorIndex 永远对应同一组颜色，
 * 换手机、换团都不变，看久了能靠颜色认出「这是孕妇」。
 */
object TagColors {

    data class Style(val bg: Color, val fg: Color)

    private val palette = listOf(
        Style(Color(0xFFFFEEDD), Color(0xFFB4651F)), // 暖橙
        Style(Color(0xFFE4EFFC), Color(0xFF2A63A8)), // 淡蓝
        Style(Color(0xFFFFE6EF), Color(0xFFB0315F)), // 淡粉
        Style(Color(0xFFE7F5E3), Color(0xFF3F7A33)), // 淡绿
        Style(Color(0xFFE0F4F2), Color(0xFF2C6E6B)), // 淡青
        Style(Color(0xFFEDE6FB), Color(0xFF5B3D9C)), // 淡紫
        Style(Color(0xFFFFF6D6), Color(0xFF8A6A12)), // 淡黄
        Style(Color(0xFFFFE9E0), Color(0xFFA84A2A)), // 淡珊瑚
        Style(Color(0xFFDFF0FA), Color(0xFF1F5E7A)), // 天蓝
        Style(Color(0xFFFBE4F0), Color(0xFF8F2F63)), // 淡玫
        Style(Color(0xFFEAF2DA), Color(0xFF557024)), // 苔绿
        Style(Color(0xFFE8EDF4), Color(0xFF42566E)), // 灰蓝
    )

    fun of(colorIndex: Int): Style = palette[colorIndex.mod(palette.size)]
}
