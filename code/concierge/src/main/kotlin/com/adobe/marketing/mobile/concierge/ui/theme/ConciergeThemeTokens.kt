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

/**
 * Enhanced theme configuration.
 * Includes metadata, behavior, assets, and content configuration.
 */
data class ConciergeThemeTokens(
    val metadata: ConciergeThemeMetadata = ConciergeThemeMetadata(),
    val behavior: ConciergeThemeBehavior = ConciergeThemeBehavior(),
    val colors: ConciergeThemeColors? = null,
    val assets: ConciergeThemeAssets = ConciergeThemeAssets(),
    val content: ConciergeThemeContent = ConciergeThemeContent(),
    val layout: ConciergeThemeLayout = ConciergeThemeLayout(),
    val cssLayout: ConciergeLayout? = null,
    val typography: ConciergeTypography? = null,
    val components: ConciergeComponentsConfig? = null,
    val cssVariables: Map<String, String> = emptyMap()
)

/**
 * Typography configuration for fonts and text styling
 */
data class ConciergeTypography(
    val fontFamily: String? = null,
    val lineHeight: Double? = null
)

/**
 * Layout configuration matching CSS theme structure
 */
data class ConciergeLayout(
    // Input layout
    val inputHeight: Double? = null,
    val inputBorderRadius: Double? = null,
    val inputOutlineWidth: Double? = null,
    val inputFocusOutlineWidth: Double? = null,
    val inputFontSize: Double? = null,
    val inputButtonHeight: Double? = null,
    val inputButtonWidth: Double? = null,
    val inputButtonBorderRadius: Double? = null,
    val inputBoxShadow: Map<String, Any>? = null,
    
    // Message layout
    val messageBorderRadius: Double? = null,
    val messagePadding: List<Double>? = null,
    val messageMaxWidth: Double? = null,
    
    // Chat layout
    val chatInterfaceMaxWidth: Double? = null,
    val chatHistoryPadding: Double? = null,
    val chatHistoryPaddingTopExpanded: Double? = null,
    val chatHistoryBottomPadding: Double? = null,
    val messageBlockerHeight: Double? = null,
    
    // Card layout
    val borderRadiusCard: Double? = null,
    val multimodalCardBoxShadow: Map<String, Any>? = null,
    
    // Button layout
    val buttonHeightSmall: Double? = null,
    
    // Feedback layout
    val feedbackContainerGap: Double? = null,
    
    // Citations layout
    val citationsTextFontWeight: Int? = null,
    val citationsDesktopButtonFontSize: Double? = null,
    
    // Disclaimer layout
    val disclaimerFontSize: Double? = null,
    val disclaimerFontWeight: Int? = null,
    
    // Welcome layout
    val welcomeInputOrder: Int? = null,
    val welcomeCardsOrder: Int? = null,
    val welcomeTitleFontSize: Double? = null,
    val welcomeTextAlign: String? = null,
    val welcomeContentPadding: Double? = null,
    val welcomePromptImageSize: Double? = null,
    val welcomePromptSpacing: Double? = null,
    val welcomeTitleBottomSpacing: Double? = null,
    val welcomePromptsTopSpacing: Double? = null,
    val welcomePromptPadding: Double? = null,
    val welcomePromptCornerRadius: Double? = null,
    val headerTitleFontSize: Double? = null,

    // Extended product cards
    val productCardTitleFontWeight: Int? = null,
    val productCardTitleFontSize: Double? = null,
    val productCardSubtitleFontWeight: Int? = null,
    val productCardSubtitleFontSize: Double? = null,
    val productCardPriceFontWeight: Int? = null,
    val productCardPriceFontSize: Double? = null,
    val productCardBadgeFontSize: Double? = null,
    val productCardBadgeFontWeight: Int? = null,
    val productCardBadgeTextColor: String? = null,
    val productCardBadgeBackgroundColor: String? = null,
    val productCardBackgroundColor: String? = null,
    val productCardTitleColor: String? = null,
    val productCardSubtitleColor: String? = null,
    val productCardPriceColor: String? = null,
    val productCardOutlineColor: String? = null,
    val productCardWidth: Double? = null,
    val productCardHeight: Double? = null,
    val productCardBorderRadius: Double? = null,
    val productCardWasPriceColor: String? = null,
    val productCardWasPriceFontSize: Double? = null,
    val productCardWasPriceFontWeight: Int? = null,
    val productCardWasPriceTextPrefix: String? = null,
    val productCardTextSpacing: Double? = null,
    val productCardTextTopPadding: Double? = null,
    val productCardTextBottomPadding: Double? = null,
    val productCardTextHorizontalPadding: Double? = null,
    val productCardCarouselHorizontalPadding: Double? = null,
    val productCardCarouselSpacing: Double? = null,

    // Nested layout for hierarchical themes
    val spacing: ConciergeSpacingLayout? = null,
    val sizing: ConciergeSizingLayout? = null,
    val positioning: ConciergePositioningLayout? = null
)

