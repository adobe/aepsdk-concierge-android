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
internal class MarkdownTokenizer {

    /**
     * Tokenizes the given markdown string into a list of [MarkdownToken] objects.
     *
     * The order of pattern matching is important to avoid conflicts, especially with links.
     * Links are processed first to ensure they are not broken up by other patterns.
     *
     * @param markdown The markdown string to tokenize.
     * @return A list of [MarkdownToken] objects representing the parsed tokens.
     */
    fun tokenize(markdown: String): List<MarkdownToken> {
        val tokens = mutableListOf<MarkdownToken>()
        
        // First, find all links
        val linkPattern = """\[(.*?)\]\((.*?)\)""".toRegex()
        linkPattern.findAll(markdown).forEach { result ->
            val matchedGroups = (1..result.groups.size - 1)
                .map { i -> result.groups[i]?.value ?: "" }
            
            tokens += MarkdownToken(
                type = TokenType.LINK,
                start = result.range.first,
                end = result.range.last + 1,
                groups = matchedGroups
            )
        }
        
        // Then find other patterns, but skip if they overlap with existing tokens
        val otherPatterns = mapOf(
            TokenType.CODE_BLOCK to """```([\s\S]*?)```""".toRegex(),
            TokenType.INLINE_CODE to """`(.*?)`""".toRegex(),
            TokenType.HEADING to """^(#{1,2})\s*(.*)""".toRegex(RegexOption.MULTILINE),
            TokenType.LIST to """^- (.*)""".toRegex(RegexOption.MULTILINE),
            TokenType.BLOCKQUOTE to """^>\s+(.*)""".toRegex(RegexOption.MULTILINE),
            TokenType.ITALIC to """\*(.*?)\*""".toRegex(),
            TokenType.BOLD to """\*\*(.*?)\*\*""".toRegex()
        )
        
        otherPatterns.forEach { (type, pattern) ->
            addMatchesIfNoOverlap(markdown, pattern, type, tokens)
        }
        
        val sortedTokens = tokens.sortedBy { it.start }
        Log.debug(ConciergeConstants.EXTENSION_NAME, "tokenize", "Total tokens found: ${sortedTokens.size}")
        sortedTokens.forEach { token ->
            Log.debug(ConciergeConstants.EXTENSION_NAME, "tokenize", "Token: $token")
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
            val newToken = MarkdownToken(
                type = type,
                start = result.range.first,
                end = result.range.last + 1,
                groups = (1..result.groups.size - 1).map { i -> result.groups[i]?.value ?: "" }
            )
            
            // Only add if it doesn't overlap with existing tokens
            val hasOverlap = tokens.any { existing ->
                (newToken.start < existing.end && newToken.end > existing.start)
            }
            
            if (!hasOverlap) {
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
    val groups: List<String>
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
    BLOCKQUOTE
}
