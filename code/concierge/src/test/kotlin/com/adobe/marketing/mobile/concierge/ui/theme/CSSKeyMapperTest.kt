/*
 * Copyright 2026 Adobe. All rights reserved.
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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [CSSKeyMapper.apply] covering all CSS variable categories.
 */
class CSSKeyMapperTest {

    private val emptyTheme = ConciergeThemeTokens()

    // -----------------------------------------------------------------------
    // Key normalization and unknown keys
    // -----------------------------------------------------------------------

    @Test
    fun `apply normalizes double-dash prefix`() {
        val result = CSSKeyMapper.apply("--color-primary", "#EB1000", emptyTheme)
        assertNotNull(result.colors?.primaryColors?.primary)
    }

    @Test
    fun `apply works without double-dash prefix`() {
        val result = CSSKeyMapper.apply("color-primary", "#EB1000", emptyTheme)
        assertNotNull(result.colors?.primaryColors?.primary)
    }

    @Test
    fun `apply ignores unknown key and returns theme unchanged`() {
        val result = CSSKeyMapper.apply("--unknown-key-xyz", "someValue", emptyTheme)
        assertEquals(emptyTheme, result)
    }

    @Test
    fun `supportedCSSKeys contains all expected keys`() {
        val keys = CSSKeyMapper.supportedCSSKeys
        assertTrue(keys.contains("color-primary"))
        assertTrue(keys.contains("product-card-border-radius"))
        assertTrue(keys.contains("feedback-icon-btn-size-desktop"))
    }

    @Test
    fun `supportedCSSKeys registers all 12 feedback dialog color keys`() {
        val keys = CSSKeyMapper.supportedCSSKeys
        val feedbackColorKeys = setOf(
            "feedback-sheet-background-color",
            "feedback-title-text-color",
            "feedback-question-text-color",
            "feedback-options-text-color",
            "feedback-checkbox-border-color",
            "feedback-drag-handle-color",
            "feedback-submit-button-fill-color",
            "feedback-submit-button-text-color",
            "feedback-cancel-button-fill-color",
            "feedback-cancel-button-text-color",
            "feedback-cancel-button-border-color"
        )
        val missing = feedbackColorKeys - keys
        assertTrue("CSSKeyMapper is missing feedback color keys: ${missing.sorted().joinToString()}", missing.isEmpty())
    }

    @Test
    fun `supportedCSSKeys registers all 8 feedback dialog layout keys`() {
        val keys = CSSKeyMapper.supportedCSSKeys
        val feedbackLayoutKeys = setOf(
            "feedback-submit-button-border-radius",
            "feedback-cancel-button-border-radius",
            "feedback-cancel-button-border-width",
            "feedback-submit-button-font-weight",
            "feedback-cancel-button-font-weight",
            "feedback-checkbox-border-radius",
            "feedback-title-text-align",
            "feedback-title-font-size"
        )
        val missing = feedbackLayoutKeys - keys
        assertTrue("CSSKeyMapper is missing feedback layout keys: ${missing.sorted().joinToString()}", missing.isEmpty())
    }

    // -----------------------------------------------------------------------
    // Typography
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps font-family to typography`() {
        val result = CSSKeyMapper.apply("--font-family", "\"Adobe Clean\", sans-serif", emptyTheme)
        assertEquals("\"Adobe Clean\", sans-serif", result.typography?.fontFamily)
    }

    @Test
    fun `apply maps line-height-body to typography`() {
        val result = CSSKeyMapper.apply("--line-height-body", "1.5", emptyTheme)
        assertEquals(1.5, result.typography?.lineHeight)
    }

    // -----------------------------------------------------------------------
    // Primary colors
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps color-primary`() {
        val result = CSSKeyMapper.apply("--color-primary", "#EB1000", emptyTheme)
        assertEquals("#EB1000", result.colors?.primaryColors?.primary)
    }

    @Test
    fun `apply maps color-text`() {
        val result = CSSKeyMapper.apply("--color-text", "#2C2C2C", emptyTheme)
        assertEquals("#2C2C2C", result.colors?.primaryColors?.text)
    }

    @Test
    fun `apply maps color-container`() {
        val result = CSSKeyMapper.apply("--color-container", "#F0F0F0", emptyTheme)
        assertEquals("#F0F0F0", result.colors?.container)
    }

    @Test
    fun `apply builds up both primary colors from a realistic sequence of keys without clobbering earlier ones`() {
        val cssBlock = listOf("color-primary" to "#EB1000", "color-text" to "#2C2C2C")
        val theme = cssBlock.fold(emptyTheme) { acc, (key, value) -> CSSKeyMapper.apply("--$key", value, acc) }

        assertEquals("#EB1000", theme.colors?.primaryColors?.primary)
        assertEquals("#2C2C2C", theme.colors?.primaryColors?.text)
    }

    // -----------------------------------------------------------------------
    // Surface colors
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps main-container-background`() {
        val result = CSSKeyMapper.apply("--main-container-background", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.surfaceColors?.mainContainerBackground)
    }

    @Test
    fun `apply maps main-container-bottom-background`() {
        val result = CSSKeyMapper.apply("--main-container-bottom-background", "#F5F5F5", emptyTheme)
        assertEquals("#F5F5F5", result.colors?.surfaceColors?.mainContainerBottomBackground)
    }

    @Test
    fun `apply maps message-blocker-background`() {
        val result = CSSKeyMapper.apply("--message-blocker-background", "#000000", emptyTheme)
        assertEquals("#000000", result.colors?.surfaceColors?.messageBlockerBackground)
    }

    @Test
    fun `apply builds up all surface colors from a realistic sequence of keys without clobbering earlier ones`() {
        val cssBlock = listOf(
            "main-container-background" to "#FFFFFF",
            "main-container-bottom-background" to "#F5F5F5",
            "message-blocker-background" to "#000000"
        )
        val theme = cssBlock.fold(emptyTheme) { acc, (key, value) -> CSSKeyMapper.apply("--$key", value, acc) }

        val surface = theme.colors?.surfaceColors
        assertEquals("#FFFFFF", surface?.mainContainerBackground)
        assertEquals("#F5F5F5", surface?.mainContainerBottomBackground)
        assertEquals("#000000", surface?.messageBlockerBackground)
    }

    // -----------------------------------------------------------------------
    // Message colors
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps message-user-background`() {
        val result = CSSKeyMapper.apply("--message-user-background", "#EBEEFF", emptyTheme)
        assertEquals("#EBEEFF", result.colors?.message?.userBackground)
    }

    @Test
    fun `apply maps message-user-text`() {
        val result = CSSKeyMapper.apply("--message-user-text", "#292929", emptyTheme)
        assertEquals("#292929", result.colors?.message?.userText)
    }

    @Test
    fun `apply maps message-concierge-background`() {
        val result = CSSKeyMapper.apply("--message-concierge-background", "#F5F5F5", emptyTheme)
        assertEquals("#F5F5F5", result.colors?.message?.conciergeBackground)
    }

    @Test
    fun `apply maps message-concierge-text`() {
        val result = CSSKeyMapper.apply("--message-concierge-text", "#292929", emptyTheme)
        assertEquals("#292929", result.colors?.message?.conciergeText)
    }

    @Test
    fun `apply maps message-concierge-link-color`() {
        val result = CSSKeyMapper.apply("--message-concierge-link-color", "#274DEA", emptyTheme)
        assertEquals("#274DEA", result.colors?.message?.conciergeLink)
    }

    @Test
    fun `apply builds up all message colors from a realistic sequence of keys without clobbering earlier ones`() {
        val cssBlock = listOf(
            "message-user-background" to "#EBEEFF",
            "message-user-text" to "#292929",
            "message-concierge-background" to "#F5F5F5",
            "message-concierge-text" to "#292929",
            "message-concierge-link-color" to "#274DEA"
        )
        val theme = cssBlock.fold(emptyTheme) { acc, (key, value) -> CSSKeyMapper.apply("--$key", value, acc) }

        val message = theme.colors?.message
        assertEquals("#EBEEFF", message?.userBackground)
        assertEquals("#292929", message?.userText)
        assertEquals("#F5F5F5", message?.conciergeBackground)
        assertEquals("#292929", message?.conciergeText)
        assertEquals("#274DEA", message?.conciergeLink)
    }

