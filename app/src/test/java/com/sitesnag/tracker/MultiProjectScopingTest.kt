package com.sitesnag.tracker

import com.sitesnag.tracker.data.local.entity.*
import com.sitesnag.tracker.domain.model.Priority
import com.sitesnag.tracker.domain.model.RecordType
import com.sitesnag.tracker.domain.model.Status
import org.junit.Assert.*
import org.junit.Test

class MultiProjectScopingTest {

    @Test
    fun testProjectEntityDefaultsAndProperties() {
        val project1 = ProjectEntity(
            id = 1L,
            projectName = "Metro Residences",
            projectCode = "MR-TOWER-A",
            clientContractorName = "Apex Infrastructure Ltd",
            spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms",
            defaultCcEmails = "director@apex.com",
            emailFrequency = "Daily",
            isActive = true
        )

        val project2 = ProjectEntity(
            id = 2L,
            projectName = "Grand Bayfront Complex",
            projectCode = "GBC-PHASE-1",
            clientContractorName = "Skyline Developers Group",
            spreadsheetId = "2CyjNWt1YSB6oGNeLwCeCakhVVRrqumct85PhwF3vqnt",
            defaultCcEmails = "pmc@skyline.com",
            emailFrequency = "Alternate Days",
            isActive = false
        )

        assertEquals(1L, project1.id)
        assertTrue(project1.isActive)
        assertEquals("MR-TOWER-A", project1.projectCode)

        assertEquals(2L, project2.id)
        assertFalse(project2.isActive)
        assertEquals("GBC-PHASE-1", project2.projectCode)
        assertNotEquals(project1.spreadsheetId, project2.spreadsheetId)
    }

    @Test
    fun testObservationIsolationByProjectId() {
        val obsProject1 = listOf(
            ObservationEntity(
                id = 101L,
                projectId = 1L,
                entryNumber = "OBS-001",
                recordType = RecordType.NORMAL_OBSERVATION,
                dateLogged = 1000L,
                block = "Tower A",
                floor = "Ground",
                specificLocation = "Lobby",
                vendorName = "Apex Waterproofing",
                trade = "Waterproofing",
                priority = Priority.HIGH,
                targetDate = 2000L,
                issueDescription = "Water seepage near column C1",
                status = Status.OPEN
            ),
            ObservationEntity(
                id = 102L,
                projectId = 1L,
                entryNumber = "NCR-001",
                recordType = RecordType.NCR,
                dateLogged = 1000L,
                block = "Tower A",
                floor = "1st Floor",
                specificLocation = "Flat 101",
                vendorName = "Star MEP",
                trade = "MEP",
                priority = Priority.HIGH,
                targetDate = 2000L,
                issueDescription = "Conduit damaged",
                status = Status.OPEN
            )
        )

        val obsProject2 = listOf(
            ObservationEntity(
                id = 201L,
                projectId = 2L,
                entryNumber = "OBS-001",
                recordType = RecordType.NORMAL_OBSERVATION,
                dateLogged = 1000L,
                block = "Marina Block",
                floor = "Level 2",
                specificLocation = "Corridor",
                vendorName = "Oceanic Civil",
                trade = "Civil",
                priority = Priority.MEDIUM,
                targetDate = 2000L,
                issueDescription = "Tile crack on corridor floor",
                status = Status.OPEN
            )
        )

        val allObservations = obsProject1 + obsProject2

        // Filter for Project 1
        val project1Filtered = allObservations.filter { it.projectId == 1L }
        assertEquals(2, project1Filtered.size)
        assertTrue(project1Filtered.all { it.projectId == 1L })
        assertEquals("MR Tower A records", 1, project1Filtered.count { it.recordType == RecordType.NCR })

        // Filter for Project 2
        val project2Filtered = allObservations.filter { it.projectId == 2L }
        assertEquals(1, project2Filtered.size)
        assertEquals("Marina Block", project2Filtered.first().block)
        assertEquals("Oceanic Civil", project2Filtered.first().vendorName)
    }

    @Test
    fun testMasterDataScopingByProjectId() {
        val blockP1 = BlockEntity(id = 1L, projectId = 1L, name = "Tower A")
        val blockP2 = BlockEntity(id = 2L, projectId = 2L, name = "Marina Block")

        val vendorP1 = VendorEntity(id = 1L, projectId = 1L, name = "Apex Waterproofing", trade = "Waterproofing", primaryEmail = "apex@water.com")
        val vendorP2 = VendorEntity(id = 2L, projectId = 2L, name = "Oceanic Civil", trade = "Civil", primaryEmail = "oceanic@civil.com")

        assertNotEquals(blockP1.projectId, blockP2.projectId)
        assertNotEquals(vendorP1.projectId, vendorP2.projectId)
    }
}
