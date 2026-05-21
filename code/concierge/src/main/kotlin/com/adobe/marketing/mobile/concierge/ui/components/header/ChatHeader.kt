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

package com.adobe.marketing.mobile.concierge.ui.components.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.components.image.LocalAssetImage
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme

private const val HEADER_LAYOUT_IMAGE_ONLY = "imageOnly"

/**
 * Header component for the chat interface with a close button and one of two content modes:
 *
 * - `layoutType == "imageOnly"` — only the image is rendered. When no `image` is configured
 *   (or the field is blank), falls back to Material `Icons.AutoMirrored.Filled.Chat`.
 * - any other value (including `"textOnly"`, `null`, or unknown) — only the title and
 *   subtitle are rendered. This is the default.
 *
 * The image source is resolved by [LocalAssetImage]: an `http(s)://` URL is loaded remotely,
 * otherwise the value is treated as a basename under `assets/icons/` and matched against
 * `.png`, `.webp`, `.jpg`, `.jpeg` in that order — i.e. extension is irrelevant in the config.
 *
 * @param modifier Modifier for the header
 * @param onClose Callback when the close button is pressed
 */
@Composable
internal fun ChatHeader(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val style = ConciergeStyles.headerStyle
    val headerConfig = ConciergeTheme.header
    // Per-field fallbacks would shadow a deliberately-blank value; only fall back to defaults
    // when BOTH title and subtitle are blank/unset, so that setting just one keeps the other hidden.
    val configuredTitle = headerConfig?.title.orEmpty()
    val configuredSubtitle = headerConfig?.subtitle.orEmpty()
    val useDefaults = configuredTitle.isBlank() && configuredSubtitle.isBlank()

    val titleText = if (useDefaults) ConciergeConstants.ChatHeader.TITLE else configuredTitle
    val subtitleText = if (useDefaults) ConciergeConstants.ChatHeader.SUBTITLE else configuredSubtitle
    val showTitle = titleText.isNotBlank()
    val showSubtitle = subtitleText.isNotBlank()

    val imageSource = headerConfig?.image?.takeIf { it.isNotBlank() }

    // Resolve layout mode. Image is shown only when `layoutType == "imageOnly"` (explicit).
    // Every other value — including `"textOnly"`, `null`, or any unknown string — renders
    // only the title and subtitle.
    val isImageOnly = headerConfig?.layoutType
        .equals(HEADER_LAYOUT_IMAGE_ONLY, ignoreCase = true)

    val closeButtonAtStart = ConciergeTheme.behavior?.welcomeCard
        ?.closeButtonAlignment.equals("start", ignoreCase = true)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = style.horizontalPadding,
                    vertical = style.verticalPadding
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (closeButtonAtStart) {
                CloseButton(style = style, onClose = onClose)
                Spacer(modifier = Modifier.width(8.dp))
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isImageOnly) {
                    if (imageSource != null) {
                        HeaderImage(source = imageSource, style = style)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(style.imageHeight),
                            tint = style.titleColor
                        )
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        if (showTitle) {
                            Text(
                                text = titleText,
                                style = style.titleStyle,
                                fontWeight = style.titleFontWeight,
                                color = style.titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (showSubtitle) {
                            Text(
                                text = subtitleText,
                                style = style.subtitleStyle,
                                color = style.subtitleColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (!closeButtonAtStart) {
                CloseButton(style = style, onClose = onClose)
            }
        }

        HorizontalDivider(
            thickness = style.dividerThickness,
            color = style.dividerColor
        )
    }
}

@Composable
private fun HeaderImage(
    source: String,
    style: ConciergeStyles.HeaderStyle
) {
    LocalAssetImage(
        source = source,
        contentDescription = null,
        modifier = Modifier
            .height(style.imageHeight)
            .wrapContentWidth(Alignment.Start, unbounded = true),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun CloseButton(
    style: ConciergeStyles.HeaderStyle,
    onClose: () -> Unit
) {
    IconButton(
        onClick = onClose,
        modifier = Modifier.size(style.iconSize)
    ) {
        Icon(
            painter = painterResource(R.drawable.close),
            contentDescription = "Close chat",
            tint = style.iconColor
        )
    }
}
