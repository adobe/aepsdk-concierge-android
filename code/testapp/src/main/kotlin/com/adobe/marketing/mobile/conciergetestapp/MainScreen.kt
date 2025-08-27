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

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.conciergetestapp.ui.ChatScreen
import com.adobe.marketing.mobile.conciergetestapp.ui.MarkdownDemoScreen

@Composable
fun MainScreenWrapper() {
    val showChat = rememberSaveable { mutableStateOf(false) }
    val showMarkdownDemo = rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main screen with chat button
        MainScreen(
            onStartChat = { showChat.value = true },
            onShowMarkdownDemo = { showMarkdownDemo.value = true }
        )

        if (showChat.value) {
            ChatScreen(
                modifier = Modifier.fillMaxSize().background(Color.White),
                onClose = { showChat.value = false },
            )
        }

        // Markdown demo modal sheet
        if (showMarkdownDemo.value) {
            ModalBottomSheet(
                onDismissRequest = { showMarkdownDemo.value = false },
                sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true
                ),
                containerColor = Color.White,
                dragHandle = null,
                modifier = Modifier.fillMaxSize()
            ) {
                MarkdownDemoScreen(
                    modifier = Modifier.fillMaxSize(),
                    onClose = { showMarkdownDemo.value = false }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onStartChat: () -> Unit,
    onShowMarkdownDemo: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App title
            Text(
                text = "Concierge Test App",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtitle
            Text(
                text = "Choose your integration approach",
                fontSize = 16.sp,
                color = Color(0xFF666666)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Compose Chat button
            Button(
                onClick = { onStartChat() },
                modifier = Modifier
                    .size(width = 200.dp, height = 60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "💬 Compose Chat",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // XML Integration button
            Button(
                onClick = { 
                    val intent = Intent(context, XmlChatActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .size(width = 200.dp, height = 60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "📝 XML Integration",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            
            // Chat button label
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start Chat",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Markdown demo button
            Button(
                onClick = { onShowMarkdownDemo() },
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(40.dp)
            ) {
                Text(
                    text = "📝",
                    fontSize = 24.sp
                )
            }
            
            // Markdown demo button label
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Markdown Demo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666)
            )
        }
    }
}