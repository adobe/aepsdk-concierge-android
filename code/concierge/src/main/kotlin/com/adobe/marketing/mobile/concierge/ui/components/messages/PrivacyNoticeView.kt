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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.components.links.LinkedTextFormatter
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.PrivacyNoticeConfig

/**
 * Informational privacy notice shown before the first user query.
 *
 * Links route through [handleLink] with the `privacyNotice` origin, so they get the same
 * interception, tracking, and blocked-scheme handling as other chat links.
 */
@Composable
internal fun PrivacyNoticeView(
    config: PrivacyNoticeConfig,
    handleLink: (url: String, origin: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!config.hasContent) return

    val colors = ConciergeTheme.colors
    val bodyStyle = ConciergeStyles.messageBubbleStyle.textStyle
    val textColor = colors.conciergeMessageText ?: colors.onSurface
    val bodyColor = textColor.copy(alpha = 0.75f)
    val linkColor = colors.messageConciergeLink ?: colors.onSurface
    val title = config.title ?: stringResource(R.string.concierge_privacy_notice_title)
    val openLinkFormat = stringResource(R.string.concierge_privacy_notice_open_link)
    val noticeShape = RoundedCornerShape(ConciergeTheme.tokens?.cssLayout?.borderRadiusCard?.dp ?: 16.dp)
    val currentHandleLink by rememberUpdatedState(handleLink)
    val openLink: (String) -> Unit = { url ->
        currentHandleLink(url, ConciergeConstants.TrackingEvent.LinkClickOrigin.PRIVACY_NOTICE)
    }
    val annotatedText = remember(config, bodyStyle, bodyColor, linkColor) {
        LinkedTextFormatter.format(
            text = config.text,
            links = config.links,
            baseStyle = SpanStyle(
                color = bodyColor,
                fontSize = bodyStyle.fontSize,
                fontWeight = bodyStyle.fontWeight,
                fontFamily = bodyStyle.fontFamily
            ),
            linkColor = linkColor
        )
    }
    val linkActions = remember(annotatedText, openLinkFormat) {
        annotatedText.getStringAnnotations("URL", 0, annotatedText.length)
            .map { range ->
                range.item to annotatedText.text.substring(range.start, range.end)
            }
            .distinct()
            .map { (url, label) ->
                CustomAccessibilityAction(label = openLinkFormat.format(label)) {
                    openLink(url)
                    true
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = ConciergeStyles.messageBubbleStyle.padding)
            .clip(noticeShape)
            .background(colors.conciergeMessageBackground ?: colors.surface)
            .border(1.dp, textColor.copy(alpha = 0.12f), noticeShape)
            .padding(16.dp)
    ) {
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        style = bodyStyle.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                ClickableText(
                    text = annotatedText,
                    textStyle = bodyStyle,
                    textAlign = TextAlign.Start,
                    handleLink = openLink,
                    modifier = Modifier.semantics {
                        if (linkActions.isNotEmpty()) customActions = linkActions
                    }
                )
            }
        }
    }
}
