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

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure-Kotlin text and value helpers for untrusted vendor payloads. No Android dependencies so the
 * mapper stays JVM-testable.
 */
object FnbText {

    private val ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " "
    )
    private val SKIPPED_ELEMENTS = setOf("script", "style")
    private const val MAX_ENTITY_LENGTH = 10

    /**
     * Strips HTML tags in a single linear pass (no regex backtracking), drops `<script>`/`<style>`
     * content, decodes common entities, removes control characters, collapses whitespace, and
     * clamps to [maxLength].
     */
    fun stripHtml(html: String?, maxLength: Int = Int.MAX_VALUE): String {
        if (html.isNullOrEmpty()) return ""
        val out = StringBuilder(minOf(html.length, 1024))
        var i = 0
        var skipUntil: String? = null
        while (i < html.length) {
            val c = html[i]
            if (c == '<') {
                val end = html.indexOf('>', i + 1)
                if (end < 0) break
                val tag = html.substring(i + 1, end).trim().lowercase()
                val name = tag.removePrefix("/").takeWhile { it.isLetterOrDigit() }
                if (skipUntil != null) {
                    if (tag.startsWith("/") && name == skipUntil) skipUntil = null
                } else if (!tag.startsWith("/") && name in SKIPPED_ELEMENTS && !tag.endsWith("/")) {
                    skipUntil = name
                } else {
                    out.append(' ')
                }
                i = end + 1
                continue
            }
            if (skipUntil != null) {
                i++
                continue
            }
            if (c == '&') {
                val limit = minOf(html.length, i + MAX_ENTITY_LENGTH + 1)
                var semi = -1
                for (j in i + 1 until limit) {
                    if (html[j] == ';') { semi = j; break }
                }
                if (semi > i + 1) {
                    val decoded = decodeEntity(html.substring(i + 1, semi))
                    if (decoded != null) {
                        out.append(decoded)
                        i = semi + 1
                        continue
                    }
                }
            }
            out.append(c)
            i++
        }
        return sanitizeInline(out.toString(), maxLength)
    }

    /** Removes control characters, collapses whitespace to single spaces, trims, and clamps. */
    fun sanitizeInline(text: String?, maxLength: Int = Int.MAX_VALUE): String {
        if (text.isNullOrEmpty()) return ""
        val sb = StringBuilder(text.length)
        var lastWasSpace = true
        for (ch in text) {
            val normalized = if (ch.isWhitespace() || ch.isISOControl() || ch == '\u00A0') ' ' else ch
            if (normalized == ' ') {
                if (!lastWasSpace) sb.append(' ')
                lastWasSpace = true
            } else {
                sb.append(normalized)
                lastWasSpace = false
            }
        }
        val collapsed = sb.toString().trim()
        return if (collapsed.length > maxLength) collapsed.take(maxLength).trimEnd() + "…" else collapsed
    }

    private fun decodeEntity(body: String): String? {
        ENTITIES[body.lowercase()]?.let { return it }
        if (!body.startsWith("#")) return null
        val code = if (body.startsWith("#x") || body.startsWith("#X")) {
            body.substring(2).toIntOrNull(16)
        } else {
            body.substring(1).toIntOrNull()
        } ?: return null
        if (code !in 0x20..0x10FFFF || code in 0xD800..0xDFFF) return null
        return String(Character.toChars(code))
    }

    /** Returns [url] only when it is an absolute `https` URL; anything else renders a placeholder. */
    fun httpsUrlOrNull(url: String?): String? {
        val trimmed = url?.trim().orEmpty()
        if (trimmed.length > 2048) return null
        return trimmed.takeIf { it.startsWith("https://", ignoreCase = true) && it.length > "https://".length }
    }

    /** Converts a decimal currency amount (e.g. `8.5`, `"8.50"`) to cents, rounding half-up. */
    fun toCents(value: Any?): Long? {
        val decimal = when (value) {
            is BigDecimal -> value
            is Int, is Long, is Short, is Byte -> BigDecimal.valueOf((value as Number).toLong())
            is Double -> if (value.isFinite()) BigDecimal(value.toString()) else null
            is Float -> if (value.isFinite()) BigDecimal(value.toString()) else null
            is String -> value.trim().toBigDecimalOrNull()
            else -> null
        } ?: return null
        return try {
            decimal.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (e: ArithmeticException) {
            null
        }
    }
}
