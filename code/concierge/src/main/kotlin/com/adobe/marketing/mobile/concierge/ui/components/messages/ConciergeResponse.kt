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
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeCitationsBehavior
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.toComposeColor
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.LinkHint

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
    modifier: Modifier = Modifier,
    sources: List<Citation> = emptyList(),
    linkHints: List<LinkHint> = emptyList(),
    handleLink: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val style = ConciergeStyles.citationBadgeStyle
    val colorScheme = ConciergeTheme.colorScheme

    Crossfade(
        targetState = text.isEmpty(),
        animationSpec = tween(durationMillis = 200)
    ) { isEmpty ->
        // Apply citation annotations to the complete text first
        val annotatedText = remember(text, sources) {
            CitationAnnotator.annotateText(text.trimEnd(), sources)
        }

        // Create inline content maps once for all child components to share
        val citationInlineContentMap = remember(annotatedText.uniqueSources, handleLink) {
            CitationUiUtils.createInlineContentMap(
                annotatedText.uniqueSources,
                style.size,
                context,
                handleLink
            )
        }
        val citationsBehavior = ConciergeTheme.tokens?.behavior?.citations ?: ConciergeCitationsBehavior()

        // Compute style values from linkIconStyle, falling back to theme link color then primary
        val iconStyle = citationsBehavior.linkIconStyle
        val iconSize = iconStyle?.size?.dp ?: 16.dp
        val iconSpacing = iconStyle?.spacing?.dp ?: 2.dp
        val iconColor = iconStyle?.color?.toComposeColor()
            ?: ConciergeTheme.colors.messageConciergeLink
            ?: colorScheme.primary

        // Augment linkHints to cover every URL in the text when showLinkIcon is enabled.
        // Non-hint URLs receive kind "default" so they get the default icon asset.
        val effectiveLinkHints = remember(text, linkHints, citationsBehavior.showLinkIcon) {
            LinkHintUiUtils.augmentedLinkHints(text, linkHints, citationsBehavior.showLinkIcon)
        }

        val linkHintInlineContentMap = remember(effectiveLinkHints, iconColor, iconSize, iconSpacing, citationsBehavior, handleLink) {
            LinkHintUiUtils.createLinkHintInlineContentMap(
                linkHints = effectiveLinkHints,
                iconColor = iconColor,
                citationsBehavior = citationsBehavior,
                context = context,
                iconSize = iconSize,
                iconSpacing = iconSpacing,
                handleLink = handleLink
            )
        }
        val inlineContentMap = remember(citationInlineContentMap, linkHintInlineContentMap) {
            citationInlineContentMap + linkHintInlineContentMap
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
                    linkHints = effectiveLinkHints,
                    handleLink = handleLink,
                    modifier = modifier
                )
            } else {
                ConciergeResponseText(
                    text = annotatedText.text,
                    uniqueSources = annotatedText.uniqueSources,
                    inlineContentMap = inlineContentMap,
                    linkHints = effectiveLinkHints,
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
    linkHints: List<LinkHint> = emptyList(),
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
                        linkHints = linkHints,
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
                        linkHints = linkHints,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
