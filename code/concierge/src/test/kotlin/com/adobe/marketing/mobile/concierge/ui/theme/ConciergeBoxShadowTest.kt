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

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ConciergeBoxShadowTest {

    private val sampleShadow = ConciergeBoxShadow(
        offsetX = 0.dp,
        offsetY = 2.dp,
        blurRadius = 8.dp,
        spreadRadius = 0.dp,
        color = Color(0, 0, 0, 0x26)
    )
    private val sampleShape = RoundedCornerShape(8.dp)

    private fun Modifier.elementClassNames(): List<String> =
        foldIn(mutableListOf()) { names, element -> names.apply { add(element.javaClass.simpleName) } }

    @Test
    fun fromTokens_parsedCssValue_mapsAllComponents() {
        val shadow = ConciergeBoxShadow.fromTokens(
            CSSValueConverter.parseBoxShadow("2px 4px 16px 3px #00000029")
        )

        assertNotNull(shadow)
        assertEquals(2.dp, shadow!!.offsetX)
        assertEquals(4.dp, shadow.offsetY)
        assertEquals(16.dp, shadow.blurRadius)
        assertEquals(3.dp, shadow.spreadRadius)
        assertEquals(Color(0, 0, 0, 0x29), shadow.color)
    }

    @Test
    fun fromTokens_negativeSpread_isPreserved() {
        val shadow = ConciergeBoxShadow.fromTokens(
            CSSValueConverter.parseBoxShadow("0 2px 8px -2px #000000")
        )

        assertEquals((-2).dp, shadow?.spreadRadius)
    }

    @Test
    fun fromTokens_null_returnsNull() {
        assertNull(ConciergeBoxShadow.fromTokens(null))
    }

    @Test
    fun fromTokens_none_returnsNull() {
        assertNull(ConciergeBoxShadow.fromTokens(CSSValueConverter.parseBoxShadow("none")))
    }

    @Test
    fun fromTokens_transparentColor_returnsNull() {
        assertNull(
            ConciergeBoxShadow.fromTokens(CSSValueConverter.parseBoxShadow("0 2px 8px 0 #00000000"))
        )
    }

    @Test
    fun fromTokens_unparseableColor_returnsNull() {
        assertNull(
            ConciergeBoxShadow.fromTokens(CSSValueConverter.parseBoxShadow("0 2px 8px 0 blue"))
        )
    }

    @Test
    fun fromTokens_missingColorKey_returnsNull() {
        assertNull(ConciergeBoxShadow.fromTokens(mapOf("blurRadius" to 8.0)))
    }

    @Test
    fun cssBlurToMaskFilterRadius_matchesCssGaussianSigma() {
        // CSS sigma = blur / 2 = 8; Skia sigma = 0.57735 * radius + 0.5.
        val radius = cssBlurToMaskFilterRadius(16f)

        assertNotNull(radius)
        assertEquals(8f, 0.57735f * radius!! + 0.5f, 0.001f)
    }

    @Test
    fun cssBlurToMaskFilterRadius_zeroOrTinyBlur_returnsNull() {
        assertNull(cssBlurToMaskFilterRadius(0f))
        assertNull(cssBlurToMaskFilterRadius(1f))
    }

    @Test
    fun conciergeBoxShadow_nullShadow_returnsReceiverUnchanged() {
        val base: Modifier = Modifier

        assertSame(base, base.conciergeBoxShadow(null, sampleShape))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun conciergeBoxShadow_api28Plus_appliesCustomDrawElement() {
        val names = Modifier.conciergeBoxShadow(sampleShadow, sampleShape).elementClassNames()

        assertTrue("expected a custom box-shadow element, got $names", names.any { it.contains("CssBoxShadow") })
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
    fun conciergeBoxShadow_belowApi28_withBlur_appliesElevationShadowFallback() {
        val names = Modifier.conciergeBoxShadow(sampleShadow, sampleShape).elementClassNames()

        assertFalse("expected the elevation fallback, not the custom draw", names.any { it.contains("CssBoxShadow") })
        assertTrue("expected the elevation fallback to apply a modifier, got $names", names.isNotEmpty())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
    fun conciergeBoxShadow_belowApi28_zeroBlur_returnsReceiverUnchanged() {
        val base: Modifier = Modifier

        assertSame(base, base.conciergeBoxShadow(sampleShadow.copy(blurRadius = 0.dp), sampleShape))
    }
}
