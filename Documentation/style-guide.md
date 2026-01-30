# BrandConcierge Style Guide (Android)

This document provides a comprehensive reference for all styling properties supported by the BrandConcierge extension on Android. Themes are configured using JSON files that follow a web-compatible CSS variable format.

## Table of Contents

- [Overview](#overview)
- [JSON Structure](#json-structure)
- [Loading a Theme](#loading-a-theme)
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
- [Value Formats](#value-formats)
- [Complete Example](#complete-example)
- [Implementation Status](#implementation-status)

---

## Overview

The theme configuration is loaded from a JSON file using `ConciergeThemeLoader.load()`. The framework supports CSS-like variable names (prefixed with `--`) that are automatically mapped to native Kotlin/Compose properties.

### Key Features

- **Simple static methods** for common use cases
- **Caching** to avoid repeated file reads
- **Fallback support** for graceful degradation
- **Multiple source types** (assets, files, JSON strings)
- **Thread-safe** singleton pattern

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

## Loading a Theme

### Basic Loading (Recommended)

Load a theme from your app's assets folder:

```kotlin
@Composable
fun MyScreen() {
    val context = LocalContext.current
    val theme = remember {
        ConciergeThemeLoader.load(context, "myTheme.json")
            ?: ConciergeThemeLoader.default()
    }
    
    ConciergeTheme(theme = theme) {
        // Your UI here
        ConciergeChat(/* ... */)
    }
}
```

### Load with Different Sources

```kotlin
val loader = ConciergeThemeLoader.instance

// From assets (same as static method)
val theme1 = loader.loadTheme(
    context = context,
    source = "theme.json",
    sourceType = ThemeSourceType.ASSET
)

// From file system
val theme2 = loader.loadTheme(
    context = context,
    source = "/sdcard/custom_theme.json",
    sourceType = ThemeSourceType.FILE
)

// From JSON string
val jsonString = """{"metadata": {"brandName": "Test"}}"""
val theme3 = loader.loadTheme(
    context = context,
    source = jsonString,
    sourceType = ThemeSourceType.JSON_STRING
)
```

### XML/Views Integration

```kotlin
class MyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my)
        
        // Load theme
        val theme = ConciergeThemeLoader.load(this, "myTheme.json")
        
        // Find ConciergeChatView in layout
        val chatView = findViewById<ConciergeChatView>(R.id.concierge_chat)
        
        // Bind with theme
        chatView.bind(
            lifecycleOwner = this,
            theme = theme
        )
    }
}
```

---

## Metadata

Theme identification information.

| JSON Key | Type | Default | Description |
|----------|------|---------|-------------|
| `metadata.brandName` | `String` | `""` | Brand/company name |
| `metadata.version` | `String` | `"1.0.0"` | Theme version |
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

**Note:** The `metadata.brandName` property is used to replace the `[Name]` placeholder in the welcome heading text.

---

## Behavior

Feature toggles and interaction configuration.

### Multimodal Carousel

| JSON Key | Type | Default | Description |
|----------|------|---------|-------------|
| `behavior.multimodalCarousel.cardClickAction` | `String` | `"openLink"` | Action when carousel card is tapped |

### Input

| JSON Key | Type | Default | Description |
|----------|------|---------|-------------|
| `behavior.input.enableVoiceInput` | `Boolean` | `true` | Enable voice input button |
| `behavior.input.disableMultiline` | `Boolean` | `false` | Disable multiline text input |
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

### Welcome Screen

| JSON Key | Default | Description |
|----------|---------|-------------|
| `text["welcome.heading"]` | `"Explore what you can do..."` | Welcome screen heading |
| `text["welcome.subheading"]` | `"Choose an option or tell us..."` | Welcome screen subheading |

### Input

| JSON Key | Default | Description |
|----------|---------|-------------|
| `text["input.placeholder"]` | `"How can I help?"` | Input field placeholder |

### System Messages

| JSON Key | Default | Description |
|----------|---------|-------------|
| `text["error.network"]` | `"I'm sorry, I'm having trouble..."` | Network error message |
| `text["loading.message"]` | `"Generating response..."` | Loading/thinking animation text |

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

### Example

```json
{
  "text": {
    "welcome.heading": "Welcome to Brand Concierge!",
    "welcome.subheading": "I'm your personal guide to help you explore and find exactly what you need.",
    "input.placeholder": "How can I help?",
    "error.network": "I'm sorry, I'm having trouble connecting to our services right now.",
    "loading.message": "Generating response from our knowledge base",
    "feedback.dialog.title.positive": "Your feedback is appreciated",
    "feedback.dialog.submit": "Submit",
    "feedback.dialog.cancel": "Cancel"
  }
}
```

**Note:** The `[Name]` placeholder in `welcome.heading` is automatically replaced with `metadata.brandName`.

---

## Arrays

List-based configuration for examples and feedback options.

### Welcome Examples

| JSON Key | Type | Description |
|----------|------|-------------|
| `arrays["welcome.examples"]` | `Array` | Welcome screen example cards |
| `arrays["welcome.examples"][].text` | `String` | Card display text |
| `arrays["welcome.examples"][].image` | `String?` | Card image URL |
| `arrays["welcome.examples"][].backgroundColor` | `String?` | Card background color (hex) |

### Feedback Options

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
      },
      {
        "text": "I want to touch up and enhance my photos.",
        "image": "https://example.com/photo.png",
        "backgroundColor": "#F5F5F5"
      }
    ],
    "feedback.positive.options": [
      "Helpful and relevant recommendations",
      "Clear and easy to understand",
      "Friendly and conversational tone",
      "Other"
    ],
    "feedback.negative.options": [
      "Didn't understand my request",
      "Unhelpful or irrelevant information",
      "Too vague or lacking detail",
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

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--font-family` | `ConciergeTypography.fontFamily` | `String` | `""` | Font family name |
| `--line-height-body` | `ConciergeTypography.lineHeight` | `Double` | `1.75` | Line height multiplier |

### Colors - Primary

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--color-primary` | `ConciergeColors.primary` | `Color` | `#EB1000` | Primary brand color |
| `--color-text` | `ConciergeColors.onPrimary` | `Color` | `#131313` | Primary text color |

### Colors - Surface

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--main-container-background` | `ConciergeSurfaceColors.mainContainerBackground` | `Color` | `#FFFFFF` | Main container background |
| `--main-container-bottom-background` | `ConciergeSurfaceColors.mainContainerBottomBackground` | `Color` | `#FFFFFF` | Bottom container background |
| `--message-blocker-background` | `ConciergeSurfaceColors.messageBlockerBackground` | `Color` | `#FFFFFF` | Message blocker overlay |

### Colors - Messages

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--message-user-background` | `ConciergeMessageColors.userBackground` | `Color` | `#EBEEFF` | User message bubble background |
| `--message-user-text` | `ConciergeMessageColors.userText` | `Color` | `#292929` | User message text color |
| `--message-concierge-background` | `ConciergeMessageColors.conciergeBackground` | `Color` | `#F5F5F5` | AI message bubble background |
| `--message-concierge-text` | `ConciergeMessageColors.conciergeText` | `Color` | `#292929` | AI message text color |
| `--message-concierge-link-color` | `ConciergeMessageColors.conciergeLink` | `Color` | `#274DEA` | Link color in AI messages |

### Colors - Buttons

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--button-primary-background` | `ConciergeButtonColors.primaryBackground` | `Color` | `#3B63FB` | Primary button background |
| `--button-primary-text` | `ConciergeButtonColors.primaryText` | `Color` | `#FFFFFF` | Primary button text |
| `--button-primary-hover` | `ConciergeButtonColors.primaryHover` | `Color` | `#274DEA` | Primary button hover state |
| `--button-secondary-border` | `ConciergeButtonColors.secondaryBorder` | `Color` | `#2C2C2C` | Secondary button border |
| `--button-secondary-text` | `ConciergeButtonColors.secondaryText` | `Color` | `#2C2C2C` | Secondary button text |
| `--button-secondary-hover` | `ConciergeButtonColors.secondaryHover` | `Color` | `#000000` | Secondary button hover bg |
| `--color-button-secondary-hover-text` | `ConciergeButtonColors.secondaryHoverText` | `Color` | `#FFFFFF` | Secondary button hover text |
| `--submit-button-fill-color` | `ConciergeButtonColors.submitFill` | `Color` | `#FFFFFF` | Submit button fill |
| `--submit-button-fill-color-disabled` | `ConciergeButtonColors.submitFillDisabled` | `Color` | `#C6C6C6` | Disabled submit button fill |
| `--color-button-submit` | `ConciergeButtonColors.submitText` | `Color` | `#292929` | Submit button icon/text color |
| `--color-button-submit-hover` | `ConciergeButtonColors.submitTextHover` | `Color` | `#292929` | Submit button hover color |
| `--button-disabled-background` | `ConciergeButtonColors.disabledBackground` | `Color` | `#FFFFFF` | Disabled button background |
| `--button-height-s` | `ConciergeLayout.buttonHeightSmall` | `Dp` | `30.dp` | Small button height |

### Colors - Input

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--input-background` | `ConciergeInputColors.background` | `Color` | `#FFFFFF` | Input field background |
| `--input-text-color` | `ConciergeInputColors.text` | `Color` | `#292929` | Input text color |
| `--input-outline-color` | `ConciergeInputColors.outline` | `Color?` | `null` | Input border color |
| `--input-focus-outline-color` | `ConciergeInputColors.outlineFocus` | `Color` | `#4B75FF` | Focused input border color |

### Colors - Feedback

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--feedback-icon-btn-background` | `ConciergeFeedbackColors.iconButtonBackground` | `Color` | `#FFFFFF` | Feedback button background |
| `--feedback-icon-btn-hover-background` | `ConciergeFeedbackColors.iconButtonHoverBackground` | `Color` | `#FFFFFF` | Feedback button hover |

### Colors - Citations

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--citations-background-color` | `ConciergeCitationColors.backgroundColor` | `Color` | System default | Citation pill background |
| `--citations-text-color` | `ConciergeCitationColors.textColor` | `Color` | System default | Citation text color |

### Colors - Disclaimer

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--disclaimer-color` | `ConciergeColors.disclaimer` | `Color` | `#4B4B4B` | Disclaimer text color |

### Layout - Input

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--input-height-mobile` | `ConciergeLayout.inputHeight` | `Dp` | `52.dp` | Input field height |
| `--input-border-radius-mobile` | `ConciergeLayout.inputBorderRadius` | `Dp` | `12.dp` | Input field corner radius |
| `--input-outline-width` | `ConciergeLayout.inputOutlineWidth` | `Dp` | `2.dp` | Input border width |
| `--input-focus-outline-width` | `ConciergeLayout.inputFocusOutlineWidth` | `Dp` | `2.dp` | Focused input border width |
| `--input-font-size` | `ConciergeTypographyConfig.inputFontSize` | `TextUnit` | `16.sp` | Input text font size |
| `--input-button-height` | `ConciergeLayout.inputButtonHeight` | `Dp` | `32.dp` | Input button height |
| `--input-button-width` | `ConciergeLayout.inputButtonWidth` | `Dp` | `32.dp` | Input button width |
| `--input-button-border-radius` | `ConciergeLayout.inputButtonBorderRadius` | `Dp` | `8.dp` | Input button corner radius |
| `--input-box-shadow` | `ConciergeLayout.inputBoxShadow` | `Map` | `none` | Input field shadow |

### Layout - Messages

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--message-border-radius` | `ConciergeLayout.messageBorderRadius` | `Dp` | `10.dp` | Message bubble corner radius |
| `--message-padding` | `ConciergeLayout.messagePadding` | `List<Dp>` | `[8dp, 16dp]` | Message content padding |
| `--message-max-width` | `ConciergeLayout.messageMaxWidth` | `Double?` | `100.0` | Max message width (parsed as percentage) |

### Layout - Chat

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--chat-interface-max-width` | `ConciergeLayout.chatInterfaceMaxWidth` | `Dp` | `768.dp` | Max chat interface width |
| `--chat-history-padding` | `ConciergeLayout.chatHistoryPadding` | `Dp` | `16.dp` | Chat history horizontal padding |
| `--chat-history-padding-top-expanded` | `ConciergeLayout.chatHistoryPaddingTopExpanded` | `Dp` | `0.dp` | Top padding when expanded |
| `--chat-history-bottom-padding` | `ConciergeLayout.chatHistoryBottomPadding` | `Dp` | `0.dp` | Bottom padding |
| `--message-blocker-height` | `ConciergeLayout.messageBlockerHeight` | `Dp` | `105.dp` | Message blocker overlay height |

### Layout - Cards & Feedback

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--border-radius-card` | `ConciergeLayout.borderRadiusCard` | `Dp` | `16.dp` | Card corner radius |
| `--multimodal-card-box-shadow` | `ConciergeLayout.multimodalCardBoxShadow` | `Map` | `none` | Card shadow |
| `--feedback-container-gap` | `ConciergeLayout.feedbackContainerGap` | `Dp` | `4.dp` | Gap between feedback buttons |
| `--feedback-icon-btn-size-desktop` | `ConciergeComponentsConfig.feedback.iconButtonSizeDesktop` | `Dp` | `32.dp` | Feedback button hit target size |

### Layout - Citations

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--citations-text-font-weight` | `ConciergeLayout.citationsTextFontWeight` | `Int` | `700` | Citation text weight (100-900) |
| `--citations-desktop-button-font-size` | `ConciergeLayout.citationsDesktopButtonFontSize` | `Double` | `14.0` | Citation button font size |

### Layout - Disclaimer

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--disclaimer-font-size` | `ConciergeTypographyConfig.disclaimerFontSize` | `TextUnit` | `12.sp` | Disclaimer font size |
| `--disclaimer-font-weight` | `ConciergeLayout.disclaimerFontWeight` | `Int` | `400` | Disclaimer font weight (100-900) |

### Layout - Welcome Screen Order

| CSS Variable | Compose Property | Type | Default | Description |
|--------------|------------------|------|---------|-------------|
| `--welcome-input-order` | `ConciergeLayout.welcomeInputOrder` | `Int` | `3` | Input field display order |
| `--welcome-cards-order` | `ConciergeLayout.welcomeCardsOrder` | `Int` | `2` | Example cards display order |

---

## Reserved for Future Support

The following properties are parsed from theme JSON files but are not currently implemented in the Android UI. They are reserved for future enhancements and cross-platform compatibility.

### Metadata Properties

These metadata properties are parsed but not actively used:

| JSON Key | Type | Description |
|----------|------|-------------|
| `metadata.version` | `String` | Theme version identifier |
| `metadata.language` | `String` | Locale/language code |
| `metadata.namespace` | `String` | Theme namespace identifier |

### Behavior Properties

All behavior configuration properties are parsed but not implemented:

| JSON Key | Type | Description |
|----------|------|-------------|
| `behavior.multimodalCarousel.cardClickAction` | `String` | Carousel card tap action |
| `behavior.input.enableVoiceInput` | `Boolean` | Voice input toggle |
| `behavior.input.disableMultiline` | `Boolean` | Multiline input toggle |
| `behavior.input.showAiChatIcon` | `Object?` | AI chat icon config |
| `behavior.chat.messageAlignment` | `String` | Message alignment setting |
| `behavior.chat.messageWidth` | `String` | Message width setting |
| `behavior.privacyNotice.title` | `String` | Privacy notice title |
| `behavior.privacyNotice.text` | `String` | Privacy notice content |

**Note:** The Android SDK also parses additional flat behavior properties (`enableDarkMode`, `enableAnimations`, `enableHaptics`, `enableSoundEffects`, `autoScrollToBottom`, `showTimestamps`, `enableMarkdown`, `enableCitations`, `enableFeedback`, `maxMessageLength`, `typingIndicatorDelay`) but these are not currently implemented.

### Typography Properties

Typography customization properties are parsed but not applied:

| CSS Variable | Description |
|--------------|-------------|
| `--font-family` | Custom font family name |
| `--line-height-body` | Line height multiplier |

**Note:** The SDK currently uses system fonts with default line heights.

### Layout Properties

These layout properties are parsed but not currently used:

| CSS Variable | Description |
|--------------|-------------|
| `--input-outline-width` | Input outline thickness (non-focused state) |
| `--welcome-input-order` | Welcome screen input field display order |
| `--welcome-cards-order` | Welcome screen cards display order |

**Note:** Input outline width is parsed but only the focused outline width is rendered. Welcome screen layout order is not customizable.

### Asset Properties

Asset configuration is parsed but not rendered:

| JSON Key | Description |
|----------|-------------|
| `assets.icons.company` | Company logo (SVG or URL) |

**Note:** Additional asset properties (`icons.send`, `icons.microphone`, `icons.close`, `images.welcomeBanner`, `fonts.*`, etc.) may be parsed if present but are not documented or used. The SDK uses built-in icons and system fonts.

### Example with Reserved Properties

You can include these properties in your theme JSON for future compatibility:

```json
{
  "metadata": {
    "brandName": "My Brand",
    "version": "1.0.0",
    "language": "en-US",
    "namespace": "my-brand-theme"
  },
  "behavior": {
    "input": {
      "enableVoiceInput": true,
      "disableMultiline": false
    },
    "chat": {
      "messageAlignment": "left",
      "messageWidth": "100%"
    }
  },
  "theme": {
    "--font-family": "CustomFont",
    "--line-height-body": "1.5",
    "--input-outline-width": "2px",
    "--welcome-input-order": "3",
    "--welcome-cards-order": "2"
  }
}
```

These properties will be safely ignored by the current implementation but may be utilized in future SDK updates.

---

## Value Formats

The Android SDK uses `CSSValueConverter` to parse CSS-style values from JSON.

### Colors

Colors are specified as hex strings or CSS color functions:

```json
"--color-primary": "#EB1000"
"--message-user-background": "#EBEEFF"
```

Supported formats:
- `#RGB` - 3 digit hex (e.g., `#F00` = `#FF0000`)
- `#RRGGBB` - 6 digit hex
- `#RRGGBBAA` - 8 digit hex with alpha
- `rgb(r, g, b)` - RGB function
- `rgba(r, g, b, a)` - RGBA function with alpha

**Special handling:**
- Gradients (e.g., `linear-gradient(...)`) are detected and converted to `null`
- Invalid colors default to `null` and fall back to theme defaults

### Dimensions

Dimensions use CSS pixel units or percentages:

```json
"--input-height-mobile": "52px"
"--message-max-width": "100%"
```

Supported formats:
- `"16px"` - Explicit pixel value
- `"16"` - Numeric string (treated as pixels)
- `"100%"` - Percentage (for width properties)

**Conversion:** Pixel values are converted to `Dp` in Compose

### Padding

Padding follows CSS shorthand syntax:

```json
"--message-padding": "8px 16px"
```

Supported formats:
- `"8px"` - All sides (returns `[8, 8, 8, 8]`)
- `"8px 16px"` - Vertical, horizontal (returns `[8, 16, 8, 16]`)
- `"8px 16px 4px"` - Top, horizontal, bottom (returns `[8, 16, 4, 16]`)
- `"8px 16px 4px 2px"` - Top, right, bottom, left (returns `[8, 16, 4, 2]`)

**Return type:** `List<Double>` with indices `[top, right, bottom, left]`

### Shadows

Shadows use CSS box-shadow syntax:

```json
"--input-box-shadow": "0 2px 8px 0 #00000014"
"--multimodal-card-box-shadow": "none"
```

Format: `offsetX offsetY blurRadius [spreadRadius] [color]`

**Return type:** `Map<String, Any>` with keys:
- `offsetX` (Double)
- `offsetY` (Double)
- `blurRadius` (Double)
- `spreadRadius` (Double, optional)
- `color` (String, hex format)

**Special values:** `"none"` returns empty map

### Font Weights

Font weights use CSS numeric or named values:

```json
"--citations-text-font-weight": "700"
"--disclaimer-font-weight": "400"
```

Supported values:

| CSS Value | Numeric | Compose Equivalent |
|-----------|---------|-------------------|
| `"normal"` | `400` | `FontWeight.Normal` |
| `"bold"` | `700` | `FontWeight.Bold` |
| `"100"` - `"900"` | `100-900` | `FontWeight.W100` - `FontWeight.Black` |

---

## Complete Example

This example shows the complete reference theme configuration:

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
    "welcome.subheading": "I'm your personal guide to help you explore and find exactly what you need. Let's get started!\n\nNot sure where to start? Explore the suggested ideas below.",
    "input.placeholder": "How can I help?",
    "error.network": "I'm sorry, I'm having trouble connecting to our services right now.",
    "loading.message": "Generating response from our knowledge base",
    "feedback.dialog.title.positive": "Your feedback is appreciated",
    "feedback.dialog.title.negative": "Your feedback is appreciated",
    "feedback.dialog.question.positive": "What went well? Select all that apply.",
    "feedback.dialog.question.negative": "What went wrong? Select all that apply.",
    "feedback.dialog.notes": "Notes",
    "feedback.dialog.submit": "Submit",
    "feedback.dialog.cancel": "Cancel",
    "feedback.dialog.notes.placeholder": "Additional notes (optional)",
    "feedback.toast.success": "Thank you for the feedback."
  },
  "arrays": {
    "welcome.examples": [
      {
        "text": "I'd like to explore templates to see what I can create.",
        "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_142fd6e4e46332d8f41f5aef982448361c0c8c65e.png",
        "backgroundColor": "#F5F5F5"
      },
      {
        "text": "I want to touch up and enhance my photos.",
        "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_1e188097a1bc580b26c8be07d894205c5c6ca5560.png",
        "backgroundColor": "#F5F5F5"
      },
      {
        "text": "I'd like to edit PDFs and make them interactive.",
        "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_1f6fed23045bbbd57fc17dadc3aa06bcc362f84cb.png",
        "backgroundColor": "#F5F5F5"
      },
      {
        "text": "I want to turn my clips into polished videos.",
        "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_16c2ca834ea8f2977296082ae6f55f305a96674ac.png",
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
    "--color-button-submit-hover": "#292929",
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
    "--chat-history-padding-top-expanded": "0",
    "--chat-history-bottom-padding": "0",
    "--message-blocker-height": "105px",
    "--citations-background-color": "#F5F5F5",
    "--citations-text-color": "#292929",
    "--citations-text-font-weight": "700",
    "--citations-desktop-button-font-size": "14px",
    "--feedback-icon-btn-background": "#FFFFFF",
    "--feedback-icon-btn-hover-background": "#FFFFFF",
    "--feedback-icon-btn-size-desktop": "32px",
    "--feedback-container-gap": "4px",
    "--multimodal-card-box-shadow": "none",
    "--border-radius-card": "16px",
    "--button-height-s": "30px",
    "--button-primary-background": "#3B63FB",
    "--button-primary-text": "#FFFFFF",
    "--button-primary-hover": "#274DEA",
    "--button-secondary-border": "#2C2C2C",
    "--button-secondary-text": "#2C2C2C",
    "--button-secondary-hover": "#000000",
    "--color-button-secondary-hover-text": "#FFFFFF",
    "--message-concierge-link-color": "#274DEA"
  }
}
```

**Usage:**

1. Save this JSON file as `theme.json` in `app/src/main/assets/`
2. Load it in your app:

```kotlin
@Composable
fun MyApp() {
    val context = LocalContext.current
    val theme = remember {
        ConciergeThemeLoader.load(context, "theme.json")
            ?: ConciergeThemeLoader.default()
    }
    
    ConciergeTheme(theme = theme) {
        ConciergeChat(/* ... */)
    }
}
```

## 