    // -----------------------------------------------------------------------
    // Button colors
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps button-primary-background`() {
        val result = CSSKeyMapper.apply("--button-primary-background", "#3B63FB", emptyTheme)
        assertEquals("#3B63FB", result.colors?.button?.primaryBackground)
    }

    @Test
    fun `apply maps button-primary-text`() {
        val result = CSSKeyMapper.apply("--button-primary-text", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.button?.primaryText)
    }

    @Test
    fun `apply maps button-primary-hover`() {
        val result = CSSKeyMapper.apply("--button-primary-hover", "#274DEA", emptyTheme)
        assertEquals("#274DEA", result.colors?.button?.primaryHover)
    }

    @Test
    fun `apply maps button-secondary-border`() {
        val result = CSSKeyMapper.apply("--button-secondary-border", "#2C2C2C", emptyTheme)
        assertEquals("#2C2C2C", result.colors?.button?.secondaryBorder)
    }

    @Test
    fun `apply maps button-secondary-text`() {
        val result = CSSKeyMapper.apply("--button-secondary-text", "#2C2C2C", emptyTheme)
        assertEquals("#2C2C2C", result.colors?.button?.secondaryText)
    }

    @Test
    fun `apply maps button-secondary-hover`() {
        val result = CSSKeyMapper.apply("--button-secondary-hover", "#000000", emptyTheme)
        assertEquals("#000000", result.colors?.button?.secondaryHover)
    }

    @Test
    fun `apply maps color-button-secondary-hover-text`() {
        val result = CSSKeyMapper.apply("--color-button-secondary-hover-text", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.button?.secondaryHoverText)
    }

    @Test
    fun `apply maps submit-button-fill-color`() {
        val result = CSSKeyMapper.apply("--submit-button-fill-color", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.button?.submitFill)
    }

    @Test
    fun `apply maps submit-button-fill-color-disabled`() {
        val result = CSSKeyMapper.apply("--submit-button-fill-color-disabled", "#C6C6C6", emptyTheme)
        assertEquals("#C6C6C6", result.colors?.button?.submitFillDisabled)
    }

    @Test
    fun `apply maps color-button-submit`() {
        val result = CSSKeyMapper.apply("--color-button-submit", "#292929", emptyTheme)
        assertEquals("#292929", result.colors?.button?.submitText)
    }

    @Test
    fun `apply maps color-button-submit-hover`() {
        val result = CSSKeyMapper.apply("--color-button-submit-hover", "#292929", emptyTheme)
        assertEquals("#292929", result.colors?.button?.submitTextHover)
    }

    @Test
    fun `apply maps button-disabled-background`() {
        val result = CSSKeyMapper.apply("--button-disabled-background", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.button?.disabledBackground)
    }

    @Test
    fun `apply builds up all button colors from a realistic sequence of keys without clobbering earlier ones`() {
        val cssBlock = listOf(
            "button-primary-background" to "#3B63FB",
            "button-primary-text" to "#FFFFFF",
            "button-primary-hover" to "#274DEA",
            "button-secondary-border" to "#2C2C2C",
            "button-secondary-text" to "#2C2C2C",
            "button-secondary-hover" to "#000000",
            "color-button-secondary-hover-text" to "#FFFFFF",
            "submit-button-fill-color" to "#FFFFFF",
            "submit-button-fill-color-disabled" to "#C6C6C6",
            "color-button-submit" to "#292929",
            "color-button-submit-hover" to "#292929",
            "button-disabled-background" to "#EEEEEE"
        )
        val theme = cssBlock.fold(emptyTheme) { acc, (key, value) -> CSSKeyMapper.apply("--$key", value, acc) }

        val button = theme.colors?.button
        assertEquals("#3B63FB", button?.primaryBackground)
        assertEquals("#FFFFFF", button?.primaryText)
        assertEquals("#274DEA", button?.primaryHover)
        assertEquals("#2C2C2C", button?.secondaryBorder)
        assertEquals("#2C2C2C", button?.secondaryText)
        assertEquals("#000000", button?.secondaryHover)
        assertEquals("#FFFFFF", button?.secondaryHoverText)
        assertEquals("#FFFFFF", button?.submitFill)
        assertEquals("#C6C6C6", button?.submitFillDisabled)
        assertEquals("#292929", button?.submitText)
        assertEquals("#292929", button?.submitTextHover)
        assertEquals("#EEEEEE", button?.disabledBackground)
    }

    // -----------------------------------------------------------------------
    // Input colors
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps input-background`() {
        val result = CSSKeyMapper.apply("--input-background", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.input?.background)
    }

    @Test
    fun `apply maps input-text-color`() {
        val result = CSSKeyMapper.apply("--input-text-color", "#292929", emptyTheme)
        assertEquals("#292929", result.colors?.input?.text)
    }

    @Test
    fun `apply maps input-outline-color solid`() {
        val result = CSSKeyMapper.apply("--input-outline-color", "#4B75FF", emptyTheme)
        assertEquals("#4B75FF", result.colors?.input?.outline)
    }

    @Test
    fun `apply maps input-outline-color gradient to null`() {
        val result = CSSKeyMapper.apply(
            "--input-outline-color",
            "linear-gradient(90deg, #FF0000 0%, #FF8800 100%)",
            emptyTheme
        )
        assertNull(result.colors?.input?.outline)
    }

    @Test
    fun `apply maps input-focus-outline-color`() {
        val result = CSSKeyMapper.apply("--input-focus-outline-color", "#4B75FF", emptyTheme)
        assertEquals("#4B75FF", result.colors?.input?.outlineFocus)
    }

    // -----------------------------------------------------------------------
    // Feedback colors
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps feedback-icon-btn-background`() {
        val result = CSSKeyMapper.apply("--feedback-icon-btn-background", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.feedback?.iconButtonBackground)
    }

    @Test
    fun `apply maps feedback-icon-btn-hover-background`() {
        val result = CSSKeyMapper.apply("--feedback-icon-btn-hover-background", "#F0F0F0", emptyTheme)
        assertEquals("#F0F0F0", result.colors?.feedback?.iconButtonHoverBackground)
    }

    // -----------------------------------------------------------------------
    // Disclaimer color
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps disclaimer-color`() {
        val result = CSSKeyMapper.apply("--disclaimer-color", "#4B4B4B", emptyTheme)
        assertEquals("#4B4B4B", result.colors?.disclaimer)
    }

    // -----------------------------------------------------------------------
    // Citation colors
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps citations-background-color`() {
        val result = CSSKeyMapper.apply("--citations-background-color", "#4B4B4B", emptyTheme)
        assertEquals("#4B4B4B", result.colors?.citation?.backgroundColor)
    }

    @Test
    fun `apply maps citations-text-color`() {
        val result = CSSKeyMapper.apply("--citations-text-color", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.citation?.textColor)
    }

    @Test
    fun `apply builds up both citation colors from a realistic sequence of keys without clobbering earlier ones`() {
        val cssBlock = listOf("citations-background-color" to "#4B4B4B", "citations-text-color" to "#FFFFFF")
        val theme = cssBlock.fold(emptyTheme) { acc, (key, value) -> CSSKeyMapper.apply("--$key", value, acc) }

        assertEquals("#4B4B4B", theme.colors?.citation?.backgroundColor)
        assertEquals("#FFFFFF", theme.colors?.citation?.textColor)
    }

    // -----------------------------------------------------------------------
    // Layout - Input
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps input-height-mobile`() {
        val result = CSSKeyMapper.apply("--input-height-mobile", "52px", emptyTheme)
        assertEquals(52.0, result.cssLayout?.inputHeight)
    }

    @Test
    fun `apply maps input-border-radius-mobile`() {
        val result = CSSKeyMapper.apply("--input-border-radius-mobile", "12px", emptyTheme)
        assertEquals(12.0, result.cssLayout?.inputBorderRadius)
    }

    @Test
    fun `apply maps input-outline-width`() {
        val result = CSSKeyMapper.apply("--input-outline-width", "2px", emptyTheme)
        assertEquals(2.0, result.cssLayout?.inputOutlineWidth)
    }

    @Test
    fun `apply maps input-focus-outline-width`() {
        val result = CSSKeyMapper.apply("--input-focus-outline-width", "2px", emptyTheme)
        assertEquals(2.0, result.cssLayout?.inputFocusOutlineWidth)
    }

    @Test
    fun `apply maps input-font-size`() {
        val result = CSSKeyMapper.apply("--input-font-size", "16px", emptyTheme)
        assertEquals(16.0, result.cssLayout?.inputFontSize)
    }

    @Test
    fun `apply maps input-button-height`() {
        val result = CSSKeyMapper.apply("--input-button-height", "32px", emptyTheme)
        assertEquals(32.0, result.cssLayout?.inputButtonHeight)
    }

    @Test
    fun `apply maps input-button-width`() {
        val result = CSSKeyMapper.apply("--input-button-width", "32px", emptyTheme)
        assertEquals(32.0, result.cssLayout?.inputButtonWidth)
    }

