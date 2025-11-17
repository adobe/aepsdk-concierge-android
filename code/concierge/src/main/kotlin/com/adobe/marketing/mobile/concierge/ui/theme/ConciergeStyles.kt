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

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Central styling configuration for all Concierge UI composables.
 * Organized by composable-level styles for consistency and maintainability.
 * Fully supports light and dark modes through MaterialTheme and ConciergeTheme.
 */
object ConciergeStyles {

    /**
     * Styling for the chat header component
     */
    @Immutable
    data class HeaderStyle(
        val padding: Dp,
        val titleStyle: TextStyle,
        val titleFontWeight: FontWeight,
        val titleColor: Color,
        val iconSize: Dp,
        val iconColor: Color
    )

    val headerStyle: HeaderStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return HeaderStyle(
                padding = 16.dp,
                titleStyle = MaterialTheme.typography.headlineSmall,
                titleFontWeight = FontWeight.Bold,
                titleColor = themeColors.primary,
                iconSize = 24.dp,
                iconColor = themeColors.primary
            )
        }

    /**
     * Styling for chat input panel
     */
    @Immutable
    data class InputPanelStyle(
        val outerShape: Shape,
        val innerShape: Shape,
        val outerPadding: Dp,
        val innerPadding: Dp,
        val backgroundColor: Color,
        val recordingBorderColors: List<Color>,
        val recordingBorderAnimationDuration: Int,
        val buttonSpacing: Dp,
        val placeholderText: String,
        val listeningPlaceholderText: String
    )

    val inputPanelStyle: InputPanelStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return InputPanelStyle(
                outerShape = RoundedCornerShape(12.dp),
                innerShape = RoundedCornerShape(10.dp),
                outerPadding = 2.dp,
                innerPadding = 4.dp,
                backgroundColor = themeColors.container,
                recordingBorderColors = listOf(
                    themeColors.primary,
                    themeColors.surface,
                    themeColors.surface,
                    themeColors.primary
                ),
                recordingBorderAnimationDuration = 1500,
                buttonSpacing = 8.dp,
                placeholderText = "How can I help",
                listeningPlaceholderText = "Listening..."
            )
        }

    /**
     * Styling for voice recording panel
     */
    @Immutable
    data class VoiceRecordingPanelStyle(
        val shape: Shape,
        val elevation: Dp,
        val backgroundColor: Color,
        val padding: Dp,
        val iconSize: Dp,
        val iconColor: Color,
        val cancelIconColor: Color,
        val contentSpacing: Dp,
        val pulseAnimationDuration: Int,
        val textStyle: TextStyle,
        val textColor: Color,
        val listeningText: String
    )

    val voiceRecordingPanelStyle: VoiceRecordingPanelStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return VoiceRecordingPanelStyle(
                shape = RoundedCornerShape(12.dp),
                elevation = 0.dp,
                backgroundColor = themeColors.surface,
                padding = 16.dp,
                iconSize = 24.dp,
                iconColor = themeColors.primary,
                cancelIconColor = themeColors.onSurface,
                contentSpacing = 12.dp,
                pulseAnimationDuration = 1000,
                textStyle = MaterialTheme.typography.bodyLarge,
                textColor = themeColors.onSurface,
                listeningText = "Listening"
            )
        }

    /**
     * Styling for chat message bubbles
     */
    @Immutable
    data class MessageBubbleStyle(
        val padding: Dp,
        val innerPadding: Dp,
        val shape: Shape,
        val elevation: Dp,
        val userMessageBackgroundColor: Color,
        val botMessageBackgroundColor: Color,
        val userMessageTextColor: Color,
        val botMessageTextColor: Color,
        val textStyle: TextStyle,
        val contentSpacing: Dp,
        val segmentSpacing: Dp
    )

    val messageBubbleStyle: MessageBubbleStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return MessageBubbleStyle(
                padding = 8.dp,
                innerPadding = 16.dp,
                shape = RoundedCornerShape(12.dp),
                elevation = 0.dp,
                userMessageBackgroundColor = themeColors.primary,
                botMessageBackgroundColor = themeColors.container,
                userMessageTextColor = themeColors.onPrimary,
                botMessageTextColor = themeColors.onSurface,
                textStyle = MaterialTheme.typography.bodyLarge,
                contentSpacing = 12.dp,
                segmentSpacing = 4.dp
            )
        }

    /**
     * Styling for thinking animation
     */
    @Immutable
    data class ThinkingAnimationStyle(
        val dotSize: Dp,
        val dotSpacing: Dp,
        val textDotSpacing: Dp,
        val dotColorAlpha: Float,
        val dotAnimationDuration: Int,
        val dotAnimationDelay: Int,
        val textStyle: TextStyle,
        val textColor: Color,
        val dotColor: Color,
        val thinkingText: String
    )

    val thinkingAnimationStyle: ThinkingAnimationStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ThinkingAnimationStyle(
                dotSize = 8.dp,
                dotSpacing = 8.dp,
                textDotSpacing = 8.dp,
                dotColorAlpha = 0.7f,
                dotAnimationDuration = 600,
                dotAnimationDelay = 200,
                textStyle = MaterialTheme.typography.bodyLarge,
                textColor = themeColors.onSurface,
                dotColor = themeColors.onSurface.copy(alpha = 0.7f),
                thinkingText = "Thinking"
            )
        }

    /**
     * Styling for product cards
     */
    @Immutable
    data class ProductCardStyle(
        val shape: Shape,
        val elevation: Dp,
        val backgroundColor: Color,
        val imageHeight: Dp,
        val titleStyle: TextStyle,
        val titleFontWeight: FontWeight,
        val titleColor: Color,
        val titleMaxLines: Int,
        val captionStyle: TextStyle,
        val captionColor: Color,
        val captionTopPadding: Dp,
        val captionBottomPadding: Dp,
        val textTopPadding: Dp,
        val fallbackGradientColors: List<Color>
    )

    val productCardStyle: ProductCardStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ProductCardStyle(
                shape = RoundedCornerShape(0.dp),
                elevation = 1.dp,
                backgroundColor = themeColors.container,
                imageHeight = 250.dp,
                titleStyle = MaterialTheme.typography.bodyLarge,
                titleFontWeight = FontWeight.Bold,
                titleColor = themeColors.onSurface,
                titleMaxLines = 2,
                captionStyle = MaterialTheme.typography.bodyLarge,
                captionColor = themeColors.onSurface.copy(alpha = 0.9f),
                captionTopPadding = 12.dp,
                captionBottomPadding = 16.dp,
                textTopPadding = 16.dp,
                fallbackGradientColors = listOf(
                    themeColors.primary.copy(alpha = 0.8f),
                    themeColors.primary.copy(alpha = 0.6f)
                )
            )
        }

    /**
     * Styling for product images (used in carousel and single cards)
     */
    @Immutable
    data class ProductImageStyle(
        val singleImageShape: Shape,
        val multiImageShape: Shape,
        val elevation: Dp,
        val backgroundColor: Color,
        val overlayBackgroundColor: Color,
        val overlayShape: Shape,
        val overlayPadding: Dp,
        val overlayInnerPadding: Dp,
        val overlayTextColor: Color,
        val overlayTextSize: Dp,
        val overlayTextFontWeight: FontWeight,
        val overlayTextStyle: TextStyle
    )

    val productImageStyle: ProductImageStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ProductImageStyle(
                singleImageShape = RoundedCornerShape(0.dp),
                multiImageShape = RoundedCornerShape(16.dp),
                elevation = 0.dp,
                backgroundColor = themeColors.surface,
                overlayBackgroundColor = themeColors.surface.copy(alpha = 0.95f),
                overlayShape = RoundedCornerShape(8.dp),
                overlayPadding = 12.dp,
                overlayInnerPadding = 12.dp,
                overlayTextColor = themeColors.onSurface,
                overlayTextSize = 16.dp,
                overlayTextFontWeight = FontWeight.Medium,
                overlayTextStyle = MaterialTheme.typography.bodyMedium
            )
        }

    /**
     * Styling for product carousel
     */
    @Immutable
    data class ProductCarouselStyle(
        val itemSpacing: Dp,
        val horizontalPadding: Dp,
        val verticalPadding: Dp,
        val imageWidth: Dp,
        val imageHeight: Dp,
        val indicatorSize: Dp,
        val indicatorSpacing: Dp,
        val indicatorActiveColor: Color,
        val indicatorInactiveColor: Color,
        val indicatorInactiveAlpha: Float,
        val navigationIconActiveColor: Color,
        val navigationIconInactiveColor: Color,
        val navigationIconInactiveAlpha: Float,
        val navigationSpacing: Dp
    )

    val productCarouselStyle: ProductCarouselStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ProductCarouselStyle(
                itemSpacing = 12.dp,
                horizontalPadding = 4.dp,
                verticalPadding = 8.dp,
                imageWidth = 200.dp,
                imageHeight = 150.dp,
                indicatorSize = 8.dp,
                indicatorSpacing = 8.dp,
                indicatorActiveColor = themeColors.onSurface,
                indicatorInactiveColor = themeColors.onSurface.copy(alpha = 0.3f),
                indicatorInactiveAlpha = 0.3f,
                navigationIconActiveColor = themeColors.onSurface,
                navigationIconInactiveColor = themeColors.onSurface.copy(alpha = 0.3f),
                navigationIconInactiveAlpha = 0.3f,
                navigationSpacing = 8.dp
            )
        }

    /**
     * Styling for product action buttons
     */
    @Immutable
    data class ProductActionButtonsStyle(
        val height: Dp,
        val shape: Shape,
        val spacing: Dp,
        val primaryBackgroundColor: Color,
        val primaryContentColor: Color,
        val secondaryBackgroundColor: Color,
        val secondaryContentColor: Color,
        val secondaryBorderWidth: Dp,
        val secondaryBorderColor: Color,
        val secondaryBorderAlpha: Float,
        val textStyle: TextStyle,
        val textAlign: TextAlign,
        val fontSize: Dp,
        val fontWeight: FontWeight,
        val maxLines: Int,
        val overflow: TextOverflow
    )

    val productActionButtonsStyle: ProductActionButtonsStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ProductActionButtonsStyle(
                height = 40.dp,
                shape = RoundedCornerShape(20.dp),
                spacing = 8.dp,
                primaryBackgroundColor = themeColors.primary,
                primaryContentColor = themeColors.onPrimary,
                secondaryBackgroundColor = themeColors.surface,
                secondaryContentColor = themeColors.onSurface,
                secondaryBorderWidth = 1.dp,
                secondaryBorderColor = themeColors.outline.copy(alpha = 0.5f),
                secondaryBorderAlpha = 0.5f,
                textStyle = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                fontSize = 12.dp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Visible
            )
        }

    /**
     * Styling for prompt suggestions
     */
    @Immutable
    data class PromptSuggestionsStyle(
        val containerTopPadding: Dp,
        val containerStartPadding: Dp,
        val containerEndPadding: Dp,
        val itemSpacing: Dp,
        val itemShape: Shape,
        val itemBackgroundColor: Color,
        val itemHorizontalPadding: Dp,
        val itemVerticalPadding: Dp,
        val iconSize: Dp,
        val iconColor: Color,
        val iconSpacing: Dp,
        val textStyle: TextStyle,
        val textColor: Color,
        val textMaxLines: Int
    )

    val promptSuggestionsStyle: PromptSuggestionsStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return PromptSuggestionsStyle(
                containerTopPadding = 6.dp,
                containerStartPadding = 12.dp,
                containerEndPadding = 48.dp,
                itemSpacing = 8.dp,
                itemShape = RoundedCornerShape(10.dp),
                itemBackgroundColor = themeColors.container,
                itemHorizontalPadding = 16.dp,
                itemVerticalPadding = 12.dp,
                iconSize = 10.dp,
                iconColor = themeColors.onSurfaceVariant,
                iconSpacing = 12.dp,
                textStyle = MaterialTheme.typography.bodyMedium,
                textColor = themeColors.onSurfaceVariant,
                textMaxLines = 2
            )
        }

    /**
     * Styling for citation items
     */
    @Immutable
    data class CitationStyle(
        val containerPadding: Dp,
        val separatorHeight: Dp,
        val separatorColor: Color,
        val textStyle: TextStyle,
        val textColor: Color,
        val textLength: Int,
        val urlColor: Color,
        val expandAnimationDuration: Int,
        val collapseAnimationDuration: Int
    )

    val citationStyle: CitationStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return CitationStyle(
                containerPadding = 8.dp,
                separatorHeight = 1.dp,
                separatorColor = themeColors.outline.copy(alpha = 0.3f),
                textStyle = MaterialTheme.typography.bodyMedium,
                textColor = themeColors.onSurface,
                textLength = 2,
                urlColor = themeColors.primary,
                expandAnimationDuration = 200,
                collapseAnimationDuration = 200
            )
        }

    /**
     * Styling for chat footer (contains citations and feedback)
     */
    @Immutable
    data class ChatFooterStyle(
        val sourcesButtonPadding: Dp,
        val textStyle: TextStyle,
        val textColor: Color,
        val iconColor: Color,
        val iconSpacing: Dp,
        val sourcesText: String
    )

    val chatFooterStyle: ChatFooterStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ChatFooterStyle(
                sourcesButtonPadding = 0.dp,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                textColor = themeColors.onSurfaceVariant,
                iconColor = themeColors.onSurfaceVariant,
                iconSpacing = 4.dp,
                sourcesText = "Sources"
            )
        }

    /**
     * Styling for feedback buttons (thumbs up/down)
     */
    @Immutable
    data class FeedbackButtonsStyle(
        val buttonSize: Dp,
        val iconSize: Dp,
        val spacing: Dp,
        val iconColor: Color
    )

    val feedbackButtonsStyle: FeedbackButtonsStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return FeedbackButtonsStyle(
                buttonSize = 32.dp,
                iconSize = 16.dp,
                spacing = 4.dp,
                iconColor = themeColors.onSurface
            )
        }

    /**
     * Styling for error overlay
     */
    @Immutable
    data class ErrorOverlayStyle(
        val padding: Dp,
        val backgroundColor: Color,
        val contentPadding: Dp,
        val messageTextStyle: TextStyle,
        val messageTextColor: Color,
        val dismissTextStyle: TextStyle,
        val dismissTextColor: Color,
        val dismissStartPadding: Dp
    )

    val errorOverlayStyle: ErrorOverlayStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ErrorOverlayStyle(
                padding = 16.dp,
                backgroundColor = themeColors.error,
                contentPadding = 16.dp,
                messageTextStyle = MaterialTheme.typography.bodyMedium,
                messageTextColor = themeColors.onError,
                dismissTextStyle = MaterialTheme.typography.bodySmall,
                dismissTextColor = themeColors.primary,
                dismissStartPadding = 8.dp
            )
        }

    /**
     * Styling for microphone button
     */
    @Immutable
    data class MicButtonStyle(
        val size: Dp,
        val iconColor: Color,
        val recordingIconColor: Color,
        val pulsingBackgroundColor: Color,
        val pulsingBackgroundAlpha: Float,
        val pulseAnimationDuration: Int,
        val pulseScaleRange: Pair<Float, Float>,
        val ringAlpha: Float
    )

    val micButtonStyle: MicButtonStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return MicButtonStyle(
                size = 24.dp,
                iconColor = themeColors.primary,
                recordingIconColor = themeColors.onPrimary,
                pulsingBackgroundColor = themeColors.primary,
                pulsingBackgroundAlpha = 0.25f,
                pulseAnimationDuration = 1000,
                pulseScaleRange = 1.5f to 2.0f,
                ringAlpha = 0.30f,
            )
        }

    /**
     * Styling for send button
     */
    @Immutable
    data class SendButtonStyle(
        val size: Dp,
        val enabledIconColor: Color,
        val disabledIconAlpha: Float
    )

    val sendButtonStyle: SendButtonStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return SendButtonStyle(
                size = 24.dp,
                enabledIconColor = themeColors.primary,
                disabledIconAlpha = 0.3f
            )
        }

    /**
     * Styling for message list
     */
    @Immutable
    data class MessageListStyle(
        val verticalSpacing: Dp,
        val horizontalPadding: Dp
    )

    val messageListStyle: MessageListStyle
        @Composable get() = MessageListStyle(
            verticalSpacing = 2.dp,
            horizontalPadding = 16.dp
        )

    /**
     * Styling for chat screen container
     */
    @Immutable
    data class ChatScreenStyle(
        val backgroundColor: Color
    )

    val chatScreenStyle: ChatScreenStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ChatScreenStyle(
                backgroundColor = themeColors.background
            )
        }

    /**
     * Styling for chat text field
     */
    @Immutable
    data class ChatTextFieldStyle(
        val horizontalPadding: Dp,
        val maxLines: Int,
        val textStyle: TextStyle,
        val placeholderColor: Color
    )

    val chatTextFieldStyle: ChatTextFieldStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ChatTextFieldStyle(
                horizontalPadding = 8.dp,
                maxLines = 7,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholderColor = themeColors.onSurfaceVariant
            )
        }

    /**
     * Styling for chat input field container
     */
    @Immutable
    data class ChatInputFieldStyle(
        val padding: Dp
    )

    val chatInputFieldStyle: ChatInputFieldStyle
        @Composable get() = ChatInputFieldStyle(
            padding = 16.dp
        )

    /**
     * Styling for feedback dialog
     */
    @Immutable
    data class FeedbackDialogStyle(
        val padding: Dp,
        val backgroundColor: Color,
        val elevation: Dp,
        val shape: Shape,
        val contentPadding: Dp,
        val titleStyle: TextStyle,
        val titleColor: Color,
        val titleSpacing: Dp,
        val questionStyle: TextStyle,
        val questionColor: Color,
        val questionSpacing: Dp,
        val categorySpacing: Dp,
        val checkboxCheckedColor: Color,
        val checkboxUncheckedColor: Color,
        val checkboxSpacing: Dp,
        val categoryTextStyle: TextStyle,
        val categoryTextColor: Color,
        val categoriesNotesSpacing: Dp,
        val notesLabelStyle: TextStyle,
        val notesLabelColor: Color,
        val notesLabelSpacing: Dp,
        val notesPlaceholderStyle: TextStyle,
        val notesPlaceholderColor: Color,
        val notesButtonsSpacing: Dp,
        val textFieldBorderColor: Color,
        val textFieldTextColor: Color,
        val buttonSpacing: Dp,
        val cancelButtonColor: Color,
        val submitButtonColor: Color,
        val submitButtonTextColor: Color,
        val buttonTextStyle: TextStyle
    )

    val feedbackDialogStyle: FeedbackDialogStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return FeedbackDialogStyle(
                padding = 16.dp,
                backgroundColor = themeColors.surface,
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                contentPadding = 20.dp,
                titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                titleColor = themeColors.onSurface,
                titleSpacing = 12.dp,
                questionStyle = MaterialTheme.typography.bodyMedium,
                questionColor = themeColors.onSurface,
                questionSpacing = 6.dp,
                categorySpacing = 0.dp,
                checkboxCheckedColor = themeColors.primary,
                checkboxUncheckedColor = themeColors.onSurfaceVariant,
                checkboxSpacing = 8.dp,
                categoryTextStyle = MaterialTheme.typography.bodyMedium,
                categoryTextColor = themeColors.onSurface,
                categoriesNotesSpacing = 6.dp,
                notesLabelStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                notesLabelColor = themeColors.onSurface,
                notesLabelSpacing = 8.dp,
                notesPlaceholderStyle = MaterialTheme.typography.bodyMedium,
                notesPlaceholderColor = themeColors.onSurfaceVariant,
                notesButtonsSpacing = 24.dp,
                textFieldBorderColor = themeColors.outline,
                textFieldTextColor = themeColors.onSurface,
                buttonSpacing = 8.dp,
                cancelButtonColor = themeColors.onSurfaceVariant,
                submitButtonColor = themeColors.primary,
                submitButtonTextColor = themeColors.onPrimary,
                buttonTextStyle = MaterialTheme.typography.labelMedium
            )
        }

    /**
     * Styling for feedback toast notification
     */
    @Immutable
    data class FeedbackToastStyle(
        val backgroundColor: Color,
        val elevation: Dp,
        val shape: Shape,
        val contentPadding: Dp,
        val iconSize: Dp,
        val iconColor: Color,
        val iconSpacing: Dp,
        val messageTextStyle: TextStyle,
        val messageTextColor: Color,
        val messageCloseSpacing: Dp,
        val closeButtonSize: Dp,
        val closeIconSize: Dp,
        val closeIconColor: Color
    )

    val feedbackToastStyle: FeedbackToastStyle
        @Composable get() {
            return FeedbackToastStyle(
                backgroundColor = Color(0xFF4CAF50),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                contentPadding = 16.dp,
                iconSize = 20.dp,
                iconColor = Color.White,
                iconSpacing = 12.dp,
                messageTextStyle = MaterialTheme.typography.bodyMedium,
                messageTextColor = Color.White,
                messageCloseSpacing = 8.dp,
                closeButtonSize = 24.dp,
                closeIconSize = 16.dp,
                closeIconColor = Color.White
            )
        }
}