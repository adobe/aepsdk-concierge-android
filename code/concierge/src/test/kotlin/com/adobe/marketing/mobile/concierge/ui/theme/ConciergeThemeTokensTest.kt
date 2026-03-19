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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConciergeThemeTokensTest {

    // ========== ConciergeThemeTokens Tests ==========

    @Test
    fun `ConciergeThemeTokens creates with defaults`() {
        val tokens = ConciergeThemeTokens()
        
        assertNotNull(tokens.metadata)
        assertNotNull(tokens.behavior)
        assertNull(tokens.colors)
        assertNotNull(tokens.assets)
        assertNotNull(tokens.content)
        assertNotNull(tokens.layout)
        assertNull(tokens.cssLayout)
        assertNull(tokens.typography)
        assertNull(tokens.components)
        assertTrue(tokens.cssVariables.isEmpty())
    }

    @Test
    fun `ConciergeThemeTokens creates with custom values`() {
        val tokens = ConciergeThemeTokens(
            metadata = ConciergeThemeMetadata(name = "Custom Theme"),
            colors = ConciergeThemeColors(primary = "#FF0000"),
            cssVariables = mapOf("--color-primary" to "#FF0000")
        )
        
        assertEquals("Custom Theme", tokens.metadata.name)
        assertEquals("#FF0000", tokens.colors?.primary)
        assertEquals(1, tokens.cssVariables.size)
    }

    @Test
    fun `ConciergeThemeTokens supports copy`() {
        val original = ConciergeThemeTokens()
        val updated = original.copy(
            metadata = ConciergeThemeMetadata(name = "Updated")
        )
        
        assertEquals("Default Theme", original.metadata.name)
        assertEquals("Updated", updated.metadata.name)
    }

    // ========== ConciergeTypography Tests ==========

    @Test
    fun `ConciergeTypography creates with nulls by default`() {
        val typography = ConciergeTypography()
        
        assertNull(typography.fontFamily)
        assertNull(typography.lineHeight)
    }

    @Test
    fun `ConciergeTypography creates with custom values`() {
        val typography = ConciergeTypography(
            fontFamily = "Helvetica",
            lineHeight = 1.5
        )
        
        assertEquals("Helvetica", typography.fontFamily)
        assertEquals(1.5, typography.lineHeight)
    }

    @Test
    fun `ConciergeTypography supports copy`() {
        val original = ConciergeTypography(fontFamily = "Arial")
        val updated = original.copy(lineHeight = 2.0)
        
        assertEquals("Arial", updated.fontFamily)
        assertEquals(2.0, updated.lineHeight)
    }

    // ========== ConciergeLayout Tests ==========

    @Test
    fun `ConciergeLayout creates with all nulls by default`() {
        val layout = ConciergeLayout()
        
        assertNull(layout.inputHeight)
        assertNull(layout.inputBorderRadius)
        assertNull(layout.messageBorderRadius)
        assertNull(layout.chatInterfaceMaxWidth)
    }

    @Test
    fun `ConciergeLayout creates with input properties`() {
        val layout = ConciergeLayout(
            inputHeight = 52.0,
            inputBorderRadius = 12.0,
            inputFontSize = 16.0
        )
        
        assertEquals(52.0, layout.inputHeight)
        assertEquals(12.0, layout.inputBorderRadius)
        assertEquals(16.0, layout.inputFontSize)
    }

    @Test
    fun `ConciergeLayout creates with message properties`() {
        val layout = ConciergeLayout(
            messageBorderRadius = 10.0,
            messagePadding = listOf(10.0, 20.0, 10.0, 20.0),
            messageMaxWidth = 0.8
        )
        
        assertEquals(10.0, layout.messageBorderRadius)
        assertEquals(4, layout.messagePadding?.size)
        assertEquals(0.8, layout.messageMaxWidth)
    }

    @Test
    fun `ConciergeLayout creates with chat properties`() {
        val layout = ConciergeLayout(
            chatInterfaceMaxWidth = 768.0,
            chatHistoryPadding = 16.0,
            messageBlockerHeight = 105.0
        )
        
        assertEquals(768.0, layout.chatInterfaceMaxWidth)
        assertEquals(16.0, layout.chatHistoryPadding)
        assertEquals(105.0, layout.messageBlockerHeight)
    }

    @Test
    fun `ConciergeLayout creates with box shadow`() {
        val shadow = mapOf(
            "offsetX" to 0.0,
            "offsetY" to 2.0,
            "blurRadius" to 8.0,
            "color" to "#00000033"
        )
        val layout = ConciergeLayout(inputBoxShadow = shadow)
        
        assertNotNull(layout.inputBoxShadow)
        assertEquals(0.0, layout.inputBoxShadow?.get("offsetX"))
    }

    @Test
    fun `ConciergeLayout supports copy`() {
        val original = ConciergeLayout(inputHeight = 50.0)
        val updated = original.copy(inputBorderRadius = 8.0)
        
        assertEquals(50.0, updated.inputHeight)
        assertEquals(8.0, updated.inputBorderRadius)
    }

    // ========== ConciergeComponentsConfig Tests ==========

    @Test
    fun `ConciergeComponentsConfig creates with null feedback`() {
        val components = ConciergeComponentsConfig()
        
        assertNull(components.feedback)
    }

    @Test
    fun `ConciergeComponentsConfig creates with feedback component`() {
        val components = ConciergeComponentsConfig(
            feedback = ConciergeFeedbackComponent(iconButtonSizeDesktop = 32.0)
        )
        
        assertEquals(32.0, components.feedback?.iconButtonSizeDesktop)
    }

    @Test
    fun `ConciergeFeedbackComponent creates with custom size`() {
        val feedback = ConciergeFeedbackComponent(iconButtonSizeDesktop = 40.0)
        
        assertEquals(40.0, feedback.iconButtonSizeDesktop)
    }

    // ========== ConciergeThemeMetadata Tests ==========

    @Test
    fun `ConciergeThemeMetadata creates with defaults`() {
        val metadata = ConciergeThemeMetadata()
        
        assertEquals("1.0.0", metadata.version)
        assertEquals("Default Theme", metadata.name)
        assertNull(metadata.description)
        assertNull(metadata.author)
        assertNull(metadata.lastModified)
    }

    @Test
    fun `ConciergeThemeMetadata creates with all fields`() {
        val metadata = ConciergeThemeMetadata(
            version = "2.0.0",
            name = "Custom Theme",
            description = "A custom theme",
            author = "Author Name",
            lastModified = "2025-01-28"
        )
        
        assertEquals("2.0.0", metadata.version)
        assertEquals("Custom Theme", metadata.name)
        assertEquals("A custom theme", metadata.description)
        assertEquals("Author Name", metadata.author)
        assertEquals("2025-01-28", metadata.lastModified)
    }

    @Test
    fun `ConciergeThemeMetadata supports copy`() {
        val original = ConciergeThemeMetadata()
        val updated = original.copy(name = "New Theme")
        
        assertEquals("Default Theme", original.name)
        assertEquals("New Theme", updated.name)
    }

    // ========== ConciergeThemeBehavior Tests ==========

    @Test
    fun `ConciergeThemeBehavior creates with defaults`() {
        val behavior = ConciergeThemeBehavior()
        
        assertTrue(behavior.enableDarkMode)
        assertTrue(behavior.enableAnimations)
        assertTrue(behavior.enableHaptics)
        assertFalse(behavior.enableSoundEffects)
        assertTrue(behavior.autoScrollToBottom)
        assertFalse(behavior.showTimestamps)
        assertTrue(behavior.enableMarkdown)
        assertTrue(behavior.enableCitations)
        assertTrue(behavior.enableVoiceInput)
        assertEquals(2000, behavior.maxMessageLength)
        assertEquals(500, behavior.typingIndicatorDelay)
    }

    @Test
    fun `ConciergeThemeBehavior creates with custom flags`() {
        val behavior = ConciergeThemeBehavior(
            enableDarkMode = false,
            enableAnimations = false,
            enableHaptics = false,
            enableSoundEffects = true
        )
        
        assertFalse(behavior.enableDarkMode)
        assertFalse(behavior.enableAnimations)
        assertFalse(behavior.enableHaptics)
        assertTrue(behavior.enableSoundEffects)
    }

    @Test
    fun `ConciergeThemeBehavior supports copy`() {
        val original = ConciergeThemeBehavior()
        val updated = original.copy(
            maxMessageLength = 5000,
            typingIndicatorDelay = 1000
        )
        
        assertEquals(2000, original.maxMessageLength)
        assertEquals(5000, updated.maxMessageLength)
        assertEquals(1000, updated.typingIndicatorDelay)
    }

    // ========== Asset Data Classes Tests ==========

    @Test
    fun `ConciergeThemeAssets creates with defaults`() {
        val assets = ConciergeThemeAssets()
        
        assertNotNull(assets.icons)
        assertNotNull(assets.images)
        assertNotNull(assets.fonts)
    }

    @Test
    fun `ConciergeIconAssets creates with all nulls`() {
        val icons = ConciergeIconAssets()
        
        assertNull(icons.company)
        assertNull(icons.send)
        assertNull(icons.microphone)
        assertNull(icons.close)
        assertNull(icons.thumbsUp)
        assertNull(icons.thumbsDown)
        assertNull(icons.chevronDown)
        assertNull(icons.chevronRight)
    }

    @Test
    fun `ConciergeIconAssets creates with custom icons`() {
        val icons = ConciergeIconAssets(
            company = "company.svg",
            send = "send.svg",
            microphone = "mic.svg"
        )
        
        assertEquals("company.svg", icons.company)
        assertEquals("send.svg", icons.send)
        assertEquals("mic.svg", icons.microphone)
    }

    @Test
    fun `ConciergeImageAssets creates with custom images`() {
        val images = ConciergeImageAssets(
            welcomeBanner = "banner.png",
            errorPlaceholder = "error.png",
            avatarBot = "bot.png",
            avatarUser = "user.png"
        )
        
        assertEquals("banner.png", images.welcomeBanner)
        assertEquals("error.png", images.errorPlaceholder)
        assertEquals("bot.png", images.avatarBot)
        assertEquals("user.png", images.avatarUser)
    }

    @Test
    fun `ConciergeThemeFonts creates with custom fonts`() {
        val fonts = ConciergeThemeFonts(
            regular = "Regular.ttf",
            medium = "Medium.ttf",
            bold = "Bold.ttf",
            light = "Light.ttf"
        )
        
        assertEquals("Regular.ttf", fonts.regular)
        assertEquals("Medium.ttf", fonts.medium)
        assertEquals("Bold.ttf", fonts.bold)
        assertEquals("Light.ttf", fonts.light)
    }

    // ========== Content Data Classes Tests ==========

    @Test
    fun `ConciergeThemeContent creates with defaults`() {
        val content = ConciergeThemeContent()
        
        assertNotNull(content.text)
        assertNotNull(content.placeholders)
        assertNotNull(content.accessibility)
    }

    @Test
    fun `ConciergeTextContent creates with defaults`() {
        val text = ConciergeTextContent()
        
        assertEquals("How can I help you?", text.welcomeTitle)
        assertNull(text.welcomeSubtitle)
        assertNull(text.disclaimerText)
        assertEquals("Something went wrong", text.errorTitle)
        assertEquals("Try again", text.errorRetry)
        assertEquals("Provide feedback", text.feedbackTitle)
        assertEquals("Submit", text.feedbackSubmit)
        assertEquals("Cancel", text.feedbackCancel)
        assertEquals("Sources", text.sourcesLabel)
        assertEquals("Thinking", text.thinkingLabel)
        assertEquals("Listening", text.listeningLabel)
    }

    @Test
    fun `ConciergeTextContent creates with custom text`() {
        val text = ConciergeTextContent(
            welcomeTitle = "Welcome!",
            errorTitle = "Oops!",
            feedbackSubmit = "Send"
        )
        
        assertEquals("Welcome!", text.welcomeTitle)
        assertEquals("Oops!", text.errorTitle)
        assertEquals("Send", text.feedbackSubmit)
    }

    @Test
    fun `ConciergePlaceholderContent creates with defaults`() {
        val placeholders = ConciergePlaceholderContent()
        
        assertEquals("How can I help", placeholders.inputPlaceholder)
        assertEquals("Listening...", placeholders.listeningPlaceholder)
        assertNull(placeholders.emptyStateMessage)
    }

    @Test
    fun `ConciergeAccessibilityContent creates with defaults`() {
        val accessibility = ConciergeAccessibilityContent()
        
        assertEquals("Send message", accessibility.sendButtonLabel)
        assertEquals("Voice input", accessibility.micButtonLabel)
        assertEquals("Close", accessibility.closeButtonLabel)
        assertEquals("Like this response", accessibility.thumbsUpLabel)
        assertEquals("Dislike this response", accessibility.thumbsDownLabel)
    }

    // ========== Layout Data Classes Tests ==========

    @Test
    fun `ConciergeThemeLayout creates with defaults`() {
        val layout = ConciergeThemeLayout()
        
        assertNotNull(layout.spacing)
        assertNotNull(layout.sizing)
        assertNotNull(layout.positioning)
    }

    @Test
    fun `ConciergeSpacingLayout creates with defaults`() {
        val spacing = ConciergeSpacingLayout()
        
        assertEquals(4.0, spacing.xs, 0.001)
        assertEquals(8.0, spacing.sm, 0.001)
        assertEquals(16.0, spacing.md, 0.001)
        assertEquals(24.0, spacing.lg, 0.001)
        assertEquals(32.0, spacing.xl, 0.001)
        assertEquals(48.0, spacing.xxl, 0.001)
    }

    @Test
    fun `ConciergeSpacingLayout creates with custom values`() {
        val spacing = ConciergeSpacingLayout(
            xs = 2.0,
            sm = 4.0,
            md = 8.0
        )
        
        assertEquals(2.0, spacing.xs, 0.001)
        assertEquals(4.0, spacing.sm, 0.001)
        assertEquals(8.0, spacing.md, 0.001)
    }

    @Test
    fun `ConciergeSizingLayout creates with defaults`() {
        val sizing = ConciergeSizingLayout()
        
        assertEquals(16.0, sizing.iconSm, 0.001)
        assertEquals(24.0, sizing.iconMd, 0.001)
        assertEquals(32.0, sizing.iconLg, 0.001)
        assertEquals(32.0, sizing.avatarSm, 0.001)
        assertEquals(40.0, sizing.avatarMd, 0.001)
        assertEquals(48.0, sizing.avatarLg, 0.001)
        assertEquals(32.0, sizing.buttonHeightSm, 0.001)
        assertEquals(40.0, sizing.buttonHeightMd, 0.001)
        assertEquals(48.0, sizing.buttonHeightLg, 0.001)
    }

    @Test
    fun `ConciergePositioningLayout creates with defaults`() {
        val positioning = ConciergePositioningLayout()
        
        assertEquals(56.0, positioning.headerHeight, 0.001)
        assertEquals(72.0, positioning.footerHeight, 0.001)
        assertEquals(800.0, positioning.maxContentWidth, 0.001)
        assertEquals(280.0, positioning.minContentWidth, 0.001)
    }

    // ========== CSSVariableMapper Tests ==========

    @Test
    fun `CSSVariableMapper mapCSSVariable handles color mappings`() {
        assertEquals("primary", CSSVariableMapper.mapCSSVariable("--color-primary"))
        assertEquals("onPrimary", CSSVariableMapper.mapCSSVariable("--color-on-primary"))
        assertEquals("secondary", CSSVariableMapper.mapCSSVariable("--color-secondary"))
        assertEquals("surface", CSSVariableMapper.mapCSSVariable("--color-surface"))
        assertEquals("onSurface", CSSVariableMapper.mapCSSVariable("--color-on-surface"))
        assertEquals("background", CSSVariableMapper.mapCSSVariable("--color-background"))
        assertEquals("container", CSSVariableMapper.mapCSSVariable("--color-container"))
        assertEquals("outline", CSSVariableMapper.mapCSSVariable("--color-outline"))
        assertEquals("error", CSSVariableMapper.mapCSSVariable("--color-error"))
        assertEquals("onError", CSSVariableMapper.mapCSSVariable("--color-on-error"))
    }

    @Test
    fun `CSSVariableMapper mapCSSVariable handles spacing mappings`() {
        assertEquals("xs", CSSVariableMapper.mapCSSVariable("--spacing-xs"))
        assertEquals("sm", CSSVariableMapper.mapCSSVariable("--spacing-sm"))
        assertEquals("md", CSSVariableMapper.mapCSSVariable("--spacing-md"))
        assertEquals("lg", CSSVariableMapper.mapCSSVariable("--spacing-lg"))
        assertEquals("xl", CSSVariableMapper.mapCSSVariable("--spacing-xl"))
        assertEquals("xxl", CSSVariableMapper.mapCSSVariable("--spacing-xxl"))
    }

    @Test
    fun `CSSVariableMapper mapCSSVariable handles unknown variables`() {
        val result = CSSVariableMapper.mapCSSVariable("--custom-variable")
        assertEquals("custom_variable", result)
    }

    @Test
    fun `CSSVariableMapper mapCSSVariable removes prefix and replaces dashes`() {
        assertEquals("my_custom_var", CSSVariableMapper.mapCSSVariable("--my-custom-var"))
        assertEquals("font_size", CSSVariableMapper.mapCSSVariable("--font-size"))
    }

    @Test
    fun `CSSVariableMapper parseCSSValue removes var() function`() {
        assertEquals("--primary-color", CSSVariableMapper.parseCSSValue("var(--primary-color)"))
    }

    @Test
    fun `CSSVariableMapper parseCSSValue removes calc() function`() {
        assertEquals("100% - 20px", CSSVariableMapper.parseCSSValue("calc(100% - 20px)"))
    }

    @Test
    fun `CSSVariableMapper parseCSSValue handles plain values`() {
        assertEquals("#FF0000", CSSVariableMapper.parseCSSValue("#FF0000"))
        assertEquals("16px", CSSVariableMapper.parseCSSValue("16px"))
        assertEquals("bold", CSSVariableMapper.parseCSSValue("bold"))
    }

    @Test
    fun `CSSVariableMapper parseCSSValue trims whitespace`() {
        assertEquals("#FF0000", CSSVariableMapper.parseCSSValue("  #FF0000  "))
        assertEquals("--primary", CSSVariableMapper.parseCSSValue("var(  --primary  )"))
    }

    @Test
    fun `CSSVariableMapper parseCSSValue handles empty var()`() {
        assertEquals("", CSSVariableMapper.parseCSSValue("var()"))
    }

    @Test
    fun `CSSVariableMapper parseCSSValue handles nested functions`() {
        assertEquals("--size", CSSVariableMapper.parseCSSValue("var(--size)"))
    }

    @Test
    fun `CSSVariableMapper parseCSSValue handles complex calc expressions`() {
        val expression = "calc(100vh - 200px)"
        val result = CSSVariableMapper.parseCSSValue(expression)
        assertEquals("100vh - 200px", result)
    }

    // ========== Integration Tests ==========

    @Test
    fun `complete theme tokens can be constructed`() {
        val tokens = ConciergeThemeTokens(
            metadata = ConciergeThemeMetadata(name = "Complete Theme"),
            behavior = ConciergeThemeBehavior(enableDarkMode = true),
            colors = ConciergeThemeColors(primary = "#FF0000"),
            assets = ConciergeThemeAssets(),
            content = ConciergeThemeContent(),
            layout = ConciergeThemeLayout(),
            cssLayout = ConciergeLayout(inputHeight = 52.0),
            typography = ConciergeTypography(fontFamily = "Arial"),
            components = ConciergeComponentsConfig(),
            cssVariables = mapOf("--test" to "value")
        )
        
        assertEquals("Complete Theme", tokens.metadata.name)
        assertEquals("#FF0000", tokens.colors?.primary)
        assertEquals(52.0, tokens.cssLayout?.inputHeight)
        assertEquals("Arial", tokens.typography?.fontFamily)
        assertEquals(1, tokens.cssVariables.size)
    }

    // -----------------------------------------------------------------------
    // ConciergeLayout - CTA button fields
    // -----------------------------------------------------------------------

    @Test
    fun `ConciergeLayout cta button fields default to null`() {
        val layout = ConciergeLayout()
        assertNull(layout.ctaButtonBorderRadius)
        assertNull(layout.ctaButtonHorizontalPadding)
        assertNull(layout.ctaButtonVerticalPadding)
        assertNull(layout.ctaButtonFontSize)
        assertNull(layout.ctaButtonFontWeight)
        assertNull(layout.ctaButtonIconSize)
    }

    @Test
    fun `ConciergeLayout creates with cta button values`() {
        val layout = ConciergeLayout(
            ctaButtonBorderRadius = 99.0,
            ctaButtonHorizontalPadding = 16.0,
            ctaButtonVerticalPadding = 12.0,
            ctaButtonFontSize = 14.0,
            ctaButtonFontWeight = 400,
            ctaButtonIconSize = 16.0
        )
        assertEquals(99.0, layout.ctaButtonBorderRadius)
        assertEquals(16.0, layout.ctaButtonHorizontalPadding)
        assertEquals(12.0, layout.ctaButtonVerticalPadding)
        assertEquals(14.0, layout.ctaButtonFontSize)
        assertEquals(400, layout.ctaButtonFontWeight)
        assertEquals(16.0, layout.ctaButtonIconSize)
    }

    @Test
    fun `ConciergeLayout copy preserves cta button fields`() {
        val original = ConciergeLayout(ctaButtonBorderRadius = 99.0, ctaButtonFontSize = 14.0)
        val updated = original.copy(ctaButtonHorizontalPadding = 20.0)
        assertEquals(99.0, updated.ctaButtonBorderRadius)
        assertEquals(14.0, updated.ctaButtonFontSize)
        assertEquals(20.0, updated.ctaButtonHorizontalPadding)
        assertNull(original.ctaButtonHorizontalPadding)
    }

    // -----------------------------------------------------------------------
    // ConciergeCtaButtonColors
    // -----------------------------------------------------------------------

    @Test
    fun `ConciergeCtaButtonColors creates with all nulls by default`() {
        val colors = ConciergeCtaButtonColors()
        assertNull(colors.backgroundColor)
        assertNull(colors.textColor)
        assertNull(colors.iconColor)
    }

    @Test
    fun `ConciergeCtaButtonColors creates with custom values`() {
        val colors = ConciergeCtaButtonColors(
            backgroundColor = "#EDEDED",
            textColor = "#191F1C",
            iconColor = "#161313"
        )
        assertEquals("#EDEDED", colors.backgroundColor)
        assertEquals("#191F1C", colors.textColor)
        assertEquals("#161313", colors.iconColor)
    }

    @Test
    fun `ConciergeCtaButtonColors supports partial construction`() {
        val colors = ConciergeCtaButtonColors(backgroundColor = "#FFFFFF")
        assertEquals("#FFFFFF", colors.backgroundColor)
        assertNull(colors.textColor)
        assertNull(colors.iconColor)
    }

    @Test
    fun `ConciergeCtaButtonColors supports copy`() {
        val original = ConciergeCtaButtonColors(backgroundColor = "#EDEDED")
        val updated = original.copy(textColor = "#191F1C")
        assertEquals("#EDEDED", updated.backgroundColor)
        assertEquals("#191F1C", updated.textColor)
        assertNull(updated.iconColor)
        assertNull(original.textColor)
    }

    @Test
    fun `ConciergeThemeColors accepts ctaButton field`() {
        val ctaColors = ConciergeCtaButtonColors(
            backgroundColor = "#EDEDED",
            textColor = "#191F1C",
            iconColor = "#161313"
        )
        val themeColors = ConciergeThemeColors(ctaButton = ctaColors)
        assertEquals("#EDEDED", themeColors.ctaButton?.backgroundColor)
        assertEquals("#191F1C", themeColors.ctaButton?.textColor)
        assertEquals("#161313", themeColors.ctaButton?.iconColor)
    }

    @Test
    fun `ConciergeThemeColors ctaButton defaults to null`() {
        val themeColors = ConciergeThemeColors()
        assertNull(themeColors.ctaButton)
    }

    @Test
    fun `theme tokens support deep copy`() {
        val original = ConciergeThemeTokens(
            metadata = ConciergeThemeMetadata(name = "Original")
        )
        val updated = original.copy(
            metadata = original.metadata.copy(name = "Updated")
        )
        
        assertEquals("Original", original.metadata.name)
        assertEquals("Updated", updated.metadata.name)
    }
}
