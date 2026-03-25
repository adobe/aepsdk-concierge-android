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

package com.adobe.marketing.mobile.concierge.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.config.WelcomeConfig

/**
 * Theme configuration.
 */
data class ConciergeThemeConfig(
    val name: String? = null,
    val colors: ConciergeThemeColors? = null,
    val styles: ConciergeThemeStyles? = null,
    val text: ConciergeTextStrings? = null,
    val disclaimer: DisclaimerConfig? = null,
    val welcomeExamples: List<ConciergeWelcomeExample>? = null,
    val feedbackPositiveOptions: List<String>? = null,
    val feedbackNegativeOptions: List<String>? = null,
    val typography: ConciergeTypographyConfig? = null
)

/**
 * Typography configuration from theme (font sizes and weights)
 */
data class ConciergeTypographyConfig(
    val inputFontSize: Double? = null,
    val disclaimerFontSize: Double? = null,
    val disclaimerFontWeight: Int? = null,
    val citationsFontSize: Double? = null
)

/**
 * Welcome example/prompt from the theme configuration
 */
data class ConciergeWelcomeExample(
    val text: String,
    val image: String? = null,
    val backgroundColor: String? = null
)

/**
 * Converts theme configuration to WelcomeConfig
 */
internal fun ConciergeThemeConfig.toWelcomeConfig(
    showWelcomeCard: Boolean = true
): WelcomeConfig {
    return WelcomeConfig(
        showWelcomeCard = showWelcomeCard,
        welcomeHeader = text?.welcomeHeading 
            ?: com.adobe.marketing.mobile.concierge.ConciergeConstants.WelcomeCard.DEFAULT_HEADING,
        subHeader = text?.welcomeSubheading 
            ?: com.adobe.marketing.mobile.concierge.ConciergeConstants.WelcomeCard.DEFAULT_SUBHEADING,
        suggestedPrompts = welcomeExamples?.map { example ->
            com.adobe.marketing.mobile.concierge.ui.config.SuggestedPrompt(
                text = example.text,
                imageUrl = example.image,
                backgroundColor = example.backgroundColor
            )
        } ?: emptyList()
    )
}

/**
 * Text strings from the theme configuration
 */
data class ConciergeTextStrings(
    // Input
    val inputPlaceholder: String? = null,

    // Header
    val headerTitle: String? = null,
    val headerSubtitle: String? = null,

    // Welcome
    val welcomeHeading: String? = null,
    val welcomeSubheading: String? = null,
    
    // Loading/Thinking
    val loadingMessage: String? = null,
    
    // Feedback Dialog
    val feedbackDialogTitlePositive: String? = null,
    val feedbackDialogTitleNegative: String? = null,
    val feedbackDialogQuestionPositive: String? = null,
    val feedbackDialogQuestionNegative: String? = null,
    val feedbackDialogNotes: String? = null,
    val feedbackDialogSubmit: String? = null,
    val feedbackDialogCancel: String? = null,
    val feedbackDialogNotesPlaceholder: String? = null,
    val feedbackToastSuccess: String? = null,
    val feedbackHelpfulLabel: String? = null,
    val sourcesLabel: String? = null,

    // Error
    val errorNetwork: String? = null
)

/**
 * Disclaimer configuration from the theme json
 */
data class DisclaimerConfig(
    val text: String? = null,
    val links: List<DisclaimerLink>? = null
)

data class DisclaimerLink(
    val text: String,
    val url: String
)

/**
 * Theme color configuration
 * Supports both simple color properties for basic themes and nested configuration for CSS themes
 */
data class ConciergeThemeColors(
    // Simple color properties for basic themes
    val primary: String? = null,
    val onPrimary: String? = null,
    val secondary: String? = null,
    val surface: String? = null,
    val onSurface: String? = null,
    val onSurfaceVariant: String? = null,
    val background: String? = null,
    val container: String? = null,
    val outline: String? = null,
    val error: String? = null,
    val onError: String? = null,
    val disclaimer: String? = null,
    
    // Nested color configuration for CSS themes
    val primaryColors: ConciergePrimaryColors? = null,
    val surfaceColors: ConciergeSurfaceColors? = null,
    val message: ConciergeMessageColors? = null,
    val button: ConciergeButtonColors? = null,
    val input: ConciergeInputColors? = null,
    val feedback: ConciergeFeedbackColors? = null,
    val citation: ConciergeCitationColors? = null,
    val welcomePrompt: ConciergeWelcomePromptColors? = null,
    val ctaButton: ConciergeCtaButtonColors? = null
)

