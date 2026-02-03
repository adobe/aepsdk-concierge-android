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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
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
    var selectedTheme by remember { mutableStateOf("default") }
    
    // Theme options
    val themeOptions = listOf(
        ThemeOption("default", "Default Theme", "Standard theme"),
        ThemeOption("demo", "Demo Theme", "Blue-themed demo"),
        ThemeOption("input field border", "Input Field Borders Test", "Configure input field borders"),
        ThemeOption("behaviors disabled", "Behavior Test", "No voice input")
    )
    
    // Load theme once, with fallback to default
    val theme = remember(selectedTheme) {
        val fileName = when (selectedTheme) {
            "demo" -> "themeDemo.json"
            "input field border" -> "theme-test-implementation.json"
            "behaviors disabled" -> "theme-behavior-disabled.json"
            else -> "themeDSG.json"
        }
        // Load complete theme (config + tokens) from JSON file
        ConciergeThemeLoader.load(context, fileName) ?: ConciergeThemeLoader.default()
    }
    
    // Apply theme at the root level - clean and simple!
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
                    text = "Choose your theme and integration",
                    fontSize = 16.sp,
                    color = Color(0xFF666666)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Theme selector
                ThemeSelector(
                    selectedTheme = selectedTheme,
                    themeOptions = themeOptions,
                    onThemeSelected = { selectedTheme = it }
                )
                
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
                        val themeFile = when (selectedTheme) {
                            "demo" -> "themeDemo"
                            "input field border" -> "theme-test-implementation"
                            "behaviors disabled" -> "theme-behavior-disabled"
                            else -> "themeDSG"
                        }
                        intent.putExtra("theme_file", themeFile)
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

/**
 * Data class representing a theme option
 */
data class ThemeOption(
    val id: String,
    val name: String,
    val description: String
)

/**
 * Theme selector component with dropdown-style UI
 */
@Composable
fun ThemeSelector(
    selectedTheme: String,
    themeOptions: List<ThemeOption>,
    onThemeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = themeOptions.find { it.id == selectedTheme } ?: themeOptions[0]
    
    Column(
        modifier = Modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Theme:",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Dropdown button
        Box {
            Button(
                onClick = { expanded = !expanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                modifier = Modifier
                    .width(280.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE0E0E0))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = selectedOption.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = selectedOption.description,
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                    
                    Icon(
                        painter = painterResource(
                            id = if (expanded) 
                                android.R.drawable.arrow_up_float
                            else 
                                android.R.drawable.arrow_down_float
                        ),
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Dropdown menu
            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(280.dp)
            ) {
                themeOptions.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = option.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (option.id == selectedTheme) 
                                        FontWeight.Bold 
                                    else 
                                        FontWeight.Normal,
                                    color = if (option.id == selectedTheme)
                                        Color(0xFF5E35B1)
                                    else
                                        Color(0xFF333333)
                                )
                                Text(
                                    text = option.description,
                                    fontSize = 12.sp,
                                    color = Color(0xFF999999)
                                )
                            }
                        },
                        onClick = {
                            onThemeSelected(option.id)
                            expanded = false
                        },
                        leadingIcon = {
                            if (option.id == selectedTheme) {
                                Icon(
                                    painter = painterResource(android.R.drawable.checkbox_on_background),
                                    contentDescription = "Selected",
                                    tint = Color(0xFF5E35B1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )
                    
                    if (option != themeOptions.last()) {
                        androidx.compose.material3.HorizontalDivider(
                            color = Color(0xFFE0E0E0),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}