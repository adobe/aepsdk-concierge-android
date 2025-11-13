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

package com.adobe.marketing.mobile.concierge.network

import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.ui.config.WelcomeConfig

/**
 * Example showing how to use WelcomeResponseParser to parse welcome API responses
 * and create a WelcomeConfig.
 */
internal object WelcomeResponseParserExample {

    /**
     * Example: Parse a mock welcome API response and create a WelcomeConfig
     */
    fun parseAndCreateConfig(jsonResponse: String): WelcomeConfig? {
        // Parse the mock JSON response, return null if no prompts were parsed
        val welcomeData = WelcomeResponseParser.parseWelcomeData(jsonResponse) ?: return null
        
        return WelcomeConfig(
            showWelcomeCard = true,
            welcomeHeader = welcomeData.heading ?: ConciergeConstants.WelcomeCard.DEFAULT_HEADING,
            subHeader = welcomeData.subheading ?: ConciergeConstants.WelcomeCard.DEFAULT_SUBHEADING,
            suggestedPrompts = welcomeData.prompts
        )
    }

    /**
     * Example: Mock API response matching the expected format
     * TODO: Update this mock with the actual payload returned for the Welcome screen config
     */
    fun getMockWelcomeResponse(): String = """
        {
            "welcome.heading": "Explore what you can do with Adobe apps.",
            "welcome.subheading": "Choose an option or tell us what interests you and we'll point you in the right direction.",
            "welcome.examples": [
                {
                    "text": "I'd like to explore templates to see what I can create.",
                    "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_142fd6e4e46332d8f41f5aef982448361c0c8c65e.png",
                    "backgroundColor": "#FFFFFF"
                },
                {
                    "text": "I want to touch up and enhance my photos.",
                    "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_1e188097a1bc580b26c8be07d894205c5c6ca5560.png",
                    "backgroundColor": "#FFFFFF"
                },
                {
                    "text": "I'd like to edit PDFs and make them interactive.",
                    "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_1f6fed23045bbbd57fc17dadc3aa06bcc362f84cb.png",
                    "backgroundColor": "#FFFFFF"
                },
                {
                    "text": "I want to turn my clips into polished videos.",
                    "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_16c2ca834ea8f2977296082ae6f55f305a96674ac.png",
                    "backgroundColor": "#FFFFFF"
                }
            ]
        }
    """.trimIndent()
}