/**
 * CSS theme color classes for granular color control
 */
data class ConciergePrimaryColors(
    val primary: String? = null,
    val text: String? = null
)

data class ConciergeSurfaceColors(
    val mainContainerBackground: String? = null,
    val mainContainerBottomBackground: String? = null,
    val messageBlockerBackground: String? = null
)

data class ConciergeMessageColors(
    val userBackground: String? = null,
    val userText: String? = null,
    val conciergeBackground: String? = null,
    val conciergeText: String? = null,
    val conciergeLink: String? = null
)

data class ConciergeButtonColors(
    val primaryBackground: String? = null,
    val primaryText: String? = null,
    val primaryHover: String? = null,
    val secondaryBorder: String? = null,
    val secondaryText: String? = null,
    val secondaryHover: String? = null,
    val secondaryHoverText: String? = null,
    val submitFill: String? = null,
    val submitFillDisabled: String? = null,
    val submitText: String? = null,
    val submitTextHover: String? = null,
    val disabledBackground: String? = null
)

data class ConciergeInputColors(
    val background: String? = null,
    val text: String? = null,
    val outline: String? = null,
    val outlineFocus: String? = null,
    val sendIconColor: String? = null,
    val sendArrowIconColor: String? = null,
    val sendArrowBackgroundColor: String? = null,
    val micIconColor: String? = null,
    val micRecordingIconColor: String? = null
)

data class ConciergeFeedbackColors(
    val iconButtonBackground: String? = null,
    val iconButtonHoverBackground: String? = null
)

data class ConciergeCitationColors(
    val backgroundColor: String? = null,
    val textColor: String? = null
)

data class ConciergeWelcomePromptColors(
    val backgroundColor: String? = null,
    val textColor: String? = null
)

data class ConciergeCtaButtonColors(
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val iconColor: String? = null
)

/**
 * Theme styles configuration - contains all component styles
 */
data class ConciergeThemeStyles(
    val header: ConciergeHeaderStyle? = null,
    val inputPanel: ConciergeInputPanelStyle? = null,
    val voiceRecordingPanel: ConciergeVoiceRecordingPanelStyle? = null,
    val messageBubble: ConciergeMessageBubbleStyle? = null,
    val thinkingAnimation: ConciergeThinkingAnimationStyle? = null,
    val productCard: ConciergeProductCardStyle? = null,
    val productImage: ConciergeProductImageStyle? = null,
    val productCarousel: ConciergeProductCarouselStyle? = null,
    val productActionButtons: ConciergeProductActionButtonsStyle? = null,
    val promptSuggestions: ConciergePromptSuggestionsStyle? = null,
    val citation: ConciergeCitationStyle? = null,
    val chatFooter: ConciergeChatFooterStyle? = null,
    val feedbackButtons: ConciergeFeedbackButtonsStyle? = null,
    val errorOverlay: ConciergeErrorOverlayStyle? = null,
    val micButton: ConciergeMicButtonStyle? = null,
    val sendButton: ConciergeSendButtonStyle? = null,
    val messageList: ConciergeMessageListStyle? = null,
    val chatScreen: ConciergeChatScreenStyle? = null,
    val chatTextField: ConciergeChatTextFieldStyle? = null,
    val chatInputField: ConciergeChatInputFieldStyle? = null,
    val feedbackDialog: ConciergeFeedbackDialogStyle? = null,
    val snackbar: ConciergeSnackbarStyle? = null,
    val welcomeCard: ConciergeWelcomeCardStyle? = null
)

// Individual style configurations
data class ConciergeHeaderStyle(
    val padding: Double? = null,
    val titleFontWeight: String? = null,
    val iconSize: Double? = null
)

