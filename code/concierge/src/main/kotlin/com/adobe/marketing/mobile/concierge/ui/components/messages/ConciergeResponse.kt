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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownParser
import androidx.core.net.toUri

/**
 * Component that renders brand concierge responses containing markdown text
 * with proper styling and clickable links.
 * 
 * @param text The markdown text to be rendered
 * @param modifier Optional modifier for the text component
 */
@Composable
fun ConciergeResponse(
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val annotatedString = MarkdownParser.parse(text)
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    BasicText(
        text = annotatedString,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(annotatedString) {
                detectTapGestures { tapOffsetPosition ->
                    val layoutResult = textLayoutResult ?: return@detectTapGestures
                    val position = layoutResult.getOffsetForPosition(tapOffsetPosition)
                    
                    annotatedString
                        .getStringAnnotations(start = position, end = position)
                        .firstOrNull { it.tag == "URL" }
                        ?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                            context.startActivity(intent)
                        }
                }
            },
        onTextLayout = { result ->
            textLayoutResult = result
        }
    )
}