    @Test
    fun `apply maps input-button-border-radius`() {
        val result = CSSKeyMapper.apply("--input-button-border-radius", "8px", emptyTheme)
        assertEquals(8.0, result.cssLayout?.inputButtonBorderRadius)
    }

    @Test
    fun `apply maps input-box-shadow`() {
        val result = CSSKeyMapper.apply("--input-box-shadow", "0 2px 8px rgba(0, 0, 0, 0.1)", emptyTheme)
        assertNotNull(result.cssLayout?.inputBoxShadow)
    }

    // -----------------------------------------------------------------------
    // Layout - Message
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps message-border-radius`() {
        val result = CSSKeyMapper.apply("--message-border-radius", "10px", emptyTheme)
        assertEquals(10.0, result.cssLayout?.messageBorderRadius)
    }

    @Test
    fun `apply maps message-padding`() {
        val result = CSSKeyMapper.apply("--message-padding", "8px 16px", emptyTheme)
        assertNotNull(result.cssLayout?.messagePadding)
        assertEquals(8.0, result.cssLayout?.messagePadding?.get(0))
        assertEquals(16.0, result.cssLayout?.messagePadding?.get(1))
    }

    @Test
    fun `apply maps message-max-width percentage`() {
        val result = CSSKeyMapper.apply("--message-max-width", "100%", emptyTheme)
        assertNotNull(result.cssLayout?.messageMaxWidth)
    }

    @Test
    fun `apply maps agent-icon-size`() {
        val result = CSSKeyMapper.apply("--agent-icon-size", "39px", emptyTheme)
        assertEquals(39.0, result.cssLayout?.agentIconSize)
    }

    @Test
    fun `apply maps agent-icon-spacing`() {
        val result = CSSKeyMapper.apply("--agent-icon-spacing", "12px", emptyTheme)
        assertEquals(12.0, result.cssLayout?.agentIconSpacing)
    }

    // -----------------------------------------------------------------------
    // Layout - Chat
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps chat-interface-max-width`() {
        val result = CSSKeyMapper.apply("--chat-interface-max-width", "768px", emptyTheme)
        assertEquals(768.0, result.cssLayout?.chatInterfaceMaxWidth)
    }

    @Test
    fun `apply maps chat-history-padding`() {
        val result = CSSKeyMapper.apply("--chat-history-padding", "16px", emptyTheme)
        assertEquals(16.0, result.cssLayout?.chatHistoryPadding)
    }

    @Test
    fun `apply maps chat-history-padding-top-expanded`() {
        val result = CSSKeyMapper.apply("--chat-history-padding-top-expanded", "0px", emptyTheme)
        assertEquals(0.0, result.cssLayout?.chatHistoryPaddingTopExpanded)
    }

    @Test
    fun `apply maps chat-history-bottom-padding`() {
        val result = CSSKeyMapper.apply("--chat-history-bottom-padding", "0px", emptyTheme)
        assertEquals(0.0, result.cssLayout?.chatHistoryBottomPadding)
    }

    @Test
    fun `apply maps message-blocker-height`() {
        val result = CSSKeyMapper.apply("--message-blocker-height", "105px", emptyTheme)
        assertEquals(105.0, result.cssLayout?.messageBlockerHeight)
    }

    // -----------------------------------------------------------------------
    // Layout - Card
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps border-radius-card`() {
        val result = CSSKeyMapper.apply("--border-radius-card", "16px", emptyTheme)
        assertEquals(16.0, result.cssLayout?.borderRadiusCard)
    }

    @Test
    fun `apply maps multimodal-card-box-shadow none`() {
        val result = CSSKeyMapper.apply("--multimodal-card-box-shadow", "none", emptyTheme)
        // "none" parses to a null or empty shadow map — layout object is created
        assertNotNull(result.cssLayout)
    }

    // -----------------------------------------------------------------------
    // Layout - Button / Feedback / Citations / Disclaimer
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps button-height-s`() {
        val result = CSSKeyMapper.apply("--button-height-s", "30px", emptyTheme)
        assertEquals(30.0, result.cssLayout?.buttonHeightSmall)
    }

    @Test
    fun `apply maps feedback-container-gap`() {
        val result = CSSKeyMapper.apply("--feedback-container-gap", "4px", emptyTheme)
        assertEquals(4.0, result.cssLayout?.feedbackContainerGap)
    }

    @Test
    fun `apply maps citations-text-font-weight`() {
        val result = CSSKeyMapper.apply("--citations-text-font-weight", "700", emptyTheme)
        assertEquals(700, result.cssLayout?.citationsTextFontWeight)
    }

    @Test
    fun `apply maps citations-desktop-button-font-size`() {
        val result = CSSKeyMapper.apply("--citations-desktop-button-font-size", "12px", emptyTheme)
        assertEquals(12.0, result.cssLayout?.citationsDesktopButtonFontSize)
    }

    @Test
    fun `apply maps disclaimer-font-size`() {
        val result = CSSKeyMapper.apply("--disclaimer-font-size", "12px", emptyTheme)
        assertEquals(12.0, result.cssLayout?.disclaimerFontSize)
    }

    @Test
    fun `apply maps disclaimer-font-weight`() {
        val result = CSSKeyMapper.apply("--disclaimer-font-weight", "400", emptyTheme)
        assertEquals(400, result.cssLayout?.disclaimerFontWeight)
    }

    // -----------------------------------------------------------------------
    // Layout - Welcome order
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps welcome-input-order`() {
        val result = CSSKeyMapper.apply("--welcome-input-order", "3", emptyTheme)
        assertEquals(3, result.cssLayout?.welcomeInputOrder)
    }

    @Test
    fun `apply maps welcome-cards-order`() {
        val result = CSSKeyMapper.apply("--welcome-cards-order", "2", emptyTheme)
        assertEquals(2, result.cssLayout?.welcomeCardsOrder)
    }

    // -----------------------------------------------------------------------
    // Product card — font sizes and weights
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps product-card-title-font-weight`() {
        val result = CSSKeyMapper.apply("--product-card-title-font-weight", "700", emptyTheme)
        assertEquals(700, result.cssLayout?.productCardTitleFontWeight)
    }

    @Test
    fun `apply maps product-card-title-font-size`() {
        val result = CSSKeyMapper.apply("--product-card-title-font-size", "14px", emptyTheme)
        assertEquals(14.0, result.cssLayout?.productCardTitleFontSize)
    }

    @Test
    fun `apply maps product-card-subtitle-font-weight`() {
        val result = CSSKeyMapper.apply("--product-card-subtitle-font-weight", "400", emptyTheme)
        assertEquals(400, result.cssLayout?.productCardSubtitleFontWeight)
    }

    @Test
    fun `apply maps product-card-subtitle-font-size`() {
        val result = CSSKeyMapper.apply("--product-card-subtitle-font-size", "12px", emptyTheme)
        assertEquals(12.0, result.cssLayout?.productCardSubtitleFontSize)
    }

    @Test
    fun `apply maps product-card-price-font-weight`() {
        val result = CSSKeyMapper.apply("--product-card-price-font-weight", "400", emptyTheme)
        assertEquals(400, result.cssLayout?.productCardPriceFontWeight)
    }

    @Test
    fun `apply maps product-card-price-font-size`() {
        val result = CSSKeyMapper.apply("--product-card-price-font-size", "14px", emptyTheme)
        assertEquals(14.0, result.cssLayout?.productCardPriceFontSize)
    }

    @Test
    fun `apply maps product-card-badge-font-size`() {
        val result = CSSKeyMapper.apply("--product-card-badge-font-size", "12px", emptyTheme)
        assertEquals(12.0, result.cssLayout?.productCardBadgeFontSize)
    }

    @Test
    fun `apply maps product-card-badge-font-weight`() {
        val result = CSSKeyMapper.apply("--product-card-badge-font-weight", "700", emptyTheme)
        assertEquals(700, result.cssLayout?.productCardBadgeFontWeight)
    }