data class ConciergeInputPanelStyle(
    val outerCornerRadius: Double? = null,
    val innerCornerRadius: Double? = null,
    val outerPadding: Double? = null,
    val innerPadding: Double? = null,
    val recordingBorderAnimationDuration: Int? = null,
    val buttonSpacing: Double? = null,
    val placeholderText: String? = null,
    val listeningPlaceholderText: String? = null
)

data class ConciergeVoiceRecordingPanelStyle(
    val cornerRadius: Double? = null,
    val elevation: Double? = null,
    val padding: Double? = null,
    val iconSize: Double? = null,
    val contentSpacing: Double? = null,
    val pulseAnimationDuration: Int? = null,
    val listeningText: String? = null
)

data class ConciergeMessageBubbleStyle(
    val padding: Double? = null,
    val innerPadding: Double? = null,
    val cornerRadius: Double? = null,
    val elevation: Double? = null,
    val contentSpacing: Double? = null,
    val segmentSpacing: Double? = null
)

data class ConciergeThinkingAnimationStyle(
    val dotSize: Double? = null,
    val dotSpacing: Double? = null,
    val textDotSpacing: Double? = null,
    val dotColorAlpha: Double? = null,
    val dotAnimationDuration: Int? = null,
    val dotAnimationDelay: Int? = null,
    val thinkingText: String? = null
)

data class ConciergeProductCardStyle(
    val cornerRadius: Double? = null,
    val elevation: Double? = null,
    val imageHeight: Double? = null,
    val titleFontWeight: String? = null,
    val titleMaxLines: Int? = null,
    val captionTopPadding: Double? = null,
    val captionBottomPadding: Double? = null,
    val textTopPadding: Double? = null
)

data class ConciergeProductImageStyle(
    val singleImageCornerRadius: Double? = null,
    val multiImageCornerRadius: Double? = null,
    val elevation: Double? = null,
    val overlayCornerRadius: Double? = null,
    val overlayPadding: Double? = null,
    val overlayInnerPadding: Double? = null,
    val overlayTextSize: Double? = null,
    val overlayTextFontWeight: String? = null
)

data class ConciergeProductCarouselStyle(
    val itemSpacing: Double? = null,
    val horizontalPadding: Double? = null,
    val verticalPadding: Double? = null,
    val imageWidth: Double? = null,
    val imageHeight: Double? = null,
    val indicatorSize: Double? = null,
    val indicatorSpacing: Double? = null,
    val indicatorInactiveAlpha: Double? = null,
    val navigationIconInactiveAlpha: Double? = null,
    val navigationSpacing: Double? = null
)

data class ConciergeProductActionButtonsStyle(
    val height: Double? = null,
    val cornerRadius: Double? = null,
    val spacing: Double? = null,
    val secondaryBorderWidth: Double? = null,
    val secondaryBorderAlpha: Double? = null,
    val fontSize: Double? = null,
    val fontWeight: String? = null,
    val maxLines: Int? = null
)

data class ConciergePromptSuggestionsStyle(
    val containerTopPadding: Double? = null,
    val containerStartPadding: Double? = null,
    val containerEndPadding: Double? = null,
    val itemSpacing: Double? = null,
    val itemCornerRadius: Double? = null,
    val itemHorizontalPadding: Double? = null,
    val itemVerticalPadding: Double? = null,
    val iconSize: Double? = null,
    val iconSpacing: Double? = null,
    val textMaxLines: Int? = null
)

data class ConciergeCitationStyle(
    val containerPadding: Double? = null,
    val separatorHeight: Double? = null,
    val textLength: Int? = null,
    val expandAnimationDuration: Int? = null,
    val collapseAnimationDuration: Int? = null
)

data class ConciergeChatFooterStyle(
    val sourcesButtonPadding: Double? = null,
    val iconSpacing: Double? = null,
    val sourcesText: String? = null
)

data class ConciergeFeedbackButtonsStyle(
    val buttonSize: Double? = null,
    val iconSize: Double? = null,
    val spacing: Double? = null
)

data class ConciergeErrorOverlayStyle(
    val padding: Double? = null,
    val contentPadding: Double? = null,
    val dismissStartPadding: Double? = null
)

