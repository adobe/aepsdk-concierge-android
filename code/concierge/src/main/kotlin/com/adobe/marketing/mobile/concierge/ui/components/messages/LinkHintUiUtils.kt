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
import androidx.compose.foundation.layout.size
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
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeLinkHintIconAssets
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownRenderer

/**
 * UI-specific utility functions for rendering inline link-hint icons within markdown text.
 */
internal object LinkHintUiUtils {

    /**
     * Creates an inline text content map for link hint icons.
     *
     * One entry is emitted per unique (kind, href) pair so each icon can carry its own click
     * handler — tapping the icon invokes [handleLink] (or opens the href in the browser if
     * [handleLink] is null).
     *
     * Every hint produces an icon. The icon source is resolved per-kind from [iconAssets]:
     * - `phone`  → [ConciergeLinkHintIconAssets.phone]
     * - `store`  → [ConciergeLinkHintIconAssets.store]
     * - anything else → [ConciergeLinkHintIconAssets.default]
     *
     * If the resolved name is null/blank or the named asset can't be loaded, the SDK's
     * built-in `external_link` drawable is used as the fallback (see [rememberLinkHintIconPainter]).
     *
     * @param linkHints The link hints from the backend response
     * @param iconColor The tint color to apply to every icon (typically the primary/link color)
     * @param iconAssets Theme-supplied asset names for the per-kind icons
     * @param context Used only for the default link-handling fallback when [handleLink] is null
     * @param iconSize The size of each icon and its placeholder slot
     * @param handleLink Optional handler invoked when the icon is tapped
     */
    internal fun createLinkHintInlineContentMap(
        linkHints: List<LinkHint>,
        iconColor: Color,
        iconAssets: ConciergeLinkHintIconAssets,
        context: Context,
        iconSize: Dp = 16.dp,
        handleLink: ((String) -> Unit)? = null
    ): Map<String, InlineTextContent> {
        if (linkHints.isEmpty()) return emptyMap()

        // Inline content width/height must be expressed in Sp. Using Density(1f, 1f) yields a 1:1
        // dp->sp mapping so the placeholder matches the icon's logical size regardless of the
        // user's font scale. This matches the pattern used in CitationUiUtils.
        val placeholderSize = with(Density(1f, 1f)) { iconSize.toSp() }

        return linkHints.asSequence()
            .distinctBy { it.kind to it.href }.associate { hint ->
                val themedAssetName = themedAssetNameFor(hint.kind, iconAssets)
                val content = InlineTextContent(
                    placeholder = Placeholder(
                        width = placeholderSize,
                        height = placeholderSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    Icon(
                        painter = rememberLinkHintIconPainter(themedAssetName),
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
                MarkdownRenderer.linkIconId(hint.kind, hint.href) to content
            }
    }

    private fun themedAssetNameFor(
        kind: String,
        assets: ConciergeLinkHintIconAssets
    ): String? = when (kind) {
        "phone" -> assets.phone
        "store" -> assets.store
        else -> assets.default
    }
}
