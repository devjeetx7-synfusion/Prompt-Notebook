package com.synfusion.snipnote.ui.theme

import androidx.compose.ui.graphics.Color

object AuroraLight {
    val Primary = Color(0xFF7567F8)
    val PrimaryDeep = Color(0xFF5A4BE0)
    val Secondary = Color(0xFF55C7E8)
    val Accent = Color(0xFFA5E6C8)
    val Pink = Color(0xFFF3A6D0)
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
    val Favorite = Color(0xFFF5B21F)
}

object AuroraDark {
    val Primary = Color(0xFFA49AFF)
    val Secondary = Color(0xFF72D9F2)
    val Accent = Color(0xFF7FD9B4)
    val Pink = Color(0xFFE79AC8)
    val Background = Color(0xFF0C0E1A)
    val Surface = Color(0xFF1A1E30)
    val SurfaceVariant = Color(0xFF242A3F)
    val OnSurface = Color(0xFFF4F4FC)
    val SecondaryText = Color(0xFFA6ACC6)
    val Outline = Color(0xFF39405A)
    val Secure = Color(0xFF3FC79A)
    val Error = Color(0xFFFFB4AB)
    val Danger = Color(0xFFFF8A9B)
    val Pin = Color(0xFFFFC46B)
    val Favorite = Color(0xFFFFCB5C)
}

/** Translucent glass tokens, resolved per theme. */
data class GlassTokens(
    val cardFill: Color,
    val cardBorder: Color,
    val cardFillStrong: Color,
    val navFill: Color,
    val navBorder: Color,
    val shadow: Color,
    val chipFill: Color,
)

val LightGlass = GlassTokens(
    cardFill = Color.White.copy(alpha = 0.62f),
    cardBorder = Color.White.copy(alpha = 0.85f),
    cardFillStrong = Color.White.copy(alpha = 0.80f),
    navFill = Color.White.copy(alpha = 0.74f),
    navBorder = Color.White.copy(alpha = 0.90f),
    shadow = Color(0xFF3B3F6B).copy(alpha = 0.10f),
    chipFill = Color.White.copy(alpha = 0.72f),
)

val DarkGlass = GlassTokens(
    cardFill = Color(0xFF1A1E30).copy(alpha = 0.66f),
    cardBorder = Color.White.copy(alpha = 0.09f),
    cardFillStrong = Color(0xFF20253A).copy(alpha = 0.86f),
    navFill = Color(0xFF161A29).copy(alpha = 0.86f),
    navBorder = Color.White.copy(alpha = 0.10f),
    shadow = Color.Black.copy(alpha = 0.45f),
    chipFill = Color.White.copy(alpha = 0.07f),
)

/** Category palette used by the category editor. */
object CategoryPalette {
    val options: List<Long> = listOf(
        0xFF7567F8, 0xFF55C7E8, 0xFF7FC8A9, 0xFFFFC46B, 0xFFF49AC1,
        0xFF9BA8FF, 0xFF6FD3C7, 0xFFFF9E9E, 0xFFE2A0FF, 0xFF8AB6F9,
    )
}

/** Per content-type accent used by list rows, chips and the editor. */
data class TypeStyle(val tint: Color, val container: Color)

fun typeStyle(type: com.synfusion.snipnote.domain.model.NoteType, dark: Boolean): TypeStyle =
    when (type) {
        com.synfusion.snipnote.domain.model.NoteType.PROMPT -> if (dark) {
            TypeStyle(Color(0xFFB4A8FF), Color(0xFF3A3470).copy(alpha = 0.55f))
        } else {
            TypeStyle(Color(0xFF6F5BF0), Color(0xFFEDE9FF))
        }
        com.synfusion.snipnote.domain.model.NoteType.CODE -> if (dark) {
            TypeStyle(Color(0xFF7FDCF5), Color(0xFF123B4A).copy(alpha = 0.55f))
        } else {
            TypeStyle(Color(0xFF1E93BC), Color(0xFFDFF3FB))
        }
        com.synfusion.snipnote.domain.model.NoteType.LINK -> if (dark) {
            TypeStyle(Color(0xFFFFB27A), Color(0xFF4A2C1A).copy(alpha = 0.55f))
        } else {
            TypeStyle(Color(0xFFD9762F), Color(0xFFFDE7D3))
        }
        com.synfusion.snipnote.domain.model.NoteType.NOTE -> if (dark) {
            TypeStyle(Color(0xFF9DBCFF), Color(0xFF1C2C50).copy(alpha = 0.55f))
        } else {
            TypeStyle(Color(0xFF4A72D8), Color(0xFFE3EBFD))
        }
    }
