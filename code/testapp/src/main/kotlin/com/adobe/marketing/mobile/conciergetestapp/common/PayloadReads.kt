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

package com.adobe.marketing.mobile.conciergetestapp.common

import java.math.BigDecimal
import kotlin.math.floor

/** Shared text limits for mapped vendor strings. */
object PayloadLimits {
    const val MAX_NAME_LENGTH = 80
    const val MAX_DESCRIPTION_LENGTH = 500
}

/**
 * Defensive readers for tapin2 values as they arrive through org.json / `JSONUtils.toMap`:
 * ids are Int/Long/Double/BigDecimal or String, flags may be strings, and any field may be
 * missing or mistyped. Nothing here throws.
 */
internal object PayloadReads {

    @Suppress("UNCHECKED_CAST")
    fun obj(value: Any?): Map<String, Any?>? =
        (value as? Map<*, *>)?.takeIf { m -> m.keys.all { it is String } } as? Map<String, Any?>

    /** Reads only a bounded prefix without allocating an intermediate list of every map. */
    fun objectSequence(value: Any?, maxEntries: Int): Sequence<Map<String, Any?>> =
        (value as? List<*>).orEmpty().asSequence().take(maxEntries.coerceAtLeast(0)).mapNotNull(::obj)

    /** Non-blank id string (at most 64 chars); integral numbers only. */
    fun id(value: Any?): String? = when (value) {
        is String -> value.trim().takeIf { it.isNotEmpty() && it.length <= 64 }
        else -> long(value)?.toString()
    }

    /** Largest magnitude a Double holds as an exact integer (2^53). */
    private const val MAX_EXACT_DOUBLE = 9_007_199_254_740_992.0

    /** Integral numbers only; fractional or out-of-range values return null. */
    fun long(value: Any?): Long? = when (value) {
        is Int -> value.toLong()
        is Long -> value
        is Double -> if (value == floor(value) && value in -MAX_EXACT_DOUBLE..MAX_EXACT_DOUBLE) value.toLong() else null
        is BigDecimal -> runCatching { value.longValueExact() }.getOrNull()
        is String -> value.trim().toLongOrNull()
        else -> null
    }

    fun int(value: Any?): Int? = long(value)?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()

    fun bool(value: Any?): Boolean? = when (value) {
        is Boolean -> value
        is String -> when (value.trim().lowercase()) { "true" -> true; "false" -> false; else -> null }
        else -> null
    }

    /** tapin2 decimal amount (e.g. `8.0000`) → cents. */
    fun cents(value: Any?): Long? = PayloadText.toCents(value)

    fun name(value: Any?): String = PayloadText.sanitizeInline(value as? String, PayloadLimits.MAX_NAME_LENGTH)
}
