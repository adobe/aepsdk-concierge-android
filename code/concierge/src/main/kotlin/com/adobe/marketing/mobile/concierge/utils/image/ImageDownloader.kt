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

package com.adobe.marketing.mobile.concierge.utils.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.HttpMethod
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.NetworkRequest
import com.adobe.marketing.mobile.services.ServiceProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Image downloader class that handles downloading and caching images in-memory.
 */
internal class ImageDownloader {

    companion object {
        @Volatile
        private var INSTANCE: ImageDownloader? = null

        fun getInstance(): ImageDownloader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageDownloader().also { INSTANCE = it }
            }
        }
    }

    private val cache: LruCache<String, Bitmap>

    init {
        val maxMemory = Runtime.getRuntime().maxMemory()
        val calculatedCacheSize = (maxMemory / 4).toInt() // Use 1/4th of available memory for cache
        val minCacheSize = 50 * 1024 * 1024 // 50MB cache
        val cacheSize = maxOf(calculatedCacheSize, minCacheSize)


        cache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun entryRemoved(
                evicted: Boolean,
                key: String?,
                oldValue: Bitmap?,
                newValue: Bitmap?
            ) {
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME,
                    "ImageDownloader",
                    "Image removed from cache: $key"
                )
            }
        }
    }

    /**
     * Downloads an image from the given URL.
     * First checks the cache, then downloads if not cached.
     *
     * @param url The URL of the image to download
     * @return The downloaded bitmap or null if download failed
     */
    suspend fun downloadImage(url: String): Bitmap? {
        // Check cache first
        cache.get(url)?.let { cachedBitmap ->
            return cachedBitmap
        }

        return try {
            val request = createImageRequest(url)
            val connection = connect(request)

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Failed to download image: HTTP $responseCode")
            }

            // Download and decode image
            val inputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            connection.close()

            // Cache the bitmap if successfully downloaded
            bitmap?.let {
                cache.put(url, it)
            }
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                "ImageDownloader",
                "Image downloaded and cached from: $url"
            )
            bitmap
        } catch (e: IOException) {
            Log.error(
                ConciergeConstants.EXTENSION_NAME,
                "ImageDownloader",
                "IOException while downloading image: ${e.message}"
            )
            null
        } catch (e: Exception) {
            Log.error(
                ConciergeConstants.EXTENSION_NAME,
                "ImageDownloader",
                "Unexpected error while downloading image: ${e.message}"
            )
            null
        }
    }

    /**
     * Gets a bitmap from cache without downloading
     *
     * @param url The URL of the cached image
     * @return The cached bitmap or null if not in cache
     */
    fun getCachedBitmap(url: String): Bitmap? {
        return cache.get(url)
    }

    /**
     * Creates a GET NetworkRequest for downloading an image.
     *
     * @param url The URL of the image to download
     * @return NetworkRequest configured for image download
     */
    private fun createImageRequest(url: String): NetworkRequest {
        val headers = mapOf(
            "Accept" to "image/*",
            "Cache-Control" to "no-cache"
        )

        return NetworkRequest(
            url,
            HttpMethod.GET,
            null, // No body for GET request
            headers,
            10, // 10 seconds connect timeout
            30  // 30 seconds read timeout
        )
    }

    /**
     * Connects asynchronously using the provided [NetworkRequest] and returns an [HttpConnecting]
     * object upon successful connection.
     *
     * @param request NetworkRequest to use for establishing the connection
     * @throws IOException when connection could not be established
     */
    private suspend fun connect(request: NetworkRequest): HttpConnecting =
        suspendCancellableCoroutine { continuation ->
            val callback = object : NetworkCallback {
                override fun call(connection: HttpConnecting?) {
                    when {
                        connection == null -> continuation.resumeWithException(
                            IOException("Failed to establish connection")
                        )
                        continuation.isActive -> continuation.resume(connection)
                    }
                }
            }

            ServiceProvider.getInstance().networkService.connectAsync(request, callback)

            continuation.invokeOnCancellation {
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME,
                    "ImageDownloader",
                    "Connection cancelled"
                )
            }
        }
}