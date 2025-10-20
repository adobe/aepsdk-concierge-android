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

import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log

/**
 * Handles the tokenization of markdown text into structured tokens.
 */
internal object MarkdownTokenizer {
    private const val TAG = "MarkdownTokenizer"

    /**
     * Tokenizes the given markdown string into a list of [MarkdownToken] objects.
     *
     * Uses a priority-based approach to handle nested markdown properly:
     * 1. First processes block-level elements (code blocks, headings, lists, blockquotes)
     * 2. Then processes inline elements (links, bold, italic, inline code) within those blocks
     *
     * @param markdown The markdown string to tokenize.
     * @return A list of [MarkdownToken] objects representing the parsed tokens.
     */
    fun tokenize(markdown: String): List<MarkdownToken> {
        val tokens = mutableListOf<MarkdownToken>()

        // Process block-level elements first (these have higher priority)
        val blockPatterns = mapOf(
            TokenType.CODE_BLOCK to """```([\s\S]*?)```""".toRegex(),
            TokenType.HEADING to """^(#{1,3})\s*(.*)""".toRegex(RegexOption.MULTILINE),
            TokenType.LIST to """^(\s*)([-•]|\d+\.)\s+(.*)""".toRegex(RegexOption.MULTILINE),
            TokenType.BLOCKQUOTE to """^>\s+(.*)""".toRegex(RegexOption.MULTILINE)
        )

        blockPatterns.forEach { (type, pattern) ->
            addMatchesIfNoOverlap(markdown, pattern, type, tokens)
        }

        // Process inline elements, allowing them to be nested within block elements
        // Note: Order matters! Process citations BEFORE links to avoid conflicts
        val inlinePatterns = mapOf(
            TokenType.CITATION to """\[\^(\d+)\]""".toRegex(),
            TokenType.LINK to """\[([^\^][^\]]*)\]\((.*?)\)""".toRegex(),  // Updated to exclude [^ patterns
            TokenType.INLINE_CODE to """`(.*?)`""".toRegex(),
            TokenType.BOLD to """\*\*(.*?)\*\*""".toRegex(),
            TokenType.ITALIC to """\*(.*?)\*""".toRegex()
        )

        inlinePatterns.forEach { (type, pattern) ->
            addInlineMatches(markdown, pattern, type, tokens)
        }

        val sortedTokens = tokens.sortedBy { it.start }
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Total tokens found: ${sortedTokens.size}"
        )
        sortedTokens.forEach { token ->
            Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Token: $token")
        }

        return sortedTokens
    }

    private fun addMatchesIfNoOverlap(
        markdown: String,
        pattern: Regex,
        type: TokenType,
        tokens: MutableList<MarkdownToken>
    ) {
        pattern.findAll(markdown).forEach { result ->
            val groups = (1..result.groups.size - 1).map { i -> result.groups[i]?.value ?: "" }

            val newToken = when (type) {
                TokenType.LIST -> {
                    val indentation = groups.firstOrNull() ?: ""
                    val listMarker = groups.getOrNull(1) ?: ""
                    val content = groups.getOrNull(2) ?: ""
                    val indentationLevel = indentation.length / 2 // Assuming 2 spaces per level

                    MarkdownToken(
                        type = type,
                        start = result.range.first,
                        end = result.range.last + 1,
                        groups = listOf(content, listMarker),
                        indentationLevel = indentationLevel
                    )
                }

                else -> MarkdownToken(
                    type = type,
                    start = result.range.first,
                    end = result.range.last + 1,
                    groups = groups
                )
            }

            // Only add if it doesn't overlap with existing tokens
            val hasOverlap = tokens.any { existing ->
                (newToken.start < existing.end && newToken.end > existing.start)
            }

            if (!hasOverlap) {
                tokens += newToken
            }
        }
    }

    private fun addInlineMatches(
        markdown: String,
        pattern: Regex,
        type: TokenType,
        tokens: MutableList<MarkdownToken>
    ) {
        pattern.findAll(markdown).forEach { result ->
            val newToken = MarkdownToken(
                type = type,
                start = result.range.first,
                end = result.range.last + 1,
                groups = (1..result.groups.size - 1).map { i -> result.groups[i]?.value ?: "" }
            )

            // For inline elements, allow them to be nested within block elements
            // but prevent them from overlapping with other inline elements
            val hasInlineOverlap = tokens.any { existing ->
                val isInlineElement = existing.type in listOf(
                    TokenType.LINK,
                    TokenType.INLINE_CODE,
                    TokenType.BOLD,
                    TokenType.ITALIC,
                    TokenType.CITATION
                )
                isInlineElement && (newToken.start < existing.end && newToken.end > existing.start)
            }

            if (!hasInlineOverlap) {
                tokens += newToken
            }
        }
    }
}

/**
 * Data class representing a parsed markdown token
 */
internal data class MarkdownToken(
    val type: TokenType,
    val start: Int,
    val end: Int,
    val groups: List<String>,
    val indentationLevel: Int = 0
)

/**
 * Enum representing different types of markdown tokens
 */
internal enum class TokenType {
    CODE_BLOCK,
    INLINE_CODE,
    LINK,
    BOLD,
    ITALIC,
    HEADING,
    LIST,
    BLOCKQUOTE,
    CITATION
}
