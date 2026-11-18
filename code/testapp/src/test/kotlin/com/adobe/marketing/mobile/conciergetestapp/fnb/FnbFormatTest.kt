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

package com.adobe.marketing.mobile.conciergetestapp.fnb

import com.adobe.marketing.mobile.conciergetestapp.fnb.model.OptionChoice
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbFormat
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.optionLabel
import java.util.Locale
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FnbFormatTest {

    private lateinit var previous: Locale

    @Before
    fun setUp() {
        previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(previous)
    }

    @Test
    fun `price always shows cents and compact price drops a zero fraction`() {
        assertEquals("$14.00", FnbFormat.price(1400, "USD"))
        assertEquals("$3", FnbFormat.priceCompact(300, "USD"))
        assertEquals("$1.50", FnbFormat.priceCompact(150, "USD"))
    }

    @Test
    fun `formatting preserves locale currency fallback and amount edge cases`() {
        for (locale in listOf(Locale.US, Locale.GERMANY, Locale.JAPAN)) {
            for (currencyCode in listOf("USD", "EUR", "JPY", "invalid")) {
                for (cents in listOf(0L, 100L, 150L, -100L, -150L, Long.MIN_VALUE, Long.MAX_VALUE)) {
                    for (compact in listOf(false, true)) {
                        val legacy = NumberFormat.getCurrencyInstance(locale)
                        runCatching { legacy.currency = Currency.getInstance(currencyCode) }
                        if (compact && cents % 100 == 0L) {
                            legacy.minimumFractionDigits = 0
                            legacy.maximumFractionDigits = 0
                        }
                        val actual = if (compact) FnbFormat.priceCompact(cents, currencyCode, locale)
                            else FnbFormat.price(cents, currencyCode, locale)
                        assertEquals(
                            "$locale/$currencyCode/$cents/compact=$compact",
                            legacy.format(BigDecimal.valueOf(cents, 2)),
                            actual
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `option labels append a compact price delta only when the option costs extra`() {
        assertEquals("Regular", optionLabel(OptionChoice("r", "Regular"), "USD"))
        assertEquals("Large (+$1.50)", optionLabel(OptionChoice("l", "Large", priceDeltaCents = 150), "USD"))
        assertEquals("Sharing (+$3)", optionLabel(OptionChoice("s", "Sharing", priceDeltaCents = 300), "USD"))
    }
}
