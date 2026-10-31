package com.synfusion.snipnote.ui.theme

import androidx.compose.ui.graphics.Color

object AuroraLight {
    val Primary = Color(0xFF7567F8)
    val PrimaryDark = Color(0xFF5A4BE0)
    val Secondary = Color(0xFF55C7E8)
    val Accent = Color(0xFFA5E6C8)
    val Background = Color(0xFFF6F7FC)
    val OnBackground = Color(0xFF20243A)
    val SecondaryText = Color(0xFF777D96)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFEDEFF8)
    val Outline = Color(0xFFD7DBEA)
    val Secure = Color(0xFF24A879)
    val Error = Color(0xFFB3261E)
    val Danger = Color(0xFFC4384F)
    val Pin = Color(0xFFF2A03D)
    val Favorite = Color(0xFFE85D8A)
}

object AuroraDark {
    val Primary = Color(0xFFA49AFF)
    val Secondary = Color(0xFF72D9F2)
    val Accent = Color(0xFF7FD9B4)
    val Background = Color(0xFF10111B)
    val Surface = Color(0xFF1D2030)
    val SurfaceVariant = Color(0xFF262A3D)
    val OnSurface = Color(0xFFF4F4FC)
    val SecondaryText = Color(0xFFA3A9C2)
    val Outline = Color(0xFF39405A)
    val Secure = Color(0xFF3FC79A)
    val Error = Color(0xFFFFB4AB)
    val Danger = Color(0xFFFF8A9B)
    val Pin = Color(0xFFFFC46B)
    val Favorite = Color(0xFFFF9BB8)
}

/** Category chip palette; kept in one place so the editor and category list agree. */
object CategoryPalette {
    val options: List<Long> = listOf(
        0xFF7567F8, 0xFF55C7E8, 0xFF7FC8A9, 0xFFFFC46B, 0xFFF49AC1,
        0xFF9BA8FF, 0xFF6FD3C7, 0xFFFF9E9E, 0xFFE2A0FF, 0xFF8AB6F9,
    )
}
