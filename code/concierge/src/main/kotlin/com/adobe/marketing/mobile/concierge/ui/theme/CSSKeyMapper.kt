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

package com.adobe.marketing.mobile.concierge.ui.theme

import android.util.Log

/**
 * Direct assignment function that converts CSS value and applies it to theme
 */
typealias CSSAssignment = (String, ConciergeThemeTokens) -> ConciergeThemeTokens

/**
 * Maps CSS variable names (ex: "--input-box-shadow") directly to property assignments.
 * Used to convert web CSS theme format to ConciergeTheme structure.
 */
internal object CSSKeyMapper {
    
    private const val LOG_TAG = "ConciergeTheme"
    
    // Helper functions to reduce boilerplate

    /**
     * Helper to update nested color structures.
     */
    private fun updateColors(
        theme: ConciergeThemeTokens,
        updater: (ConciergeThemeColors?) -> ConciergeThemeColors
    ): ConciergeThemeTokens {
        return theme.copy(colors = updater(theme.colors))
    }

    /**
     * Generic helper that parses [cssValue] as a color, extracts the current nested color object
     * via [getter], applies [updater] to produce an updated value, and writes it back via [merger].
     * All per-type color helpers delegate to this function.
     */
    private fun <T> updateNestedColors(
        cssValue: String,
        theme: ConciergeThemeTokens,
        getter: (ConciergeThemeColors?) -> T?,
        merger: (ConciergeThemeColors?, T) -> ConciergeThemeColors,
        updater: (T?, String) -> T
    ): ConciergeThemeTokens {
        val color = CSSValueConverter.parseColor(cssValue)
        return updateColors(theme) { colors ->
            merger(colors, updater(getter(colors), color.toHexString()))
        }
    }

    private fun updatePrimaryColors(cssValue: String, theme: ConciergeThemeTokens, updater: (ConciergePrimaryColors?, String) -> ConciergePrimaryColors) =
        updateNestedColors(cssValue, theme, { it?.primaryColors }, { c, v -> c?.copy(primaryColors = v) ?: ConciergeThemeColors(primaryColors = v) }, updater)

    private fun updateSurfaceColors(cssValue: String, theme: ConciergeThemeTokens, updater: (ConciergeSurfaceColors?, String) -> ConciergeSurfaceColors) =
        updateNestedColors(cssValue, theme, { it?.surfaceColors }, { c, v -> c?.copy(surfaceColors = v) ?: ConciergeThemeColors(surfaceColors = v) }, updater)

    private fun updateMessageColors(cssValue: String, theme: ConciergeThemeTokens, updater: (ConciergeMessageColors?, String) -> ConciergeMessageColors) =
        updateNestedColors(cssValue, theme, { it?.message }, { c, v -> c?.copy(message = v) ?: ConciergeThemeColors(message = v) }, updater)

    private fun updateButtonColors(cssValue: String, theme: ConciergeThemeTokens, updater: (ConciergeButtonColors?, String) -> ConciergeButtonColors) =
        updateNestedColors(cssValue, theme, { it?.button }, { c, v -> c?.copy(button = v) ?: ConciergeThemeColors(button = v) }, updater)

    private fun updateInputColors(cssValue: String, theme: ConciergeThemeTokens, updater: (ConciergeInputColors?, String) -> ConciergeInputColors) =
        updateNestedColors(cssValue, theme, { it?.input }, { c, v -> c?.copy(input = v) ?: ConciergeThemeColors(input = v) }, updater)

    private fun updateFeedbackColors(cssValue: String, theme: ConciergeThemeTokens, updater: (ConciergeFeedbackColors?, String) -> ConciergeFeedbackColors) =
        updateNestedColors(cssValue, theme, { it?.feedback }, { c, v -> c?.copy(feedback = v) ?: ConciergeThemeColors(feedback = v) }, updater)

    private fun updateCtaButtonColors(cssValue: String, theme: ConciergeThemeTokens, updater: (ConciergeCtaButtonColors?, String) -> ConciergeCtaButtonColors) =
        updateNestedColors(cssValue, theme, { it?.ctaButton }, { c, v -> c?.copy(ctaButton = v) ?: ConciergeThemeColors(ctaButton = v) }, updater)

    private fun updateCitationColors(cssValue: String, theme: ConciergeThemeTokens, updater: (ConciergeCitationColors?, String) -> ConciergeCitationColors) =
        updateNestedColors(cssValue, theme, { it?.citation }, { c, v -> c?.copy(citation = v) ?: ConciergeThemeColors(citation = v) }, updater)

    private fun updateWelcomePromptColors(cssValue: String, theme: ConciergeThemeTokens, updater: (ConciergeWelcomePromptColors?, String) -> ConciergeWelcomePromptColors) =
        updateNestedColors(cssValue, theme, { it?.welcomePrompt }, { c, v -> c?.copy(welcomePrompt = v) ?: ConciergeThemeColors(welcomePrompt = v) }, updater)

