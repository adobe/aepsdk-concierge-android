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

package com.adobe.marketing.mobile.concierge.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.os.Build
import com.adobe.marketing.mobile.services.Log
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
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
import org.robolectric.annotation.Config

/**
 * Tests for [tryOpenWithSystemHandler] and the early-exit paths of [tryOpenAsAppLink]
 * that are API-version agnostic.
 */
@RunWith(RobolectricTestRunner::class)
class AppLinkUtilsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.debug(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // ---- tryOpenWithSystemHandler ----

    @Test
    fun `tryOpenWithSystemHandler calls startActivity with FLAG_ACTIVITY_NEW_TASK`() {
        val intentSlot = slot<Intent>()
        every { context.startActivity(capture(intentSlot)) } just Runs

        tryOpenWithSystemHandler(context, "tel:+15555550100")

        verify { context.startActivity(any()) }
        assertTrue(
            "Expected FLAG_ACTIVITY_NEW_TASK to be set",
            intentSlot.captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0
        )
    }

    @Test
    fun `tryOpenWithSystemHandler does not throw and logs when startActivity throws`() {
        every { context.startActivity(any()) } throws ActivityNotFoundException("no handler")

        tryOpenWithSystemHandler(context, "tel:+15555550100") // must not propagate exception

        verify { Log.debug(any(), any(), match { it.contains("tryOpenWithSystemHandler failed") }) }
    }

    @Test
    fun `tryOpenWithSystemHandler routes a geo URL to the system handler with FLAG_ACTIVITY_NEW_TASK`() {
        val geoUrl = "geo:0,0?q=The%20Mall%20At%20Robinson%2C%20Pittsburgh%2C%20PA%2015205-4834"
        val intentSlot = slot<Intent>()
        every { context.startActivity(capture(intentSlot)) } just Runs

        tryOpenWithSystemHandler(context, geoUrl)

        verify { context.startActivity(any()) }
        val captured = intentSlot.captured
        assertTrue(
            "Expected FLAG_ACTIVITY_NEW_TASK to be set",
            captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0
        )
        assertTrue(captured.data.toString().startsWith("geo:"))
    }

    // ---- tryOpenAsAppLink: early exits (API-agnostic) ----

    @Test
    fun `tryOpenAsAppLink returns false for blank URL`() {
        assertFalse(tryOpenAsAppLink(context, ""))
        assertFalse(tryOpenAsAppLink(context, "   "))
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `tryOpenAsAppLink returns false for URL with no host`() {
        // tel: URIs have no host; function must return false before any API-version check
        assertFalse(tryOpenAsAppLink(context, "tel:+15555550100"))
        verify(exactly = 0) { context.startActivity(any()) }
    }
}

/**
 * Tests for [tryOpenAsAppLink] on API 31+ (DomainVerificationManager path).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class AppLinkUtilsApi31Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.debug(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `tryOpenAsAppLink returns true and opens when domain state is VERIFIED`() {
        setupDomainVerification("example.com", DomainVerificationUserState.DOMAIN_STATE_VERIFIED)

        assertTrue(tryOpenAsAppLink(context, "https://example.com/page"))
        verify { context.startActivity(any()) }
    }

    @Test
    fun `tryOpenAsAppLink returns true and opens when domain state is SELECTED`() {
        setupDomainVerification("example.com", DomainVerificationUserState.DOMAIN_STATE_SELECTED)

        assertTrue(tryOpenAsAppLink(context, "https://example.com/page"))
        verify { context.startActivity(any()) }
    }

    @Test
    fun `tryOpenAsAppLink returns false and does not open when domain state is NONE`() {
        setupDomainVerification("example.com", DomainVerificationUserState.DOMAIN_STATE_NONE)

        assertFalse(tryOpenAsAppLink(context, "https://example.com/page"))
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `tryOpenAsAppLink returns false when host is absent from hostToStateMap`() {
        setupDomainVerification("other.com", DomainVerificationUserState.DOMAIN_STATE_VERIFIED)

        assertFalse(tryOpenAsAppLink(context, "https://example.com/page"))
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `tryOpenAsAppLink returns false when DomainVerificationManager is null`() {
        every { context.getSystemService(DomainVerificationManager::class.java) } returns null

        assertFalse(tryOpenAsAppLink(context, "https://example.com/page"))
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `tryOpenAsAppLink returns false and logs when getDomainVerificationUserState throws`() {
        val dvm = mockk<DomainVerificationManager>()
        every { context.getSystemService(DomainVerificationManager::class.java) } returns dvm
        every { context.packageName } returns "com.example.app"
        every { dvm.getDomainVerificationUserState(any()) } throws SecurityException("not allowed")

        assertFalse(tryOpenAsAppLink(context, "https://example.com/page"))
        verify { Log.debug(any(), any(), match { it.contains("getDomainVerificationUserState failed") }) }
    }

    @Test
    fun `tryOpenAsAppLink returns false and logs when startActivity throws`() {
        setupDomainVerification("example.com", DomainVerificationUserState.DOMAIN_STATE_VERIFIED)
        every { context.startActivity(any()) } throws ActivityNotFoundException("no activity")

        assertFalse(tryOpenAsAppLink(context, "https://example.com/page"))
        verify { Log.debug(any(), any(), match { it.contains("tryOpenAsAppLink failed") }) }
    }

    @Test
    fun `tryOpenAsAppLink launches only the host package as a browsable intent`() {
        setupDomainVerification("example.com", DomainVerificationUserState.DOMAIN_STATE_VERIFIED)
        val intentSlot = slot<Intent>()
        every { context.startActivity(capture(intentSlot)) } just Runs

        assertTrue(tryOpenAsAppLink(context, "https://example.com/page"))

        assertEquals("com.example.app", intentSlot.captured.`package`)
        assertTrue(intentSlot.captured.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun `tryOpenAsAppLink returns false when the host app declares no activity for the URL path`() {
        setupDomainVerification("example.com", DomainVerificationUserState.DOMAIN_STATE_VERIFIED, hasActivity = false)

        assertFalse(tryOpenAsAppLink(context, "https://example.com/unhandled"))
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `tryOpenAsAppLink returns false when the user turned off link handling`() {
        setupDomainVerification(
            "example.com",
            DomainVerificationUserState.DOMAIN_STATE_VERIFIED,
            linkHandlingAllowed = false
        )

        assertFalse(tryOpenAsAppLink(context, "https://example.com/page"))
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `tryOpenAsAppLink matches a verified wildcard domain`() {
        setupDomainVerification("*.example.com", DomainVerificationUserState.DOMAIN_STATE_VERIFIED)

        assertTrue(tryOpenAsAppLink(context, "https://shop.EXAMPLE.com/page"))
        verify { context.startActivity(any()) }
    }

    private fun setupDomainVerification(
        host: String,
        state: Int,
        linkHandlingAllowed: Boolean = true,
        hasActivity: Boolean = true
    ) {
        val dvm = mockk<DomainVerificationManager>()
        val userState = mockk<DomainVerificationUserState>()
        val pm = mockk<PackageManager>()
        every { context.getSystemService(DomainVerificationManager::class.java) } returns dvm
        every { context.packageName } returns "com.example.app"
        every { context.packageManager } returns pm
        every { pm.queryIntentActivities(any<Intent>(), PackageManager.MATCH_DEFAULT_ONLY) } returns
            if (hasActivity) listOf(ResolveInfo()) else emptyList()
        every { dvm.getDomainVerificationUserState("com.example.app") } returns userState
        every { userState.isLinkHandlingAllowed } returns linkHandlingAllowed
        every { userState.hostToStateMap } returns mapOf(host to state)
    }
}

/**
 * Tests for [tryOpenAsAppLink] on API 30 and below (PackageManager.resolveActivity path).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class AppLinkUtilsApi30Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.debug(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `tryOpenAsAppLink returns true and opens when resolveActivity matches packageName`() {
        val resolveInfo = resolveInfoFor("com.example.app")
        val pm = mockk<PackageManager>()
        every { context.packageManager } returns pm
        every { context.packageName } returns "com.example.app"
        every { pm.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) } returns resolveInfo
        every { pm.queryIntentActivities(any<Intent>(), PackageManager.MATCH_DEFAULT_ONLY) } returns listOf(resolveInfo)
        val intentSlot = slot<Intent>()
        every { context.startActivity(capture(intentSlot)) } just Runs

        assertTrue(tryOpenAsAppLink(context, "https://example.com/page"))
        assertEquals("com.example.app", intentSlot.captured.`package`)
    }

    @Test
    fun `tryOpenAsAppLink returns false when resolveActivity matches a different package`() {
        val resolveInfo = resolveInfoFor("com.browser.app")
        val pm = mockk<PackageManager>()
        every { context.packageManager } returns pm
        every { context.packageName } returns "com.example.app"
        every { pm.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) } returns resolveInfo

        assertFalse(tryOpenAsAppLink(context, "https://example.com/page"))
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `tryOpenAsAppLink returns false when resolveActivity returns null`() {
        val pm = mockk<PackageManager>()
        every { context.packageManager } returns pm
        every { pm.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY) } returns null

        assertFalse(tryOpenAsAppLink(context, "https://example.com/page"))
        verify(exactly = 0) { context.startActivity(any()) }
    }

    private fun resolveInfoFor(packageName: String): ResolveInfo {
        val activityInfo = ActivityInfo().apply { this.packageName = packageName }
        return ResolveInfo().apply { this.activityInfo = activityInfo }
    }
}

/**
 * Tests for [tryOpenInNativeApp] on API 30+ (FLAG_ACTIVITY_REQUIRE_NON_BROWSER available).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class AppLinkUtilsNativeAppTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.debug(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `tryOpenInNativeApp starts a browsable non-browser intent and returns true`() {
        val intentSlot = slot<Intent>()
        every { context.startActivity(capture(intentSlot)) } just Runs

        assertTrue(tryOpenInNativeApp(context, "https://www.google.com/maps/dir/?api=1"))

        val intent = intentSlot.captured
        assertTrue(intent.action == Intent.ACTION_VIEW)
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `tryOpenInNativeApp returns false when no non-browser app handles the URL`() {
        every { context.startActivity(any()) } throws ActivityNotFoundException()

        assertFalse(tryOpenInNativeApp(context, "https://example.com/page"))
    }

    @Test
    fun `tryOpenInNativeApp never hands cleartext http to another app`() {
        assertFalse(tryOpenInNativeApp(context, "http://example.com/page"))
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `tryOpenInNativeApp never launches non-web schemes`() {
        assertFalse(tryOpenInNativeApp(context, "intent://maps#Intent;package=com.google.android.apps.maps;end"))
        assertFalse(tryOpenInNativeApp(context, "tel:+15555550100"))
        verify(exactly = 0) { context.startActivity(any()) }
    }
}

/**
 * Tests for [tryOpenInNativeApp] below API 30, where the WebView is used as before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AppLinkUtilsNativeAppApi29Test {

    @Test
    fun `tryOpenInNativeApp returns false without launching on Android 10`() {
        val context = mockk<Context>(relaxed = true)

        assertFalse(tryOpenInNativeApp(context, "https://www.google.com/maps/dir/?api=1"))
        verify(exactly = 0) { context.startActivity(any()) }
    }
}
