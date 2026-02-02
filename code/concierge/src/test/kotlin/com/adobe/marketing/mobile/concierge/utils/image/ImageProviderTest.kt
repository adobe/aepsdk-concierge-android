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

package com.adobe.marketing.mobile.concierge.utils.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.NetworkRequest
import com.adobe.marketing.mobile.services.Networking
import com.adobe.marketing.mobile.services.ServiceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.time.ExperimentalTime

@ExperimentalTime
@RunWith(RobolectricTestRunner::class)
class ImageProviderTest {

    private lateinit var imageProvider: DefaultImageProvider
    private lateinit var mockNetworkService: Networking
    private lateinit var mockServiceProvider: ServiceProvider
    private lateinit var mockBitmap: Bitmap

    @Before
    fun setup() {
        mockNetworkService = mockk(relaxed = true)
        mockServiceProvider = mockk(relaxed = true)
        mockBitmap = mockk(relaxed = true)

        mockkStatic(ServiceProvider::class)
        every { ServiceProvider.getInstance() } returns mockServiceProvider
        every { mockServiceProvider.networkService } returns mockNetworkService

        imageProvider = DefaultImageProvider(maxEntries = 64)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getCached should return null for non-existent URL`() {
        val result = imageProvider.getCached("http://example.com/image.png")

        assertNull("Should return null for non-cached image", result)
    }

    @Test
    fun `getCached should return cached bitmap after successful download`() = runTest {
        val url = "http://example.com/image.png"
        val mockConnection = mockSuccessfulConnection(mockBitmap)
        setupNetworkServiceCallback(mockConnection)

        // Download image first to populate cache
        imageProvider.get(url)

        val cachedResult = imageProvider.getCached(url)

        assertNotNull("Should return cached bitmap", cachedResult)
        assertEquals("Should return same bitmap instance", mockBitmap, cachedResult)
    }

    @Test
    fun `get should download and cache image on first call`() = runTest {
        val url = "http://example.com/image.png"
        val mockConnection = mockSuccessfulConnection(mockBitmap)
        setupNetworkServiceCallback(mockConnection)

        val result = imageProvider.get(url)

        assertNotNull("Should return bitmap", result)
        assertEquals("Should return correct bitmap", mockBitmap, result)
        verify(exactly = 1) { mockNetworkService.connectAsync(any(), any()) }

        // Verify it's cached
        val cachedResult = imageProvider.getCached(url)
        assertNotNull("Image should be cached", cachedResult)
        assertEquals("Cached image should match downloaded image", mockBitmap, cachedResult)
    }

    @Test
    fun `get should return cached bitmap without downloading on second call`() = runTest {
        val url = "http://example.com/image.png"
        val mockConnection = mockSuccessfulConnection(mockBitmap)
        setupNetworkServiceCallback(mockConnection)

        // First call - downloads
        imageProvider.get(url)

        // Act - Second call should use cache
        val result = imageProvider.get(url)

        assertNotNull("Should return cached bitmap", result)
        assertEquals("Should return same bitmap", mockBitmap, result)
        // Verify network was only called once (not twice)
        verify(exactly = 1) { mockNetworkService.connectAsync(any(), any()) }
    }

    @Test
    fun `get should throw IOException when connection is null`() {
        val url = "http://example.com/image.png"
        setupNetworkServiceCallback(null)

        // Act & Assert
        val exception = assertThrows(IOException::class.java) {
            runTest {
                imageProvider.get(url)
            }
        }
        assertEquals("Failed to establish connection", exception.message)
    }

    @Test
    fun `get should throw IOException when response code is not 2xx`() {
        val url = "http://example.com/image.png"
        val mockConnection = mockk<HttpConnecting>(relaxed = true)
        every { mockConnection.responseCode } returns 404
        setupNetworkServiceCallback(mockConnection)

        // Act & Assert
        val exception = assertThrows(IOException::class.java) {
            runTest {
                imageProvider.get(url)
            }
        }
        assertEquals("Failed to download image: HTTP 404", exception.message)
    }

    @Test
    fun `get should throw IOException when bitmap decode fails`() {
        val url = "http://example.com/image.png"
        val mockConnection = mockk<HttpConnecting>(relaxed = true)
        every { mockConnection.responseCode } returns 200
        every { mockConnection.inputStream } returns ByteArrayInputStream(ByteArray(0))

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns null

        setupNetworkServiceCallback(mockConnection)

        // Act & Assert
        val exception = assertThrows(IOException::class.java) {
            runTest {
                imageProvider.get(url)
            }
        }
        assertEquals("Failed to decode image from URL: $url", exception.message)
    }

    @Test
    fun `get should throw IOException when network throws exception`() {
        val url = "http://example.com/image.png"
        val callbackSlot = slot<NetworkCallback>()
        every {
            mockNetworkService.connectAsync(any(), capture(callbackSlot))
        } answers {
            // Simulate immediate connection
            val mockConnection = mockk<HttpConnecting>(relaxed = true)
            every { mockConnection.responseCode } throws IOException("Network error")
            callbackSlot.captured.call(mockConnection)
        }

        // Act & Assert
        val exception = assertThrows(IOException::class.java) {
            runTest {
                imageProvider.get(url)
            }
        }
        assertEquals("Network error", exception.message)
    }

    @Test
    fun `get should handle multiple different URLs`() = runTest {
        val url1 = "http://example.com/image1.png"
        val url2 = "http://example.com/image2.png"
        val mockBitmap1 = mockk<Bitmap>(relaxed = true)
        val mockBitmap2 = mockk<Bitmap>(relaxed = true)

        setupNetworkServiceCallbackForMultipleUrls(
            mapOf(
                url1 to mockBitmap1,
                url2 to mockBitmap2
            )
        )

        val result1 = imageProvider.get(url1)
        val result2 = imageProvider.get(url2)

        assertEquals("Should return first bitmap", mockBitmap1, result1)
        assertEquals("Should return second bitmap", mockBitmap2, result2)
        assertEquals("Should cache first bitmap", mockBitmap1, imageProvider.getCached(url1))
        assertEquals("Should cache second bitmap", mockBitmap2, imageProvider.getCached(url2))
    }

    @Test
    fun `clear should remove all cached bitmaps`() = runTest {
        val url = "http://example.com/image.png"
        val mockConnection = mockSuccessfulConnection(mockBitmap)
        setupNetworkServiceCallback(mockConnection)

        // Download and cache image
        imageProvider.get(url)
        assertNotNull("Image should be cached", imageProvider.getCached(url))

        imageProvider.clear()

        assertNull("Cache should be empty after clear", imageProvider.getCached(url))
    }

    @Test
    fun `clear should remove multiple cached bitmaps`() = runTest {
        val url1 = "http://example.com/image1.png"
        val url2 = "http://example.com/image2.png"
        val mockBitmap1 = mockk<Bitmap>(relaxed = true)
        val mockBitmap2 = mockk<Bitmap>(relaxed = true)

        setupNetworkServiceCallbackForMultipleUrls(
            mapOf(
                url1 to mockBitmap1,
                url2 to mockBitmap2
            )
        )

        // Download and cache images
        imageProvider.get(url1)
        imageProvider.get(url2)

        imageProvider.clear()

        assertNull("First image should be cleared", imageProvider.getCached(url1))
        assertNull("Second image should be cleared", imageProvider.getCached(url2))
    }

    @Test
    fun `cache should respect max entries limit`() = runTest {
        // Arrange - Create provider with small cache size
        val smallCacheProvider = DefaultImageProvider(maxEntries = 2)
        val url1 = "http://example.com/image1.png"
        val url2 = "http://example.com/image2.png"
        val url3 = "http://example.com/image3.png"

        val mockBitmap1 = mockk<Bitmap>(relaxed = true)
        val mockBitmap2 = mockk<Bitmap>(relaxed = true)
        val mockBitmap3 = mockk<Bitmap>(relaxed = true)

        setupNetworkServiceCallbackForMultipleUrls(
            mapOf(
                url1 to mockBitmap1,
                url2 to mockBitmap2,
                url3 to mockBitmap3
            )
        )

        // Act - Download 3 images into cache of size 2
        smallCacheProvider.get(url1)
        smallCacheProvider.get(url2)
        smallCacheProvider.get(url3)

        // Assert - First image should be evicted (LRU)
        assertNull("First image should be evicted", smallCacheProvider.getCached(url1))
        assertNotNull("Second image should still be cached", smallCacheProvider.getCached(url2))
        assertNotNull("Third image should be cached", smallCacheProvider.getCached(url3))
    }

    @Test
    fun `get should create correct network request with headers and timeouts`() = runTest {
        val url = "http://example.com/image.png"
        val mockConnection = mockSuccessfulConnection(mockBitmap)
        val requestSlot = slot<NetworkRequest>()

        every {
            mockNetworkService.connectAsync(capture(requestSlot), any())
        } answers {
            secondArg<NetworkCallback>().call(mockConnection)
        }

        imageProvider.get(url)

        val capturedRequest = requestSlot.captured
        assertEquals("Should use correct URL", url, capturedRequest.url)
        assertEquals("Should use GET method", "GET", capturedRequest.method.name)
        assertEquals("Should have Accept header", "image/*", capturedRequest.headers["Accept"])
        assertEquals("Should have Cache-Control header", "no-cache", capturedRequest.headers["Cache-Control"])
        assertEquals("Should have 10 second connect timeout", 10, capturedRequest.connectTimeout)
        assertEquals("Should have 30 second read timeout", 30, capturedRequest.readTimeout)
    }

    @Test
    fun `get should handle successful response with code 200`() = runTest {
        val url = "http://example.com/image.png"
        val mockConnection = mockk<HttpConnecting>(relaxed = true)
        every { mockConnection.responseCode } returns 200
        every { mockConnection.inputStream } returns ByteArrayInputStream(ByteArray(10))
        every { mockConnection.close() } returns Unit

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns mockBitmap

        setupNetworkServiceCallback(mockConnection)

        val result = imageProvider.get(url)

        assertNotNull("Should successfully download image", result)
        assertEquals("Should return correct bitmap", mockBitmap, result)
        verify { mockConnection.close() }
    }

    @Test
    fun `get should handle successful response with code 299`() = runTest {
        val url = "http://example.com/image.png"
        val mockConnection = mockk<HttpConnecting>(relaxed = true)
        every { mockConnection.responseCode } returns 299
        every { mockConnection.inputStream } returns ByteArrayInputStream(ByteArray(10))
        every { mockConnection.close() } returns Unit

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns mockBitmap

        setupNetworkServiceCallback(mockConnection)

        val result = imageProvider.get(url)

        assertNotNull("Should successfully download image", result)
        assertEquals("Should return correct bitmap", mockBitmap, result)
    }

    @Test
    fun `get should fail with response code 300`() {
        val url = "http://example.com/image.png"
        val mockConnection = mockk<HttpConnecting>(relaxed = true)
        every { mockConnection.responseCode } returns 300
        setupNetworkServiceCallback(mockConnection)

        // Act & Assert
        val exception = assertThrows(IOException::class.java) {
            runTest {
                imageProvider.get(url)
            }
        }
        assertEquals("Failed to download image: HTTP 300", exception.message)
    }

    @Test
    fun `get should fail with response code 500`() {
        val url = "http://example.com/image.png"
        val mockConnection = mockk<HttpConnecting>(relaxed = true)
        every { mockConnection.responseCode } returns 500
        setupNetworkServiceCallback(mockConnection)

        // Act & Assert
        val exception = assertThrows(IOException::class.java) {
            runTest {
                imageProvider.get(url)
            }
        }
        assertEquals("Failed to download image: HTTP 500", exception.message)
    }

    // Helper functions

    private fun mockSuccessfulConnection(bitmap: Bitmap): HttpConnecting {
        val mockConnection = mockk<HttpConnecting>(relaxed = true)
        every { mockConnection.responseCode } returns 200
        every { mockConnection.inputStream } returns ByteArrayInputStream(ByteArray(10))
        every { mockConnection.close() } returns Unit

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns bitmap

        return mockConnection
    }

    private fun setupNetworkServiceCallback(connection: HttpConnecting?) {
        val callbackSlot = slot<NetworkCallback>()
        every {
            mockNetworkService.connectAsync(any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.call(connection)
        }
    }

    private fun setupNetworkServiceCallbackForMultipleUrls(urlToBitmapMap: Map<String, Bitmap>) {
        val callbackSlot = slot<NetworkCallback>()
        val requestSlot = slot<NetworkRequest>()

        every {
            mockNetworkService.connectAsync(capture(requestSlot), capture(callbackSlot))
        } answers {
            val url = requestSlot.captured.url
            val bitmap = urlToBitmapMap[url]

            if (bitmap != null) {
                val mockConnection = mockk<HttpConnecting>(relaxed = true)
                every { mockConnection.responseCode } returns 200
                every { mockConnection.inputStream } returns ByteArrayInputStream(ByteArray(10))
                every { mockConnection.close() } returns Unit

                mockkStatic(BitmapFactory::class)
                every { BitmapFactory.decodeStream(any()) } returns bitmap

                callbackSlot.captured.call(mockConnection)
            } else {
                callbackSlot.captured.call(null)
            }
        }
    }
}

