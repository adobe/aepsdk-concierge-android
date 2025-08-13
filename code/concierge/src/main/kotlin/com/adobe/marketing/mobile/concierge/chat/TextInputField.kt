/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.configuration.ConciergeConfiguration

@Composable
fun TextInputField(
    modifier: Modifier = Modifier,
    configuration: ConciergeConfiguration,
    enabled: Boolean = true,
    onMessageSent: (String) -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        TextField(
            value = messageText,
            onValueChange = { messageText = it },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(color = Color.White),
            placeholder = {
                Text(
                    text = configuration.chat.initialMessage,
                    color = Color.Gray
                )
            },
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = {
                if (messageText.isNotBlank()) {
                    onMessageSent(messageText)
                    messageText = ""
                    focusManager.clearFocus()
                }
            },
            enabled = messageText.isNotBlank() && enabled
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
                tint = if (messageText.isNotBlank() && enabled) configuration.brandColors.secondaryLight else configuration.brandColors.primaryDark
            )
        }

        // to-do: add speech to text button
    }
}