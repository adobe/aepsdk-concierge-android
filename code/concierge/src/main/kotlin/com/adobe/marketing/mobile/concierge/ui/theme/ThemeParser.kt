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

import android.content.Context
import android.util.Log
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.util.DataReader
import com.adobe.marketing.mobile.util.JSONUtils
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Parser for JSON theme configuration files.
 */
internal object ThemeParser {
    private const val TAG = "ThemeParser"

    /**
     * Load theme configuration from a JSON file in assets
     * @param context Android context to access assets
     * @param fileName Name of the JSON file in assets folder (e.g., "themeDemo.json")
     * @return Parsed ConciergeThemeConfig or null if parsing fails
     */
    fun loadThemeFromAssets(context: Context, fileName: String): ConciergeThemeConfig? {
        return try {
            val jsonString = context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
            parseThemeJson(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load theme from assets: $fileName", e)
            null
        }
    }

    /**
     * Load theme configuration from a JSON string
     * Supports Concierge configuration with CSS variables in a "theme" block
     * @param jsonString JSON string containing theme configuration
     * @return Parsed ConciergeThemeConfig or null if parsing fails
     */
    fun parseThemeJson(jsonString: String): ConciergeThemeConfig? {
        return try {
            val jsonObject = JSONObject(jsonString)
            val json = JSONUtils.toMap(jsonObject) ?: return null
 
            // Parse the full theme (which includes CSS theme parsing)
            val themeTokens = parseThemeTokensFromMap(json)
            
            // Parse text strings
            val textMap = DataReader.optTypedMap(Any::class.java, json, "text", null)
            val textStrings = textMap?.let {
                ConciergeTextStrings(
                    inputPlaceholder = DataReader.optString(it, "input.placeholder", null),
                    welcomeHeading = DataReader.optString(it, "welcome.heading", null),
                    welcomeSubheading = DataReader.optString(it, "welcome.subheading", null),
                    loadingMessage = DataReader.optString(it, "loading.message", null),
                    feedbackDialogTitlePositive = DataReader.optString(it, "feedback.dialog.title.positive", null),
                    feedbackDialogTitleNegative = DataReader.optString(it, "feedback.dialog.title.negative", null),
                    feedbackDialogQuestionPositive = DataReader.optString(it, "feedback.dialog.question.positive", null),
                    feedbackDialogQuestionNegative = DataReader.optString(it, "feedback.dialog.question.negative", null),
                    feedbackDialogNotes = DataReader.optString(it, "feedback.dialog.notes", null),
                    feedbackDialogSubmit = DataReader.optString(it, "feedback.dialog.submit", null),
                    feedbackDialogCancel = DataReader.optString(it, "feedback.dialog.cancel", null),
                    feedbackDialogNotesPlaceholder = DataReader.optString(it, "feedback.dialog.notes.placeholder", null),
                    feedbackToastSuccess = DataReader.optString(it, "feedback.toast.success", null),
                    errorNetwork = DataReader.optString(it, "error.network", null)
                )
            }
            
            val disclaimer = parseDisclaimerFromMap(
                DataReader.optTypedMap(Any::class.java, json, "disclaimer", null)
            )

            // Parse welcome examples from arrays
            val welcomeExamples = parseArrayFromMap(json, "welcome.examples") { exampleMap ->
                val text = DataReader.optString(exampleMap, "text", null) ?: return@parseArrayFromMap null
                ConciergeWelcomeExample(
                    text = text,
                    image = DataReader.optString(exampleMap, "image", null),
                    backgroundColor = DataReader.optString(exampleMap, "backgroundColor", null)
                )
            }
            
            // Parse feedback options from arrays
            val feedbackPositiveOptions = parseStringArrayFromMap(json, "feedback.positive.options")
            val feedbackNegativeOptions = parseStringArrayFromMap(json, "feedback.negative.options")
            
            // Extract typography data from ConciergeThemeTokens
            val typography = themeTokens.cssLayout?.let { cssLayout ->
                ConciergeTypographyConfig(
                    inputFontSize = cssLayout.inputFontSize,
                    disclaimerFontSize = cssLayout.disclaimerFontSize,
                    disclaimerFontWeight = cssLayout.disclaimerFontWeight,
                    citationsFontSize = cssLayout.citationsDesktopButtonFontSize
                )
            }
            
            // Convert to ConciergeThemeConfig
            ConciergeThemeConfig(
                name = themeTokens.metadata.name,
                colors = themeTokens.colors,
                styles = null,
                text = textStrings,
                disclaimer = disclaimer,
                welcomeExamples = welcomeExamples,
                feedbackPositiveOptions = feedbackPositiveOptions,
                feedbackNegativeOptions = feedbackNegativeOptions,
                typography = typography
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse theme JSON", e)
            null
        }
    }

    /**
     * Load theme configuration with metadata, behavior, assets, content, and layout
     * All Concierge configurations contain CSS variables within a "theme" block
     * @param jsonString JSON string containing theme configuration
     * @return Parsed ConciergeThemeTokens or null if parsing fails
     */
    fun parseThemeTokens(jsonString: String): ConciergeThemeTokens? {
        return try {
            val jsonObject = JSONObject(jsonString)
            val json = JSONUtils.toMap(jsonObject) ?: return null
            parseThemeTokensFromMap(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse theme JSON", e)
            null
        }
    }
    
    /**
     * Parses theme tokens from a map
     * Extracts metadata, behavior, assets, content, layout from top level
     * Parses CSS variables from the "theme" block
     */
    private fun parseThemeTokensFromMap(json: Map<*, *>): ConciergeThemeTokens {
        // Parse standard configuration blocks
        var theme = ConciergeThemeTokens(
            metadata = parseMetadata(json["metadata"] as? Map<*, *>),
            behavior = parseBehavior(json["behavior"] as? Map<*, *>),
            assets = parseAssets(json["assets"] as? Map<*, *>),
            content = parseContent(json["content"] as? Map<*, *>),
            layout = parseLayout(json["layout"] as? Map<*, *>)
        )
        
        // Parse CSS variables from "theme" block
        val themeBlock = json["theme"] as? Map<*, *>
        if (themeBlock != null) {
            theme = parseCSSThemeBlock(themeBlock, theme)
        }
        
        return theme
    }
    
    /**
     * Parses CSS variables from theme block and applies them to the theme
     * @param themeBlock Map containing CSS variable key-value pairs
     * @param baseTheme The base theme to apply CSS variables to
     * @return Updated theme with CSS variables applied
     */
    private fun parseCSSThemeBlock(themeBlock: Map<*, *>, baseTheme: ConciergeThemeTokens): ConciergeThemeTokens {
        var theme = baseTheme
        
        // Apply each CSS variable to the theme
        themeBlock.forEach { (key, value) ->
            val cssKey = key as? String ?: return@forEach
            val cssValue = value as? String ?: return@forEach
            
            theme = CSSKeyMapper.apply(cssKey, cssValue, theme)
        }
        
        return theme
    }

    /**
     * Parses disclaimer config from the "disclaimer" map. Uses default text and default Terms link
     * when missing.
     */
    private fun parseDisclaimerFromMap(map: Map<*, *>?): DisclaimerConfig? {
        if (map == null) return null
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as Map<String, Any?>
        val disclaimerText = DataReader.optString(typedMap, "text", null)
        val linksList = DataReader.optTypedListOfMap(Any::class.java, typedMap, "links", null)
        val parsedLinks = linksList?.mapNotNull { linkMap ->
            val text = DataReader.optString(linkMap, "text", null)
            val url = DataReader.optString(linkMap, "url", null)
            if (text != null && url != null) DisclaimerLink(text, url) else null
        } ?: emptyList()
        val links = if (parsedLinks.isEmpty()) {
            listOf(
                DisclaimerLink(
                    ConciergeConstants.Disclaimer.DEFAULT_TERMS_LABEL,
                    ConciergeConstants.Disclaimer.DEFAULT_TERMS_URL
                )
            )
        } else {
            parsedLinks
        }
        return DisclaimerConfig(text = disclaimerText, links = links)
    }

    /**
     * Parses an array of objects from a nested "arrays" section.
     */
    private fun <T> parseArrayFromMap(
        json: Map<String, Any?>,
        arrayKey: String,
        mapper: (Map<String, Any?>) -> T?
    ): List<T>? {
        val arraysMap = DataReader.optTypedMap(Any::class.java, json, "arrays", null) ?: return null
        val array = DataReader.optTypedListOfMap(Any::class.java, arraysMap, arrayKey, null) ?: return null
        return array.mapNotNull(mapper)
    }
    
    /**
     * Parses a simple string array from a nested "arrays" section.
     */
    private fun parseStringArrayFromMap(json: Map<String, Any?>, arrayKey: String): List<String>? {
        val arraysMap = DataReader.optTypedMap(Any::class.java, json, "arrays", null) ?: return null
        val array = DataReader.optTypedList(String::class.java, arraysMap, arrayKey, null)
        return array?.filterNotNull()
    }

    /**
     * Create ConciergeColors from theme config, falling back to defaults for missing values
     * Handles both simple color properties and CSS theme structure
     */
    fun createColorsFromJson(
        themeColors: ConciergeThemeColors?,
        defaultColors: ConciergeColors
    ): ConciergeColors {
        if (themeColors == null) {
            return defaultColors
        }

        // Try to use CSS color structure first (from CSS themes)
        val primary = themeColors.primaryColors?.primary?.toComposeColor()
            ?: themeColors.primary?.toComposeColor()
            ?: defaultColors.primary
            
        val onPrimary = themeColors.primaryColors?.text?.toComposeColor()
            ?: themeColors.onPrimary?.toComposeColor()
            ?: defaultColors.onPrimary
        
        // Main chat screen background
        val background = themeColors.surfaceColors?.mainContainerBackground?.toComposeColor()
            ?: themeColors.background?.toComposeColor()
            ?: defaultColors.background
        
        // Surface/bottom container (input area)
        val surface = themeColors.surfaceColors?.mainContainerBottomBackground?.toComposeColor()
            ?: themeColors.surface?.toComposeColor()
            ?: defaultColors.surface
        
        // Use theme text color for onSurface when not explicitly set, so --color-text drives all body text (welcome card, prompts, input)
        val onSurface = themeColors.onSurface?.toComposeColor()
            ?: themeColors.primaryColors?.text?.toComposeColor()
            ?: defaultColors.onSurface

        val result = ConciergeColors(
            primary = primary,
            onPrimary = onPrimary,
            secondary = themeColors.secondary?.toComposeColor()
                ?: defaultColors.secondary,
            surface = surface,
            onSurface = onSurface,
            onSurfaceVariant = themeColors.onSurfaceVariant?.toComposeColor()
                ?: themeColors.primaryColors?.text?.toComposeColor()
                ?: defaultColors.onSurfaceVariant,
            background = background,
            container = themeColors.container?.toComposeColor()
                ?: defaultColors.container,
            outline = themeColors.outline?.toComposeColor()
                ?: defaultColors.outline,
            error = themeColors.error?.toComposeColor() 
                ?: defaultColors.error,
            onError = themeColors.onError?.toComposeColor() 
                ?: defaultColors.onError,
            // Message-specific colors from CSS themes. When not set by theme, use defaults so message text
            // contrasts with bubble background (e.g. dark text on white concierge card) instead of inheriting --color-text.
            userMessageBackground = themeColors.message?.userBackground?.toComposeColor(),
            userMessageText = themeColors.message?.userText?.toComposeColor() ?: defaultColors.userMessageText,
            conciergeMessageBackground = themeColors.message?.conciergeBackground?.toComposeColor(),
            conciergeMessageText = themeColors.message?.conciergeText?.toComposeColor() ?: defaultColors.conciergeMessageText,
            messageConciergeLink = themeColors.message?.conciergeLink?.toComposeColor(),
            // Button-specific colors from CSS themes
            buttonPrimaryBackground = themeColors.button?.primaryBackground?.toComposeColor(),
            buttonPrimaryText = themeColors.button?.primaryText?.toComposeColor(),
            buttonPrimaryHover = themeColors.button?.primaryHover?.toComposeColor(),
            buttonSecondaryBorder = themeColors.button?.secondaryBorder?.toComposeColor(),
            buttonSecondaryText = themeColors.button?.secondaryText?.toComposeColor(),
            buttonSecondaryHover = themeColors.button?.secondaryHover?.toComposeColor(),
            buttonSecondaryHoverText = themeColors.button?.secondaryHoverText?.toComposeColor(),
            buttonSubmitFill = themeColors.button?.submitFill?.toComposeColor(),
            buttonSubmitText = themeColors.button?.submitText?.toComposeColor(),
            buttonDisabled = themeColors.button?.disabledBackground?.toComposeColor(),
            // Input-specific colors from CSS themes
            inputBackground = themeColors.input?.background?.toComposeColor(),
            inputText = themeColors.input?.text?.toComposeColor(),
            inputOutline = themeColors.input?.outline?.toComposeColor(),
            inputOutlineFocus = themeColors.input?.outlineFocus?.toComposeColor(),
            micButtonColor = themeColors.primaryColors?.text?.toComposeColor() ?: defaultColors.micButtonColor,
            sendIconColor = themeColors.input?.sendIconColor?.toComposeColor(),
            sendArrowIconColor = themeColors.input?.sendArrowIconColor?.toComposeColor(),
            sendArrowBackgroundColor = themeColors.input?.sendArrowBackgroundColor?.toComposeColor(),
            micIconColor = themeColors.input?.micIconColor?.toComposeColor(),
            micRecordingIconColor = themeColors.input?.micRecordingIconColor?.toComposeColor(),
            // Feedback-specific colors from CSS themes
            feedbackIconButtonBackground = themeColors.feedback?.iconButtonBackground?.toComposeColor(),
            feedbackIconButtonHoverBackground = themeColors.feedback?.iconButtonHoverBackground?.toComposeColor(),
            // Citation/Disclaimer colors from CSS themes
            citationBackground = themeColors.citation?.backgroundColor?.toComposeColor(),
            citationText = themeColors.citation?.textColor?.toComposeColor(),
            disclaimerColor = themeColors.disclaimer?.toComposeColor()
        )
        
        return result
    }

    // Enhanced theme parsers

    private fun parseMetadata(map: Map<*, *>?): ConciergeThemeMetadata {
        if (map == null) return ConciergeThemeMetadata()
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as? MutableMap<String?, Any?>

        val name = DataReader.optString(typedMap, "name", null) 
            ?: DataReader.optString(typedMap, "brandName", "Brand Name")
        
        return ConciergeThemeMetadata(
            version = DataReader.optString(typedMap, "version", "1.0.0"),
            name = name,
            description = DataReader.optString(typedMap, "description", null),
            author = DataReader.optString(typedMap, "author", null),
            lastModified = DataReader.optString(typedMap, "lastModified", null)
        )
    }

    private fun parseBehavior(map: Map<*, *>?): ConciergeThemeBehavior {
        if (map == null) return ConciergeThemeBehavior()
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as? MutableMap<String?, Any?>
        val inputMap = typedMap?.get("input") as? Map<*, *>
        @Suppress("UNCHECKED_CAST")
        val inputTypedMap = inputMap as? MutableMap<String?, Any?>
        val enableVoiceInput = if (inputTypedMap != null) {
            DataReader.optBoolean(inputTypedMap, "enableVoiceInput", true)
        } else {
            // Default to false if not specified
            false
        }
        val disableMultiline = DataReader.optBoolean(inputTypedMap, "disableMultiline", false)
        
        val productCardMap = typedMap?.get("productCard") as? Map<*, *>
        @Suppress("UNCHECKED_CAST")
        val productCardTyped = productCardMap as? MutableMap<String?, Any?>
        val productCard = productCardTyped?.let {
            ConciergeProductCardBehavior(
                cardStyle = ProductCardStyle.fromString(DataReader.optString(it, "cardStyle", "actionButton"))
            )
        }

        val carouselMap = typedMap?.get("multimodalCarousel") as? Map<*, *>
        @Suppress("UNCHECKED_CAST")
        val carouselTyped = carouselMap as? MutableMap<String?, Any?>
        val multimodalCarousel = carouselTyped?.let {
            ConciergeMultimodalCarouselBehavior(
                carouselStyle = CarouselStyle.fromString(DataReader.optString(it, "carouselStyle", "paged"))
            )
        }

        return ConciergeThemeBehavior(
            enableDarkMode = DataReader.optBoolean(typedMap, "enableDarkMode", true),
            enableAnimations = DataReader.optBoolean(typedMap, "enableAnimations", true),
            enableHaptics = DataReader.optBoolean(typedMap, "enableHaptics", true),
            enableSoundEffects = DataReader.optBoolean(typedMap, "enableSoundEffects", false),
            autoScrollToBottom = DataReader.optBoolean(typedMap, "autoScrollToBottom", true),
            showTimestamps = DataReader.optBoolean(typedMap, "showTimestamps", false),
            enableMarkdown = DataReader.optBoolean(typedMap, "enableMarkdown", true),
            enableCitations = DataReader.optBoolean(typedMap, "enableCitations", true),
            enableVoiceInput = enableVoiceInput,
            disableMultiline = disableMultiline,
            sendButtonStyle = DataReader.optString(inputTypedMap, "sendButtonStyle", "default") ?: "default",
            maxMessageLength = DataReader.optInt(typedMap, "maxMessageLength", 2000),
            typingIndicatorDelay = DataReader.optInt(typedMap, "typingIndicatorDelay", 500),
            productCard = productCard,
            multimodalCarousel = multimodalCarousel
        )
    }

    private fun parseAssets(map: Map<*, *>?): ConciergeThemeAssets {
        if (map == null) return ConciergeThemeAssets()
        return ConciergeThemeAssets(
            icons = parseIconAssets(map["icons"] as? Map<*, *>),
            images = parseImageAssets(map["images"] as? Map<*, *>),
            fonts = parseFontAssets(map["fonts"] as? Map<*, *>)
        )
    }

    private fun parseIconAssets(map: Map<*, *>?): ConciergeIconAssets {
        if (map == null) return ConciergeIconAssets()
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as? MutableMap<String?, Any?>
        return ConciergeIconAssets(
            company = DataReader.optString(typedMap, "company", null),
            send = DataReader.optString(typedMap, "send", null),
            microphone = DataReader.optString(typedMap, "microphone", null),
            close = DataReader.optString(typedMap, "close", null),
            thumbsUp = DataReader.optString(typedMap, "thumbsUp", null),
            thumbsDown = DataReader.optString(typedMap, "thumbsDown", null),
            chevronDown = DataReader.optString(typedMap, "chevronDown", null),
            chevronRight = DataReader.optString(typedMap, "chevronRight", null)
        )
    }

    private fun parseImageAssets(map: Map<*, *>?): ConciergeImageAssets {
        if (map == null) return ConciergeImageAssets()
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as? MutableMap<String?, Any?>
        return ConciergeImageAssets(
            welcomeBanner = DataReader.optString(typedMap, "welcomeBanner", null),
            errorPlaceholder = DataReader.optString(typedMap, "errorPlaceholder", null),
            avatarBot = DataReader.optString(typedMap, "avatarBot", null),
            avatarUser = DataReader.optString(typedMap, "avatarUser", null)
        )
    }

    private fun parseFontAssets(map: Map<*, *>?): ConciergeThemeFonts {
        if (map == null) return ConciergeThemeFonts()
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as? MutableMap<String?, Any?>
        return ConciergeThemeFonts(
            regular = DataReader.optString(typedMap, "regular", null),
            medium = DataReader.optString(typedMap, "medium", null),
            bold = DataReader.optString(typedMap, "bold", null),
            light = DataReader.optString(typedMap, "light", null)
        )
    }

    private fun parseContent(map: Map<*, *>?): ConciergeThemeContent {
        if (map == null) return ConciergeThemeContent()
        return ConciergeThemeContent(
            text = parseTextContent(map["text"] as? Map<*, *>),
            placeholders = parsePlaceholderContent(map["placeholders"] as? Map<*, *>),
            accessibility = parseAccessibilityContent(map["accessibility"] as? Map<*, *>)
        )
    }

    private fun parseTextContent(map: Map<*, *>?): ConciergeTextContent {
        if (map == null) return ConciergeTextContent()
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as? MutableMap<String?, Any?>
        return ConciergeTextContent(
            welcomeTitle = DataReader.optString(typedMap, "welcomeTitle", "How can I help you?"),
            welcomeSubtitle = DataReader.optString(typedMap, "welcomeSubtitle", null),
            disclaimerText = DataReader.optString(typedMap, "disclaimerText", null),
            errorTitle = DataReader.optString(typedMap, "errorTitle", "Something went wrong"),
            errorRetry = DataReader.optString(typedMap, "errorRetry", "Try again"),
            feedbackTitle = DataReader.optString(typedMap, "feedbackTitle", "Provide feedback"),
            feedbackSubmit = DataReader.optString(typedMap, "feedbackSubmit", "Submit"),
            feedbackCancel = DataReader.optString(typedMap, "feedbackCancel", "Cancel"),
            sourcesLabel = DataReader.optString(typedMap, "sourcesLabel", "Sources"),
            thinkingLabel = DataReader.optString(typedMap, "thinkingLabel", "Thinking"),
            listeningLabel = DataReader.optString(typedMap, "listeningLabel", "Listening")
        )
    }

    private fun parsePlaceholderContent(map: Map<*, *>?): ConciergePlaceholderContent {
        if (map == null) return ConciergePlaceholderContent()
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as? MutableMap<String?, Any?>
        return ConciergePlaceholderContent(
            inputPlaceholder = DataReader.optString(typedMap, "inputPlaceholder", "How can I help"),
            listeningPlaceholder = DataReader.optString(typedMap, "listeningPlaceholder", "Listening..."),
            emptyStateMessage = DataReader.optString(typedMap, "emptyStateMessage", null)
        )
    }

    private fun parseAccessibilityContent(map: Map<*, *>?): ConciergeAccessibilityContent {
        if (map == null) return ConciergeAccessibilityContent()
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as? MutableMap<String?, Any?>
        return ConciergeAccessibilityContent(
            sendButtonLabel = DataReader.optString(typedMap, "sendButtonLabel", "Send message"),
            micButtonLabel = DataReader.optString(typedMap, "micButtonLabel", "Voice input"),
            closeButtonLabel = DataReader.optString(typedMap, "closeButtonLabel", "Close"),
            thumbsUpLabel = DataReader.optString(typedMap, "thumbsUpLabel", "Like this response"),
            thumbsDownLabel = DataReader.optString(typedMap, "thumbsDownLabel", "Dislike this response")
        )
    }

    private fun parseLayout(map: Map<*, *>?): ConciergeThemeLayout {
        if (map == null) return ConciergeThemeLayout()
        return ConciergeThemeLayout(
            spacing = parseSpacingLayout(map["spacing"] as? Map<*, *>),
            sizing = parseSizingLayout(map["sizing"] as? Map<*, *>),
            positioning = parsePositioningLayout(map["positioning"] as? Map<*, *>)
        )
    }

    private fun parseSpacingLayout(map: Map<*, *>?): ConciergeSpacingLayout {
        if (map == null) return ConciergeSpacingLayout()
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as? MutableMap<String?, Any?>
        return ConciergeSpacingLayout(
            xs = DataReader.optDouble(typedMap, "xs", 4.0),
            sm = DataReader.optDouble(typedMap, "sm", 8.0),
            md = DataReader.optDouble(typedMap, "md", 16.0),
            lg = DataReader.optDouble(typedMap, "lg", 24.0),
            xl = DataReader.optDouble(typedMap, "xl", 32.0),
            xxl = DataReader.optDouble(typedMap, "xxl", 48.0)
        )
    }

    private fun parseSizingLayout(map: Map<*, *>?): ConciergeSizingLayout {
        if (map == null) return ConciergeSizingLayout()
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as? MutableMap<String?, Any?>
        return ConciergeSizingLayout(
            iconSm = DataReader.optDouble(typedMap, "iconSm", 16.0),
            iconMd = DataReader.optDouble(typedMap, "iconMd", 24.0),
            iconLg = DataReader.optDouble(typedMap, "iconLg", 32.0),
            avatarSm = DataReader.optDouble(typedMap, "avatarSm", 32.0),
            avatarMd = DataReader.optDouble(typedMap, "avatarMd", 40.0),
            avatarLg = DataReader.optDouble(typedMap, "avatarLg", 48.0),
            buttonHeightSm = DataReader.optDouble(typedMap, "buttonHeightSm", 32.0),
            buttonHeightMd = DataReader.optDouble(typedMap, "buttonHeightMd", 40.0),
            buttonHeightLg = DataReader.optDouble(typedMap, "buttonHeightLg", 48.0)
        )
    }

    private fun parsePositioningLayout(map: Map<*, *>?): ConciergePositioningLayout {
        if (map == null) return ConciergePositioningLayout()
        @Suppress("UNCHECKED_CAST")
        val typedMap = map as? MutableMap<String?, Any?>
        return ConciergePositioningLayout(
            headerHeight = DataReader.optDouble(typedMap, "headerHeight", 56.0),
            footerHeight = DataReader.optDouble(typedMap, "footerHeight", 72.0),
            maxContentWidth = DataReader.optDouble(typedMap, "maxContentWidth", 800.0),
            minContentWidth = DataReader.optDouble(typedMap, "minContentWidth", 280.0)
        )
    }
}
