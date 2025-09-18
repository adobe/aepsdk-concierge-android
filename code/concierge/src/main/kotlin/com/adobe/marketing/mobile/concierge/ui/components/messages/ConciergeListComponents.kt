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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownParser
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownToken

/**
 * Renders list content with proper indentation and spacing.
 */
@Composable
internal fun ConciergeResponseList(
    listTokens: List<MarkdownToken>,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        listTokens.forEach { token ->
            ListItem(
                token = token,
                onLinkClick = onLinkClick
            )
        }
    }
}

/**
 * Renders a single list item with proper indentation and clickable links.
 */
@Composable
private fun ListItem(
    token: MarkdownToken,
    onLinkClick: (String) -> Unit
) {
    val listItemContent = token.groups.firstOrNull() ?: ""
    val listMarker = token.groups.getOrNull(1) ?: "•"
    val indentationLevel = token.indentationLevel
    val annotatedString = MarkdownParser.parse(listItemContent)
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        ListMarker(
            marker = listMarker,
            indentationLevel = indentationLevel
        )
        
        ClickableText(
            text = annotatedString,
            onLinkClick = onLinkClick,
            onTextLayout = { textLayoutResult = it }
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
    
    BasicText(
        text = displayMarker,
        style = TextStyle(color = MaterialTheme.colorScheme.onSurface),
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
