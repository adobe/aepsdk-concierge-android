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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import android.content.Intent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownParser
import androidx.core.net.toUri

/**
 * Renders brand concierge content with clickable links.
 */
@Composable
internal fun ConciergeResponseText(
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val annotatedString = MarkdownParser.parse(text)
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    ClickableText(
        text = annotatedString,
        modifier = modifier.fillMaxWidth(),
        onLinkClick = { url ->
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(intent)
        }
    )
}

/**
 * Reusable composable for rendering text with clickable links.
 */
@Composable
internal fun ClickableText(
    text: androidx.compose.ui.text.AnnotatedString,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .padding(end = ListSpacing.END_PADDING)
            .pointerInput(text) {
                // TODO: This is messing with focus handling in the UserInput field. Fix it.
                detectTapGestures { tapOffsetPosition ->
                    // Link click handling logic
                    text.getStringAnnotations(start = 0, end = text.length)
                        .firstOrNull { it.tag == "URL" }
                        ?.let { annotation ->
                            onLinkClick(annotation.item)
                        }
                }
            }
    )
}
