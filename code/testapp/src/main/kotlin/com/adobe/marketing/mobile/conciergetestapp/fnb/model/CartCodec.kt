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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Bundle-safe (String) encoding of [Cart] and Customize selections for `rememberSaveable`, so the
 * cart survives the menu scrolling out of the chat list, rotation, and process death.
 */
object CartCodec {

    fun encode(cart: Cart): String = JSONArray().apply {
        cart.lines.forEach { line ->
            put(JSONObject().apply {
                put("i", line.itemId)
                put("n", line.name)
                put("p", line.unitPriceCents)
                put("q", line.quantity)
                line.note?.let { put("t", it) }
                put("o", JSONArray().apply {
                    line.selectedOptions.forEach { option ->
                        put(JSONObject().apply {
                            put("g", option.groupId)
                            put("id", option.optionId)
                            put("l", option.label)
                            put("d", option.priceDeltaCents)
                            put("m", option.groupMultiSelect)
                        })
                    }
                })
            })
        }
    }.toString()

    fun decode(encoded: String?): Cart {
        if (encoded.isNullOrEmpty()) return Cart()
        return try {
            val array = JSONArray(encoded)
            Cart((0 until array.length()).map { index ->
                val line = array.getJSONObject(index)
                val options = line.getJSONArray("o")
                CartLine(
                    itemId = line.getString("i"),
                    name = line.getString("n"),
                    unitPriceCents = line.getLong("p"),
                    quantity = line.getInt("q"),
                    note = line.optString("t", "").ifEmpty { null },
                    selectedOptions = (0 until options.length()).map { optionIndex ->
                        val option = options.getJSONObject(optionIndex)
                        SelectedOption(
                            groupId = option.getString("g"),
                            optionId = option.getString("id"),
                            label = option.getString("l"),
                            priceDeltaCents = option.getLong("d"),
                            groupMultiSelect = option.optBoolean("m", false)
                        )
                    }
                )
            })
        } catch (e: JSONException) {
            Cart()
        }
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
