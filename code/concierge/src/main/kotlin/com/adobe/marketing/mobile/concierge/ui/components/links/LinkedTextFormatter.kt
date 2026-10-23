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

package com.adobe.marketing.mobile.concierge.ui.components.links

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.adobe.marketing.mobile.concierge.ui.theme.DisclaimerLink
import java.net.URI
import java.net.URISyntaxException

internal object LinkedTextFormatter {
    private const val URL_ANNOTATION = "URL"

    fun format(
        text: String,
        links: List<DisclaimerLink>?,
        baseStyle: SpanStyle,
        linkColor: Color,
        linkTextDecoration: TextDecoration = TextDecoration.Underline
    ): AnnotatedString = buildAnnotatedString {
        pushStyle(baseStyle)
        var cursor = 0
        while (cursor < text.length) {
            val openingBrace = text.indexOf('{', cursor)
            if (openingBrace < 0) {
                append(text.substring(cursor))
                break
            }

            val closingBrace = text.indexOf('}', openingBrace + 1)
            if (closingBrace < 0) {
                append(text.substring(cursor))
                break
            }

            append(text.substring(cursor, openingBrace))
            val label = text.substring(openingBrace + 1, closingBrace)
            val link = links.orEmpty().firstOrNull { it.text == label }
            if (link != null && isValidUrl(link.url)) {
                pushStringAnnotation(URL_ANNOTATION, link.url)
                pushStyle(baseStyle.copy(color = linkColor, textDecoration = linkTextDecoration))
                append(label)
                pop()
                pop()
            } else {
                append(label)
            }
            cursor = closingBrace + 1
        }
        pop()
    }

    private fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            URI(url)
            true
        } catch (_: URISyntaxException) {
            false
        }
    }
}
