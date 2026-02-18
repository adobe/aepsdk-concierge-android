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

import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for ConciergeStyles that require a Composable context (e.g. disclaimerStyle).
 */
class ConciergeStylesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun disclaimerStyle_returnsStyleWithExpectedDefaults() {
        var style: ConciergeStyles.DisclaimerStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.disclaimerStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(8.dp, style!!.padding)
        assertEquals(TextDecoration.Underline, style!!.linkTextDecoration)
    }

    @Test
    fun disclaimerStyle_withThemeTypography_appliesFontSizeAndWeight() {
        var style: ConciergeStyles.DisclaimerStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(
                typography = ConciergeTypographyConfig(
                    disclaimerFontSize = 14.0,
                    disclaimerFontWeight = 700
                )
            ),
            tokens = null
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.disclaimerStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(14.0, style!!.textStyle.fontSize.value.toDouble(), 0.1)
        assertEquals(700, style!!.textStyle.fontWeight?.weight ?: 0)
    }
}
