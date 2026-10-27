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

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Collections

/**
 * JSON value model for response elements, renderer actions, and action results:
 * `String`, `Boolean`, finite `Number`, `null`, `List`, and string-keyed `Map`.
 */
internal object CardJson {
    private const val MAX_DEPTH = 64
    private const val MAX_NODES = 100_000

    private class CopyBudget(var remaining: Int = MAX_NODES) {
        fun consume(path: String) {
            require(remaining > 0) { "$path exceeds the $MAX_NODES value limit" }
            remaining--
        }
    }

    /**
     * Returns a deep, read-only copy of [value], preserving nulls and number types.
     *
     * @throws IllegalArgumentException naming the path of the first unsupported value.
     */
    fun readOnlyCopy(value: Map<*, *>): Map<String, Any?> = copyObject(value, "$", 0, CopyBudget())

    private fun copyObject(value: Map<*, *>, path: String, depth: Int, budget: CopyBudget): Map<String, Any?> {
        require(depth < MAX_DEPTH) { "$path nests deeper than $MAX_DEPTH levels" }
        budget.consume(path)
        require(value.size <= budget.remaining) { "$path exceeds the $MAX_NODES value limit" }
        val copy = LinkedHashMap<String, Any?>(value.size)
        for ((key, entry) in value) {
            require(key is String) { "$path has a non-string key" }
            copy[key] = copyValue(entry, "$path.$key", depth + 1, budget)
        }
        return Collections.unmodifiableMap(copy)
    }

    private fun copyValue(value: Any?, path: String, depth: Int, budget: CopyBudget): Any? = when (value) {
        is Map<*, *> -> copyObject(value, path, depth, budget)
        is List<*> -> {
            require(depth < MAX_DEPTH) { "$path nests deeper than $MAX_DEPTH levels" }
            budget.consume(path)
            require(value.size <= budget.remaining) { "$path exceeds the $MAX_NODES value limit" }
            Collections.unmodifiableList(value.mapIndexed { index, item -> copyValue(item, "$path[$index]", depth + 1, budget) })
        }
        null, is String, is Boolean -> value.also { budget.consume(path) }
        is Double, is Float -> value.also {
            require((value as Number).toDouble().isFinite()) { "$path is not a finite number" }
            budget.consume(path)
        }
        is Int, is Long, is Short, is Byte, is BigDecimal, is BigInteger -> value.also { budget.consume(path) }
        else -> throw IllegalArgumentException("$path has an unsupported ${value.javaClass.simpleName} value")
    }

    /** Converts a value of the JSON model to org.json types (null becomes [JSONObject.NULL]). */
    fun toJsonValue(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().apply { value.forEach { (key, entry) -> put(key.toString(), toJsonValue(entry)) } }
        is List<*> -> JSONArray().apply { value.forEach { put(toJsonValue(it)) } }
        else -> value
    }
}
