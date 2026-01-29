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

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CSSValueConverterTest {

    // ========== parseColor() Tests ==========

    @Test
    fun `parseColor handles 6-digit hex color`() {
        // Given
        val cssValue = "#FF5733"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(255, (color.red * 255).toInt())
        assertEquals(87, (color.green * 255).toInt())
        assertEquals(51, (color.blue * 255).toInt())
        assertEquals(255, (color.alpha * 255).toInt())
    }

    @Test
    fun `parseColor handles 8-digit hex color with alpha`() {
        // Given
        val cssValue = "#FF573380"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(255, (color.red * 255).toInt())
        assertEquals(87, (color.green * 255).toInt())
        assertEquals(51, (color.blue * 255).toInt())
        assertEquals(128, (color.alpha * 255).toInt())
    }

    @Test
    fun `parseColor handles rgb() format`() {
        // Given
        val cssValue = "rgb(100, 150, 200)"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(100, (color.red * 255).toInt())
        assertEquals(150, (color.green * 255).toInt())
        assertEquals(200, (color.blue * 255).toInt())
        assertEquals(255, (color.alpha * 255).toInt())
    }

    @Test
    fun `parseColor handles rgba() format`() {
        // Given
        val cssValue = "rgba(100, 150, 200, 0.5)"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(100, (color.red * 255).toInt())
        assertEquals(150, (color.green * 255).toInt())
        assertEquals(200, (color.blue * 255).toInt())
        // Alpha is 0.5 * 255 = ~127
        assertTrue((color.alpha * 255).toInt() in 125..130)
    }

    @Test
    fun `parseColor handles hex with lowercase letters`() {
        // Given
        val cssValue = "#aabbcc"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(170, (color.red * 255).toInt())
        assertEquals(187, (color.green * 255).toInt())
        assertEquals(204, (color.blue * 255).toInt())
    }

    @Test
    fun `parseColor handles black color`() {
        // Given
        val cssValue = "#000000"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(0, (color.red * 255).toInt())
        assertEquals(0, (color.green * 255).toInt())
        assertEquals(0, (color.blue * 255).toInt())
    }

    @Test
    fun `parseColor handles white color`() {
        // Given
        val cssValue = "#FFFFFF"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(255, (color.red * 255).toInt())
        assertEquals(255, (color.green * 255).toInt())
        assertEquals(255, (color.blue * 255).toInt())
    }

    @Test
    fun `parseColor returns Unspecified for invalid hex length`() {
        // Given
        val cssValue = "#FF"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(Color.Unspecified, color)
    }

    @Test
    fun `parseColor returns Unspecified for invalid rgb values`() {
        // Given
        val cssValue = "rgb(invalid, values, here)"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(Color.Unspecified, color)
    }

    @Test
    fun `parseColor returns Unspecified for unknown format`() {
        // Given
        val cssValue = "not-a-color"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(Color.Unspecified, color)
    }

    @Test
    fun `parseColor handles rgb with extra whitespace`() {
        // Given
        val cssValue = "rgb( 100 , 150 , 200 )"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(100, (color.red * 255).toInt())
        assertEquals(150, (color.green * 255).toInt())
        assertEquals(200, (color.blue * 255).toInt())
    }

    @Test
    fun `parseColor handles rgba with alpha 0`() {
        // Given
        val cssValue = "rgba(255, 0, 0, 0.0)"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(255, (color.red * 255).toInt())
        assertEquals(0, (color.alpha * 255).toInt())
    }

    @Test
    fun `parseColor handles rgba with alpha 1`() {
        // Given
        val cssValue = "rgba(0, 255, 0, 1.0)"

        // When
        val color = CSSValueConverter.parseColor(cssValue)

        // Then
        assertEquals(255, (color.green * 255).toInt())
        assertEquals(255, (color.alpha * 255).toInt())
    }

    // ========== parsePxValue() Tests ==========

    @Test
    fun `parsePxValue handles pixel value with px suffix`() {
        // Given
        val cssValue = "16px"

        // When
        val result = CSSValueConverter.parsePxValue(cssValue)

        // Then
        assertEquals(16.0, result!!, 0.001)
    }

    @Test
    fun `parsePxValue handles decimal pixel value`() {
        // Given
        val cssValue = "16.5px"

        // When
        val result = CSSValueConverter.parsePxValue(cssValue)

        // Then
        assertEquals(16.5, result!!, 0.001)
    }

    @Test
    fun `parsePxValue handles numeric value without px`() {
        // Given
        val cssValue = "32"

        // When
        val result = CSSValueConverter.parsePxValue(cssValue)

        // Then
        assertEquals(32.0, result!!, 0.001)
    }

    @Test
    fun `parsePxValue handles value with extra whitespace`() {
        // Given
        val cssValue = "  24px  "

        // When
        val result = CSSValueConverter.parsePxValue(cssValue)

        // Then
        assertEquals(24.0, result!!, 0.001)
    }

    @Test
    fun `parsePxValue returns null for invalid value`() {
        // Given
        val cssValue = "not-a-number"

        // When
        val result = CSSValueConverter.parsePxValue(cssValue)

        // Then
        assertNull(result)
    }

    @Test
    fun `parsePxValue handles zero value`() {
        // Given
        val cssValue = "0px"

        // When
        val result = CSSValueConverter.parsePxValue(cssValue)

        // Then
        assertEquals(0.0, result!!, 0.001)
    }

    @Test
    fun `parsePxValue handles negative value`() {
        // Given
        val cssValue = "-10px"

        // When
        val result = CSSValueConverter.parsePxValue(cssValue)

        // Then
        assertEquals(-10.0, result!!, 0.001)
    }

    // ========== parseFontFamily() Tests ==========

    @Test
    fun `parseFontFamily removes double quotes`() {
        // Given
        val cssValue = "\"Helvetica\""

        // When
        val result = CSSValueConverter.parseFontFamily(cssValue)

        // Then
        assertEquals("Helvetica", result)
    }

    @Test
    fun `parseFontFamily removes single quotes`() {
        // Given
        val cssValue = "'Arial'"

        // When
        val result = CSSValueConverter.parseFontFamily(cssValue)

        // Then
        assertEquals("Arial", result)
    }

    @Test
    fun `parseFontFamily handles unquoted value`() {
        // Given
        val cssValue = "Georgia"

        // When
        val result = CSSValueConverter.parseFontFamily(cssValue)

        // Then
        assertEquals("Georgia", result)
    }

    @Test
    fun `parseFontFamily trims whitespace`() {
        // Given
        val cssValue = "  \"Times New Roman\"  "

        // When
        val result = CSSValueConverter.parseFontFamily(cssValue)

        // Then
        assertEquals("Times New Roman", result)
    }

    // ========== parseLineHeight() Tests ==========

    @Test
    fun `parseLineHeight handles px value`() {
        // Given
        val cssValue = "24px"

        // When
        val result = CSSValueConverter.parseLineHeight(cssValue)

        // Then
        assertEquals(24.0, result, 0.001)
    }

    @Test
    fun `parseLineHeight handles unitless multiplier`() {
        // Given
        val cssValue = "1.5"

        // When
        val result = CSSValueConverter.parseLineHeight(cssValue)

        // Then
        assertEquals(1.5, result, 0.001)
    }

    @Test
    fun `parseLineHeight returns default for invalid value`() {
        // Given
        val cssValue = "invalid"

        // When
        val result = CSSValueConverter.parseLineHeight(cssValue)

        // Then
        assertEquals(1.5, result, 0.001)
    }

    @Test
    fun `parseLineHeight handles decimal multiplier`() {
        // Given
        val cssValue = "2.5"

        // When
        val result = CSSValueConverter.parseLineHeight(cssValue)

        // Then
        assertEquals(2.5, result, 0.001)
    }

    // ========== parsePadding() Tests ==========

    @Test
    fun `parsePadding handles single value - all sides equal`() {
        // Given
        val cssValue = "10px"

        // When
        val result = CSSValueConverter.parsePadding(cssValue)

        // Then
        assertEquals(listOf(10.0, 10.0, 10.0, 10.0), result)
    }

    @Test
    fun `parsePadding handles two values - vertical horizontal`() {
        // Given
        val cssValue = "10px 20px"

        // When
        val result = CSSValueConverter.parsePadding(cssValue)

        // Then
        assertEquals(listOf(10.0, 20.0, 10.0, 20.0), result)
    }

    @Test
    fun `parsePadding handles three values - top horizontal bottom`() {
        // Given
        val cssValue = "10px 20px 30px"

        // When
        val result = CSSValueConverter.parsePadding(cssValue)

        // Then
        assertEquals(listOf(10.0, 20.0, 30.0, 20.0), result)
    }

    @Test
    fun `parsePadding handles four values - top right bottom left`() {
        // Given
        val cssValue = "10px 20px 30px 40px"

        // When
        val result = CSSValueConverter.parsePadding(cssValue)

        // Then
        assertEquals(listOf(10.0, 20.0, 30.0, 40.0), result)
    }

    @Test
    fun `parsePadding returns zeros for invalid values`() {
        // Given
        val cssValue = "invalid"

        // When
        val result = CSSValueConverter.parsePadding(cssValue)

        // Then
        assertEquals(listOf(0.0, 0.0, 0.0, 0.0), result)
    }

    @Test
    fun `parsePadding handles decimal values`() {
        // Given
        val cssValue = "10.5px 20.75px"

        // When
        val result = CSSValueConverter.parsePadding(cssValue)

        // Then
        assertEquals(listOf(10.5, 20.75, 10.5, 20.75), result)
    }

    // ========== parseWidth() Tests ==========

    @Test
    fun `parseWidth handles percentage value`() {
        // Given
        val cssValue = "50%"

        // When
        val result = CSSValueConverter.parseWidth(cssValue)

        // Then
        assertEquals(0.5, result!!, 0.001)
    }

    @Test
    fun `parseWidth handles px value`() {
        // Given
        val cssValue = "320px"

        // When
        val result = CSSValueConverter.parseWidth(cssValue)

        // Then
        assertEquals(320.0, result!!, 0.001)
    }

    @Test
    fun `parseWidth handles numeric value`() {
        // Given
        val cssValue = "500"

        // When
        val result = CSSValueConverter.parseWidth(cssValue)

        // Then
        assertEquals(500.0, result!!, 0.001)
    }

    @Test
    fun `parseWidth returns null for invalid value`() {
        // Given
        val cssValue = "invalid"

        // When
        val result = CSSValueConverter.parseWidth(cssValue)

        // Then
        assertNull(result)
    }

    @Test
    fun `parseWidth handles 100 percent`() {
        // Given
        val cssValue = "100%"

        // When
        val result = CSSValueConverter.parseWidth(cssValue)

        // Then
        assertEquals(1.0, result!!, 0.001)
    }

    @Test
    fun `parseWidth handles decimal percentage`() {
        // Given
        val cssValue = "33.33%"

        // When
        val result = CSSValueConverter.parseWidth(cssValue)

        // Then
        assertEquals(0.3333, result!!, 0.001)
    }

    // ========== parseBoxShadow() Tests ==========

    @Test
    fun `parseBoxShadow handles none value`() {
        // Given
        val cssValue = "none"

        // When
        val result = CSSValueConverter.parseBoxShadow(cssValue)

        // Then
        assertNull(result)
    }

    @Test
    fun `parseBoxShadow handles basic shadow with 3 values`() {
        // Given
        val cssValue = "0px 2px 8px"

        // When
        val result = CSSValueConverter.parseBoxShadow(cssValue)

        // Then
        assertNotNull(result)
        assertEquals(0.0, result!!["offsetX"])
        assertEquals(2.0, result["offsetY"])
        assertEquals(8.0, result["blurRadius"])
        assertEquals(0.0, result["spreadRadius"])
    }

    @Test
    fun `parseBoxShadow handles shadow with rgba color`() {
        // Given
        val cssValue = "0px 2px 8px rgba(0, 0, 0, 0.1)"

        // When
        val result = CSSValueConverter.parseBoxShadow(cssValue)

        // Then
        assertNotNull(result)
        assertEquals(0.0, result!!["offsetX"])
        assertEquals(2.0, result["offsetY"])
        assertEquals(8.0, result["blurRadius"])
        assertNotNull(result["color"])
    }

    @Test
    fun `parseBoxShadow handles shadow with spread radius`() {
        // Given
        val cssValue = "0px 2px 8px 4px"

        // When
        val result = CSSValueConverter.parseBoxShadow(cssValue)

        // Then
        assertNotNull(result)
        assertEquals(4.0, result!!["spreadRadius"])
    }

    @Test
    fun `parseBoxShadow handles shadow with spread and color`() {
        // Given
        val cssValue = "2px 4px 6px 1px #FF5733"

        // When
        val result = CSSValueConverter.parseBoxShadow(cssValue)

        // Then
        assertNotNull(result)
        assertEquals(2.0, result!!["offsetX"])
        assertEquals(4.0, result["offsetY"])
        assertEquals(6.0, result["blurRadius"])
        assertEquals(1.0, result["spreadRadius"])
        assertNotNull(result["color"])
    }

    @Test
    fun `parseBoxShadow returns null for invalid format`() {
        // Given
        val cssValue = "invalid"

        // When
        val result = CSSValueConverter.parseBoxShadow(cssValue)

        // Then
        assertNull(result)
    }

    @Test
    fun `parseBoxShadow handles negative offset values`() {
        // Given
        val cssValue = "-2px -4px 8px"

        // When
        val result = CSSValueConverter.parseBoxShadow(cssValue)

        // Then
        assertNotNull(result)
        assertEquals(-2.0, result!!["offsetX"])
        assertEquals(-4.0, result["offsetY"])
    }

    // ========== parseFontWeight() Tests ==========

    @Test
    fun `parseFontWeight handles normal keyword`() {
        // Given
        val cssValue = "normal"

        // When
        val result = CSSValueConverter.parseFontWeight(cssValue)

        // Then
        assertEquals(400, result)
    }

    @Test
    fun `parseFontWeight handles bold keyword`() {
        // Given
        val cssValue = "bold"

        // When
        val result = CSSValueConverter.parseFontWeight(cssValue)

        // Then
        assertEquals(700, result)
    }

    @Test
    fun `parseFontWeight handles lighter keyword`() {
        // Given
        val cssValue = "lighter"

        // When
        val result = CSSValueConverter.parseFontWeight(cssValue)

        // Then
        assertEquals(300, result)
    }

    @Test
    fun `parseFontWeight handles bolder keyword`() {
        // Given
        val cssValue = "bolder"

        // When
        val result = CSSValueConverter.parseFontWeight(cssValue)

        // Then
        assertEquals(700, result)
    }

    @Test
    fun `parseFontWeight handles numeric value`() {
        // Given
        val cssValue = "600"

        // When
        val result = CSSValueConverter.parseFontWeight(cssValue)

        // Then
        assertEquals(600, result)
    }

    @Test
    fun `parseFontWeight returns default for invalid value`() {
        // Given
        val cssValue = "invalid"

        // When
        val result = CSSValueConverter.parseFontWeight(cssValue)

        // Then
        assertEquals(400, result)
    }

    @Test
    fun `parseFontWeight handles case insensitive keywords`() {
        // Given
        val cssValues = listOf("BOLD", "Normal", "LIGHTER")

        // When & Then
        assertEquals(700, CSSValueConverter.parseFontWeight(cssValues[0]))
        assertEquals(400, CSSValueConverter.parseFontWeight(cssValues[1]))
        assertEquals(300, CSSValueConverter.parseFontWeight(cssValues[2]))
    }

    // ========== parseOrder() Tests ==========

    @Test
    fun `parseOrder handles numeric value`() {
        // Given
        val cssValue = "5"

        // When
        val result = CSSValueConverter.parseOrder(cssValue)

        // Then
        assertEquals(5, result)
    }

    @Test
    fun `parseOrder handles zero`() {
        // Given
        val cssValue = "0"

        // When
        val result = CSSValueConverter.parseOrder(cssValue)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `parseOrder handles negative value`() {
        // Given
        val cssValue = "-1"

        // When
        val result = CSSValueConverter.parseOrder(cssValue)

        // Then
        assertEquals(-1, result)
    }

    @Test
    fun `parseOrder returns default for invalid value`() {
        // Given
        val cssValue = "invalid"

        // When
        val result = CSSValueConverter.parseOrder(cssValue)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `parseOrder handles value with whitespace`() {
        // Given
        val cssValue = "  10  "

        // When
        val result = CSSValueConverter.parseOrder(cssValue)

        // Then
        assertEquals(10, result)
    }

    // ========== Color.toHexString() Extension Tests ==========

    @Test
    fun `Color toHexString converts opaque color to 6-digit hex`() {
        // Given
        val color = Color(255, 87, 51)

        // When
        val hex = color.toHexString()

        // Then
        assertEquals("#FF5733", hex)
    }

    @Test
    fun `Color toHexString converts color with alpha to 8-digit hex`() {
        // Given
        val color = Color(255, 87, 51, 128)

        // When
        val hex = color.toHexString()

        // Then
        assertEquals("#FF573380", hex)
    }

    @Test
    fun `Color toHexString handles black color`() {
        // Given
        val color = Color.Black

        // When
        val hex = color.toHexString()

        // Then
        assertEquals("#000000", hex)
    }

    @Test
    fun `Color toHexString handles white color`() {
        // Given
        val color = Color.White

        // When
        val hex = color.toHexString()

        // Then
        assertEquals("#FFFFFF", hex)
    }

    @Test
    fun `Color toHexString handles transparent color`() {
        // Given
        val color = Color.Transparent

        // When
        val hex = color.toHexString()

        // Then
        // Transparent is RGBA(0, 0, 0, 0)
        assertEquals("#00000000", hex)
    }
}
