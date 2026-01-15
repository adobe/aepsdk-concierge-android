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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Complete color definitions for Concierge UI components.
 * All colors are explicitly defined for full brand control.
 */
@Immutable
data class ConciergeColors(
    // Primary colors
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,

    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,

    val background: Color,

    val container: Color,

    val outline: Color,

    val error: Color,
    val onError: Color,
    
    // Message-specific colors (from CSS themes)
    val userMessageBackground: Color? = null,
    val userMessageText: Color? = null,
    val conciergeMessageBackground: Color? = null,
    val conciergeMessageText: Color? = null,
    val messageConciergeLink: Color? = null,

    // Button-specific colors (from CSS themes)
    val buttonPrimaryBackground: Color? = null,
    val buttonPrimaryText: Color? = null,
    val buttonPrimaryHover: Color? = null,
    val buttonSecondaryBorder: Color? = null,
    val buttonSecondaryText: Color? = null,
    val buttonSecondaryHover: Color? = null,
    val buttonSecondaryHoverText: Color? = null,
    val buttonSubmitFill: Color? = null,
    val buttonSubmitText: Color? = null,
    val buttonDisabled: Color? = null,

    // Input-specific colors (from CSS themes)
    val inputBackground: Color? = null,
    val inputText: Color? = null,
    val inputOutline: Color? = null,
    val inputOutlineFocus: Color? = null,

    // Feedback-specific colors (from CSS themes)
    val feedbackIconButtonBackground: Color? = null,
    val feedbackIconButtonHoverBackground: Color? = null,

    // Citation/Disclaimer colors (from CSS themes)
    val citationBackground: Color? = null,
    val citationText: Color? = null,
    val disclaimerColor: Color? = null
)

/**
 * Light mode color scheme - Soft pastel blue palette
 */
val LightConciergeColors = ConciergeColors(
    primary = Color(0xFF3949AB),
    onPrimary = Color.White,
    secondary = Color(0xFFE7E4F3),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    background = Color.White,
    container = Color(0xFFF5F5F5),
    outline = Color(0xFFC4EBFF),
    error = Color(0xFFE99B9B),
    onError = Color(0xFF3D1515)
)

/**
 * Dark mode color scheme
 */
val DarkConciergeColors = ConciergeColors(
    primary = Color(0xFF1E88E5),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF03DAC6),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    background = Color(0xFF1C1B1F),
    container = Color(0xFF2B2930),
    outline = Color(0xFF938F99),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)
