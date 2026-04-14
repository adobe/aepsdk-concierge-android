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
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.os.Build
import com.adobe.marketing.mobile.services.Log
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
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
    fun `tryOpenWithSystemHandler calls startActivity for the given URL`() {
        tryOpenWithSystemHandler(context, "tel:+15555550100")

        verify { context.startActivity(any()) }
    }

    @Test
    fun `tryOpenWithSystemHandler does not throw and logs when startActivity throws`() {
        every { context.startActivity(any()) } throws ActivityNotFoundException("no handler")

        tryOpenWithSystemHandler(context, "tel:+15555550100") // must not propagate exception

        verify { Log.debug(any(), any(), match { it.contains("tryOpenWithSystemHandler failed") }) }
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

    private fun setupDomainVerification(host: String, state: Int) {
        val dvm = mockk<DomainVerificationManager>()
        val userState = mockk<DomainVerificationUserState>()
        every { context.getSystemService(DomainVerificationManager::class.java) } returns dvm
        every { context.packageName } returns "com.example.app"
        every { dvm.getDomainVerificationUserState("com.example.app") } returns userState
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

        assertTrue(tryOpenAsAppLink(context, "https://example.com/page"))
        verify { context.startActivity(any()) }
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
