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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.Citation
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
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = tween(200)),
        exit = shrinkVertically(animationSpec = tween(200))
    ) {
        Column(modifier = modifier) {
            citations.forEachIndexed { index, citation ->
                CitationItem(
                    citation = citation,
                    index = index + 1
                )
                // Add separator line between items
                if (index < citations.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

/**
 * A citation item component that displays a title and clickable url.
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Citation title
        Text(
            text = "$index. ${citation.title}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // URL with clickable styling if it exists
        if (!citation.url.isNullOrBlank()) {
            Text(
                text = citation.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable {
                        citation.url.let { url ->
                            ServiceProvider.getInstance().uriService.openUri(url)
                        }
                    }
            )
        }
    }
}