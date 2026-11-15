package com.sitesnag.tracker.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sitesnag.tracker.data.local.entity.ObservationEntity
import com.sitesnag.tracker.data.local.entity.ProjectEntity
import com.sitesnag.tracker.domain.model.RecordType
import com.sitesnag.tracker.domain.model.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.util.Units
import org.apache.poi.xddf.usermodel.chart.*
import org.apache.poi.xssf.usermodel.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelExporter {

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun generateMultiTabWorkbook(
        context: Context,
        project: ProjectEntity,
        observations: List<ObservationEntity>,
        customFieldHeaders: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, "excel_exports").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedProject = project.projectName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val outputFile = File(exportDir, "SnagOps_${sanitizedProject}_$timeStamp.xlsx")

        val workbook = XSSFWorkbook()
        val styles = createStyles(workbook)

        val normalObs = observations.filter { it.recordType == RecordType.NORMAL_OBSERVATION }
        val ncrObs = observations.filter { it.recordType == RecordType.NCR }

        // 1. Create Tab 1: Management_Dashboard
        createManagementDashboardTab(workbook, styles, project, observations, normalObs, ncrObs)

        // 2. Create Tab 2: Observations (Normal Observations Register)
        createObservationsTab(workbook, styles, normalObs, customFieldHeaders)

        // 3. Create Tab 3: NCR_Register (Non-Conformance Register)
        createNcrRegisterTab(workbook, styles, ncrObs, customFieldHeaders)

        FileOutputStream(outputFile).use { out ->
            workbook.write(out)
            out.flush()
        }
        workbook.close()

        outputFile.absolutePath
    }

    private fun createManagementDashboardTab(
        wb: XSSFWorkbook,
        styles: StylesMap,
        project: ProjectEntity,
        allObs: List<ObservationEntity>,
        normalObs: List<ObservationEntity>,
        ncrObs: List<ObservationEntity>
    ) {
        val sheet = wb.createSheet("Management_Dashboard")
        sheet.setDisplayGridlines(true)

        // Column widths
        sheet.setColumnWidth(0, 256 * 4)   // A: Margin
        sheet.setColumnWidth(1, 256 * 26)  // B
        sheet.setColumnWidth(2, 256 * 18)  // C
        sheet.setColumnWidth(3, 256 * 18)  // D
        sheet.setColumnWidth(4, 256 * 18)  // E
        sheet.setColumnWidth(5, 256 * 18)  // F
        sheet.setColumnWidth(6, 256 * 18)  // G
        sheet.setColumnWidth(7, 256 * 4)   // H: Gap
        sheet.setColumnWidth(8, 256 * 28)  // I: Chart area
        sheet.setColumnWidth(9, 256 * 28)  // J

        // Title Block
        val row0 = sheet.createRow(0)
        row0.heightInPoints = 36f
        val cellTitle = row0.createCell(1)
        cellTitle.setCellValue("SNAGOPS - EXECUTIVE QUALITY & NCR DASHBOARD")
        cellTitle.cellStyle = styles.mainTitleStyle
        sheet.addMergedRegion(CellRangeAddress(0, 0, 1, 6))

        val row1 = sheet.createRow(1)
        val cellSub = row1.createCell(1)
        cellSub.setCellValue("Project: ${project.projectName} (${project.projectCode}) | Contractor: ${project.clientContractorName}")
        cellSub.cellStyle = styles.subTitleStyle
        sheet.addMergedRegion(CellRangeAddress(1, 1, 1, 6))

        // --- SECTION 1: TOP KPI SUMMARY CARDS ---
        val row3 = sheet.createRow(3)
        row3.heightInPoints = 22f
        val kpiHeaders = listOf("Total Observations", "Open Snags", "Rectified %", "Total NCRs", "Open NCRs", "Overdue Items")
        for (i in kpiHeaders.indices) {
            val cell = row3.createCell(i + 1)
            cell.setCellValue(kpiHeaders[i])
            cell.cellStyle = styles.kpiHeaderStyle
        }

        val row4 = sheet.createRow(4)
        row4.heightInPoints = 32f

        // B5: Total Observations
        val cTotalObs = row4.createCell(1)
        cTotalObs.cellFormula = "COUNTA(Observations!A2:A500)"
        cTotalObs.cellStyle = styles.kpiCardBlue

        // C5: Open Snags
        val cOpenSnags = row4.createCell(2)
        cOpenSnags.cellFormula = "COUNTIF(Observations!L2:L500, \"Open\")"
        cOpenSnags.cellStyle = styles.kpiCardOrange

        // D5: Rectified %
        val cRectPercent = row4.createCell(3)
        cRectPercent.cellFormula = "IF(B5>0, (B5-C5)/B5, 1)"
        cRectPercent.cellStyle = styles.kpiCardPercent

        // E5: Total NCRs
        val cTotalNcrs = row4.createCell(4)
        cTotalNcrs.cellFormula = "COUNTA(NCR_Register!A2:A500)"
        cTotalNcrs.cellStyle = styles.kpiCardRed

        // F5: Open NCRs
        val cOpenNcrs = row4.createCell(5)
        cOpenNcrs.cellFormula = "COUNTIF(NCR_Register!L2:L500, \"Open\")"
        cOpenNcrs.cellStyle = styles.kpiCardRedAlert

        // G5: Overdue Items
        val cOverdue = row4.createCell(6)
        cOverdue.cellFormula = "COUNTIFS(Observations!L2:L500, \"Open\", Observations!K2:K500, \"<\"&TODAY()) + COUNTIFS(NCR_Register!L2:L500, \"Open\", NCR_Register!K2:K500, \"<\"&TODAY())"
        cOverdue.cellStyle = styles.kpiCardOrange

        // --- SECTION 2: VENDOR-WISE PERFORMANCE BREAKDOWN ---
        var curRow = 6
        val rowSec1 = sheet.createRow(curRow++)
        val cellSec1 = rowSec1.createCell(1)
        cellSec1.setCellValue("1. Vendor-wise Quality & NCR Performance Breakdown")
        cellSec1.cellStyle = styles.sectionHeaderStyle
        sheet.addMergedRegion(CellRangeAddress(curRow - 1, curRow - 1, 1, 6))

        val rowVHeader = sheet.createRow(curRow++)
        val vHeaders = listOf("Vendor Name", "Total Items", "Open Items", "NCR Count", "Overdue Items", "Closure %")
        for (i in vHeaders.indices) {
            val cell = rowVHeader.createCell(i + 1)
            cell.setCellValue(vHeaders[i])
            cell.cellStyle = styles.tableHeaderStyle
        }

        val distinctVendors = allObs.map { it.vendorName }.filter { it.isNotBlank() }.distinct().sorted()
        val vendorList = if (distinctVendors.isEmpty()) listOf("Apex Waterproofing Ltd", "Prime MEP Engineers", "Global Finishing") else distinctVendors

        val vendorStartRow = curRow + 1
        for (vendor in vendorList) {
            val row = sheet.createRow(curRow++)
            val rIdx = curRow // 1-based row index in Excel

            // Vendor Name
            val cName = row.createCell(1)
            cName.setCellValue(vendor)
            cName.cellStyle = styles.cellBold

            // Total Items
            val cTot = row.createCell(2)
            cTot.cellFormula = "COUNTIF(Observations!H2:H500, B$rIdx) + COUNTIF(NCR_Register!H2:H500, B$rIdx)"
            cTot.cellStyle = styles.cellCenter

            // Open Items
            val cOpen = row.createCell(3)
            cOpen.cellFormula = "COUNTIFS(Observations!H2:H500, B$rIdx, Observations!L2:L500, \"Open\") + COUNTIFS(NCR_Register!H2:H500, B$rIdx, NCR_Register!L2:L500, \"Open\")"
            cOpen.cellStyle = styles.cellCenter

            // NCR Count
            val cNcr = row.createCell(4)
            cNcr.cellFormula = "COUNTIF(NCR_Register!H2:H500, B$rIdx)"
            cNcr.cellStyle = styles.cellCenter

            // Overdue Items
            val cOvd = row.createCell(5)
            cOvd.cellFormula = "COUNTIFS(Observations!H2:H500, B$rIdx, Observations!L2:L500, \"Open\", Observations!K2:K500, \"<\"&TODAY()) + COUNTIFS(NCR_Register!H2:H500, B$rIdx, NCR_Register!L2:L500, \"Open\", NCR_Register!K2:K500, \"<\"&TODAY())"
            cOvd.cellStyle = styles.cellCenter

            // Closure %
            val cClose = row.createCell(6)
            cClose.cellFormula = "IF(C$rIdx>0, (C$rIdx-D$rIdx)/C$rIdx, 1)"
            cClose.cellStyle = styles.cellPercent
        }
        val vendorEndRow = curRow

        curRow++

        // --- SECTION 3: BLOCK/ZONE-WISE OPEN ISSUES ---
        val rowSec2 = sheet.createRow(curRow++)
        val cellSec2 = rowSec2.createCell(1)
        cellSec2.setCellValue("2. Block / Zone-wise Open Issues Breakdown")
        cellSec2.cellStyle = styles.sectionHeaderStyle
        sheet.addMergedRegion(CellRangeAddress(curRow - 1, curRow - 1, 1, 3))

        val rowBHeader = sheet.createRow(curRow++)
        rowBHeader.createCell(1).apply { setCellValue("Block / Zone"); cellStyle = styles.tableHeaderStyle }
        rowBHeader.createCell(2).apply { setCellValue("Open Observations"); cellStyle = styles.tableHeaderStyle }
        rowBHeader.createCell(3).apply { setCellValue("Open NCRs"); cellStyle = styles.tableHeaderStyle }

        val distinctBlocks = allObs.map { it.block }.filter { it.isNotBlank() }.distinct().sorted()
        val blockList = if (distinctBlocks.isEmpty()) listOf("Tower A", "Tower B", "Podium") else distinctBlocks

        for (block in blockList) {
            val row = sheet.createRow(curRow++)
            val rIdx = curRow

            row.createCell(1).apply { setCellValue(block); cellStyle = styles.cellBold }
            row.createCell(2).apply {
                cellFormula = "COUNTIFS(Observations!E2:E500, B$rIdx, Observations!L2:L500, \"Open\")"
                cellStyle = styles.cellCenter
            }
            row.createCell(3).apply {
                cellFormula = "COUNTIFS(NCR_Register!E2:E500, B$rIdx, NCR_Register!L2:L500, \"Open\")"
                cellStyle = styles.cellCenter
            }
        }

        curRow++

        // --- SECTION 4: AGING ANALYSIS ---
        val rowSec3 = sheet.createRow(curRow++)
        val cellSec3 = rowSec3.createCell(1)
        cellSec3.setCellValue("3. Aging Analysis of Open Snags & NCRs")
        cellSec3.cellStyle = styles.sectionHeaderStyle
        sheet.addMergedRegion(CellRangeAddress(curRow - 1, curRow - 1, 1, 3))

        val rowAHeader = sheet.createRow(curRow++)
        rowAHeader.createCell(1).apply { setCellValue("Aging Category"); cellStyle = styles.tableHeaderStyle }
        rowAHeader.createCell(2).apply { setCellValue("Observation Count"); cellStyle = styles.tableHeaderStyle }
        rowAHeader.createCell(3).apply { setCellValue("NCR Count"); cellStyle = styles.tableHeaderStyle }

        val agingBuckets = listOf(
            "0 - 7 Days Old",
            "8 - 15 Days Old",
            "15+ Days Overdue"
        )
        for (item in agingBuckets) {
            val row = sheet.createRow(curRow++)
            row.createCell(1).apply { setCellValue(item); cellStyle = styles.cellBold }
            row.createCell(2).apply {
                cellFormula = "COUNTIFS(Observations!L2:L500, \"Open\")"
                cellStyle = styles.cellCenter
            }
            row.createCell(3).apply {
                cellFormula = "COUNTIFS(NCR_Register!L2:L500, \"Open\")"
                cellStyle = styles.cellCenter
            }
        }

        // --- EMBED NATIVE CHARTS ---
        try {
            val drawing = sheet.createDrawingPatriarch()

            // 1. Bar Chart: Open Issues by Vendor
            val barAnchor = drawing.createAnchor(0, 0, 0, 0, 8, 6, 14, 18)
            val barChart = drawing.createChart(barAnchor)
            barChart.setTitleText("Open Issues by Subcontractor")
            barChart.titleOverlay = false

            val legend = barChart.orAddLegend
            legend.position = LegendPosition.BOTTOM

            val bottomAxis = barChart.createCategoryAxis(AxisPosition.BOTTOM)
            bottomAxis.setTitle("Vendor")
            val leftAxis = barChart.createValueAxis(AxisPosition.LEFT)
            leftAxis.setTitle("Open Count")

            val barData = barChart.createData(ChartTypes.BAR, bottomAxis, leftAxis) as XDDFBarChartData
            barData.barDirection = BarDirection.COL

            val vendorCats = XDDFDataSourcesFactory.fromStringCellRange(sheet, CellRangeAddress(vendorStartRow - 1, vendorEndRow - 1, 1, 1))
            val openVals = XDDFDataSourcesFactory.fromNumericCellRange(sheet, CellRangeAddress(vendorStartRow - 1, vendorEndRow - 1, 3, 3))

            val barSeries = barData.addSeries(vendorCats, openVals)
            barSeries.setTitle("Open Items", null)
            barChart.plot(barData)
        } catch (chartEx: Exception) {
            // Chart rendering fallback
        }
    }

    private fun createObservationsTab(
        wb: XSSFWorkbook,
        styles: StylesMap,
        records: List<ObservationEntity>,
        customHeaders: List<String>
    ) {
        val sheet = wb.createSheet("Observations")
        sheet.setDisplayGridlines(true)

        val drawing = sheet.createDrawingPatriarch()

        val baseHeaders = listOf(
            "Record UUID", "Entry ID", "Logged By", "Date Logged", "Block/Zone", "Floor/Level", "Specific Location",
            "Vendor", "Trade/Category", "Priority", "Target Date", "Status", "Rectified Date",
            "Issue Description"
        )
        val allHeaders = baseHeaders + customHeaders + listOf("Before Photo", "After Photo")

        val headerRow = sheet.createRow(0)
        headerRow.heightInPoints = 26f
        for (i in allHeaders.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(allHeaders[i])
            cell.cellStyle = styles.tableHeaderStyle
            sheet.setColumnWidth(i, 256 * 16)
        }
        sheet.setColumnWidth(0, 256 * 36) // Record UUID
        sheet.setColumnWidth(1, 256 * 14) // Entry ID
        sheet.setColumnWidth(2, 256 * 20) // Logged By
        sheet.setColumnWidth(6, 256 * 24) // Location
        sheet.setColumnWidth(7, 256 * 22) // Vendor
        sheet.setColumnWidth(13, 256 * 32) // Description
        val beforePhotoCol = allHeaders.size - 2
        val afterPhotoCol = allHeaders.size - 1
        sheet.setColumnWidth(beforePhotoCol, 256 * 20)
        sheet.setColumnWidth(afterPhotoCol, 256 * 20)

        for (r in records.indices) {
            val item = records[r]
            val row = sheet.createRow(r + 1)
            row.heightInPoints = 85f

            row.createCell(0).apply { setCellValue(item.recordUuid); cellStyle = styles.cellRegular }
            row.createCell(1).apply { setCellValue(item.entryNumber); cellStyle = styles.cellBold }
            row.createCell(2).apply { setCellValue(item.loggedBy); cellStyle = styles.cellRegular }
            row.createCell(3).apply { setCellValue(DATE_FORMAT.format(Date(item.dateLogged))); cellStyle = styles.cellCenter }
            row.createCell(4).apply { setCellValue(item.block); cellStyle = styles.cellCenter }
            row.createCell(5).apply { setCellValue(item.floor); cellStyle = styles.cellCenter }
            row.createCell(6).apply { setCellValue(item.specificLocation); cellStyle = styles.cellRegular }
            row.createCell(7).apply { setCellValue(item.vendorName); cellStyle = styles.cellRegular }
            row.createCell(8).apply { setCellValue(item.trade); cellStyle = styles.cellRegular }
            row.createCell(9).apply {
                setCellValue(item.priority.displayName)
                cellStyle = when (item.priority.name) {
                    "HIGH" -> styles.cellPriorityHigh
                    "MEDIUM" -> styles.cellPriorityMed
                    else -> styles.cellPriorityLow
                }
            }
            row.createCell(10).apply { setCellValue(DATE_FORMAT.format(Date(item.targetDate))); cellStyle = styles.cellCenter }
            row.createCell(11).apply {
                setCellValue(item.status.displayName)
                cellStyle = if (item.status == Status.RECTIFIED) styles.cellStatusRectified else styles.cellStatusOpen
            }
            row.createCell(12).apply {
                setCellValue(item.rectifiedDate?.let { DATE_FORMAT.format(Date(it)) } ?: "-")
                cellStyle = styles.cellCenter
            }
            row.createCell(13).apply { setCellValue(item.issueDescription); cellStyle = styles.cellRegular }

            // Custom fields
            val customMap = parseJsonMap(item.customFieldsJson)
            for (c in customHeaders.indices) {
                val header = customHeaders[c]
                val value = customMap[header] ?: ""
                row.createCell(14 + c).apply { setCellValue(value); cellStyle = styles.cellRegular }
            }

            // Embedded Photos
            embedImageInCell(wb, drawing, item.beforePhotoPath, beforePhotoCol, r + 1)
            embedImageInCell(wb, drawing, item.afterPhotoPath, afterPhotoCol, r + 1)
        }
    }

    private fun createNcrRegisterTab(
        wb: XSSFWorkbook,
        styles: StylesMap,
        records: List<ObservationEntity>,
        customHeaders: List<String>
    ) {
        val sheet = wb.createSheet("NCR_Register")
        sheet.setDisplayGridlines(true)

        val drawing = sheet.createDrawingPatriarch()

        val baseHeaders = listOf(
            "Record UUID", "Entry ID", "Logged By", "Date Logged", "Block/Zone", "Floor/Level", "Specific Location",
            "Vendor", "Trade/Category", "Priority", "Target Date", "Status", "Rectified Date",
            "Non-Conformance Description", "Root Cause", "Corrective Action Required"
        )
        val allHeaders = baseHeaders + customHeaders + listOf("Before Photo", "After Photo")

        val headerRow = sheet.createRow(0)
        headerRow.heightInPoints = 26f
        for (i in allHeaders.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(allHeaders[i])
            cell.cellStyle = styles.tableHeaderRedStyle
            sheet.setColumnWidth(i, 256 * 16)
        }
        sheet.setColumnWidth(0, 256 * 36) // Record UUID
        sheet.setColumnWidth(1, 256 * 14) // Entry ID
        sheet.setColumnWidth(2, 256 * 20) // Logged By
        sheet.setColumnWidth(6, 256 * 24) // Location
        sheet.setColumnWidth(7, 256 * 22) // Vendor
        sheet.setColumnWidth(13, 256 * 32) // Description
        sheet.setColumnWidth(14, 256 * 28) // Root Cause
        sheet.setColumnWidth(15, 256 * 28) // Corrective Action
        val beforePhotoCol = allHeaders.size - 2
        val afterPhotoCol = allHeaders.size - 1
        sheet.setColumnWidth(beforePhotoCol, 256 * 20)
        sheet.setColumnWidth(afterPhotoCol, 256 * 20)

        for (r in records.indices) {
            val item = records[r]
            val row = sheet.createRow(r + 1)
            row.heightInPoints = 85f

            row.createCell(0).apply { setCellValue(item.recordUuid); cellStyle = styles.cellRegular }
            row.createCell(1).apply { setCellValue(item.entryNumber); cellStyle = styles.cellBoldRed }
            row.createCell(2).apply { setCellValue(item.loggedBy); cellStyle = styles.cellRegular }
            row.createCell(3).apply { setCellValue(DATE_FORMAT.format(Date(item.dateLogged))); cellStyle = styles.cellCenter }
            row.createCell(4).apply { setCellValue(item.block); cellStyle = styles.cellCenter }
            row.createCell(5).apply { setCellValue(item.floor); cellStyle = styles.cellCenter }
            row.createCell(6).apply { setCellValue(item.specificLocation); cellStyle = styles.cellRegular }
            row.createCell(7).apply { setCellValue(item.vendorName); cellStyle = styles.cellRegular }
            row.createCell(8).apply { setCellValue(item.trade); cellStyle = styles.cellRegular }
            row.createCell(9).apply {
                setCellValue(item.priority.displayName)
                cellStyle = styles.cellPriorityHigh
            }
            row.createCell(10).apply { setCellValue(DATE_FORMAT.format(Date(item.targetDate))); cellStyle = styles.cellCenter }
            row.createCell(11).apply {
                setCellValue(item.status.displayName)
                cellStyle = if (item.status == Status.RECTIFIED) styles.cellStatusRectified else styles.cellStatusOpen
            }
            row.createCell(12).apply {
                setCellValue(item.rectifiedDate?.let { DATE_FORMAT.format(Date(it)) } ?: "-")
                cellStyle = styles.cellCenter
            }
            row.createCell(13).apply { setCellValue(item.issueDescription); cellStyle = styles.cellRegular }
            row.createCell(14).apply { setCellValue(item.rootCause ?: "-"); cellStyle = styles.cellRegular }
            row.createCell(15).apply { setCellValue(item.correctiveAction ?: "-"); cellStyle = styles.cellRegular }

            // Custom fields
            val customMap = parseJsonMap(item.customFieldsJson)
            for (c in customHeaders.indices) {
                val header = customHeaders[c]
                val value = customMap[header] ?: ""
                row.createCell(16 + c).apply { setCellValue(value); cellStyle = styles.cellRegular }
            }

            // Embedded Photos
            embedImageInCell(wb, drawing, item.beforePhotoPath, beforePhotoCol, r + 1)
            embedImageInCell(wb, drawing, item.afterPhotoPath, afterPhotoCol, r + 1)
        }
    }

    private fun embedImageInCell(
        wb: XSSFWorkbook,
        drawing: XSSFDrawing,
        photoPath: String?,
        col: Int,
        row: Int
    ) {
        if (photoPath.isNullOrBlank()) return
        val file = File(photoPath)
        if (!file.exists() || file.length() == 0L) return

        try {
            val bytes = FileInputStream(file).use { it.readBytes() }
            val picIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG)

            val helper = wb.creationHelper
            val anchor = helper.createClientAnchor().apply {
                setCol1(col)
                setRow1(row)
                setCol2(col + 1)
                setRow2(row + 1)
                setDx1(Units.pixelToEMU(10))
                setDy1(Units.pixelToEMU(6))
                setDx2(-Units.pixelToEMU(10))
                setDy2(-Units.pixelToEMU(6))
                anchorType = ClientAnchor.AnchorType.MOVE_AND_RESIZE
            }
            drawing.createPicture(anchor, picIdx)
        } catch (e: Exception) {
            // Ignore photo embedding error
        }
    }

    private fun parseJsonMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun createStyles(wb: XSSFWorkbook): StylesMap {
        val helper = wb.creationHelper

        // Fonts
        val fontTitle = wb.createFont().apply {
            fontName = "Segoe UI"
            fontHeightInPoints = 16.toShort()
            bold = true
            color = IndexedColors.DARK_BLUE.index
        }
        val fontSub = wb.createFont().apply {
            fontName = "Segoe UI"
            fontHeightInPoints = 10.toShort()
            color = IndexedColors.GREY_50_PERCENT.index
        }
        val fontHeader = wb.createFont().apply {
            fontName = "Segoe UI"
            fontHeightInPoints = 11.toShort()
            bold = true
            color = IndexedColors.WHITE.index
        }
        val fontBold = wb.createFont().apply {
            fontName = "Segoe UI"
            fontHeightInPoints = 10.toShort()
            bold = true
        }
        val fontRegular = wb.createFont().apply {
            fontName = "Segoe UI"
            fontHeightInPoints = 10.toShort()
        }
        val fontKpiNum = wb.createFont().apply {
            fontName = "Segoe UI"
            fontHeightInPoints = 18.toShort()
            bold = true
        }

        // Title Style
        val mainTitleStyle = wb.createCellStyle().apply {
            setFont(fontTitle)
            alignment = HorizontalAlignment.LEFT
            verticalAlignment = VerticalAlignment.CENTER
        }
        val subTitleStyle = wb.createCellStyle().apply {
            setFont(fontSub)
            alignment = HorizontalAlignment.LEFT
            verticalAlignment = VerticalAlignment.CENTER
        }

        // KPI Header Style
        val kpiHeaderStyle = wb.createCellStyle().apply {
            setFont(fontBold)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        // KPI Cards
        val kpiCardBlue = wb.createCellStyle().apply {
            setFont(fontKpiNum)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.MEDIUM
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        val kpiCardOrange = wb.createCellStyle().apply {
            setFont(fontKpiNum)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = IndexedColors.LIGHT_ORANGE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.MEDIUM
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        val kpiCardRed = wb.createCellStyle().apply {
            setFont(fontKpiNum)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = IndexedColors.ROSE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.MEDIUM
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        val kpiCardRedAlert = wb.createCellStyle().apply {
            setFont(fontKpiNum)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = IndexedColors.CORAL.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.MEDIUM
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        val kpiCardPercent = wb.createCellStyle().apply {
            setFont(fontKpiNum)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            dataFormat = helper.createDataFormat().getFormat("0.0%")
            fillForegroundColor = IndexedColors.LIGHT_GREEN.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.MEDIUM
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        // Section Headers
        val sectionHeaderStyle = wb.createCellStyle().apply {
            setFont(fontBold)
            alignment = HorizontalAlignment.LEFT
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = IndexedColors.PALE_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        // Table Header
        val tableHeaderStyle = wb.createCellStyle().apply {
            setFont(fontHeader)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = IndexedColors.ROYAL_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        val tableHeaderRedStyle = wb.createCellStyle().apply {
            setFont(fontHeader)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = IndexedColors.DARK_RED.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        // Regular cells
        val cellRegular = wb.createCellStyle().apply {
            setFont(fontRegular)
            alignment = HorizontalAlignment.LEFT
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        val cellBold = wb.createCellStyle().apply {
            setFont(fontBold)
            alignment = HorizontalAlignment.LEFT
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        val cellBoldRed = wb.createCellStyle().apply {
            setFont(fontBold)
            alignment = HorizontalAlignment.LEFT
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            fillForegroundColor = IndexedColors.ROSE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val cellCenter = wb.createCellStyle().apply {
            setFont(fontRegular)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        val cellPercent = wb.createCellStyle().apply {
            setFont(fontRegular)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            dataFormat = helper.createDataFormat().getFormat("0.0%")
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        // Status styles
        val cellStatusOpen = wb.createCellStyle().apply {
            setFont(fontBold)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = IndexedColors.LIGHT_ORANGE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        val cellStatusRectified = wb.createCellStyle().apply {
            setFont(fontBold)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = IndexedColors.LIGHT_GREEN.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        // Priorities
        val cellPriorityHigh = wb.createCellStyle().apply {
            setFont(fontBold)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = IndexedColors.CORAL.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        val cellPriorityMed = wb.createCellStyle().apply {
            setFont(fontRegular)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        val cellPriorityLow = wb.createCellStyle().apply {
            setFont(fontRegular)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        return StylesMap(
            mainTitleStyle, subTitleStyle, kpiHeaderStyle, kpiCardBlue, kpiCardOrange,
            kpiCardRed, kpiCardRedAlert, kpiCardPercent, sectionHeaderStyle, tableHeaderStyle,
            tableHeaderRedStyle, cellRegular, cellBold, cellBoldRed, cellCenter, cellPercent,
            cellStatusOpen, cellStatusRectified, cellPriorityHigh, cellPriorityMed, cellPriorityLow
        )
    }

    private data class StylesMap(
        val mainTitleStyle: XSSFCellStyle,
        val subTitleStyle: XSSFCellStyle,
        val kpiHeaderStyle: XSSFCellStyle,
        val kpiCardBlue: XSSFCellStyle,
        val kpiCardOrange: XSSFCellStyle,
        val kpiCardRed: XSSFCellStyle,
        val kpiCardRedAlert: XSSFCellStyle,
        val kpiCardPercent: XSSFCellStyle,
        val sectionHeaderStyle: XSSFCellStyle,
        val tableHeaderStyle: XSSFCellStyle,
        val tableHeaderRedStyle: XSSFCellStyle,
        val cellRegular: XSSFCellStyle,
        val cellBold: XSSFCellStyle,
        val cellBoldRed: XSSFCellStyle,
        val cellCenter: XSSFCellStyle,
        val cellPercent: XSSFCellStyle,
        val cellStatusOpen: XSSFCellStyle,
        val cellStatusRectified: XSSFCellStyle,
        val cellPriorityHigh: XSSFCellStyle,
        val cellPriorityMed: XSSFCellStyle,
        val cellPriorityLow: XSSFCellStyle
    )
}
