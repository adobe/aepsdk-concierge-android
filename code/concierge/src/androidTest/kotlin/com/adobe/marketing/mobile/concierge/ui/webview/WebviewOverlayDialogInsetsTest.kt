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

import android.app.UiAutomation
import android.graphics.Rect
import android.view.View
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebviewOverlayDialogInsetsTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @After
    fun restoreRotation() {
        instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
    }

    @Test
    fun webViewStaysInsideTheDialogSafeAreaInPortrait() =
        assertWebViewInsideSafeArea(UiAutomation.ROTATION_FREEZE_0, edgeToEdgeHost = false)

    @Test
    fun webViewStaysInsideTheDialogSafeAreaInLandscape() =
        assertWebViewInsideSafeArea(UiAutomation.ROTATION_FREEZE_90, edgeToEdgeHost = false)

    @Test
    fun webViewStaysInsideTheDialogSafeAreaInPortraitOverEdgeToEdgeHost() =
        assertWebViewInsideSafeArea(UiAutomation.ROTATION_FREEZE_0, edgeToEdgeHost = true)

    @Test
    fun webViewStaysInsideTheDialogSafeAreaInLandscapeOverEdgeToEdgeHost() =
        assertWebViewInsideSafeArea(UiAutomation.ROTATION_FREEZE_90, edgeToEdgeHost = true)

    private fun assertWebViewInsideSafeArea(rotation: Int, edgeToEdgeHost: Boolean) {
        instrumentation.uiAutomation.setRotation(rotation)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity {
                if (edgeToEdgeHost) WindowCompat.setDecorFitsSystemWindows(it.window, false)
                it.setContent { WebviewOverlayDialog(url = "about:blank", onDismiss = {}) }
            }
            composeTestRule.waitForIdle()

            var webView: View? = null
            onView(isAssignableFrom(WebView::class.java)).inRoot(isDialog()).check { view, _ -> webView = view }
            lateinit var bounds: Rect
            lateinit var safeArea: Rect
            lateinit var visibleFrame: Rect
            instrumentation.runOnMainSync {
                val view = requireNotNull(webView)
                val root = view.rootView
                val insets = requireNotNull(ViewCompat.getRootWindowInsets(root)).getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
                        WindowInsetsCompat.Type.ime()
                )
                // Unclipped View geometry, in screen coordinates, so overflow past the window is visible.
                val location = IntArray(2).also(view::getLocationOnScreen)
                bounds = Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
                val rootLocation = IntArray(2).also(root::getLocationOnScreen)
                safeArea = Rect(
                    rootLocation[0] + insets.left,
                    rootLocation[1] + insets.top,
                    rootLocation[0] + root.width - insets.right,
                    rootLocation[1] + root.height - insets.bottom
                )
                visibleFrame = Rect().also(root::getWindowVisibleDisplayFrame)
            }

            // Compose sizes dialog windows from the dp-rounded screen size, which can add 1px.
            safeArea.inset(-1, -1)
            visibleFrame.inset(-1, -1)
            assertTrue("WebView $bounds is outside the dialog's safe area $safeArea", safeArea.contains(bounds))
            assertTrue("WebView $bounds is outside the visible window $visibleFrame", visibleFrame.contains(bounds))
        }
    }
}
