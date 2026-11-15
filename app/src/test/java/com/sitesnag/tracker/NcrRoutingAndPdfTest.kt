package com.sitesnag.tracker

import com.sitesnag.tracker.domain.model.Priority
import com.sitesnag.tracker.domain.model.RecordType
import com.sitesnag.tracker.domain.model.Status
import org.junit.Assert.*
import org.junit.Test

class NcrRoutingAndPdfTest {

    @Test
    fun testRecordTypePrefixRouting() {
        assertEquals("OBS", RecordType.NORMAL_OBSERVATION.prefix)
        assertEquals("NCR", RecordType.NCR.prefix)

        assertEquals("Normal Observation", RecordType.NORMAL_OBSERVATION.displayName)
        assertEquals("Non-Conformance Report (NCR)", RecordType.NCR.displayName)
    }

    @Test
    fun testSequentialIdFormatting() {
        fun formatId(type: RecordType, count: Int): String {
            return String.format("%s-%03d", type.prefix, count + 1)
        }

        assertEquals("OBS-001", formatId(RecordType.NORMAL_OBSERVATION, 0))
        assertEquals("OBS-015", formatId(RecordType.NORMAL_OBSERVATION, 14))
        assertEquals("OBS-100", formatId(RecordType.NORMAL_OBSERVATION, 99))

        assertEquals("NCR-001", formatId(RecordType.NCR, 0))
        assertEquals("NCR-007", formatId(RecordType.NCR, 6))
        assertEquals("NCR-250", formatId(RecordType.NCR, 249))
    }

    @Test
    fun testNcrPdfFilenameContract() {
        fun generateNcrFileName(entryNumber: String, block: String, vendor: String): String {
            val sanitizedBlock = block.replace(Regex("[^a-zA-Z0-9]"), "_")
            val sanitizedVendor = vendor.replace(Regex("[^a-zA-Z0-9]"), "_")
            return "${entryNumber}_${sanitizedBlock}_${sanitizedVendor}.pdf"
        }

        val name1 = generateNcrFileName("NCR-001", "Tower A", "Apex Waterproofing Ltd")
        assertEquals("NCR-001_Tower_A_Apex_Waterproofing_Ltd.pdf", name1)

        val name2 = generateNcrFileName("NCR-012", "Podium & Deck", "Build/Strong Co.")
        assertEquals("NCR-012_Podium___Deck_Build_Strong_Co_.pdf", name2)
    }

    @Test
    fun testPriorityAndStatusProperties() {
        assertEquals("Low", Priority.LOW.displayName)
        assertEquals("Medium", Priority.MEDIUM.displayName)
        assertEquals("High", Priority.HIGH.displayName)

        assertEquals("Open", Status.OPEN.displayName)
        assertEquals("Rectified", Status.RECTIFIED.displayName)
    }
}
