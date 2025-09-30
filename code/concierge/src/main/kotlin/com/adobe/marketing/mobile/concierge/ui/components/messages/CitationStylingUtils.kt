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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.utils.markdown.CitationAnnotation

/**
 * Utility object for handling citation annotation styling and processing.
 */
internal object CitationStylingUtils {
    
    /**
     * Creates a [SpanStyle] for citation annotations with the provided colors.
     * 
     * @param backgroundColor The background color for citation annotations
     * @param textColor The text color for citation annotations
     * @return A [SpanStyle] configured for citation display with theme-aware colors
     */
    fun createCitationSpanStyle(
        backgroundColor: Color,
        textColor: Color
    ): SpanStyle {
        return SpanStyle(
            textDecoration = TextDecoration.None,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            fontFamily = FontFamily.Default,
            background = backgroundColor,
            color = textColor
        )
    }
    
    /**
     * Applies citation annotations to an [AnnotatedString].
     * 
     * This function processes a list of [CitationAnnotation] objects and applies
     * styling and clickable functionality to the corresponding text ranges in
     * the provided [AnnotatedString].
     * 
     * @param annotatedString The base annotated string to modify
     * @param citationAnnotations List of [CitationAnnotation] objects to apply
     * @param backgroundColor Background color for citation styling
     * @param textColor Text color for citation styling
     * @return [AnnotatedString] with citation styling and clickable annotations applied
     */
    fun applyCitationAnnotations(
        annotatedString: AnnotatedString,
        citationAnnotations: List<CitationAnnotation>,
        backgroundColor: Color,
        textColor: Color
    ): AnnotatedString {
        if (citationAnnotations.isEmpty()) {
            return annotatedString
        }
        
        val citationStyle = createCitationSpanStyle(backgroundColor, textColor)
        val builder = AnnotatedString.Builder(annotatedString)
        
        citationAnnotations.forEach { citationAnnotation ->
            // Check if the annotation is within the text bounds
            if (citationAnnotation.startIndex < annotatedString.length &&
                citationAnnotation.endIndex <= annotatedString.length) {
                
                // Apply custom style for citation annotations
                builder.addStyle(
                    style = citationStyle,
                    start = citationAnnotation.startIndex,
                    end = citationAnnotation.endIndex
                )
                
                // Add clickable annotation if URL is available
                citationAnnotation.url?.let { url ->
                    builder.addStringAnnotation(
                        tag = "CITATION",
                        annotation = url,
                        start = citationAnnotation.startIndex,
                        end = citationAnnotation.endIndex
                    )
                }
            }
        }
        
        return builder.toAnnotatedString()
    }
}
