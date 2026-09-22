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

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.adobe.marketing.mobile.concierge.Concierge
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChat
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatViewModel
import com.adobe.marketing.mobile.concierge.ui.chat.MockDataHandoffRoutingHints
import com.adobe.marketing.mobile.concierge.ui.chat.createMockDataHandoffChatViewModel
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeLoader
import java.util.UUID

private val SURFACES = listOf("web://dsg-stage-setup.awesome-sites.corp.adobe.com/")

private data class HandoffPreset(
    val label: String,
    val routingHint: String,
    // A supplier rather than a static Map so fields like a freshly minted correlationId are
    // rebuilt on every send instead of reused from one fixed value across taps.
    val xdmFields: () -> Map<String, Any>,
    val localMessage: String?,
    /** Only offered while the mock service is on, which is what simulates the outcome. */
    val mockOnly: Boolean = false,
    /** Reject reason this preset is meant to produce, rendered under [label] on its button. */
    val expectation: String? = null
)

/** The outcome of the most recent [Concierge.sendDataHandoff] call, for the status field. */
private sealed class CompletionStatus(val label: String, val color: Color) {
    object Idle : CompletionStatus("Idle", Color(0xFF888888))
    object Pending : CompletionStatus("Pending...", Color(0xFFF9A825))
    object Accepted : CompletionStatus("Accepted", Color(0xFF2E7D32))
    class Rejected(reason: ConciergeDataHandoffRejectReason?) :
        CompletionStatus("Rejected: ${reason.describe()}", Color(0xFFC62828))
}

