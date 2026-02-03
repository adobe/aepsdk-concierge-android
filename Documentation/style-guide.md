# Brand Concierge Style Guide

This document provides a comprehensive reference for all styling properties supported by the Brand Concierge Android SDK. Themes are configured using JSON files that follow a web-compatible CSS variable format.

> **⚠️ Implementation Status**: While the theme system parses all CSS variables for web compatibility, not all properties are currently used in the Android UI. See the [Implementation Status](#implementation-status) section for detailed information on which properties are actively rendered versus defined but unused.

## Table of Contents

- [Overview](#overview)
- [JSON Structure](#json-structure)
- [Value Formats](#value-formats)
- [Metadata](#metadata)
- [Behavior](#behavior)
- [Disclaimer](#disclaimer)
- [Text (Copy)](#text-copy)
- [Arrays](#arrays)
- [Assets](#assets)
- [Theme Tokens](#theme-tokens)
  - [Typography](#typography)
  - [Colors](#colors)
  - [Layout](#layout)
- [Implementation Status](#implementation-status)

---

## Overview

The theme configuration is loaded from a JSON file using `ConciergeThemeLoader.load(context, filename)`. The framework supports CSS-like variable names (prefixed with `--`) that are automatically mapped to native Kotlin properties.

### Loading a Theme

```kotlin
// Load from app assets
val theme = ConciergeThemeLoader.load(context, "theme-default")

// Use default theme
val defaultTheme = ConciergeThemeLoader.default()
```

### Applying a Theme

Apply the theme using the `ConciergeTheme` composable:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeLoader

@Composable
fun MyApp() {
    val context = LocalContext.current
    
    // Load theme from assets
    val theme = remember {
        ConciergeThemeLoader.load(context, "my-theme") 
            ?: ConciergeThemeLoader.default()
    }
    
    ConciergeTheme(theme = theme) {
        // Your app content here
        // Concierge UI components will use the theme
    }
}
```

You can also load themes by filename directly:

```kotlin
@Composable
fun MyApp() {
    ConciergeTheme(themeFileName = "my-theme.json") {
        // Your app content here
    }
}
```

### Applying a Theme in XML/Views

For XML-based apps using `ConciergeChatView`, pass the theme when binding:

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Load theme
        val theme = ConciergeThemeLoader.load(this, "my-theme")
        
        val chatView = findViewById<ConciergeChatView>(R.id.concierge_chat)
        chatView.bind(
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            theme = theme,
            onClose = { finish() }
        )
    }
}
```

> **Important:** The `ConciergeTheme` composable provides theme tokens to all child composables through CompositionLocal.

---

## JSON Structure

The theme JSON file contains these top-level keys:

| Key | Description |
|-----|-------------|
| `metadata` | Theme identification and versioning |
| `behavior` | Feature toggles and interaction settings |
| `disclaimer` | Legal/disclaimer text configuration |
| `text` | Localized UI strings (copy) |
| `arrays` | Welcome examples and feedback options |
| `assets` | Icon and image assets |
| `theme` | Visual styling tokens (CSS variables) |

---

## Value Formats

Understanding the value formats used throughout this document.

### Colors

Colors are specified as hex strings:

```json
"--color-primary": "#EB1000"
"--message-user-background": "#EBEEFF"
"--input-box-shadow": "0 2px 8px 0 #00000014"
```

Supported formats:
- `#RRGGBB` - 6 digit hex
- `#RRGGBBAA` - 8 digit hex with alpha

### Dimensions

Dimensions use CSS pixel units:

```json
"--input-height-mobile": "52px"
"--input-border-radius-mobile": "12px"
"--message-max-width": "100%"
```

### Padding

Padding follows CSS shorthand syntax:

```json
"--message-padding": "8px 16px"
```

Formats:
- `8px` - All sides
- `8px 16px` - Vertical, horizontal
- `8px 16px 4px` - Top, horizontal, bottom
- `8px 16px 4px 2px` - Top, right, bottom, left

### Shadows

Shadows use CSS box-shadow syntax:

```json
"--input-box-shadow": "0 2px 8px 0 #00000014"
"--multimodal-card-box-shadow": "none"
```

Format: `offsetX offsetY blurRadius spreadRadius color`

### Font Weights

Font weights use CSS numeric or named values:

| Value | Name | Android FontWeight |
|-------|------|--------------------|
| `100` | `ultraLight` | `W100` |
| `200` | `thin` | `W200` |
| `300` | `light` | `Light` |
| `400` / `normal` | `regular` | `Normal` |
| `500` | `medium` | `Medium` |
| `600` | `semibold` | `SemiBold` |
| `700` / `bold` | `bold` | `Bold` |
| `800` | `heavy` | `W800` |
| `900` | `black` | `Black` |

### Text Alignment

| Value | Compose Equivalent |
|-------|-------------------|
| `left` | `TextAlign.Start` |
| `center` | `TextAlign.Center` |
| `right` | `TextAlign.End` |

---

## Metadata

Theme identification information.

| JSON Key | Type | Default | Description |
|----------|------|---------|-------------|
| `metadata.brandName` | `String` | `""` | Brand/company name |
| `metadata.version` | `String` | `"0.0.0"` | Theme version |
| `metadata.language` | `String` | `"en-US"` | Locale identifier |
| `metadata.namespace` | `String` | `"brand-concierge"` | Theme namespace |

### Example

```json
{
  "metadata": {
    "brandName": "Concierge Demo",
    "version": "1.0.0",
    "language": "en-US",
    "namespace": "brand-concierge"
  }
}
```

---

## Behavior

Feature toggles and interaction configuration.

### Multimodal Carousel

| JSON Key | Type | Default | Description |
|----------|------|---------|-------------|
| `behavior.multimodalCarousel.cardClickAction` | `String` | `"openLink"` | Action when carousel card is tapped. Currently "openLink" is the only option available. |

### Input

| JSON Key | Type | Default | Description |
|----------|------|---------|-------------|
| `behavior.input.enableVoiceInput` | `Bool` | `false` | Enable voice input button |
| `behavior.input.disableMultiline` | `Bool` | `true` | Disable multiline text input |
| `behavior.input.showAiChatIcon` | `Object?` | `null` | AI chat icon configuration |

### Chat

| JSON Key | Type | Default | Description |
|----------|------|---------|-------------|
| `behavior.chat.messageAlignment` | `String` | `"left"` | Message alignment (`"left"`, `"center"`, `"right"`) |
| `behavior.chat.messageWidth` | `String` | `"100%"` | Max message width (e.g., `"100%"`, `"768px"`) |

### Privacy Notice

| JSON Key | Type | Default | Description |
|----------|------|---------|-------------|
| `behavior.privacyNotice.title` | `String` | `"Privacy Notice"` | Privacy dialog title |
| `behavior.privacyNotice.text` | `String` | `"Privacy notice text."` | Privacy notice content |

### Example

```json
{
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
  }
}
```

---

## Disclaimer

Legal disclaimer text with embedded links.

| JSON Key | Type | Default | Description |
|----------|------|---------|-------------|
| `disclaimer.text` | `String` | `"AI responses may be inaccurate..."` | Disclaimer text with `{placeholders}` for links |
| `disclaimer.links` | `Array` | `[]` | Array of link objects |
| `disclaimer.links[].text` | `String` | `""` | Link display text (matches placeholder) |
| `disclaimer.links[].url` | `String` | `""` | Link URL |

### Example

```json
{
  "disclaimer": {
    "text": "AI responses may be inaccurate. Check answers and sources. {Terms}",
    "links": [
      {
        "text": "Terms",
        "url": "https://www.adobe.com/legal/licenses-terms/adobe-gen-ai-user-guidelines.html"
      }
    ]
  }
}
```

---

## Text (Copy)

Localized UI strings using dot-notation keys.

### ✅ Content Recommendations

While there are no strict requirements for character limits in many of these text fields, it is **_strongly_** recommended that the values be tested on target device(s) prior to deployment, ensuring the UI renders as desired.

### Welcome Screen

| JSON Key | Default | Description |
|----------|---------|-------------|
| `text["welcome.heading"]` | `"Explore what you can do with Adobe apps."` | Welcome screen heading |
| `text["welcome.subheading"]` | `"Choose an option or tell us..."` | Welcome screen subheading |

### Input

| JSON Key | Default | Description |
|----------|---------|-------------|
| `text["input.placeholder"]` | `"Tell us what you'd like to do or create"` | Input field placeholder |
| `text["input.messageInput.aria"]` | `"Message input"` | Accessibility label for input |
| `text["input.send.aria"]` | `"Send message"` | Accessibility label for send button |
| `text["input.aiChatIcon.tooltip"]` | `"Ask AI"` | AI icon tooltip |
| `text["input.mic.aria"]` | `"Voice input"` | Accessibility label for mic button |

### Cards & Carousel

| JSON Key | Default | Description |
|----------|---------|-------------|
| `text["card.aria.select"]` | `"Select example message"` | Card selection accessibility |
| `text["carousel.prev.aria"]` | `"Previous cards"` | Previous button accessibility |
| `text["carousel.next.aria"]` | `"Next cards"` | Next button accessibility |

### System Messages

| JSON Key | Default | Description |
|----------|---------|-------------|
| `text["scroll.bottom.aria"]` | `"Scroll to bottom"` | Scroll button accessibility |
| `text["error.network"]` | `"I'm sorry, I'm having trouble..."` | Network error message |
| `text["loading.message"]` | `"Generating response from our knowledge base"` | Loading indicator text |

### Feedback Dialog

| JSON Key | Default | Description |
|----------|---------|-------------|
| `text["feedback.dialog.title.positive"]` | `"Your feedback is appreciated"` | Positive feedback dialog title |
| `text["feedback.dialog.title.negative"]` | `"Your feedback is appreciated"` | Negative feedback dialog title |
| `text["feedback.dialog.question.positive"]` | `"What went well? Select all that apply."` | Positive feedback question |
| `text["feedback.dialog.question.negative"]` | `"What went wrong? Select all that apply."` | Negative feedback question |
| `text["feedback.dialog.notes"]` | `"Notes"` | Notes section label |
| `text["feedback.dialog.submit"]` | `"Submit"` | Submit button text |
| `text["feedback.dialog.cancel"]` | `"Cancel"` | Cancel button text |
| `text["feedback.dialog.notes.placeholder"]` | `"Additional notes (optional)"` | Notes placeholder |
| `text["feedback.toast.success"]` | `"Thank you for the feedback."` | Success toast message |
| `text["feedback.thumbsUp.aria"]` | `"Thumbs up"` | Thumbs up accessibility |
| `text["feedback.thumbsDown.aria"]` | `"Thumbs down"` | Thumbs down accessibility |

### Example

```json
{
  "text": {
    "welcome.heading": "Welcome to Brand Concierge!",
    "welcome.subheading": "I'm your personal guide to help you explore.",
    "input.placeholder": "How can I help?",
    "error.network": "I'm sorry, I'm having trouble connecting."
  }
}
```

---

## Arrays

List-based configuration for examples and feedback options.

### Welcome Examples

> It is recommended to have no more than four items in your provided welcome examples.
>
> Always test your values on device to ensure the UI looks as desired.

| JSON Key | Type | Description |
|----------|------|-------------|
| `arrays["welcome.examples"]` | `Array` | Welcome screen example cards |
| `arrays["welcome.examples"][].text` | `String` | Card display text |
| `arrays["welcome.examples"][].image` | `String?` | Card image URL |
| `arrays["welcome.examples"][].backgroundColor` | `String?` | Card background color (hex) |

### Feedback Options

> It is recommended to have no more than five options available for feedback. 
>
> Always test your values on device to ensure the UI looks as desired.

| JSON Key | Type | Description |
|----------|------|-------------|
| `arrays["feedback.positive.options"]` | `Array<String>` | Positive feedback checkbox options |
| `arrays["feedback.negative.options"]` | `Array<String>` | Negative feedback checkbox options |

### Example

```json
{
  "arrays": {
    "welcome.examples": [
      {
        "text": "I'd like to explore templates to see what I can create.",
        "image": "https://example.com/template.png",
        "backgroundColor": "#F5F5F5"
      }
    ],
    "feedback.positive.options": [
      "Helpful and relevant recommendations",
      "Clear and easy to understand",
      "Other"
    ],
    "feedback.negative.options": [
      "Didn't understand my request",
      "Unhelpful or irrelevant information",
      "Other"
    ]
  }
}
```

---

## Assets

Icon and image asset configuration.

| JSON Key | Type | Default | Description |
|----------|------|---------|-------------|
| `assets.icons.company` | `String` | `""` | Company logo (SVG string or URL) |

### Example

```json
{
  "assets": {
    "icons": {
      "company": ""
    }
  }
}
```

---

## Theme Tokens

Visual styling using CSS-like variable names. All properties in the `theme` object use the `--property-name` format.

### Typography

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--font-family` | `typography.fontFamily` | `String` | `null` (system font) | Font family name |
| `--line-height-body` | `typography.lineHeight` | `Double` | `1.0` | Line height multiplier |

### Colors - Primary

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--color-primary` | `colors.primaryColors.primary` | `String` | `"#1976D2"` | Primary brand color (hex) |
| `--color-text` | `colors.primaryColors.text` | `String` | `"#000000"` | Primary text color (hex) |

### Colors - Surface

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--main-container-background` | `colors.surfaceColors.mainContainerBackground` | `String` | `"#FFFFFF"` | Main container background (hex) |
| `--main-container-bottom-background` | `colors.surfaceColors.mainContainerBottomBackground` | `String` | `"#FFFFFF"` | Bottom container background (hex) |
| `--message-blocker-background` | `colors.surfaceColors.messageBlockerBackground` | `String` | `"#FFFFFF"` | Message blocker overlay (hex) |

### Colors - Messages

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--message-user-background` | `colors.message.userBackground` | `String` | `"#E3F2FD"` | User message bubble background (hex) |
| `--message-user-text` | `colors.message.userText` | `String` | `"#000000"` | User message text color (hex) |
| `--message-concierge-background` | `colors.message.conciergeBackground` | `String` | `"#F5F5F5"` | AI message bubble background (hex) |
| `--message-concierge-text` | `colors.message.conciergeText` | `String` | `"#000000"` | AI message text color (hex) |
| `--message-concierge-link-color` | `colors.message.conciergeLink` | `String` | `"#1976D2"` | Link color in AI messages (hex) |

### Colors - Buttons

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--button-primary-background` | `colors.button.primaryBackground` | `String` | `"#1976D2"` | Primary button background (hex) |
| `--button-primary-text` | `colors.button.primaryText` | `String` | `"#FFFFFF"` | Primary button text (hex) |
| `--button-primary-hover` | `colors.button.primaryHover` | `String` | `"#1565C0"` | Primary button hover state (hex) |
| `--button-secondary-border` | `colors.button.secondaryBorder` | `String` | `"#1976D2"` | Secondary button border (hex) |
| `--button-secondary-text` | `colors.button.secondaryText` | `String` | `"#1976D2"` | Secondary button text (hex) |
| `--button-secondary-hover` | `colors.button.secondaryHover` | `String` | `"#E3F2FD"` | Secondary button hover state (hex) |
| `--color-button-secondary-hover-text` | `colors.button.secondaryHoverText` | `String` | `"#1976D2"` | Secondary button hover text (hex) |
| `--submit-button-fill-color` | `colors.button.submitFill` | `String` | `"#FFFFFF"` | Submit button fill (hex) |
| `--submit-button-fill-color-disabled` | `colors.button.submitFillDisabled` | `String` | `"#E0E0E0"` | Disabled submit button fill (hex) |
| `--color-button-submit` | `colors.button.submitText` | `String` | `"#1976D2"` | Submit button icon/text color (hex) |
| `--color-button-submit-hover` | `colors.button.submitTextHover` | `String` | `"#1565C0"` | Submit button hover color (hex) |
| `--button-disabled-background` | `colors.button.disabledBackground` | `String` | `"#E0E0E0"` | Disabled button background (hex) |

### Colors - Input

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--input-background` | `colors.input.background` | `String` | `"#FFFFFF"` | Input field background (hex) |
| `--input-text-color` | `colors.input.text` | `String` | `"#000000"` | Input text color (hex) |
| `--input-outline-color` | `colors.input.outline` | `String?` | `null` | Input border color (hex) |
| `--input-focus-outline-color` | `colors.input.outlineFocus` | `String` | `"#1976D2"` | Focused input border color (hex) |

### Colors - Citations

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--citations-background-color` | `colors.citation.backgroundColor` | `String` | `"#E0E0E0"` | Citation pill background (hex) |
| `--citations-text-color` | `colors.citation.textColor` | `String` | `"#000000"` | Citation text color (hex) |

### Colors - Feedback

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--feedback-icon-btn-background` | `colors.feedback.iconButtonBackground` | `String` | `"#FFFFFF"` | Feedback button background (hex) |
| `--feedback-icon-btn-hover-background` | `colors.feedback.iconButtonHoverBackground` | `String` | `"#F5F5F5"` | Feedback button hover background (hex) |

### Colors - Disclaimer

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--disclaimer-color` | `colors.disclaimer` | `String` | `"#757575"` | Disclaimer text color (hex) |

### Layout - Input

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--input-height-mobile` | `cssLayout.inputHeight` | `Double` | `52.0` | Input field height (dp) |
| `--input-border-radius-mobile` | `cssLayout.inputBorderRadius` | `Double` | `12.0` | Input field corner radius (dp) |
| `--input-outline-width` | `cssLayout.inputOutlineWidth` | `Double` | `2.0` | Input border width (dp) |
| `--input-focus-outline-width` | `cssLayout.inputFocusOutlineWidth` | `Double` | `2.0` | Focused input border width (dp) |
| `--input-font-size` | `cssLayout.inputFontSize` | `Double` | `16.0` | Input text font size (sp) |
| `--input-button-height` | `cssLayout.inputButtonHeight` | `Double` | `32.0` | Input button height (dp) |
| `--input-button-width` | `cssLayout.inputButtonWidth` | `Double` | `32.0` | Input button width (dp) |
| `--input-button-border-radius` | `cssLayout.inputButtonBorderRadius` | `Double` | `8.0` | Input button corner radius (dp) |
| `--input-box-shadow` | `cssLayout.inputBoxShadow` | `Map<String, Any>` | `null` | Input field shadow |

### Layout - Messages

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--message-border-radius` | `cssLayout.messageBorderRadius` | `Double` | `10.0` | Message bubble corner radius (dp) |
| `--message-padding` | `cssLayout.messagePadding` | `List<Double>` | `[8, 16]` | Message content padding (dp) |
| `--message-max-width` | `cssLayout.messageMaxWidth` | `Double?` | `null` | Max message width (dp or %) |

### Layout - Chat

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--chat-interface-max-width` | `cssLayout.chatInterfaceMaxWidth` | `Double` | `768.0` | Max chat interface width (dp) |
| `--chat-history-padding` | `cssLayout.chatHistoryPadding` | `Double` | `16.0` | Chat history horizontal padding (dp) |
| `--chat-history-padding-top-expanded` | `cssLayout.chatHistoryPaddingTopExpanded` | `Double` | `8.0` | Top padding when expanded (dp) |
| `--chat-history-bottom-padding` | `cssLayout.chatHistoryBottomPadding` | `Double` | `12.0` | Bottom padding (dp) |
| `--message-blocker-height` | `cssLayout.messageBlockerHeight` | `Double` | `105.0` | Message blocker overlay height (dp) |

### Layout - Cards & Carousel

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--border-radius-card` | `cssLayout.borderRadiusCard` | `Double` | `16.0` | Card corner radius (dp) |
| `--multimodal-card-box-shadow` | `cssLayout.multimodalCardBoxShadow` | `Map<String, Any>` | `null` | Card shadow |

### Layout - Buttons

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--button-height-s` | `cssLayout.buttonHeightSmall` | `Double` | `30.0` | Small button height (dp) |

### Layout - Feedback

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--feedback-container-gap` | `cssLayout.feedbackContainerGap` | `Double` | `4.0` | Gap between feedback buttons (dp) |
| `--feedback-icon-btn-size-desktop` | `components.feedback.iconButtonSizeDesktop` | `Double` | `32.0` | Feedback button hit target size (dp) |

### Layout - Citations

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--citations-text-font-weight` | `cssLayout.citationsTextFontWeight` | `Int` | `700` | Citation text weight |
| `--citations-desktop-button-font-size` | `cssLayout.citationsDesktopButtonFontSize` | `Double` | `14.0` | Citation button font size (sp) |

### Layout - Disclaimer

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--disclaimer-font-size` | `cssLayout.disclaimerFontSize` | `Double` | `12.0` | Disclaimer font size (sp) |
| `--disclaimer-font-weight` | `cssLayout.disclaimerFontWeight` | `Int` | `400` | Disclaimer font weight |

### Layout - Welcome Screen Order

| CSS Variable | Kotlin Property | Type | Default | Description |
|--------------|-----------------|------|---------|-------------|
| `--welcome-input-order` | `cssLayout.welcomeInputOrder` | `Int` | `3` | Input field display order |
| `--welcome-cards-order` | `cssLayout.welcomeCardsOrder` | `Int` | `2` | Example cards display order |

---

## Complete Example

```json
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
    "welcome.subheading": "I'm your personal guide to help you explore.",
    "input.placeholder": "How can I help?",
    "input.messageInput.aria": "Message input",
    "input.send.aria": "Send message",
    "feedback.dialog.title.positive": "Your feedback is appreciated",
    "feedback.dialog.submit": "Submit",
    "feedback.dialog.cancel": "Cancel"
  },
  "arrays": {
    "welcome.examples": [
      {
        "text": "I'd like to explore templates to see what I can create.",
        "image": "https://example.com/template.png",
        "backgroundColor": "#F5F5F5"
      }
    ],
    "feedback.positive.options": [
      "Helpful and relevant recommendations",
      "Clear and easy to understand",
      "Other"
    ],
    "feedback.negative.options": [
      "Didn't understand my request",
      "Unhelpful or irrelevant information",
      "Other"
    ]
  },
  "assets": {
    "icons": {
      "company": ""
    }
  },
  "theme": {
    "--welcome-input-order": "3",
    "--welcome-cards-order": "2",
    "--font-family": "",
    "--color-primary": "#EB1000",
    "--color-text": "#131313",
    "--line-height-body": "1.75",
    "--main-container-background": "#FFFFFF",
    "--main-container-bottom-background": "#FFFFFF",
    "--message-blocker-background": "#FFFFFF",
    "--input-height-mobile": "52px",
    "--input-border-radius-mobile": "12px",
    "--input-background": "#FFFFFF",
    "--input-outline-color": null,
    "--input-outline-width": "2px",
    "--input-focus-outline-width": "2px",
    "--input-focus-outline-color": "#4B75FF",
    "--input-font-size": "16px",
    "--input-text-color": "#292929",
    "--input-button-height": "32px",
    "--input-button-width": "32px",
    "--input-button-border-radius": "8px",
    "--input-box-shadow": "0 2px 8px 0 #00000014",
    "--submit-button-fill-color": "#FFFFFF",
    "--submit-button-fill-color-disabled": "#C6C6C6",
    "--color-button-submit": "#292929",
    "--button-disabled-background": "#FFFFFF",
    "--button-primary-background": "#3B63FB",
    "--button-primary-text": "#FFFFFF",
    "--button-secondary-border": "#2C2C2C",
    "--button-secondary-text": "#2C2C2C",
    "--button-height-s": "30px",
    "--disclaimer-color": "#4B4B4B",
    "--disclaimer-font-size": "12px",
    "--disclaimer-font-weight": "400",
    "--message-user-background": "#EBEEFF",
    "--message-user-text": "#292929",
    "--message-concierge-background": "#F5F5F5",
    "--message-concierge-text": "#292929",
    "--message-concierge-link-color": "#274DEA",
    "--message-border-radius": "10px",
    "--message-padding": "8px 16px",
    "--message-max-width": "100%",
    "--chat-interface-max-width": "768px",
    "--chat-history-padding": "16px",
    "--chat-history-padding-top-expanded": "0",
    "--chat-history-bottom-padding": "0",
    "--message-blocker-height": "105px",
    "--border-radius-card": "16px",
    "--multimodal-card-box-shadow": "none",
    "--feedback-container-gap": "4px",
    "--feedback-icon-btn-background": "#FFFFFF",
    "--feedback-icon-btn-size-desktop": "32px",
    "--citations-text-font-weight": "700",
    "--citations-desktop-button-font-size": "12px"
  }
}
```

---

## Implementation Status

This section documents which properties are fully implemented, partially implemented, or not yet implemented in the Android SDK based on actual usage in composables.

### Legend

| Status | Description |
|--------|-------------|
| ✅ | Fully implemented - property is mapped and actively used in composables |
| ⚠️ | Defined but unused - property is parsed but not rendered in any composable |
| ❌ | Not supported - property exists in web JSON but is ignored by Android |

**Note**: The tables below include a "Used In" column showing which UI composables consume each theme property.

### Implementation Summary

**Overall Implementation Status:**
- **Colors**: ~70% implemented (most core colors used, hover states not applicable)
- **Typography**: 60% implemented (`fontFamily` not yet supported)
- **Layout**: ~15% implemented (only outline widths and font sizes currently used)
- **Behavior**: ~10% implemented (only `enableVoiceInput` functional)
- **Text/Copy**: ~50% implemented (main strings used, accessibility labels not yet implemented)

**Key Differences from Web/iOS:**
- Hover states (`--button-primary-hover`, `--feedback-icon-btn-hover-background`) are parsed but not applicable on Android
- Box shadows are parsed but not currently rendered
- Most layout dimensions (padding, margins, border radius) are hardcoded rather than theme-driven
- Accessibility labels (aria) are parsed but not yet connected to content descriptions

### Metadata

| Property | Status | Notes | Used In |
|----------|--------|-------|---------|
| `metadata.brandName` | ✅ | Replaces `[Name]` placeholder in welcome heading text | `WelcomeCard` |
| `metadata.version` | ⚠️ | Parsed but not used | - |
| `metadata.language` | ⚠️ | Parsed but not used for localization | - |
| `metadata.namespace` | ⚠️ | Parsed but not used | - |

### Behavior

| Property | Status | Notes | Used In |
|----------|--------|-------|---------|
| `behavior.multimodalCarousel.cardClickAction` | ⚠️ | Parsed but not implemented in carousel composables | - |
| `behavior.input.enableVoiceInput` | ✅ | Controls mic button visibility | `InputActionButtons` |
| `behavior.input.disableMultiline` | ⚠️ | Parsed but not implemented | - |
| `behavior.input.showAiChatIcon` | ⚠️ | Parsed but not rendered | - |
| `behavior.chat.messageAlignment` | ⚠️ | Parsed but not implemented | - |
| `behavior.chat.messageWidth` | ⚠️ | Parsed but not implemented | - |
| `behavior.privacyNotice.title` | ⚠️ | Parsed but no privacy dialog implemented | - |
| `behavior.privacyNotice.text` | ⚠️ | Parsed but no privacy dialog implemented | - |

### Disclaimer

| Property | Status | Notes | Used In |
|----------|--------|-------|---------|
| `disclaimer.text` | ⚠️ | Parsed but not implemented | Disclaimer component |
| `disclaimer.links` | ⚠️ | Parsed but not implemented | Disclaimer component |

### Text (Copy)

| Property | Status | Notes | Used In |
|----------|--------|-------|---------|
| `text["welcome.heading"]` | ✅ | Welcome screen title with `[Name]` placeholder replacement | `WelcomeCard` |
| `text["welcome.subheading"]` | ✅ | Welcome screen description | `WelcomeCard` |
| `text["input.placeholder"]` | ✅ | Input field hint text | `ChatTextField` |
| `text["input.messageInput.aria"]` | ⚠️ | Parsed but not used for accessibility | - |
| `text["input.send.aria"]` | ⚠️ | Parsed but not used for accessibility | - |
| `text["input.aiChatIcon.tooltip"]` | ⚠️ | Parsed but AI icon not rendered | - |
| `text["input.mic.aria"]` | ⚠️ | Parsed but not used for accessibility | - |
| `text["card.aria.select"]` | ⚠️ | Parsed but not used for accessibility | - |
| `text["carousel.prev.aria"]` | ⚠️ | Parsed but not used for accessibility | - |
| `text["carousel.next.aria"]` | ⚠️ | Parsed but not used for accessibility | - |
| `text["scroll.bottom.aria"]` | ⚠️ | Parsed but scroll button not implemented | - |
| `text["error.network"]` | ⚠️ | Parsed but error uses hardcoded text | - |
| `text["loading.message"]` | ✅ | Loading animation text | `ConciergeThinking` |
| `text["feedback.dialog.title.positive"]` | ✅ | Feedback dialog title for positive feedback | `FeedbackDialog` |
| `text["feedback.dialog.title.negative"]` | ✅ | Feedback dialog title for negative feedback | `FeedbackDialog` |
| `text["feedback.dialog.question.positive"]` | ✅ | Feedback dialog question for positive feedback | `FeedbackDialog` |
| `text["feedback.dialog.question.negative"]` | ✅ | Feedback dialog question for negative feedback | `FeedbackDialog` |
| `text["feedback.dialog.notes"]` | ✅ | Feedback dialog notes label | `FeedbackDialog` |
| `text["feedback.dialog.submit"]` | ✅ | Feedback dialog submit button text | `FeedbackDialog` |
| `text["feedback.dialog.cancel"]` | ✅ | Feedback dialog cancel button text | `FeedbackDialog` |
| `text["feedback.dialog.notes.placeholder"]` | ✅ | Feedback dialog notes placeholder | `FeedbackDialog` |
| `text["feedback.toast.success"]` | ⚠️ | Parsed but toast not implemented | - |
| `text["feedback.thumbsUp.aria"]` | ⚠️ | Parsed but not used for accessibility | - |
| `text["feedback.thumbsDown.aria"]` | ⚠️ | Parsed but not used for accessibility | - |

### Arrays

| Property | Status | Notes | Used In |
|----------|--------|-------|---------|
| `arrays["welcome.examples"]` | ✅ | Suggested prompts displayed on welcome screen | `WelcomeCard` → `PromptSuggestions` |
| `arrays["feedback.positive.options"]` | ✅ | Positive feedback category options | `FeedbackDialog` |
| `arrays["feedback.negative.options"]` | ✅ | Negative feedback category options | `FeedbackDialog` |

### Assets

| Property | Status | Notes | Used In |
|----------|--------|-------|---------|
| `assets.icons.company` | ⚠️ | Parsed but not rendered in any composable | - |

### Theme Tokens - Typography

| CSS Variable | Status | Notes | Used In |
|--------------|--------|-------|---------|
| `--font-family` | ⚠️ | Parsed but not implemented (commented out in `withThemeTypography`) | - |
| `--line-height-body` | ✅ | Body text line height | All text components via `ConciergeStyles.withThemeTypography` |
| `--input-font-size` | ✅ | Input field text size | `ChatTextField` |
| `--citations-desktop-button-font-size` | ✅ | Citation pill text size | `CircularCitation` |
| `--disclaimer-font-size` | ⚠️ | Parsed but not used in composables | - |

### Theme Tokens - Colors

**Note**: The following base colors are **not configurable via JSON themes**. They are hardcoded in `LightConciergeColors` / `DarkConciergeColors` and serve as fallback colors throughout the UI:
- `secondary`, `onSurfaceVariant`, `container`, `outline`, `error`, `onError`

These colors are used internally by composables but cannot be customized in theme JSON files. See "Fallback Colors" section at the end.

| CSS Variable | Status | Notes | Used In |
|--------------|--------|-------|---------|
| `--color-primary` | ✅ | Primary brand color used throughout UI | `ChatHeader`, `InputActionButtons`, `WelcomeCard`, `FeedbackDialog`, `ErrorOverlay`, `ProductCard` (fallback), `VoiceRecordingPanel` |
| `--color-text` | ✅ | Main text color (mapped to `onPrimary`) | All text components |
| `--main-container-background` | ✅ | Main chat screen background | `ChatScreen` |
| `--main-container-bottom-background` | ✅ | Bottom container/surface background | `FeedbackDialog`, `VoiceRecordingPanel` |
| `--message-blocker-background` | ⚠️ | Parsed but not used in UI | - |
| `--message-user-background` | ✅ | User message bubble background | `ChatMessageItem` |
| `--message-user-text` | ✅ | User message text color | `ChatMessageItem` |
| `--message-concierge-background` | ✅ | AI message bubble background | `ChatMessageItem` |
| `--message-concierge-text` | ✅ | AI message text color | `ChatMessageItem` |
| `--message-concierge-link-color` | ⚠️ | Parsed but links use `primary` color | - |
| `--button-primary-background` | ✅ | Primary button background | `ProductActionButtons` |
| `--button-primary-text` | ✅ | Primary button text | `ProductActionButtons` |
| `--button-primary-hover` | ⚠️ | Parsed but no hover states on Android | - |
| `--button-secondary-border` | ✅ | Secondary button border | `ProductActionButtons` |
| `--button-secondary-text` | ✅ | Secondary button text | `ProductActionButtons` |
| `--button-secondary-hover` | ⚠️ | Parsed but no hover states on Android | - |
| `--color-button-secondary-hover-text` | ⚠️ | Parsed but no hover states on Android | - |
| `--submit-button-fill-color` | ✅ | Feedback dialog submit button background | `FeedbackDialog` |
| `--submit-button-fill-color-disabled` | ⚠️ | Parsed but disabled state not implemented | - |
| `--color-button-submit` | ✅ | Feedback dialog submit button text/icon | `FeedbackDialog` |
| `--color-button-submit-hover` | ⚠️ | Parsed but no hover states on Android | - |
| `--button-disabled-background` | ⚠️ | Parsed but disabled state not implemented | - |
| `--input-background` | ✅ | Input field background | `ChatInputPanel` |
| `--input-text-color` | ✅ | Input field text color | `ChatTextField`, `FeedbackDialog` |
| `--input-outline-color` | ✅ | Input field border color | `ChatInputPanel` |
| `--input-focus-outline-color` | ✅ | Input field focused border color | `ChatInputPanel` |
| `--citations-background-color` | ✅ | Citation pill background | `CircularCitation` |
| `--citations-text-color` | ✅ | Citation pill text | `CircularCitation` |
| `--feedback-icon-btn-background` | ✅ | Thumbs up/down button background | `FeedbackComponents` |
| `--feedback-icon-btn-hover-background` | ⚠️ | Parsed but no hover states on Android | - |
| `--disclaimer-color` | ⚠️ | Parsed but no disclaimer component in UI | - |

### Theme Tokens - Layout

| CSS Variable | Status | Notes | Used In |
|--------------|--------|-------|---------|
| `--input-height-mobile` | ⚠️ | Parsed but not used in composables | - |
| `--input-border-radius-mobile` | ⚠️ | Parsed but not used in composables | - |
| `--input-outline-width` | ✅ | Input field border width | `ChatInputPanel` |
| `--input-focus-outline-width` | ✅ | Input field focused border width | `ChatInputPanel` |
| `--input-font-size` | ✅ | Input field text size | `ChatTextField` |
| `--input-button-height` | ⚠️ | Parsed but not used in composables | - |
| `--input-button-width` | ⚠️ | Parsed but not used in composables | - |
| `--input-button-border-radius` | ⚠️ | Parsed but not used in composables | - |
| `--input-box-shadow` | ⚠️ | Parsed but shadows not rendered | - |
| `--message-border-radius` | ⚠️ | Parsed but not used in composables | - |
| `--message-padding` | ⚠️ | Parsed but not used in composables | - |
| `--message-max-width` | ⚠️ | Parsed but not used in composables | - |
| `--chat-interface-max-width` | ⚠️ | Parsed but not used in composables | - |
| `--chat-history-padding` | ⚠️ | Parsed but not used in composables | - |
| `--chat-history-padding-top-expanded` | ⚠️ | Parsed but not used in composables | - |
| `--chat-history-bottom-padding` | ⚠️ | Parsed but not used in composables | - |
| `--message-blocker-height` | ⚠️ | Parsed but not used in composables | - |
| `--border-radius-card` | ⚠️ | Parsed but not used in composables | - |
| `--multimodal-card-box-shadow` | ⚠️ | Parsed but shadows not rendered | - |
| `--button-height-s` | ⚠️ | Parsed but not used in composables | - |
| `--feedback-container-gap` | ⚠️ | Parsed but not used in composables | - |
| `--feedback-icon-btn-size-desktop` | ⚠️ | Parsed but not used in composables | - |
| `--citations-text-font-weight` | ⚠️ | Parsed but not used in composables | - |
| `--citations-desktop-button-font-size` | ✅ | Citation pill text size | `CircularCitation` |
| `--disclaimer-font-size` | ⚠️ | Parsed but not used in composables | - |
| `--disclaimer-font-weight` | ⚠️ | Parsed but not used in composables | - |
| `--welcome-input-order` | ⚠️ | Parsed but welcome layout not customizable | - |
| `--welcome-cards-order` | ⚠️ | Parsed but welcome layout not customizable | - |

### Unsupported CSS Variables

The following CSS variables from web themes are **not supported** on Android (desktop-only properties like `--input-height` and `--input-border-radius` without `-mobile` suffix):

| CSS Variable | Notes |
|--------------|-------|
| `--input-height` | Use `--input-height-mobile` instead |
| `--input-border-radius` | Use `--input-border-radius-mobile` instead |
| `--message-alignment` | Use `behavior.chat.messageAlignment` instead |
| `--message-width` | Use `behavior.chat.messageWidth` instead |

---

## Fallback Colors (Not Theme-Configurable)

The following colors from `LightConciergeColors` / `DarkConciergeColors` are hardcoded and **cannot be customized via JSON themes**. They serve as fallback colors throughout the UI:

| Color | Purpose | Used In Composables |
|-------|---------|---------------------|
| `secondary` | Secondary accent color (currently unused) | - |
| `onSurfaceVariant` | Muted text and icons for secondary UI elements | `PromptSuggestions`, `ChatFooter`, `FeedbackDialog` (unchecked checkboxes) |
| `container` | Background for cards and container elements | `ProductCard`, `PromptSuggestions`, message bubbles (fallback), `ChatInputPanel` (fallback) |
| `outline` | Borders, separators, and outline elements | `ChatFooter` separator, `ProductActionButtons` (secondary button fallback), `FeedbackDialog` text field border, `ProductCarousel` nav buttons |
| `error` | Error state background | `ErrorOverlay` background |
| `onError` | Error state text | `ErrorOverlay` message text |
| `onSurface` | Primary text on surface backgrounds | Most text components, `VoiceRecordingPanel`, `ProductCard`, `FeedbackDialog` |

**Note**: While these colors provide consistent fallback styling, they cannot be overridden in theme JSON files. If you need custom colors for these UI elements, use the theme-specific CSS variables that map to these elements (e.g., use `--input-outline-color` instead of relying on the `outline` fallback).

---

## Recommendations for Theme Authors

### What to Focus On

When creating themes for the Android SDK, focus on these **actively used** properties for the best results:

**Essential Colors (Highest Impact):**
- `--color-primary` - Primary brand color
- `--color-text` - Main text color
- `--message-user-background` / `--message-user-text` - User message styling
- `--message-concierge-background` / `--message-concierge-text` - AI message styling
- `--button-primary-background` / `--button-primary-text` - Primary buttons
- `--button-secondary-border` / `--button-secondary-text` - Secondary buttons
- `--input-background` / `--input-text-color` - Input field colors
- `--input-outline-color` / `--input-focus-outline-color` - Input borders
- `--citations-background-color` / `--citations-text-color` - Citation pills
- `--feedback-icon-btn-background` - Feedback button styling

**Essential Text/Copy:**
- `text["welcome.heading"]` - Welcome screen title
- `text["welcome.subheading"]` - Welcome screen description
- `text["input.placeholder"]` - Input field hint
- `text["loading.message"]` - Loading indicator text
- All `text["feedback.dialog.*"]` - Feedback dialog strings

**Essential Behavior:**
- `behavior.input.enableVoiceInput` - Show/hide microphone button

**Essential Layout:**
- `--input-outline-width` / `--input-focus-outline-width` - Input border thickness
- `--input-font-size` - Input text size
- `--citations-desktop-button-font-size` - Citation text size
- `--line-height-body` - Text line spacing

### What Can Be Skipped

These properties are parsed but **not currently used** and can be omitted without affecting the UI:

- Hover states (all `*-hover` properties)
- Box shadows (all `*-box-shadow` properties)
- Most layout dimensions (padding, margins, border radius) - currently hardcoded
- Disabled button states
- Accessibility labels (not yet connected to Android content descriptions)
- Welcome screen ordering (`--welcome-input-order`, `--welcome-cards-order`)
- Font family (`--font-family` - not yet implemented)

### Testing Your Theme

1. **Test core flows**: Create a theme, load it, send messages, provide feedback
2. **Check on multiple devices**: Test on different screen sizes and Android versions
3. **Verify contrast ratios**: Ensure text is readable on all backgrounds
4. **Test light/dark modes**: If supporting both, verify colors work in both contexts

---
