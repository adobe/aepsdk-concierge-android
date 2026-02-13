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

import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.delay

private const val WEBVIEW_SCRIM_ALPHA = 0.32f
private const val WEBVIEW_SHEET_HEIGHT_FRACTION = 0.9f
private const val WEBVIEW_SHEET_SLIDE_DELAY_MS = 50
private const val WEBVIEW_SHEET_SLIDE_DURATION_MS = 300
private const val WEBVIEW_DIM_AMOUNT = 0.4f

/**
 * Dialog that presents a URL in a bottom sheet with scrim and slide-up animation.
 *
 * @param url The URL to load in the overlay WebView
 * @param onDismiss Callback when the dialog is dismissed (scrim tap, back, or close button)
 */
@Composable
internal fun WebviewOverlayDialog(
    url: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val parentView = LocalView.current.parent as View
        val window = (parentView as DialogWindowProvider).window
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setDimAmount(WEBVIEW_DIM_AMOUNT)
        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = WEBVIEW_SCRIM_ALPHA))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        ) {
            val sheetHeightDp = maxHeight * WEBVIEW_SHEET_HEIGHT_FRACTION
            var sheetOffsetTarget by remember { mutableStateOf<Dp>(sheetHeightDp) }
            LaunchedEffect(Unit) {
                delay(WEBVIEW_SHEET_SLIDE_DELAY_MS.toLong())
                sheetOffsetTarget = 0.dp
            }
            val sheetOffset by animateDpAsState(
                targetValue = sheetOffsetTarget,
                animationSpec = tween(durationMillis = WEBVIEW_SHEET_SLIDE_DURATION_MS),
                label = "webviewSheetSlide"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = sheetOffset)
                    .fillMaxWidth()
                    .height(sheetHeightDp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* consume taps so scrim dismisses; scroll goes to WebView */ }
            ) {
                WebViewSheetContent(url = url, onDismiss = onDismiss)
            }
        }
    }
}
