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
data class PromptConfig(
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
     * Brand name to display in the welcome card
     */
    val brandName: String = "Concierge",

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
     * Description text shown in the welcome card
     */
    val welcomeDescription: String = ConciergeConstants.WelcomeCard.DEFAULT_DESCRIPTION,

    /**
     * List of suggested prompts to show in the welcome card
     * TODO: Make this configurable via backend
     */
    val suggestedPrompts: List<PromptConfig> = listOf(
        PromptConfig(
            text = "I'd like to explore templates to see what I can create.",
            imageUrl = "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_142fd6e4e46332d8f41f5aef982448361c0c8c65e.png",
            backgroundColor = "#FFFFFF"
        ),
        PromptConfig(
            text = "I want to touch up and enhance my photos.",
            imageUrl = "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_1e188097a1bc580b26c8be07d894205c5c6ca5560.png",
            backgroundColor = "#FFFFFF"
        ),
        PromptConfig(
            text = "I'd like to edit PDFs and make them interactive.",
            imageUrl = "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_1f6fed23045bbbd57fc17dadc3aa06bcc362f84cb.png",
            backgroundColor = "#FFFFFF"
        ),
        PromptConfig(
            text = "I want to turn my clips into polished videos.",
            imageUrl = "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_16c2ca834ea8f2977296082ae6f55f305a96674ac.png",
            backgroundColor = "#FFFFFF"
        )
    )
)