private val PRESETS = listOf(
    HandoffPreset(
        label = "Successful Checkout",
        routingHint = "successful-checkout",
        // Nested under "_dsg" -> "coachCheckout" so the merged XDM object's field paths match
        // the _dsg.coachCheckout.* schema. failureReasonCode is omitted - it only applies when
        // status == "failed".
        xdmFields = {
            mapOf(
                "_dsg" to mapOf(
                    "coachCheckout" to mapOf(
                        "correlationId" to UUID.randomUUID().toString(),
                        "status" to "completed",
                        "eCode" to "ECODE-DEMO-001",
                        "confirmationNumber" to "CONF-DEMO-1001",
                        "quantity" to 1
                    )
                )
            )
        },
        localMessage = "Thank you for your purchase!"
    ),
    HandoffPreset(
        label = "Product Interest",
        routingHint = "product-interest",
        xdmFields = {
            mapOf(
                "productListItems" to listOf(
                    mapOf("SKU" to "TEST-SKU-42", "name" to "Trail Running Shoes")
                )
            )
        },
        localMessage = null
    ),
    HandoffPreset(
        label = "Invalid Payload",
        routingHint = "invalid-demo",
        // identityMap is an SDK-reserved top-level XDM key; sending it should be rejected with
        // RESERVED_KEY_COLLISION, exercising the reject path.
        xdmFields = { mapOf("identityMap" to mapOf("email" to listOf(mapOf("id" to "test@example.com")))) },
        localMessage = null,
        expectation = "RESERVED_KEY_COLLISION"
    ),
    // The three below fail *after* the handoff is accepted, which is the part the SDK renders no
    // error UI for: localMessage stays on screen, nothing follows it, and only the completion
    // callback says why. They are the only way to see in the demo what a user is left looking at.
    HandoffPreset(
        label = "Mock: Service Error",
        routingHint = MockDataHandoffRoutingHints.STREAM_ERROR,
        xdmFields = { mapOf("orderId" to "DEMO-FAIL-1001") },
        localMessage = "Thank you for your purchase!",
        mockOnly = true,
        expectation = "DELIVERY_FAILED"
    ),
    HandoffPreset(
        label = "Mock: Empty Response",
        routingHint = MockDataHandoffRoutingHints.EMPTY_RESPONSE,
        xdmFields = { mapOf("orderId" to "DEMO-EMPTY-1001") },
        localMessage = "Thank you for your purchase!",
        mockOnly = true,
        expectation = "EMPTY_RESPONSE"
    ),
    HandoffPreset(
        label = "Mock: Silent Service",
        routingHint = MockDataHandoffRoutingHints.SILENT,
        xdmFields = { mapOf("orderId" to "DEMO-SILENT-1001") },
        localMessage = "Thank you for your purchase!",
        mockOnly = true,
        expectation = "DELIVERY_TIMEOUT"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataHandoffDemoScreen(onBack: () -> Unit) {
    var mockEnabled by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("No handoff sent yet") }
    var completionStatus by remember { mutableStateOf<CompletionStatus>(CompletionStatus.Idle) }
    var sending by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val application = context.applicationContext as Application
    // Recreated (fresh chat session) whenever the switch flips, since mocked and live handoffs
    // run through differently-backed ViewModels.
    val viewModel = viewModel<ConciergeChatViewModel>(
        key = if (mockEnabled) "data-handoff-demo-mock" else "data-handoff-demo-live",
        factory = viewModelFactory {
            initializer {
                if (mockEnabled) {
                    createMockDataHandoffChatViewModel(application)
                } else {
                    ConciergeChatViewModel(application)
                }
            }
        }
    )
    val theme = remember {
        ConciergeThemeLoader.load(context, "themeDefault.json") ?: ConciergeThemeLoader.default()
    }

    // The direct-mode ConciergeChat overload below has no surfaces parameter. Route surfaces in
    // via the dialog-mode wrapper instead, without ever opening its dialog (empty content, showChat
    // unused) - it sets ConciergeStateRepository's surfaces as an unconditional side effect.
    ConciergeTheme(theme = theme) {
        ConciergeChat(viewModel = viewModel, surfaces = SURFACES) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Handoff Demo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
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
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mock Brand Concierge responses",
                        fontSize = 14.sp,
                        color = Color(0xFF333333)
                    )
                    Switch(
                        checked = mockEnabled,
                        onCheckedChange = {
                            mockEnabled = it
                            statusText = "No handoff sent yet"
                            completionStatus = CompletionStatus.Idle
                        }
                    )
                }

                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = Color(0xFF444444),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Completion status: ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF444444)
                    )
                    Text(
                        text = completionStatus.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = completionStatus.color
                    )
                }

                // Two per row, compact: the embedded chat below needs the vertical space more
                // than the buttons do, and the list grows with every failure preset.
                PRESETS.filter { mockEnabled || !it.mockOnly }.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { preset ->
                            PresetButton(
                                preset = preset,
                                enabled = !sending,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    sending = true
                                    statusText = "Sending \"${preset.label}\"..."
                                    completionStatus = CompletionStatus.Pending
                                    Concierge.sendDataHandoff(
                                        routingHint = preset.routingHint,
                                        xdmFields = preset.xdmFields(),
                                        localMessage = preset.localMessage
                                    ) { accepted, rejectReason ->
                                        sending = false
                                        statusText =
                                            "\"${preset.label}\" ${if (accepted) "accepted" else "rejected"}"
                                        completionStatus = if (accepted) {
                                            CompletionStatus.Accepted
                                        } else {
                                            CompletionStatus.Rejected(rejectReason)
                                        }
                                    }
                                }
                            )
                        }
                        // Keeps a lone trailing button half-width instead of stretching it.
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Embedded, always-visible chat session — sendDataHandoff needs an active rendered
            // chat to route into and render its response.
            ConciergeTheme(theme = theme) {
                ConciergeChat(
                    viewModel = viewModel,
                    onClose = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

private fun ConciergeDataHandoffRejectReason?.describe(): String = this?.name ?: "no response"

/**
 * One preset button, sized to share a row with a second one: the label on top, and the reject
 * reason the preset is meant to produce underneath, so the status field above can be read against
 * what was expected.
 */
@Composable
private fun PresetButton(
    preset: HandoffPreset,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = preset.label,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            preset.expectation?.let { expectation ->
                Text(
                    text = expectation,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    color = Color(0xCCFFFFFF),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
