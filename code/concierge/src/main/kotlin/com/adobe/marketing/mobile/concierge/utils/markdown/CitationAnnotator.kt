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

package com.adobe.marketing.mobile.concierge.utils.markdown

import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.utils.citation.CitationUtils
import com.adobe.marketing.mobile.services.Log

/**
 * Utility class for annotating text with citation numbers based on source positions.
 */
internal object CitationAnnotator {
    private const val TAG = "CitationAnnotator"

    /**
     * Annotates text with citation numbers based on the provided sources.
     *
     * @param text The original markdown text to annotate
     * @param sources List of sources with citation positions based on the original markdown text
     * @return AnnotatedText containing the annotated text with citation numbers inserted
     */
    internal fun annotateText(text: String, sources: List<Citation>): AnnotatedText {
        if (sources.isEmpty()) {
            return AnnotatedText(text, emptyList())
        }

        // Filter sources that have valid citation numbers and positions
        val validSources = sources.filter { source ->
            source.citationNumber != null &&
                    source.startIndex != null &&
                    source.endIndex != null &&
                    source.startIndex >= 0 &&
                    source.endIndex > source.startIndex &&
                    source.endIndex <= text.length
        }

        if (validSources.isEmpty()) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "No valid sources found for text of length ${text.length}"
            )
            return AnnotatedText(text, emptyList())
        }

        // Sort sources by start index in reverse order to maintain correct indices when inserting
        val sortedSources = validSources.sortedByDescending { it.startIndex }

        // Insert citation numbers into the original markdown text
        val annotatedText = CitationUtils.insertCitationNumbersInMarkdown(text, sortedSources)

        return AnnotatedText(
            text = annotatedText,
            uniqueSources = CitationUtils.createUniqueSources(validSources)
        )
    }
}

/**
 * Result of text annotation containing the annotated text with citation markers and unique sources.
 */
internal data class AnnotatedText(
    val text: String,
    val uniqueSources: List<Citation>
)
