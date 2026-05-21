/*
 * Copyright 2026 Adobe. All rights reserved.
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.adobe.marketing.mobile.concierge.network.LinkHint
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeCitationsBehavior
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownRenderer

/**
 * UI-specific utility functions for rendering inline link-hint icons within markdown text.
 */
internal object LinkHintUiUtils {

    private val MARKDOWN_LINK_REGEX = Regex("""\[([^\]]*)\]\(([^)]+)\)""")

    /**
     * Extracts all link URLs from a raw markdown string.
     */
    internal fun extractMarkdownLinkUrls(text: String): List<String> =
        MARKDOWN_LINK_REGEX.findAll(text).map { it.groupValues[2].trim() }.toList()

    /**
     * Builds an augmented list of [LinkHint]s that covers every link URL found in [text].
     *
     * URLs already present in [linkHints] retain their original kind (phone, store, etc.).
     * All other URLs receive kind `"default"` so the renderer appends the default icon.
     * Returns an empty list when [showLinkIcon] is false.
     */
    internal fun augmentedLinkHints(
        text: String,
        linkHints: List<LinkHint>,
        showLinkIcon: Boolean
    ): List<LinkHint> {
        if (!showLinkIcon) return emptyList()
        val hintsByHref = linkHints.associateBy { it.href }
        return extractMarkdownLinkUrls(text)
            .distinct()
            .map { url -> hintsByHref[url] ?: LinkHint(kind = "default", href = url) }
    }

    /**
     * Creates an inline text content map for link hint icons.
     *
     * For each hint two entries are emitted:
     * - A transparent spacer keyed by [MarkdownRenderer.linkSpacingId] — controls the gap
     *   between the link text and the icon.
     * - An icon keyed by [MarkdownRenderer.linkIconId] — the actual icon with its click handler.
     *
     * The icon asset is resolved per-kind from [citationsBehavior]:
     * - `phone`  → [ConciergeCitationsBehavior.phoneIcon]
     * - `store`  → [ConciergeCitationsBehavior.storeIcon]
     * - anything else → [ConciergeCitationsBehavior.defaultLinkIcon]
     *
     * A null/blank asset name or an asset that fails to load falls back to the SDK's built-in
     * `external_link` drawable (see [rememberLinkHintIconPainter]).
     *
     * @param linkHints The (augmented) link hints covering every URL in the message
     * @param iconColor Tint color applied to every icon
     * @param citationsBehavior Theme-supplied citations config with per-kind asset names and style
     * @param context Used for the default link-handling fallback when [handleLink] is null
     * @param iconSize Size of each icon and its placeholder slot
     * @param iconSpacing Width of the transparent spacer slot before each icon
     * @param handleLink Optional handler invoked when an icon is tapped
     */
    internal fun createLinkHintInlineContentMap(
        linkHints: List<LinkHint>,
        iconColor: Color,
        citationsBehavior: ConciergeCitationsBehavior,
        context: Context,
        iconSize: Dp = 16.dp,
        iconSpacing: Dp = 2.dp,
        handleLink: ((String) -> Unit)? = null
    ): Map<String, InlineTextContent> {
        if (linkHints.isEmpty()) return emptyMap()

        // Inline content dimensions must be in Sp. Using Density(1f, 1f) gives a 1:1 dp→sp
        // mapping so placeholders match logical sizes regardless of user font scale.
        val iconSizeSp = with(Density(1f, 1f)) { iconSize.toSp() }
        val spacingSp = with(Density(1f, 1f)) { iconSpacing.toSp() }

        val result = mutableMapOf<String, InlineTextContent>()

        linkHints.distinctBy { it.kind to it.href }.forEach { hint ->
            // Spacer slot — transparent, provides the configurable gap before the icon
            result[MarkdownRenderer.linkSpacingId(hint.kind, hint.href)] = InlineTextContent(
                placeholder = Placeholder(
                    width = spacingSp,
                    height = iconSizeSp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                Spacer(modifier = Modifier.width(iconSpacing))
            }

            // Icon slot
            val assetName = themedAssetNameFor(hint.kind, citationsBehavior)
            result[MarkdownRenderer.linkIconId(hint.kind, hint.href)] = InlineTextContent(
                placeholder = Placeholder(
                    width = iconSizeSp,
                    height = iconSizeSp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                Icon(
                    painter = rememberLinkHintIconPainter(assetName),
                    contentDescription = hint.kind,
                    tint = iconColor,
                    modifier = Modifier
                        .size(iconSize)
                        .clickable {
                            if (handleLink != null) {
                                handleLink(hint.href)
                            } else {
                                val intent = Intent(Intent.ACTION_VIEW, hint.href.toUri())
                                context.startActivity(intent)
                            }
                        }
                )
            }
        }

        return result
    }

    private fun themedAssetNameFor(
        kind: String,
        behavior: ConciergeCitationsBehavior
    ): String? = when (kind) {
        "phone" -> behavior.phoneIcon
        "store" -> behavior.storeIcon
        else -> behavior.defaultLinkIcon
    }
}
