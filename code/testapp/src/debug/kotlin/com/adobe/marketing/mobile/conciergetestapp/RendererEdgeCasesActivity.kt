/*
 * Copyright 2026 Adobe. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.adobe.marketing.mobile.conciergetestapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.adobe.marketing.mobile.concierge.ui.chat.RendererEdgeCasesDemoScreen
import com.adobe.marketing.mobile.conciergetestapp.theme.ConciergeAppTheme

class RendererEdgeCasesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConciergeAppTheme {
                RendererEdgeCasesDemoScreen(onBack = { finish() })
            }
        }
    }
}
