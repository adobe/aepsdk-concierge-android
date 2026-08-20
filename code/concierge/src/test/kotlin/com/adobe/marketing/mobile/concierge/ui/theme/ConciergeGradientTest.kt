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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConciergeGradientTest {

    // ========== isRenderable Tests ==========

    @Test
    fun `isRenderable is true when both colors are real`() {
        val gradient = ConciergeGradient(startColor = Color.Red, endColor = Color.Blue)
        assertTrue(gradient.isRenderable)
    }

    @Test
    fun `isRenderable is false when start color is transparent`() {
        val gradient = ConciergeGradient(startColor = Color.Transparent, endColor = Color.Blue)
        assertFalse(gradient.isRenderable)
    }

    @Test
    fun `isRenderable is false when end color is transparent`() {
        val gradient = ConciergeGradient(startColor = Color.Red, endColor = Color.Transparent)
        assertFalse(gradient.isRenderable)
    }

    @Test
    fun `default angle is 180`() {
        val gradient = ConciergeGradient(startColor = Color.Red, endColor = Color.Blue)
        assertEquals(180f, gradient.angle, 0.001f)
    }

    // ========== unitPoints Tests ==========

    @Test
    fun `unitPoints at angle 0 points up`() {
        val gradient = ConciergeGradient(startColor = Color.Red, endColor = Color.Blue, angle = 0f)
        val (start, end) = gradient.unitPoints
        assertEquals(0.5f, start.x, 0.001f)
        assertEquals(1.0f, start.y, 0.001f)
        assertEquals(0.5f, end.x, 0.001f)
        assertEquals(0.0f, end.y, 0.001f)
    }

    @Test
    fun `unitPoints at angle 90 points right`() {
        val gradient = ConciergeGradient(startColor = Color.Red, endColor = Color.Blue, angle = 90f)
        val (start, end) = gradient.unitPoints
        assertEquals(0.0f, start.x, 0.001f)
        assertEquals(0.5f, start.y, 0.001f)
        assertEquals(1.0f, end.x, 0.001f)
        assertEquals(0.5f, end.y, 0.001f)
    }

    @Test
    fun `unitPoints at angle 180 points down`() {
        val gradient = ConciergeGradient(startColor = Color.Red, endColor = Color.Blue, angle = 180f)
        val (start, end) = gradient.unitPoints
        assertEquals(0.5f, start.x, 0.001f)
        assertEquals(0.0f, start.y, 0.001f)
        assertEquals(0.5f, end.x, 0.001f)
        assertEquals(1.0f, end.y, 0.001f)
    }

    @Test
    fun `unitPoints at angle 270 points left`() {
        val gradient = ConciergeGradient(startColor = Color.Red, endColor = Color.Blue, angle = 270f)
        val (start, end) = gradient.unitPoints
        assertEquals(1.0f, start.x, 0.001f)
        assertEquals(0.5f, start.y, 0.001f)
        assertEquals(0.0f, end.x, 0.001f)
        assertEquals(0.5f, end.y, 0.001f)
    }

    // ========== toBrush Tests ==========

    @Test
    fun `toBrush resolves unit points against the actual draw size`() {
        val gradient = ConciergeGradient(startColor = Color.Red, endColor = Color.Blue, angle = 90f)
        val size = Size(200f, 100f)
        val brush = gradient.toBrush(size)
        val expected: Brush = Brush.linearGradient(
            colors = listOf(Color.Red, Color.Blue),
            start = Offset(0f, 50f),
            end = Offset(200f, 50f)
        )
        assertEquals(expected, brush)
    }

    @Test
    fun `toBrush at angle 180 spans the full height regardless of width`() {
        val gradient = ConciergeGradient(startColor = Color.Red, endColor = Color.Blue, angle = 180f)
        val size = Size(300f, 40f)
        val brush = gradient.toBrush(size)
        val expected: Brush = Brush.linearGradient(
            colors = listOf(Color.Red, Color.Blue),
            start = Offset(150f, 0f),
            end = Offset(150f, 40f)
        )
        assertEquals(expected, brush)
    }
}
