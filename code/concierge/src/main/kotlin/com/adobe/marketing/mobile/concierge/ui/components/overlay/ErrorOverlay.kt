/*
 * Copyright 2025 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge.ui.components.overlay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Component that displays error messages as an overlay.
 */
@Composable
internal fun ErrorOverlay(
    modifier: Modifier = Modifier,
    errorMessage: String,
    onDismiss: () -> Unit
) {
    val style = ConciergeStyles.errorOverlayStyle

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(style.padding),
        colors = CardDefaults.cardColors(
            containerColor = style.backgroundColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(style.contentPadding)
        ) {
            Text(
                text = errorMessage,
                style = style.messageTextStyle,
                color = style.messageTextColor
            )

            Text(
                text = "Dismiss",
                style = style.dismissTextStyle,
                color = style.dismissTextColor,
                modifier = Modifier
                    .padding(start = style.dismissStartPadding)
                    .pointerInput(Unit) {
                        detectDragGestures { _, _ ->
                            onDismiss()
                        }
                    }
            )
        }
    }
}
