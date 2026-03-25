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

package com.adobe.marketing.mobile.concierge.ui.components.serviceintent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.network.CtaButton
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Displays a CTA pill button (e.g., "Chat with a teammate ↗")
 * below a bot message.
 *
 * @param cta The CTA button data containing the label and URL
 * @param onClick Callback invoked with the CTA URL when the button is clicked
 * @param modifier Optional modifier for the container
 */
@Composable
internal fun CtaButton(
    cta: CtaButton,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = ConciergeStyles.ctaButtonStyle

    Card(
        modifier = modifier
            .padding(
                top = style.containerTopPadding,
                start = style.containerStartPadding
            )
            .clickable { onClick(cta.url) },
        colors = CardDefaults.cardColors(
            containerColor = style.backgroundColor
        ),
        shape = style.shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = style.horizontalPadding,
                vertical = style.verticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(style.iconSpacing)
        ) {
            Text(
                text = cta.label,
                style = style.textStyle,
                color = style.textColor
            )
            Icon(
                painter = painterResource(id = R.drawable.open_in_new),
                contentDescription = null,
                modifier = Modifier.size(style.iconSize),
                tint = style.iconColor
            )
        }
    }
}
