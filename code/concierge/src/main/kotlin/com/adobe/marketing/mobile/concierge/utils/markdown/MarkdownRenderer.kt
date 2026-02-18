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

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.appendInlineContent
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log

/**
 * Handles the rendering of tokens into an [AnnotatedString].
 */
internal object MarkdownRenderer {
    private const val TAG = "MarkdownRenderer"

    /**
     * Renders the provided list of [MarkdownToken]s into an [AnnotatedString].
     *
     * @param markdown The original markdown string.
     * @param tokens The list of [MarkdownToken]s to render.
     * @param colorScheme The Material Design color scheme.
     * @param baseTextStyle The base text style to apply to regular text.
     * @return An [AnnotatedString] with the appropriate styling applied.
     */
    fun render(
        markdown: String,
        tokens: List<MarkdownToken>,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ): AnnotatedString {
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Starting rendering of ${tokens.size} tokens"
        )

        val builder = AnnotatedString.Builder()
        var currentIndex = 0

        tokens.forEach { token ->
            if (token.start < currentIndex) return@forEach

            appendGapText(markdown, currentIndex, token.start, builder, colorScheme, baseTextStyle)
            renderToken(token, builder, colorScheme, baseTextStyle)
            currentIndex = token.end
        }

        appendGapText(markdown, currentIndex, markdown.length, builder, colorScheme, baseTextStyle)

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Rendering completed, final text length: ${builder.length}"
        )
        return builder.toAnnotatedString()
    }

    private fun appendGapText(
        markdown: String,
        from: Int,
        to: Int,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        if (from < to) {
            val text = markdown.substring(from, to)
            val styleStart = builder.length
            builder.append(text)

            // Apply base text style with theme-aware color to regular text
            val spanStyle = baseTextStyle.toSpanStyle()
            builder.addStyle(
                if (spanStyle.color != Color.Unspecified) spanStyle else spanStyle.copy(color = colorScheme.onSurface),
                styleStart,
                builder.length
            )
        }
    }

    private fun renderToken(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        when (token.type) {
            TokenType.CODE_BLOCK -> renderCodeBlock(token, builder, colorScheme, baseTextStyle)
            TokenType.INLINE_CODE -> renderInlineCode(token, builder, colorScheme, baseTextStyle)
            TokenType.BOLD_LINK -> renderBoldLink(token, builder, colorScheme, baseTextStyle)
            TokenType.ITALIC_LINK -> renderItalicLink(token, builder, colorScheme, baseTextStyle)
            TokenType.LINK -> renderLink(token, builder, colorScheme, baseTextStyle)
            TokenType.CITATION -> renderCitation(token, builder, colorScheme, baseTextStyle)
            TokenType.BOLD -> renderBold(token, builder, colorScheme, baseTextStyle)
            TokenType.ITALIC -> renderItalic(token, builder, colorScheme, baseTextStyle)
            TokenType.HEADING -> renderHeading(token, builder, colorScheme, baseTextStyle)
            TokenType.LIST -> renderList(token, builder, colorScheme, baseTextStyle)
            TokenType.BLOCKQUOTE -> renderBlockquote(token, builder, colorScheme, baseTextStyle)
        }
    }

    private fun renderCodeBlock(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        val codeContent = token.groups[0].trim()
        val styleStart = builder.length
        val contentColor = baseTextStyle.color.takeIf { it != Color.Unspecified } ?: colorScheme.onSurface

        builder.append(codeContent)
        builder.addStyle(
            SpanStyle(
                background = colorScheme.surfaceContainerHighest,
                color = contentColor,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            ),
            styleStart,
            builder.length
        )
    }

    private fun renderInlineCode(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        val codeContent = token.groups[0]
        val styleStart = builder.length
        val contentColor = baseTextStyle.color.takeIf { it != Color.Unspecified } ?: colorScheme.onSurface

        builder.append(codeContent)
        builder.addStyle(
            SpanStyle(
                background = colorScheme.surfaceContainer,
                color = contentColor,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            ),
            styleStart,
            builder.length
        )
    }

    private fun renderLink(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        val (linkText, linkUrl) = token.groups
        val styleStart = builder.length

        builder.append(linkText)
        builder.addStyle(
            baseTextStyle.toSpanStyle().copy(
                color = colorScheme.primary,
                textDecoration = TextDecoration.Underline
            ),
            styleStart,
            builder.length
        )

        builder.addStringAnnotation(
            tag = "URL",
            annotation = linkUrl,
            start = styleStart,
            end = builder.length
        )
    }

    private fun renderBoldLink(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        val (linkText, linkUrl) = token.groups
        val styleStart = builder.length

        builder.append(linkText)
        builder.addStyle(
            baseTextStyle.toSpanStyle().copy(
                color = colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Bold
            ),
            styleStart,
            builder.length
        )

        builder.addStringAnnotation(
            tag = "URL",
            annotation = linkUrl,
            start = styleStart,
            end = builder.length
        )
    }

    private fun renderItalicLink(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        val (linkText, linkUrl) = token.groups
        val styleStart = builder.length

        builder.append(linkText)
        builder.addStyle(
            baseTextStyle.toSpanStyle().copy(
                color = colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                fontStyle = FontStyle.Italic
            ),
            styleStart,
            builder.length
        )

        builder.addStringAnnotation(
            tag = "URL",
            annotation = linkUrl,
            start = styleStart,
            end = builder.length
        )
    }

    private fun renderCitation(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        val citationNumber = token.groups[0] // This contains the number from [^1]
        val styleStart = builder.length

        // Create a unique citation ID for inline content
        val citationId = "citation_$citationNumber"

        // Add inline content placeholder for the circular citation
        builder.appendInlineContent(citationId, "[^$citationNumber]")

        // Add annotation to track this citation for click handling
        builder.addStringAnnotation(
            tag = "CITATION",
            annotation = citationNumber,
            start = styleStart,
            end = builder.length
        )
    }

    private fun renderBold(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        val boldContent = token.groups[0]
        val styleStart = builder.length

        builder.append(boldContent)
        val spanStyle = baseTextStyle.toSpanStyle()
        val contentColor = if (spanStyle.color != Color.Unspecified) spanStyle.color else colorScheme.onSurface
        builder.addStyle(
            baseTextStyle.toSpanStyle().copy(
                fontWeight = FontWeight.Bold,
                color = contentColor
            ),
            styleStart,
            builder.length
        )
    }

    private fun renderItalic(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        val italicContent = token.groups[0]
        val styleStart = builder.length

        builder.append(italicContent)
        val spanStyle = baseTextStyle.toSpanStyle()
        val contentColor = if (spanStyle.color != Color.Unspecified) spanStyle.color else colorScheme.onSurface
        builder.addStyle(
            baseTextStyle.toSpanStyle().copy(
                fontStyle = FontStyle.Italic,
                color = contentColor
            ),
            styleStart,
            builder.length
        )
    }

    private fun renderHeading(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        val headingLevel = token.groups[0].length
        val headingText = token.groups[1]
        val styleStart = builder.length
        val contentColor = baseTextStyle.color.takeIf { it != Color.Unspecified } ?: colorScheme.onSurface

        builder.append(headingText)

        val headingColor = when (headingLevel) {
            1 -> colorScheme.primary
            2 -> colorScheme.secondary
            3 -> colorScheme.tertiary
            else -> contentColor
        }

        val headingSize = when (headingLevel) {
            1 -> 26.sp
            2 -> 22.sp
            3 -> 18.sp
            else -> 16.sp
        }

        builder.addStyle(
            SpanStyle(
                fontSize = headingSize,
                fontWeight = FontWeight.Bold,
                color = headingColor
            ),
            styleStart,
            builder.length
        )
    }

    private fun renderList(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        val listItem = token.groups[0]
        val listMarker = token.groups.getOrNull(1) ?: "•"

        if (listMarker.matches(Regex("\\d+\\."))) {
            // For ordered lists, keep the number and period
            builder.append("$listMarker ")
        } else {
            // For unordered lists, use bullet points
            builder.append("• ")
        }

        renderNestedMarkdown(listItem, builder, colorScheme, baseTextStyle)
    }

    private fun renderBlockquote(
        token: MarkdownToken,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle
    ) {
        val quoteText = token.groups[0]

        val contentColor = baseTextStyle.color.takeIf { it != Color.Unspecified } ?: colorScheme.onSurface
        renderNestedMarkdown(quoteText, builder, colorScheme, baseTextStyle) { text, _, _ ->
            val styleStart = builder.length
            builder.append(text)
            builder.addStyle(
                baseTextStyle.toSpanStyle().copy(
                    background = colorScheme.surfaceContainer,
                    fontStyle = FontStyle.Italic,
                    color = contentColor
                ),
                styleStart,
                builder.length
            )
        }
    }

    /**
     * Renders nested markdown content.
     *
     * @param content The content to render
     * @param builder The AnnotatedString builder
     * @param colorScheme The Material Design color scheme
     * @param textRenderer Optional function to render plain text with custom styling
     */
    private fun renderNestedMarkdown(
        content: String,
        builder: AnnotatedString.Builder,
        colorScheme: ColorScheme,
        baseTextStyle: TextStyle,
        textRenderer: ((String, Int, Int) -> Unit)? = null
    ) {
        val nestedTokens = MarkdownTokenizer.tokenize(content)
        var currentIndex = 0

        nestedTokens.forEach { nestedToken ->
            if (nestedToken.start < currentIndex) return@forEach

            // Append text before the nested token
            if (nestedToken.start > currentIndex) {
                val textBefore = content.substring(currentIndex, nestedToken.start)
                if (textRenderer != null) {
                    textRenderer(textBefore, currentIndex, nestedToken.start)
                } else {
                    val styleStart = builder.length
                    val spanStyle = baseTextStyle.toSpanStyle()
                    builder.append(textBefore)
                    builder.addStyle(
                        if (spanStyle.color != Color.Unspecified) spanStyle else spanStyle.copy(color = colorScheme.onSurface),
                        styleStart,
                        builder.length
                    )
                }
            }

            // Render the nested token
            renderToken(nestedToken, builder, colorScheme, baseTextStyle)
            currentIndex = nestedToken.end
        }

        // Append any remaining text
        if (currentIndex < content.length) {
            val remainingText = content.substring(currentIndex)
            if (textRenderer != null) {
                textRenderer(remainingText, currentIndex, content.length)
            } else {
                val styleStart = builder.length
                val spanStyle = baseTextStyle.toSpanStyle()
                builder.append(remainingText)
                builder.addStyle(
                    if (spanStyle.color != Color.Unspecified) spanStyle else spanStyle.copy(color = colorScheme.onSurface),
                    styleStart,
                    builder.length
                )
            }
        }
    }
}
