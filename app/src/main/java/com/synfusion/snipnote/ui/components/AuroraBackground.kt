package com.synfusion.snipnote.ui.components

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.synfusion.snipnote.ui.theme.AuroraDark
import com.synfusion.snipnote.ui.theme.AuroraLight

/**
 * Three soft radial gradients painted once per screen. Real Gaussian blur is only applied on
 * API 31+ devices that are not low-RAM, and only to this single backdrop layer – never per card.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // Derived from the active scheme rather than the system, so a forced Light/Dark theme
    // setting is respected by the backdrop as well.
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    val allowBlur = rememberBlurSupported(context)
    val base = if (dark) AuroraDark.Background else AuroraLight.Background
    val blobA = if (dark) AuroraDark.Primary.copy(alpha = 0.30f) else AuroraLight.Primary.copy(alpha = 0.22f)
    val blobB = if (dark) AuroraDark.Secondary.copy(alpha = 0.22f) else AuroraLight.Secondary.copy(alpha = 0.20f)
    val blobC = if (dark) AuroraDark.Accent.copy(alpha = 0.16f) else AuroraLight.Accent.copy(alpha = 0.28f)

    Box(modifier = modifier.fillMaxSize(), content = {
        Box(modifier = Modifier.fillMaxSize().let { if (allowBlur) it.blur(48.dp) else it }) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = base)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobA, Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.06f),
                        radius = size.maxDimension * 0.55f,
                    ),
                    radius = size.maxDimension * 0.55f,
                    center = Offset(size.width * 0.12f, size.height * 0.06f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobB, Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.20f),
                        radius = size.maxDimension * 0.45f,
                    ),
                    radius = size.maxDimension * 0.45f,
                    center = Offset(size.width * 0.95f, size.height * 0.20f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobC, Color.Transparent),
                        center = Offset(size.width * 0.55f, size.height * 0.95f),
                        radius = size.maxDimension * 0.6f,
                    ),
                    radius = size.maxDimension * 0.6f,
                    center = Offset(size.width * 0.55f, size.height * 0.95f),
                )
            }
        }
        content()
    })
}

@Composable
private fun rememberBlurSupported(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return activityManager?.isLowRamDevice != true
}

/** Convenience wrapper used by every screen so the aurora layer behaves identically. */
@Composable
fun AuroraScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier, content = { AuroraBackground { content() } })
}
