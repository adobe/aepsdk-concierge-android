package com.adobe.marketing.mobile.concierge.utils.citation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.ui.components.messages.CircularCitation

/**
 * Utility functions for handling citation-related operations.
 */
internal object CitationUtils {

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

    /**
     * Creates a list of unique citations.
     * Filters out citations without citation numbers and deduplicates by citation number.
     * 
     * @param citations List of citations to process
     * @return List of unique citations sorted by citation number
     */
    internal fun createUniqueSources(citations: List<Citation>): List<Citation> {
        if (citations.isEmpty()) {
            return emptyList()
        }
        
        return citations
            .filter { it.citationNumber != null }
            .groupBy { it.citationNumber }
            .map { (_, sources) -> sources.first() }
            .sortedBy { it.citationNumber }
    }

    /**
     * Converts citation number to the markdown format [^1], [^2], etc.
     * 
     * @param citationNumber The citation number to convert
     * @return The citation symbol in markdown format
     */
    internal fun getCitationSymbol(citationNumber: Int): String {
        return "[^$citationNumber]"
    }

    /**
     * Inserts citation numbers into markdown text at the specified positions.
     * Citation numbers are inserted at the end_index position provided by the backend.
     * 
     * @param text The original markdown text
     * @param sources List of citation sources sorted by start index (in reverse order)
     * @return The markdown text with citation numbers inserted
     */
    internal fun insertCitationNumbersInMarkdown(text: String, sources: List<Citation>): String {
        val result = StringBuilder(text)
        
        // Insert citations in reverse order to maintain indices
        sources.forEach { source ->
            val citationNumber = source.citationNumber ?: return@forEach
            val endIndex = source.endIndex ?: return@forEach
            val citationSymbol = getCitationSymbol(citationNumber)
            
            if (endIndex <= result.length) {
                result.insert(endIndex, citationSymbol)
            }
        }
        
        return result.toString()
    }
}