    // -----------------------------------------------------------------------
    // Product card — colors stored as raw strings in ConciergeLayout
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps product-card-badge-text-color`() {
        val result = CSSKeyMapper.apply("--product-card-badge-text-color", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.cssLayout?.productCardBadgeTextColor)
    }

    @Test
    fun `apply maps product-card-badge-background-color`() {
        val result = CSSKeyMapper.apply("--product-card-badge-background-color", "#EB1000", emptyTheme)
        assertEquals("#EB1000", result.cssLayout?.productCardBadgeBackgroundColor)
    }

    @Test
    fun `apply maps product-card-background-color`() {
        val result = CSSKeyMapper.apply("--product-card-background-color", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.cssLayout?.productCardBackgroundColor)
    }

    @Test
    fun `apply maps product-card-title-color`() {
        val result = CSSKeyMapper.apply("--product-card-title-color", "#191F1C", emptyTheme)
        assertEquals("#191F1C", result.cssLayout?.productCardTitleColor)
    }

    @Test
    fun `apply maps product-card-subtitle-color`() {
        val result = CSSKeyMapper.apply("--product-card-subtitle-color", "#4F4F4F", emptyTheme)
        assertEquals("#4F4F4F", result.cssLayout?.productCardSubtitleColor)
    }

    @Test
    fun `apply maps product-card-price-color`() {
        val result = CSSKeyMapper.apply("--product-card-price-color", "#191F1C", emptyTheme)
        assertEquals("#191F1C", result.cssLayout?.productCardPriceColor)
    }

    @Test
    fun `apply maps product-card-outline-color`() {
        val result = CSSKeyMapper.apply("--product-card-outline-color", "#E3E3E3", emptyTheme)
        assertEquals("#E3E3E3", result.cssLayout?.productCardOutlineColor)
    }

    // -----------------------------------------------------------------------
    // Product card — dimensions
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps product-card-width`() {
        val result = CSSKeyMapper.apply("--product-card-width", "222px", emptyTheme)
        assertEquals(222.0, result.cssLayout?.productCardWidth)
    }

    @Test
    fun `apply maps product-card-min-height`() {
        val result = CSSKeyMapper.apply("--product-card-min-height", "240px", emptyTheme)
        assertEquals(240.0, result.cssLayout?.productCardMinHeight)
    }

    @Test
    fun `apply maps product-card-max-height`() {
        val result = CSSKeyMapper.apply("--product-card-max-height", "360px", emptyTheme)
        assertEquals(360.0, result.cssLayout?.productCardMaxHeight)
    }

    @Test
    fun `apply maps product-image-width`() {
        val result = CSSKeyMapper.apply("--product-image-width", "190px", emptyTheme)
        assertEquals(190.0, result.cssLayout?.productImageWidth)
    }

    @Test
    fun `apply maps product-image-height`() {
        val result = CSSKeyMapper.apply("--product-image-height", "190px", emptyTheme)
        assertEquals(190.0, result.cssLayout?.productImageHeight)
    }

    @Test
    fun `apply maps product-image-scale`() {
        val result = CSSKeyMapper.apply("--product-image-scale", "fit", emptyTheme)
        assertEquals("fit", result.cssLayout?.productImageScale)
    }

    // -----------------------------------------------------------------------
    // Product card — was-price
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps product-card-was-price-color`() {
        val result = CSSKeyMapper.apply("--product-card-was-price-color", "#6E6E6E", emptyTheme)
        assertEquals("#6E6E6E", result.cssLayout?.productCardWasPriceColor)
    }

    @Test
    fun `apply maps product-card-was-price-font-size`() {
        val result = CSSKeyMapper.apply("--product-card-was-price-font-size", "12px", emptyTheme)
        assertEquals(12.0, result.cssLayout?.productCardWasPriceFontSize)
    }

    @Test
    fun `apply maps product-card-was-price-font-weight`() {
        val result = CSSKeyMapper.apply("--product-card-was-price-font-weight", "400", emptyTheme)
        assertEquals(400, result.cssLayout?.productCardWasPriceFontWeight)
    }

    @Test
    fun `apply maps product-card-was-price-text-prefix unquoted`() {
        val result = CSSKeyMapper.apply("--product-card-was-price-text-prefix", "was ", emptyTheme)
        assertEquals("was ", result.cssLayout?.productCardWasPriceTextPrefix)
    }

    @Test
    fun `apply maps product-card-was-price-text-prefix strips double quotes`() {
        val result = CSSKeyMapper.apply("--product-card-was-price-text-prefix", "\"was \"", emptyTheme)
        assertEquals("was ", result.cssLayout?.productCardWasPriceTextPrefix)
    }

    @Test
    fun `apply maps product-card-was-price-text-prefix strips single quotes`() {
        val result = CSSKeyMapper.apply("--product-card-was-price-text-prefix", "'was '", emptyTheme)
        assertEquals("was ", result.cssLayout?.productCardWasPriceTextPrefix)
    }

    // -----------------------------------------------------------------------
    // Product card — granular text spacing
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps product-card-title-subtitle-spacing`() {
        val result = CSSKeyMapper.apply("--product-card-title-subtitle-spacing", "6px", emptyTheme)
        assertEquals(6.0, result.cssLayout?.productCardTitleSubtitleSpacing)
    }

    @Test
    fun `apply maps product-card-section-spacing`() {
        val result = CSSKeyMapper.apply("--product-card-section-spacing", "12px", emptyTheme)
        assertEquals(12.0, result.cssLayout?.productCardSectionSpacing)
    }

    @Test
    fun `apply maps product-card-price-spacing`() {
        val result = CSSKeyMapper.apply("--product-card-price-spacing", "4px", emptyTheme)
        assertEquals(4.0, result.cssLayout?.productCardPriceSpacing)
    }

    // -----------------------------------------------------------------------
    // Components - Feedback button size
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps feedback-icon-btn-size-desktop`() {
        val result = CSSKeyMapper.apply("--feedback-icon-btn-size-desktop", "32px", emptyTheme)
        assertEquals(32.0, result.components?.feedback?.iconButtonSizeDesktop)
    }

    // -----------------------------------------------------------------------
    // Layout - CTA button
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps cta-button-border-radius`() {
        val result = CSSKeyMapper.apply("--cta-button-border-radius", "99px", emptyTheme)
        assertEquals(99.0, result.cssLayout?.ctaButtonBorderRadius)
    }

    @Test
    fun `apply maps cta-button-horizontal-padding`() {
        val result = CSSKeyMapper.apply("--cta-button-horizontal-padding", "16px", emptyTheme)
        assertEquals(16.0, result.cssLayout?.ctaButtonHorizontalPadding)
    }

    @Test
    fun `apply maps cta-button-vertical-padding`() {
        val result = CSSKeyMapper.apply("--cta-button-vertical-padding", "12px", emptyTheme)
        assertEquals(12.0, result.cssLayout?.ctaButtonVerticalPadding)
    }

    @Test
    fun `apply maps cta-button-font-size`() {
        val result = CSSKeyMapper.apply("--cta-button-font-size", "14px", emptyTheme)
        assertEquals(14.0, result.cssLayout?.ctaButtonFontSize)
    }

    @Test
    fun `apply maps cta-button-font-weight`() {
        val result = CSSKeyMapper.apply("--cta-button-font-weight", "400", emptyTheme)
        assertEquals(400, result.cssLayout?.ctaButtonFontWeight)
    }

    @Test
    fun `apply maps cta-button-icon-size`() {
        val result = CSSKeyMapper.apply("--cta-button-icon-size", "16px", emptyTheme)
        assertEquals(16.0, result.cssLayout?.ctaButtonIconSize)
    }

    @Test
    fun `apply maps all cta-button layout properties independently`() {
        var theme = CSSKeyMapper.apply("--cta-button-border-radius", "24px", emptyTheme)
        theme = CSSKeyMapper.apply("--cta-button-horizontal-padding", "20px", theme)
        theme = CSSKeyMapper.apply("--cta-button-vertical-padding", "8px", theme)
        theme = CSSKeyMapper.apply("--cta-button-font-size", "16px", theme)
        theme = CSSKeyMapper.apply("--cta-button-font-weight", "700", theme)
        theme = CSSKeyMapper.apply("--cta-button-icon-size", "18px", theme)
        assertEquals(24.0, theme.cssLayout?.ctaButtonBorderRadius)
        assertEquals(20.0, theme.cssLayout?.ctaButtonHorizontalPadding)
        assertEquals(8.0, theme.cssLayout?.ctaButtonVerticalPadding)
        assertEquals(16.0, theme.cssLayout?.ctaButtonFontSize)
        assertEquals(700, theme.cssLayout?.ctaButtonFontWeight)
        assertEquals(18.0, theme.cssLayout?.ctaButtonIconSize)
    }

    @Test
    fun `apply preserves existing layout when adding cta-button layout`() {
        val withInput = CSSKeyMapper.apply("--input-height-mobile", "52px", emptyTheme)
        val withBoth = CSSKeyMapper.apply("--cta-button-border-radius", "99px", withInput)
        assertEquals(52.0, withBoth.cssLayout?.inputHeight)
        assertEquals(99.0, withBoth.cssLayout?.ctaButtonBorderRadius)
    }