/**
 * Components configuration
 */
data class ConciergeComponentsConfig(
    val feedback: ConciergeFeedbackComponent? = null
)

data class ConciergeFeedbackComponent(
    val iconButtonSizeDesktop: Double? = null
)

/**
 * Theme metadata - version, name, description
 */
data class ConciergeThemeMetadata(
    val version: String = "1.0.0",
    val name: String = "Default Theme",
    val description: String? = null,
    val author: String? = null,
    val lastModified: String? = null
)

/**
 * Theme behavior configuration - feature flags and settings
 */
data class ConciergeThemeBehavior(
    val enableDarkMode: Boolean = true,
    val enableAnimations: Boolean = true,
    val enableHaptics: Boolean = true,
    val enableSoundEffects: Boolean = false,
    val autoScrollToBottom: Boolean = true,
    val showTimestamps: Boolean = false,
    val enableMarkdown: Boolean = true,
    val enableCitations: Boolean = true,
    val welcomeCard: ConciergeWelcomeCardBehavior? = null,
    val enableVoiceInput: Boolean = true,
    val sendButtonStyle: String = "default",
    val maxMessageLength: Int = 2000,
    val typingIndicatorDelay: Int = 500,
    val feedback: ConciergeFeedbackBehavior? = null,
    val citations: ConciergeCitationsBehavior? = null,
    val productCard: ConciergeProductCardBehavior? = null,
    val multimodalCarousel: ConciergeMultimodalCarouselBehavior? = null
)

/**
 * Display mode for the feedback dialog.
 * CARD renders inline as a Card overlay; BOTTOM_SHEET renders as a ModalBottomSheet.
 */
enum class FeedbackDisplayMode(val value: String) {
    CARD("card"),
    BOTTOM_SHEET("modal");

    companion object {
        fun fromString(value: String): FeedbackDisplayMode =
            values().firstOrNull { it.value.equals(value, ignoreCase = true) } ?: CARD
    }
}

/**
 * Product card behavior: ACTION_BUTTON = image overlay with action buttons,
 * PRODUCT_DETAIL = structured layout (image, badge, title, subtitle, price).
 */
enum class ProductCardStyle(val value: String) {
    ACTION_BUTTON("actionButton"),
    PRODUCT_DETAIL("productDetail");

    companion object {
        fun fromString(value: String): ProductCardStyle =
            values().find { it.value == value } ?: ACTION_BUTTON
    }
}

/**
 * Multimodal carousel behavior: PAGED = snap to item with prev/next and dots,
 * SCROLL = continuous horizontal scroll with no paging controls.
 */
enum class CarouselStyle(val value: String) {
    PAGED("paged"),
    SCROLL("scroll");

    companion object {
        fun fromString(value: String): CarouselStyle =
            values().find { it.value == value } ?: PAGED
    }
}

data class ConciergeFeedbackBehavior(
    val displayMode: FeedbackDisplayMode = FeedbackDisplayMode.CARD,
    val thumbsPlacement: FeedbackThumbsPlacement = FeedbackThumbsPlacement.INLINE
)

/**
 * Placement of the feedback thumbs relative to the sources accordion.
 * INLINE renders thumbs on the same row as the sources label (default).
 * BELOW renders thumbs on a separate row beneath the sources accordion with the feedback helpful label.
 */
enum class FeedbackThumbsPlacement(val value: String) {
    INLINE("inline"),
    BELOW("below");

    companion object {
        fun fromString(value: String): FeedbackThumbsPlacement =
            values().firstOrNull { it.value.equals(value, ignoreCase = true) } ?: INLINE
    }
}

data class ConciergeCitationsBehavior(
    val showLinkIcon: Boolean = false
)

data class ConciergeProductCardBehavior(
    val cardStyle: ProductCardStyle = ProductCardStyle.ACTION_BUTTON
)

data class ConciergeMultimodalCarouselBehavior(
    val carouselStyle: CarouselStyle = CarouselStyle.PAGED
)

data class ConciergeWelcomeCardBehavior(
    val closeButtonAlignment: String = "end",
    val promptFullWidth: Boolean = true,
    val promptMaxLines: Int = Int.MAX_VALUE,
    val contentAlignment: String = "top"
)

/**
 * Asset configuration - icons, images, fonts
 */
data class ConciergeThemeAssets(
    val icons: ConciergeIconAssets = ConciergeIconAssets(),
    val images: ConciergeImageAssets = ConciergeImageAssets(),
    val fonts: ConciergeThemeFonts = ConciergeThemeFonts()
)

data class ConciergeIconAssets(
    val company: String? = null,
    val send: String? = null,
    val microphone: String? = null,
    val close: String? = null,
    val thumbsUp: String? = null,
    val thumbsDown: String? = null,
    val chevronDown: String? = null,
    val chevronRight: String? = null
)

