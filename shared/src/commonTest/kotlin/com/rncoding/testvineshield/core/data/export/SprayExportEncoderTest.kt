package com.rncoding.testvineshield.core.data.export

import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.export.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.*
import kotlin.test.*

class SprayExportEncoderTest {
    private val encoder = SprayExportEncoderImpl()
    private fun snapshot() = SprayExportSnapshot(10, "Лозе 日本", null, null, "Spray", "2026/2027", 1234, true,
        SprayRecord(30, LocalDate(2026, 11, 10), null, "UTC", null, null, 10.0, SprayQuantityUnit.US_GALLON, "Notes", 2,
            listOf(SprayProductLine(5, "A", 0.5, SprayQuantityUnit.KILOGRAM, "Ref", SprayRate(2.0, SprayQuantityUnit.KILOGRAM, 1.0, SprayRateBasis.HECTARE)), SprayProductLine(6, "B")), "a".repeat(32)))
    @Test fun jsonRetainsExplicitRateDimensionsUnknownsAndUnicode() = runTest {
        val doc = encoder.encode(snapshot(), DocumentExportFormat.JSON)
        val root = Json.parseToJsonElement(doc.content).jsonObject
        assertEquals("Лозе 日本", root.getValue("vineyard_name").jsonPrimitive.content)
        assertEquals(JsonNull, root["local_time"]); assertEquals(JsonNull, root["block_id"])
        assertEquals("whole_vineyard", root.getValue("scope_type").jsonPrimitive.content)
        assertFalse(root.getValue("legacy_activity_fields_exported").jsonPrimitive.boolean)
        val products = root.getValue("products").jsonArray
        val rate = products[0].jsonObject.getValue("rate").jsonObject
        assertEquals("2.0", rate.getValue("product_amount").jsonPrimitive.content)
        assertEquals("area_ha", rate.getValue("basis_unit").jsonPrimitive.content)
        assertEquals("treated_area", rate.getValue("basis_dimension").jsonPrimitive.content)
        assertEquals(JsonNull, products[1].jsonObject["rate"])
        assertFalse(doc.content.contains("generation")); assertEquals(2, doc.recordCount)
    }
    @Test fun csvQuotesTextPrefixesFormulasAndHasSeparateRateColumns() = runTest {
        val sample = snapshot().copy(vineyardName = "=1+2", record = snapshot().record.copy(notes = "a,\"b\"\n日本"))
        val csv = encoder.encode(sample, DocumentExportFormat.CSV).content
        assertTrue(csv.startsWith("\uFEFF")); assertTrue(csv.endsWith("\r\n"))
        assertTrue(csv.contains("\"'=1+2\"")); assertTrue(csv.contains("\"a,\"\"b\"\"\n日本\""))
        assertTrue(csv.contains("\"rate_basis_amount\"")); assertTrue(csv.contains("\"rate_basis_dimension\""))
        assertTrue(csv.contains("\"app_carrier_water\""))
        val json = Json.parseToJsonElement(encoder.encode(sample, DocumentExportFormat.JSON).content).jsonObject
        assertEquals("=1+2", json.getValue("vineyard_name").jsonPrimitive.content)
    }
    @Test fun invalidStoredNumbersAreNotSilentlyExportedAsZeroOrUnknown() = runTest {
        val original = snapshot()
        val sample = original.copy(record = original.record.copy(products = listOf(original.record.products.first().copy(
            rate = SprayRate(Double.POSITIVE_INFINITY, SprayQuantityUnit.LITRE, 1.0, SprayRateBasis.LITRE)))))
        val product = Json.parseToJsonElement(encoder.encode(sample, DocumentExportFormat.JSON).content).jsonObject.getValue("products").jsonArray.single().jsonObject
        assertEquals("invalid", product.getValue("rate_status").jsonPrimitive.content)
        assertEquals("Infinity", product.getValue("rate").jsonObject.getValue("product_amount").jsonPrimitive.content)
    }
    @Test fun sizeBoundsAndGeneratedNameDoNotUseUserText() = runTest {
        assertEquals("vineshield-spray-activity-30-1234.csv", encoder.encode(snapshot().copy(activityType = "../../private"), DocumentExportFormat.CSV).fileName)
        assertFailsWith<SprayExportLimitException> {
            encoder.encode(snapshot().copy(vineyardName = "x".repeat(SprayExportEncoderImpl.MAX_INPUT_CHARACTERS + 1)), DocumentExportFormat.JSON)
        }
    }
}
