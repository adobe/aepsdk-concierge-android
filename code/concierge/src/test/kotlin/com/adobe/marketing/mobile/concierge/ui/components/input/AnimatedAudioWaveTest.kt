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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeGradient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedAudioWaveTest {

    @Test
    fun `bar scale is at minimum floor at its phase trough`() {
        // time chosen so sin(time * 4.0) == -1.0 (trough)
        val scale = audioWaveBarScale(index = 0, timeSeconds = -Math.PI / 8.0)
        assertEquals(0.12f, scale, 0.001f)
    }

    @Test
    fun `bar scale reaches maximum at its phase peak`() {
        // time chosen so sin(time * 4.0) == 1.0 (peak)
        val scale = audioWaveBarScale(index = 0, timeSeconds = Math.PI / 8.0)
        assertEquals(1.0f, scale, 0.001f)
    }

    @Test
    fun `bar scale differs across bar indices at the same instant due to phase stagger`() {
        val bar0 = audioWaveBarScale(index = 0, timeSeconds = 0.3)
        val bar1 = audioWaveBarScale(index = 1, timeSeconds = 0.3)
        assertTrue(bar0 != bar1)
    }

    @Test
    fun `bar scale is fully static at the floor when audio level is zero`() {
        val atPeakPhase = audioWaveBarScale(index = 0, timeSeconds = Math.PI / 8.0, audioLevel = 0f)
        val atTroughPhase = audioWaveBarScale(index = 0, timeSeconds = -Math.PI / 8.0, audioLevel = 0f)
        assertEquals(0.12f, atPeakPhase, 0.001f)
        assertEquals(0.12f, atTroughPhase, 0.001f)
    }

    @Test
    fun `bar scale amplitude scales linearly with a mid-range audio level`() {
        val scale = audioWaveBarScale(index = 0, timeSeconds = Math.PI / 8.0, audioLevel = 0.5f)
        // At peak phase: floor + (0.88 * 0.5)
        assertEquals(0.56f, scale, 0.001f)
    }

    @Test
    fun `bar scale reaches full range by default when audio level is not specified`() {
        val scale = audioWaveBarScale(index = 0, timeSeconds = Math.PI / 8.0)
        assertEquals(1.0f, scale, 0.001f)
    }

    @Test
    fun `brush falls back to solid color when no gradient is set`() {
        val brush = audioWaveBarBrush(color = Color.Red, gradient = null, size = Size(100f, 100f))
        assertEquals(SolidColor(Color.Red), brush)
    }

    @Test
    fun `brush falls back to solid color when the gradient is not renderable`() {
        val gradient = ConciergeGradient(startColor = Color.Cyan, endColor = Color.Transparent)
        val brush = audioWaveBarBrush(color = Color.Red, gradient = gradient, size = Size(100f, 100f))
        assertEquals(SolidColor(Color.Red), brush)
    }

    @Test
    fun `brush is the gradient's linear-gradient brush when it is renderable`() {
        val gradient = ConciergeGradient(startColor = Color.Cyan, endColor = Color.Black, angle = 180f)
        val size = Size(20f, 40f)
        val brush = audioWaveBarBrush(color = Color.Red, gradient = gradient, size = size)
        val expected: Brush = Brush.linearGradient(
            colors = listOf(Color.Cyan, Color.Black),
            start = Offset(10f, 0f),
            end = Offset(10f, 40f)
        )
        assertEquals(expected, brush)
        assertTrue(brush !is SolidColor)
    }
}
