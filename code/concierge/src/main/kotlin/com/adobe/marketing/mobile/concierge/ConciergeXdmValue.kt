/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge

internal object ConciergeXdmValue {
    const val MAX_DEPTH = 20

    fun copyAndValidate(value: Any?, allowNull: Boolean, depth: Int = 0): Any? {
        require(depth <= MAX_DEPTH) { "XDM values must not exceed a nesting depth of $MAX_DEPTH." }
        return when (value) {
            null -> {
                require(allowNull) { "XDM values must not be null." }
                null
            }
            is String, is Boolean, is Byte, is Short, is Int, is Long -> value
            is Float -> {
                require(value.isFinite()) { "XDM numbers must be finite." }
                value
            }
            is Double -> {
                require(value.isFinite()) { "XDM numbers must be finite." }
                value
            }
            is Map<*, *> -> {
                val copied = linkedMapOf<String, Any?>()
                value.forEach { (key, nestedValue) ->
                    require(key is String) { "XDM object keys must be strings." }
                    copied[key] = copyAndValidate(nestedValue, allowNull, depth + 1)
                }
                copied
            }
            // Nulls are literal values throughout an array subtree, not deletion sentinels.
            is List<*> -> value.map { copyAndValidate(it, allowNull = true, depth + 1) }
            else -> throw IllegalArgumentException("Unsupported value type in XDM context: ${value::class.java.name}.")
        }
    }
}
