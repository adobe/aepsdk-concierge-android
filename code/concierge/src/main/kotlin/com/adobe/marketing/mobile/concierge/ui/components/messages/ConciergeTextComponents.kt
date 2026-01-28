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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownParser

/**
 * Renders concierge response text with markdown formatting.
 */
@Composable
internal fun ConciergeResponseText(
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Parse markdown to get the rendered text
    val markdownAnnotatedString = MarkdownParser.parse(text)

    AnimatedContent(
        targetState = markdownAnnotatedString,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 220,
                    easing = FastOutSlowInEasing
                )
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = LinearOutSlowInEasing
                )
            )
        },
        label = "responseFadeIn"
    ) { rendered ->
        ClickableText(
            text = rendered,
            modifier = modifier.fillMaxWidth(),
            onLinkClick = { url ->
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            }
        )
    }
}

/**
 * Reusable composable for rendering text with clickable links and optional inline content.
 *
 * @param text The annotated string to render
 * @param onLinkClick Callback for handling link clicks
 * @param modifier Optional modifier for the component
 * @param inlineContent Optional map of inline content for embedded composables (e.g., citations)
 */
@Composable
internal fun ClickableText(
    text: androidx.compose.ui.text.AnnotatedString,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    inlineContent: Map<String, InlineTextContent> = emptyMap()
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        inlineContent = inlineContent,
        onTextLayout = { textLayoutResult = it },
        softWrap = true,
        minLines = 1,
        maxLines = Int.MAX_VALUE,
        overflow = TextOverflow.Visible,
        modifier = modifier
            .pointerInput(text) {
                detectTapGestures { tapOffsetPosition ->
                    // Get the character offset at the tap position
                    textLayoutResult?.let { layoutResult ->
                        val offset = layoutResult.getOffsetForPosition(tapOffsetPosition)

                        // Find URL annotation at the clicked position
                        text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()
                            ?.let { annotation ->
                                onLinkClick(annotation.item)
                            }
                    }
                }
            }
    )
}
