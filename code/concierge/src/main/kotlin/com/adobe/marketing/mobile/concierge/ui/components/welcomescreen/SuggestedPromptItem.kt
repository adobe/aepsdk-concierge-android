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

package com.adobe.marketing.mobile.concierge.ui.components.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import com.adobe.marketing.mobile.concierge.ui.components.image.AsyncImage
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Data class representing a suggested prompt with optional image
 */
data class SuggestedPrompt(
    val text: String,
    val imageVector: ImageVector? = null,
    val imagePainter: Painter? = null,
    val imageUrl: String? = null,
    val backgroundColor: Color? = null
)

/**
 * A single suggested prompt item with optional image
 * 
 * @param prompt The suggested prompt data
 * @param onClick Callback when the prompt is clicked
 */
@Composable
fun SuggestedPromptItem(
    prompt: SuggestedPrompt,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = ConciergeStyles.welcomeCardStyle

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = prompt.backgroundColor ?: style.promptBackgroundColor,
        shape = style.promptShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(style.promptPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Suggested prompt image or icon
            Box(
                modifier = Modifier
                    .size(style.promptImageSize)
                    .background(
                        color = style.promptImagePlaceholderColor,
                        shape = style.promptImageShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    prompt.imageUrl != null -> {
                        AsyncImage(
                            url = prompt.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(style.promptImageSize),
                            contentScale = ContentScale.Crop
                        )
                    }
                    prompt.imagePainter != null -> {
                        Image(
                            painter = prompt.imagePainter,
                            contentDescription = null,
                            modifier = Modifier.size(style.promptImageSize),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        // Always show an icon (either the provided one or a default)
                        Icon(
                            imageVector = prompt.imageVector ?: Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = style.promptTextColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(style.promptImageSize * 0.6f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(style.promptImageSpacing))
            
            // Prompt text
            Text(
                text = prompt.text,
                style = style.promptTextStyle,
                color = style.promptTextColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * // TODO: for testing only
 * Convenience function to get a default icon for a prompt based on keywords
 * Returns null if no match is found, allowing the default fallback icon to be used
 */
fun getDefaultIconForPrompt(prompt: String): ImageVector? {
    return when {
        prompt.contains("search", ignoreCase = true) || 
        prompt.contains("find", ignoreCase = true) || 
        prompt.contains("look", ignoreCase = true) ||
        prompt.contains("list", ignoreCase = true) ||
        prompt.contains("show", ignoreCase = true) -> Icons.Default.Search
        
        prompt.contains("buy", ignoreCase = true) || 
        prompt.contains("purchase", ignoreCase = true) || 
        prompt.contains("cart", ignoreCase = true) ||
        prompt.contains("shop", ignoreCase = true) -> Icons.Default.ShoppingCart
        
        prompt.contains("recommend", ignoreCase = true) || 
        prompt.contains("popular", ignoreCase = true) || 
        prompt.contains("best", ignoreCase = true) ||
        prompt.contains("top", ignoreCase = true) -> Icons.Default.Star
        
        prompt.contains("what is", ignoreCase = true) || 
        prompt.contains("what's", ignoreCase = true) ||
        prompt.contains("help", ignoreCase = true) || 
        prompt.contains("info", ignoreCase = true) || 
        prompt.contains("about", ignoreCase = true) ||
        prompt.contains("explain", ignoreCase = true) ||
        prompt.contains("tell me", ignoreCase = true) -> Icons.Default.QuestionMark
        
        else -> null
    }
}

