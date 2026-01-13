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

package com.adobe.marketing.mobile.conciergetestapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatView
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeLoader
import com.adobe.marketing.mobile.conciergeapp.R

/**
 * Example Activity showing how to integrate ConciergeChatView into an XML-based application.
 * This demonstrates two integration approaches:
 * 
 * 1. Direct Chat Mode (current): Full-screen chat that shows immediately
 * 2. Dialog Mode (commented): Show a trigger view that opens chat in a dialog
 * 
 * Supports dynamic theme loading via intent extra "theme_file"
 */
class XmlChatActivity : AppCompatActivity() {

    private lateinit var chatView: ConciergeChatView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xml_chat)
        enableEdgeToEdge()

        chatView = findViewById(R.id.concierge_chat)

        // Get theme from intent if provided
        // this is set by the theme toggle in MainScreen.kt
        val themeFileName = intent.getStringExtra("theme_file")
        val theme = themeFileName?.let {
            ConciergeThemeLoader.load(this, it)
        }

        // --- MODE 1: Direct Chat (Full-screen) with optional theme ---
        // Shows the chat interface directly without a wrapper
        chatView.bind(
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            theme = theme,
            onClose = { finish() }
        )

        // --- MODE 2: Dialog-based Chat with optional theme ---
        // Uncomment this and comment out Mode 1 above to test dialog mode
        /*val triggerButton = Button(this).apply {
            text = "Start Chat"
            textSize = 38f
            setPadding(32, 16, 32, 16)
        }

        chatView.bind(
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            theme = theme,
            triggerView = triggerButton
        )*/
    }
}