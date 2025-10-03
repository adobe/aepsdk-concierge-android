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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.utils.markdown.CitationAnnotation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownParser
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownToken

/**
 * Renders list content with proper indentation and spacing.
 * 
 * @param listTokens List of [MarkdownToken] objects representing list items
 * @param onLinkClick Callback function for handling link clicks
 * @param citationAnnotations List of [CitationAnnotation] objects to apply to list items
 * @param modifier [Modifier] to be applied to the [Column] container
 */
@Composable
internal fun ConciergeResponseList(
    listTokens: List<MarkdownToken>,
    onLinkClick: (String) -> Unit,
    citationAnnotations: List<CitationAnnotation> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        listTokens.forEach { token ->
            ListItem(
                token = token,
                onLinkClick = onLinkClick,
                citationAnnotations = citationAnnotations
            )
        }
    }
}

/**
 * Renders a single list item with proper indentation and clickable links.
 * 
 * This composable processes a [MarkdownToken] and renders it as a list item with
 * citation annotations applied using [CitationStylingUtils].
 * 
 * @param token The [MarkdownToken] representing the list item
 * @param onLinkClick Callback function for handling link clicks within the list item
 * @param citationAnnotations List of [CitationAnnotation] objects to apply to this list item
 */
@Composable
private fun ListItem(
    token: MarkdownToken,
    onLinkClick: (String) -> Unit,
    citationAnnotations: List<CitationAnnotation> = emptyList()
) {
    val listItemContent = remember {  token.groups.firstOrNull() ?: "" }
    val listMarker = remember {  token.groups.getOrNull(1) ?: "•" }
    val indentationLevel = token.indentationLevel
    val annotatedString = MarkdownParser.parse(listItemContent)
    
    // Create citation annotations for list item content
    val listItemAnnotations = createListItemAnnotations(annotatedString.text, citationAnnotations)
    
    // Get theme colors for citation styling based on device's light/dark mode
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.primary
    
    // Apply citation annotations
    val finalAnnotatedString = CitationStylingUtils.applyCitationAnnotations(annotatedString, listItemAnnotations, backgroundColor, textColor)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        ListMarker(
            marker = listMarker,
            indentationLevel = indentationLevel
        )
        
            ClickableText(
                text = finalAnnotatedString,
                onClick = { offset ->
                    // Find the annotation at the clicked position
                    val allAnnotations = finalAnnotatedString.getStringAnnotations(start = 0, end = finalAnnotatedString.length)
                    val clickedAnnotation = allAnnotations.firstOrNull { annotation ->
                        offset >= annotation.start && offset <= annotation.end
                    }

                    // Handle the clicked annotation
                    clickedAnnotation?.let { annotation ->
                        onLinkClick(annotation.item)
                    }
                }
            )
    }
}

/**
 * Renders the list marker with proper indentation.
 * Unordered lists alternate between filled (•) and unfilled (◦) bullet points based on indentation level.
 * Level 0: • (filled), Level 1: ◦ (unfilled), Level 2: • (filled), etc.
 * Ordered lists (1., 2., etc.) keep their original numbering.
 */
@Composable
private fun ListMarker(
    marker: String,
    indentationLevel: Int
) {
    val displayMarker = if (marker.matches(Regex("\\d+\\."))) {
        // For numbered lists, keep the number and period
        marker
    } else {
        // For unordered lists, alternate between filled and unfilled bullet points, starting with filled
        val bulletPoint = if (indentationLevel % 2 == 0) "•" else "◦"
        "$bulletPoint "
    }

    Text(
        text = displayMarker,
        style = ConciergeStyles.messageBubbleStyle.textStyle,
        color = ConciergeStyles.messageBubbleStyle.botMessageTextColor,
        modifier = Modifier.padding(
            start = ListSpacing.BASE_INDENTATION + (indentationLevel * ListSpacing.INDENTATION_PER_LEVEL).dp
        )
    )
}

/**
 * Constants for list spacing and indentation.
 */
internal object ListSpacing {
    val BASE_INDENTATION = 16.dp
    val INDENTATION_PER_LEVEL = 16
    val END_PADDING = 16.dp
}

/**
 * Creates citation annotations for the given list item content.
 * 
 * This function searches for citation numbers within the list item content and creates
 * corresponding [CitationAnnotation] objects by matching them with the provided
 * annotation list. It uses regex pattern matching to find citation numbers.
 * 
 * @param listItemContent The text content of the list item to search for citations
 * @param allAnnotations List of all available [CitationAnnotation] objects to match against
 * @return List of [CitationAnnotation] objects that apply to this specific list item
 */
private fun createListItemAnnotations(
    listItemContent: String,
    allAnnotations: List<CitationAnnotation>
): List<CitationAnnotation> {
    val annotations = mutableListOf<CitationAnnotation>()
    
    // Look for citation numbers in the list item content
    val citationPattern = Regex("\\d+")
    val matches = citationPattern.findAll(listItemContent)
    
    matches.forEach { match ->
        val citationNumber = match.value.toIntOrNull()
        if (citationNumber != null) {
            // Find the corresponding annotation from the original list
            val originalAnnotation = allAnnotations.find { it.citationNumber == citationNumber }
            if (originalAnnotation != null) {
                annotations.add(
                    CitationAnnotation(
                        citationNumber = citationNumber,
                        startIndex = match.range.first,
                        endIndex = match.range.last + 1,
                        url = originalAnnotation.url
                    )
                )
            }
        }
    }
    
    return annotations
}
