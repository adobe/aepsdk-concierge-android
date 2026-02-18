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

import com.adobe.marketing.mobile.concierge.ConciergeConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ConciergeThemeModelsTest {

    // ========== String.toComposeColor() Extension Tests ==========

    @Test
    fun `toComposeColor handles 3-digit hex - RGB`() {
        // Given
        val hex = "#F53"

        // When
        val color = hex.toComposeColor()

        // Then
        assertNotNull(color)
        assertEquals(255, (color!!.red * 255).toInt())
        assertEquals(85, (color.green * 255).toInt())
        assertEquals(51, (color.blue * 255).toInt())
        assertEquals(1f, color.alpha, 0.01f)
    }

    @Test
    fun `toComposeColor handles 6-digit hex - RRGGBB`() {
        // Given
        val hex = "#FF5733"

        // When
        val color = hex.toComposeColor()

        // Then
        assertNotNull(color)
        assertEquals(255, (color!!.red * 255).toInt())
        assertEquals(87, (color.green * 255).toInt())
        assertEquals(51, (color.blue * 255).toInt())
        assertEquals(1f, color.alpha, 0.01f)
    }

    @Test
    fun `toComposeColor handles 8-digit hex - AARRGGBB`() {
        // Given
        val hex = "#80FF5733"

        // When
        val color = hex.toComposeColor()

        // Then
        assertNotNull(color)
        assertEquals(255, (color!!.red * 255).toInt())
        assertEquals(87, (color.green * 255).toInt())
        assertEquals(51, (color.blue * 255).toInt())
        // Alpha is 0x80 = 128 / 255 ≈ 0.502
        assertEquals(0.502f, color.alpha, 0.01f)
    }

    @Test
    fun `toComposeColor handles hex without hash prefix`() {
        // Given
        val hex = "FF5733"

        // When
        val color = hex.toComposeColor()

        // Then
        assertNotNull(color)
        assertEquals(255, (color!!.red * 255).toInt())
        assertEquals(87, (color.green * 255).toInt())
        assertEquals(51, (color.blue * 255).toInt())
    }

    @Test
    fun `toComposeColor handles lowercase hex`() {
        // Given
        val hex = "#aabbcc"

        // When
        val color = hex.toComposeColor()

        // Then
        assertNotNull(color)
        assertEquals(170, (color!!.red * 255).toInt())
        assertEquals(187, (color.green * 255).toInt())
        assertEquals(204, (color.blue * 255).toInt())
    }

    @Test
    fun `toComposeColor handles black`() {
        // Given
        val hex = "#000000"

        // When
        val color = hex.toComposeColor()

        // Then
        assertNotNull(color)
        assertEquals(0, (color!!.red * 255).toInt())
        assertEquals(0, (color.green * 255).toInt())
        assertEquals(0, (color.blue * 255).toInt())
    }

    @Test
    fun `toComposeColor handles white`() {
        // Given
        val hex = "#FFFFFF"

        // When
        val color = hex.toComposeColor()

        // Then
        assertNotNull(color)
        assertEquals(255, (color!!.red * 255).toInt())
        assertEquals(255, (color.green * 255).toInt())
        assertEquals(255, (color.blue * 255).toInt())
    }

    @Test
    fun `toComposeColor returns null for invalid hex length`() {
        // Given
        val hex = "#FF"

        // When
        val color = hex.toComposeColor()

        // Then
        assertNull(color)
    }

    @Test
    fun `toComposeColor returns null for invalid hex characters`() {
        // Given
        val hex = "#GGHHII"

        // When
        val color = hex.toComposeColor()

        // Then
        assertNull(color)
    }

    @Test
    fun `toComposeColor handles fully transparent color`() {
        // Given
        val hex = "#00000000"

        // When
        val color = hex.toComposeColor()

        // Then
        assertNotNull(color)
        assertEquals(0f, color!!.alpha, 0.01f)
    }

    @Test
    fun `toComposeColor handles fully opaque color with alpha`() {
        // Given
        val hex = "#FFFF5733"

        // When
        val color = hex.toComposeColor()

        // Then
        assertNotNull(color)
        assertEquals(1f, color!!.alpha, 0.01f)
        assertEquals(255, (color.red * 255).toInt())
    }

    // ========== String.toFontWeight() Extension Tests ==========

    @Test
    fun `toFontWeight handles thin`() {
        assertEquals(FontWeight.Thin, "thin".toFontWeight())
    }

    @Test
    fun `toFontWeight handles extralight`() {
        assertEquals(FontWeight.ExtraLight, "extralight".toFontWeight())
        assertEquals(FontWeight.ExtraLight, "extra_light".toFontWeight())
    }

    @Test
    fun `toFontWeight handles light`() {
        assertEquals(FontWeight.Light, "light".toFontWeight())
    }

    @Test
    fun `toFontWeight handles normal`() {
        assertEquals(FontWeight.Normal, "normal".toFontWeight())
        assertEquals(FontWeight.Normal, "regular".toFontWeight())
    }

    @Test
    fun `toFontWeight handles medium`() {
        assertEquals(FontWeight.Medium, "medium".toFontWeight())
    }

    @Test
    fun `toFontWeight handles semibold`() {
        assertEquals(FontWeight.SemiBold, "semibold".toFontWeight())
        assertEquals(FontWeight.SemiBold, "semi_bold".toFontWeight())
    }

    @Test
    fun `toFontWeight handles bold`() {
        assertEquals(FontWeight.Bold, "bold".toFontWeight())
    }

    @Test
    fun `toFontWeight handles extrabold`() {
        assertEquals(FontWeight.ExtraBold, "extrabold".toFontWeight())
        assertEquals(FontWeight.ExtraBold, "extra_bold".toFontWeight())
    }

    @Test
    fun `toFontWeight handles black`() {
        assertEquals(FontWeight.Black, "black".toFontWeight())
    }

    @Test
    fun `toFontWeight returns null for unknown weight`() {
        assertNull("unknown".toFontWeight())
    }

    @Test
    fun `toFontWeight handles case insensitive`() {
        assertEquals(FontWeight.Bold, "BOLD".toFontWeight())
        assertEquals(FontWeight.Normal, "NORMAL".toFontWeight())
        assertEquals(FontWeight.Medium, "Medium".toFontWeight())
    }

    // ========== Double.toDp() Extension Tests ==========

    @Test
    fun `toDp converts Double to Dp`() {
        // Given
        val value = 16.0

        // When
        val dp = value.toDp()

        // Then
        assertEquals(16.dp, dp)
    }

    @Test
    fun `toDp handles zero`() {
        assertEquals(0.dp, 0.0.toDp())
    }

    @Test
    fun `toDp handles decimal values`() {
        assertEquals(16.5.dp, 16.5.toDp())
    }

    @Test
    fun `toDp handles negative values`() {
        assertEquals((-10).dp, (-10.0).toDp())
    }

    @Test
    fun `toDp handles large values`() {
        assertEquals(1000.dp, 1000.0.toDp())
    }

    // ========== Double.toAlpha() Extension Tests ==========

    @Test
    fun `toAlpha converts Double to alpha in range 0 to 1`() {
        assertEquals(0.5f, 0.5.toAlpha(), 0.001f)
    }

    @Test
    fun `toAlpha handles zero`() {
        assertEquals(0f, 0.0.toAlpha(), 0.001f)
    }

    @Test
    fun `toAlpha handles one`() {
        assertEquals(1f, 1.0.toAlpha(), 0.001f)
    }

    @Test
    fun `toAlpha clamps values above 1`() {
        assertEquals(1f, 2.0.toAlpha(), 0.001f)
        assertEquals(1f, 10.0.toAlpha(), 0.001f)
    }

    @Test
    fun `toAlpha clamps values below 0`() {
        assertEquals(0f, (-1.0).toAlpha(), 0.001f)
        assertEquals(0f, (-10.0).toAlpha(), 0.001f)
    }

    @Test
    fun `toAlpha handles decimal values`() {
        assertEquals(0.25f, 0.25.toAlpha(), 0.001f)
        assertEquals(0.75f, 0.75.toAlpha(), 0.001f)
    }

    // ========== Data Class Tests ==========

    @Test
    fun `ConciergeThemeConfig creates with all fields`() {
        // Given & When
        val config = ConciergeThemeConfig(
            name = "TestTheme",
            colors = ConciergeThemeColors(),
            styles = ConciergeThemeStyles(),
            text = ConciergeTextStrings(),
            disclaimer = DisclaimerConfig(),
            welcomeExamples = listOf(ConciergeWelcomeExample("Example")),
            feedbackPositiveOptions = listOf("Good", "Helpful"),
            feedbackNegativeOptions = listOf("Bad", "Unhelpful"),
            typography = ConciergeTypographyConfig()
        )

        // Then
        assertEquals("TestTheme", config.name)
        assertNotNull(config.colors)
        assertNotNull(config.styles)
        assertNotNull(config.text)
        assertNotNull(config.disclaimer)
        assertEquals(1, config.welcomeExamples?.size)
        assertEquals(2, config.feedbackPositiveOptions?.size)
        assertEquals(2, config.feedbackNegativeOptions?.size)
    }

    @Test
    fun `ConciergeWelcomeExample creates with all fields`() {
        // Given & When
        val example = ConciergeWelcomeExample(
            text = "How do I get started?",
            image = "https://example.com/icon.png",
            backgroundColor = "#FF5733"
        )

        // Then
        assertEquals("How do I get started?", example.text)
        assertEquals("https://example.com/icon.png", example.image)
        assertEquals("#FF5733", example.backgroundColor)
    }

    @Test
    fun `ConciergeWelcomeExample creates with minimal fields`() {
        // Given & When
        val example = ConciergeWelcomeExample(text = "Simple prompt")

        // Then
        assertEquals("Simple prompt", example.text)
        assertNull(example.image)
        assertNull(example.backgroundColor)
    }

    @Test
    fun `DisclaimerLink creates correctly`() {
        // Given & When
        val link = DisclaimerLink(
            text = "Privacy Policy",
            url = "https://example.com/privacy"
        )

        // Then
        assertEquals("Privacy Policy", link.text)
        assertEquals("https://example.com/privacy", link.url)
    }

    @Test
    fun `ConciergeDisclaimer creates with text and links`() {
        // Given & When
        val disclaimer = DisclaimerConfig(
            text = "By using this service, you agree to our terms.",
            links = listOf(
                DisclaimerLink("Terms", "https://example.com/terms"),
                DisclaimerLink("Privacy", "https://example.com/privacy")
            )
        )

        // Then
        assertEquals("By using this service, you agree to our terms.", disclaimer.text)
        assertEquals(2, disclaimer.links?.size)
    }

    @Test
    fun `DisclaimerConfig creates with ConciergeConstants default text and default Terms link`() {
        val disclaimer = DisclaimerConfig(
            text = ConciergeConstants.Disclaimer.DEFAULT_TEXT,
            links = listOf(
                DisclaimerLink("Terms", ConciergeConstants.Disclaimer.DEFAULT_TERMS_URL)
            )
        )

        assertEquals(ConciergeConstants.Disclaimer.DEFAULT_TEXT, disclaimer.text)
        assertNotNull(disclaimer.links)
        assertEquals(1, disclaimer.links?.size)
        assertEquals("Terms", disclaimer.links?.get(0)?.text)
        assertEquals(ConciergeConstants.Disclaimer.DEFAULT_TERMS_URL, disclaimer.links?.get(0)?.url)
    }

    @Test
    fun `ConciergePrimaryColors data class supports copy`() {
        // Given
        val original = ConciergePrimaryColors(
            primary = "#FF0000",
            text = "#000000"
        )

        // When
        val modified = original.copy(primary = "#00FF00")

        // Then
        assertEquals("#00FF00", modified.primary)
        assertEquals("#000000", modified.text)
        // Original unchanged
        assertEquals("#FF0000", original.primary)
    }

    @Test
    fun `ConciergeMessageColors data class supports copy`() {
        // Given
        val original = ConciergeMessageColors(
            userBackground = "#FFFFFF",
            userText = "#000000",
            conciergeBackground = "#F0F0F0",
            conciergeText = "#333333",
            conciergeLink = "#0000FF"
        )

        // When
        val modified = original.copy(conciergeLink = "#FF0000")

        // Then
        assertEquals("#FF0000", modified.conciergeLink)
        assertEquals("#000000", original.userText)
    }

    @Test
    fun `ConciergeButtonColors creates with all fields`() {
        // Given & When
        val colors = ConciergeButtonColors(
            primaryBackground = "#0066CC",
            primaryText = "#FFFFFF",
            primaryHover = "#0052A3",
            secondaryBorder = "#CCCCCC",
            secondaryText = "#333333",
            secondaryHover = "#EEEEEE",
            secondaryHoverText = "#000000",
            submitFill = "#00CC00",
            submitFillDisabled = "#CCCCCC",
            submitText = "#FFFFFF",
            submitTextHover = "#EEEEEE",
            disabledBackground = "#F0F0F0"
        )

        // Then
        assertEquals("#0066CC", colors.primaryBackground)
        assertEquals("#FFFFFF", colors.primaryText)
        assertEquals("#00CC00", colors.submitFill)
    }

    @Test
    fun `ConciergeTypographyConfig creates with nullable fields`() {
        // Given & When
        val typography = ConciergeTypographyConfig(
            inputFontSize = 16.0,
            disclaimerFontSize = 12.0,
            disclaimerFontWeight = 700,
            citationsFontSize = 14.0
        )

        // Then
        assertEquals(16.0, typography.inputFontSize)
        assertEquals(12.0, typography.disclaimerFontSize)
        assertEquals(700, typography.disclaimerFontWeight)
        assertEquals(14.0, typography.citationsFontSize)
    }

    @Test
    fun `ConciergeTypographyConfig creates with default nulls`() {
        // Given & When
        val typography = ConciergeTypographyConfig()

        // Then
        assertNull(typography.inputFontSize)
        assertNull(typography.disclaimerFontSize)
        assertNull(typography.disclaimerFontWeight)
        assertNull(typography.citationsFontSize)
    }

    @Test
    fun `ConciergeTextStrings creates with all fields`() {
        // Given & When
        val strings = ConciergeTextStrings(
            inputPlaceholder = "Type your message...",
            welcomeHeading = "Welcome!",
            welcomeSubheading = "How can I help?",
            loadingMessage = "Thinking...",
            feedbackDialogTitlePositive = "Great!",
            feedbackDialogTitleNegative = "Sorry",
            feedbackDialogQuestionPositive = "What did you like?",
            feedbackDialogQuestionNegative = "What went wrong?",
            feedbackDialogNotes = "Additional notes",
            feedbackDialogSubmit = "Submit",
            feedbackDialogCancel = "Cancel",
            feedbackDialogNotesPlaceholder = "Enter notes...",
            feedbackToastSuccess = "Thank you!",
            errorNetwork = "Network error"
        )

        // Then
        assertEquals("Type your message...", strings.inputPlaceholder)
        assertEquals("Welcome!", strings.welcomeHeading)
        assertEquals("Thinking...", strings.loadingMessage)
        assertEquals("Submit", strings.feedbackDialogSubmit)
    }

    @Test
    fun `ConciergeInputColors creates and supports copy`() {
        // Given
        val original = ConciergeInputColors(
            background = "#FFFFFF",
            text = "#000000",
            outline = "#CCCCCC",
            outlineFocus = "#0066CC"
        )

        // When
        val modified = original.copy(outline = "#FF0000")

        // Then
        assertEquals("#FF0000", modified.outline)
        assertEquals("#CCCCCC", original.outline)
    }

    @Test
    fun `ConciergeFeedbackColors creates with fields`() {
        // Given & When
        val colors = ConciergeFeedbackColors(
            iconButtonBackground = "#F0F0F0",
            iconButtonHoverBackground = "#E0E0E0"
        )

        // Then
        assertEquals("#F0F0F0", colors.iconButtonBackground)
        assertEquals("#E0E0E0", colors.iconButtonHoverBackground)
    }

    @Test
    fun `ConciergeCitationColors creates with fields`() {
        // Given & When
        val colors = ConciergeCitationColors(
            backgroundColor = "#FAFAFA",
            textColor = "#333333"
        )

        // Then
        assertEquals("#FAFAFA", colors.backgroundColor)
        assertEquals("#333333", colors.textColor)
    }

    @Test
    fun `ConciergeSurfaceColors creates with fields`() {
        // Given & When
        val colors = ConciergeSurfaceColors(
            mainContainerBackground = "#FFFFFF",
            mainContainerBottomBackground = "#F5F5F5",
            messageBlockerBackground = "#F0F0F0"
        )

        // Then
        assertEquals("#FFFFFF", colors.mainContainerBackground)
        assertEquals("#F5F5F5", colors.mainContainerBottomBackground)
        assertEquals("#F0F0F0", colors.messageBlockerBackground)
    }

    @Test
    fun `ConciergeThemeColors creates with nested structures`() {
        // Given & When
        val colors = ConciergeThemeColors(
            primary = "#0066CC",
            primaryColors = ConciergePrimaryColors(primary = "#0066CC", text = "#FFFFFF"),
            surfaceColors = ConciergeSurfaceColors(mainContainerBackground = "#FFFFFF"),
            message = ConciergeMessageColors(userBackground = "#E0E0E0"),
            button = ConciergeButtonColors(primaryBackground = "#0066CC"),
            input = ConciergeInputColors(background = "#FFFFFF"),
            feedback = ConciergeFeedbackColors(iconButtonBackground = "#F0F0F0"),
            citation = ConciergeCitationColors(backgroundColor = "#FAFAFA")
        )

        // Then
        assertEquals("#0066CC", colors.primary)
        assertNotNull(colors.primaryColors)
        assertNotNull(colors.surfaceColors)
        assertNotNull(colors.message)
        assertNotNull(colors.button)
        assertNotNull(colors.input)
        assertNotNull(colors.feedback)
        assertNotNull(colors.citation)
    }

    // ========== Style Data Classes Tests ==========

    @Test
    fun `ConciergeMessageBubbleStyle creates with fields`() {
        // Given & When
        val style = ConciergeMessageBubbleStyle(
            padding = 16.0,
            innerPadding = 12.0,
            cornerRadius = 8.0,
            elevation = 2.0,
            contentSpacing = 8.0,
            segmentSpacing = 12.0
        )

        // Then
        assertEquals(16.0, style.padding)
        assertEquals(12.0, style.innerPadding)
        assertEquals(8.0, style.cornerRadius)
    }

    @Test
    fun `ConciergeThinkingAnimationStyle creates with fields`() {
        // Given & When
        val style = ConciergeThinkingAnimationStyle(
            dotSize = 8.0,
            dotSpacing = 4.0,
            textDotSpacing = 8.0,
            dotColorAlpha = 0.5,
            dotAnimationDuration = 600,
            dotAnimationDelay = 100,
            thinkingText = "Thinking..."
        )

        // Then
        assertEquals(8.0, style.dotSize)
        assertEquals(600, style.dotAnimationDuration)
        assertEquals("Thinking...", style.thinkingText)
    }

    @Test
    fun `ConciergeProductCardStyle creates with fields`() {
        // Given & When
        val style = ConciergeProductCardStyle(
            cornerRadius = 12.0,
            elevation = 4.0,
            imageHeight = 200.0,
            titleFontWeight = "bold",
            titleMaxLines = 2,
            captionTopPadding = 8.0,
            captionBottomPadding = 8.0,
            textTopPadding = 12.0
        )

        // Then
        assertEquals(12.0, style.cornerRadius)
        assertEquals(200.0, style.imageHeight)
        assertEquals("bold", style.titleFontWeight)
        assertEquals(2, style.titleMaxLines)
    }

    @Test
    fun `ConciergeFeedbackButtonsStyle creates with fields`() {
        // Given & When
        val style = ConciergeFeedbackButtonsStyle(
            buttonSize = 32.0,
            iconSize = 20.0,
            spacing = 8.0
        )

        // Then
        assertEquals(32.0, style.buttonSize)
        assertEquals(20.0, style.iconSize)
        assertEquals(8.0, style.spacing)
    }

    @Test
    fun `ConciergeMicButtonStyle creates with animation properties`() {
        // Given & When
        val style = ConciergeMicButtonStyle(
            size = 40.0,
            pulsingBackgroundAlpha = 0.3,
            pulseAnimationDuration = 1000,
            pulseScaleMin = 0.9,
            pulseScaleMax = 1.1,
            ringAlpha = 0.2
        )

        // Then
        assertEquals(40.0, style.size)
        assertEquals(0.3, style.pulsingBackgroundAlpha)
        assertEquals(1000, style.pulseAnimationDuration)
        assertEquals(0.9, style.pulseScaleMin)
        assertEquals(1.1, style.pulseScaleMax)
    }

    @Test
    fun `ConciergeThemeStyles creates with nested style objects`() {
        // Given & When
        val styles = ConciergeThemeStyles(
            messageBubble = ConciergeMessageBubbleStyle(padding = 16.0),
            thinkingAnimation = ConciergeThinkingAnimationStyle(dotSize = 8.0),
            productCard = ConciergeProductCardStyle(cornerRadius = 12.0),
            feedbackButtons = ConciergeFeedbackButtonsStyle(buttonSize = 32.0),
            micButton = ConciergeMicButtonStyle(size = 40.0),
            sendButton = ConciergeSendButtonStyle(size = 32.0)
        )

        // Then
        assertNotNull(styles.messageBubble)
        assertNotNull(styles.thinkingAnimation)
        assertNotNull(styles.productCard)
        assertNotNull(styles.feedbackButtons)
        assertNotNull(styles.micButton)
        assertNotNull(styles.sendButton)
    }
}
