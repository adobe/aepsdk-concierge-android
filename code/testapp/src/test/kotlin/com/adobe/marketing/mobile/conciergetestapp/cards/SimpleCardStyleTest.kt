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

package com.adobe.marketing.mobile.conciergetestapp.cards

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeLayout
import com.adobe.marketing.mobile.concierge.ui.theme.DarkConciergeColors
import org.junit.Assert.assertEquals
import org.junit.Test

/** The Kings palette, as the SDK resolves it from `themeDemo.json`. */
private val kings = DarkConciergeColors.copy(
    primary = Color(0xFF8921FE),
    onPrimary = Color.White,
    secondary = Color(0xFF2D3437),
    surface = Color(0xFF1C2123),
    onSurface = Color.White,
    outline = Color(0xFF2D3437)
)

class SimpleCardStyleTest {

    @Test
    fun `a theme that only sets the generic button colour still gets a branded button`() {
        // Regression: the card used to fall through to its hard-coded orange whenever a theme set
        // --cta-button-background-color but not --product-card-cta-button-background-color.
        val style = SimpleCardTheme.resolve(null, kings.copy(ctaButtonBackground = Color(0xFF8921FE)))
        assertEquals(Color(0xFF8921FE), style.ctaBackgroundColor)
    }

    @Test
    fun `the product card's own button colour still wins when the theme sets one`() {
        val style = SimpleCardTheme.resolve(
            null,
            kings.copy(ctaButtonBackground = Color(0xFF8921FE), productCardCtaButtonBackground = Color(0xFF5A2D81))
        )
        assertEquals(Color(0xFF5A2D81), style.ctaBackgroundColor)
    }

    @Test
    fun `a theme with no button colour at all keeps the built-in card's default`() {
        assertEquals(Color(0xFFBB5811), SimpleCardTheme.resolve(null, kings).ctaBackgroundColor)
    }

    @Test
    fun `a missing image reads as an empty slot, not as a hole in the card`() {
        // Deriving it from the card background would make the placeholder invisible.
        assertEquals(kings.secondary, SimpleCardTheme.resolve(null, kings).imagePlaceholderColor)
        assertEquals(
            Color(0xFF2D3437),
            SimpleCardTheme.resolve(null, kings, mapOf(SimpleCardKeys.IMAGE_PLACEHOLDER_COLOR to "#2D3437")).imagePlaceholderColor
        )
    }

    @Test
    fun `the badge is square until the theme rounds it`() {
        assertEquals(RoundedCornerShape(0.dp), SimpleCardTheme.resolve(null, kings).badgeShape)
        val rounded = SimpleCardTheme.resolve(
            null,
            kings,
            mapOf(SimpleCardKeys.BADGE_BORDER_RADIUS to "99px", SimpleCardKeys.BADGE_INSET to "12px")
        )
        assertEquals(RoundedCornerShape(99.dp), rounded.badgeShape)
        assertEquals(12.dp, rounded.badgeInset)
    }

    @Test
    fun `the meta line follows the price styling until it is overridden`() {
        val layout = ConciergeLayout(productCardPriceColor = "#FFFFFF", productCardPriceFontSize = 14.0, productCardPriceFontWeight = 400)
        val default = SimpleCardTheme.resolve(layout, kings)
        assertEquals(Color.White, default.detailColor)
        assertEquals(14.sp, default.detailFontSize)

        val overridden = SimpleCardTheme.resolve(
            layout,
            kings,
            mapOf(
                SimpleCardKeys.DETAIL_COLOR to "#ADADAD",
                SimpleCardKeys.DETAIL_FONT_SIZE to "12px",
                SimpleCardKeys.DETAIL_FONT_WEIGHT to "600"
            )
        )
        assertEquals(Color(0xFFADADAD), overridden.detailColor)
        assertEquals(12.sp, overridden.detailFontSize)
        assertEquals(600, overridden.detailFontWeight.weight)
    }

    @Test
    fun `theme values that aren't a colour or a length leave the default in place`() {
        val style = SimpleCardTheme.resolve(
            null,
            kings,
            mapOf(
                SimpleCardKeys.IMAGE_PLACEHOLDER_COLOR to "rebeccapurple",
                SimpleCardKeys.BADGE_INSET to "a lot",
                SimpleCardKeys.DISABLED_OPACITY to "very"
            )
        )
        assertEquals(kings.secondary, style.imagePlaceholderColor)
        assertEquals(0.dp, style.badgeInset)
        assertEquals(0.45f, style.disabledAlpha, 0f)
    }

    @Test
    fun `an opacity outside zero to one is clamped rather than ignored`() {
        assertEquals(1f, SimpleCardTheme.resolve(null, kings, mapOf(SimpleCardKeys.DISABLED_OPACITY to "4")).disabledAlpha, 0f)
        assertEquals(0f, SimpleCardTheme.resolve(null, kings, mapOf(SimpleCardKeys.DISABLED_OPACITY to "-1")).disabledAlpha, 0f)
    }

    @Test
    fun `an opacity that isn't a finite number leaves the default in place`() {
        for (raw in listOf("NaN", "Infinity", "-Infinity")) {
            assertEquals(raw, 0.45f, SimpleCardTheme.resolve(null, kings, mapOf(SimpleCardKeys.DISABLED_OPACITY to raw)).disabledAlpha, 0f)
        }
    }

    @Test
    fun `dimensions that aren't finite or are out of range leave the defaults in place`() {
        val style = SimpleCardTheme.resolve(
            null,
            kings,
            mapOf(
                SimpleCardKeys.DETAIL_FONT_SIZE to "Infinity",
                SimpleCardKeys.BADGE_INSET to "1e9px",
                SimpleCardKeys.IMAGE_BORDER_RADIUS to "NaN"
            )
        )
        assertEquals(14.sp, style.detailFontSize)
        assertEquals(0.dp, style.badgeInset)
        assertEquals(SimpleCardTheme.resolve(null, kings).imageShape, style.imageShape)
    }

    @Test
    fun `rgba theme colors are honoured like the built-in card`() {
        val style = SimpleCardTheme.resolve(
            ConciergeLayout(productCardBackgroundColor = "rgba(17, 34, 51, 0.5)"),
            kings,
            mapOf(SimpleCardKeys.IMAGE_PLACEHOLDER_COLOR to "rgb(1, 2, 3)")
        )
        assertEquals(Color(17, 34, 51, 127), style.cardBackgroundColor)
        assertEquals(Color(1, 2, 3), style.imagePlaceholderColor)
    }

    @Test
    fun `font weights past either end of 1 to 1000 are clamped instead of crashing`() {
        val heavy = ConciergeLayout(
            productCardBadgeFontWeight = 5000,
            productCardTitleFontWeight = 0,
            productCardSubtitleFontWeight = -10,
            productCardPriceFontWeight = 1001,
            productCardCtaButtonFontWeight = Int.MAX_VALUE
        )
        val style = SimpleCardTheme.resolve(heavy, kings)
        assertEquals(1000, style.badgeFontWeight.weight)
        assertEquals(1, style.titleFontWeight.weight)
        assertEquals(1, style.subtitleFontWeight.weight)
        assertEquals(1000, style.detailFontWeight.weight)
        assertEquals(1000, style.ctaFontWeight.weight)

        val custom = SimpleCardTheme.resolve(null, kings, mapOf(SimpleCardKeys.DETAIL_FONT_WEIGHT to "5000"))
        assertEquals(1000, custom.detailFontWeight.weight)
    }
}
