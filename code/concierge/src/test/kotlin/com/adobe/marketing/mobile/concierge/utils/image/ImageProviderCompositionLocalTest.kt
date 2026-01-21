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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ImageProviderCompositionLocalTest {

    @Test
    fun `LocalImageProvider should be non-null`() {
        assertNotNull("LocalImageProvider should be defined", LocalImageProvider)
    }

    @Test
    fun `DefaultImageProvider should implement ImageProvider interface`() {
        val provider = DefaultImageProvider()

        assertNotNull("DefaultImageProvider should be instantiable", provider)
        assert(provider is ImageProvider) { "DefaultImageProvider should implement ImageProvider" }
    }

    @Test
    fun `DefaultImageProvider should be usable with LocalImageProvider`() {
        val provider: ImageProvider = DefaultImageProvider(maxEntries = 10)

        // Assert - If this compiles and runs, it means DefaultImageProvider is compatible
        assertNotNull("DefaultImageProvider should be compatible with ImageProvider type", provider)
        assertEquals(
            "DefaultImageProvider should be assignable to ImageProvider",
            DefaultImageProvider::class.java.interfaces.contains(ImageProvider::class.java),
            true
        )
    }
}