    @Test
    fun `supportedCSSKeys contains cta button layout keys`() {
        val keys = CSSKeyMapper.supportedCSSKeys
        assertTrue(keys.contains("cta-button-border-radius"))
        assertTrue(keys.contains("cta-button-horizontal-padding"))
        assertTrue(keys.contains("cta-button-vertical-padding"))
        assertTrue(keys.contains("cta-button-font-size"))
        assertTrue(keys.contains("cta-button-font-weight"))
        assertTrue(keys.contains("cta-button-icon-size"))
    }

    // -----------------------------------------------------------------------
    // Colors - CTA button
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps cta-button-background-color`() {
        val result = CSSKeyMapper.apply("--cta-button-background-color", "#EDEDED", emptyTheme)
        assertNotNull(result.colors?.ctaButton?.backgroundColor)
        assertEquals("#EDEDED", result.colors?.ctaButton?.backgroundColor)
    }

    @Test
    fun `apply maps cta-button-text-color`() {
        val result = CSSKeyMapper.apply("--cta-button-text-color", "#191F1C", emptyTheme)
        assertNotNull(result.colors?.ctaButton?.textColor)
        assertEquals("#191F1C", result.colors?.ctaButton?.textColor)
    }

    @Test
    fun `apply maps cta-button-icon-color`() {
        val result = CSSKeyMapper.apply("--cta-button-icon-color", "#161313", emptyTheme)
        assertNotNull(result.colors?.ctaButton?.iconColor)
        assertEquals("#161313", result.colors?.ctaButton?.iconColor)
    }

    @Test
    fun `apply maps all three cta-button colors independently`() {
        var theme = CSSKeyMapper.apply("--cta-button-background-color", "#FFFFFF", emptyTheme)
        theme = CSSKeyMapper.apply("--cta-button-text-color", "#000000", theme)
        theme = CSSKeyMapper.apply("--cta-button-icon-color", "#FF0000", theme)
        assertEquals("#FFFFFF", theme.colors?.ctaButton?.backgroundColor)
        assertEquals("#000000", theme.colors?.ctaButton?.textColor)
        assertEquals("#FF0000", theme.colors?.ctaButton?.iconColor)
    }

    @Test
    fun `apply preserves other cta-button colors when setting one`() {
        val withBackground = CSSKeyMapper.apply("--cta-button-background-color", "#EDEDED", emptyTheme)
        val withBoth = CSSKeyMapper.apply("--cta-button-text-color", "#191F1C", withBackground)
        assertEquals("#EDEDED", withBoth.colors?.ctaButton?.backgroundColor)
        assertEquals("#191F1C", withBoth.colors?.ctaButton?.textColor)
        assertNull(withBoth.colors?.ctaButton?.iconColor)
    }

    @Test
    fun `supportedCSSKeys contains cta button keys`() {
        val keys = CSSKeyMapper.supportedCSSKeys
        assertTrue(keys.contains("cta-button-background-color"))
        assertTrue(keys.contains("cta-button-text-color"))
        assertTrue(keys.contains("cta-button-icon-color"))
    }

    // -----------------------------------------------------------------------
    // Existing theme fields are preserved on incremental apply
    // -----------------------------------------------------------------------

    @Test
    fun `apply preserves existing theme fields when updating a different key`() {
        val withPrimary = CSSKeyMapper.apply("--color-primary", "#EB1000", emptyTheme)
        val withBoth = CSSKeyMapper.apply("--color-text", "#2C2C2C", withPrimary)
        assertEquals("#EB1000", withBoth.colors?.primaryColors?.primary)
        assertEquals("#2C2C2C", withBoth.colors?.primaryColors?.text)
    }

    @Test
    fun `apply preserves existing layout fields when adding a new layout key`() {
        val withHeight = CSSKeyMapper.apply("--input-height-mobile", "52px", emptyTheme)
        val withBoth = CSSKeyMapper.apply("--message-border-radius", "10px", withHeight)
        assertEquals(52.0, withBoth.cssLayout?.inputHeight)
        assertEquals(10.0, withBoth.cssLayout?.messageBorderRadius)
    }

    // -----------------------------------------------------------------------
    // Welcome Screen Layout
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps header-title-font-size`() {
        val result = CSSKeyMapper.apply("--header-title-font-size", "18px", emptyTheme)
        assertEquals(18.0, result.cssLayout?.headerTitleFontSize)
    }

    @Test
    fun `apply maps welcome-title-font-size`() {
        val result = CSSKeyMapper.apply("--welcome-title-font-size", "16px", emptyTheme)
        assertEquals(16.0, result.cssLayout?.welcomeTitleFontSize)
    }

    @Test
    fun `apply maps welcome-text-align`() {
        val result = CSSKeyMapper.apply("--welcome-text-align", "left", emptyTheme)
        assertEquals("left", result.cssLayout?.welcomeTextAlign)
    }

    @Test
    fun `apply maps welcome-text-align center`() {
        val result = CSSKeyMapper.apply("--welcome-text-align", "center", emptyTheme)
        assertEquals("center", result.cssLayout?.welcomeTextAlign)
    }

    @Test
    fun `apply maps welcome-content-padding`() {
        val result = CSSKeyMapper.apply("--welcome-content-padding", "16px", emptyTheme)
        assertEquals(16.0, result.cssLayout?.welcomeContentPadding)
    }

    @Test
    fun `apply maps welcome-prompt-image-size`() {
        val result = CSSKeyMapper.apply("--welcome-prompt-image-size", "48px", emptyTheme)
        assertEquals(48.0, result.cssLayout?.welcomePromptImageSize)
    }

    @Test
    fun `apply maps welcome-prompt-spacing`() {
        val result = CSSKeyMapper.apply("--welcome-prompt-spacing", "6px", emptyTheme)
        assertEquals(6.0, result.cssLayout?.welcomePromptSpacing)
    }

    @Test
    fun `apply maps welcome-title-bottom-spacing`() {
        val result = CSSKeyMapper.apply("--welcome-title-bottom-spacing", "6px", emptyTheme)
        assertEquals(6.0, result.cssLayout?.welcomeTitleBottomSpacing)
    }

    @Test
    fun `apply maps welcome-prompts-top-spacing`() {
        val result = CSSKeyMapper.apply("--welcome-prompts-top-spacing", "12px", emptyTheme)
        assertEquals(12.0, result.cssLayout?.welcomePromptsTopSpacing)
    }

    @Test
    fun `apply maps welcome-prompt-padding`() {
        val result = CSSKeyMapper.apply("--welcome-prompt-padding", "12px", emptyTheme)
        assertEquals(12.0, result.cssLayout?.welcomePromptPadding)
    }

    @Test
    fun `apply maps welcome-prompt-corner-radius`() {
        val result = CSSKeyMapper.apply("--welcome-prompt-corner-radius", "20px", emptyTheme)
        assertEquals(20.0, result.cssLayout?.welcomePromptCornerRadius)
    }

    @Test
    fun `apply maps suggestion-item-border-radius`() {
        val result = CSSKeyMapper.apply("--suggestion-item-border-radius", "24px", emptyTheme)
        assertEquals(24.0, result.cssLayout?.suggestionItemBorderRadius)
    }

