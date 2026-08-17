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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MicButtonLogicTest {

    @Test
    fun `pulsing background shows only while recording with the toggle enabled`() {
        assertTrue(shouldShowMicPulsingBackground(isRecording = true, pulsingBackgroundEnabled = true))
    }

    @Test
    fun `pulsing background is hidden while recording when the toggle is disabled`() {
        assertFalse(shouldShowMicPulsingBackground(isRecording = true, pulsingBackgroundEnabled = false))
    }

    @Test
    fun `pulsing background is hidden when not recording even if the toggle is enabled`() {
        assertFalse(shouldShowMicPulsingBackground(isRecording = false, pulsingBackgroundEnabled = true))
    }

    @Test
    fun `icon size is enlarged while recording without the pulsing background`() {
        val size = micIconSize(baseSize = 24.dp, isRecording = true, showPulsingBackground = false)
        assertEquals(24.dp * MIC_INNER_DISC_SCALE, size)
    }

    @Test
    fun `icon size stays at base size while recording with the pulsing background shown`() {
        val size = micIconSize(baseSize = 24.dp, isRecording = true, showPulsingBackground = true)
        assertEquals(24.dp, size)
    }

    @Test
    fun `icon size stays at base size when not recording`() {
        val size = micIconSize(baseSize = 24.dp, isRecording = false, showPulsingBackground = false)
        assertEquals(24.dp, size)
    }

    @Test
    fun `color is unchanged when enabled`() {
        assertEquals(Color.Red, dimIfDisabled(Color.Red, isEnabled = true))
    }

    @Test
    fun `color is dimmed to disabled alpha when disabled`() {
        assertEquals(Color.Red.copy(alpha = 0.38f), dimIfDisabled(Color.Red, isEnabled = false))
    }

    @Test
    fun `null color stays null when disabled`() {
        assertNull(dimIfDisabled(null, isEnabled = false))
    }
}
