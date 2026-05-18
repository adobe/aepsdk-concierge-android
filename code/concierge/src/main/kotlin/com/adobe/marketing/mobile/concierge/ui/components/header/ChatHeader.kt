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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.components.image.LocalAssetImage
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme

private const val HEADER_IMAGE_POSITION_TRAILING = "trailing"

/**
 * Header component for the chat interface with title, subtitle, optional image (leading or
 * trailing) and a close button.
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
    val themeText = ConciergeTheme.text
    val titleText = themeText?.headerTitle ?: ConciergeConstants.ChatHeader.TITLE
    val subtitleText = themeText?.headerSubtitle ?: ConciergeConstants.ChatHeader.SUBTITLE
    val showSubtitle = subtitleText.isNotBlank()

    val imageSource = themeText?.headerImage?.takeIf { it.isNotBlank() }
    val isTrailingImage = themeText?.headerImagePosition
        .equals(HEADER_IMAGE_POSITION_TRAILING, ignoreCase = true)

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
                if (imageSource != null && !isTrailingImage) {
                    HeaderImage(source = imageSource, style = style)
                    Spacer(modifier = Modifier.width(style.imageSpacing))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleText,
                        style = style.titleStyle,
                        fontWeight = style.titleFontWeight,
                        color = style.titleColor
                    )
                    if (showSubtitle) {
                        Text(
                            text = subtitleText,
                            style = style.subtitleStyle,
                            color = style.subtitleColor
                        )
                    }
                }

                if (imageSource != null && isTrailingImage) {
                    Spacer(modifier = Modifier.width(style.imageSpacing))
                    HeaderImage(source = imageSource, style = style)
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
