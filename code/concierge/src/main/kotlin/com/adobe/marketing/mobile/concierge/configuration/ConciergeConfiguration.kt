/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge.configuration

import androidx.compose.ui.graphics.Color

class ConciergeConfiguration private constructor(
    val chat: ChatTextConfiguration,
    val brandColors: BrandColorsConfiguration
) {
    companion object {
        data class ChatTextConfiguration(
            val conciergeName: String,
            val conciergeTagline: String,
            val initialMessage: String,
            val initialPrompts: List<String>
        )

        data class BrandColorsConfiguration(
            val primary: Color,
            val primaryLight: Color,
            val secondaryLight: Color,
            val primaryDark: Color,
            val textPrimary: Color,
            val textSecondary: Color,
            val cardBackground: Color,
            val buttonGradient: List<Color>,
            val cardGradient: List<Color>,
            val waveformGradient: List<Color>,
        ) {
            object Alpha {
                const val High = 0.9f
                const val Medium = 0.6f
                const val Low = 0.3f
                const val VeryLow = 0.1f
                const val Subtle = 0.05f
            }
        }
    }

    class Builder() {
        private var chat: ChatTextConfiguration? = null
        private var brandColors: BrandColorsConfiguration? = null

        fun chatTextConfiguration(chat: ChatTextConfiguration) = apply { this.chat = chat }

        fun brandColorsConfiguration(brandColors: BrandColorsConfiguration) = apply { this.brandColors = brandColors }

        fun build() = ConciergeConfiguration(chat = chat ?: Default.chat,
                brandColors = brandColors ?: Default.brandColors
            )

    }

    object Default {
        private val PRIMARY_COLOR = Color(0xFF595652)
        private val PRIMARY_DARK_COLOR = Color(0xFF40403F)
        private val SECONDARY_COLOR = Color(0xFF88BABF)

        val chat = ChatTextConfiguration(
            conciergeName = "Brand Concierge",
            conciergeTagline = "Powered by Adobe",
            initialMessage = "Please tell me how I can assist you today.",
            initialPrompts = listOf(
                "I'd like to explore Adobe Brand Concierge.",
            )
        )

        val brandColors = BrandColorsConfiguration(
            primary = PRIMARY_COLOR,
            primaryLight = PRIMARY_COLOR,
            primaryDark = PRIMARY_DARK_COLOR,
            secondaryLight = SECONDARY_COLOR,
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.7f),
            cardBackground = SECONDARY_COLOR.copy(alpha = 0.7f),
            buttonGradient = listOf(
                PRIMARY_COLOR,
                PRIMARY_DARK_COLOR
            ),
            cardGradient = listOf(
                Color(0xFF2B2B3D).copy(alpha = 0.2f)
            ),
            waveformGradient = listOf(
                PRIMARY_COLOR,
                PRIMARY_COLOR.copy(alpha = BrandColorsConfiguration.Alpha.Medium),
                PRIMARY_COLOR.copy(alpha = BrandColorsConfiguration.Alpha.Low),
                Color.Transparent
            )
        )
    }
}
