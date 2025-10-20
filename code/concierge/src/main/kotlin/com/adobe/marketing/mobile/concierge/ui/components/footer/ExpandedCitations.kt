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

package com.adobe.marketing.mobile.concierge.ui.components.footer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.utils.citation.CitationUtils
import com.adobe.marketing.mobile.services.ServiceProvider

/**
 * Component that displays a list of citations as individual accordion items.
 * Each citation can be expanded/collapsed independently.
 *
 * @param modifier Optional [Modifier] for this component.
 * @param citations List of [Citation]s to display.
 * @param expanded Current expanded state for the overall container.
 */
@Composable
internal fun ExpandedCitations(
    modifier: Modifier = Modifier,
    citations: List<Citation>,
    expanded: Boolean
) {
    // Get unique sources with proper citation numbers
    val uniqueSources: List<Citation> = remember(citations) {
        CitationUtils.createUniqueSources(citations)
    }
    val style = ConciergeStyles.citationStyle
    
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = tween(style.expandAnimationDuration)),
        exit = shrinkVertically(animationSpec = tween(style.collapseAnimationDuration))
    ) {
        Column(modifier = modifier) {
            uniqueSources.forEachIndexed { index, citation ->
                CitationItem(
                    citation = citation,
                    index = citation.citationNumber ?: (index + 1)
                )
                // Add separator line between items
                if (index < uniqueSources.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(style.separatorHeight)
                            .background(style.separatorColor)
                    )
                }
            }
        }
    }
}

/**
 * A citation item component that displays a source link.
 *
 * @param modifier Optional [Modifier] for this component.
 * @param citation The [Citation] to display.
 * @param index The index number of the citation in the list.
 */
@Composable
internal fun CitationItem(
    modifier: Modifier = Modifier,
    citation: Citation,
    index: Int
) {
    val style = ConciergeStyles.citationStyle
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(style.containerPadding)
    ) {
        // Citation index number
        Text(
            text = "$index. ",
            style = style.textStyle,
            color = style.textColor
        )
        
        // Source link, clickable if URL is present
        Text(
            text = citation.title,
            style = style.textStyle,
            maxLines = style.textLength,
            color = if (!citation.url.isNullOrBlank()) style.urlColor else style.textColor,
            textDecoration = if (!citation.url.isNullOrBlank()) TextDecoration.Underline else null,
            modifier = Modifier.then(
                if (!citation.url.isNullOrBlank()) {
                    Modifier.clickable {
                        citation.url?.let { url ->
                            ServiceProvider.getInstance().uriService.openUri(url)
                        }
                    }
                } else {
                    Modifier
                }
            )
        )
    }
}