data class ConciergeImageAssets(
    val welcomeBanner: String? = null,
    val errorPlaceholder: String? = null,
    val avatarBot: String? = null,
    val avatarUser: String? = null
)

data class ConciergeThemeFonts(
    val regular: String? = null,
    val medium: String? = null,
    val bold: String? = null,
    val light: String? = null
)

/**
 * Content configuration - text strings and localization
 */
data class ConciergeThemeContent(
    val text: ConciergeTextContent = ConciergeTextContent(),
    val placeholders: ConciergePlaceholderContent = ConciergePlaceholderContent(),
    val accessibility: ConciergeAccessibilityContent = ConciergeAccessibilityContent()
)

data class ConciergeTextContent(
    val welcomeTitle: String = "How can I help you?",
    val welcomeSubtitle: String? = null,
    val disclaimerText: String? = null,
    val errorTitle: String = "Something went wrong",
    val errorRetry: String = "Try again",
    val feedbackTitle: String = "Provide feedback",
    val feedbackSubmit: String = "Submit",
    val feedbackCancel: String = "Cancel",
    val thinkingLabel: String = "Thinking",
    val listeningLabel: String = "Listening"
)

data class ConciergePlaceholderContent(
    val inputPlaceholder: String = "How can I help",
    val listeningPlaceholder: String = "Listening...",
    val emptyStateMessage: String? = null
)

data class ConciergeAccessibilityContent(
    val sendButtonLabel: String = "Send message",
    val micButtonLabel: String = "Voice input",
    val closeButtonLabel: String = "Close",
    val thumbsUpLabel: String = "Like this response",
    val thumbsDownLabel: String = "Dislike this response"
)

/**
 * Layout configuration - spacing, sizing, positioning for hierarchical themes
 */
data class ConciergeThemeLayout(
    val spacing: ConciergeSpacingLayout = ConciergeSpacingLayout(),
    val sizing: ConciergeSizingLayout = ConciergeSizingLayout(),
    val positioning: ConciergePositioningLayout = ConciergePositioningLayout()
)

data class ConciergeSpacingLayout(
    val xs: Double = 4.0,
    val sm: Double = 8.0,
    val md: Double = 16.0,
    val lg: Double = 24.0,
    val xl: Double = 32.0,
    val xxl: Double = 48.0
)

data class ConciergeSizingLayout(
    val iconSm: Double = 16.0,
    val iconMd: Double = 24.0,
    val iconLg: Double = 32.0,
    val avatarSm: Double = 32.0,
    val avatarMd: Double = 40.0,
    val avatarLg: Double = 48.0,
    val buttonHeightSm: Double = 32.0,
    val buttonHeightMd: Double = 40.0,
    val buttonHeightLg: Double = 48.0
)

data class ConciergePositioningLayout(
    val headerHeight: Double = 56.0,
    val footerHeight: Double = 72.0,
    val maxContentWidth: Double = 800.0,
    val minContentWidth: Double = 280.0
)


/**
 * CSS variable mapping for backwards compatibility with web-based themes
 */
internal object CSSVariableMapper {
    private val colorMappings = mapOf(
        "--color-primary" to "primary",
        "--color-on-primary" to "onPrimary",
        "--color-secondary" to "secondary",
        "--color-surface" to "surface",
        "--color-on-surface" to "onSurface",
        "--color-background" to "background",
        "--color-container" to "container",
        "--color-outline" to "outline",
        "--color-error" to "error",
        "--color-on-error" to "onError"
    )

    private val spacingMappings = mapOf(
        "--spacing-xs" to "xs",
        "--spacing-sm" to "sm",
        "--spacing-md" to "md",
        "--spacing-lg" to "lg",
        "--spacing-xl" to "xl",
        "--spacing-xxl" to "xxl"
    )

    /**
     * Convert CSS variable name to theme property name
     */
    fun mapCSSVariable(cssVar: String): String? {
        return colorMappings[cssVar] 
            ?: spacingMappings[cssVar]
            ?: cssVar.removePrefix("--").replace("-", "_")
    }

    /**
     * Parse CSS variable value (handles calc(), var(), etc.)
     */
    fun parseCSSValue(cssValue: String): String {
        // Remove CSS functions like calc(), var()
        var value = cssValue.trim()
        
        // Handle var(--variable-name)
        if (value.startsWith("var(")) {
            value = value.removePrefix("var(").removeSuffix(")").trim()
        }
        
        // Handle calc() - simple implementation
        if (value.startsWith("calc(")) {
            // For now, just remove calc() wrapper
            // A full implementation would evaluate the expression
            value = value.removePrefix("calc(").removeSuffix(")").trim()
        }
        
        return value
    }
}

