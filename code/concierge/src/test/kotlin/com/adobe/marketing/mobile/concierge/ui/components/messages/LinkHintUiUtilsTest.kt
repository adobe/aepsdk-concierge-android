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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeLinkIconStyle
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [LinkHintUiUtils.resolveLinkIconColor].
 */
class LinkHintUiUtilsTest {

    private val linkColor = Color(0xFF0000FF)
    private val textColor = Color(0xFF123456)

    @Test
    fun resolveLinkIconColor_usesIconStyleColor_whenSet() {
        val result = LinkHintUiUtils.resolveLinkIconColor(
            iconStyle = ConciergeLinkIconStyle(color = "#00FF00"),
            linkColor = linkColor,
            textColor = textColor
        )

        assertEquals(Color(0xFF00FF00), result)
    }

    @Test
    fun resolveLinkIconColor_usesLinkColor_whenIconStyleColorMissing() {
        val result = LinkHintUiUtils.resolveLinkIconColor(
            iconStyle = ConciergeLinkIconStyle(size = 20f),
            linkColor = linkColor,
            textColor = textColor
        )

        assertEquals(linkColor, result)
    }

    @Test
    fun resolveLinkIconColor_usesLinkColor_whenIconStyleColorInvalid() {
        val result = LinkHintUiUtils.resolveLinkIconColor(
            iconStyle = ConciergeLinkIconStyle(color = "not-a-color"),
            linkColor = linkColor,
            textColor = textColor
        )

        assertEquals(linkColor, result)
    }

    @Test
    fun resolveLinkIconColor_usesTextColor_whenNoIconStyleOrLinkColor() {
        val result = LinkHintUiUtils.resolveLinkIconColor(
            iconStyle = null,
            linkColor = null,
            textColor = textColor
        )

        assertEquals(textColor, result)
    }
}
