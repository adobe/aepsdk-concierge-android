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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownToken

/**
 * Sealed class representing different types of content segments in a response.
 */
internal sealed class ContentSegment {
    data class Text(val content: String, val startIndex: Int, val endIndex: Int) : ContentSegment()
    data class List(val tokens: kotlin.collections.List<MarkdownToken>) : ContentSegment()
}

/**
 * Utility class for parsing content into segments.
 */
internal object ContentSegmentParser {
    /**
     * Creates content segments that preserve the original order of text and lists in the response.
     */
    fun createSegments(
        text: String,
        listTokens: List<MarkdownToken>
    ): List<ContentSegment> {
        if (listTokens.isEmpty()) {
            return listOf(ContentSegment.Text(text, 0, text.length))
        }

        val sortedTokens = listTokens.sortedBy { it.start }
        val segments = mutableListOf<ContentSegment>()
        var currentIndex = 0

        sortedTokens.forEach { token ->
            // Add text content before the list item
            if (token.start > currentIndex) {
                val textContent = text.substring(currentIndex, token.start).trim()
                if (textContent.isNotEmpty()) {
                    segments.add(ContentSegment.Text(textContent, currentIndex, token.start))
                }
            }

            // Add the list item
            segments.add(ContentSegment.List(listOf(token)))
            currentIndex = token.end
        }

        // Add any remaining text content after the last list item
        if (currentIndex < text.length) {
            val remainingText = text.substring(currentIndex).trim()
            if (remainingText.isNotEmpty()) {
                segments.add(ContentSegment.Text(remainingText, currentIndex, text.length))
            }
        }

        return segments
    }
}