    // -----------------------------------------------------------------------
    // Input Icon Colors
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps input-send-icon-color`() {
        val result = CSSKeyMapper.apply("--input-send-icon-color", "#FFFFFF", emptyTheme)
        assertNotNull(result.colors?.input?.sendIconColor)
    }

    @Test
    fun `apply maps input-mic-icon-color`() {
        val result = CSSKeyMapper.apply("--input-mic-icon-color", "#FF0000", emptyTheme)
        assertNotNull(result.colors?.input?.micIconColor)
    }

    @Test
    fun `apply maps input-send-arrow-icon-color`() {
        val result = CSSKeyMapper.apply("--input-send-arrow-icon-color", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.input?.sendArrowIconColor)
    }

    @Test
    fun `apply maps input-send-arrow-background-color solid`() {
        val result = CSSKeyMapper.apply("--input-send-arrow-background-color", "#1976D2", emptyTheme)
        assertEquals("#1976D2", result.colors?.input?.sendArrowBackgroundColor)
    }

    @Test
    fun `apply maps input-mic-recording-icon-color`() {
        val result = CSSKeyMapper.apply("--input-mic-recording-icon-color", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.input?.micRecordingIconColor)
    }

    @Test
    fun `apply builds up all solid input colors from a realistic sequence of keys without clobbering earlier ones`() {
        // Mirrors how ThemeParser.parseCSSThemeBlock actually applies a theme JSON: one CSS
        // variable at a time, threading the same theme forward. Verifies the whole input color set
        // survives being built up incrementally, not just an isolated pair.
        val cssBlock = listOf(
            "input-background" to "#FFFFFF",
            "input-text-color" to "#000000",
            "input-focus-outline-color" to "#4B75FF",
            "input-send-icon-color" to "#111111",
            "input-send-arrow-icon-color" to "#222222",
            "input-send-arrow-background-color" to "#333333",
            "input-mic-icon-color" to "#444444",
            "input-mic-recording-icon-color" to "#555555"
        )
        val theme = cssBlock.fold(emptyTheme) { acc, (key, value) -> CSSKeyMapper.apply("--$key", value, acc) }

        val input = theme.colors?.input
        assertEquals("#FFFFFF", input?.background)
        assertEquals("#000000", input?.text)
        assertEquals("#4B75FF", input?.outlineFocus)
        assertEquals("#111111", input?.sendIconColor)
        assertEquals("#222222", input?.sendArrowIconColor)
        assertEquals("#333333", input?.sendArrowBackgroundColor)
        assertEquals("#444444", input?.micIconColor)
        assertEquals("#555555", input?.micRecordingIconColor)
    }

    @Test
    fun `apply maps input-mic-waveform-gradient start end and angle keys to one gradient`() {
        var theme = CSSKeyMapper.apply("--input-mic-waveform-gradient-start-color", "#00F5D4", emptyTheme)
        theme = CSSKeyMapper.apply("--input-mic-waveform-gradient-end-color", "#003D33", theme)
        theme = CSSKeyMapper.apply("--input-mic-waveform-gradient-angle", "90deg", theme)
        assertEquals("#00F5D4", theme.colors?.input?.micWaveformGradient?.startColor)
        assertEquals("#003D33", theme.colors?.input?.micWaveformGradient?.endColor)
        assertEquals(90.0, theme.colors?.input?.micWaveformGradient?.angle)
    }

    @Test
    fun `apply defaults input-mic-waveform-gradient angle to null when omitted`() {
        var theme = CSSKeyMapper.apply("--input-mic-waveform-gradient-start-color", "#00F5D4", emptyTheme)
        theme = CSSKeyMapper.apply("--input-mic-waveform-gradient-end-color", "#003D33", theme)
        assertNull(theme.colors?.input?.micWaveformGradient?.angle)
    }

    @Test
    fun `apply preserves other input colors when setting input-mic-waveform-gradient-start-color`() {
        val withIcon = CSSKeyMapper.apply("--input-mic-icon-color", "#FF0000", emptyTheme)
        val withBoth = CSSKeyMapper.apply("--input-mic-waveform-gradient-start-color", "#00F5D4", withIcon)
        assertEquals("#FF0000", withBoth.colors?.input?.micIconColor)
        assertNotNull(withBoth.colors?.input?.micWaveformGradient)
    }

    @Test
    fun `apply preserves other input colors when setting input-mic-waveform-gradient-end-color`() {
        val withIcon = CSSKeyMapper.apply("--input-mic-icon-color", "#FF0000", emptyTheme)
        val withBoth = CSSKeyMapper.apply("--input-mic-waveform-gradient-end-color", "#003D33", withIcon)
        assertEquals("#FF0000", withBoth.colors?.input?.micIconColor)
        assertNotNull(withBoth.colors?.input?.micWaveformGradient)
    }

    @Test
    fun `apply maps input-outline-gradient start end and angle keys to one gradient`() {
        var theme = CSSKeyMapper.apply("--input-outline-gradient-start-color", "#12B0A0", emptyTheme)
        theme = CSSKeyMapper.apply("--input-outline-gradient-end-color", "#6DD3C4", theme)
        theme = CSSKeyMapper.apply("--input-outline-gradient-angle", "to right", theme)
        assertEquals("#12B0A0", theme.colors?.input?.outlineGradient?.startColor)
        assertEquals("#6DD3C4", theme.colors?.input?.outlineGradient?.endColor)
        assertEquals(90.0, theme.colors?.input?.outlineGradient?.angle)
    }

    @Test
    fun `apply maps input-mic-icon-gradient start end and angle keys to one gradient`() {
        var theme = CSSKeyMapper.apply("--input-mic-icon-gradient-start-color", "#12B0A0", emptyTheme)
        theme = CSSKeyMapper.apply("--input-mic-icon-gradient-end-color", "#6DD3C4", theme)
        theme = CSSKeyMapper.apply("--input-mic-icon-gradient-angle", "45deg", theme)
        assertEquals("#12B0A0", theme.colors?.input?.micIconGradient?.startColor)
        assertEquals("#6DD3C4", theme.colors?.input?.micIconGradient?.endColor)
        assertEquals(45.0, theme.colors?.input?.micIconGradient?.angle)
    }

    @Test
    fun `apply maps input-send-arrow-background-gradient start end and angle keys to one gradient`() {
        var theme = CSSKeyMapper.apply("--input-send-arrow-background-gradient-start-color", "#12B0A0", emptyTheme)
        theme = CSSKeyMapper.apply("--input-send-arrow-background-gradient-end-color", "#6DD3C4", theme)
        theme = CSSKeyMapper.apply("--input-send-arrow-background-gradient-angle", "to left", theme)
        assertEquals("#12B0A0", theme.colors?.input?.sendArrowBackgroundGradient?.startColor)
        assertEquals("#6DD3C4", theme.colors?.input?.sendArrowBackgroundGradient?.endColor)
        assertEquals(270.0, theme.colors?.input?.sendArrowBackgroundGradient?.angle)
    }

    @Test
    fun `apply setting only input-outline-gradient-angle creates a gradient with no colors set yet`() {
        // Regression test: a theme that only sets the angle key (ex: JSON key ordering happens to
        // put it first) must still build a placeholder gradient rather than losing the angle.
        val theme = CSSKeyMapper.apply("--input-outline-gradient-angle", "45deg", emptyTheme)
        assertEquals(45.0, theme.colors?.input?.outlineGradient?.angle)
        assertNull(theme.colors?.input?.outlineGradient?.startColor)
        assertNull(theme.colors?.input?.outlineGradient?.endColor)
    }

    @Test
    fun `apply setting only input-mic-icon-gradient-angle creates a gradient with no colors set yet`() {
        val theme = CSSKeyMapper.apply("--input-mic-icon-gradient-angle", "45deg", emptyTheme)
        assertEquals(45.0, theme.colors?.input?.micIconGradient?.angle)
        assertNull(theme.colors?.input?.micIconGradient?.startColor)
        assertNull(theme.colors?.input?.micIconGradient?.endColor)
    }

    @Test
    fun `apply setting only input-send-arrow-background-gradient-angle creates a gradient with no colors set yet`() {
        val theme = CSSKeyMapper.apply("--input-send-arrow-background-gradient-angle", "45deg", emptyTheme)
        assertEquals(45.0, theme.colors?.input?.sendArrowBackgroundGradient?.angle)
        assertNull(theme.colors?.input?.sendArrowBackgroundGradient?.startColor)
        assertNull(theme.colors?.input?.sendArrowBackgroundGradient?.endColor)
    }

    @Test
    fun `apply setting only input-mic-waveform-gradient-angle creates a gradient with no colors set yet`() {
        val theme = CSSKeyMapper.apply("--input-mic-waveform-gradient-angle", "45deg", emptyTheme)
        assertEquals(45.0, theme.colors?.input?.micWaveformGradient?.angle)
        assertNull(theme.colors?.input?.micWaveformGradient?.startColor)
        assertNull(theme.colors?.input?.micWaveformGradient?.endColor)
    }

    @Test
    fun `supportedCSSKeys contains all 12 gradient keys`() {
        val keys = CSSKeyMapper.supportedCSSKeys
        val prefixes = listOf(
            "input-outline-gradient",
            "input-mic-icon-gradient",
            "input-send-arrow-background-gradient",
            "input-mic-waveform-gradient"
        )
        for (prefix in prefixes) {
            assertTrue("supportedCSSKeys should contain $prefix-start-color", keys.contains("$prefix-start-color"))
            assertTrue("supportedCSSKeys should contain $prefix-end-color", keys.contains("$prefix-end-color"))
            assertTrue("supportedCSSKeys should contain $prefix-angle", keys.contains("$prefix-angle"))
        }
    }

    // -----------------------------------------------------------------------
    // Welcome Prompt Colors
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps welcome-prompt-background-color`() {
        val result = CSSKeyMapper.apply("--welcome-prompt-background-color", "#F5F5F5", emptyTheme)
        assertNotNull(result.colors?.welcomePrompt?.backgroundColor)
    }

