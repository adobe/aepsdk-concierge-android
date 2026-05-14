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

package com.adobe.marketing.mobile.concierge.ui.theme

import android.content.Context
import android.content.res.AssetManager
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConciergeFontResolverTest {

    private lateinit var context: Context
    private lateinit var assetManager: AssetManager

    @Before
    fun setup() {
        context = mock(Context::class.java)
        assetManager = mock(AssetManager::class.java)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.assets).thenReturn(assetManager)
        ConciergeFontResolver.clearCaches()
    }

    @After
    fun tearDown() {
        ConciergeFontResolver.clearCaches()
    }

    private fun setFontAssets(vararg fileNames: String) {
        `when`(assetManager.list("fonts")).thenReturn(fileNames)
    }

    @Test
    fun `resolve returns null when spec is null`() {
        setFontAssets("anything.ttf")
        val result = ConciergeFontResolver.resolve(context, null)
        assertNull(result)
    }

    @Test
    fun `resolve returns null when spec has no slots set`() {
        setFontAssets("MyFont.ttf")
        val result = ConciergeFontResolver.resolve(context, ConciergeFontFamilySpec())
        assertNull(result)
    }

    @Test
    fun `resolve returns null when fonts dir is missing`() {
        `when`(assetManager.list("fonts")).thenReturn(null)
        val spec = ConciergeFontFamilySpec(regular = "MyFont")
        val result = ConciergeFontResolver.resolve(context, spec)
        assertNull(result)
    }

    @Test
    fun `resolve returns null when none of declared slot files exist`() {
        setFontAssets("OtherFont.ttf", "Unrelated.otf")
        val spec = ConciergeFontFamilySpec(regular = "MissingFont", bold = "MissingBold")
        val result = ConciergeFontResolver.resolve(context, spec)
        assertNull(result)
    }

    @Test
    fun `resolve returns non-null FontFamily when at least one slot matches a ttf asset`() {
        setFontAssets("MyFont.ttf")
        val spec = ConciergeFontFamilySpec(regular = "MyFont")
        val result = ConciergeFontResolver.resolve(context, spec)
        assertNotNull(result)
    }

    @Test
    fun `resolve falls back to otf when ttf is not present`() {
        setFontAssets("MyFont.otf")
        val spec = ConciergeFontFamilySpec(regular = "MyFont")
        val result = ConciergeFontResolver.resolve(context, spec)
        assertNotNull(result)
    }

    @Test
    fun `resolve matches font filenames case-insensitively`() {
        setFontAssets("myfont.TTF")
        val spec = ConciergeFontFamilySpec(regular = "MyFont")
        val result = ConciergeFontResolver.resolve(context, spec)
        assertNotNull(result)
    }

    @Test
    fun `resolve drops missing slots and resolves the rest`() {
        setFontAssets("MyFont_Regular.ttf")
        val spec = ConciergeFontFamilySpec(
            regular = "MyFont_Regular",
            bold = "MyFont_Bold_Missing"
        )
        val result = ConciergeFontResolver.resolve(context, spec)
        assertNotNull(result)
    }

    @Test
    fun `resolve ignores blank slot basenames`() {
        setFontAssets("MyFont.ttf")
        val spec = ConciergeFontFamilySpec(regular = "MyFont", bold = "   ")
        val result = ConciergeFontResolver.resolve(context, spec)
        assertNotNull(result)
    }

    @Test
    fun `resolve caches the FontFamily and returns the same instance on repeat`() {
        setFontAssets("MyFont.ttf")
        val spec = ConciergeFontFamilySpec(regular = "MyFont")

        val first = ConciergeFontResolver.resolve(context, spec)
        val second = ConciergeFontResolver.resolve(context, spec)

        assertNotNull(first)
        assertSame(first, second)
    }

    @Test
    fun `resolve remembers misses and keeps returning null without re-listing`() {
        setFontAssets("OtherFont.ttf")
        val spec = ConciergeFontFamilySpec(regular = "MissingFont")

        val first = ConciergeFontResolver.resolve(context, spec)
        val second = ConciergeFontResolver.resolve(context, spec)

        assertNull(first)
        assertNull(second)
    }

    @Test
    fun `resolve handles AssetManager list throwing gracefully`() {
        `when`(assetManager.list("fonts")).thenThrow(RuntimeException("io"))
        val spec = ConciergeFontFamilySpec(regular = "MyFont")
        val result = ConciergeFontResolver.resolve(context, spec)
        assertNull(result)
    }
}
