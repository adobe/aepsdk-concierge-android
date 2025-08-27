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
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatView
import com.adobe.marketing.mobile.conciergeapp.R

/**
 * Example Activity showing how to integrate ConciergeChatView into an XML-based application.
 * This demonstrates the integration approach for apps that use traditional XML layouts.
 */
class XmlChatActivity : AppCompatActivity() {

    private lateinit var chatView: ConciergeChatView
    private lateinit var toggleButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xml_chat)
        enableEdgeToEdge()

        // Get references to views from layout
        chatView = findViewById(R.id.concierge_chat)

        // Bind the chat view - this is where the magic happens!
        chatView.bind(
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            onClose = {
                finish()
//                // Handle close button press
//                chatView.visibility = View.GONE
//                toggleButton.text = "Show Chat"
            }
        )
//
//        // Toggle chat visibility
//        toggleButton.setOnClickListener {
//            if (chatView.isVisible) {
//                chatView.visibility = View.GONE
//                toggleButton.text = "Show Chat"
//            } else {
//                chatView.visibility = View.VISIBLE
//                toggleButton.text = "Hide Chat"
//            }
//        }
    }
}