    @Test
    fun `apply maps welcome-prompt-text-color`() {
        val result = CSSKeyMapper.apply("--welcome-prompt-text-color", "#000000", emptyTheme)
        assertNotNull(result.colors?.welcomePrompt?.textColor)
    }

    @Test
    fun `apply maps suggestion-background-color`() {
        val result = CSSKeyMapper.apply("--suggestion-background-color", "#E8E8E8", emptyTheme)
        assertEquals("#E8E8E8", result.colors?.promptSuggestion?.backgroundColor)
    }

    @Test
    fun `apply maps suggestion-text-color`() {
        val result = CSSKeyMapper.apply("--suggestion-text-color", "#333333", emptyTheme)
        assertEquals("#333333", result.colors?.promptSuggestion?.textColor)
    }

    @Test
    fun `apply builds up both welcome-prompt colors from a realistic sequence of keys without clobbering earlier ones`() {
        val cssBlock = listOf("welcome-prompt-background-color" to "#F5F5F5", "welcome-prompt-text-color" to "#000000")
        val theme = cssBlock.fold(emptyTheme) { acc, (key, value) -> CSSKeyMapper.apply("--$key", value, acc) }

        assertEquals("#F5F5F5", theme.colors?.welcomePrompt?.backgroundColor)
        assertEquals("#000000", theme.colors?.welcomePrompt?.textColor)
    }

    @Test
    fun `apply builds up both suggestion colors from a realistic sequence of keys without clobbering earlier ones`() {
        val cssBlock = listOf("suggestion-background-color" to "#E8E8E8", "suggestion-text-color" to "#333333")
        val theme = cssBlock.fold(emptyTheme) { acc, (key, value) -> CSSKeyMapper.apply("--$key", value, acc) }

        assertEquals("#E8E8E8", theme.colors?.promptSuggestion?.backgroundColor)
        assertEquals("#333333", theme.colors?.promptSuggestion?.textColor)
    }

    // -----------------------------------------------------------------------
    // Colors - Thinking Animation
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps thinking-dot-color`() {
        val result = CSSKeyMapper.apply("--thinking-dot-color", "#FF0000", emptyTheme)
        assertNotNull(result.colors?.thinking?.dotColor)
        assertEquals("#FF0000", result.colors?.thinking?.dotColor)
    }

    @Test
    fun `apply preserves existing colors when setting thinking-dot-color`() {
        val withPrimary = CSSKeyMapper.apply("--color-primary", "#EB1000", emptyTheme)
        val withBoth = CSSKeyMapper.apply("--thinking-dot-color", "#888888", withPrimary)
        assertEquals("#EB1000", withBoth.colors?.primaryColors?.primary)
        assertEquals("#888888", withBoth.colors?.thinking?.dotColor)
    }

    @Test
    fun `supportedCSSKeys contains thinking-dot-color`() {
        assertTrue(CSSKeyMapper.supportedCSSKeys.contains("thinking-dot-color"))
    }

    // -----------------------------------------------------------------------
    // Layout - Thinking Animation
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps thinking-dot-size`() {
        val result = CSSKeyMapper.apply("--thinking-dot-size", "8px", emptyTheme)
        assertEquals(8.0, result.cssLayout?.thinkingDotSize)
    }

    @Test
    fun `apply maps thinking-dot-spacing`() {
        val result = CSSKeyMapper.apply("--thinking-dot-spacing", "6px", emptyTheme)
        assertEquals(6.0, result.cssLayout?.thinkingDotSpacing)
    }

    @Test
    fun `apply maps thinking-bubble-border-radius`() {
        val result = CSSKeyMapper.apply("--thinking-bubble-border-radius", "16px", emptyTheme)
        assertEquals(16.0, result.cssLayout?.thinkingBubbleBorderRadius)
    }

    @Test
    fun `apply maps thinking-bubble-padding-horizontal`() {
        val result = CSSKeyMapper.apply("--thinking-bubble-padding-horizontal", "14px", emptyTheme)
        assertEquals(14.0, result.cssLayout?.thinkingBubblePaddingHorizontal)
    }

    @Test
    fun `apply maps thinking-bubble-padding-vertical`() {
        val result = CSSKeyMapper.apply("--thinking-bubble-padding-vertical", "10px", emptyTheme)
        assertEquals(10.0, result.cssLayout?.thinkingBubblePaddingVertical)
    }

    @Test
    fun `apply maps thinking-dot-vertical-alignment`() {
        val result = CSSKeyMapper.apply("--thinking-dot-vertical-alignment", "center", emptyTheme)
        assertEquals("center", result.cssLayout?.thinkingDotVerticalAlignment)
    }

    @Test
    fun `apply maps thinking-dot-vertical-alignment top`() {
        val result = CSSKeyMapper.apply("--thinking-dot-vertical-alignment", "top", emptyTheme)
        assertEquals("top", result.cssLayout?.thinkingDotVerticalAlignment)
    }

    @Test
    fun `apply maps thinking-dot-vertical-alignment bottom`() {
        val result = CSSKeyMapper.apply("--thinking-dot-vertical-alignment", "bottom", emptyTheme)
        assertEquals("bottom", result.cssLayout?.thinkingDotVerticalAlignment)
    }

    @Test
    fun `apply maps all thinking animation layout properties independently`() {
        var theme = CSSKeyMapper.apply("--thinking-dot-size", "8px", emptyTheme)
        theme = CSSKeyMapper.apply("--thinking-dot-spacing", "6px", theme)
        theme = CSSKeyMapper.apply("--thinking-bubble-border-radius", "16px", theme)
        theme = CSSKeyMapper.apply("--thinking-bubble-padding-horizontal", "14px", theme)
        theme = CSSKeyMapper.apply("--thinking-bubble-padding-vertical", "10px", theme)
        theme = CSSKeyMapper.apply("--thinking-dot-vertical-alignment", "center", theme)
        assertEquals(8.0, theme.cssLayout?.thinkingDotSize)
        assertEquals(6.0, theme.cssLayout?.thinkingDotSpacing)
        assertEquals(16.0, theme.cssLayout?.thinkingBubbleBorderRadius)
        assertEquals(14.0, theme.cssLayout?.thinkingBubblePaddingHorizontal)
        assertEquals(10.0, theme.cssLayout?.thinkingBubblePaddingVertical)
        assertEquals("center", theme.cssLayout?.thinkingDotVerticalAlignment)
    }

    @Test
    fun `apply preserves existing layout when adding thinking animation layout`() {
        val withInput = CSSKeyMapper.apply("--input-height-mobile", "52px", emptyTheme)
        val withBoth = CSSKeyMapper.apply("--thinking-dot-size", "8px", withInput)
        assertEquals(52.0, withBoth.cssLayout?.inputHeight)
        assertEquals(8.0, withBoth.cssLayout?.thinkingDotSize)
    }

    @Test
    fun `supportedCSSKeys contains thinking animation layout keys`() {
        val keys = CSSKeyMapper.supportedCSSKeys
        assertTrue(keys.contains("thinking-dot-size"))
        assertTrue(keys.contains("thinking-dot-spacing"))
        assertTrue(keys.contains("thinking-bubble-border-radius"))
        assertTrue(keys.contains("thinking-bubble-padding-horizontal"))
        assertTrue(keys.contains("thinking-bubble-padding-vertical"))
        assertTrue(keys.contains("thinking-dot-vertical-alignment"))
    }

