package com.sitesnag.tracker.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sitesnag.tracker.data.local.entity.ObservationEntity
import com.sitesnag.tracker.data.local.entity.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 36f // 0.5 inch margins

    suspend fun generateNcrPdf(
        context: Context,
        project: ProjectEntity,
        ncr: ObservationEntity
    ): String = withContext(Dispatchers.IO) {
        val pdfDir = File(context.filesDir, "ncr_reports").apply { if (!exists()) mkdirs() }
        
        // Filename: NCR_<Number>_<Block>_<Vendor>.pdf
        val sanitizedBlock = ncr.block.replace(Regex("[^a-zA-Z0-9]"), "_")
        val sanitizedVendor = ncr.vendorName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val fileName = "${ncr.entryNumber}_${sanitizedBlock}_${sanitizedVendor}.pdf"
        val outputFile = File(pdfDir, fileName)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawNcrContent(context, canvas, project, ncr)

        document.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
            out.flush()
        }
        document.close()

        outputFile.absolutePath
    }

    private fun drawNcrContent(
        context: Context,
        canvas: Canvas,
        project: ProjectEntity,
        ncr: ObservationEntity
    ) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateLoggedStr = dateFormat.format(Date(ncr.dateLogged))
        val targetDateStr = dateFormat.format(Date(ncr.targetDate))

        val paint = Paint().apply { isAntiAlias = true }

        // --- 1. TOP HEADER BANNER ---
        val headerRect = RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 46f)
        paint.color = Color.parseColor("#9B2C2C") // Deep Crimson Red
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(headerRect, 4f, 4f, paint)

        // Title Text
        paint.color = Color.WHITE
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NON-CONFORMANCE REPORT (NCR)", MARGIN + 12f, MARGIN + 28f, paint)

        // NCR Number in Header
        paint.textSize = 13f
        val ncrText = ncr.entryNumber
        val ncrWidth = paint.measureText(ncrText)
        canvas.drawText(ncrText, PAGE_WIDTH - MARGIN - ncrWidth - 12f, MARGIN + 28f, paint)

        var curY = MARGIN + 56f

        // --- 2. PROJECT METADATA CARD ---
        val projRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 44f)
        paint.color = Color.parseColor("#F7FAFC")
        canvas.drawRoundRect(projRect, 4f, 4f, paint)
        paint.color = Color.parseColor("#CBD5E0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(projRect, 4f, 4f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#4A5568")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Project: ${project.projectName} (${project.projectCode})", MARGIN + 10f, curY + 16f, paint)
        canvas.drawText("Client: ${project.clientContractorName}  |  Logged By: ${ncr.loggedBy}", MARGIN + 10f, curY + 32f, paint)

        val dateLabel = "Date Issued: $dateLoggedStr"
        val dateWidth = paint.measureText(dateLabel)
        canvas.drawText(dateLabel, PAGE_WIDTH - MARGIN - dateWidth - 10f, curY + 16f, paint)

        curY += 52f

        // --- 3. METADATA GRID ---
        val colWidth = (PAGE_WIDTH - 2 * MARGIN) / 2f
        val rowH = 22f

        drawDataRow(canvas, paint, "Location:", "${ncr.block} > ${ncr.floor} (${ncr.specificLocation})", MARGIN, curY, colWidth * 2)
        curY += rowH
        drawDataRow(canvas, paint, "Vendor / Trade:", "${ncr.vendorName} (${ncr.trade})", MARGIN, curY, colWidth)
        drawDataRow(canvas, paint, "Priority / Severity:", ncr.priority.displayName, MARGIN + colWidth, curY, colWidth, isUrgent = ncr.priority.name == "HIGH")
        curY += rowH
        drawDataRow(canvas, paint, "Target Resolution:", targetDateStr, MARGIN, curY, colWidth)
        drawDataRow(canvas, paint, "Current Status:", ncr.status.displayName, MARGIN + colWidth, curY, colWidth)
        curY += rowH + 6f

        // --- 4. NON-CONFORMANCE DESCRIPTION BOX ---
        curY = drawSectionBox(
            canvas, paint,
            title = "1. Non-Conformance Description",
            content = ncr.issueDescription,
            left = MARGIN,
            top = curY,
            width = PAGE_WIDTH - 2 * MARGIN,
            boxHeight = 65f
        )
        curY += 8f

        // --- 5. ROOT CAUSE BOX ---
        curY = drawSectionBox(
            canvas, paint,
            title = "2. Root Cause Analysis",
            content = ncr.rootCause ?: "Root cause to be investigated and confirmed by Subcontractor / QA Engineer.",
            left = MARGIN,
            top = curY,
            width = PAGE_WIDTH - 2 * MARGIN,
            boxHeight = 50f
        )
        curY += 8f

        // --- 6. CORRECTIVE ACTION REQUIRED BOX ---
        curY = drawSectionBox(
            canvas, paint,
            title = "3. Corrective Action Required",
            content = ncr.correctiveAction ?: "Provide remedial method statement, execute corrective works, and request inspection.",
            left = MARGIN,
            top = curY,
            width = PAGE_WIDTH - 2 * MARGIN,
            boxHeight = 50f
        )
        curY += 8f

        // --- 7. DYNAMIC CUSTOM FIELDS (IF ANY) ---
        val customFields = parseCustomFields(ncr.customFieldsJson)
        if (customFields.isNotEmpty()) {
            val customText = customFields.entries.joinToString("  |  ") { "${it.key}: ${it.value}" }
            drawDataRow(canvas, paint, "Custom Fields:", customText, MARGIN, curY, PAGE_WIDTH - 2 * MARGIN)
            curY += rowH + 4f
        }

        // --- 8. DEFECT EVIDENCE PHOTO ---
        val photoBoxH = 200f
        val photoBoxW = PAGE_WIDTH - 2 * MARGIN
        val photoRect = RectF(MARGIN, curY, MARGIN + photoBoxW, curY + photoBoxH)

        paint.color = Color.parseColor("#EDF2F7")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(photoRect, 4f, 4f, paint)
        paint.color = Color.parseColor("#CBD5E0")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(photoRect, 4f, 4f, paint)

        var photoDrawn = false
        if (!ncr.beforePhotoPath.isNullOrBlank()) {
            val photoFile = File(ncr.beforePhotoPath)
            if (photoFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                if (bitmap != null) {
                    val scale = minOf((photoBoxW - 16f) / bitmap.width, (photoBoxH - 24f) / bitmap.height)
                    val destW = bitmap.width * scale
                    val destH = bitmap.height * scale
                    val left = MARGIN + (photoBoxW - destW) / 2f
                    val top = curY + 18f + (photoBoxH - 24f - destH) / 2f
                    canvas.drawBitmap(bitmap, null, RectF(left, top, left + destW, top + destH), null)
                    bitmap.recycle()
                    photoDrawn = true
                }
            }
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#4A5568")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SITE DEFECT EVIDENCE PHOTO", MARGIN + 10f, curY + 13f, paint)

        if (!photoDrawn) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            paint.color = Color.parseColor("#A0AEC0")
            canvas.drawText("[No Photo Captured on Site]", MARGIN + photoBoxW / 2f - 60f, curY + photoBoxH / 2f, paint)
        }

        curY += photoBoxH + 12f

        // --- 9. SIGN-OFF BLOCKS (3 COLUMNS) ---
        val signBoxW = (PAGE_WIDTH - 2 * MARGIN - 16f) / 3f
        val signBoxH = 68f

        drawSignBox(canvas, paint, "Issued By: ${ncr.loggedByInitials}", ncr.loggedBy, "Date: $dateLoggedStr", MARGIN, curY, signBoxW, signBoxH)
        drawSignBox(canvas, paint, "Vendor Representative", "Name / Signature", "Date:", MARGIN + signBoxW + 8f, curY, signBoxW, signBoxH)
        drawSignBox(canvas, paint, "QA/QC Manager", "Approved / Signature", "Date:", MARGIN + (signBoxW + 8f) * 2f, curY, signBoxW, signBoxH)

        // Footer disclaimer
        paint.color = Color.parseColor("#A0AEC0")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated offline by SnagOps Quality & NCR Mobile Suite • Official Quality Record • UUID: ${ncr.recordUuid}", MARGIN, PAGE_HEIGHT - 16f, paint)
    }

    private fun drawDataRow(
        canvas: Canvas,
        paint: Paint,
        label: String,
        value: String,
        left: Float,
        top: Float,
        width: Float,
        isUrgent: Boolean = false
    ) {
        paint.style = Paint.Style.FILL
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#2D3748")
        canvas.drawText(label, left + 4f, top + 14f, paint)

        val labelW = paint.measureText(label)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = if (isUrgent) Color.parseColor("#C53030") else Color.parseColor("#4A5568")

        // Truncate if too long
        var displayValue = value
        while (paint.measureText(displayValue) > (width - labelW - 14f) && displayValue.length > 3) {
            displayValue = displayValue.substring(0, displayValue.length - 4) + "..."
        }
        canvas.drawText(displayValue, left + labelW + 8f, top + 14f, paint)
    }

    private fun drawSectionBox(
        canvas: Canvas,
        paint: Paint,
        title: String,
        content: String,
        left: Float,
        top: Float,
        width: Float,
        boxHeight: Float
    ): Float {
        val rect = RectF(left, top, left + width, top + boxHeight)
        paint.color = Color.parseColor("#F7FAFC")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, 3f, 3f, paint)

        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(rect, 3f, 3f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1A202C")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, left + 8f, top + 14f, paint)

        // Draw multi-line wrapped content
        paint.color = Color.parseColor("#4A5568")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val maxTextWidth = width - 16f
        var textY = top + 26f
        val words = content.split(" ")
        var line = ""

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) > maxTextWidth) {
                if (textY < top + boxHeight - 8f) {
                    canvas.drawText(line, left + 8f, textY, paint)
                    textY += 11f
                    line = word
                } else {
                    line = "$line..."
                    break
                }
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty() && textY < top + boxHeight - 4f) {
            canvas.drawText(line, left + 8f, textY, paint)
        }

        return top + boxHeight
    }

    private fun drawSignBox(
        canvas: Canvas,
        paint: Paint,
        role: String,
        subtext: String,
        dateLabel: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        val rect = RectF(left, top, left + width, top + height)
        paint.color = Color.parseColor("#FAFAFA")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, 2f, 2f, paint)

        paint.color = Color.parseColor("#CBD5E0")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(rect, 2f, 2f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#2D3748")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(role, left + 6f, top + 13f, paint)

        paint.color = Color.parseColor("#A0AEC0")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(subtext, left + 6f, top + 24f, paint)

        // Signature horizontal line
        paint.color = Color.parseColor("#A0AEC0")
        paint.strokeWidth = 0.8f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(left + 6f, top + 48f, left + width - 6f, top + 48f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#718096")
        canvas.drawText(dateLabel, left + 6f, top + 60f, paint)
    }

    private fun parseCustomFields(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
