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

package com.adobe.marketing.mobile.concierge.cards

import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import java.util.concurrent.ConcurrentHashMap

/** Process-wide card renderers by exact element `type`; the last registration wins. */
internal object ConciergeCardRenderers {
    private val renderers = ConcurrentHashMap<String, ConciergeCardRenderer>()

    fun register(type: String, renderer: ConciergeCardRenderer) {
        require(type.isNotBlank()) { "Card renderer type must not be blank." }
        renderers[type] = renderer
    }

    fun rendererFor(type: String?): ConciergeCardRenderer? = type?.let(renderers::get)

    val hasAnyRenderer: Boolean
        get() = renderers.isNotEmpty()

    internal fun clear() = renderers.clear()
}
