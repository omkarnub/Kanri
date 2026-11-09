package com.omkarnub.kanri.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import com.omkarnub.kanri.R
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.util.CurrencyUtils
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfStatementExporter {

    private const val PAGE_WIDTH = 595 // A4 standard width (pt)
    private const val PAGE_HEIGHT = 842 // A4 standard height (pt)
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun exportPdf(
        context: Context,
        transactions: List<TransactionWithCategory>,
        periodTitle: String,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }

        val logoBitmap = getKanriLogoBitmap(context)

        var currentY = MARGIN

        // =========================================================================
        // 1. CLEAN MINIMAL HEADER WITH OFFICIAL KANRI LOGO
        // =========================================================================
        val headerHeight = 44f

        if (logoBitmap != null) {
            val targetHeight = 16f
            val targetWidth = targetHeight * (logoBitmap.width.toFloat() / logoBitmap.height.toFloat())
            canvas.drawBitmap(
                logoBitmap,
                null,
                RectF(MARGIN, currentY + 2f, MARGIN + targetWidth, currentY + 2f + targetHeight),
                paint
            )
        } else {
            // Elegant fallback text wordmark
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 16f
            paint.color = Color.rgb(15, 23, 42) // Slate 900
            canvas.drawText("KANRI", MARGIN, currentY + 16f, paint)
        }

        // Subtitle below logo
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        paint.color = Color.rgb(100, 116, 139) // Slate 500
        canvas.drawText("PERSONAL FINANCIAL STATEMENT", MARGIN, currentY + 30f, paint)

        // Statement Period & Timestamp on the Right
        val rightEdge = MARGIN + CONTENT_WIDTH
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        paint.color = Color.rgb(148, 163, 184) // Slate 400
        val periodLabel = "STATEMENT PERIOD"
        val periodLabelWidth = paint.measureText(periodLabel)
        canvas.drawText(periodLabel, rightEdge - periodLabelWidth, currentY + 10f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        paint.color = Color.rgb(15, 23, 42) // Slate 900
        val periodWidth = paint.measureText(periodTitle)
        canvas.drawText(periodTitle, rightEdge - periodWidth, currentY + 24f, paint)

        val genDate = "Generated on " + SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        paint.color = Color.rgb(148, 163, 184)
        val genWidth = paint.measureText(genDate)
        canvas.drawText(genDate, rightEdge - genWidth, currentY + 36f, paint)

        currentY += headerHeight

        // Clean hairline divider
        paint.color = Color.rgb(226, 232, 240) // Slate 200
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, currentY, rightEdge, currentY, paint)

        currentY += 16f

        // =========================================================================
        // 2. EXECUTIVE FINANCIAL KPI CARDS
        // =========================================================================
        val summary = CsvExporter.calculateSummary(transactions)
        val creditCount = transactions.count { it.transaction.type.equals("CREDIT", ignoreCase = true) }
        val debitCount = transactions.count { it.transaction.type.equals("DEBIT", ignoreCase = true) }
        val cardGap = 10f
        val cardWidth = (CONTENT_WIDTH - (cardGap * 2f)) / 3f
        val cardHeight = 52f

        // Total Received Card
        drawExecutiveCard(
            canvas = canvas,
            paint = paint,
            x = MARGIN,
            y = currentY,
            width = cardWidth,
            height = cardHeight,
            label = "TOTAL RECEIVED",
            amount = CurrencyUtils.formatCurrency(summary.totalCredit),
            subtext = "$creditCount transactions",
            amountColor = Color.rgb(22, 163, 74), // Emerald 600
            bgColor = Color.rgb(240, 253, 244),  // Green 50
            borderColor = Color.rgb(220, 252, 231) // Green 100
        )

        // Total Spent Card
        drawExecutiveCard(
            canvas = canvas,
            paint = paint,
            x = MARGIN + cardWidth + cardGap,
            y = currentY,
            width = cardWidth,
            height = cardHeight,
            label = "TOTAL SPENT",
            amount = CurrencyUtils.formatCurrency(summary.totalDebit),
            subtext = "$debitCount transactions",
            amountColor = Color.rgb(220, 38, 38), // Red 600
            bgColor = Color.rgb(254, 242, 242),  // Red 50
            borderColor = Color.rgb(254, 226, 226) // Red 100
        )

        // Net Cash Flow Card
        val isNetPositive = summary.netAmount >= 0
        val netColor = if (isNetPositive) Color.rgb(22, 163, 74) else Color.rgb(220, 38, 38)
        val netPrefix = if (isNetPositive) "+" else ""
        drawExecutiveCard(
            canvas = canvas,
            paint = paint,
            x = MARGIN + (cardWidth * 2f) + (cardGap * 2f),
            y = currentY,
            width = cardWidth,
            height = cardHeight,
            label = "NET CASH FLOW",
            amount = netPrefix + CurrencyUtils.formatCurrency(summary.netAmount),
            subtext = if (isNetPositive) "Net surplus" else "Net deficit",
            amountColor = netColor,
            bgColor = Color.rgb(248, 250, 252),   // Slate 50
            borderColor = Color.rgb(226, 232, 240) // Slate 200
        )

        currentY += cardHeight + 16f

        // =========================================================================
        // 3. TOP EXPENSE BREAKDOWN (Clean minimal horizontal cards)
        // =========================================================================
        val categoryBreakdown = transactions
            .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
            .groupBy { it.category?.name ?: "Uncategorized" }
            .mapValues { entry -> entry.value.sumOf { it.transaction.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(4)

        if (categoryBreakdown.isNotEmpty() && summary.totalDebit > 0) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            paint.color = Color.rgb(30, 41, 59)
            canvas.drawText("TOP EXPENSE CATEGORIES", MARGIN, currentY + 9f, paint)

            currentY += 16f

            var catX = MARGIN
            val catGap = 8f
            val catCardWidth = (CONTENT_WIDTH - (catGap * 3f)) / 4f
            val catCardHeight = 42f

            for ((catName, amount) in categoryBreakdown) {
                val pct = ((amount / summary.totalDebit) * 100).toInt()

                // Card background
                paint.color = Color.rgb(248, 250, 252) // Slate 50
                canvas.drawRoundRect(RectF(catX, currentY, catX + catCardWidth, currentY + catCardHeight), 6f, 6f, paint)

                // Border
                paint.color = Color.rgb(226, 232, 240)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(RectF(catX, currentY, catX + catCardWidth, currentY + catCardHeight), 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                // Category Name
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 8f
                paint.color = Color.rgb(71, 85, 105)
                val truncatedCat = if (catName.length > 13) catName.take(11) + "…" else catName
                canvas.drawText(truncatedCat, catX + 8f, currentY + 14f, paint)

                // Amount & Percentage
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 9f
                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText(CurrencyUtils.formatCurrency(amount), catX + 8f, currentY + 27f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 7.5f
                paint.color = Color.rgb(100, 116, 139)
                val pctText = "$pct%"
                val pctWidth = paint.measureText(pctText)
                canvas.drawText(pctText, catX + catCardWidth - 8f - pctWidth, currentY + 27f, paint)

                // Subtle mini progress bar at bottom of card
                val barY = currentY + catCardHeight - 5f
                val barWidth = catCardWidth - 16f
                paint.color = Color.rgb(226, 232, 240)
                paint.strokeWidth = 2f
                canvas.drawLine(catX + 8f, barY, catX + 8f + barWidth, barY, paint)

                val filledBarWidth = (barWidth * (pct / 100f)).coerceIn(2f, barWidth)
                paint.color = Color.rgb(239, 68, 68) // Red 500
                canvas.drawLine(catX + 8f, barY, catX + 8f + filledBarWidth, barY, paint)

                catX += catCardWidth + catGap
            }

            currentY += catCardHeight + 16f
        }

        // =========================================================================
        // 4. TRANSACTION HISTORY TABLE
        // =========================================================================
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("TRANSACTION LEDGER (${transactions.size} records)", MARGIN, currentY + 10f, paint)

        currentY += 18f

        // Table Header
        currentY = drawTableHeader(canvas, paint, currentY)

        val rowHeight = 25f
        val footerHeight = 32f

        for ((index, item) in transactions.withIndex()) {
            // Check if row exceeds printable height
            if (currentY + rowHeight > PAGE_HEIGHT - MARGIN - footerHeight) {
                drawPageFooter(canvas, paint, pageNumber)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                currentY = MARGIN
                currentY = drawTableHeader(canvas, paint, currentY)
            }

            // Alternating soft row background
            if (index % 2 == 1) {
                paint.color = Color.rgb(250, 250, 250) // Soft subtle gray
                canvas.drawRect(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + rowHeight, paint)
            }

            // Subtle row divider line
            paint.color = Color.rgb(241, 245, 249) // Slate 100
            paint.strokeWidth = 0.5f
            canvas.drawLine(MARGIN, currentY + rowHeight, MARGIN + CONTENT_WIDTH, currentY + rowHeight, paint)

            val t = item.transaction
            val isTransfer = t.isTransfer
            val isDebit = t.type.equals("DEBIT", ignoreCase = true)

            // Date
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.color = Color.rgb(71, 85, 105)
            canvas.drawText(dateFormat.format(Date(t.timestamp)), MARGIN + 8f, currentY + 16f, paint)

            // Description / Counterparty
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8.5f
            paint.color = Color.rgb(15, 23, 42)
            val desc = if (isTransfer) {
                "Transfer (${t.wallet} → ${t.transferToWallet ?: ""})".take(26)
            } else {
                t.counterparty?.take(26) ?: (t.bank?.take(26) ?: "Manual")
            }
            canvas.drawText(desc, MARGIN + 80f, currentY + 16f, paint)

            // Category
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.color = Color.rgb(100, 116, 139)
            val cat = if (isTransfer) "Transfer" else (item.category?.name ?: "Uncategorized").take(18)
            canvas.drawText(cat, MARGIN + 230f, currentY + 16f, paint)

            // Source / Wallet
            val sourceText = if (isTransfer) t.wallet else "${t.wallet} • ${t.sourceType}"
            canvas.drawText(sourceText.take(18), MARGIN + 350f, currentY + 16f, paint)

            // Amount
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            val prefix = if (isTransfer) "⇄ " else if (isDebit) "- " else "+ "
            paint.color = if (isTransfer) {
                Color.rgb(71, 85, 105)
            } else if (isDebit) {
                Color.rgb(220, 38, 38)
            } else {
                Color.rgb(22, 163, 74)
            }
            val amtText = prefix + CurrencyUtils.formatCurrency(t.amount)
            val amtWidth = paint.measureText(amtText)
            canvas.drawText(amtText, MARGIN + CONTENT_WIDTH - 8f - amtWidth, currentY + 16f, paint)

            currentY += rowHeight
        }

        // Draw footer on final page
        drawPageFooter(canvas, paint, pageNumber)
        pdfDocument.finishPage(page)

        // Write output and close
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        logoBitmap?.recycle()
    }

    private fun drawExecutiveCard(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        amount: String,
        subtext: String,
        amountColor: Int,
        bgColor: Int,
        borderColor: Int
    ) {
        // Card Background
        paint.color = bgColor
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 8f, 8f, paint)

        // Hairline Border
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Label
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(label, x + 10f, y + 16f, paint)

        // Amount
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        paint.color = amountColor
        canvas.drawText(amount, x + 10f, y + 33f, paint)

        // Subtitle
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7f
        paint.color = Color.rgb(148, 163, 184)
        canvas.drawText(subtext, x + 10f, y + 45f, paint)
    }

    private fun drawTableHeader(canvas: Canvas, paint: Paint, y: Float): Float {
        val height = 22f

        // Header Background
        paint.color = Color.rgb(241, 245, 249) // Slate 100
        canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + height), 4f, 4f, paint)

        // Hairline border
        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + height), 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        paint.color = Color.rgb(71, 85, 105)

        canvas.drawText("DATE", MARGIN + 8f, y + 14f, paint)
        canvas.drawText("DESCRIPTION", MARGIN + 80f, y + 14f, paint)
        canvas.drawText("CATEGORY", MARGIN + 230f, y + 14f, paint)
        canvas.drawText("ACCOUNT", MARGIN + 350f, y + 14f, paint)

        val amtLabel = "AMOUNT"
        val amtLabelWidth = paint.measureText(amtLabel)
        canvas.drawText(amtLabel, MARGIN + CONTENT_WIDTH - 8f - amtLabelWidth, y + 14f, paint)

        return y + height + 3f
    }

    private fun drawPageFooter(canvas: Canvas, paint: Paint, pageNumber: Int) {
        val y = PAGE_HEIGHT - MARGIN + 12f

        // Footer divider line
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 0.75f
        canvas.drawLine(MARGIN, y - 8f, MARGIN + CONTENT_WIDTH, y - 8f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        paint.color = Color.rgb(148, 163, 184)
        canvas.drawText("Kanri — Personal Financial Statement • Confidential", MARGIN, y + 6f, paint)

        val pageStr = "Page $pageNumber"
        val pageStrWidth = paint.measureText(pageStr)
        canvas.drawText(pageStr, MARGIN + CONTENT_WIDTH - pageStrWidth, y + 6f, paint)
    }

    /**
     * Extracts and tightly crops the authentic Kanri Panchang vector mark from resources
     * for high-resolution vector printing on the PDF canvas.
     */
    private fun getKanriLogoBitmap(context: Context): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.panchang_dark) ?: return null
            val targetSize = 432
            val bmp = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            drawable.setBounds(0, 0, targetSize, targetSize)
            drawable.draw(c)

            var minX = targetSize
            var minY = targetSize
            var maxX = 0
            var maxY = 0
            val pixels = IntArray(targetSize * targetSize)
            bmp.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)
            for (y in 0 until targetSize) {
                for (x in 0 until targetSize) {
                    val alpha = (pixels[y * targetSize + x] ushr 24) and 0xFF
                    if (alpha > 20) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }
            if (maxX > minX && maxY > minY) {
                val pad = 4
                val cropX = (minX - pad).coerceAtLeast(0)
                val cropY = (minY - pad).coerceAtLeast(0)
                val cropW = (maxX - minX + pad * 2).coerceAtMost(targetSize - cropX)
                val cropH = (maxY - minY + pad * 2).coerceAtMost(targetSize - cropY)
                val cropped = Bitmap.createBitmap(bmp, cropX, cropY, cropW, cropH)
                if (cropped != bmp) {
                    bmp.recycle()
                }
                cropped
            } else {
                bmp
            }
        } catch (_: Exception) {
            null
        }
    }
}
