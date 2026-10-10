package com.omkarnub.kanri.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.omkarnub.kanri.data.db.TransactionWithCategory
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfStatementExporter {

    private const val PAGE_WIDTH = 595 // A4 standard width (pt)
    private const val PAGE_HEIGHT = 842 // A4 standard height (pt)
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    private val amountFormat = DecimalFormat("₹#,##0.00")
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun exportPdf(
        transactions: List<TransactionWithCategory>,
        periodTitle: String,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        var currentY = MARGIN

        // --- DRAW HEADER ---
        // Header background accent bar
        paint.color = Color.rgb(15, 23, 42) // Dark Navy
        canvas.drawRoundRect(RectF(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + 64f), 8f, 8f, paint)

        // Logo / Title
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        canvas.drawText("KANRI", MARGIN + 16f, currentY + 30f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.rgb(148, 163, 184)
        canvas.drawText("Personal Financial Statement", MARGIN + 16f, currentY + 48f, paint)

        // Statement Period (Right aligned)
        paint.color = Color.rgb(56, 189, 248) // Light Cyan
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        val periodText = periodTitle
        val periodWidth = paint.measureText(periodText)
        canvas.drawText(periodText, MARGIN + CONTENT_WIDTH - 16f - periodWidth, currentY + 30f, paint)

        val genDate = "Generated: " + SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        paint.color = Color.rgb(148, 163, 184)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        val genWidth = paint.measureText(genDate)
        canvas.drawText(genDate, MARGIN + CONTENT_WIDTH - 16f - genWidth, currentY + 46f, paint)

        currentY += 76f

        // --- DRAW SUMMARY CARDS (Spent, Received, Net) ---
        val summary = CsvExporter.calculateSummary(transactions)
        val cardWidth = (CONTENT_WIDTH - 16f) / 3f
        val cardHeight = 52f

        // Spent Card
        drawSummaryCard(
            canvas, paint, MARGIN, currentY, cardWidth, cardHeight,
            label = "TOTAL SPENT",
            amount = amountFormat.format(summary.totalDebit),
            amountColor = Color.rgb(220, 38, 38), // Red
            bgColor = Color.rgb(254, 242, 242)
        )

        // Received Card
        drawSummaryCard(
            canvas, paint, MARGIN + cardWidth + 8f, currentY, cardWidth, cardHeight,
            label = "TOTAL RECEIVED",
            amount = amountFormat.format(summary.totalCredit),
            amountColor = Color.rgb(22, 163, 74), // Green
            bgColor = Color.rgb(240, 253, 244)
        )

        // Net Flow Card
        val netColor = if (summary.netAmount >= 0) Color.rgb(22, 163, 74) else Color.rgb(220, 38, 38)
        val netSign = if (summary.netAmount >= 0) "+" else ""
        drawSummaryCard(
            canvas, paint, MARGIN + (cardWidth * 2) + 16f, currentY, cardWidth, cardHeight,
            label = "NET CASH FLOW",
            amount = netSign + amountFormat.format(summary.netAmount),
            amountColor = netColor,
            bgColor = Color.rgb(248, 250, 252)
        )

        currentY += cardHeight + 16f

        // --- DRAW CATEGORY BREAKDOWN ---
        val categoryBreakdown = transactions
            .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
            .groupBy { it.category?.name ?: "Uncategorized" }
            .mapValues { entry -> entry.value.sumOf { it.transaction.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(4)

        if (categoryBreakdown.isNotEmpty() && summary.totalDebit > 0) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 11f
            paint.color = Color.rgb(30, 41, 59)
            canvas.drawText("TOP EXPENSE CATEGORIES", MARGIN, currentY + 10f, paint)

            currentY += 18f

            var catX = MARGIN
            val catCardWidth = (CONTENT_WIDTH - 24f) / 4f
            for ((catName, amount) in categoryBreakdown) {
                val pct = ((amount / summary.totalDebit) * 100).toInt()
                paint.color = Color.rgb(241, 245, 249)
                canvas.drawRoundRect(RectF(catX, currentY, catX + catCardWidth, currentY + 36f), 6f, 6f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 9f
                paint.color = Color.rgb(100, 116, 139)
                val truncatedCat = if (catName.length > 14) catName.take(12) + ".." else catName
                canvas.drawText(truncatedCat, catX + 8f, currentY + 14f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText(amountFormat.format(amount) + " ($pct%)", catX + 8f, currentY + 28f, paint)

                catX += catCardWidth + 8f
            }

            currentY += 46f
        }

        // --- DRAW TRANSACTION LEDGER TABLE ---
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("TRANSACTION HISTORY (${transactions.size} records)", MARGIN, currentY + 12f, paint)

        currentY += 22f

        // Table Header
        currentY = drawTableHeader(canvas, paint, currentY)

        val rowHeight = 26f
        val footerHeight = 30f

        for ((index, item) in transactions.withIndex()) {
            // Check if page overflow
            if (currentY + rowHeight > PAGE_HEIGHT - MARGIN - footerHeight) {
                // Draw footer on current page
                drawPageFooter(canvas, paint, pageNumber)
                pdfDocument.finishPage(page)

                // Start new page
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                currentY = MARGIN
                // Draw table header on next page too
                currentY = drawTableHeader(canvas, paint, currentY)
            }

            // Alternating row background
            if (index % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + rowHeight, paint)
            }

            // Divider line
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawLine(MARGIN, currentY + rowHeight, MARGIN + CONTENT_WIDTH, currentY + rowHeight, paint)

            val t = item.transaction
            val isTransfer = t.isTransfer
            val isDebit = t.type.equals("DEBIT", ignoreCase = true)

            // Date
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f
            paint.color = Color.rgb(71, 85, 105)
            canvas.drawText(dateFormat.format(Date(t.timestamp)), MARGIN + 6f, currentY + 16f, paint)

            // Description / Counterparty
            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val desc = if (isTransfer) {
                "Transfer (${t.wallet} → ${t.transferToWallet ?: ""})".take(22)
            } else {
                t.counterparty?.take(22) ?: (t.bank ?: "Manual")
            }
            canvas.drawText(desc, MARGIN + 75f, currentY + 16f, paint)

            // Category
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.rgb(100, 116, 139)
            val cat = if (isTransfer) {
                "Transfer"
            } else {
                (item.category?.name ?: "Uncategorized").take(18)
            }
            canvas.drawText(cat, MARGIN + 215f, currentY + 16f, paint)

            // Source
            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText(t.sourceType, MARGIN + 335f, currentY + 16f, paint)

            // Amount
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            val prefix = if (isTransfer) "⇄ " else if (isDebit) "- " else "+ "
            paint.color = if (isTransfer) Color.rgb(71, 85, 105) else if (isDebit) Color.rgb(220, 38, 38) else Color.rgb(22, 163, 74)
            val amtText = prefix + amountFormat.format(t.amount)
            val amtWidth = paint.measureText(amtText)
            canvas.drawText(amtText, MARGIN + CONTENT_WIDTH - 8f - amtWidth, currentY + 16f, paint)

            currentY += rowHeight
        }

        // Draw footer on last page
        drawPageFooter(canvas, paint, pageNumber)
        pdfDocument.finishPage(page)

        // Write to stream
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    private fun drawSummaryCard(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        amount: String,
        amountColor: Int,
        bgColor: Int
    ) {
        paint.color = bgColor
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 6f, 6f, paint)

        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(label, x + 10f, y + 18f, paint)

        paint.textSize = 12f
        paint.color = amountColor
        canvas.drawText(amount, x + 10f, y + 38f, paint)
    }

    private fun drawTableHeader(canvas: Canvas, paint: Paint, y: Float): Float {
        val height = 22f
        paint.color = Color.rgb(241, 245, 249) // Slate 100
        canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + height), 4f, 4f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.color = Color.rgb(71, 85, 105)

        canvas.drawText("DATE", MARGIN + 6f, y + 14f, paint)
        canvas.drawText("DESCRIPTION", MARGIN + 75f, y + 14f, paint)
        canvas.drawText("CATEGORY", MARGIN + 215f, y + 14f, paint)
        canvas.drawText("SOURCE", MARGIN + 335f, y + 14f, paint)

        val amtLabel = "AMOUNT"
        val amtLabelWidth = paint.measureText(amtLabel)
        canvas.drawText(amtLabel, MARGIN + CONTENT_WIDTH - 8f - amtLabelWidth, y + 14f, paint)

        return y + height + 2f
    }

    private fun drawPageFooter(canvas: Canvas, paint: Paint, pageNumber: Int) {
        val y = PAGE_HEIGHT - MARGIN + 12f
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, y - 10f, MARGIN + CONTENT_WIDTH, y - 10f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        paint.color = Color.rgb(148, 163, 184)
        canvas.drawText("Kanri — Personal Financial Statement", MARGIN, y + 4f, paint)

        val pageStr = "Page $pageNumber"
        val pageStrWidth = paint.measureText(pageStr)
        canvas.drawText(pageStr, MARGIN + CONTENT_WIDTH - pageStrWidth, y + 4f, paint)
    }
}
