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
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownTokenizer
import com.adobe.marketing.mobile.concierge.utils.markdown.TokenType
import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownToken
import androidx.core.net.toUri

/**
 * Component that renders brand concierge responses containing markdown text
 * with proper styling and clickable links.
 * 
 * @param text The markdown text to be rendered
 * @param modifier Optional modifier for the text component
 */
@Composable
internal fun ConciergeResponse(
    text: String,
    modifier: Modifier = Modifier
) {
    val tokens = remember(text) { MarkdownTokenizer.tokenize(text) }
    val listTokens = remember(tokens) { 
        tokens.filter { it.type == TokenType.LIST }
    }
    
    if (listTokens.isNotEmpty()) {
        ConciergeResponseWithLists(
            text = text,
            listTokens = listTokens,
            modifier = modifier
        )
    } else {
        ConciergeResponseText(
            text = text,
            modifier = modifier
        )
    }
}

/**
 * Renders concierge response content that contains lists, integrating list content within the
 * text flow while maintaining uniform indentation.
 */
@Composable
private fun ConciergeResponseWithLists(
    text: String,
    listTokens: List<MarkdownToken>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contentSegments = remember(text, listTokens) {
        ContentSegmentParser.createSegments(text, listTokens)
    }

    Column(modifier = modifier) {
        contentSegments.forEach { segment ->
            Spacer(modifier = Modifier.height(4.dp))
            when (segment) {
                is ContentSegment.Text -> {
                    ConciergeResponseText(
                        text = segment.content,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is ContentSegment.List -> {
                    ConciergeResponseList(
                        listTokens = segment.tokens,
                        onLinkClick = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}
