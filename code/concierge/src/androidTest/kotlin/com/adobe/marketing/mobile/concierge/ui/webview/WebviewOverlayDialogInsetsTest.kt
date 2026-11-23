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

package com.adobe.marketing.mobile.concierge.ui.webview

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebviewOverlayDialogInsetsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sheetAndWebViewStayWithinTheVisibleWindow() {
        composeTestRule.setContent {
            WebviewOverlayDialog(url = "about:blank", onDismiss = {})
        }
        composeTestRule.waitForIdle()

        val sheet = composeTestRule.onNodeWithTag(WEBVIEW_OVERLAY_SHEET_TAG).fetchSemanticsNode()
        val webView = composeTestRule.onNodeWithTag(WEBVIEW_OVERLAY_WEBVIEW_TAG).fetchSemanticsNode()
        val windowHeight = sheet.windowRoot().size.height
        val navBarBottom = composeTestRule.runOnUiThread {
            ViewCompat.getRootWindowInsets(composeTestRule.activity.window.decorView)
                ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        }

        val sheetBottom = sheet.boundsInWindow.bottom
        val webViewBottom = webView.boundsInWindow.bottom
        assertTrue("Sheet bottom $sheetBottom exceeds window height $windowHeight", sheetBottom <= windowHeight + 1f)
        assertTrue(
            "WebView bottom $webViewBottom overlaps navigation bar (window $windowHeight, inset $navBarBottom)",
            webViewBottom <= windowHeight - navBarBottom + 1f
        )
    }

    private fun SemanticsNode.windowRoot(): SemanticsNode {
        var node = this
        while (true) node = node.parent ?: return node
    }
}
