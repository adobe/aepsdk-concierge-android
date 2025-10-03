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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownParser
import androidx.core.net.toUri
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.utils.citation.CitationUtils

/**
 * Renders concierge response text with markdown formatting and circular citation components.
 */
@Composable
internal fun ConciergeResponseText(
    text: String,
    uniqueSources: List<Citation> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val style = ConciergeStyles.citationBadgeStyle
    
    // Parse markdown first to get the rendered text with inline content placeholders
    val markdownAnnotatedString = MarkdownParser.parse(text)
    
    // Create inline content map for circular citations
    val inlineContentMap = CitationUtils.createInlineContentMap(
        uniqueSources,
        style.size,
        context
    )
    
    ClickableText(
        text = markdownAnnotatedString,
        inlineContent = inlineContentMap,
        onLinkClick = { url ->
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(intent)
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(end = ListSpacing.END_PADDING)
    )
}

/**
 * Reusable composable for rendering text with clickable links and optional inline content.
 * 
 * @param text The annotated string to render
 * @param onLinkClick Callback for handling link clicks
 * @param modifier Optional modifier for the component
 * @param inlineContent Optional map of inline content for embedded composables (e.g., citations)
 */
@Composable
internal fun ClickableText(
    text: androidx.compose.ui.text.AnnotatedString,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    inlineContent: Map<String, InlineTextContent> = emptyMap()
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    Text(
        text = text,
        inlineContent = inlineContent,
        onTextLayout = { textLayoutResult = it },
        modifier = modifier
            .pointerInput(text) {
                detectTapGestures { tapOffsetPosition ->
                    // Get the character offset at the tap position
                    textLayoutResult?.let { layoutResult ->
                        val offset = layoutResult.getOffsetForPosition(tapOffsetPosition)
                        
                        // Find URL annotation at the clicked position
                        text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()
                            ?.let { annotation ->
                                onLinkClick(annotation.item)
                            }
                    }
                }
            }
    )
}
