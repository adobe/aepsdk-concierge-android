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

package com.adobe.marketing.mobile.concierge.utils.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

/**
 * Centralized styling constants for markdown elements.
 */
internal object MarkdownStyles {
    
    // Color constants
    val H1_COLOR = Color(0xFFA75CF2)
    val H2_COLOR = Color(0xFF48D883)
    val CODE_BLOCK_BACKGROUND = Color(0xFFEFEFEF)
    val CODE_BLOCK_TEXT = Color(0xFF333333)
    val BLOCKQUOTE_BACKGROUND = Color(0xFFE0E0E0)
    val LINK_COLOR = Color.Blue
    
    // Font sizes
    val H1_FONT_SIZE = 26.sp
    val H2_FONT_SIZE = 22.sp
    val CODE_BLOCK_FONT_SIZE = 16.sp
    val INLINE_CODE_FONT_SIZE = 14.sp
    
    // Font families
    val MONOSPACE_FONT = FontFamily.Monospace
    
    // Styles
    val BOLD_STYLE = SpanStyle(fontWeight = FontWeight.Bold)
    val ITALIC_STYLE = SpanStyle(fontStyle = FontStyle.Italic)

    fun headingStyle(level: Int): SpanStyle {
        return SpanStyle(
            fontSize = if (level == 1) H1_FONT_SIZE else H2_FONT_SIZE,
            fontWeight = FontWeight.Bold,
            color = if (level == 1) H1_COLOR else H2_COLOR
        )
    }
    
    fun codeBlockStyle(): SpanStyle {
        return SpanStyle(
            background = CODE_BLOCK_BACKGROUND,
            color = CODE_BLOCK_TEXT,
            fontSize = CODE_BLOCK_FONT_SIZE,
            fontFamily = MONOSPACE_FONT
        )
    }
    
    fun inlineCodeStyle(): SpanStyle {
        return SpanStyle(
            background = Color.LightGray,
            fontSize = INLINE_CODE_FONT_SIZE,
            fontFamily = MONOSPACE_FONT
        )
    }
    
    fun linkStyle(): SpanStyle {
        return SpanStyle(
            color = LINK_COLOR,
            textDecoration = TextDecoration.Underline
        )
    }
    
    fun blockquoteStyle(): SpanStyle {
        return SpanStyle(
            background = BLOCKQUOTE_BACKGROUND,
            fontStyle = FontStyle.Italic
        )
    }
}
