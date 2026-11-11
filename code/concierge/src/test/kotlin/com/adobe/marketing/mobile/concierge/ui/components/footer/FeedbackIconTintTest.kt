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

package com.adobe.marketing.mobile.concierge.ui.components.footer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackIconTintTest {
    private val style = ConciergeStyles.FeedbackButtonsStyle(
        buttonSize = 32.dp,
        iconSize = 16.dp,
        spacing = 4.dp,
        iconColor = Color.Green.copy(alpha = 0.2f),
        selectedIconColor = Color.Magenta.copy(alpha = 0.7f),
        dimmedIconColor = Color(0x66808080),
        helpfulLabelText = "",
        helpfulLabelStyle = TextStyle.Default,
        helpfulLabelColor = Color.Black
    )

    @Test
    fun `no vote preserves text color and alpha on both thumbs`() {
        assertEquals(style.iconColor, style.iconTintFor(FeedbackState.None, isThumbUp = true))
        assertEquals(style.iconColor, style.iconTintFor(FeedbackState.None, isThumbUp = false))
    }

    @Test
    fun `positive vote selects up and dims down`() {
        assertEquals(style.selectedIconColor, style.iconTintFor(FeedbackState.Positive, isThumbUp = true))
        assertEquals(style.dimmedIconColor, style.iconTintFor(FeedbackState.Positive, isThumbUp = false))
    }

    @Test
    fun `negative vote dims up and selects down`() {
        assertEquals(style.dimmedIconColor, style.iconTintFor(FeedbackState.Negative, isThumbUp = true))
        assertEquals(style.selectedIconColor, style.iconTintFor(FeedbackState.Negative, isThumbUp = false))
    }
}
