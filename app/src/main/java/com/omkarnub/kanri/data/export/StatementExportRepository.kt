package com.omkarnub.kanri.data.export

import android.content.Context
import android.net.Uri
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.TransactionWithCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    PDF("pdf", "application/pdf", "PDF Statement"),
    CSV("csv", "text/csv", "Excel / CSV")
}

sealed class ExportRange(val label: String) {
    object CurrentMonth : ExportRange("Current Month")
    object LastMonth : ExportRange("Last Month")
    object YearToDate : ExportRange("Year to Date")
    object AllTime : ExportRange("All Time")
    data class SpecificMonth(val year: Int, val month: Int) : ExportRange("Selected Month")
}

class StatementExportRepository(private val context: Context) {

    private val db = KanriDatabase.getDatabase(context)
    private val transactionDao = db.transactionDao()

    suspend fun getTransactionsForRange(range: ExportRange): List<TransactionWithCategory> =
        withContext(Dispatchers.IO) {
            val (startTime, endTime) = getTimeBounds(range)
            if (startTime == 0L && endTime == Long.MAX_VALUE) {
                transactionDao.getAllTransactionsWithCategorySync()
            } else {
                transactionDao.getTransactionsWithCategoryBetweenSync(startTime, endTime)
            }
        }

    suspend fun getPreview(range: ExportRange): ExportSummary = withContext(Dispatchers.IO) {
        val list = getTransactionsForRange(range)
        CsvExporter.calculateSummary(list)
    }

    suspend fun exportStatementToUri(
        uri: Uri,
        format: ExportFormat,
        range: ExportRange
    ): Result<ExportSummary> = withContext(Dispatchers.IO) {
        try {
            val transactions = getTransactionsForRange(range)
            val periodTitle = getPeriodTitle(range)
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(Exception("Unable to open output stream for URI"))

            outputStream.use { stream ->
                when (format) {
                    ExportFormat.CSV -> {
                        val csvContent = CsvExporter.generateCsv(transactions, periodTitle)
                        val writer = OutputStreamWriter(stream, StandardCharsets.UTF_8)
                        writer.write(csvContent)
                        writer.flush()
                    }
                    ExportFormat.PDF -> {
                        PdfStatementExporter.exportPdf(transactions, periodTitle, stream)
                    }
                }
            }

            val summary = CsvExporter.calculateSummary(transactions)
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportStatementToFile(
        file: File,
        format: ExportFormat,
        range: ExportRange
    ): Result<ExportSummary> = withContext(Dispatchers.IO) {
        try {
            val transactions = getTransactionsForRange(range)
            val periodTitle = getPeriodTitle(range)
            java.io.FileOutputStream(file).use { stream ->
                when (format) {
                    ExportFormat.CSV -> {
                        val csvContent = CsvExporter.generateCsv(transactions, periodTitle)
                        val writer = OutputStreamWriter(stream, StandardCharsets.UTF_8)
                        writer.write(csvContent)
                        writer.flush()
                    }
                    ExportFormat.PDF -> {
                        PdfStatementExporter.exportPdf(transactions, periodTitle, stream)
                    }
                }
            }
            val summary = CsvExporter.calculateSummary(transactions)
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSuggestedFilename(format: ExportFormat, range: ExportRange): String {
        val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val rangeTag = when (range) {
            ExportRange.CurrentMonth -> {
                val cal = Calendar.getInstance()
                String.format(Locale.getDefault(), "%04d_%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            }
            ExportRange.LastMonth -> "last_month"
            ExportRange.YearToDate -> "YTD"
            ExportRange.AllTime -> "all_time"
            is ExportRange.SpecificMonth -> String.format(Locale.getDefault(), "%04d_%02d", range.year, range.month + 1)
        }
        return "kanri_statement_${rangeTag}_${dateStamp}.${format.extension}"
    }

    fun getPeriodTitle(range: ExportRange): String {
        val cal = Calendar.getInstance()
        return when (range) {
            ExportRange.CurrentMonth -> {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }
            ExportRange.LastMonth -> {
                cal.add(Calendar.MONTH, -1)
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }
            ExportRange.YearToDate -> {
                "Jan 1 - " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
            }
            ExportRange.AllTime -> "All Time History"
            is ExportRange.SpecificMonth -> {
                cal.set(Calendar.YEAR, range.year)
                cal.set(Calendar.MONTH, range.month)
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }
        }
    }

    private fun getTimeBounds(range: ExportRange): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (range) {
            ExportRange.CurrentMonth -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            ExportRange.LastMonth -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            ExportRange.YearToDate -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                val end = System.currentTimeMillis()
                Pair(start, end)
            }
            ExportRange.AllTime -> Pair(0L, Long.MAX_VALUE)
            is ExportRange.SpecificMonth -> {
                cal.set(Calendar.YEAR, range.year)
                cal.set(Calendar.MONTH, range.month)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
        }
    }
}
