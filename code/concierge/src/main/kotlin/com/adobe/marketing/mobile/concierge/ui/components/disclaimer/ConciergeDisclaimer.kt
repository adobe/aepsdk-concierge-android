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

package com.adobe.marketing.mobile.concierge.ui.components.disclaimer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.adobe.marketing.mobile.concierge.ui.components.messages.ClickableText
import com.adobe.marketing.mobile.concierge.ui.theme.DisclaimerConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.services.ServiceProvider

/**
 * Disclaimer text and link.
 * The disclaimer is rendered when [disclaimerConfig] is non-null and has text.
 * Placeholders in text like `{Terms}` are replaced by clickable links from [DisclaimerConfig.links].
 */
@Composable
internal fun ConciergeDisclaimer(
    modifier: Modifier = Modifier,
    disclaimerConfig: DisclaimerConfig?
) {
    if (disclaimerConfig?.text.isNullOrBlank()) return
    val config = disclaimerConfig!!

    val style = ConciergeStyles.disclaimerStyle
    val annotatedText = remember(config, style) {
        buildDisclaimerAnnotatedString(config, style)
    }
    val modifierWithPadding = modifier
        .fillMaxWidth()
        .padding(style.padding)

    if (annotatedText.getStringAnnotations("URL", 0, annotatedText.length).isEmpty()) {
        Text(
            text = config.text!!,
            style = style.textStyle,
            color = style.textColor,
            textAlign = TextAlign.Center,
            modifier = modifierWithPadding
        )
    } else {
        ClickableText(
            text = annotatedText,
            textStyle = style.textStyle,
            onLinkClick = { url ->
                ServiceProvider.getInstance().uriService.openUri(url)
            },
            textAlign = TextAlign.Center,
            modifier = modifierWithPadding
        )
    }
}

/**
 * Builds an [AnnotatedString] from disclaimer text, replacing placeholders like `{Terms}` with
 * the link text and adding URL annotations for click handling.
 */
private fun buildDisclaimerAnnotatedString(
    disclaimer: DisclaimerConfig,
    style: ConciergeStyles.DisclaimerStyle
): AnnotatedString {
    val text = disclaimer.text ?: ""
    val links = disclaimer.links.orEmpty()
    val baseSpanStyle = SpanStyle(
        color = style.textColor,
        fontSize = style.textStyle.fontSize,
        fontWeight = style.textStyle.fontWeight
    )

    return buildAnnotatedString {
        pushStyle(baseSpanStyle)
        var remaining: String = text
        for (link in links) {
            val placeholder = "{${link.text}}"
            val index = remaining.indexOf(placeholder)
            if (index >= 0) {
                append(remaining.substring(0, index))
                pop()
                pushStringAnnotation("URL", link.url)
                pushStyle(SpanStyle(textDecoration = style.linkTextDecoration))
                append(link.text)
                pop()
                pushStyle(baseSpanStyle)
                remaining = remaining.substring(index + placeholder.length)
            }
        }
        append(remaining)
        pop()
    }
}
