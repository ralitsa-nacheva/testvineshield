package com.rncoding.testvineshield.core.data.export

import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.export.*
import com.rncoding.testvineshield.core.domain.usecases.harvest.HarvestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.*
import kotlin.test.*

class HarvestExportEncoderTest {
    private val encoder = HarvestExportEncoderImpl()
    private fun record(id: Long = 40, brix: Double? = null) = HarvestDomainModel(id, 20, "2026/2027", LocalDate(2026, 11, 9), "kg", 12.5, brix)
    private fun snapshot(records: List<HarvestDomainModel> = listOf(record())) = HarvestExportSnapshot(HarvestScope(10, 20), "Южен лозов масив", "North", null, 1234, records)

    @Test fun jsonContainsVersionScopeExactUnicodeAndExplicitUnknown() = runTest {
        val document = encoder.encode(snapshot(), HarvestExportFormat.JSON)
        val root = Json.parseToJsonElement(document.content).jsonObject
        assertEquals("vineshield.harvest", root.getValue("format").jsonPrimitive.content)
        assertEquals(1, root.getValue("format_version").jsonPrimitive.int)
        assertEquals("Южен лозов масив", root.getValue("vineyard_name").jsonPrimitive.content)
        assertEquals(JsonNull, root["season_filter"])
        val row = root.getValue("records").jsonArray.single().jsonObject
        assertEquals("12.5", row.getValue("quantity").jsonPrimitive.content)
        assertEquals(JsonNull, row["brix"])
        assertEquals("unknown", row.getValue("brix_status").jsonPrimitive.content)
        assertEquals("vineshield-harvest-block-20-1234.json", document.fileName)
    }
    @Test fun knownZeroAndInvalidLegacyNumbersAreDistinctFromUnknown() = runTest {
        val records = listOf(record(40, 0.0), record(41, Double.POSITIVE_INFINITY).copy(quantity = -2.0, units = ""))
        val root = Json.parseToJsonElement(encoder.encode(snapshot(records), HarvestExportFormat.JSON).content).jsonObject
        val rows = root.getValue("records").jsonArray.map { it.jsonObject }.associateBy { it.getValue("harvest_id").jsonPrimitive.long }
        assertEquals("0.0", rows.getValue(40L).getValue("brix").jsonPrimitive.content)
        assertEquals("recorded", rows.getValue(40L).getValue("brix_status").jsonPrimitive.content)
        assertEquals("Infinity", rows.getValue(41L).getValue("brix").jsonPrimitive.content)
        assertEquals("invalid", rows.getValue(41L).getValue("brix_status").jsonPrimitive.content)
        assertEquals("invalid", rows.getValue(41L).getValue("quantity_status").jsonPrimitive.content)
        assertEquals("unknown", rows.getValue(41L).getValue("unit_status").jsonPrimitive.content)
    }
    @Test fun csvQuotesCommasQuotesAndNewlinesAndKeepsUnicode() = runTest {
        val row = record().copy(units = "baskets, \"large\"\n日本")
        val content = encoder.encode(snapshot(listOf(row)), HarvestExportFormat.CSV).content
        assertTrue(content.startsWith("\uFEFF\"format_version\""))
        assertTrue(content.endsWith("\r\n"))
        assertTrue(content.contains("\"baskets, \"\"large\"\"\n日本\""))
        assertTrue(content.contains("\"Южен лозов масив\""))
        assertTrue(content.contains("\"\",\"unknown\"\r\n"))
    }
    @Test fun spreadsheetFormulaLikeTextIsPrefixedAndJsonStaysExact() = runTest {
        for (name in listOf("=1+2", "+1", "-1", "@SUM(A1)", "  =1", "\t=1", "＝1", "＠SUM(A1)")) {
            val sample = snapshot().copy(vineyardName = name)
            assertTrue(encoder.encode(sample, HarvestExportFormat.CSV).content.contains("\"'" + name + "\""))
            val root = Json.parseToJsonElement(encoder.encode(sample, HarvestExportFormat.JSON).content).jsonObject
            assertEquals(name, root.getValue("vineyard_name").jsonPrimitive.content)
        }
    }
    @Test fun recordsAreDeterministicallyOrderedWithoutConvertingOrCombiningUnits() = runTest {
        val old = record(40).copy(harvestDate = LocalDate(2026, 11, 8))
        val newer = record(41, 22.5).copy(units = "lb")
        val newestId = record(42).copy(units = "baskets")
        val root = Json.parseToJsonElement(encoder.encode(snapshot(listOf(old, newer, newestId)), HarvestExportFormat.JSON).content).jsonObject
        val rows = root.getValue("records").jsonArray.map { it.jsonObject }
        assertEquals(listOf(42L, 41L, 40L), rows.map { it.getValue("harvest_id").jsonPrimitive.long })
        assertEquals(listOf("baskets", "lb", "kg"), rows.map { it.getValue("quantity_unit").jsonPrimitive.content })
        assertTrue(rows.all { it.getValue("quantity").jsonPrimitive.content == "12.5" })
        assertFalse("total" in root)
    }
    @Test fun generatedFileNameDoesNotUseUserText() = runTest {
        val document = encoder.encode(snapshot().copy(blockName = "../../private", vineyardName = "a/b", season = "2026/2027"), HarvestExportFormat.CSV)
        assertEquals("vineshield-harvest-block-20-1234.csv", document.fileName)
        assertEquals("text/csv", document.mimeType)
    }
    @Test fun oversizedRequestsFailBeforeNativeFileCreation() = runTest {
        assertFailsWith<HarvestExportLimitException> {
            encoder.encode(snapshot(List(HarvestExportEncoderImpl.MAX_RECORDS + 1) { record() }), HarvestExportFormat.JSON)
        }
        assertFailsWith<HarvestExportLimitException> {
            encoder.encode(snapshot().copy(blockName = "x".repeat(HarvestExportEncoderImpl.MAX_INPUT_CHARACTERS + 1)), HarvestExportFormat.CSV)
        }
    }
}
