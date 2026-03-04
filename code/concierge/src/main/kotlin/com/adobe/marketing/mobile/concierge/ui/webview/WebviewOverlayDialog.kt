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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Full-height overlay dialog that presents a URL with a top bar and WebView.
 *
 * @param url The URL to load in the overlay WebView
 * @param onDismiss Callback when the dialog is dismissed (back press, close button, or tap on top bar)
 */
@Composable
internal fun WebviewOverlayDialog(
    url: String,
    onDismiss: () -> Unit
) {
    val style = ConciergeStyles.webviewStyle
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(style.scrimHeightFraction)
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = style.scrimAlpha))
                    .clickable(
                        onClick = onDismiss,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(style.contentHeightFraction)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = style.contentCornerRadius, topEnd = style.contentCornerRadius))
                    .background(style.contentBackgroundColor),
                contentAlignment = Alignment.TopCenter
            ) {
                WebViewSheetContent(
                    url = url,
                    onDismiss = onDismiss,
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}
