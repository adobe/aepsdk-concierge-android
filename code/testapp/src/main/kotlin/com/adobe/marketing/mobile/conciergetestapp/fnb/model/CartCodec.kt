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

package com.adobe.marketing.mobile.conciergetestapp.fnb.model

import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * JSON-compatible encodings of [CartLine]s for a card's `saveState`, and Bundle-safe (String)
 * encodings of Customize selections for `rememberSaveable`.
 */
object CartCodec {

    fun encodeLines(lines: List<CartLine>): List<Map<String, Any?>> = lines.map { line ->
        mapOf(
            "i" to line.itemId,
            "n" to line.name,
            "p" to line.unitPriceCents,
            "q" to line.quantity,
            "t" to line.note,
            "o" to line.selectedOptions.map { option ->
                mapOf(
                    "g" to option.groupId,
                    "id" to option.optionId,
                    "l" to option.label,
                    "d" to option.priceDeltaCents,
                    "m" to option.groupMultiSelect
                )
            }
        )
    }

    /** Decodes [encodeLines] output; a malformed line (fractional or out-of-range numbers included) is dropped. */
    fun decodeLines(value: Any?): List<CartLine> = (value as? List<*>).orEmpty().mapNotNull { entry ->
        val line = entry as? Map<*, *> ?: return@mapNotNull null
        CartLine(
            itemId = line["i"] as? String ?: return@mapNotNull null,
            name = line["n"] as? String ?: return@mapNotNull null,
            unitPriceCents = PayloadReads.long(line["p"])?.takeIf { it >= 0 } ?: return@mapNotNull null,
            quantity = PayloadReads.int(line["q"])?.takeIf { it in 1..CartReducer.MAX_LINE_QUANTITY } ?: return@mapNotNull null,
            note = (line["t"] as? String)?.ifEmpty { null },
            selectedOptions = (line["o"] as? List<*>).orEmpty().map { item ->
                val option = item as? Map<*, *> ?: return@mapNotNull null
                SelectedOption(
                    groupId = option["g"] as? String ?: return@mapNotNull null,
                    optionId = option["id"] as? String ?: return@mapNotNull null,
                    label = option["l"] as? String ?: return@mapNotNull null,
                    priceDeltaCents = PayloadReads.long(option["d"]) ?: return@mapNotNull null,
                    groupMultiSelect = option["m"] == true
                )
            }
        )
    }

    fun encodeSelections(selections: Map<String, Set<String>>): String = JSONObject().apply {
        selections.forEach { (groupId, ids) -> put(groupId, JSONArray(ids.toList())) }
    }.toString()

    fun decodeSelections(encoded: String?): Map<String, Set<String>>? {
        if (encoded.isNullOrEmpty()) return null
        return try {
            val obj = JSONObject(encoded)
            obj.keys().asSequence().associateWith { key ->
                val ids = obj.getJSONArray(key)
                (0 until ids.length()).map { ids.getString(it) }.toSet()
            }
        } catch (e: JSONException) {
            null
        }
    }
}
