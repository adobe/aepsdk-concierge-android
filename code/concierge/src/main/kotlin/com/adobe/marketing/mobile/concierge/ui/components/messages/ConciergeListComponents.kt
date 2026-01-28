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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
 * @param modifier [Modifier] to be applied to the [Column] container
 */
@Composable
internal fun ConciergeResponseList(
    listTokens: List<MarkdownToken>,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.wrapContentHeight()) {
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
 *
 * This composable processes a [MarkdownToken] and renders it as a list item.
 *
 * @param token The [MarkdownToken] representing the list item
 * @param onLinkClick Callback function for handling link clicks within the list item
 */
@Composable
private fun ListItem(
    token: MarkdownToken,
    onLinkClick: (String) -> Unit
) {
    val listItemContent = remember(token) { token.groups.firstOrNull() ?: "" }
    val listMarker = remember(token) { token.groups.getOrNull(1) ?: "•" }
    val indentationLevel = token.indentationLevel

    // Parse markdown to get the rendered text
    val annotatedString = MarkdownParser.parse(listItemContent)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        ListMarker(
            marker = listMarker,
            indentationLevel = indentationLevel
        )

        ClickableText(
            text = annotatedString,
            onLinkClick = onLinkClick,
            modifier = Modifier
                .weight(1f, fill = true)
                .wrapContentHeight()
                .padding(end = ListSpacing.END_PADDING)
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
