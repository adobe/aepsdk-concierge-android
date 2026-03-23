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

import com.adobe.marketing.mobile.concierge.ConciergeConstants
import androidx.compose.ui.graphics.Color
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class ThemeParserTest {

    @Test
    fun `parseThemeJson should parse valid complete theme`() {
        val json = """
            {
                "metadata": {
                    "brandName": "Concierge Demo",
                    "version": "1.0.0",
                    "language": "en-US"
                },
                "behavior": {
                    "input": {
                        "enableVoiceInput": true
                    }
                },
                "theme": {
                    "--color-primary": "#EB1000",
                    "--color-text": "#131313",
                    "--main-container-background": "#FFFFFF",
                    "--main-container-bottom-background": "#FFFFFF"
                },
                "text": {
                    "welcome.heading": "Welcome to Brand Concierge!",
                    "input.placeholder": "How can I help?"
                },
                "disclaimer": {
                    "text": "AI responses may be inaccurate.",
                    "links": [
                        {
                            "text": "Terms",
                            "url": "https://example.com/terms"
                        }
                    ]
                },
                "arrays": {
                    "welcome.examples": [
                        {
                            "text": "Example prompt",
                            "backgroundColor": "#F5F5F5"
                        }
                    ],
                    "feedback.positive.options": ["Helpful", "Clear"]
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertNotNull(config)
        assertEquals("Concierge Demo", config?.name)
        assertEquals("#EB1000", config?.colors?.primaryColors?.primary)
        assertEquals("#131313", config?.colors?.primaryColors?.text)
        assertEquals("Welcome to Brand Concierge!", config?.text?.welcomeHeading)
        assertEquals("How can I help?", config?.text?.inputPlaceholder)
        assertEquals("AI responses may be inaccurate.", config?.disclaimer?.text)
        assertEquals(1, config?.disclaimer?.links?.size)
        assertEquals("Terms", config?.disclaimer?.links?.get(0)?.text)
        assertEquals(1, config?.welcomeExamples?.size)
        assertEquals(2, config?.feedbackPositiveOptions?.size)
    }

    @Test
    fun `parseThemeJson should handle minimal theme`() {
        val json = """
            {
                "theme": {
                    "--color-primary": "#FF0000"
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertNotNull(config)
        assertEquals("#FF0000", config?.colors?.primaryColors?.primary)
        assertNull(config?.styles)
    }

    @Test
    fun `parseThemeJson should return null for invalid JSON`() {
        val json = "{ invalid json }"
        val config = ThemeParser.parseThemeJson(json)
        assertNull(config)
    }

    @Test
    fun `toComposeColor should parse 6-digit hex colors`() {
        val color = "#3949AB".toComposeColor()
        assertNotNull(color)
        assertTrue(color is Color)
    }

    @Test
    fun `toComposeColor should parse 8-digit hex colors with alpha`() {
        val color = "#803949AB".toComposeColor()
        assertNotNull(color)
        assertTrue(color is Color)
    }

    @Test
    fun `toComposeColor should parse 3-digit hex colors`() {
        val color = "#FFF".toComposeColor()
        assertNotNull(color)
        assertTrue(color is Color)
    }

    @Test
    fun `toComposeColor should return null for invalid hex`() {
        val color = "not-a-color".toComposeColor()
        assertNull(color)
    }

    @Test
    fun `toFontWeight should parse valid font weights`() {
        assertEquals(androidx.compose.ui.text.font.FontWeight.Thin, "thin".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Light, "light".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Normal, "normal".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Medium, "medium".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, "bold".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Black, "black".toFontWeight())
    }

    @Test
    fun `toFontWeight should handle case insensitivity`() {
        assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, "BOLD".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, "Bold".toFontWeight())
    }

    @Test
    fun `toFontWeight should return null for invalid weight`() {
        assertNull("invalid".toFontWeight())
    }

    @Test
    fun `createColorsFromJson should merge with defaults`() {
        val jsonColors = ConciergeThemeColors(
            primary = "#FF0000",
            onPrimary = "#FFFFFF"
            // Other colors null
        )

        val colors = ThemeParser.createColorsFromJson(jsonColors, LightConciergeColors)

        // Should use JSON values where provided
        assertEquals(Color(0xFFFF0000), colors.primary)
        assertEquals(Color.White, colors.onPrimary)

        // Should fall back to defaults for missing values
        assertEquals(LightConciergeColors.secondary, colors.secondary)
        assertEquals(LightConciergeColors.surface, colors.surface)
        assertEquals(LightConciergeColors.background, colors.background)
    }

    @Test
    fun `createColorsFromJson should return defaults when json is null`() {
        val colors = ThemeParser.createColorsFromJson(null, LightConciergeColors)
        assertEquals(LightConciergeColors, colors)
    }

    @Test
    fun `toDp should convert Double to Dp`() {
        val dp = 16.0.toDp()
        assertEquals(16.0f, dp.value)
    }

    @Test
    fun `toAlpha should convert Double to Float and clamp`() {
        assertEquals(0.5f, 0.5.toAlpha())
        assertEquals(0.0f, (-0.5).toAlpha()) // Should clamp to 0
        assertEquals(1.0f, 1.5.toAlpha()) // Should clamp to 1
    }

    @Test
    fun `parseThemeJson should handle custom text values`() {
        val json = """
            {
                "text": {
                    "input.placeholder": "Custom placeholder",
                    "loading.message": "Processing"
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertEquals("Custom placeholder", config?.text?.inputPlaceholder)
        assertEquals("Processing", config?.text?.loadingMessage)
    }

    @Test
    fun `parseThemeJson should handle empty JSON object`() {
        val json = "{}"
        val config = ThemeParser.parseThemeJson(json)
        assertNotNull(config)
    }

    @Test
    fun `parseThemeJson should handle all text strings`() {
        val json = """
            {
                "text": {
                    "input.placeholder": "Type here",
                    "welcome.heading": "Welcome!",
                    "welcome.subheading": "How can I help?",
                    "loading.message": "Loading...",
                    "feedback.dialog.title.positive": "Great!",
                    "feedback.dialog.title.negative": "Sorry!",
                    "feedback.dialog.question.positive": "What went well?",
                    "feedback.dialog.question.negative": "What went wrong?",
                    "feedback.dialog.notes": "Notes",
                    "feedback.dialog.submit": "Submit",
                    "feedback.dialog.cancel": "Cancel",
                    "feedback.dialog.notes.placeholder": "Enter notes",
                    "feedback.toast.success": "Thank you!",
                    "error.network": "Network error"
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)
        
        assertNotNull(config?.text)
        assertEquals("Type here", config?.text?.inputPlaceholder)
        assertEquals("Welcome!", config?.text?.welcomeHeading)
        assertEquals("How can I help?", config?.text?.welcomeSubheading)
        assertEquals("Loading...", config?.text?.loadingMessage)
        assertEquals("Great!", config?.text?.feedbackDialogTitlePositive)
        assertEquals("Sorry!", config?.text?.feedbackDialogTitleNegative)
        assertEquals("What went well?", config?.text?.feedbackDialogQuestionPositive)
        assertEquals("What went wrong?", config?.text?.feedbackDialogQuestionNegative)
        assertEquals("Notes", config?.text?.feedbackDialogNotes)
        assertEquals("Submit", config?.text?.feedbackDialogSubmit)
        assertEquals("Cancel", config?.text?.feedbackDialogCancel)
        assertEquals("Enter notes", config?.text?.feedbackDialogNotesPlaceholder)
        assertEquals("Thank you!", config?.text?.feedbackToastSuccess)
        assertEquals("Network error", config?.text?.errorNetwork)
    }

    @Test
    fun `parseThemeJson should handle disclaimer with links`() {
        val json = """
            {
                "disclaimer": {
                    "text": "By using this service, you agree to our terms.",
                    "links": [
                        {
                            "text": "Privacy Policy",
                            "url": "https://example.com/privacy"
                        },
                        {
                            "text": "Terms of Service",
                            "url": "https://example.com/terms"
                        }
                    ]
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)
        
        assertNotNull(config?.disclaimer)
        assertEquals("By using this service, you agree to our terms.", config?.disclaimer?.text)
        assertEquals(2, config?.disclaimer?.links?.size)
        assertEquals("Privacy Policy", config?.disclaimer?.links?.get(0)?.text)
        assertEquals("https://example.com/privacy", config?.disclaimer?.links?.get(0)?.url)
        assertEquals("Terms of Service", config?.disclaimer?.links?.get(1)?.text)
        assertEquals("https://example.com/terms", config?.disclaimer?.links?.get(1)?.url)
    }

    @Test
    fun `parseThemeJson should handle disclaimer without links and use default Terms link`() {
        val json = """
            {
                "disclaimer": {
                    "text": "Disclaimer text only"
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertEquals("Disclaimer text only", config?.disclaimer?.text)
        assertNotNull(config?.disclaimer?.links)
        assertEquals(1, config?.disclaimer?.links?.size)
        assertEquals("Terms", config?.disclaimer?.links?.get(0)?.text)
        assertEquals(ConciergeConstants.Disclaimer.DEFAULT_TERMS_URL, config?.disclaimer?.links?.get(0)?.url)
    }

    @Test
    fun `parseThemeJson should use default Terms link when disclaimer has empty links`() {
        // When disclaimer has text but links array is empty, parser applies default Terms link
        val json = """
            {
                "metadata": { "brandName": "Concierge Demo", "version": "1.0.0" },
                "behavior": { "input": { "enableVoiceInput": true } },
                "theme": { "--color-primary": "#EB1000", "--color-text": "#131313" },
                "text": { "welcome.heading": "Welcome", "input.placeholder": "How can I help?" },
                "disclaimer": {
                    "text": "AI responses may be inaccurate. Check answers and sources. {Terms}",
                    "links": []
                },
                "arrays": {
                    "welcome.examples": [{ "text": "Example prompt" }],
                    "feedback.positive.options": ["Helpful", "Clear"]
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertNotNull(config)
        assertNotNull(config?.disclaimer)
        assertEquals(ConciergeConstants.Disclaimer.DEFAULT_TEXT, config?.disclaimer?.text)
        assertNotNull(config?.disclaimer?.links)
        assertEquals(1, config?.disclaimer?.links?.size)
        assertEquals("Terms", config?.disclaimer?.links?.get(0)?.text)
        assertEquals(ConciergeConstants.Disclaimer.DEFAULT_TERMS_URL, config?.disclaimer?.links?.get(0)?.url)
    }

    @Test
    fun `parseThemeJson should filter invalid disclaimer links`() {
        val json = """
            {
                "metadata": { "brandName": "Concierge Demo", "version": "1.0.0" },
                "behavior": { "input": { "enableVoiceInput": true } },
                "theme": { "--color-primary": "#EB1000" },
                "text": { "input.placeholder": "How can I help?" },
                "disclaimer": {
                    "text": "Custom disclaimer. {Valid Link}",
                    "links": [
                        { "text": "Valid Link", "url": "https://example.com/valid" },
                        { "text": "Invalid Link" },
                        { "url": "https://example.com/no-text" }
                    ]
                },
                "arrays": {
                    "welcome.examples": [{ "text": "Example" }],
                    "feedback.positive.options": ["Helpful"]
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertNotNull(config)
        assertNotNull(config?.disclaimer)
        assertEquals("Custom disclaimer. {Valid Link}", config?.disclaimer?.text)
        assertEquals(1, config?.disclaimer?.links?.size)
        assertEquals("Valid Link", config?.disclaimer?.links?.get(0)?.text)
    }

    @Test
    fun `parseThemeJson should handle welcome examples`() {
        val json = """
            {
                "arrays": {
                    "welcome.examples": [
                        {
                            "text": "What products do you offer?",
                            "image": "https://example.com/icon1.png",
                            "backgroundColor": "#FF5733"
                        },
                        {
                            "text": "How do I contact support?",
                            "image": "https://example.com/icon2.png"
                        },
                        {
                            "text": "Tell me about your services"
                        }
                    ]
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)
        
        assertNotNull(config?.welcomeExamples)
        assertEquals(3, config?.welcomeExamples?.size)
        assertEquals("What products do you offer?", config?.welcomeExamples?.get(0)?.text)
        assertEquals("https://example.com/icon1.png", config?.welcomeExamples?.get(0)?.image)
        assertEquals("#FF5733", config?.welcomeExamples?.get(0)?.backgroundColor)
        assertEquals("How do I contact support?", config?.welcomeExamples?.get(1)?.text)
        assertEquals("https://example.com/icon2.png", config?.welcomeExamples?.get(1)?.image)
        assertNull(config?.welcomeExamples?.get(1)?.backgroundColor)
        assertEquals("Tell me about your services", config?.welcomeExamples?.get(2)?.text)
        assertNull(config?.welcomeExamples?.get(2)?.image)
    }

    @Test
    fun `parseThemeJson should filter welcome examples without text`() {
        val json = """
            {
                "arrays": {
                    "welcome.examples": [
                        {
                            "text": "Valid example"
                        },
                        {
                            "image": "https://example.com/icon.png"
                        }
                    ]
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)
        
        // Should only include examples with text
        assertEquals(1, config?.welcomeExamples?.size)
        assertEquals("Valid example", config?.welcomeExamples?.get(0)?.text)
    }

    @Test
    fun `parseThemeJson should handle feedback options arrays`() {
        val json = """
            {
                "arrays": {
                    "feedback.positive.options": ["Helpful", "Accurate", "Clear"],
                    "feedback.negative.options": ["Unhelpful", "Inaccurate", "Confusing"]
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)
        
        assertEquals(3, config?.feedbackPositiveOptions?.size)
        assertEquals("Helpful", config?.feedbackPositiveOptions?.get(0))
        assertEquals("Accurate", config?.feedbackPositiveOptions?.get(1))
        assertEquals("Clear", config?.feedbackPositiveOptions?.get(2))
        
        assertEquals(3, config?.feedbackNegativeOptions?.size)
        assertEquals("Unhelpful", config?.feedbackNegativeOptions?.get(0))
        assertEquals("Inaccurate", config?.feedbackNegativeOptions?.get(1))
        assertEquals("Confusing", config?.feedbackNegativeOptions?.get(2))
    }

    @Test
    fun `parseThemeJson should handle typography config`() {
        val json = """
            {
                "theme": {
                    "--input-font-size": "16px",
                    "--disclaimer-font-size": "12px",
                    "--disclaimer-font-weight": "700",
                    "--citations-desktop-button-font-size": "14px"
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertNotNull(config?.typography)
        assertEquals(16.0, config?.typography?.inputFontSize)
        assertEquals(12.0, config?.typography?.disclaimerFontSize)
        assertEquals(700, config?.typography?.disclaimerFontWeight)
        assertEquals(14.0, config?.typography?.citationsFontSize)
    }

    @Test
    fun `parseThemeJson should handle complex theme with multiple sections`() {
        val json = """
            {
                "metadata": {
                    "brandName": "Complete Theme",
                    "version": "1.0.0",
                    "language": "en-US"
                },
                "behavior": {
                    "multimodalCarousel": {
                        "cardClickAction": "openLink"
                    },
                    "input": {
                        "enableVoiceInput": true,
                        "disableMultiline": false
                    }
                },
                "theme": {
                    "--color-primary": "#3B63FB",
                    "--message-user-background": "#EBEEFF",
                    "--message-concierge-background": "#F5F5F5",
                    "--input-height-mobile": "52px",
                    "--input-border-radius-mobile": "12px"
                },
                "text": {
                    "input.placeholder": "Ask me anything",
                    "welcome.heading": "Welcome!",
                    "loading.message": "Generating response"
                },
                "disclaimer": {
                    "text": "AI responses may be inaccurate. {Terms}",
                    "links": [
                        {
                            "text": "Terms",
                            "url": "https://example.com/terms"
                        }
                    ]
                },
                "arrays": {
                    "welcome.examples": [
                        {
                            "text": "Example prompt",
                            "image": "https://example.com/image.png",
                            "backgroundColor": "#F5F5F5"
                        }
                    ],
                    "feedback.positive.options": ["Helpful", "Clear"],
                    "feedback.negative.options": ["Unhelpful", "Errors"]
                },
                "assets": {
                    "icons": {
                        "company": "company-logo.svg"
                    }
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)
        
        assertNotNull(config)
        assertEquals("Complete Theme", config?.name)
        assertEquals("#3B63FB", config?.colors?.primaryColors?.primary)
        assertEquals("#EBEEFF", config?.colors?.message?.userBackground)
        assertEquals("#F5F5F5", config?.colors?.message?.conciergeBackground)
        assertEquals("Ask me anything", config?.text?.inputPlaceholder)
        assertEquals("Welcome!", config?.text?.welcomeHeading)
        assertEquals("Generating response", config?.text?.loadingMessage)
        assertEquals("AI responses may be inaccurate. {Terms}", config?.disclaimer?.text)
        assertEquals(1, config?.disclaimer?.links?.size)
        assertEquals(1, config?.welcomeExamples?.size)
        assertEquals("Example prompt", config?.welcomeExamples?.get(0)?.text)
        assertEquals("#F5F5F5", config?.welcomeExamples?.get(0)?.backgroundColor)
        assertEquals(2, config?.feedbackPositiveOptions?.size)
        assertEquals(2, config?.feedbackNegativeOptions?.size)
    }

    // ========== Tests for parseThemeTokens ==========

    @Test
    fun `parseThemeTokens should parse valid theme tokens`() {
        val json = """
            {
                "metadata": {
                    "brandName": "Test Theme",
                    "version": "1.0.0"
                },
                "theme": {
                    "--color-primary": "#FF0000",
                    "--main-container-background": "#FFFFFF"
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertNotNull(tokens)
        assertEquals("Test Theme", tokens?.metadata?.name)
        assertEquals("1.0.0", tokens?.metadata?.version)
        assertEquals("#FF0000", tokens?.colors?.primaryColors?.primary)
        assertEquals("#FFFFFF", tokens?.colors?.surfaceColors?.mainContainerBackground)
    }

    @Test
    fun `parseThemeTokens should return null for invalid JSON`() {
        val json = "{ invalid }"
        val tokens = ThemeParser.parseThemeTokens(json)
        assertNull(tokens)
    }

    @Test
    fun `parseThemeTokens should handle empty theme object`() {
        val json = "{}"
        val tokens = ThemeParser.parseThemeTokens(json)
        assertNotNull(tokens)
    }

    @Test
    fun `parseThemeTokens should parse metadata with all fields`() {
        val json = """
            {
                "metadata": {
                    "name": "Full Theme",
                    "version": "2.0.0",
                    "description": "A comprehensive theme",
                    "author": "Test Author",
                    "lastModified": "2025-01-01"
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertEquals("Full Theme", tokens?.metadata?.name)
        assertEquals("2.0.0", tokens?.metadata?.version)
        assertEquals("A comprehensive theme", tokens?.metadata?.description)
        assertEquals("Test Author", tokens?.metadata?.author)
        assertEquals("2025-01-01", tokens?.metadata?.lastModified)
    }

    @Test
    fun `parseThemeTokens should handle brandName fallback for metadata name`() {
        val json = """
            {
                "metadata": {
                    "brandName": "Brand Theme"
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertEquals("Brand Theme", tokens?.metadata?.name)
    }

    @Test
    fun `parseThemeTokens should parse behavior section`() {
        val json = """
            {
                "behavior": {
                    "enableDarkMode": false,
                    "enableAnimations": true,
                    "enableHaptics": false,
                    "enableSoundEffects": true,
                    "autoScrollToBottom": false,
                    "showTimestamps": true,
                    "enableMarkdown": true,
                    "enableCitations": false,
                    "input": {
                        "enableVoiceInput": false
                    },
                    "maxMessageLength": 5000,
                    "typingIndicatorDelay": 1000
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertNotNull(tokens?.behavior)
        assertEquals(false, tokens?.behavior?.enableDarkMode)
        assertEquals(true, tokens?.behavior?.enableAnimations)
        assertEquals(false, tokens?.behavior?.enableHaptics)
        assertEquals(true, tokens?.behavior?.enableSoundEffects)
        assertEquals(false, tokens?.behavior?.autoScrollToBottom)
        assertEquals(true, tokens?.behavior?.showTimestamps)
        assertEquals(true, tokens?.behavior?.enableMarkdown)
        assertEquals(false, tokens?.behavior?.enableCitations)
        assertEquals(false, tokens?.behavior?.enableVoiceInput)
        assertEquals(5000, tokens?.behavior?.maxMessageLength)
        assertEquals(1000, tokens?.behavior?.typingIndicatorDelay)
    }

    @Test
    fun `parseThemeTokens should parse assets section with all icons`() {
        val json = """
            {
                "assets": {
                    "icons": {
                        "company": "company.svg",
                        "send": "send.svg",
                        "microphone": "mic.svg",
                        "close": "close.svg",
                        "thumbsUp": "thumbs-up.svg",
                        "thumbsDown": "thumbs-down.svg",
                        "chevronDown": "chevron-down.svg",
                        "chevronRight": "chevron-right.svg"
                    }
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertEquals("company.svg", tokens?.assets?.icons?.company)
        assertEquals("send.svg", tokens?.assets?.icons?.send)
        assertEquals("mic.svg", tokens?.assets?.icons?.microphone)
        assertEquals("close.svg", tokens?.assets?.icons?.close)
        assertEquals("thumbs-up.svg", tokens?.assets?.icons?.thumbsUp)
        assertEquals("thumbs-down.svg", tokens?.assets?.icons?.thumbsDown)
        assertEquals("chevron-down.svg", tokens?.assets?.icons?.chevronDown)
        assertEquals("chevron-right.svg", tokens?.assets?.icons?.chevronRight)
    }

    @Test
    fun `parseThemeTokens should parse assets section with images`() {
        val json = """
            {
                "assets": {
                    "images": {
                        "welcomeBanner": "banner.png",
                        "errorPlaceholder": "error.png",
                        "avatarBot": "bot-avatar.png",
                        "avatarUser": "user-avatar.png"
                    }
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertEquals("banner.png", tokens?.assets?.images?.welcomeBanner)
        assertEquals("error.png", tokens?.assets?.images?.errorPlaceholder)
        assertEquals("bot-avatar.png", tokens?.assets?.images?.avatarBot)
        assertEquals("user-avatar.png", tokens?.assets?.images?.avatarUser)
    }

    @Test
    fun `parseThemeTokens should parse assets section with fonts`() {
        val json = """
            {
                "assets": {
                    "fonts": {
                        "regular": "Regular.ttf",
                        "medium": "Medium.ttf",
                        "bold": "Bold.ttf",
                        "light": "Light.ttf"
                    }
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertEquals("Regular.ttf", tokens?.assets?.fonts?.regular)
        assertEquals("Medium.ttf", tokens?.assets?.fonts?.medium)
        assertEquals("Bold.ttf", tokens?.assets?.fonts?.bold)
        assertEquals("Light.ttf", tokens?.assets?.fonts?.light)
    }

    @Test
    fun `parseThemeTokens should parse content section with text`() {
        val json = """
            {
                "content": {
                    "text": {
                        "welcomeTitle": "Welcome to Chat",
                        "welcomeSubtitle": "How may I assist you?",
                        "disclaimerText": "AI responses may be inaccurate",
                        "errorTitle": "Oops!",
                        "errorRetry": "Retry",
                        "feedbackTitle": "Give Feedback",
                        "feedbackSubmit": "Send",
                        "feedbackCancel": "Close",
                        "sourcesLabel": "References",
                        "thinkingLabel": "Processing",
                        "listeningLabel": "Recording"
                    }
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertEquals("Welcome to Chat", tokens?.content?.text?.welcomeTitle)
        assertEquals("How may I assist you?", tokens?.content?.text?.welcomeSubtitle)
        assertEquals("AI responses may be inaccurate", tokens?.content?.text?.disclaimerText)
        assertEquals("Oops!", tokens?.content?.text?.errorTitle)
        assertEquals("Retry", tokens?.content?.text?.errorRetry)
        assertEquals("Give Feedback", tokens?.content?.text?.feedbackTitle)
        assertEquals("Send", tokens?.content?.text?.feedbackSubmit)
        assertEquals("Close", tokens?.content?.text?.feedbackCancel)
        assertEquals("References", tokens?.content?.text?.sourcesLabel)
        assertEquals("Processing", tokens?.content?.text?.thinkingLabel)
        assertEquals("Recording", tokens?.content?.text?.listeningLabel)
    }

    @Test
    fun `parseThemeTokens should parse content section with placeholders`() {
        val json = """
            {
                "content": {
                    "placeholders": {
                        "inputPlaceholder": "Type a message",
                        "listeningPlaceholder": "Say something",
                        "emptyStateMessage": "No messages yet"
                    }
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertEquals("Type a message", tokens?.content?.placeholders?.inputPlaceholder)
        assertEquals("Say something", tokens?.content?.placeholders?.listeningPlaceholder)
        assertEquals("No messages yet", tokens?.content?.placeholders?.emptyStateMessage)
    }

    @Test
    fun `parseThemeTokens should parse content section with accessibility`() {
        val json = """
            {
                "content": {
                    "accessibility": {
                        "sendButtonLabel": "Send message button",
                        "micButtonLabel": "Voice input button",
                        "closeButtonLabel": "Close dialog",
                        "thumbsUpLabel": "Positive feedback",
                        "thumbsDownLabel": "Negative feedback"
                    }
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertEquals("Send message button", tokens?.content?.accessibility?.sendButtonLabel)
        assertEquals("Voice input button", tokens?.content?.accessibility?.micButtonLabel)
        assertEquals("Close dialog", tokens?.content?.accessibility?.closeButtonLabel)
        assertEquals("Positive feedback", tokens?.content?.accessibility?.thumbsUpLabel)
        assertEquals("Negative feedback", tokens?.content?.accessibility?.thumbsDownLabel)
    }

    @Test
    fun `parseThemeTokens should parse layout section with spacing`() {
        val json = """
            {
                "layout": {
                    "spacing": {
                        "xs": 2.0,
                        "sm": 4.0,
                        "md": 8.0,
                        "lg": 16.0,
                        "xl": 24.0,
                        "xxl": 32.0
                    }
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertEquals(2.0, tokens?.layout?.spacing?.xs)
        assertEquals(4.0, tokens?.layout?.spacing?.sm)
        assertEquals(8.0, tokens?.layout?.spacing?.md)
        assertEquals(16.0, tokens?.layout?.spacing?.lg)
        assertEquals(24.0, tokens?.layout?.spacing?.xl)
        assertEquals(32.0, tokens?.layout?.spacing?.xxl)
    }

    @Test
    fun `parseThemeTokens should parse layout section with sizing`() {
        val json = """
            {
                "layout": {
                    "sizing": {
                        "iconSm": 12.0,
                        "iconMd": 18.0,
                        "iconLg": 24.0,
                        "avatarSm": 28.0,
                        "avatarMd": 36.0,
                        "avatarLg": 44.0,
                        "buttonHeightSm": 30.0,
                        "buttonHeightMd": 38.0,
                        "buttonHeightLg": 46.0
                    }
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertEquals(12.0, tokens?.layout?.sizing?.iconSm)
        assertEquals(18.0, tokens?.layout?.sizing?.iconMd)
        assertEquals(24.0, tokens?.layout?.sizing?.iconLg)
        assertEquals(28.0, tokens?.layout?.sizing?.avatarSm)
        assertEquals(36.0, tokens?.layout?.sizing?.avatarMd)
        assertEquals(44.0, tokens?.layout?.sizing?.avatarLg)
        assertEquals(30.0, tokens?.layout?.sizing?.buttonHeightSm)
        assertEquals(38.0, tokens?.layout?.sizing?.buttonHeightMd)
        assertEquals(46.0, tokens?.layout?.sizing?.buttonHeightLg)
    }

    @Test
    fun `parseThemeTokens should parse layout section with positioning`() {
        val json = """
            {
                "layout": {
                    "positioning": {
                        "headerHeight": 60.0,
                        "footerHeight": 80.0,
                        "maxContentWidth": 1000.0,
                        "minContentWidth": 320.0
                    }
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        
        assertEquals(60.0, tokens?.layout?.positioning?.headerHeight)
        assertEquals(80.0, tokens?.layout?.positioning?.footerHeight)
        assertEquals(1000.0, tokens?.layout?.positioning?.maxContentWidth)
        assertEquals(320.0, tokens?.layout?.positioning?.minContentWidth)
    }

    // ========== Tests for createColorsFromJson ==========

    @Test
    fun `createColorsFromJson should handle CSS theme structure`() {
        val themeColors = ConciergeThemeColors(
            primaryColors = ConciergePrimaryColors(
                primary = "#FF0000",
                text = "#FFFFFF"
            ),
            surfaceColors = ConciergeSurfaceColors(
                mainContainerBackground = "#F0F0F0",
                mainContainerBottomBackground = "#E0E0E0"
            ),
            message = ConciergeMessageColors(
                userBackground = "#FFFFFF",
                userText = "#000000",
                conciergeBackground = "#F5F5F5",
                conciergeText = "#333333",
                conciergeLink = "#0066CC"
            )
        )

        val colors = ThemeParser.createColorsFromJson(themeColors, LightConciergeColors)
        
        assertEquals(Color(0xFFFF0000), colors.primary)
        assertEquals(Color.White, colors.onPrimary)
        assertEquals(Color(0xFFE0E0E0), colors.surface)
        assertEquals(Color(0xFFF0F0F0), colors.background)
        assertEquals(Color.White, colors.userMessageBackground)
        assertEquals(Color.Black, colors.userMessageText)
        assertEquals(Color(0xFFF5F5F5), colors.conciergeMessageBackground)
        assertEquals(Color(0xFF333333), colors.conciergeMessageText)
        assertEquals(Color(0xFF0066CC), colors.messageConciergeLink)
    }

    @Test
    fun `createColorsFromJson should handle button colors`() {
        val themeColors = ConciergeThemeColors(
            button = ConciergeButtonColors(
                primaryBackground = "#0066CC",
                primaryText = "#FFFFFF",
                primaryHover = "#0052A3",
                secondaryBorder = "#CCCCCC",
                secondaryText = "#333333",
                secondaryHover = "#EEEEEE",
                secondaryHoverText = "#000000",
                submitFill = "#00CC00",
                submitText = "#FFFFFF",
                disabledBackground = "#F0F0F0"
            )
        )

        val colors = ThemeParser.createColorsFromJson(themeColors, LightConciergeColors)
        
        assertEquals(Color(0xFF0066CC), colors.buttonPrimaryBackground)
        assertEquals(Color.White, colors.buttonPrimaryText)
        assertEquals(Color(0xFF0052A3), colors.buttonPrimaryHover)
        assertEquals(Color(0xFFCCCCCC), colors.buttonSecondaryBorder)
        assertEquals(Color(0xFF333333), colors.buttonSecondaryText)
        assertEquals(Color(0xFFEEEEEE), colors.buttonSecondaryHover)
        assertEquals(Color.Black, colors.buttonSecondaryHoverText)
        assertEquals(Color(0xFF00CC00), colors.buttonSubmitFill)
        assertEquals(Color.White, colors.buttonSubmitText)
        assertEquals(Color(0xFFF0F0F0), colors.buttonDisabled)
    }

    @Test
    fun `createColorsFromJson should handle input and feedback colors`() {
        val themeColors = ConciergeThemeColors(
            input = ConciergeInputColors(
                background = "#FFFFFF",
                text = "#000000",
                outline = "#CCCCCC",
                outlineFocus = "#0066CC"
            ),
            feedback = ConciergeFeedbackColors(
                iconButtonBackground = "#F0F0F0",
                iconButtonHoverBackground = "#E0E0E0"
            )
        )

        val colors = ThemeParser.createColorsFromJson(themeColors, LightConciergeColors)
        
        assertEquals(Color.White, colors.inputBackground)
        assertEquals(Color.Black, colors.inputText)
        assertEquals(Color(0xFFCCCCCC), colors.inputOutline)
        assertEquals(Color(0xFF0066CC), colors.inputOutlineFocus)
        assertEquals(Color(0xFFF0F0F0), colors.feedbackIconButtonBackground)
        assertEquals(Color(0xFFE0E0E0), colors.feedbackIconButtonHoverBackground)
    }

    @Test
    fun `createColorsFromJson should handle citation and disclaimer colors`() {
        val themeColors = ConciergeThemeColors(
            citation = ConciergeCitationColors(
                backgroundColor = "#FAFAFA",
                textColor = "#666666"
            ),
            disclaimer = "#999999"
        )

        val colors = ThemeParser.createColorsFromJson(themeColors, LightConciergeColors)
        
        assertEquals(Color(0xFFFAFAFA), colors.citationBackground)
        assertEquals(Color(0xFF666666), colors.citationText)
        assertEquals(Color(0xFF999999), colors.disclaimerColor)
    }

    @Test
    fun `createColorsFromJson should prioritize CSS colors over simple colors`() {
        val themeColors = ConciergeThemeColors(
            primary = "#00FF00",
            primaryColors = ConciergePrimaryColors(primary = "#FF0000")
        )

        val colors = ThemeParser.createColorsFromJson(themeColors, LightConciergeColors)
        
        // CSS color should take priority
        assertEquals(Color(0xFFFF0000), colors.primary)
    }

    @Test
    fun `createColorsFromJson should set micButtonColor from primaryColors text when provided`() {
        val themeColors = ConciergeThemeColors(
            primaryColors = ConciergePrimaryColors(primary = "#EB1000", text = "#00FF00")
        )
        val colors = ThemeParser.createColorsFromJson(themeColors, LightConciergeColors)
        assertEquals(Color(0xFF00FF00), colors.micButtonColor)
    }

    @Test
    fun `createColorsFromJson should fall back to default micButtonColor when primaryColors text is absent`() {
        val themeColors = ConciergeThemeColors(primary = "#EB1000")
        val defaultColors = DarkConciergeColors
        val colors = ThemeParser.createColorsFromJson(themeColors, defaultColors)
        assertEquals(defaultColors.micButtonColor, colors.micButtonColor)
    }

    @Test
    fun `parseThemeJson should parse complete real-world theme structure`() {
        val json = """
            {
              "metadata": {
                "brandName": "Concierge Demo",
                "version": "1.0.0",
                "language": "en-US",
                "namespace": "brand-concierge"
              },
              "behavior": {
                "multimodalCarousel": {
                  "cardClickAction": "openLink"
                },
                "input": {
                  "enableVoiceInput": true,
                  "disableMultiline": false,
                  "showAiChatIcon": null
                },
                "chat": {
                  "messageAlignment": "left",
                  "messageWidth": "100%"
                },
                "privacyNotice": {
                  "title": "Privacy Notice",
                  "text": "Privacy notice text."
                }
              },
              "disclaimer": {
                "text": "AI responses may be inaccurate. Check answers and sources. {Terms}",
                "links": [
                  {
                    "text": "Terms",
                    "url": "https://www.adobe.com/legal/licenses-terms/adobe-gen-ai-user-guidelines.html"
                  }
                ]
              },
              "text": {
                "welcome.heading": "Welcome to Brand Concierge!",
                "welcome.subheading": "I'm your personal guide to help you explore and find exactly what you need.",
                "input.placeholder": "How can I help?",
                "loading.message": "Generating response from our knowledge base",
                "feedback.dialog.title.positive": "Your feedback is appreciated",
                "feedback.dialog.title.negative": "Your feedback is appreciated",
                "feedback.dialog.question.positive": "What went well? Select all that apply.",
                "feedback.dialog.question.negative": "What went wrong? Select all that apply.",
                "feedback.dialog.notes": "Notes",
                "feedback.dialog.submit": "Submit",
                "feedback.dialog.cancel": "Cancel",
                "feedback.dialog.notes.placeholder": "Additional notes (optional)",
                "feedback.toast.success": "Thank you for the feedback.",
                "error.network": "I'm sorry, I'm having trouble connecting to our services right now."
              },
              "arrays": {
                "welcome.examples": [
                  {
                    "text": "I'd like to explore templates to see what I can create.",
                    "image": "https://example.com/image1.png",
                    "backgroundColor": "#F5F5F5"
                  },
                  {
                    "text": "I want to touch up and enhance my photos.",
                    "image": "https://example.com/image2.png",
                    "backgroundColor": "#F5F5F5"
                  }
                ],
                "feedback.positive.options": [
                  "Helpful and relevant recommendations",
                  "Clear and easy to understand",
                  "Friendly and conversational tone",
                  "Visually appealing presentation",
                  "Other"
                ],
                "feedback.negative.options": [
                  "Didn't understand my request",
                  "Unhelpful or irrelevant information",
                  "Too vague or lacking detail",
                  "Errors or poor quality response",
                  "Other"
                ]
              },
              "assets": {
                "icons": {
                  "company": "company-logo.svg"
                }
              },
              "theme": {
                "--welcome-input-order": "3",
                "--welcome-cards-order": "2",
                "--font-family": "Arial, sans-serif",
                "--color-primary": "#EB1000",
                "--color-text": "#131313",
                "--line-height-body": "1.75",
                "--main-container-background": "#FFFFFF",
                "--main-container-bottom-background": "#FFFFFF",
                "--message-blocker-background": "#FFFFFF",
                "--input-height-mobile": "52px",
                "--input-border-radius-mobile": "12px",
                "--input-background": "#FFFFFF",
                "--input-outline-width": "2px",
                "--input-box-shadow": "0 2px 8px 0 #00000014",
                "--input-focus-outline-width": "2px",
                "--input-focus-outline-color": "#4B75FF",
                "--input-font-size": "16px",
                "--input-text-color": "#292929",
                "--input-button-height": "32px",
                "--input-button-width": "32px",
                "--submit-button-fill-color": "#FFFFFF",
                "--submit-button-fill-color-disabled": "#C6C6C6",
                "--color-button-submit": "#292929",
                "--input-button-border-radius": "8px",
                "--button-disabled-background": "#FFFFFF",
                "--disclaimer-color": "#4B4B4B",
                "--disclaimer-font-size": "12px",
                "--disclaimer-font-weight": "400",
                "--message-user-background": "#EBEEFF",
                "--message-user-text": "#292929",
                "--message-border-radius": "10px",
                "--message-padding": "8px 16px",
                "--message-concierge-background": "#F5F5F5",
                "--message-concierge-text": "#292929",
                "--message-max-width": "100%",
                "--chat-interface-max-width": "768px",
                "--chat-history-padding": "16px",
                "--citations-desktop-button-font-size": "12px",
                "--feedback-icon-btn-background": "#FFFFFF",
                "--feedback-icon-btn-hover-background": "#FFFFFF",
                "--feedback-icon-btn-size-desktop": "32px",
                "--border-radius-card": "16px",
                "--button-primary-background": "#3B63FB",
                "--button-primary-text": "#FFFFFF",
                "--button-primary-hover": "#274DEA",
                "--button-secondary-border": "#2C2C2C",
                "--button-secondary-text": "#2C2C2C",
                "--message-concierge-link-color": "#274DEA"
              }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)
        
        // Verify metadata
        assertNotNull(config)
        assertEquals("Concierge Demo", config?.name)
        
        // Verify colors from theme block
        assertEquals("#EB1000", config?.colors?.primaryColors?.primary)
        assertEquals("#131313", config?.colors?.primaryColors?.text)
        assertEquals("#FFFFFF", config?.colors?.surfaceColors?.mainContainerBackground)
        assertEquals("#FFFFFF", config?.colors?.surfaceColors?.mainContainerBottomBackground)
        assertEquals("#EBEEFF", config?.colors?.message?.userBackground)
        assertEquals("#292929", config?.colors?.message?.userText)
        assertEquals("#F5F5F5", config?.colors?.message?.conciergeBackground)
        assertEquals("#292929", config?.colors?.message?.conciergeText)
        assertEquals("#274DEA", config?.colors?.message?.conciergeLink)
        
        // Verify button colors
        assertEquals("#3B63FB", config?.colors?.button?.primaryBackground)
        assertEquals("#FFFFFF", config?.colors?.button?.primaryText)
        assertEquals("#274DEA", config?.colors?.button?.primaryHover)
        assertEquals("#2C2C2C", config?.colors?.button?.secondaryBorder)
        assertEquals("#2C2C2C", config?.colors?.button?.secondaryText)
        
        // Verify input colors
        assertEquals("#FFFFFF", config?.colors?.input?.background)
        assertEquals("#292929", config?.colors?.input?.text)
        assertEquals("#4B75FF", config?.colors?.input?.outlineFocus)
        
        // Verify feedback colors
        assertEquals("#FFFFFF", config?.colors?.feedback?.iconButtonBackground)
        assertEquals("#FFFFFF", config?.colors?.feedback?.iconButtonHoverBackground)
        
        // Verify disclaimer
        assertEquals("AI responses may be inaccurate. Check answers and sources. {Terms}", config?.disclaimer?.text)
        assertEquals(1, config?.disclaimer?.links?.size)
        assertEquals("Terms", config?.disclaimer?.links?.get(0)?.text)
        assertEquals("https://www.adobe.com/legal/licenses-terms/adobe-gen-ai-user-guidelines.html", config?.disclaimer?.links?.get(0)?.url)
        
        // Verify text strings
        assertEquals("Welcome to Brand Concierge!", config?.text?.welcomeHeading)
        assertEquals("I'm your personal guide to help you explore and find exactly what you need.", config?.text?.welcomeSubheading)
        assertEquals("How can I help?", config?.text?.inputPlaceholder)
        assertEquals("Generating response from our knowledge base", config?.text?.loadingMessage)
        assertEquals("Your feedback is appreciated", config?.text?.feedbackDialogTitlePositive)
        assertEquals("Your feedback is appreciated", config?.text?.feedbackDialogTitleNegative)
        assertEquals("What went well? Select all that apply.", config?.text?.feedbackDialogQuestionPositive)
        assertEquals("What went wrong? Select all that apply.", config?.text?.feedbackDialogQuestionNegative)
        assertEquals("Notes", config?.text?.feedbackDialogNotes)
        assertEquals("Submit", config?.text?.feedbackDialogSubmit)
        assertEquals("Cancel", config?.text?.feedbackDialogCancel)
        assertEquals("Additional notes (optional)", config?.text?.feedbackDialogNotesPlaceholder)
        assertEquals("Thank you for the feedback.", config?.text?.feedbackToastSuccess)
        assertEquals("I'm sorry, I'm having trouble connecting to our services right now.", config?.text?.errorNetwork)
        
        // Verify welcome examples
        assertEquals(2, config?.welcomeExamples?.size)
        assertEquals("I'd like to explore templates to see what I can create.", config?.welcomeExamples?.get(0)?.text)
        assertEquals("https://example.com/image1.png", config?.welcomeExamples?.get(0)?.image)
        assertEquals("#F5F5F5", config?.welcomeExamples?.get(0)?.backgroundColor)
        assertEquals("I want to touch up and enhance my photos.", config?.welcomeExamples?.get(1)?.text)
        
        // Verify feedback options
        assertEquals(5, config?.feedbackPositiveOptions?.size)
        assertEquals("Helpful and relevant recommendations", config?.feedbackPositiveOptions?.get(0))
        assertEquals("Other", config?.feedbackPositiveOptions?.get(4))
        assertEquals(5, config?.feedbackNegativeOptions?.size)
        assertEquals("Didn't understand my request", config?.feedbackNegativeOptions?.get(0))
        assertEquals("Other", config?.feedbackNegativeOptions?.get(4))
        
        // Verify typography
        assertNotNull(config?.typography)
        assertEquals(16.0, config?.typography?.inputFontSize)
        assertEquals(12.0, config?.typography?.disclaimerFontSize)
        assertEquals(400, config?.typography?.disclaimerFontWeight)
        assertEquals(12.0, config?.typography?.citationsFontSize)
    }

    // -----------------------------------------------------------------------
    // Header text keys
    // -----------------------------------------------------------------------

    @Test
    fun `parseThemeJson should parse header title and subtitle`() {
        val json = """
            {
                "text": {
                    "header.title": "My Assistant",
                    "header.subtitle": "Powered by Adobe"
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)
        assertNotNull(config)
        assertEquals("My Assistant", config?.text?.headerTitle)
        assertEquals("Powered by Adobe", config?.text?.headerSubtitle)
    }

    @Test
    fun `parseThemeJson should return null header text when not provided`() {
        val json = """
            {
                "text": {
                    "input.placeholder": "Ask me anything"
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)
        assertNotNull(config)
        assertNull(config?.text?.headerTitle)
        assertNull(config?.text?.headerSubtitle)
    }

    // -----------------------------------------------------------------------
    // Welcome card behavior
    // -----------------------------------------------------------------------

    @Test
    fun `parseThemeJson should parse welcomeCard closeButtonAlignment`() {
        val json = """
            {
                "behavior": {
                    "welcomeCard": {
                        "closeButtonAlignment": "start"
                    }
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        assertNotNull(tokens)
        assertNotNull(tokens?.behavior?.welcomeCard)
        assertEquals("start", tokens?.behavior?.welcomeCard?.closeButtonAlignment)
    }

    @Test
    fun `parseThemeJson should default closeButtonAlignment to end`() {
        val json = """
            {
                "behavior": {
                    "welcomeCard": {}
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        assertNotNull(tokens)
        assertEquals("end", tokens?.behavior?.welcomeCard?.closeButtonAlignment)
    }

    @Test
    fun `parseThemeJson should return null welcomeCard when not provided`() {
        val json = """
            {
                "behavior": {
                    "input": {
                        "enableVoiceInput": true
                    }
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        assertNotNull(tokens)
        assertNull(tokens?.behavior?.welcomeCard)
    }

    // -----------------------------------------------------------------------
    // Welcome screen CSS layout keys
    // -----------------------------------------------------------------------

    @Test
    fun `parseThemeJson should parse welcome screen layout tokens`() {
        val json = """
            {
                "theme": {
                    "--header-title-font-size": "18px",
                    "--welcome-title-font-size": "16px",
                    "--welcome-text-align": "left",
                    "--welcome-content-padding": "16px",
                    "--welcome-prompt-image-size": "48px",
                    "--welcome-prompt-spacing": "6px",
                    "--welcome-title-bottom-spacing": "6px",
                    "--welcome-prompts-top-spacing": "12px"
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        assertNotNull(tokens)
        val layout = tokens?.cssLayout
        assertNotNull(layout)
        assertEquals(18.0, layout?.headerTitleFontSize)
        assertEquals(16.0, layout?.welcomeTitleFontSize)
        assertEquals("left", layout?.welcomeTextAlign)
        assertEquals(16.0, layout?.welcomeContentPadding)
        assertEquals(48.0, layout?.welcomePromptImageSize)
        assertEquals(6.0, layout?.welcomePromptSpacing)
        assertEquals(6.0, layout?.welcomeTitleBottomSpacing)
        assertEquals(12.0, layout?.welcomePromptsTopSpacing)
    }

    // -----------------------------------------------------------------------
    // Input icon color keys
    // -----------------------------------------------------------------------

    @Test
    fun `parseThemeJson should parse input icon colors`() {
        val json = """
            {
                "theme": {
                    "--input-send-icon-color": "#FFFFFF",
                    "--input-mic-icon-color": "#FF0000"
                }
            }
        """.trimIndent()

        val tokens = ThemeParser.parseThemeTokens(json)
        assertNotNull(tokens)
        assertNotNull(tokens?.colors?.input?.sendIconColor)
        assertNotNull(tokens?.colors?.input?.micIconColor)
    }

    @Test
    fun `createColorsFromJson should map send and mic icon colors`() {
        val themeColors = ConciergeThemeColors(
            input = ConciergeInputColors(
                sendIconColor = "#FFFFFF",
                micIconColor = "#FF0000"
            )
        )

        val colors = ThemeParser.createColorsFromJson(themeColors, LightConciergeColors)
        assertNotNull(colors.sendButtonColor)
        assertEquals(Color.White, colors.sendButtonColor)
    }
}

