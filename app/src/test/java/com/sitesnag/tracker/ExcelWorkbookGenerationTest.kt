package com.sitesnag.tracker

import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ExcelWorkbookGenerationTest {

    @Test
    fun testMultiTabStructureAndFormulas() {
        val workbook = XSSFWorkbook()

        // 1. Create Sheets
        val dashboardSheet = workbook.createSheet("Management_Dashboard")
        val obsSheet = workbook.createSheet("Observations")
        val ncrSheet = workbook.createSheet("NCR_Register")

        assertEquals(3, workbook.numberOfSheets)
        assertEquals("Management_Dashboard", workbook.getSheetAt(0).sheetName)
        assertEquals("Observations", workbook.getSheetAt(1).sheetName)
        assertEquals("NCR_Register", workbook.getSheetAt(2).sheetName)

        // 2. Populate Sample Data in Observations (Tab 2)
        val obsHeader = obsSheet.createRow(0)
        listOf("Entry ID", "Date", "Block", "Floor", "Location", "Vendor", "Trade", "Priority", "Target Date", "Status", "Rectified Date", "Description")
            .forEachIndexed { idx, title -> obsHeader.createCell(idx).setCellValue(title) }

        val obsRow1 = obsSheet.createRow(1)
        obsRow1.createCell(0).setCellValue("OBS-001")
        obsRow1.createCell(5).setCellValue("Apex Waterproofing")
        obsRow1.createCell(9).setCellValue("Open")

        val obsRow2 = obsSheet.createRow(2)
        obsRow2.createCell(0).setCellValue("OBS-002")
        obsRow2.createCell(5).setCellValue("Apex Waterproofing")
        obsRow2.createCell(9).setCellValue("Rectified")

        // 3. Populate Sample Data in NCR_Register (Tab 3)
        val ncrHeader = ncrSheet.createRow(0)
        listOf("Entry ID", "Date", "Block", "Floor", "Location", "Vendor", "Trade", "Priority", "Target Date", "Status", "Rectified Date", "Description", "Root Cause", "Corrective Action")
            .forEachIndexed { idx, title -> ncrHeader.createCell(idx).setCellValue(title) }

        val ncrRow1 = ncrSheet.createRow(1)
        ncrRow1.createCell(0).setCellValue("NCR-001")
        ncrRow1.createCell(5).setCellValue("Apex Waterproofing")
        ncrRow1.createCell(9).setCellValue("Open")

        // 4. Setup Live Excel Formulas in Dashboard (Tab 1)
        val rowKpiVal = dashboardSheet.createRow(4)

        // B5: Total Observations
        val cellTotalObs = rowKpiVal.createCell(1)
        cellTotalObs.cellFormula = "COUNTA(Observations!A2:A500)"
        assertEquals("COUNTA(Observations!A2:A500)", cellTotalObs.cellFormula)

        // C5: Open Snags
        val cellOpenSnags = rowKpiVal.createCell(2)
        cellOpenSnags.cellFormula = "COUNTIF(Observations!J2:J500, \"Open\")"
        assertEquals("COUNTIF(Observations!J2:J500, \"Open\")", cellOpenSnags.cellFormula)

        // D5: Rectified %
        val cellRectPct = rowKpiVal.createCell(3)
        cellRectPct.cellFormula = "IF(B5>0, (B5-C5)/B5, 1)"
        assertEquals("IF(B5>0, (B5-C5)/B5, 1)", cellRectPct.cellFormula)

        // E5: Total NCRs
        val cellTotalNcrs = rowKpiVal.createCell(4)
        cellTotalNcrs.cellFormula = "COUNTA(NCR_Register!A2:A500)"
        assertEquals("COUNTA(NCR_Register!A2:A500)", cellTotalNcrs.cellFormula)

        // F5: Open NCRs
        val cellOpenNcrs = rowKpiVal.createCell(5)
        cellOpenNcrs.cellFormula = "COUNTIF(NCR_Register!J2:J500, \"Open\")"
        assertEquals("COUNTIF(NCR_Register!J2:J500, \"Open\")", cellOpenNcrs.cellFormula)

        // 5. Verify Workbook Serializes and Deserializes without errors
        val bos = ByteArrayOutputStream()
        workbook.write(bos)
        workbook.close()

        val bytes = bos.toByteArray()
        assertTrue(bytes.isNotEmpty())

        val readBackWb = XSSFWorkbook(ByteArrayInputStream(bytes))
        assertEquals(3, readBackWb.numberOfSheets)
        val readDashboard = readBackWb.getSheet("Management_Dashboard")
        assertNotNull(readDashboard)
        val readB5 = readDashboard.getRow(4).getCell(1)
        assertEquals("COUNTA(Observations!A2:A500)", readB5.cellFormula)
        readBackWb.close()
    }

    @Test
    fun testDynamicCustomFieldsColumnsInExport() {
        val workbook = XSSFWorkbook()
        val obsSheet = workbook.createSheet("Observations")

        val baseHeaders = listOf("Entry ID", "Date Logged", "Block/Zone", "Floor/Level", "Specific Location", "Vendor", "Trade/Category", "Priority", "Target Date", "Status", "Rectified Date", "Issue Description")
        val customFields = listOf("Drawing Ref", "Package", "Quality Stamp")
        val photoHeaders = listOf("Before Photo", "After Photo")

        val fullHeaders = baseHeaders + customFields + photoHeaders

        val headerRow = obsSheet.createRow(0)
        fullHeaders.forEachIndexed { i, title -> headerRow.createCell(i).setCellValue(title) }

        assertEquals(17, fullHeaders.size)
        assertEquals("Drawing Ref", headerRow.getCell(12).stringCellValue)
        assertEquals("Package", headerRow.getCell(13).stringCellValue)
        assertEquals("Quality Stamp", headerRow.getCell(14).stringCellValue)
        assertEquals("Before Photo", headerRow.getCell(15).stringCellValue)
        assertEquals("After Photo", headerRow.getCell(16).stringCellValue)

        workbook.close()
    }
}
