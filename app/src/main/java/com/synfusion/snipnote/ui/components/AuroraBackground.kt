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
import androidx.compose.runtime.remember
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
 * Soft lavender / sky / pink wash painted once per screen.
 *
 * A single Gaussian blur is applied to this backdrop layer only on API 31+ devices that are not
 * low-RAM; every other device gets the same gradients without the blur cost. No per-card blur.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // Derived from the active scheme so a forced light/dark setting is honoured.
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    val allowBlur = rememberBlurAllowed(context)

    val base = if (dark) Color(0xFF0C0E1A) else Color(0xFFF6F7FC)
    val lavender = if (dark) AuroraDark.Primary.copy(alpha = 0.26f) else AuroraLight.Primary.copy(alpha = 0.20f)
    val sky = if (dark) AuroraDark.Secondary.copy(alpha = 0.18f) else AuroraLight.Secondary.copy(alpha = 0.22f)
    val pink = if (dark) AuroraDark.Pink.copy(alpha = 0.14f) else AuroraLight.Pink.copy(alpha = 0.24f)
    val mint = if (dark) AuroraDark.Accent.copy(alpha = 0.10f) else AuroraLight.Accent.copy(alpha = 0.18f)

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().let { if (allowBlur) it.blur(56.dp) else it }) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = base)
                fun blob(color: Color, x: Float, y: Float, radiusFactor: Float) {
                    val radius = size.maxDimension * radiusFactor
                    val center = Offset(size.width * x, size.height * y)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color, Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
                blob(lavender, 0.10f, 0.02f, 0.60f)
                blob(sky, 0.98f, 0.16f, 0.48f)
                blob(pink, 0.86f, 0.92f, 0.52f)
                blob(mint, 0.20f, 1.00f, 0.42f)
            }
        }
        content()
    }
}

@Composable
fun AuroraScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) { AuroraBackground { content() } }
}

@Composable
private fun rememberBlurAllowed(context: Context): Boolean = remember(context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@remember false
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    activityManager?.isLowRamDevice != true
}
