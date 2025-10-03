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

package com.adobe.marketing.mobile.concierge.ui.components.input

import androidx.compose.foundation.Image
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * A send button for submitting chat messages.
 *
 * @param modifier Modifier for the composable
 * @param canSendMessage Whether a message can be sent
 * @param isEnabled Whether the button is enabled
 * @param onSend Callback when the send button is pressed
 */
@Composable
internal fun SendButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    onSend: () -> Unit
) {
    val style = ConciergeStyles.sendButtonStyle
    
    IconButton(
        onClick = {
            if (isEnabled) {
                onSend()
            }
        },
        enabled = isEnabled,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(R.drawable.send),
            contentDescription = "Send message",
            colorFilter = ColorFilter.tint(
                if (isEnabled) {
                    style.enabledIconColor
                } else {
                    style.enabledIconColor.copy(alpha = style.disabledIconAlpha)
                }
            )
        )
    }
}
