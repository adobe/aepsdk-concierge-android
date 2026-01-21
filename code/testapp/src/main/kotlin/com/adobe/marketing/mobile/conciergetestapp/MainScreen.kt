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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChat
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatViewModel
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeLoader

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var useDemoTheme by remember { mutableStateOf(false) }
    
    // Load theme once, with fallback to default
    val theme = remember(useDemoTheme) {
        val fileName = if (useDemoTheme) "themeDemo.json" else "themeDefault.json"
        ConciergeThemeLoader.load(context, fileName) ?: ConciergeThemeLoader.default()
    }
    
    // Apply theme at the root level
    ConciergeTheme(theme = theme) {
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
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Theme toggle
                Row(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Default Theme",
                        fontSize = 16.sp,
                        color = if (!useDemoTheme) Color(0xFF333333) else Color(0xFF999999),
                        fontWeight = if (!useDemoTheme) FontWeight.SemiBold else FontWeight.Normal
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Switch(
                        checked = useDemoTheme,
                        onCheckedChange = { useDemoTheme = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF5E35B1),
                            checkedTrackColor = Color(0xFF5E35B1).copy(alpha = 0.5f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = "Demo Theme",
                        fontSize = 16.sp,
                        color = if (useDemoTheme) Color(0xFF333333) else Color(0xFF999999),
                        fontWeight = if (useDemoTheme) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))

                // Compose wrapper implementation button
                val viewModel = viewModel<ConciergeChatViewModel>()

                ConciergeChat(modifier = Modifier.fillMaxSize(), viewModel = viewModel) { showChat ->
                    Button(
                        onClick = { showChat() },
                        modifier = Modifier.size(width = 240.dp, height = 60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5E35B1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🗨️ Compose Chat", fontSize = 16.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // XML Integration button
                Button(
                    onClick = { 
                        val intent = Intent(context, XmlChatActivity::class.java)
                        // Pass theme selection to XML activity
                        if (useDemoTheme) {
                            intent.putExtra("theme_file", "themeDemo")
                        } else {
                            intent.putExtra("theme_file", "themeDefault")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(width = 240.dp, height = 60.dp),
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
            }
        }
    }
}