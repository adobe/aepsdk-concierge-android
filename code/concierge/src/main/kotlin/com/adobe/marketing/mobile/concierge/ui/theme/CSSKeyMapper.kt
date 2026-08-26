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

    /**
     * Applies one field-mutation on top of whatever gradient (if any) prior CSS keys for the same
     * gradient-capable input token (border outline, mic icon, send-arrow background, mic waveform)
     * already built. Each of those tokens is configured via 3 independent CSS keys (start color,
     * end color, angle) that may arrive in any order, so the existing partially-built gradient is
     * preserved and only the touched field is updated -- see [ConciergeGradientColors.toConciergeGradient]
     * for how an unset side/angle is defaulted at runtime.
     */
    private fun updateInputGradient(
        theme: ConciergeThemeTokens,
        getGradient: (ConciergeInputColors) -> ConciergeGradientColors?,
        setGradient: (ConciergeInputColors, ConciergeGradientColors) -> ConciergeInputColors,
        mutate: (ConciergeGradientColors) -> ConciergeGradientColors
    ): ConciergeThemeTokens {
        return updateColors(theme) { colors ->
            val existingInput = colors?.input ?: ConciergeInputColors()
            val updatedGradient = mutate(getGradient(existingInput) ?: ConciergeGradientColors())
            val updatedInput = setGradient(existingInput, updatedGradient)
            colors?.copy(input = updatedInput) ?: ConciergeThemeColors(input = updatedInput)
        }
    }

    /**
     * One gradient-capable input token's CSS key prefix (ex: "input-outline-gradient") plus the
     * getter/setter pair used to read/write its [ConciergeGradientColors] field on
     * [ConciergeInputColors].
     */
    private data class GradientToken(
        val cssKeyPrefix: String,
        val get: (ConciergeInputColors) -> ConciergeGradientColors?,
        val set: (ConciergeInputColors, ConciergeGradientColors) -> ConciergeInputColors
    )

    private val gradientTokens = listOf(
        GradientToken("input-outline-gradient", { it.outlineGradient }, { input, g -> input.copy(outlineGradient = g) }),
        GradientToken("input-mic-icon-gradient", { it.micIconGradient }, { input, g -> input.copy(micIconGradient = g) }),
        GradientToken(
            "input-send-arrow-background-gradient",
            { it.sendArrowBackgroundGradient },
            { input, g -> input.copy(sendArrowBackgroundGradient = g) }
        ),
        GradientToken("input-mic-waveform-gradient", { it.micWaveformGradient }, { input, g -> input.copy(micWaveformGradient = g) })
    )

    /**
     * Generates the 3 CSS key assignments (start color, end color, angle) that together configure
     * one gradient-capable input token, keyed by [token]'s `cssKeyPrefix` (ex: "input-outline-gradient"
     * -> "input-outline-gradient-start-color", "-end-color", "-angle"). Every gradient-capable token
     * shares this exact 3-key shape.
     */
    private fun gradientAssignments(token: GradientToken): Map<String, CSSAssignment> = mapOf(
        "${token.cssKeyPrefix}-start-color" to { cssValue, theme ->
            updateInputGradient(theme, token.get, token.set) {
                it.copy(startColor = CSSValueConverter.parseColor(cssValue).toHexString())
            }
        },
        "${token.cssKeyPrefix}-end-color" to { cssValue, theme ->
            updateInputGradient(theme, token.get, token.set) {
                it.copy(endColor = CSSValueConverter.parseColor(cssValue).toHexString())
            }
        },
        "${token.cssKeyPrefix}-angle" to { cssValue, theme ->
            updateInputGradient(theme, token.get, token.set) {
                it.copy(angle = CSSValueConverter.parseGradientAngle(cssValue).toDouble())
            }
        }
    )

    /** All 12 gradient CSS key assignments (4 tokens x 3 keys), merged into [cssToAssignmentMap]. */
    private val gradientCssAssignments: Map<String, CSSAssignment> =
        gradientTokens.flatMap { gradientAssignments(it).toList() }.toMap()

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
     * A CSS key plus the `.copy(...)`-based field mutation used to build one nested colors object
     * (ex: [ConciergeButtonColors]) from its existing value (or a fresh instance) and a parsed color
     * hex string.
     */
    private data class ColorToken<T>(
        val cssKey: String,
        val copyField: (T?, String) -> T
    )

    /**
     * Generates one CSS key assignment per entry in [tokens], each delegating to [updateFn] (one of
     * the per-nested-type `updateXColors` helpers above) with that token's field mutation.
     */
    private fun <T> colorAssignments(
        tokens: List<ColorToken<T>>,
        updateFn: (String, ConciergeThemeTokens, (T?, String) -> T) -> ConciergeThemeTokens
    ): Map<String, CSSAssignment> = tokens.associate { token ->
        token.cssKey to { cssValue: String, theme: ConciergeThemeTokens -> updateFn(cssValue, theme, token.copyField) }
    }

    private val primaryColorTokens: List<ColorToken<ConciergePrimaryColors>> = listOf(
        ColorToken("color-primary") { existing, color -> existing?.copy(primary = color) ?: ConciergePrimaryColors(primary = color) },
        ColorToken("color-text") { existing, color -> existing?.copy(text = color) ?: ConciergePrimaryColors(text = color) }
    )

    private val surfaceColorTokens: List<ColorToken<ConciergeSurfaceColors>> = listOf(
        ColorToken("main-container-background") { existing, color ->
            existing?.copy(mainContainerBackground = color) ?: ConciergeSurfaceColors(mainContainerBackground = color)
        },
        ColorToken("main-container-bottom-background") { existing, color ->
            existing?.copy(mainContainerBottomBackground = color) ?: ConciergeSurfaceColors(mainContainerBottomBackground = color)
        },
        ColorToken("message-blocker-background") { existing, color ->
            existing?.copy(messageBlockerBackground = color) ?: ConciergeSurfaceColors(messageBlockerBackground = color)
        }
    )

    private val messageColorTokens: List<ColorToken<ConciergeMessageColors>> = listOf(
        ColorToken("message-user-background") { existing, color -> existing?.copy(userBackground = color) ?: ConciergeMessageColors(userBackground = color) },
        ColorToken("message-user-text") { existing, color -> existing?.copy(userText = color) ?: ConciergeMessageColors(userText = color) },
        ColorToken("message-concierge-background") { existing, color ->
            existing?.copy(conciergeBackground = color) ?: ConciergeMessageColors(conciergeBackground = color)
        },
        ColorToken("message-concierge-text") { existing, color -> existing?.copy(conciergeText = color) ?: ConciergeMessageColors(conciergeText = color) },
        ColorToken("message-concierge-link-color") { existing, color -> existing?.copy(conciergeLink = color) ?: ConciergeMessageColors(conciergeLink = color) }
    )

    private val buttonColorTokens: List<ColorToken<ConciergeButtonColors>> = listOf(
        ColorToken("button-primary-background") { existing, color -> existing?.copy(primaryBackground = color) ?: ConciergeButtonColors(primaryBackground = color) },
        ColorToken("button-primary-text") { existing, color -> existing?.copy(primaryText = color) ?: ConciergeButtonColors(primaryText = color) },
        ColorToken("button-primary-hover") { existing, color -> existing?.copy(primaryHover = color) ?: ConciergeButtonColors(primaryHover = color) },
        ColorToken("button-secondary-border") { existing, color -> existing?.copy(secondaryBorder = color) ?: ConciergeButtonColors(secondaryBorder = color) },
        ColorToken("button-secondary-text") { existing, color -> existing?.copy(secondaryText = color) ?: ConciergeButtonColors(secondaryText = color) },
        ColorToken("button-secondary-hover") { existing, color -> existing?.copy(secondaryHover = color) ?: ConciergeButtonColors(secondaryHover = color) },
        ColorToken("color-button-secondary-hover-text") { existing, color ->
            existing?.copy(secondaryHoverText = color) ?: ConciergeButtonColors(secondaryHoverText = color)
        },
        ColorToken("submit-button-fill-color") { existing, color -> existing?.copy(submitFill = color) ?: ConciergeButtonColors(submitFill = color) },
        ColorToken("submit-button-fill-color-disabled") { existing, color ->
            existing?.copy(submitFillDisabled = color) ?: ConciergeButtonColors(submitFillDisabled = color)
        },
        ColorToken("color-button-submit") { existing, color -> existing?.copy(submitText = color) ?: ConciergeButtonColors(submitText = color) },
        ColorToken("color-button-submit-hover") { existing, color -> existing?.copy(submitTextHover = color) ?: ConciergeButtonColors(submitTextHover = color) },
        ColorToken("button-disabled-background") { existing, color -> existing?.copy(disabledBackground = color) ?: ConciergeButtonColors(disabledBackground = color) }
    )

    private val inputColorTokens: List<ColorToken<ConciergeInputColors>> = listOf(
        ColorToken("input-background") { existing, color -> existing?.copy(background = color) ?: ConciergeInputColors(background = color) },
        ColorToken("input-text-color") { existing, color -> existing?.copy(text = color) ?: ConciergeInputColors(text = color) },
        ColorToken("input-focus-outline-color") { existing, color -> existing?.copy(outlineFocus = color) ?: ConciergeInputColors(outlineFocus = color) },
        ColorToken("input-send-icon-color") { existing, color -> existing?.copy(sendIconColor = color) ?: ConciergeInputColors(sendIconColor = color) },
        ColorToken("input-send-arrow-icon-color") { existing, color -> existing?.copy(sendArrowIconColor = color) ?: ConciergeInputColors(sendArrowIconColor = color) },
        ColorToken("input-send-arrow-background-color") { existing, color ->
            existing?.copy(sendArrowBackgroundColor = color) ?: ConciergeInputColors(sendArrowBackgroundColor = color)
        },
        ColorToken("input-mic-icon-color") { existing, color -> existing?.copy(micIconColor = color) ?: ConciergeInputColors(micIconColor = color) },
        ColorToken("input-mic-recording-icon-color") { existing, color ->
            existing?.copy(micRecordingIconColor = color) ?: ConciergeInputColors(micRecordingIconColor = color)
        }
    )

    private val feedbackColorTokens: List<ColorToken<ConciergeFeedbackColors>> = listOf(
        ColorToken("feedback-icon-btn-background") { existing, color -> existing?.copy(iconButtonBackground = color) ?: ConciergeFeedbackColors(iconButtonBackground = color) },
        ColorToken("feedback-icon-btn-hover-background") { existing, color ->
            existing?.copy(iconButtonHoverBackground = color) ?: ConciergeFeedbackColors(iconButtonHoverBackground = color)
        },
        ColorToken("feedback-sheet-background-color") { existing, color -> existing?.copy(sheetBackground = color) ?: ConciergeFeedbackColors(sheetBackground = color) },
        ColorToken("feedback-title-text-color") { existing, color -> existing?.copy(titleText = color) ?: ConciergeFeedbackColors(titleText = color) },
        ColorToken("feedback-question-text-color") { existing, color -> existing?.copy(questionText = color) ?: ConciergeFeedbackColors(questionText = color) },
        ColorToken("feedback-options-text-color") { existing, color -> existing?.copy(optionsText = color) ?: ConciergeFeedbackColors(optionsText = color) },
        ColorToken("feedback-checkbox-border-color") { existing, color -> existing?.copy(checkboxBorder = color) ?: ConciergeFeedbackColors(checkboxBorder = color) },
        ColorToken("feedback-drag-handle-color") { existing, color -> existing?.copy(dragHandle = color) ?: ConciergeFeedbackColors(dragHandle = color) },
        ColorToken("feedback-submit-button-fill-color") { existing, color ->
            existing?.copy(submitButtonFill = color) ?: ConciergeFeedbackColors(submitButtonFill = color)
        },
        ColorToken("feedback-submit-button-text-color") { existing, color ->
            existing?.copy(submitButtonText = color) ?: ConciergeFeedbackColors(submitButtonText = color)
        },
        ColorToken("feedback-cancel-button-fill-color") { existing, color ->
            existing?.copy(cancelButtonFill = color) ?: ConciergeFeedbackColors(cancelButtonFill = color)
        },
        ColorToken("feedback-cancel-button-text-color") { existing, color ->
            existing?.copy(cancelButtonText = color) ?: ConciergeFeedbackColors(cancelButtonText = color)
        },
        ColorToken("feedback-cancel-button-border-color") { existing, color ->
            existing?.copy(cancelButtonBorder = color) ?: ConciergeFeedbackColors(cancelButtonBorder = color)
        }
    )

    private val citationColorTokens: List<ColorToken<ConciergeCitationColors>> = listOf(
        ColorToken("citations-background-color") { existing, color -> existing?.copy(backgroundColor = color) ?: ConciergeCitationColors(backgroundColor = color) },
        ColorToken("citations-text-color") { existing, color -> existing?.copy(textColor = color) ?: ConciergeCitationColors(textColor = color) }
    )

    private val welcomePromptColorTokens: List<ColorToken<ConciergeWelcomePromptColors>> = listOf(
        ColorToken("welcome-prompt-background-color") { existing, color -> existing?.copy(backgroundColor = color) ?: ConciergeWelcomePromptColors(backgroundColor = color) },
        ColorToken("welcome-prompt-text-color") { existing, color -> existing?.copy(textColor = color) ?: ConciergeWelcomePromptColors(textColor = color) }
    )

    private val suggestionColorTokens: List<ColorToken<ConciergeWelcomePromptColors>> = listOf(
        ColorToken("suggestion-background-color") { existing, color -> existing?.copy(backgroundColor = color) ?: ConciergeWelcomePromptColors(backgroundColor = color) },
        ColorToken("suggestion-text-color") { existing, color -> existing?.copy(textColor = color) ?: ConciergeWelcomePromptColors(textColor = color) }
    )

    private val thinkingColorTokens: List<ColorToken<ConciergeThinkingColors>> = listOf(
        ColorToken("thinking-dot-color") { existing, color -> existing?.copy(dotColor = color) ?: ConciergeThinkingColors(dotColor = color) }
    )

    private val ctaButtonColorTokens: List<ColorToken<ConciergeCtaButtonColors>> = listOf(
        ColorToken("cta-button-background-color") { existing, color -> existing?.copy(backgroundColor = color) ?: ConciergeCtaButtonColors(backgroundColor = color) },
        ColorToken("cta-button-text-color") { existing, color -> existing?.copy(textColor = color) ?: ConciergeCtaButtonColors(textColor = color) },
        ColorToken("cta-button-icon-color") { existing, color -> existing?.copy(iconColor = color) ?: ConciergeCtaButtonColors(iconColor = color) }
    )

    /** All uniform single-field solid-color CSS key assignments, merged into [cssToAssignmentMap]. */
    private val colorCssAssignments: Map<String, CSSAssignment> =
        colorAssignments(primaryColorTokens, ::updatePrimaryColors) +
            colorAssignments(surfaceColorTokens, ::updateSurfaceColors) +
            colorAssignments(messageColorTokens, ::updateMessageColors) +
            colorAssignments(buttonColorTokens, ::updateButtonColors) +
            colorAssignments(inputColorTokens, ::updateInputColors) +
            colorAssignments(feedbackColorTokens, ::updateFeedbackColors) +
            colorAssignments(citationColorTokens, ::updateCitationColors) +
            colorAssignments(welcomePromptColorTokens, ::updateWelcomePromptColors) +
            colorAssignments(suggestionColorTokens, ::updateSuggestionColors) +
            colorAssignments(thinkingColorTokens, ::updateThinkingColors) +
            colorAssignments(ctaButtonColorTokens, ::updateCtaButtonColors)

    /**
     * Mapping from CSS variable name (without --) to direct assignment function
     */
    private val cssToAssignmentMap: Map<String, CSSAssignment> = mapOf<String, CSSAssignment>(
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
        
        // Colors - Primary/Surface/Message/Button/Input/Feedback/Citations/Prompt Pill/Prompt Suggestions
        // are generated below via colorCssAssignments -- the couple of bespoke ones that don't fit the
        // uniform single-field-color-parse shape stay inline here.
        "color-container" to { cssValue, theme ->
            val color = CSSValueConverter.parseColor(cssValue)
            updateColors(theme) { colors ->
                colors?.copy(container = color.toHexString())
                    ?: ConciergeThemeColors(container = color.toHexString())
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
        "disclaimer-color" to { cssValue, theme ->
            val color = CSSValueConverter.parseColor(cssValue)
            updateColors(theme) { colors ->
                colors?.copy(disclaimer = color.toHexString())
                    ?: ConciergeThemeColors(disclaimer = color.toHexString())
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
                val height = CSSValueConverter.parsePxValue(cssValue) ?: 24.0
                layout?.copy(inputButtonHeight = height) ?: ConciergeLayout(inputButtonHeight = height)
            }
        },
        "input-button-width" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val width = CSSValueConverter.parsePxValue(cssValue) ?: 24.0
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

        // Colors - Thinking Animation: "thinking-dot-color" is generated via colorCssAssignments.

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
                val width = CSSValueConverter.parsePxValue(cssValue) ?: 250.0
                layout?.copy(productCardWidth = width) ?: ConciergeLayout(productCardWidth = width)
            }
        },
        "product-card-min-height" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val height = CSSValueConverter.parsePxValue(cssValue) ?: 240.0
                layout?.copy(productCardMinHeight = height) ?: ConciergeLayout(productCardMinHeight = height)
            }
        },
        "product-card-max-height" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val height = CSSValueConverter.parsePxValue(cssValue) ?: 360.0
                layout?.copy(productCardMaxHeight = height) ?: ConciergeLayout(productCardMaxHeight = height)
            }
        },
        "product-image-width" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val width = CSSValueConverter.parsePxValue(cssValue) ?: 190.0
                layout?.copy(productImageWidth = width) ?: ConciergeLayout(productImageWidth = width)
            }
        },
        "product-image-height" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val height = CSSValueConverter.parsePxValue(cssValue) ?: 190.0
                layout?.copy(productImageHeight = height) ?: ConciergeLayout(productImageHeight = height)
            }
        },
        "product-image-scale" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val scale = cssValue.trim().lowercase()
                layout?.copy(productImageScale = scale) ?: ConciergeLayout(productImageScale = scale)
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
        "product-card-title-subtitle-spacing" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val spacing = CSSValueConverter.parsePxValue(cssValue) ?: 8.0
                layout?.copy(productCardTitleSubtitleSpacing = spacing) ?: ConciergeLayout(productCardTitleSubtitleSpacing = spacing)
            }
        },
        "product-card-section-spacing" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val spacing = CSSValueConverter.parsePxValue(cssValue) ?: 8.0
                layout?.copy(productCardSectionSpacing = spacing) ?: ConciergeLayout(productCardSectionSpacing = spacing)
            }
        },
        "product-card-price-spacing" to { cssValue, theme ->
            updateLayout(theme) { layout ->
                val spacing = CSSValueConverter.parsePxValue(cssValue) ?: 8.0
                layout?.copy(productCardPriceSpacing = spacing) ?: ConciergeLayout(productCardPriceSpacing = spacing)
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

        // Colors - CTA Button: "cta-button-background/text/icon-color" are generated via colorCssAssignments.

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
    ) + gradientCssAssignments + colorCssAssignments

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
