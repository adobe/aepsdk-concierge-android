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
import org.junit.Test

class ConciergeStylesLogicTest {

    @Test
    fun `default recording icon color is onPrimary when the pulsing disc is shown`() {
        val color = defaultRecordingIconColor(
            pulsingBackgroundEnabled = true,
            onPrimary = Color.White,
            micColor = Color.Red
        )
        assertEquals(Color.White, color)
    }

    @Test
    fun `default recording icon color is the mic color when the pulsing disc is hidden`() {
        val color = defaultRecordingIconColor(
            pulsingBackgroundEnabled = false,
            onPrimary = Color.White,
            micColor = Color.Red
        )
        assertEquals(Color.Red, color)
    }
}
