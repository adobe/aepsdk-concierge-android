/*
 * Copyright 2026 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge.ui.theme

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Typed form of a CSS `box-shadow` token as parsed by [CSSValueConverter.parseBoxShadow].
 * Lengths are CSS px, rendered 1:1 as dp like the SDK's other px-based tokens.
 */
@Immutable
internal data class ConciergeBoxShadow(
    val offsetX: Dp,
    val offsetY: Dp,
    val blurRadius: Dp,
    val spreadRadius: Dp,
    val color: Color
) {
    companion object {
        /**
         * Converts a parsed box-shadow token map into a [ConciergeBoxShadow]. Returns null when the
         * token is unset, `"none"` (which parses to null), or has an invisible color.
         */
        fun fromTokens(tokens: Map<String, Any>?): ConciergeBoxShadow? {
            if (tokens == null) return null
            val color = tokens["color"] as? Color ?: return null
            if (!color.isSpecified || color.alpha <= 0f) return null
            fun length(key: String): Dp = ((tokens[key] as? Number)?.toFloat() ?: 0f).dp
            return ConciergeBoxShadow(
                offsetX = length("offsetX"),
                offsetY = length("offsetY"),
                blurRadius = length("blurRadius").coerceAtLeast(0.dp),
                spreadRadius = length("spreadRadius"),
                color = color
            )
        }
    }
}

/**
 * Converts a CSS blur radius (px) to the radius [BlurMaskFilter] expects. CSS defines the blur as
 * a Gaussian with sigma = blur / 2, while Skia derives sigma = 0.57735 * radius + 0.5 from the
 * mask filter radius. Returns null when no blur should be applied (BlurMaskFilter rejects 0).
 */
internal fun cssBlurToMaskFilterRadius(cssBlurPx: Float): Float? {
    val sigma = cssBlurPx / 2f
    val radius = (sigma - 0.5f) / 0.57735f
    return radius.takeIf { it > 0f }
}

/**
 * Draws a CSS-style outer `box-shadow` behind the composable, honoring offset, blur, spread and
 * color. Like CSS, the shadow is clipped out from under the box itself, so it never shows through
 * a translucent background.
 *
 * On API 28+ the shadow is rendered with a blurred native paint. Older releases don't support
 * hardware-accelerated blur mask filters or `clipOutPath`, so they fall back to an elevation-based
 * [Modifier.shadow] using the blur radius as elevation. Offset and spread are ignored there, and
 * so is the color, since custom shadow colors are only supported on API 28+.
 */
internal fun Modifier.conciergeBoxShadow(shadow: ConciergeBoxShadow?, shape: Shape): Modifier = when {
    shadow == null -> this
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> this.then(CssBoxShadowElement(shadow, shape))
    shadow.blurRadius > 0.dp -> this.shadow(
        elevation = shadow.blurRadius,
        shape = shape,
        clip = false,
        ambientColor = shadow.color,
        spotColor = shadow.color
    )
    else -> this
}

/**
 * Compares [shadow] and [shape] by value, so recompositions with an equal style keep the existing
 * node and its cached paths instead of rebuilding them.
 */
@RequiresApi(Build.VERSION_CODES.P)
private data class CssBoxShadowElement(
    val shadow: ConciergeBoxShadow,
    val shape: Shape
) : ModifierNodeElement<CssBoxShadowNode>() {
    override fun create() = CssBoxShadowNode(shadow, shape)

    override fun update(node: CssBoxShadowNode) = node.update(shadow, shape)
}

@RequiresApi(Build.VERSION_CODES.P)
private class CssBoxShadowNode(
    private var shadow: ConciergeBoxShadow,
    private var shape: Shape
) : Modifier.Node(), DrawModifierNode {
    private var cachedSize: Size? = null
    private var cachedLayoutDirection: LayoutDirection? = null
    private var cachedDensity = 0f
    private var shadowPath: android.graphics.Path? = null
    private var boxPath: android.graphics.Path? = null
    private var paint: android.graphics.Paint? = null

    fun update(shadow: ConciergeBoxShadow, shape: Shape) {
        this.shadow = shadow
        this.shape = shape
        cachedSize = null
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        if (size != cachedSize || layoutDirection != cachedLayoutDirection || density != cachedDensity) {
            buildCache()
        }
        val shadowPath = shadowPath
        val boxPath = boxPath
        val paint = paint
        if (shadowPath != null && boxPath != null && paint != null) {
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                nativeCanvas.save()
                nativeCanvas.clipOutPath(boxPath)
                nativeCanvas.drawPath(shadowPath, paint)
                nativeCanvas.restore()
            }
        }
        drawContent()
    }

    private fun DrawScope.buildCache() {
        val spreadPx = shadow.spreadRadius.toPx()
        val shadowSize = Size(
            (size.width + 2 * spreadPx).coerceAtLeast(0f),
            (size.height + 2 * spreadPx).coerceAtLeast(0f)
        )
        shadowPath = Path().apply {
            addOutline(shape.createOutline(shadowSize, layoutDirection, this@buildCache))
            translate(Offset(shadow.offsetX.toPx() - spreadPx, shadow.offsetY.toPx() - spreadPx))
        }.asAndroidPath()
        boxPath = Path().apply {
            addOutline(shape.createOutline(size, layoutDirection, this@buildCache))
        }.asAndroidPath()
        paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = shadow.color.toArgb()
            cssBlurToMaskFilterRadius(shadow.blurRadius.toPx())?.let {
                maskFilter = BlurMaskFilter(it, BlurMaskFilter.Blur.NORMAL)
            }
        }
        cachedSize = size
        cachedLayoutDirection = layoutDirection
        cachedDensity = density
    }
}
