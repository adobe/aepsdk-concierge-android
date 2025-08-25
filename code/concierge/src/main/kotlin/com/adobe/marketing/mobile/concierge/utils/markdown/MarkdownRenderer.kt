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

import androidx.compose.ui.text.AnnotatedString
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log

/**
 * Handles the rendering of tokens into an [AnnotatedString].
 */
internal class MarkdownRenderer {

    /**
     * Renders the provided list of [MarkdownToken]s into an [AnnotatedString].
     *
     * @param markdown The original markdown string.
     * @param tokens The list of [MarkdownToken]s to render.
     * @return An [AnnotatedString] with the appropriate styling applied.
     */
    fun render(markdown: String, tokens: List<MarkdownToken>): AnnotatedString {
        Log.debug(ConciergeConstants.EXTENSION_NAME, "render", "Starting rendering of ${tokens.size} tokens")
        val builder = AnnotatedString.Builder()
        var currentIndex = 0
        
        tokens.forEach { token ->
            if (token.start < currentIndex) return@forEach
            
            appendGapText(markdown, currentIndex, token.start, builder)
            currentIndex = token.start
            renderToken(token, builder)
            currentIndex = token.end
        }
        
        appendGapText(markdown, currentIndex, markdown.length, builder)
        
        Log.debug(ConciergeConstants.EXTENSION_NAME, "render", "Rendering completed, final text length: ${builder.length}")
        return builder.toAnnotatedString()
    }
    
    private fun appendGapText(
        markdown: String, 
        from: Int, 
        to: Int, 
        builder: AnnotatedString.Builder
    ) {
        if (from < to) {
            builder.append(markdown.substring(from, to))
        }
    }
    
    private fun renderToken(token: MarkdownToken, builder: AnnotatedString.Builder) {
        when (token.type) {
            TokenType.CODE_BLOCK -> renderCodeBlock(token, builder)
            TokenType.INLINE_CODE -> renderInlineCode(token, builder)
            TokenType.LINK -> renderLink(token, builder)
            TokenType.BOLD -> renderBold(token, builder)
            TokenType.ITALIC -> renderItalic(token, builder)
            TokenType.HEADING -> renderHeading(token, builder)
            TokenType.LIST -> renderList(token, builder)
            TokenType.BLOCKQUOTE -> renderBlockquote(token, builder)
        }
    }
    
    private fun renderCodeBlock(token: MarkdownToken, builder: AnnotatedString.Builder) {
        val codeContent = token.groups[0].trim()
        val styleStart = builder.length
        
        builder.append(codeContent)
        builder.addStyle(MarkdownStyles.codeBlockStyle(), styleStart, builder.length)
    }
    
    private fun renderInlineCode(token: MarkdownToken, builder: AnnotatedString.Builder) {
        val codeContent = token.groups[0]
        val styleStart = builder.length
        
        builder.append(codeContent)
        builder.addStyle(MarkdownStyles.inlineCodeStyle(), styleStart, builder.length)
    }
    
    private fun renderLink(token: MarkdownToken, builder: AnnotatedString.Builder) {
        val (linkText, linkUrl) = token.groups
        val styleStart = builder.length
        
        builder.append(linkText)
        builder.addStyle(MarkdownStyles.linkStyle(), styleStart, builder.length)
        
        builder.addStringAnnotation(
            tag = "URL",
            annotation = linkUrl,
            start = styleStart,
            end = builder.length
        )
    }
    
    private fun renderBold(token: MarkdownToken, builder: AnnotatedString.Builder) {
        val boldContent = token.groups[0]
        val styleStart = builder.length
        
        builder.append(boldContent)
        builder.addStyle(MarkdownStyles.BOLD_STYLE, styleStart, builder.length)
    }
    
    private fun renderItalic(token: MarkdownToken, builder: AnnotatedString.Builder) {
        val italicContent = token.groups[0]
        val styleStart = builder.length
        
        builder.append(italicContent)
        builder.addStyle(MarkdownStyles.ITALIC_STYLE, styleStart, builder.length)
    }
    
    private fun renderHeading(token: MarkdownToken, builder: AnnotatedString.Builder) {
        val headingLevel = token.groups[0].length // # or ##
        val headingText = token.groups[1]
        val styleStart = builder.length
        
        builder.append(headingText)
        builder.addStyle(MarkdownStyles.headingStyle(headingLevel), styleStart, builder.length)
    }
    
    private fun renderList(token: MarkdownToken, builder: AnnotatedString.Builder) {
        val listItem = token.groups[0]
        builder.append("• $listItem\n")
    }
    
    private fun renderBlockquote(token: MarkdownToken, builder: AnnotatedString.Builder) {
        val quoteText = token.groups[0]
        val styleStart = builder.length
        
        builder.append(quoteText)
        builder.addStyle(MarkdownStyles.blockquoteStyle(), styleStart, builder.length)
        builder.append("\n")
    }
}