    // -----------------------------------------------------------------------
    // Feedback dialog colors (12 new CSS vars)
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps feedback-sheet-background-color`() {
        val result = CSSKeyMapper.apply("--feedback-sheet-background-color", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.feedback?.sheetBackground)
    }

    @Test
    fun `apply maps feedback-title-text-color`() {
        val result = CSSKeyMapper.apply("--feedback-title-text-color", "#131313", emptyTheme)
        assertEquals("#131313", result.colors?.feedback?.titleText)
    }

    @Test
    fun `apply maps feedback-question-text-color`() {
        val result = CSSKeyMapper.apply("--feedback-question-text-color", "#424242", emptyTheme)
        assertEquals("#424242", result.colors?.feedback?.questionText)
    }

    @Test
    fun `apply maps feedback-options-text-color`() {
        val result = CSSKeyMapper.apply("--feedback-options-text-color", "#131313", emptyTheme)
        assertEquals("#131313", result.colors?.feedback?.optionsText)
    }

    @Test
    fun `apply maps feedback-checkbox-border-color`() {
        val result = CSSKeyMapper.apply("--feedback-checkbox-border-color", "#131313", emptyTheme)
        assertEquals("#131313", result.colors?.feedback?.checkboxBorder)
    }

    @Test
    fun `apply maps feedback-drag-handle-color`() {
        val result = CSSKeyMapper.apply("--feedback-drag-handle-color", "#CCCCCC", emptyTheme)
        assertEquals("#CCCCCC", result.colors?.feedback?.dragHandle)
    }

    @Test
    fun `apply maps feedback-submit-button-fill-color`() {
        val result = CSSKeyMapper.apply("--feedback-submit-button-fill-color", "#3B63FB", emptyTheme)
        assertEquals("#3B63FB", result.colors?.feedback?.submitButtonFill)
    }

    @Test
    fun `apply maps feedback-submit-button-text-color`() {
        val result = CSSKeyMapper.apply("--feedback-submit-button-text-color", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.feedback?.submitButtonText)
    }

    @Test
    fun `apply maps feedback-cancel-button-fill-color`() {
        val result = CSSKeyMapper.apply("--feedback-cancel-button-fill-color", "#FFFFFF", emptyTheme)
        assertEquals("#FFFFFF", result.colors?.feedback?.cancelButtonFill)
    }

    @Test
    fun `apply maps feedback-cancel-button-text-color`() {
        val result = CSSKeyMapper.apply("--feedback-cancel-button-text-color", "#2C2C2C", emptyTheme)
        assertEquals("#2C2C2C", result.colors?.feedback?.cancelButtonText)
    }

    @Test
    fun `apply maps feedback-cancel-button-border-color`() {
        val result = CSSKeyMapper.apply("--feedback-cancel-button-border-color", "#2C2C2C", emptyTheme)
        assertEquals("#2C2C2C", result.colors?.feedback?.cancelButtonBorder)
    }

    @Test
    fun `apply builds up all feedback colors from a realistic sequence of keys without clobbering earlier ones`() {
        val cssBlock = listOf(
            "feedback-icon-btn-background" to "#FFFFFF",
            "feedback-icon-btn-hover-background" to "#F0F0F0",
            "feedback-sheet-background-color" to "#FFFFFF",
            "feedback-title-text-color" to "#131313",
            "feedback-question-text-color" to "#424242",
            "feedback-options-text-color" to "#131313",
            "feedback-checkbox-border-color" to "#131313",
            "feedback-drag-handle-color" to "#CCCCCC",
            "feedback-submit-button-fill-color" to "#3B63FB",
            "feedback-submit-button-text-color" to "#FFFFFF",
            "feedback-cancel-button-fill-color" to "#FFFFFF",
            "feedback-cancel-button-text-color" to "#2C2C2C",
            "feedback-cancel-button-border-color" to "#2C2C2C"
        )
        val theme = cssBlock.fold(emptyTheme) { acc, (key, value) -> CSSKeyMapper.apply("--$key", value, acc) }

        val feedback = theme.colors?.feedback
        assertEquals("#FFFFFF", feedback?.iconButtonBackground)
        assertEquals("#F0F0F0", feedback?.iconButtonHoverBackground)
        assertEquals("#FFFFFF", feedback?.sheetBackground)
        assertEquals("#131313", feedback?.titleText)
        assertEquals("#424242", feedback?.questionText)
        assertEquals("#131313", feedback?.optionsText)
        assertEquals("#131313", feedback?.checkboxBorder)
        assertEquals("#CCCCCC", feedback?.dragHandle)
        assertEquals("#3B63FB", feedback?.submitButtonFill)
        assertEquals("#FFFFFF", feedback?.submitButtonText)
        assertEquals("#FFFFFF", feedback?.cancelButtonFill)
        assertEquals("#2C2C2C", feedback?.cancelButtonText)
        assertEquals("#2C2C2C", feedback?.cancelButtonBorder)
    }

    // -----------------------------------------------------------------------
    // Feedback dialog layout (8 new CSS vars)
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps feedback-submit-button-border-radius`() {
        val result = CSSKeyMapper.apply("--feedback-submit-button-border-radius", "10px", emptyTheme)
        assertEquals(10.0, result.cssLayout?.feedbackSubmitButtonBorderRadius)
    }

    @Test
    fun `apply maps feedback-cancel-button-border-radius`() {
        val result = CSSKeyMapper.apply("--feedback-cancel-button-border-radius", "10px", emptyTheme)
        assertEquals(10.0, result.cssLayout?.feedbackCancelButtonBorderRadius)
    }

    @Test
    fun `apply maps feedback-cancel-button-border-width`() {
        val result = CSSKeyMapper.apply("--feedback-cancel-button-border-width", "1px", emptyTheme)
        assertEquals(1.0, result.cssLayout?.feedbackCancelButtonBorderWidth)
    }

    @Test
    fun `apply maps feedback-submit-button-font-weight`() {
        val result = CSSKeyMapper.apply("--feedback-submit-button-font-weight", "600", emptyTheme)
        assertEquals(600, result.cssLayout?.feedbackSubmitButtonFontWeight)
    }

    @Test
    fun `apply maps feedback-cancel-button-font-weight`() {
        val result = CSSKeyMapper.apply("--feedback-cancel-button-font-weight", "600", emptyTheme)
        assertEquals(600, result.cssLayout?.feedbackCancelButtonFontWeight)
    }

    @Test
    fun `apply maps feedback-checkbox-border-radius`() {
        val result = CSSKeyMapper.apply("--feedback-checkbox-border-radius", "6px", emptyTheme)
        assertEquals(6.0, result.cssLayout?.feedbackCheckboxBorderRadius)
    }

    @Test
    fun `apply maps feedback-title-text-align leading to START`() {
        val result = CSSKeyMapper.apply("--feedback-title-text-align", "leading", emptyTheme)
        assertEquals(ConciergeTextAlignment.START, result.cssLayout?.feedbackTitleTextAlign)
    }

    @Test
    fun `apply maps feedback-title-text-align center`() {
        val result = CSSKeyMapper.apply("--feedback-title-text-align", "center", emptyTheme)
        assertEquals(ConciergeTextAlignment.CENTER, result.cssLayout?.feedbackTitleTextAlign)
    }

    @Test
    fun `apply maps feedback-title-text-align left to START`() {
        val result = CSSKeyMapper.apply("--feedback-title-text-align", "left", emptyTheme)
        assertEquals(ConciergeTextAlignment.START, result.cssLayout?.feedbackTitleTextAlign)
    }

    @Test
    fun `apply maps feedback-title-text-align right to END`() {
        val result = CSSKeyMapper.apply("--feedback-title-text-align", "right", emptyTheme)
        assertEquals(ConciergeTextAlignment.END, result.cssLayout?.feedbackTitleTextAlign)
    }

    @Test
    fun `apply maps feedback-title-text-align justify to CENTER`() {
        val result = CSSKeyMapper.apply("--feedback-title-text-align", "justify", emptyTheme)
        assertEquals(ConciergeTextAlignment.CENTER, result.cssLayout?.feedbackTitleTextAlign)
    }

    @Test
    fun `apply maps feedback-title-text-align unknown value to START`() {
        val result = CSSKeyMapper.apply("--feedback-title-text-align", "bogus", emptyTheme)
        assertEquals(ConciergeTextAlignment.START, result.cssLayout?.feedbackTitleTextAlign)
    }

    @Test
    fun `apply maps feedback-title-font-size`() {
        val result = CSSKeyMapper.apply("--feedback-title-font-size", "22px", emptyTheme)
        assertEquals(22.0, result.cssLayout?.feedbackTitleFontSize)
    }

    @Test
    fun `supportedCSSKeys contains all new feedback dialog keys`() {
        val keys = CSSKeyMapper.supportedCSSKeys
        val expected = listOf(
            "feedback-sheet-background-color",
            "feedback-title-text-color",
            "feedback-question-text-color",
            "feedback-options-text-color",
            "feedback-checkbox-border-color",
            "feedback-drag-handle-color",
            "feedback-submit-button-fill-color",
            "feedback-submit-button-text-color",
            "feedback-cancel-button-fill-color",
            "feedback-cancel-button-text-color",
            "feedback-cancel-button-border-color",
            "feedback-submit-button-border-radius",
            "feedback-cancel-button-border-radius",
            "feedback-cancel-button-border-width",
            "feedback-submit-button-font-weight",
            "feedback-cancel-button-font-weight",
            "feedback-checkbox-border-radius",
            "feedback-title-text-align",
            "feedback-title-font-size"
        )
        expected.forEach { key ->
            assertTrue("supportedCSSKeys should contain $key", keys.contains(key))
        }
    }
}
