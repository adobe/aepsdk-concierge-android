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

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import com.adobe.marketing.mobile.concierge.network.Citation

/**
 * UI-specific utility functions for handling citation-related operations in Compose.
 */
internal object CitationUiUtils {

    /**
     * Creates an inline text content map for circular citations.
     *
     * @param uniqueSources List of citation sources
     * @param badgeSize Size of the citation badge
     * @param context Android context for handling URL clicks
     * @return Map of citation IDs to InlineTextContent for embedding circular citation badges
     */
    internal fun createInlineContentMap(
        uniqueSources: List<Citation>,
        badgeSize: Dp,
        context: Context
    ): Map<String, InlineTextContent> {
        val inlineContentMap = mutableMapOf<String, InlineTextContent>()

        val placeholderSize = with(Density(1f, 1f)) { badgeSize.toSp() }

        uniqueSources.forEach { source ->
            val citationNumber = source.citationNumber ?: return@forEach
            val citationId = "citation_$citationNumber"

            inlineContentMap[citationId] = InlineTextContent(
                placeholder = Placeholder(
                    width = placeholderSize,
                    height = placeholderSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                CircularCitation(
                    citationNumber = citationNumber,
                    onClick = {
                        source.url?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }

        return inlineContentMap
    }
}
