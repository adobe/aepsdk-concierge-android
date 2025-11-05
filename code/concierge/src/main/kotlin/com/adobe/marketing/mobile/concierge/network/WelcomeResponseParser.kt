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
import com.adobe.marketing.mobile.concierge.ui.config.PromptConfig
import com.adobe.marketing.mobile.services.Log
import org.json.JSONException
import org.json.JSONObject

/**
 * Parser for welcome API responses that extracts welcome configuration from JSON data.
 * This parser handles the JSON structure returned by the welcome API.
 */
internal object WelcomeResponseParser {
    private const val TAG = "WelcomeResponseParser"

    // JSON field names
    private const val FIELD_WELCOME_EXAMPLES = "welcome.examples"
    private const val FIELD_TEXT = "text"
    private const val FIELD_IMAGE = "image"
    private const val FIELD_BACKGROUND_COLOR = "backgroundColor"

    /**
     * Parses a JSON string from the welcome API and extracts prompt configurations.
     *
     * Expected JSON structure:
     * ```
     * {
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
     * @return List of parsed prompt configurations, empty if parsing fails or no data found
     */
    fun parseWelcomeData(jsonData: String): List<PromptConfig> {
        if (jsonData.isBlank()) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Empty JSON data provided"
            )
            return emptyList()
        }

        return try {
            val jsonObject = JSONObject(jsonData)
            extractPromptConfigs(jsonObject)
        } catch (e: JSONException) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Failed to parse welcome JSON: ${e.message}"
            )
            emptyList()
        } catch (e: Exception) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Unexpected error parsing welcome data: ${e.message}"
            )
            emptyList()
        }
    }

    /**
     * Extracts prompt configurations from the parsed JSON object.
     */
    private fun extractPromptConfigs(jsonObject: JSONObject): List<PromptConfig> {
        val promptConfigs = mutableListOf<PromptConfig>()

        val examplesArray = jsonObject.optJSONArray(FIELD_WELCOME_EXAMPLES)
        if (examplesArray == null) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "No '$FIELD_WELCOME_EXAMPLES' array found in welcome response"
            )
            return emptyList()
        }

        for (i in 0 until examplesArray.length()) {
            val exampleObj = examplesArray.optJSONObject(i)
            if (exampleObj != null) {
                val promptConfig = parsePromptConfig(exampleObj)
                if (promptConfig != null) {
                    promptConfigs.add(promptConfig)
                    Log.debug(
                        ConciergeConstants.EXTENSION_NAME,
                        TAG,
                        "Parsed prompt config ${i + 1}: text='${promptConfig.text}', " +
                                "hasImage=${promptConfig.imageUrl != null}, " +
                                "backgroundColor=${promptConfig.backgroundColor}"
                    )
                }
            }
        }

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Successfully parsed ${promptConfigs.size} prompt configurations from welcome response"
        )

        return promptConfigs
    }

    /**
     * Parses a single prompt configuration from a JSONObject.
     * Returns null if required fields are missing.
     */
    private fun parsePromptConfig(jsonObject: JSONObject): PromptConfig? {
        val text = jsonObject.optString(FIELD_TEXT)
        if (text.isEmpty()) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Skipping prompt config with empty text"
            )
            return null
        }

        // Map 'image' field to 'imageUrl' in PromptConfig
        val imageUrl = jsonObject.optString(FIELD_IMAGE).takeIf { it.isNotEmpty() }
        val backgroundColor = jsonObject.optString(FIELD_BACKGROUND_COLOR).takeIf { it.isNotEmpty() }

        return PromptConfig(
            text = text,
            imageUrl = imageUrl,
            backgroundColor = backgroundColor
        )
    }
}

