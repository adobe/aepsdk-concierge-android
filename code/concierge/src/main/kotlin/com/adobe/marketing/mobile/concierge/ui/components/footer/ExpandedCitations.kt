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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.Citation
import com.adobe.marketing.mobile.services.ServiceProvider

/**
 * Component that displays a list of citations.
 *
 * @param modifier Optional [Modifier] for this component.
 * @param citations List of [Citation]s to display.
 * @param expanded Current expanded state.
 */
@Composable
internal fun ExpandedCitations(
    modifier: Modifier = Modifier,
    citations: List<Citation>,
    expanded: Boolean
) {
    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Column {
                citations.forEachIndexed { index, citation ->
                    CitationItem(
                        citation = citation,
                        index = index + 1
                    )
                }
            }
        }
    }
}

/**
 * An individual citation item component.
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
            .padding(8.dp)
            .clickable(
                enabled = !citation.url.isNullOrBlank(),
                onClick = {
                    citation.url?.let { url ->
                        ServiceProvider.getInstance().uriService.openUri(url)
                    }
                }
            )
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "$index.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                // Title with clickable styling if URL exists
                if (!citation.url.isNullOrBlank()) {
                    val annotatedString = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(citation.title)
                        }
                    }
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = citation.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                citation.description?.let { description ->
                    if (description.isNotBlank()) {
                        Spacer(modifier = Modifier.padding(top = 2.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}