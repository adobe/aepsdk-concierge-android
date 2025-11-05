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

package com.adobe.marketing.mobile.concierge.utils.markdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.services.Log

/**
 * Core class that manages the parsing workflow via the [MarkdownTokenizer] and [MarkdownRenderer]
 * objects.
 */
internal object MarkdownParser {
    private const val TAG = "MarkdownParser"

    /**
     * Parses the provided markdown string and returns an [AnnotatedString] with the appropriate
     * styling applied.
     *
     * @param markdown The markdown string to parse.
     * @return An [AnnotatedString] with the appropriate styling applied.
     */
    @Composable
    fun parse(markdown: String): AnnotatedString {
        val tokens = remember(markdown) { MarkdownTokenizer.tokenize(markdown) }
        val colorScheme = MaterialTheme.colorScheme
        val messageTextStyle = ConciergeStyles.messageBubbleStyle.textStyle

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Parsed ${tokens.size} tokens, starting rendering."
        )

        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, messageTextStyle)
        return result
    }
}
