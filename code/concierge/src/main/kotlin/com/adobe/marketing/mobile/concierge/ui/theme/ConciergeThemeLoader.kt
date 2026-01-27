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
import android.util.Log
import java.io.File

/**
 * Thread-safe singleton loader for Concierge theme configurations.
 * Supports loading themes from various sources with caching and fallback mechanisms.
 */
class ConciergeThemeLoader private constructor() {
    private val themeCache = mutableMapOf<String, ConciergeThemeConfig>()
    private val tokenCache = mutableMapOf<String, ConciergeThemeTokens>()

    companion object {
        private const val TAG = "ConciergeThemeLoader"
        
        internal val instance: ConciergeThemeLoader by lazy {
            ConciergeThemeLoader()
        }

        /**
         * Loads a ConciergeTheme from a bundled JSON file
         * @param context Context to access assets
         * @param filename Name of the JSON file (with or without .json extension) in assets
         * @return Decoded ConciergeThemeConfig instance, or null if loading/decoding fails
         */
        @JvmStatic
        fun load(context: Context, filename: String): ConciergeThemeConfig? {
            val fileName = if (filename.endsWith(".json")) filename else "$filename.json"
            return try {
                val inputStream = context.assets.open(fileName)
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                ThemeParser.parseThemeJson(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load theme '$fileName': ${e.message}", e)
                null
            }
        }

        /**
         * Creates a default ConciergeTheme instance
         * @return A ConciergeThemeConfig with all default values (null/empty)
         */
        @JvmStatic
        fun default(): ConciergeThemeConfig {
            return ConciergeThemeConfig()
        }
    }

    /**
     * Load a theme configuration from various sources
     * @param context Context to access assets/files
     * @param source The theme source (asset filename, file path, or JSON string)
     * @param sourceType Type of the source
     * @param useCache Whether to use cached version if available
     * @return Loaded theme configuration or null if loading fails
     */
    fun loadTheme(
        context: Context,
        source: String,
        sourceType: ThemeSourceType = ThemeSourceType.ASSET,
        useCache: Boolean = true
    ): ConciergeThemeConfig? {
        // Check cache first if enabled
        if (useCache && themeCache.containsKey(source)) {
            return themeCache[source]
        }

        val theme = when (sourceType) {
            ThemeSourceType.ASSET -> loadFromAssets(context, source)
            ThemeSourceType.FILE -> loadFromFile(source)
            ThemeSourceType.JSON_STRING -> loadFromJsonString(source)
        }

        // Cache the loaded theme
        theme?.let {
            themeCache[source] = it
        }

        return theme
    }

    /**
     * Load enhanced theme tokens from various sources
     * @param context Context to access assets/files
     * @param source The theme source (asset filename, file path, or JSON string)
     * @param sourceType Type of the source
     * @param useCache Whether to use cached version if available
     * @return Loaded theme tokens or null if loading fails
     */
    fun loadThemeTokens(
        context: Context,
        source: String,
        sourceType: ThemeSourceType = ThemeSourceType.ASSET,
        useCache: Boolean = true
    ): ConciergeThemeTokens? {
        // Check cache first if enabled
        if (useCache && tokenCache.containsKey(source)) {
            return tokenCache[source]
        }

        val jsonString = when (sourceType) {
            ThemeSourceType.ASSET -> readFromAssets(context, source)
            ThemeSourceType.FILE -> readFromFile(source)
            ThemeSourceType.JSON_STRING -> source
        } ?: return null

        val tokens = ThemeParser.parseThemeTokens(jsonString)

        // Cache the loaded tokens
        tokens?.let {
            tokenCache[source] = it
        }

        return tokens
    }

    /**
     * Load theme with fallback support
     * @param context Context to access assets/files
     * @param primarySource Primary theme source
     * @param fallbackSource Fallback theme source if primary fails
     * @param sourceType Type of the sources
     * @return Loaded theme configuration or null if both fail
     */
    fun loadThemeWithFallback(
        context: Context,
        primarySource: String,
        fallbackSource: String,
        sourceType: ThemeSourceType = ThemeSourceType.ASSET
    ): ConciergeThemeConfig? {
        return loadTheme(context, primarySource, sourceType) 
            ?: run {
                Log.w(TAG, "Primary theme failed, trying fallback: $fallbackSource")
                loadTheme(context, fallbackSource, sourceType)
            }
    }

    /**
     * Clear cached themes
     * @param source Specific source to clear, or null to clear all
     */
    fun clearCache(source: String? = null) {
        if (source != null) {
            themeCache.remove(source)
            tokenCache.remove(source)
        } else {
            themeCache.clear()
            tokenCache.clear()
        }
    }

    /**
     * Validate if a theme can be loaded from the source
     * @param context Context to access assets
     * @param source Theme source
     * @param sourceType Type of the source
     * @return true if theme can be loaded, false otherwise
     */
    fun validateThemeSource(
        context: Context,
        source: String,
        sourceType: ThemeSourceType = ThemeSourceType.ASSET
    ): Boolean {
        return try {
            when (sourceType) {
                ThemeSourceType.ASSET -> {
                    context.assets.list("")?.contains(source) == true
                }
                ThemeSourceType.FILE -> {
                    File(source).exists() && File(source).canRead()
                }
                ThemeSourceType.JSON_STRING -> {
                    source.isNotEmpty() && source.trim().startsWith("{")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating theme source: $source", e)
            false
        }
    }

    /**
     * Get list of available themes in assets folder
     * @param context Context to access assets
     * @param directory Assets directory to search (empty string for root)
     * @return List of theme file names
     */
    fun getAvailableThemes(context: Context, directory: String = ""): List<String> {
        return try {
            context.assets.list(directory)
                ?.filter { it.endsWith(".json") }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error listing available themes", e)
            emptyList()
        }
    }

    // Private helper methods

    private fun loadFromAssets(context: Context, fileName: String): ConciergeThemeConfig? {
        return ThemeParser.loadThemeFromAssets(context, fileName)
    }

    private fun loadFromFile(filePath: String): ConciergeThemeConfig? {
        return try {
            val jsonString = File(filePath).readText()
            ThemeParser.parseThemeJson(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load theme from file: $filePath", e)
            null
        }
    }

    private fun loadFromJsonString(jsonString: String): ConciergeThemeConfig? {
        return ThemeParser.parseThemeJson(jsonString)
    }

    private fun readFromAssets(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read from assets: $fileName", e)
            null
        }
    }

    private fun readFromFile(filePath: String): String? {
        return try {
            File(filePath).readText()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read from file: $filePath", e)
            null
        }
    }
}

/**
 * Enum representing the type of theme source
 */
enum class ThemeSourceType {
    /**
     * Theme is loaded from app assets folder
     */
    ASSET,
    
    /**
     * Theme is loaded from a file path
     */
    FILE,
    
    /**
     * Theme is provided as a JSON string
     */
    JSON_STRING
}

