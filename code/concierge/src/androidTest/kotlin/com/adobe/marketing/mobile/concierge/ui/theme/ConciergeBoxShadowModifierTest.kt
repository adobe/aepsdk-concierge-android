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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ConciergeBoxShadowModifierTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val shape = RoundedCornerShape(8.dp)

    /**
     * Drives the [conciergeBoxShadow] draw node through a recomposition with a changed shadow so the
     * node's cached paths and paint are rebuilt (the `update` path), in addition to the initial draw.
     */
    @Test
    fun conciergeBoxShadow_shadowValueChanges_recomposesWithoutCrashing() {
        var shadow by mutableStateOf(
            ConciergeBoxShadow(
                offsetX = 0.dp,
                offsetY = 2.dp,
                blurRadius = 8.dp,
                spreadRadius = 0.dp,
                color = Color(0, 0, 0, 0x26)
            )
        )

        composeTestRule.setContent {
            Box(
                Modifier
                    .size(120.dp)
                    .conciergeBoxShadow(shadow, shape)
                    .testTag("boxShadowTarget")
            )
        }
        composeTestRule.onNodeWithTag("boxShadowTarget").assertIsDisplayed()

        shadow = shadow.copy(offsetY = 12.dp, spreadRadius = 2.dp)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("boxShadowTarget").assertIsDisplayed()
    }
}
