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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownTokenizer
import com.adobe.marketing.mobile.concierge.utils.markdown.TokenType
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownToken
import com.adobe.marketing.mobile.concierge.utils.markdown.CitationAnnotator
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import androidx.core.net.toUri
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
    modifier: Modifier = Modifier
) {
    // Apply citation annotations to the complete text first
    val annotatedText = remember(text, sources) {
        if (sources.isNotEmpty()) {
            CitationAnnotator.annotateText(text, sources)
        } else {
            CitationAnnotator.annotateText(text, emptyList())
        }
    }
    
    // Show thinking animation when text is empty
    if (text.isEmpty()) {
        ConciergeThinking(modifier = modifier)
        return
    }
    
    val tokens = remember(annotatedText.text) { MarkdownTokenizer.tokenize(annotatedText.text) }

    val listTokens = remember(tokens) { 
        tokens.filter { it.type == TokenType.LIST }
    }
    
    if (listTokens.isNotEmpty()) {
        ConciergeResponseWithLists(
            text = annotatedText.text,
            listTokens = listTokens,
            citationAnnotations = annotatedText.citationAnnotations,
            modifier = modifier
        )
    } else {
        ConciergeResponseText(
            text = annotatedText.text,
            citationAnnotations = annotatedText.citationAnnotations,
            modifier = modifier
        )
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
    citationAnnotations: List<com.adobe.marketing.mobile.concierge.utils.markdown.CitationAnnotation> = emptyList(),
    modifier: Modifier = Modifier
) {
    val style = ConciergeStyles.messageBubbleStyle
    val context = LocalContext.current
    val contentSegments = remember(text, listTokens) {
        ContentSegmentParser.createSegments(text, listTokens)
    }

    Column(modifier = modifier) {
        contentSegments.forEach { segment ->
            Spacer(modifier = Modifier.height(style.segmentSpacing))
            when (segment) {
                is ContentSegment.Text -> {
                    // Adjust citation annotations for this text segment
                    val segmentAnnotations = adjustAnnotationsForSegment(
                        citationAnnotations, 
                        segment.startIndex, 
                        segment.endIndex
                    )
                    
                    ConciergeResponseText(
                        text = segment.content,
                        citationAnnotations = segmentAnnotations,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is ContentSegment.List -> {
                    ConciergeResponseList(
                        listTokens = segment.tokens,
                        onLinkClick = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        },
                        citationAnnotations = citationAnnotations
                    )
                }
            }
        }
    }
}

/**
 * Adjusts citation annotations for a text segment by offsetting their positions
 * relative to the segment's start position in the complete text. This is necessary
 * because annotations indexes are based on the full markdown text, but need to be applied
 * to individual segments when rendering.
 */
private fun adjustAnnotationsForSegment(
    annotations: List<com.adobe.marketing.mobile.concierge.utils.markdown.CitationAnnotation>,
    segmentStartIndex: Int,
    segmentEndIndex: Int
): List<com.adobe.marketing.mobile.concierge.utils.markdown.CitationAnnotation> {
    return annotations.filter { annotation ->
        // Check if annotation overlaps with this segment
        annotation.startIndex < segmentEndIndex && annotation.endIndex > segmentStartIndex
    }.map { annotation ->
        // Adjust annotation positions relative to segment start
        annotation.copy(
            startIndex = maxOf(0, annotation.startIndex - segmentStartIndex),
            endIndex = minOf(segmentEndIndex - segmentStartIndex, annotation.endIndex - segmentStartIndex)
        )
    }
}
