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
import com.adobe.marketing.mobile.concierge.ui.config.SuggestedPrompt
import com.adobe.marketing.mobile.services.Log
import org.json.JSONException
import org.json.JSONObject

/**
 * Data class to hold parsed welcome data from the API response
 */
data class WelcomeData(
    val heading: String? = null,
    val subheading: String? = null,
    val prompts: List<SuggestedPrompt> = emptyList()
)

/**
 * This parser handles the JSON structure returned by the concierge configuration.
 */
internal object WelcomeResponseParser {
    private const val TAG = "WelcomeResponseParser"

    // JSON field names
    private const val FIELD_WELCOME_EXAMPLES = "welcome.examples"
    private const val FIELD_TEXT = "text"
    private const val FIELD_IMAGE = "image"
    private const val FIELD_BACKGROUND_COLOR = "backgroundColor"
    private const val FIELD_WELCOME_HEADER = "welcome.heading"
    private const val FIELD_SUB_HEADER = "welcome.subheading"

    /**
     * Parses a JSON string from the concierge configuration and extracts welcome data including
     * heading, subheading, and prompt configurations.
     *
     * Expected JSON structure:
     * ```
     * {
     *   "welcome.heading": "Explore what you can do with Adobe apps.",
     *   "welcome.subheading": "Choose an option or tell us what interests you and we'll point you in the right direction.",
     *   "welcome.examples": [
     *     {
     *       "text": "I'd like to explore templates to see what I can create.",
     *       "image": "https://example.com/image.png",
     *       "backgroundColor": "#FFFFFF"
     *     },
     *     ...
     *   ]
     * }
     * ```
     *
     * @param jsonData The raw JSON string from the welcome API response
     * @return WelcomeData containing heading, subheading, and prompts, or null if parsing fails
     */
    fun parseWelcomeData(jsonData: String): WelcomeData? {
        if (jsonData.isBlank()) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Empty JSON data provided"
            )
            return null
        }

        return try {
            val jsonObject = JSONObject(jsonData)
            extractWelcomeData(jsonObject)
        } catch (e: JSONException) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Failed to parse welcome JSON: ${e.message}"
            )
            null
        } catch (e: Exception) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Unexpected error parsing welcome data: ${e.message}"
            )
            null
        }
    }

    /**
     * Extracts welcome data from the parsed JSON object.
     */
    private fun extractWelcomeData(jsonObject: JSONObject): WelcomeData {
        val heading = jsonObject.optString(FIELD_WELCOME_HEADER).takeIf { it.isNotEmpty() }
        val subheading = jsonObject.optString(FIELD_SUB_HEADER).takeIf { it.isNotEmpty() }
        
        val prompts = jsonObject.optJSONArray(FIELD_WELCOME_EXAMPLES)?.let { examplesArray ->
            (0 until examplesArray.length()).mapNotNull { i ->
                examplesArray.optJSONObject(i)?.let { parseSuggestedPrompt(it) }
            }
        } ?: emptyList()

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Parsed welcome data: heading='$heading', subheading='$subheading', ${prompts.size} prompts"
        )

        return WelcomeData(
            heading = heading,
            subheading = subheading,
            prompts = prompts
        )
    }

    /**
     * Parses a suggested prompt from a JSONObject.
     * Returns null if required fields are missing.
     */
    private fun parseSuggestedPrompt(jsonObject: JSONObject): SuggestedPrompt? {
        val text = jsonObject.optString(FIELD_TEXT).takeIf { it.isNotEmpty() } ?: return null
        val imageUrl = jsonObject.optString(FIELD_IMAGE).takeIf { it.isNotEmpty() }
        val backgroundColor = jsonObject.optString(FIELD_BACKGROUND_COLOR).takeIf { it.isNotEmpty() }

        return SuggestedPrompt(
            text = text,
            imageUrl = imageUrl,
            backgroundColor = backgroundColor
        )
    }
}

