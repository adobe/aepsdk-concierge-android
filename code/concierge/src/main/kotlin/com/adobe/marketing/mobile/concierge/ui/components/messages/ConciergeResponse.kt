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

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownTokenizer
import com.adobe.marketing.mobile.concierge.utils.markdown.TokenType
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownToken
import com.adobe.marketing.mobile.concierge.utils.markdown.CitationAnnotator
import androidx.core.net.toUri
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.network.Citation

/**
 * Component that renders brand concierge responses containing markdown text
 * with proper styling and clickable links.
 *
 * This composable determines whether to render text as plain content
 * or as lists based on the presence of markdown list tokens. It uses
 * [CitationAnnotator] to process citations and applies them across both text and list
 * rendering modes.
 *
 * @param text The markdown text to be rendered
 * @param sources Optional list of [Citation] objects for generating citation annotations
 * @param modifier Optional [Modifier] for the text component
 */
@Composable
internal fun ConciergeResponse(
    text: String,
    sources: List<Citation> = emptyList(),
    handleLink: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val style = ConciergeStyles.citationBadgeStyle

    Crossfade(
        targetState = text.isEmpty(),
        animationSpec = tween(durationMillis = 200)
    ) { isEmpty ->
        // Apply citation annotations to the complete text first
        val annotatedText = remember(text, sources) {
            CitationAnnotator.annotateText(text, sources)
        }

        // Create inline content map once for all child components to share
        // This avoids recreating the map for each list item
        val inlineContentMap = remember(annotatedText.uniqueSources, handleLink) {
            CitationUiUtils.createInlineContentMap(
                annotatedText.uniqueSources,
                style.size,
                context,
                handleLink
            )
        }

        if (isEmpty) {
            ConciergeThinking(modifier = modifier)
        } else {
            val tokens = remember(annotatedText.text) { MarkdownTokenizer.tokenize(annotatedText.text) }
            val listTokens = remember(tokens) {
                tokens.filter { it.type == TokenType.LIST }
            }

            if (listTokens.isNotEmpty()) {
                ConciergeResponseWithLists(
                    text = annotatedText.text,
                    listTokens = listTokens,
                    uniqueSources = annotatedText.uniqueSources,
                    inlineContentMap = inlineContentMap,
                    handleLink = handleLink,
                    modifier = modifier
                )
            } else {
                ConciergeResponseText(
                    text = annotatedText.text,
                    uniqueSources = annotatedText.uniqueSources,
                    inlineContentMap = inlineContentMap,
                    handleLink = handleLink,
                    modifier = modifier
                )
            }
        }
    }
}

/**
 * Renders concierge response content that contains lists, integrating list content within the
 * text flow while maintaining uniform indentation.
 */
@Composable
private fun ConciergeResponseWithLists(
    text: String,
    listTokens: List<MarkdownToken>,
    uniqueSources: List<Citation> = emptyList(),
    inlineContentMap: Map<String, InlineTextContent> = emptyMap(),
    handleLink: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val style = ConciergeStyles.messageBubbleStyle
    val context = LocalContext.current
    val contentSegments = remember(text, listTokens) {
        ContentSegmentParser.createSegments(text, listTokens)
    }
    val linkHandler = handleLink ?: { url ->
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        contentSegments.forEach { segment ->
            Spacer(modifier = Modifier.height(style.segmentSpacing))
            when (segment) {
                is ContentSegment.Text -> {
                    ConciergeResponseText(
                        text = segment.content,
                        uniqueSources = uniqueSources,
                        inlineContentMap = inlineContentMap,
                        handleLink = handleLink,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is ContentSegment.List -> {
                    ConciergeResponseList(
                        listTokens = segment.tokens,
                        handleLink = linkHandler,
                        uniqueSources = uniqueSources,
                        inlineContentMap = inlineContentMap,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
