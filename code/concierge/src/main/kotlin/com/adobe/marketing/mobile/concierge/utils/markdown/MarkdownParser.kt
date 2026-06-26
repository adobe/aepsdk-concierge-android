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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.network.LinkHint
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
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
    fun parse(markdown: String, linkHints: List<LinkHint> = emptyList()): AnnotatedString {
        val tokens = remember(markdown) { MarkdownTokenizer.tokenize(markdown) }
        val colorScheme = ConciergeTheme.colorScheme
        val messageBubbleStyle = ConciergeStyles.messageBubbleStyle
        val messageTextStyle = messageBubbleStyle.textStyle.copy(color = messageBubbleStyle.botMessageTextColor)
        val darkTheme = isSystemInDarkTheme()

        // Memoize the rendered AnnotatedString. Without this, MarkdownRenderer.render() re-runs on
        // every recomposition for every visible message, rebuilding the full AnnotatedString. During
        // a streaming response the message list is re-emitted on every chunk, so the cost scales as
        // O(messages x chunks) and pins the main thread / churns the heap as the conversation grows.
        //
        // Keys must be value-comparable for the cache to hold across recompositions: `markdown` and
        // `linkHints` cover the content; `messageTextStyle` (TextStyle has structural equality) covers
        // typography and text color; `darkTheme` covers the derived `colorScheme`. `colorScheme`
        // itself is NOT used as a key — ConciergeTheme.colorScheme builds a fresh ColorScheme on every
        // read and ColorScheme has only reference equality, so keying on it would invalidate the cache
        // every recomposition and defeat the memoization entirely.
        return remember(markdown, linkHints, messageTextStyle, darkTheme) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Parsed ${tokens.size} tokens, starting rendering."
            )
            MarkdownRenderer.render(markdown, tokens, colorScheme, messageTextStyle, linkHints)
        }
    }
}
