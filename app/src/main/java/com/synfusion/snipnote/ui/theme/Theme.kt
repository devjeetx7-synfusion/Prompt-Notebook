package com.synfusion.snipnote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.synfusion.snipnote.domain.model.ThemeMode

private val LightScheme = lightColorScheme(
    primary = AuroraLight.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF221C6B),
    secondary = AuroraLight.Secondary,
    onSecondary = Color(0xFF062B36),
    secondaryContainer = Color(0xFFD6F2FA),
    onSecondaryContainer = Color(0xFF083A47),
    tertiary = AuroraLight.Accent,
    onTertiary = Color(0xFF0D3A28),
    tertiaryContainer = Color(0xFFDDF7EA),
    onTertiaryContainer = Color(0xFF0C3B29),
    background = AuroraLight.Background,
    onBackground = AuroraLight.OnBackground,
    surface = AuroraLight.Surface,
    onSurface = AuroraLight.OnBackground,
    surfaceVariant = AuroraLight.SurfaceVariant,
    onSurfaceVariant = AuroraLight.SecondaryText,
    surfaceTint = AuroraLight.Primary,
    outline = AuroraLight.Outline,
    outlineVariant = Color(0xFFE6E9F4),
    error = AuroraLight.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0x99101420),
)

private val DarkScheme = darkColorScheme(
    primary = AuroraDark.Primary,
    onPrimary = Color(0xFF221C6B),
    primaryContainer = Color(0xFF3B3480),
    onPrimaryContainer = Color(0xFFE7E3FF),
    secondary = AuroraDark.Secondary,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF114B57),
    onSecondaryContainer = Color(0xFFCFF3FC),
    tertiary = AuroraDark.Accent,
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = Color(0xFF175A44),
    onTertiaryContainer = Color(0xFFC8F7E1),
    background = AuroraDark.Background,
    onBackground = AuroraDark.OnSurface,
    surface = AuroraDark.Surface,
    onSurface = AuroraDark.OnSurface,
    surfaceVariant = AuroraDark.SurfaceVariant,
    onSurfaceVariant = AuroraDark.SecondaryText,
    surfaceTint = AuroraDark.Primary,
    outline = AuroraDark.Outline,
    outlineVariant = Color(0xFF2C3247),
    error = AuroraDark.Error,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xCC05060C),
)

/** Extra Aurora colours that do not map onto Material 3 slots. */
data class AuroraExtras(
    val secure: Color,
    val favorite: Color,
    val pin: Color,
    val danger: Color,
    val glassFill: Color,
    val glassBorder: Color,
    val glassFillStrong: Color,
    val navFill: Color,
)

val LocalAuroraExtras = staticCompositionLocalOf {
    AuroraExtras(
        secure = AuroraLight.Secure,
        favorite = AuroraLight.Favorite,
        pin = AuroraLight.Pin,
        danger = AuroraLight.Danger,
        glassFill = Color.White.copy(alpha = 0.55f),
        glassBorder = Color.White.copy(alpha = 0.65f),
        glassFillStrong = Color.White.copy(alpha = 0.75f),
        navFill = Color.White.copy(alpha = 0.72f),
    )
}

private val LightExtras = AuroraExtras(
    secure = AuroraLight.Secure,
    favorite = AuroraLight.Favorite,
    pin = AuroraLight.Pin,
    danger = AuroraLight.Danger,
    glassFill = Color.White.copy(alpha = 0.58f),
    glassBorder = Color.White.copy(alpha = 0.70f),
    glassFillStrong = Color.White.copy(alpha = 0.82f),
    navFill = Color.White.copy(alpha = 0.78f),
)

private val DarkExtras = AuroraExtras(
    secure = AuroraDark.Secure,
    favorite = AuroraDark.Favorite,
    pin = AuroraDark.Pin,
    danger = AuroraDark.Danger,
    glassFill = Color(0xFF1D2030).copy(alpha = 0.62f),
    glassBorder = Color.White.copy(alpha = 0.10f),
    glassFillStrong = Color(0xFF232739).copy(alpha = 0.86f),
    navFill = Color(0xFF181B28).copy(alpha = 0.86f),
)

@Composable
fun SnipNoteTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = SnipNoteTypography,
    ) {
        CompositionLocalProvider(
            LocalAuroraExtras provides if (dark) DarkExtras else LightExtras,
            content = content,
        )
    }
}
