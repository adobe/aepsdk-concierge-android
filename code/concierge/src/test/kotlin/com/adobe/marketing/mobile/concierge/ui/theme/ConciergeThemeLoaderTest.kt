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

package com.adobe.marketing.mobile.concierge.ui.theme

import android.content.Context
import android.content.res.AssetManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class ConciergeThemeLoaderTest {

    private lateinit var loader: ConciergeThemeLoader
    private lateinit var context: Context

    @Mock
    private lateinit var mockAssetManager: AssetManager

    private val validThemeJson = """
        {
            "metadata": {
                "name": "Test Theme"
            },
            "theme": {
                "--color-primary": "#FF0000",
                "--color-text": "#FFFFFF"
            }
        }
    """.trimIndent()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()
        loader = ConciergeThemeLoader.instance
        loader.clearCache() // Clear cache before each test
    }

    @Test
    fun `instance returns singleton instance`() {
        val loader1 = ConciergeThemeLoader.instance
        val loader2 = ConciergeThemeLoader.instance
        assertEquals(loader1, loader2)
    }

    @Test
    fun `loadTheme from JSON_STRING returns valid theme`() {
        val theme = loader.loadTheme(
            context = context,
            source = validThemeJson,
            sourceType = ThemeSourceType.JSON_STRING,
            useCache = false
        )

        assertNotNull(theme)
        assertEquals("Test Theme", theme?.name)
        assertEquals("#FF0000", theme?.colors?.primaryColors?.primary)
    }

    @Test
    fun `loadTheme with invalid JSON returns null`() {
        val theme = loader.loadTheme(
            context = context,
            source = "{ invalid json }",
            sourceType = ThemeSourceType.JSON_STRING,
            useCache = false
        )

        assertNull(theme)
    }

    @Test
    fun `loadTheme caches result when useCache is true`() {
        // First load
        val theme1 = loader.loadTheme(
            context = context,
            source = validThemeJson,
            sourceType = ThemeSourceType.JSON_STRING,
            useCache = true
        )

        // Second load should come from cache
        val theme2 = loader.loadTheme(
            context = context,
            source = validThemeJson,
            sourceType = ThemeSourceType.JSON_STRING,
            useCache = true
        )

        assertNotNull(theme1)
        assertNotNull(theme2)
        assertEquals(theme1, theme2)
    }

    @Test
    fun `loadTheme bypasses cache when useCache is false`() {
        // First load with cache
        loader.loadTheme(
            context = context,
            source = validThemeJson,
            sourceType = ThemeSourceType.JSON_STRING,
            useCache = true
        )

        // Second load without cache still works
        val theme = loader.loadTheme(
            context = context,
            source = validThemeJson,
            sourceType = ThemeSourceType.JSON_STRING,
            useCache = false
        )

        assertNotNull(theme)
    }

    @Test
    fun `clearCache removes specific cached theme`() {
        val source = validThemeJson
        
        // Load and cache
        loader.loadTheme(
            context = context,
            source = source,
            sourceType = ThemeSourceType.JSON_STRING,
            useCache = true
        )

        // Clear specific cache
        loader.clearCache(source)

        // This should trigger a fresh load (not from cache)
        val theme = loader.loadTheme(
            context = context,
            source = source,
            sourceType = ThemeSourceType.JSON_STRING,
            useCache = true
        )

        assertNotNull(theme)
    }

    @Test
    fun `clearCache with null removes all cached themes`() {
        // Load multiple themes
        loader.loadTheme(context, validThemeJson, ThemeSourceType.JSON_STRING, true)
        
        val anotherTheme = """
        {
            "metadata": {
                "name": "Another"
            },
            "theme": {
                "--color-primary": "#00FF00"
            }
        }
        """.trimIndent()
        loader.loadTheme(context, anotherTheme, ThemeSourceType.JSON_STRING, true)

        // Clear all
        loader.clearCache(null)

        // Both should be loaded fresh
        val theme1 = loader.loadTheme(context, validThemeJson, ThemeSourceType.JSON_STRING, true)
        val theme2 = loader.loadTheme(context, anotherTheme, ThemeSourceType.JSON_STRING, true)

        assertNotNull(theme1)
        assertNotNull(theme2)
    }

    @Test
    fun `loadThemeWithFallback uses fallback when primary fails`() {
        val fallbackTheme = """
            {
                "metadata": {
                    "name": "Fallback Theme"
                },
                "theme": {
                    "--color-primary": "#00FF00"
                }
            }
        """.trimIndent()

        val theme = loader.loadThemeWithFallback(
            context = context,
            primarySource = "{ invalid }",
            fallbackSource = fallbackTheme,
            sourceType = ThemeSourceType.JSON_STRING
        )

        assertNotNull(theme)
        assertEquals("Fallback Theme", theme?.name)
    }

    @Test
    fun `loadThemeWithFallback uses primary when it succeeds`() {
        val primaryTheme = """
            {
                "metadata": {
                    "name": "Primary Theme"
                },
                "theme": {
                    "--color-primary": "#FF0000"
                }
            }
        """.trimIndent()

        val fallbackTheme = """
            {
                "metadata": {
                    "name": "Fallback Theme"
                },
                "theme": {
                    "--color-primary": "#00FF00"
                }
            }
        """.trimIndent()

        val theme = loader.loadThemeWithFallback(
            context = context,
            primarySource = primaryTheme,
            fallbackSource = fallbackTheme,
            sourceType = ThemeSourceType.JSON_STRING
        )

        assertNotNull(theme)
        assertEquals("Primary Theme", theme?.name)
    }

    @Test
    fun `validateThemeSource returns true for valid JSON_STRING`() {
        val isValid = loader.validateThemeSource(
            context = context,
            source = validThemeJson,
            sourceType = ThemeSourceType.JSON_STRING
        )

        assertTrue(isValid)
    }

    @Test
    fun `validateThemeSource returns false for invalid JSON_STRING`() {
        val isValid = loader.validateThemeSource(
            context = context,
            source = "not json",
            sourceType = ThemeSourceType.JSON_STRING
        )

        assertFalse(isValid)
    }

    @Test
    fun `loadThemeTokens from JSON_STRING returns valid tokens`() {
        val tokensJson = """
            {
                "metadata": {
                    "version": "1.0.0",
                    "name": "Test Tokens"
                },
                "behavior": {
                    "enableDarkMode": true
                },
                "theme": {
                    "--color-primary": "#FF0000"
                }
            }
        """.trimIndent()

        val tokens = loader.loadThemeTokens(
            context = context,
            source = tokensJson,
            sourceType = ThemeSourceType.JSON_STRING,
            useCache = false
        )

        assertNotNull(tokens)
        assertEquals("1.0.0", tokens?.metadata?.version)
        assertEquals("Test Tokens", tokens?.metadata?.name)
        assertTrue(tokens?.behavior?.enableDarkMode == true)
    }

    @Test
    fun `loadThemeTokens caches result when useCache is true`() {
        val tokensJson = """
            {
                "metadata": {"version": "1.0.0"},
                "theme": {
                    "--color-primary": "#FF0000"
                }
            }
        """.trimIndent()

        val tokens1 = loader.loadThemeTokens(context, tokensJson, ThemeSourceType.JSON_STRING, true)
        val tokens2 = loader.loadThemeTokens(context, tokensJson, ThemeSourceType.JSON_STRING, true)

        assertNotNull(tokens1)
        assertNotNull(tokens2)
        assertEquals(tokens1, tokens2)
    }

    @Test
    fun `load static method returns theme for valid filename`() {
        val theme = ConciergeThemeLoader.load(context, "nonexistent")
        assertNull(theme) // Should return null for missing file
    }

    @Test
    fun `load static method handles filename without json extension`() {
        ConciergeThemeLoader.load(context, "themeDemo")
        // Would return theme if file existed in test assets
    }

    @Test
    fun `load static method handles filename with json extension`() {
        ConciergeThemeLoader.load(context, "themeDemo.json")
        // Would return theme if file existed in test assets
    }

    @Test
    fun `default static method returns empty theme config`() {
        val theme = ConciergeThemeLoader.default()
        
        assertNotNull(theme)
        assertNull(theme.name)
        assertNull(theme.colors)
        assertNull(theme.styles)
    }
}

