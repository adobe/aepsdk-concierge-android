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

package com.adobe.marketing.mobile.concierge.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.darkColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.configuration.ConciergeConfiguration

/**
 * A composable that provides a frame for displaying chat requests, chat responses,
 * and a text field for receiving text input. A top app bar is included to display
 * a customizable chat title.
 * 
 * @param modifier Modifier to be applied to the composable
 * @param chatTitle Title for the chat input area, defaults to "Concierge Chat"
 * @param placeholderText Text to display as placeholder in the input field
 * @param configuration Configuration for the concierge chat
 * @param enabled Whether the input field is enabled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConciergeChat(
    modifier: Modifier = Modifier,
    configuration: ConciergeConfiguration,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(configuration.brandColors.primary)
    ) {
        TopAppBar(
            title = { Text(text = configuration.chat.conciergeName) },
            colors = TopAppBarColors(
                containerColor = configuration.brandColors.primary,
                titleContentColor = configuration.brandColors.textPrimary,
                scrolledContainerColor = Color.Unspecified,
                navigationIconContentColor = Color.Unspecified,
                actionIconContentColor = Color.Unspecified
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        // Spacer to push the TextInputField to the bottom
        Spacer(modifier = Modifier.weight(1f))

        TextInputField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            configuration,
            enabled = enabled
        )
    }
}
