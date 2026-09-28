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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.adobe.marketing.mobile.concierge.utils.image.ImageProvider
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gallery stand-in for the SDK's internal image provider (only `ConciergeChat` supplies one).
 * Decodes downsampled to [targetPx] and caches by bytes, which is what a production
 * customer-supplied provider should also do for menu-sized image counts.
 */
internal class GalleryImageProvider(private val targetPx: Int = 512) : ImageProvider {

    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    override fun getCached(url: String): Bitmap? = cache.get(url)

    override suspend fun get(url: String): Bitmap {
        cache.get(url)?.let { return it }
        val bytes = withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
                connection.inputStream.use { it.readBytes() }
            } finally {
                connection.disconnect()
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("Failed to decode $url")
        cache.put(url, bitmap)
        return bitmap
    }

    override fun clear() {
        cache.evictAll()
    }
}
