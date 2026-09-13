package com.example.ui.theme

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * "Liquid glass" surface kit.
 *
 * Android cannot blur what is *behind* a composable below API 31, so instead of a real backdrop
 * blur these helpers fake refraction the way frosted glass actually reads: a translucent tinted
 * fill, a bright specular edge that catches light on the top-left, and a colour bleed from the
 * accent on the opposite corner. Stacked over [AppBackground]'s gradient this gives depth on
 * every supported API level.
 */

@Composable
private fun isLightScheme(): Boolean = MaterialTheme.colorScheme.background.luminance() > 0.5f

/** Full-screen ambient gradient the glass panels float on. */
@Composable
fun appBackgroundBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    return if (isLightScheme()) {
        Brush.verticalGradient(
            0f to lerp(scheme.background, scheme.primary, 0.14f),
            0.45f to scheme.background,
            1f to lerp(scheme.background, scheme.secondary, 0.08f)
        )
    } else {
        Brush.verticalGradient(
            0f to lerp(scheme.background, scheme.primary, 0.16f),
            0.30f to lerp(scheme.background, scheme.primary, 0.07f),
            0.70f to scheme.background,
            1f to lerp(scheme.background, Color.Black, 0.25f)
        )
    }
}

/**
 * Paints the app-wide gradient plus two soft accent halos, then hosts [content] on top.
 * Everything else in the app is drawn over this, so glass surfaces have something to refract.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val base = appBackgroundBrush()
    val accent = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val light = isLightScheme()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .drawBehind {
                val topCenter = Offset(size.width * 0.15f, size.height * 0.02f)
                val topRadius = size.width * 0.95f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = if (light) 0.18f else 0.20f),
                            Color.Transparent
                        ),
                        center = topCenter,
                        radius = topRadius
                    ),
                    radius = topRadius,
                    center = topCenter
                )

                val lowCenter = Offset(size.width * 0.95f, size.height * 0.62f)
                val lowRadius = size.width * 0.80f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondary.copy(alpha = if (light) 0.10f else 0.10f),
                            Color.Transparent
                        ),
                        center = lowCenter,
                        radius = lowRadius
                    ),
                    radius = lowRadius,
                    center = lowCenter
                )
            },
        content = content
    )
}

/**
 * Translucent pane fill. [strength] scales opacity — 1f for primary panels, ~0.6f for the small
 * chips and pills nested inside them so they read as a second layer of glass rather than a patch.
 */
@Composable
fun glassFill(
    tint: Color = MaterialTheme.colorScheme.primary,
    strength: Float = 1f
): Brush = if (isLightScheme()) {
    Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.92f * strength),
        0.55f to MaterialTheme.colorScheme.surface.copy(alpha = 0.78f * strength),
        1f to tint.copy(alpha = 0.10f * strength)
    )
} else {
    val pane = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primary, 0.05f)
    Brush.linearGradient(
        0f to lerp(pane, Color.White, 0.06f).copy(alpha = 0.76f * strength),
        0.30f to pane.copy(alpha = 0.70f * strength),
        0.75f to pane.copy(alpha = 0.60f * strength),
        1f to lerp(pane, tint, 0.12f).copy(alpha = 0.66f * strength)
    )
}

/** The specular rim. Bright where light would hit, fading to an accent bleed on the far corner. */
@Composable
fun glassBorder(strength: Float = 1f): Brush = if (isLightScheme()) {
    Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.95f * strength),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f * strength),
            Color.White.copy(alpha = 0.60f * strength)
        )
    )
} else {
    Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.42f * strength),
            Color.White.copy(alpha = 0.08f * strength),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f * strength)
        )
    )
}

/**
 * Soft drop shadow painted strictly *outside* [shape].
 *
 * The platform elevation shadow ([Modifier.shadow]) assumes an opaque caster: it only paints a
 * band around the edges and leaves the inset interior unshaded. Under a translucent glass fill
 * that band bleeds through as a hard-edged lighter rectangle inside every card. Drawing the
 * shadow ourselves and clipping the pane out of it means nothing is ever painted beneath the
 * glass, so the surface stays uniform and the shadow is a clean blurred halo.
 */
fun Modifier.softShadow(
    shape: Shape,
    elevation: Dp,
    shadowColor: Color
): Modifier = if (elevation <= 0.dp) this else drawWithCache {
    val elevationPx = elevation.toPx()
    val blurRadius = elevationPx * 1.75f
    val yOffset = elevationPx * 0.55f
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    val paint = Paint().apply {
        color = shadowColor
        isAntiAlias = true
        if (canBlur) {
            asFrameworkPaint().maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }
    }
    // Hardware canvases ignore mask filters before API 28, so fake the blur with concentric
    // strokes of decreasing alpha. Overlapping rings fall off naturally with distance.
    val fallbackRings = 8
    val fallbackPaints = if (canBlur) emptyList() else List(fallbackRings) { i ->
        Paint().apply {
            color = shadowColor.copy(alpha = shadowColor.alpha / fallbackRings)
            isAntiAlias = true
            style = PaintingStyle.Stroke
            strokeWidth = 2f * blurRadius * (i + 1) / fallbackRings
        }
    }

    onDrawBehind {
        clipPath(path, ClipOp.Difference) {
            translate(top = yOffset) {
                drawIntoCanvas { canvas ->
                    if (canBlur) {
                        canvas.drawPath(path, paint)
                    } else {
                        fallbackPaints.forEach { canvas.drawPath(path, it) }
                    }
                }
            }
        }
    }
}

/** Shadow tint that suits the current scheme: cool and light on light, an accent bleed on dark. */
@Composable
fun glassShadowColor(): Color = if (isLightScheme()) {
    lerp(Color.Black, MaterialTheme.colorScheme.primary, 0.35f).copy(alpha = 0.16f)
} else {
    lerp(Color.Black, MaterialTheme.colorScheme.primary, 0.15f).copy(alpha = 0.34f)
}

/** Applies shadow + fill + rim in the right draw order. */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = MaterialTheme.colorScheme.primary,
    strength: Float = 1f,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 0.dp
): Modifier {
    val fill = glassFill(tint, strength)
    val rim = glassBorder(strength)
    val shadow = glassShadowColor()
    return this
        .softShadow(shape, elevation, shadow)
        .clip(shape)
        .background(fill)
        .border(borderWidth, rim, shape)
}

/** Coloured halo used for the FAB and the selected nav pill. Shadow tinting needs API 28+. */
fun Modifier.accentGlow(
    color: Color,
    shape: Shape,
    elevation: Dp = 16.dp
): Modifier = this.softShadow(
    shape = shape,
    elevation = elevation,
    shadowColor = color.copy(alpha = 0.30f)
)

/** Convenience wrapper for the common "glass panel with padding" case. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = MaterialTheme.colorScheme.primary,
    strength: Float = 1f,
    elevation: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .liquidGlass(shape = shape, tint = tint, strength = strength, elevation = elevation)
            .padding(contentPadding),
        content = content
    )
}
