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

import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log


/**
 * Utility class for annotating text with citation numbers based on source positions.
 * This class handles the insertion of citation numbers (e.g. ¹, ², ³) into text
 * at the positions specified by start_index and end_index from the sources.
 */
object CitationAnnotator {
    
    /**
     * Annotates text with citation numbers based on the provided sources.
     * 
     * @param text The original text to annotate
     * @param sources List of sources with citation positions
     * @return AnnotatedText containing the annotated text and unique sources
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
            return AnnotatedText(text, emptyList())
        }
        
        // Sort sources by start index to process them in order
        val sortedSources = validSources.sortedBy { it.startIndex!! }
        
        // Create citation markers and track unique sources
        val citationMarkers = mutableListOf<CitationMarker>()
        val uniqueSources = mutableMapOf<Int, Citation>()
        
        sortedSources.forEach { source ->
            val citationNumber = source.citationNumber!!
            val startIndex = source.startIndex!!
            val endIndex = source.endIndex!!
            
            // Store unique sources by citation number
            uniqueSources[citationNumber] = source
            
            // Create citation marker
            citationMarkers.add(
                CitationMarker(
                    citationNumber = citationNumber,
                    startIndex = startIndex,
                    endIndex = endIndex
                )
            )
        }
        
        // Insert citation numbers into the text
        val annotatedText = insertCitationNumbers(text, citationMarkers)
        
        // Create citation annotations for clickable behavior
        val finalAnnotations = createCitationAnnotations(annotatedText, citationMarkers, uniqueSources)
        
        
        return AnnotatedText(
            text = annotatedText,
            uniqueSources = uniqueSources.values.toList().sortedBy { it.citationNumber },
            citationAnnotations = finalAnnotations
        )
    }
    
    /**
     * Creates citation annotations for clickable behavior.
     * This function finds the positions of citation numbers in the annotated text
     * and creates annotations with their URLs.
     */
    private fun createCitationAnnotations(
        annotatedText: String, 
        markers: List<CitationMarker>, 
        uniqueSources: Map<Int, Citation>
    ): List<CitationAnnotation> {
        val annotations = mutableListOf<CitationAnnotation>()
        
        markers.forEach { marker ->
            val citationNumber = marker.citationNumber
            val citationSymbol = getCitationSymbol(citationNumber)
            val source = uniqueSources[citationNumber]
            
            
            // Find the position of this citation symbol in the annotated text
            // Search from the beginning since the annotated text may be different from the original
            val symbolIndex = annotatedText.indexOf(citationSymbol)
            if (symbolIndex != -1) {
                
                annotations.add(
                    CitationAnnotation(
                        citationNumber = citationNumber,
                        startIndex = symbolIndex,
                        endIndex = symbolIndex + citationSymbol.length,
                        url = source?.url
                    )
                )
            } else {
                Log.debug(ConciergeConstants.EXTENSION_NAME, "CitationAnnotator", 
                    "Could not find citation $citationNumber ('$citationSymbol') in annotated text")
            }
        }
        
        return annotations
    }
    
    /**
     * Inserts citation numbers into the text at the specified positions.
     * Citation numbers are inserted after the end_index of each citation.
     */
    private fun insertCitationNumbers(text: String, markers: List<CitationMarker>): String {
        val result = StringBuilder(text)
        
        // Process markers in reverse order to maintain correct indices
        markers.reversed().forEach { marker ->
            val citationNumber = marker.citationNumber
            val citationSymbol = getCitationSymbol(citationNumber)
            
            // Insert citation symbol after the end index
            result.insert(marker.endIndex, citationSymbol)
        }
        
        return result.toString()
    }
    
    /**
     * Converts citation number to regular digit string.
     * Uses regular digits (1, 2, 3) instead of superscript Unicode characters
     * to match the screenshot appearance.
     */
    private fun getCitationSymbol(citationNumber: Int): String {
        return citationNumber.toString()
    }
}

/**
 * Represents a citation marker with its position in the text.
 */
private data class CitationMarker(
    val citationNumber: Int,
    val startIndex: Int,
    val endIndex: Int
)

/**
 * Result of text annotation containing the annotated text and unique sources.
 */
internal data class AnnotatedText(
    val text: String,
    val uniqueSources: List<Citation>,
    val citationAnnotations: List<CitationAnnotation> = emptyList()
)

/**
 * Represents a citation annotation with its position and URL.
 */
internal data class CitationAnnotation(
    val citationNumber: Int,
    val startIndex: Int,
    val endIndex: Int,
    val url: String?
)
