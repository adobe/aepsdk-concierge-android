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

package com.adobe.marketing.mobile.concierge.ui.config

import com.adobe.marketing.mobile.concierge.ConciergeConstants

/**
 * Data class representing a suggested prompt with image and styling
 */
data class SuggestedPrompt(
    val text: String,
    val imageUrl: String? = null,
    val backgroundColor: String? = null
)

/**
 * Configuration for the welcome card/screen
 */
data class WelcomeConfig(
    /**
     * Whether to show the welcome card when the chat is first opened
     */
    val showWelcomeCard: Boolean = true,

    /**
     * Custom welcome message for first-time users
     * If null, a default message will be shown
     */
    val firstTimeWelcomeMessage: String? = null,

    /**
     * Custom welcome message for returning users
     * If null, a default message will be shown
     */
    val returningUserWelcomeMessage: String? = null,

    /**
     * Header text shown in the welcome card
     */
    val welcomeHeader: String = ConciergeConstants.WelcomeCard.DEFAULT_HEADING,

    /**
     * Sub-Header text shown in the welcome card
     */
    val subHeader: String = ConciergeConstants.WelcomeCard.DEFAULT_SUBHEADING,

    /**
     * List of suggested prompts to show in the welcome card
     */
    val suggestedPrompts: List<SuggestedPrompt> = emptyList()
)

