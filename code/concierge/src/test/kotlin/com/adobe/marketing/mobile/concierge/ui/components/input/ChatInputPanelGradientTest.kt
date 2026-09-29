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

package com.adobe.marketing.mobile.concierge.ui.components.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeGradient
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatInputPanelGradientTest {

    private val shape = RoundedCornerShape(8.dp)
    private val gradient = ConciergeGradient(startColor = Color.Green, endColor = Color.Blue)

    @Test
    fun focusedInput_prefersRenderableGradientAndUsesFocusWidth() {
        val border = resolveInputBorder(true, inputStyle(borderGradient = gradient))

        assertTrue(border is InputBorderStyle.Gradient)
        assertEquals(3.dp, border?.width)
        assertSame(gradient, (border as InputBorderStyle.Gradient).gradient)
        assertNotEquals(Modifier, border.toModifier())
    }

    @Test
    fun focusedInput_fallsBackToSolidColorWhenGradientIsNotRenderable() {
        val border = resolveInputBorder(
            true,
            inputStyle(
                focusBorderWidth = 2.dp,
                borderGradient = ConciergeGradient(Color.Transparent, Color.Blue)
            )
        )

        assertTrue(border is InputBorderStyle.Solid)
        assertEquals(2.dp, border?.width)
        assertEquals(Color.Red, (border as InputBorderStyle.Solid).color)
        assertNotEquals(Modifier, border.toModifier())
    }

    @Test
    fun unfocusedInput_usesOutlineGradientAndWidth() {
        val border = resolveInputBorder(false, inputStyle(borderGradient = gradient))

        assertTrue(border is InputBorderStyle.Gradient)
        assertEquals(1.dp, border?.width)
        assertSame(gradient, (border as InputBorderStyle.Gradient).gradient)
        assertNotEquals(Modifier, border.toModifier())
    }

    @Test
    fun unfocusedInput_fallsBackToSolidOutlineColor() {
        val border = resolveInputBorder(false, inputStyle(borderGradient = null))

        assertTrue(border is InputBorderStyle.Solid)
        assertEquals(1.dp, border?.width)
        assertEquals(Color.Black, (border as InputBorderStyle.Solid).color)
        assertNotEquals(Modifier, border.toModifier())
    }

    @Test
    fun inputWithoutEnabledBorder_returnsNoBorderModifier() {
        val border = resolveInputBorder(
            true,
            inputStyle(focusBorderWidth = 0.dp, borderWidth = 0.dp, borderGradient = gradient)
        )

        assertEquals(null, border)
        assertEquals(Modifier, border.toModifier())
    }

    @Test
    fun focusedInput_withoutGradientOrFocusColor_returnsNoBorder() {
        val border = resolveInputBorder(
            true,
            inputStyle(focusBorderColor = null, borderGradient = null)
        )

        assertEquals(null, border)
    }

    @Test
    fun unfocusedInput_withZeroOutlineWidth_returnsNoBorder() {
        val border = resolveInputBorder(
            false,
            inputStyle(borderWidth = 0.dp, borderGradient = gradient)
        )

        assertEquals(null, border)
    }

    @Test
    fun unfocusedInput_withoutRenderableGradientOrSolidColor_returnsNoBorder() {
        val border = resolveInputBorder(
            false,
            inputStyle(
                borderColor = null,
                borderGradient = ConciergeGradient(Color.Transparent, Color.Blue)
            )
        )

        assertEquals(null, border)
    }

    private fun inputStyle(
        focusBorderWidth: Dp = 3.dp,
        focusBorderColor: Color? = Color.Red,
        borderWidth: Dp = 1.dp,
        borderColor: Color? = Color.Black,
        borderGradient: ConciergeGradient? = gradient
    ) = ConciergeStyles.InputPanelStyle(
        outerShape = shape,
        innerShape = shape,
        outerPadding = 0.dp,
        innerPadding = PaddingValues(0.dp),
        leadingIconSpacing = 0.dp,
        backgroundColor = Color.Transparent,
        borderColor = borderColor,
        borderGradient = borderGradient,
        borderWidth = borderWidth,
        focusBorderColor = focusBorderColor,
        focusBorderWidth = focusBorderWidth,
        recordingBorderColors = emptyList(),
        recordingBorderAnimationDuration = 0,
        buttonSpacing = 0.dp,
        placeholderText = "",
        listeningPlaceholderText = ""
    )
}