    private fun updateThinkingColors(cssValue: String, theme: ConciergeThemeTokens, updater: (ConciergeThinkingColors?, String) -> ConciergeThinkingColors) =
        updateNestedColors(cssValue, theme, { it?.thinking }, { c, v -> c?.copy(thinking = v) ?: ConciergeThemeColors(thinking = v) }, updater)

    private fun updateSuggestionColors(cssValue: String, theme: ConciergeThemeTokens, updater: (ConciergeWelcomePromptColors?, String) -> ConciergeWelcomePromptColors) =
        updateNestedColors(cssValue, theme, { it?.promptSuggestion }, { c, v -> c?.copy(promptSuggestion = v) ?: ConciergeThemeColors(promptSuggestion = v) }, updater)

    /**
     * Helper to update layout properties
     */
    private fun updateLayout(
        theme: ConciergeThemeTokens,
        updater: (ConciergeLayout?) -> ConciergeLayout
    ): ConciergeThemeTokens {
        return theme.copy(cssLayout = updater(theme.cssLayout))
    }
    
    /**
     * Mapping from CSS variable name (without --) to direct assignment function
     */
    private val cssToAssignmentMap: Map<String, CSSAssignment> = mapOf(
        // Typography
        "font-family" to { cssValue, theme ->
            val fontFamily = CSSValueConverter.parseFontFamily(cssValue)
            theme.copy(
                typography = theme.typography?.copy(fontFamily = fontFamily)
                    ?: ConciergeTypography(fontFamily = fontFamily)
            )
        },
        "line-height-body" to { cssValue, theme ->
            val lineHeight = CSSValueConverter.parseLineHeight(cssValue)
            theme.copy(
                typography = theme.typography?.copy(lineHeight = lineHeight)
                    ?: ConciergeTypography(lineHeight = lineHeight)
            )
        },
        
        // Colors - Primary (using helper)
        "color-primary" to { cssValue, theme ->
            updatePrimaryColors(cssValue, theme) { existing, color ->
                existing?.copy(primary = color) ?: ConciergePrimaryColors(primary = color)
            }
        },
        "color-text" to { cssValue, theme ->
            updatePrimaryColors(cssValue, theme) { existing, color ->
                existing?.copy(text = color) ?: ConciergePrimaryColors(text = color)
            }
        },
        "color-container" to { cssValue, theme ->
            val color = CSSValueConverter.parseColor(cssValue)
            updateColors(theme) { colors ->
                colors?.copy(container = color.toHexString())
                    ?: ConciergeThemeColors(container = color.toHexString())
            }
        },

        // Colors - Surface (using helper)
        "main-container-background" to { cssValue, theme ->
            updateSurfaceColors(cssValue, theme) { existing, color ->
                existing?.copy(mainContainerBackground = color) ?: ConciergeSurfaceColors(mainContainerBackground = color)
            }
        },
        "main-container-bottom-background" to { cssValue, theme ->
            updateSurfaceColors(cssValue, theme) { existing, color ->
                existing?.copy(mainContainerBottomBackground = color) ?: ConciergeSurfaceColors(mainContainerBottomBackground = color)
            }
        },
        "message-blocker-background" to { cssValue, theme ->
            updateSurfaceColors(cssValue, theme) { existing, color ->
                existing?.copy(messageBlockerBackground = color) ?: ConciergeSurfaceColors(messageBlockerBackground = color)
            }
        },
        
        // Colors - Message (using helper)
        "message-user-background" to { cssValue, theme ->
            updateMessageColors(cssValue, theme) { existing, color ->
                existing?.copy(userBackground = color) ?: ConciergeMessageColors(userBackground = color)
            }
        },
        "message-user-text" to { cssValue, theme ->
            updateMessageColors(cssValue, theme) { existing, color ->
                existing?.copy(userText = color) ?: ConciergeMessageColors(userText = color)
            }
        },
        "message-concierge-background" to { cssValue, theme ->
            updateMessageColors(cssValue, theme) { existing, color ->
                existing?.copy(conciergeBackground = color) ?: ConciergeMessageColors(conciergeBackground = color)
            }
        },
        "message-concierge-text" to { cssValue, theme ->
            updateMessageColors(cssValue, theme) { existing, color ->
                existing?.copy(conciergeText = color) ?: ConciergeMessageColors(conciergeText = color)
            }
        },
        "message-concierge-link-color" to { cssValue, theme ->
            updateMessageColors(cssValue, theme) { existing, color ->
                existing?.copy(conciergeLink = color) ?: ConciergeMessageColors(conciergeLink = color)
            }
        },
        
        // Colors - Button (using helper)
        "button-primary-background" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(primaryBackground = color) ?: ConciergeButtonColors(primaryBackground = color)
            }
        },
        "button-primary-text" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(primaryText = color) ?: ConciergeButtonColors(primaryText = color)
            }
        },
        "button-primary-hover" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(primaryHover = color) ?: ConciergeButtonColors(primaryHover = color)
            }
        },
        "button-secondary-border" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(secondaryBorder = color) ?: ConciergeButtonColors(secondaryBorder = color)
            }
        },
        "button-secondary-text" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(secondaryText = color) ?: ConciergeButtonColors(secondaryText = color)
            }
        },
        "button-secondary-hover" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(secondaryHover = color) ?: ConciergeButtonColors(secondaryHover = color)
            }
        },
        "color-button-secondary-hover-text" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(secondaryHoverText = color) ?: ConciergeButtonColors(secondaryHoverText = color)
            }
        },
        "submit-button-fill-color" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(submitFill = color) ?: ConciergeButtonColors(submitFill = color)
            }
        },
        "submit-button-fill-color-disabled" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(submitFillDisabled = color) ?: ConciergeButtonColors(submitFillDisabled = color)
            }
        },
        "color-button-submit" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(submitText = color) ?: ConciergeButtonColors(submitText = color)
            }
        },
        "color-button-submit-hover" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(submitTextHover = color) ?: ConciergeButtonColors(submitTextHover = color)
            }
        },
        "button-disabled-background" to { cssValue, theme ->
            updateButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(disabledBackground = color) ?: ConciergeButtonColors(disabledBackground = color)
            }
        },
        
        // Colors - Input (using helper)
        "input-background" to { cssValue, theme ->
            updateInputColors(cssValue, theme) { existing, color ->
                existing?.copy(background = color) ?: ConciergeInputColors(background = color)
            }
        },
        "input-text-color" to { cssValue, theme ->
            updateInputColors(cssValue, theme) { existing, color ->
                existing?.copy(text = color) ?: ConciergeInputColors(text = color)
            }
        },
        "input-outline-color" to { cssValue, theme ->
            // Handle gradients - if starts with "linear-gradient", set to null
            if (cssValue.trim().startsWith("linear-gradient")) {
                updateInputColors(cssValue, theme) { existing, _ ->
                    existing?.copy(outline = null) ?: ConciergeInputColors(outline = null)
                }
            } else {
                updateInputColors(cssValue, theme) { existing, color ->
                    existing?.copy(outline = color) ?: ConciergeInputColors(outline = color)
                }
            }
        },
        "input-focus-outline-color" to { cssValue, theme ->
            updateInputColors(cssValue, theme) { existing, color ->
                existing?.copy(outlineFocus = color) ?: ConciergeInputColors(outlineFocus = color)
            }
        },
        "input-send-icon-color" to { cssValue, theme ->
            updateInputColors(cssValue, theme) { existing, color ->
                existing?.copy(sendIconColor = color) ?: ConciergeInputColors(sendIconColor = color)
            }
        },
        "input-send-arrow-icon-color" to { cssValue, theme ->
            updateInputColors(cssValue, theme) { existing, color ->
                existing?.copy(sendArrowIconColor = color) ?: ConciergeInputColors(sendArrowIconColor = color)
            }
        },
        "input-send-arrow-background-color" to { cssValue, theme ->
            updateInputColors(cssValue, theme) { existing, color ->
                existing?.copy(sendArrowBackgroundColor = color) ?: ConciergeInputColors(sendArrowBackgroundColor = color)
            }
        },
        "input-mic-icon-color" to { cssValue, theme ->
            updateInputColors(cssValue, theme) { existing, color ->
                existing?.copy(micIconColor = color) ?: ConciergeInputColors(micIconColor = color)
            }
        },
        "input-mic-recording-icon-color" to { cssValue, theme ->
            updateInputColors(cssValue, theme) { existing, color ->
                existing?.copy(micRecordingIconColor = color) ?: ConciergeInputColors(micRecordingIconColor = color)
            }
        },

        // Colors - Feedback (using helper)
        "feedback-icon-btn-background" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(iconButtonBackground = color) ?: ConciergeFeedbackColors(iconButtonBackground = color)
            }
        },
        "feedback-icon-btn-hover-background" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(iconButtonHoverBackground = color) ?: ConciergeFeedbackColors(iconButtonHoverBackground = color)
            }
        },
        "feedback-sheet-background-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(sheetBackground = color) ?: ConciergeFeedbackColors(sheetBackground = color)
            }
        },
        "feedback-title-text-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(titleText = color) ?: ConciergeFeedbackColors(titleText = color)
            }
        },
        "feedback-question-text-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(questionText = color) ?: ConciergeFeedbackColors(questionText = color)
            }
        },
        "feedback-options-text-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(optionsText = color) ?: ConciergeFeedbackColors(optionsText = color)
            }
        },
        "feedback-checkbox-border-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(checkboxBorder = color) ?: ConciergeFeedbackColors(checkboxBorder = color)
            }
        },
        "feedback-notes-text-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(notesText = color) ?: ConciergeFeedbackColors(notesText = color)
            }
        },
        "feedback-drag-handle-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(dragHandle = color) ?: ConciergeFeedbackColors(dragHandle = color)
            }
        },
        "feedback-submit-button-fill-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(submitButtonFill = color) ?: ConciergeFeedbackColors(submitButtonFill = color)
            }
        },
        "feedback-submit-button-text-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(submitButtonText = color) ?: ConciergeFeedbackColors(submitButtonText = color)
            }
        },
        "feedback-cancel-button-fill-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(cancelButtonFill = color) ?: ConciergeFeedbackColors(cancelButtonFill = color)
            }
        },
        "feedback-cancel-button-text-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(cancelButtonText = color) ?: ConciergeFeedbackColors(cancelButtonText = color)
            }
        },
        "feedback-cancel-button-border-color" to { cssValue, theme ->
            updateFeedbackColors(cssValue, theme) { existing, color ->
                existing?.copy(cancelButtonBorder = color) ?: ConciergeFeedbackColors(cancelButtonBorder = color)
            }
        },

        // Colors - Disclaimer
        "disclaimer-color" to { cssValue, theme ->
            val color = CSSValueConverter.parseColor(cssValue)
            updateColors(theme) { colors ->
                colors?.copy(disclaimer = color.toHexString())
                    ?: ConciergeThemeColors(disclaimer = color.toHexString())
            }
        },
        
        // Colors - Citations (using helper)
        "citations-background-color" to { cssValue, theme ->
            updateCitationColors(cssValue, theme) { existing, color ->
                existing?.copy(backgroundColor = color) ?: ConciergeCitationColors(backgroundColor = color)
            }
        },
        "citations-text-color" to { cssValue, theme ->
            updateCitationColors(cssValue, theme) { existing, color ->
                existing?.copy(textColor = color) ?: ConciergeCitationColors(textColor = color)
            }
        },

        // Colors - Prompt Pill
        "welcome-prompt-background-color" to { cssValue, theme ->
            updateWelcomePromptColors(cssValue, theme) { existing, color ->
                existing?.copy(backgroundColor = color) ?: ConciergeWelcomePromptColors(backgroundColor = color)
            }
        },
        "welcome-prompt-text-color" to { cssValue, theme ->
            updateWelcomePromptColors(cssValue, theme) { existing, color ->
                existing?.copy(textColor = color) ?: ConciergeWelcomePromptColors(textColor = color)
            }
        },

        // Colors - Prompt Suggestions
        "suggestion-background-color" to { cssValue, theme ->
            updateSuggestionColors(cssValue, theme) { existing, color ->
                existing?.copy(backgroundColor = color) ?: ConciergeWelcomePromptColors(backgroundColor = color)
            }
        },
        "suggestion-text-color" to { cssValue, theme ->
            updateSuggestionColors(cssValue, theme) { existing, color ->
                existing?.copy(textColor = color) ?: ConciergeWelcomePromptColors(textColor = color)
            }
        },

        // Layout - Input (using helper)
        "input-height-mobile" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val height = CSSValueConverter.parsePxValue(cssValue) ?: 52.0
                layout?.copy(inputHeight = height) ?: ConciergeLayout(inputHeight = height)
            }
        },
        "input-border-radius-mobile" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = CSSValueConverter.parsePxValue(cssValue) ?: 12.0
                layout?.copy(inputBorderRadius = radius) ?: ConciergeLayout(inputBorderRadius = radius)
            }
        },
        "input-outline-width" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val width = CSSValueConverter.parsePxValue(cssValue) ?: 2.0
                layout?.copy(inputOutlineWidth = width) ?: ConciergeLayout(inputOutlineWidth = width)
            }
        },
        "input-focus-outline-width" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val width = CSSValueConverter.parsePxValue(cssValue) ?: 2.0
                layout?.copy(inputFocusOutlineWidth = width) ?: ConciergeLayout(inputFocusOutlineWidth = width)
            }
        },
        "input-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 16.0
                layout?.copy(inputFontSize = size) ?: ConciergeLayout(inputFontSize = size)
            }
        },
        "input-button-height" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val height = CSSValueConverter.parsePxValue(cssValue) ?: 32.0
                layout?.copy(inputButtonHeight = height) ?: ConciergeLayout(inputButtonHeight = height)
            }
        },
        "input-button-width" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val width = CSSValueConverter.parsePxValue(cssValue) ?: 32.0
                layout?.copy(inputButtonWidth = width) ?: ConciergeLayout(inputButtonWidth = width)
            }
        },
        "input-button-border-radius" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = CSSValueConverter.parsePxValue(cssValue) ?: 8.0
                layout?.copy(inputButtonBorderRadius = radius) ?: ConciergeLayout(inputButtonBorderRadius = radius)
            }
        },
        "input-box-shadow" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val shadow = CSSValueConverter.parseBoxShadow(cssValue)
                layout?.copy(inputBoxShadow = shadow) ?: ConciergeLayout(inputBoxShadow = shadow)
            }
        },
        
        // Layout - Message
        "message-border-radius" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = CSSValueConverter.parsePxValue(cssValue) ?: 10.0
                layout?.copy(messageBorderRadius = radius) ?: ConciergeLayout(messageBorderRadius = radius)
            }
        },
        "message-padding" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePadding(cssValue)
                layout?.copy(messagePadding = padding) ?: ConciergeLayout(messagePadding = padding)
            }
        },
        "message-max-width" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val width = CSSValueConverter.parseWidth(cssValue)
                layout?.copy(messageMaxWidth = width) ?: ConciergeLayout(messageMaxWidth = width)
            }
        },
        "agent-icon-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue)
                layout?.copy(agentIconSize = size) ?: ConciergeLayout(agentIconSize = size)
            }
        },
        "agent-icon-spacing" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val spacing = CSSValueConverter.parsePxValue(cssValue)
                layout?.copy(agentIconSpacing = spacing) ?: ConciergeLayout(agentIconSpacing = spacing)
            }
        },

        // Layout - Chat
        "chat-interface-max-width" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val width = CSSValueConverter.parsePxValue(cssValue) ?: 768.0
                layout?.copy(chatInterfaceMaxWidth = width) ?: ConciergeLayout(chatInterfaceMaxWidth = width)
            }
        },
        "chat-history-padding" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePxValue(cssValue) ?: 16.0
                layout?.copy(chatHistoryPadding = padding) ?: ConciergeLayout(chatHistoryPadding = padding)
            }
        },
        "chat-history-padding-top-expanded" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePxValue(cssValue) ?: 0.0
                layout?.copy(chatHistoryPaddingTopExpanded = padding) ?: ConciergeLayout(chatHistoryPaddingTopExpanded = padding)
            }
        },
        "chat-history-bottom-padding" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePxValue(cssValue) ?: 0.0
                layout?.copy(chatHistoryBottomPadding = padding) ?: ConciergeLayout(chatHistoryBottomPadding = padding)
            }
        },
        "message-blocker-height" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val height = CSSValueConverter.parsePxValue(cssValue) ?: 105.0
                layout?.copy(messageBlockerHeight = height) ?: ConciergeLayout(messageBlockerHeight = height)
            }
        },
        
        // Layout - Card
        "border-radius-card" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = CSSValueConverter.parsePxValue(cssValue) ?: 16.0
                layout?.copy(borderRadiusCard = radius) ?: ConciergeLayout(borderRadiusCard = radius)
            }
        },
        "multimodal-card-box-shadow" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val shadow = CSSValueConverter.parseBoxShadow(cssValue)
                layout?.copy(multimodalCardBoxShadow = shadow) ?: ConciergeLayout(multimodalCardBoxShadow = shadow)
            }
        },
        
        // Layout - Button
        "button-height-s" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val height = CSSValueConverter.parsePxValue(cssValue) ?: 30.0
                layout?.copy(buttonHeightSmall = height) ?: ConciergeLayout(buttonHeightSmall = height)
            }
        },
        
        // Layout - Feedback
        "feedback-container-gap" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val gap = CSSValueConverter.parsePxValue(cssValue) ?: 4.0
                layout?.copy(feedbackContainerGap = gap) ?: ConciergeLayout(feedbackContainerGap = gap)
            }
        },
        "feedback-submit-button-border-radius" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = CSSValueConverter.parsePxValue(cssValue) ?: 10.0
                layout?.copy(feedbackSubmitButtonBorderRadius = radius)
                    ?: ConciergeLayout(feedbackSubmitButtonBorderRadius = radius)
            }
        },
        "feedback-submit-button-font-weight" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val weight = CSSValueConverter.parseFontWeight(cssValue)
                layout?.copy(feedbackSubmitButtonFontWeight = weight)
                    ?: ConciergeLayout(feedbackSubmitButtonFontWeight = weight)
            }
        },
        "feedback-cancel-button-border-radius" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = CSSValueConverter.parsePxValue(cssValue) ?: 10.0
                layout?.copy(feedbackCancelButtonBorderRadius = radius)
                    ?: ConciergeLayout(feedbackCancelButtonBorderRadius = radius)
            }
        },
        "feedback-cancel-button-border-width" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val width = CSSValueConverter.parsePxValue(cssValue) ?: 1.0
                layout?.copy(feedbackCancelButtonBorderWidth = width)
                    ?: ConciergeLayout(feedbackCancelButtonBorderWidth = width)
            }
        },
        "feedback-cancel-button-font-weight" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val weight = CSSValueConverter.parseFontWeight(cssValue)
                layout?.copy(feedbackCancelButtonFontWeight = weight)
                    ?: ConciergeLayout(feedbackCancelButtonFontWeight = weight)
            }
        },
        "feedback-checkbox-border-radius" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = CSSValueConverter.parsePxValue(cssValue) ?: 6.0
                layout?.copy(feedbackCheckboxBorderRadius = radius)
                    ?: ConciergeLayout(feedbackCheckboxBorderRadius = radius)
            }
        },
        "feedback-title-text-align" to { cssValue, theme ->
            val alignment = ConciergeTextAlignment.fromString(cssValue)
            updateLayout(theme) { layout ->
                layout?.copy(feedbackTitleTextAlign = alignment)
                    ?: ConciergeLayout(feedbackTitleTextAlign = alignment)
            }
        },
        "feedback-title-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue)
                layout?.copy(feedbackTitleFontSize = size)
                    ?: ConciergeLayout(feedbackTitleFontSize = size)
            }
        },

        // Layout - Citations
        "citations-text-font-weight" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val weight = CSSValueConverter.parseFontWeight(cssValue)
                layout?.copy(citationsTextFontWeight = weight) ?: ConciergeLayout(citationsTextFontWeight = weight)
            }
        },
        "citations-desktop-button-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 14.0
                layout?.copy(citationsDesktopButtonFontSize = size) ?: ConciergeLayout(citationsDesktopButtonFontSize = size)
            }
        },
        
        // Layout - Disclaimer
        "disclaimer-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 12.0
                layout?.copy(disclaimerFontSize = size) ?: ConciergeLayout(disclaimerFontSize = size)
            }
        },
        "disclaimer-font-weight" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val weight = CSSValueConverter.parseFontWeight(cssValue)
                layout?.copy(disclaimerFontWeight = weight) ?: ConciergeLayout(disclaimerFontWeight = weight)
            }
        },
        
        // Layout - Welcome Order
        "welcome-input-order" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val order = CSSValueConverter.parseOrder(cssValue)
                layout?.copy(welcomeInputOrder = order) ?: ConciergeLayout(welcomeInputOrder = order)
            }
        },
        "welcome-cards-order" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val order = CSSValueConverter.parseOrder(cssValue)
                layout?.copy(welcomeCardsOrder = order) ?: ConciergeLayout(welcomeCardsOrder = order)
            }
        },
        "welcome-title-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 24.0
                layout?.copy(welcomeTitleFontSize = size) ?: ConciergeLayout(welcomeTitleFontSize = size)
            }
        },
        "welcome-text-align" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val align = cssValue.trim().lowercase()
                layout?.copy(welcomeTextAlign = align) ?: ConciergeLayout(welcomeTextAlign = align)
            }
        },
        "welcome-content-padding" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePxValue(cssValue) ?: 20.0
                layout?.copy(welcomeContentPadding = padding) ?: ConciergeLayout(welcomeContentPadding = padding)
            }
        },
        "welcome-prompt-image-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 75.0
                layout?.copy(welcomePromptImageSize = size) ?: ConciergeLayout(welcomePromptImageSize = size)
            }
        },
        "welcome-prompt-spacing" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val spacing = CSSValueConverter.parsePxValue(cssValue) ?: 8.0
                layout?.copy(welcomePromptSpacing = spacing) ?: ConciergeLayout(welcomePromptSpacing = spacing)
            }
        },
        "welcome-title-bottom-spacing" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val spacing = CSSValueConverter.parsePxValue(cssValue) ?: 8.0
                layout?.copy(welcomeTitleBottomSpacing = spacing) ?: ConciergeLayout(welcomeTitleBottomSpacing = spacing)
            }
        },
        "welcome-prompts-top-spacing" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val spacing = CSSValueConverter.parsePxValue(cssValue) ?: 8.0
                layout?.copy(welcomePromptsTopSpacing = spacing) ?: ConciergeLayout(welcomePromptsTopSpacing = spacing)
            }
        },
        "welcome-prompt-padding" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePxValue(cssValue) ?: 0.0
                layout?.copy(welcomePromptPadding = padding) ?: ConciergeLayout(welcomePromptPadding = padding)
            }
        },
        "welcome-prompt-corner-radius" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = CSSValueConverter.parsePxValue(cssValue) ?: 8.0
                layout?.copy(welcomePromptCornerRadius = radius) ?: ConciergeLayout(welcomePromptCornerRadius = radius)
            }
        },
        "suggestion-item-border-radius" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = CSSValueConverter.parsePxValue(cssValue) ?: 10.0
                layout?.copy(suggestionItemBorderRadius = radius) ?: ConciergeLayout(suggestionItemBorderRadius = radius)
            }
        },

        // Colors - Thinking Animation
        "thinking-dot-color" to { cssValue, theme ->
            updateThinkingColors(cssValue, theme) { existing, color ->
                existing?.copy(dotColor = color) ?: ConciergeThinkingColors(dotColor = color)
            }
        },

        // Layout - Thinking Animation
        "thinking-dot-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = (CSSValueConverter.parsePxValue(cssValue) ?: 8.0).coerceAtLeast(0.0)
                layout?.copy(thinkingDotSize = size) ?: ConciergeLayout(thinkingDotSize = size)
            }
        },
        "thinking-dot-spacing" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val spacing = (CSSValueConverter.parsePxValue(cssValue) ?: 8.0).coerceAtLeast(0.0)
                layout?.copy(thinkingDotSpacing = spacing) ?: ConciergeLayout(thinkingDotSpacing = spacing)
            }
        },
        "thinking-bubble-border-radius" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = (CSSValueConverter.parsePxValue(cssValue) ?: 8.0).coerceAtLeast(0.0)
                layout?.copy(thinkingBubbleBorderRadius = radius) ?: ConciergeLayout(thinkingBubbleBorderRadius = radius)
            }
        },
        "thinking-bubble-padding-horizontal" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = (CSSValueConverter.parsePxValue(cssValue) ?: 16.0).coerceAtLeast(0.0)
                layout?.copy(thinkingBubblePaddingHorizontal = padding) ?: ConciergeLayout(thinkingBubblePaddingHorizontal = padding)
            }
        },
        "thinking-bubble-padding-vertical" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = (CSSValueConverter.parsePxValue(cssValue) ?: 8.0).coerceAtLeast(0.0)
                layout?.copy(thinkingBubblePaddingVertical = padding) ?: ConciergeLayout(thinkingBubblePaddingVertical = padding)
            }
        },
        "thinking-dot-vertical-alignment" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                layout?.copy(thinkingDotVerticalAlignment = cssValue.trim()) ?: ConciergeLayout(thinkingDotVerticalAlignment = cssValue.trim())
            }
        },

        "header-title-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 24.0
                layout?.copy(headerTitleFontSize = size) ?: ConciergeLayout(headerTitleFontSize = size)
            }
        },

        // Extended product cards
        "product-card-title-font-weight" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val weight = CSSValueConverter.parseFontWeight(cssValue)
                layout?.copy(productCardTitleFontWeight = weight) ?: ConciergeLayout(productCardTitleFontWeight = weight)
            }
        },
        "product-card-title-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 12.0
                layout?.copy(productCardTitleFontSize = size) ?: ConciergeLayout(productCardTitleFontSize = size)
            }
        },
        "product-card-subtitle-font-weight" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val weight = CSSValueConverter.parseFontWeight(cssValue)
                layout?.copy(productCardSubtitleFontWeight = weight) ?: ConciergeLayout(productCardSubtitleFontWeight = weight)
            }
        },
        "product-card-subtitle-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 12.0
                layout?.copy(productCardSubtitleFontSize = size) ?: ConciergeLayout(productCardSubtitleFontSize = size)
            }
        },
        "product-card-price-font-weight" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val weight = CSSValueConverter.parseFontWeight(cssValue)
                layout?.copy(productCardPriceFontWeight = weight) ?: ConciergeLayout(productCardPriceFontWeight = weight)
            }
        },
        "product-card-price-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 12.0
                layout?.copy(productCardPriceFontSize = size) ?: ConciergeLayout(productCardPriceFontSize = size)
            }
        },
        "product-card-badge-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 12.0
                layout?.copy(productCardBadgeFontSize = size) ?: ConciergeLayout(productCardBadgeFontSize = size)
            }
        },
        "product-card-badge-font-weight" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val weight = CSSValueConverter.parseFontWeight(cssValue)
                layout?.copy(productCardBadgeFontWeight = weight) ?: ConciergeLayout(productCardBadgeFontWeight = weight)
            }
        },
        "product-card-badge-text-color" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val color = cssValue.trim()
                layout?.copy(productCardBadgeTextColor = color) ?: ConciergeLayout(productCardBadgeTextColor = color)
            }
        },
        "product-card-badge-background-color" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val color = cssValue.trim()
                layout?.copy(productCardBadgeBackgroundColor = color) ?: ConciergeLayout(productCardBadgeBackgroundColor = color)
            }
        },
        "product-card-background-color" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val color = cssValue.trim()
                layout?.copy(productCardBackgroundColor = color) ?: ConciergeLayout(productCardBackgroundColor = color)
            }
        },
        "product-card-title-color" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val color = cssValue.trim()
                layout?.copy(productCardTitleColor = color) ?: ConciergeLayout(productCardTitleColor = color)
            }
        },
        "product-card-subtitle-color" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val color = cssValue.trim()
                layout?.copy(productCardSubtitleColor = color) ?: ConciergeLayout(productCardSubtitleColor = color)
            }
        },
        "product-card-price-color" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val color = cssValue.trim()
                layout?.copy(productCardPriceColor = color) ?: ConciergeLayout(productCardPriceColor = color)
            }
        },
        "product-card-outline-color" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val color = cssValue.trim()
                layout?.copy(productCardOutlineColor = color) ?: ConciergeLayout(productCardOutlineColor = color)
            }
        },
        "product-card-width" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val width = CSSValueConverter.parsePxValue(cssValue) ?: 222.0
                layout?.copy(productCardWidth = width) ?: ConciergeLayout(productCardWidth = width)
            }
        },
        "product-card-height" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val height = CSSValueConverter.parsePxValue(cssValue) ?: 285.0
                layout?.copy(productCardHeight = height) ?: ConciergeLayout(productCardHeight = height)
            }
        },
        "product-card-border-radius" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = CSSValueConverter.parsePxValue(cssValue) ?: 8.0
                layout?.copy(productCardBorderRadius = radius) ?: ConciergeLayout(productCardBorderRadius = radius)
            }
        },
        "product-card-was-price-color" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val color = cssValue.trim()
                layout?.copy(productCardWasPriceColor = color) ?: ConciergeLayout(productCardWasPriceColor = color)
            }
        },
        "product-card-was-price-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 12.0
                layout?.copy(productCardWasPriceFontSize = size) ?: ConciergeLayout(productCardWasPriceFontSize = size)
            }
        },
        "product-card-was-price-font-weight" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val weight = CSSValueConverter.parseFontWeight(cssValue)
                layout?.copy(productCardWasPriceFontWeight = weight) ?: ConciergeLayout(productCardWasPriceFontWeight = weight)
            }
        },
        "product-card-was-price-text-prefix" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val prefix = cssValue.removeSurrounding("\"").removeSurrounding("'")
                layout?.copy(productCardWasPriceTextPrefix = prefix) ?: ConciergeLayout(productCardWasPriceTextPrefix = prefix)
            }
        },
        "product-card-text-horizontal-padding" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePxValue(cssValue) ?: 16.0
                layout?.copy(productCardTextHorizontalPadding = padding) ?: ConciergeLayout(productCardTextHorizontalPadding = padding)
            }
        },
        "product-card-text-spacing" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val spacing = CSSValueConverter.parsePxValue(cssValue) ?: 8.0
                layout?.copy(productCardTextSpacing = spacing) ?: ConciergeLayout(productCardTextSpacing = spacing)
            }
        },
        "product-card-text-top-padding" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePxValue(cssValue) ?: 24.0
                layout?.copy(productCardTextTopPadding = padding) ?: ConciergeLayout(productCardTextTopPadding = padding)
            }
        },
        "product-card-text-bottom-padding" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePxValue(cssValue) ?: 16.0
                layout?.copy(productCardTextBottomPadding = padding) ?: ConciergeLayout(productCardTextBottomPadding = padding)
            }
        },
        "product-card-carousel-horizontal-padding" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePxValue(cssValue) ?: 4.0
                layout?.copy(productCardCarouselHorizontalPadding = padding) ?: ConciergeLayout(productCardCarouselHorizontalPadding = padding)
            }
        },
        "product-card-carousel-spacing" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val spacing = CSSValueConverter.parsePxValue(cssValue) ?: 12.0
                layout?.copy(productCardCarouselSpacing = spacing) ?: ConciergeLayout(productCardCarouselSpacing = spacing)
            }
        },

        // Layout - CTA button
        "cta-button-border-radius" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val radius = CSSValueConverter.parsePxValue(cssValue) ?: 99.0
                layout?.copy(ctaButtonBorderRadius = radius) ?: ConciergeLayout(ctaButtonBorderRadius = radius)
            }
        },
        "cta-button-horizontal-padding" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePxValue(cssValue) ?: 16.0
                layout?.copy(ctaButtonHorizontalPadding = padding) ?: ConciergeLayout(ctaButtonHorizontalPadding = padding)
            }
        },
        "cta-button-vertical-padding" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val padding = CSSValueConverter.parsePxValue(cssValue) ?: 12.0
                layout?.copy(ctaButtonVerticalPadding = padding) ?: ConciergeLayout(ctaButtonVerticalPadding = padding)
            }
        },
        "cta-button-font-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 14.0
                layout?.copy(ctaButtonFontSize = size) ?: ConciergeLayout(ctaButtonFontSize = size)
            }
        },
        "cta-button-font-weight" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val weight = CSSValueConverter.parseFontWeight(cssValue)
                layout?.copy(ctaButtonFontWeight = weight) ?: ConciergeLayout(ctaButtonFontWeight = weight)
            }
        },
        "cta-button-icon-size" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val size = CSSValueConverter.parsePxValue(cssValue) ?: 16.0
                layout?.copy(ctaButtonIconSize = size) ?: ConciergeLayout(ctaButtonIconSize = size)
            }
        },

        // Colors - CTA Button (using helper)
        "cta-button-background-color" to { cssValue, theme ->
            updateCtaButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(backgroundColor = color) ?: ConciergeCtaButtonColors(backgroundColor = color)
            }
        },
        "cta-button-text-color" to { cssValue, theme ->
            updateCtaButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(textColor = color) ?: ConciergeCtaButtonColors(textColor = color)
            }
        },
        "cta-button-icon-color" to { cssValue, theme ->
            updateCtaButtonColors(cssValue, theme) { existing, color ->
                existing?.copy(iconColor = color) ?: ConciergeCtaButtonColors(iconColor = color)
            }
        },

        // Components - Feedback
        "feedback-icon-btn-size-desktop" to { cssValue, theme ->
            val size = CSSValueConverter.parsePxValue(cssValue) ?: 32.0
            theme.copy(
                components = theme.components?.copy(
                    feedback = theme.components.feedback?.copy(iconButtonSizeDesktop = size)
                        ?: ConciergeFeedbackComponent(iconButtonSizeDesktop = size)
                ) ?: ConciergeComponentsConfig(
                    feedback = ConciergeFeedbackComponent(iconButtonSizeDesktop = size)
                )
            )
        }
    )
    
    /**
     * Returns the normalized CSS keys (without the leading `--`) that are supported.
     */
    val supportedCSSKeys: Set<String> get() = cssToAssignmentMap.keys
    
    /**
     * Applies CSS value to ConciergeThemeTokens using the mapped assignment function.
     * Returns the updated theme.
     */
    fun apply(cssKey: String, cssValue: String, theme: ConciergeThemeTokens): ConciergeThemeTokens {
        // Remove -- prefix if present
        val normalizedKey = cssKey.removePrefix("--")
        
        // Find and execute the assignment function
        return cssToAssignmentMap[normalizedKey]?.invoke(cssValue, theme) ?: run {
            Log.d(LOG_TAG, "Unknown CSS key '$normalizedKey' ignored.")
            theme
        }
    }
}
