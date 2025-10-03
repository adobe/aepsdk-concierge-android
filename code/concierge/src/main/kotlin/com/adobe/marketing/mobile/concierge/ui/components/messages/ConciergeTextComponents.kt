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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownParser
import androidx.core.net.toUri
import com.adobe.marketing.mobile.concierge.utils.markdown.CitationAnnotation

/**
 * Renders brand concierge content with clickable links and citation annotations.
 * 
 * This composable processes markdown text and applies citation styling using
 * [CitationStylingUtils].
 * 
 * @param text The markdown text content to render
 * @param citationAnnotations Pre-computed citation annotations to apply
 * @param modifier [Modifier] to be applied to the [ClickableText] component
 */
@Composable
internal fun ConciergeResponseText(
    text: String,
    citationAnnotations: List<CitationAnnotation> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Get theme colors for citation styling
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.primary
    
    val markdownAnnotatedString = MarkdownParser.parse(text)
    
    val finalAnnotatedString = remember(markdownAnnotatedString, citationAnnotations, backgroundColor, textColor) {
        // Apply citation annotations if available
        CitationStylingUtils.applyCitationAnnotations(markdownAnnotatedString, citationAnnotations, backgroundColor, textColor)
    }
    
    ClickableText(
        text = finalAnnotatedString,
        modifier = modifier.fillMaxWidth(),
        onClick = { offset ->
            // Find the annotation at the clicked position
            val allAnnotations = finalAnnotatedString.getStringAnnotations(start = 0, end = finalAnnotatedString.length)
            val clickedAnnotation = allAnnotations.firstOrNull { annotation ->
                offset >= annotation.start && offset <= annotation.end
            }

            // Handle the clicked annotation
            clickedAnnotation?.let { annotation ->
                val intent = Intent(Intent.ACTION_VIEW, clickedAnnotation.item.toUri())
                context.startActivity(intent)
            }
        },
        onLinkClick = { url ->
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(intent)
        }
    )
}

/**
 * Reusable composable for rendering text with clickable links.
 */
@Composable
internal fun ClickableText(
    text: androidx.compose.ui.text.AnnotatedString,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    Text(
        text = text,
        onTextLayout = { textLayoutResult = it },
        modifier = modifier
            .padding(end = ListSpacing.END_PADDING)
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
