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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PrivacyNoticeLinkTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.warning(any(), any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `successful launch preserves web and custom scheme intents`() {
        val intents = mutableListOf<Intent>()
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun startActivity(intent: Intent) {
                intents.add(intent)
            }
        }
        val urls = listOf("https://example.com/privacy", "myapp://privacy")

        urls.forEach { assertTrue(openPrivacyNoticeLink(context, it)) }

        assertEquals(urls, intents.map { it.dataString })
        assertTrue(intents.all { it.action == Intent.ACTION_VIEW })
        verify(exactly = 0) { Log.warning(any(), any(), any()) }
    }

    @Test
    fun `missing handlers return false and log without exposing URLs`() {
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun startActivity(intent: Intent) {
                throw ActivityNotFoundException("No handler")
            }
        }

        listOf("/privacy", "unregistered-privacy://policy", "https://example.com/privacy")
            .forEach { assertFalse(openPrivacyNoticeLink(context, it)) }

        verify(exactly = 3) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                "PrivacyNoticeView",
                "Unable to open privacy notice link: no activity can handle the URI."
            )
        }
    }
}
