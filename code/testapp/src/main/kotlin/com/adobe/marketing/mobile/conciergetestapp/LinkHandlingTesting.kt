/*
 * Copyright 2026 Adobe. All rights reserved.
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChat
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatViewModel

private val SURFACES = listOf(
    "web://brand-concierge-demo-stage.corp.ethos270-stage-va7.ethos.adobe.net/customer-pages/745F37C35E4B776E0A49421B@AdobeOrg/acom_m15/index.html"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkHandlingTesting(onBack: () -> Unit) {
    val viewModel = viewModel<ConciergeChatViewModel>()
    var interceptEnabled by remember { mutableStateOf(false) }
    var closeChatOnIntercept by remember { mutableStateOf(false) }
    var lastInterceptedUrl by remember { mutableStateOf<String?>(null) }

    val handleLink: ((String) -> Boolean)? = if (interceptEnabled) { url ->
        lastInterceptedUrl = url
        if (closeChatOnIntercept) viewModel.closeConcierge()
        true
    } else null

    ConciergeChat(
        viewModel = viewModel,
        surfaces = SURFACES,
        handleLink = handleLink
    ) { showChat ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Link Handling Tests") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF5E35B1),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CHAT LINK INTERCEPTION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Intercept all links", fontSize = 15.sp)
                                Text(
                                    "handleLink returns true for every link",
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = interceptEnabled,
                                onCheckedChange = {
                                    interceptEnabled = it
                                    if (!it) {
                                        closeChatOnIntercept = false
                                        lastInterceptedUrl = null
                                    }
                                }
                            )
                        }
                        if (interceptEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Close chat on intercept", fontSize = 15.sp)
                                Switch(
                                    checked = closeChatOnIntercept,
                                    onCheckedChange = { closeChatOnIntercept = it }
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Button(
                            onClick = { showChat() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1))
                        ) {
                            Text("Open Chat")
                        }
                        if (lastInterceptedUrl != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Last intercepted",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        lastInterceptedUrl!!,
                                        fontSize = 12.sp,
                                        color = Color(0xFF444444)
                                    )
                                }
                                TextButton(onClick = { lastInterceptedUrl = null }) {
                                    Text("Clear", color = Color(0xFF888888))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
