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

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.components.links.LinkedTextFormatter
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.PrivacyNoticeConfig

@Composable
internal fun PrivacyNoticeView(
    config: PrivacyNoticeConfig,
    modifier: Modifier = Modifier
) {
    if (!config.hasContent) return

    val context = LocalContext.current
    val colors = ConciergeTheme.colors
    val bodyStyle = ConciergeStyles.messageBubbleStyle.textStyle
    val bodyColor = (colors.conciergeMessageText ?: colors.onSurface).copy(alpha = 0.75f)
    val linkColor = colors.messageConciergeLink ?: colors.onSurface
    val noticeShape = RoundedCornerShape(ConciergeTheme.tokens?.cssLayout?.borderRadiusCard?.dp ?: 12.dp)
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
    val linkActions = remember(annotatedText, context) {
        annotatedText.getStringAnnotations("URL", 0, annotatedText.length)
            .map { range ->
                range.item to annotatedText.text.substring(range.start, range.end)
            }
            .distinct()
            .map { (url, label) ->
                CustomAccessibilityAction(label = "Open $label") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
            .border(1.dp, colors.outline.copy(alpha = 0.5f), noticeShape)
            .padding(16.dp)
    ) {
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (config.title.isNotBlank()) {
                    Text(
                        text = config.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.conciergeMessageText ?: colors.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                ClickableText(
                    text = annotatedText,
                    textStyle = bodyStyle,
                    textAlign = TextAlign.Start,
                    handleLink = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    modifier = Modifier.semantics {
                        if (linkActions.isNotEmpty()) customActions = linkActions
                    }
                )
            }
        }
    }
}
