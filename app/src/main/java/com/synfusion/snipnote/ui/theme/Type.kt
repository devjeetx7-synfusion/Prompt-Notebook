package com.synfusion.snipnote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val defaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun base(size: Int, lineHeight: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = spacing.sp,
    lineHeightStyle = defaultLineHeightStyle,
)

/** Rounded, airy scale that suits translucent cards. System font keeps the APK small and offline-safe. */
val SnipNoteTypography = Typography(
    displaySmall = base(34, 40, FontWeight.SemiBold, (-0.5)),
    headlineLarge = base(30, 36, FontWeight.SemiBold, (-0.5)),
    headlineMedium = base(26, 32, FontWeight.SemiBold, (-0.25)),
    headlineSmall = base(22, 28, FontWeight.SemiBold),
    titleLarge = base(20, 26, FontWeight.SemiBold),
    titleMedium = base(16, 22, FontWeight.Medium),
    titleSmall = base(14, 20, FontWeight.Medium),
    bodyLarge = base(16, 24, FontWeight.Normal),
    bodyMedium = base(14, 20, FontWeight.Normal),
    bodySmall = base(12, 18, FontWeight.Normal),
    labelLarge = base(14, 20, FontWeight.Medium),
    labelMedium = base(12, 16, FontWeight.Medium),
    labelSmall = base(11, 16, FontWeight.Medium, 0.3),
)