data class ConciergeMicButtonStyle(
    val size: Double? = null,
    val pulsingBackgroundAlpha: Double? = null,
    val pulseAnimationDuration: Int? = null,
    val pulseScaleMin: Double? = null,
    val pulseScaleMax: Double? = null,
    val ringAlpha: Double? = null
)

data class ConciergeSendButtonStyle(
    val size: Double? = null,
    val disabledIconAlpha: Double? = null
)

data class ConciergeMessageListStyle(
    val verticalSpacing: Double? = null,
    val horizontalPadding: Double? = null
)

data class ConciergeChatScreenStyle(
    val backgroundColor: String? = null
)

data class ConciergeChatTextFieldStyle(
    val horizontalPadding: Double? = null,
    val maxLines: Int? = null
)

data class ConciergeChatInputFieldStyle(
    val padding: Double? = null
)

data class ConciergeFeedbackDialogStyle(
    val padding: Double? = null,
    val elevation: Double? = null,
    val cornerRadius: Double? = null,
    val contentPadding: Double? = null,
    val titleSpacing: Double? = null,
    val questionSpacing: Double? = null,
    val categorySpacing: Double? = null,
    val checkboxSpacing: Double? = null,
    val categoriesNotesSpacing: Double? = null,
    val notesLabelSpacing: Double? = null,
    val notesButtonsSpacing: Double? = null,
    val buttonSpacing: Double? = null
)

data class ConciergeSnackbarStyle(
    val containerColor: String? = null,
    val contentColor: String? = null,
    val actionColor: String? = null
)

data class ConciergeWelcomeCardStyle(
    val cornerRadius: Double? = null,
    val elevation: Double? = null,
    val contentPadding: Double? = null,
    val titleBottomSpacing: Double? = null,
    val promptsTopSpacing: Double? = null,
    val promptsHeaderBottomSpacing: Double? = null,
    val promptsSpacing: Double? = null,
    val promptCornerRadius: Double? = null,
    val promptPadding: Double? = null,
    val promptImageSize: Double? = null,
    val promptImageCornerRadius: Double? = null,
    val promptImageSpacing: Double? = null
)

/**
 * Extension functions to convert JSON models to runtime objects
 */

/**
 * Converts a hex color string to Compose Color
 * Supports formats: #RGB, #RRGGBB, #AARRGGBB
 */
internal fun String.toComposeColor(): Color? {
    return try {
        val hex = this.removePrefix("#")
        when (hex.length) {
            3 -> {
                // #RGB -> #RRGGBB
                val r = hex[0].toString().repeat(2).toInt(16)
                val g = hex[1].toString().repeat(2).toInt(16)
                val b = hex[2].toString().repeat(2).toInt(16)
                Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = 1f)
            }
            6 -> {
                // #RRGGBB
                val r = hex.substring(0, 2).toInt(16)
                val g = hex.substring(2, 4).toInt(16)
                val b = hex.substring(4, 6).toInt(16)
                Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = 1f)
            }
            8 -> {
                // #AARRGGBB
                val a = hex.substring(0, 2).toInt(16)
                val r = hex.substring(2, 4).toInt(16)
                val g = hex.substring(4, 6).toInt(16)
                val b = hex.substring(6, 8).toInt(16)
                Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a / 255f)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Converts a string to FontWeight
 */
internal fun String.toFontWeight(): FontWeight? {
    return when (this.lowercase()) {
        "thin" -> FontWeight.Thin
        "extralight", "extra_light" -> FontWeight.ExtraLight
        "light" -> FontWeight.Light
        "normal", "regular" -> FontWeight.Normal
        "medium" -> FontWeight.Medium
        "semibold", "semi_bold" -> FontWeight.SemiBold
        "bold" -> FontWeight.Bold
        "extrabold", "extra_bold" -> FontWeight.ExtraBold
        "black" -> FontWeight.Black
        else -> null
    }
}

/**
 * Converts Double to Dp
 */
internal fun Double.toDp(): Dp = this.dp

/**
 * Converts Float to alpha value
 */
internal fun Double.toAlpha(): Float = this.toFloat().coerceIn(0f, 1f)

