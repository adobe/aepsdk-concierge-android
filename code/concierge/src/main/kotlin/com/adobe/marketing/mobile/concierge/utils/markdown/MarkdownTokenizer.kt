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
            TokenType.BLOCKQUOTE to """^>\s+(.*)""".toRegex(RegexOption.MULTILINE)
        )

        blockPatterns.forEach { (type, pattern) ->
            addMatchesIfNoOverlap(markdown, pattern, type, tokens)
        }
        
        // Process list items separately to handle multi-line content
        addListTokens(markdown, tokens)

        // Process inline elements, allowing them to be nested within block elements
        val inlinePatterns = mapOf(
            TokenType.LINK to """\[([^\]]*)\]\((.*?)\)""".toRegex(),
            TokenType.INLINE_CODE to """`(.*?)`""".toRegex(),
            TokenType.BOLD to """\*\*(.*?)\*\*""".toRegex(),
            TokenType.ITALIC to """(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""".toRegex()
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

    /**
     * Processes list items separately to handle multi-line content.
     * List items can span multiple lines until:
     * - The next list item begins
     * - A blank line is encountered AFTER all list items (end of list)
     * - The end of the string
     */
    private fun addListTokens(
        markdown: String,
        tokens: MutableList<MarkdownToken>
    ) {
        // Pattern to match the start of a list item
        val listStartPattern = """^(\s*)([-•]|\d+\.)\s+""".toRegex(RegexOption.MULTILINE)
        
        val matches = listStartPattern.findAll(markdown).toList()
        
        for (i in matches.indices) {
            val match = matches[i]
            val indentation = match.groups[1]?.value ?: ""
            val listMarker = match.groups[2]?.value ?: ""
            
            // Check if this item appears after a blank line - if so, treat as top-level
            val isPrecededByBlankLine = hasPrecedingBlankLine(markdown, matches, i, match)
            
            val indentationLevel = if (isPrecededByBlankLine) {
                0
            } else {
                indentation.length / 2
            }
            
            val contentStart = match.range.last + 1
            
            // Find the end of this list item (start of next list item or end of string)
            val contentEnd = if (i < matches.size - 1) {
                // Content ends at the start of the next list item
                // Find the position just before the next list marker, trimming any whitespace/newlines
                val nextListStart = matches[i + 1].range.first
                // Back up to find the last non-whitespace character before the next list item
                var actualEnd = nextListStart
                while (actualEnd > contentStart && markdown[actualEnd - 1].isWhitespace()) {
                    actualEnd--
                }
                actualEnd
            } else {
                // This is the last list item
                // Check if there's content after this list item (separated by blank lines)
                val remainingContent = markdown.substring(contentStart)
                val blankLineIndex = remainingContent.indexOf("\n\n")
                if (blankLineIndex != -1) {
                    contentStart + blankLineIndex
                } else {
                    markdown.length
                }
            }
            
            // Extract the content, trimming trailing newlines
            val content = markdown.substring(contentStart, contentEnd).trimEnd('\n', '\r')
            
            val newToken = MarkdownToken(
                type = TokenType.LIST,
                start = match.range.first,
                end = contentEnd,
                groups = listOf(content, listMarker),
                indentationLevel = indentationLevel
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
    
    private fun addMatchesIfNoOverlap(
        markdown: String,
        pattern: Regex,
        type: TokenType,
        tokens: MutableList<MarkdownToken>
    ) {
        pattern.findAll(markdown).forEach { result ->
            val groups = (1..result.groups.size - 1).map { i -> result.groups[i]?.value ?: "" }

            val newToken = MarkdownToken(
                type = type,
                start = result.range.first,
                end = result.range.last + 1,
                groups = groups
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
                    TokenType.ITALIC
                )
                isInlineElement && (newToken.start < existing.end && newToken.end > existing.start)
            }

            if (!hasInlineOverlap) {
                tokens += newToken
            }
        }
    }
    
    /**
     * Checks if a list item is preceded by a blank line.
     * A blank line is defined as 2+ consecutive newlines.
     * 
     * @param markdown The full markdown string
     * @param matches All list item matches found
     * @param currentIndex Index of the current list item
     * @param currentMatch The current match object
     * @return true if preceded by a blank line, false otherwise
     */
    private fun hasPrecedingBlankLine(
        markdown: String,
        matches: List<MatchResult>,
        currentIndex: Int,
        currentMatch: MatchResult
    ): Boolean {
        if (currentIndex == 0) return false
        
        val prevMatch = matches[currentIndex - 1]
        val prevContentEnd = prevMatch.range.last + 1
        
        // Find the start of current item's line (before any indentation)
        var lineStart = currentMatch.range.first
        while (lineStart > 0 && markdown[lineStart - 1] != '\n') {
            lineStart--
        }
        
        // Check content between previous item and current line
        if (lineStart <= prevContentEnd) return false
        
        val contentBetween = markdown.substring(prevContentEnd, lineStart)
        // Blank line = 2+ newlines
        return contentBetween.count { it == '\n' } >= 2
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
    BLOCKQUOTE
}
