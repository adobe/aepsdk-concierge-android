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

package com.adobe.marketing.mobile.concierge.ui.state

/**
 * Represents the different states of the input stream (text/voice input).
 */
internal sealed class UserInputState {
    // No input, ready for input
    object Empty : UserInputState()
    
    // User is actively recording audio
    object Recording : UserInputState()
    
    // Processing speech to text conversion
    object Transcribing : UserInputState()

    // Text content (typed or transcribed) received and ready for editing
    data class Editing(val content: String = "") : UserInputState()
    
    // Error state
    data class Error(val message: String) : UserInputState()
}
