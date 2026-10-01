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

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ConciergeXdmValueTest {
    private fun repository(): ConciergeStateRepository {
        val manager = mockk<ConciergeSessionManager>()
        every { manager.getSessionId() } returns "session"
        return ConciergeStateRepository(sessionManager = manager)
    }

    private fun decode(value: Any?) = ConciergeDataHandoffEvent.fromEventData(
        mapOf(ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("value" to value))
    )

    @Test
    fun `both paths accept all supported scalar types and array nulls`() {
        val values = listOf(
            "text", true, 1.toByte(), 2.toShort(), 3, 4L, 5f, 6.0,
            listOf(null, mapOf("literalNull" to null))
        )
        for (value in values) {
            val repository = repository()
            repository.updateXDMContext(mapOf("value" to value))
            assertEquals(mapOf("value" to value), repository.snapshotXDMContext("session"))
            val decoded = decode(value)
            require(decoded is DataHandoffDecodeResult.Success)
            assertEquals(mapOf("value" to value), decoded.result.xdmFields)
        }
    }

    @Test
    fun `both paths reject unsupported types nonfinite numbers and nonstring keys`() {
        val invalid = listOf(
            Any(), BigDecimal.ONE, Double.NaN, Double.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
            mapOf(1 to "invalid key")
        )
        for (value in invalid) {
            val repository = repository()
            repository.updateXDMContext(mapOf("retained" to true))
            assertThrows(IllegalArgumentException::class.java) {
                repository.updateXDMContext(mapOf("value" to value))
            }
            assertEquals(mapOf("retained" to true), repository.snapshotXDMContext("session"))
            assertEquals(
                DataHandoffDecodeResult.Rejected(ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELD_VALUE),
                decode(value)
            )
        }
    }

    @Test
    fun `both paths accept depth twenty and reject depth twenty one`() {
        for (useLists in listOf(false, true)) {
            var value: Any = "leaf"
            repeat(20) { value = if (useLists) listOf(value) else mapOf("nested" to value) }
            val repository = repository()
            repository.updateXDMContext(mapOf("value" to value))
            assertEquals(mapOf("value" to value), repository.snapshotXDMContext("session"))
            assertTrue(decode(value) is DataHandoffDecodeResult.Success)

            val tooDeep: Any = if (useLists) listOf(value) else mapOf("nested" to value)
            assertThrows(IllegalArgumentException::class.java) {
                repository.updateXDMContext(mapOf("value" to tooDeep))
            }
            assertThrows(IllegalArgumentException::class.java) {
                repository.mergeXdmFields(mapOf("value" to value), mapOf("value" to tooDeep))
            }
            assertTrue(decode(tooDeep) is DataHandoffDecodeResult.Rejected)
            assertEquals(mapOf("value" to value), repository.snapshotXDMContext("session"))
        }
    }

    @Test
    fun `both paths reject cyclic maps lists and mixed containers without stack overflow`() {
        val map = linkedMapOf<String, Any>()
        map["self"] = map
        val list = mutableListOf<Any>()
        list.add(list)
        val mixed = linkedMapOf<String, Any>()
        mixed["list"] = listOf(mixed)
        for (value in listOf(map, list, mixed)) {
            assertThrows(IllegalArgumentException::class.java) {
                repository().updateXDMContext(mapOf("value" to value))
            }
            assertTrue(decode(value) is DataHandoffDecodeResult.Rejected)
        }
    }

    @Test
    fun `handoff decoder isolates accepted fields from caller mutation`() {
        val nested = linkedMapOf<String, Any>("tier" to "gold")
        val decoded = decode(nested)
        require(decoded is DataHandoffDecodeResult.Success)
        nested["tier"] = "changed"
        assertEquals(mapOf("value" to mapOf("tier" to "gold")), decoded.result.xdmFields)
    }
}
