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

package com.adobe.marketing.mobile.concierge.ui.components.welcome

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.ui.config.WelcomeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Welcome card that blends into the chat screen.
 * Displays a welcome message, description, and suggested prompts for the user to explore.
 * 
 * @param config Configuration for the welcome card content
 * @param isReturningUser Whether the user is a returning user
 * @param onPromptClick Callback when a suggested prompt is clicked
 * @param modifier Optional modifier for the component
 */
@Composable
fun WelcomeCard(
    config: WelcomeConfig,
    isReturningUser: Boolean,
    onPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = ConciergeStyles.welcomeCardStyle

    // Convert PromptConfig to SuggestedPrompt
    val suggestedPrompts = remember(config.suggestedPrompts) {
        config.suggestedPrompts.map { promptConfig ->
            SuggestedPrompt(
                text = promptConfig.text,
                imageUrl = promptConfig.imageUrl,
                backgroundColor = promptConfig.backgroundColor?.let { 
                    try {
                        Color(android.graphics.Color.parseColor(it))
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            )
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = style.backgroundColor
        ),
        shape = style.shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = style.elevation
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(style.contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Welcome message (title)
                Text(
                    text = config.welcomeHeader,
                    style = style.titleTextStyle,
                    color = style.titleTextColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(style.titleBottomSpacing))

                // Description (subheading)
                Text(
                    text = config.subHeader,
                    style = style.descriptionTextStyle,
                    color = style.descriptionTextColor,
                    textAlign = TextAlign.Center
                )

                // Suggested prompts
                if (suggestedPrompts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(style.promptsTopSpacing))

                    suggestedPrompts.forEach { prompt ->
                        SuggestedPromptItem(
                            prompt = prompt,
                            onClick = { onPromptClick(prompt.text) }
                        )
                        Spacer(modifier = Modifier.height(style.promptsSpacing))
                    }
                }
            }
        }
    }
