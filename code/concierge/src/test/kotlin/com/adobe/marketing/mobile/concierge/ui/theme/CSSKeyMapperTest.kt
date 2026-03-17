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
    fun `apply maps product-card-height`() {
        val result = CSSKeyMapper.apply("--product-card-height", "359px", emptyTheme)
        assertEquals(359.0, result.cssLayout?.productCardHeight)
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
    // Components - Feedback button size
    // -----------------------------------------------------------------------

    @Test
    fun `apply maps feedback-icon-btn-size-desktop`() {
        val result = CSSKeyMapper.apply("--feedback-icon-btn-size-desktop", "32px", emptyTheme)
        assertEquals(32.0, result.components?.feedback?.iconButtonSizeDesktop)
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
}
