package com.omkarnub.kanri.data.export

import com.omkarnub.kanri.data.db.TransactionWithCategory
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportSummary(
    val transactionCount: Int,
    val totalDebit: Double,
    val totalCredit: Double,
    val netAmount: Double
)

object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val amountFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))

    fun generateCsv(
        transactions: List<TransactionWithCategory>,
        periodTitle: String
    ): String {
        val sb = StringBuilder()

        // Metadata Header
        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        sb.append("# Kanri Financial Statement\n")
        sb.append("# Period: ").append(escapeCsv(periodTitle)).append("\n")
        sb.append("# Generated At: ").append(generatedAt).append("\n")
        sb.append("# Total Records: ").append(transactions.size).append("\n#\n")

        // Column Headers
        sb.append("Date,Time,Type,Amount (INR),Category,Counterparty,Source,Bank,Reference No,Notes,Wallet,Transfer To Wallet\n")

        var totalDebit = 0.0
        var totalCredit = 0.0

        // Rows
        for (item in transactions) {
            val t = item.transaction
            val catName = if (t.isTransfer) {
                "Transfer (${t.wallet} → ${t.transferToWallet ?: ""})"
            } else {
                item.category?.name ?: "Uncategorized"
            }

            val dateStr = dateFormat.format(Date(t.timestamp))
            val timeStr = timeFormat.format(Date(t.timestamp))
            val typeStr = if (t.isTransfer) "TRANSFER" else t.type
            val amountStr = amountFormat.format(t.amount)
            val counterpartyStr = t.counterparty ?: ""
            val sourceStr = t.sourceType
            val bankStr = t.bank ?: ""
            val refNoStr = t.refNo ?: ""
            val notesStr = t.notes ?: t.displayName ?: ""
            val walletStr = t.wallet
            val transferToStr = t.transferToWallet ?: ""

            if (!t.isTransfer) {
                if (t.type.equals("DEBIT", ignoreCase = true)) {
                    totalDebit += t.amount
                } else if (t.type.equals("CREDIT", ignoreCase = true)) {
                    totalCredit += t.amount
                }
            }

            sb.append(escapeCsv(dateStr)).append(",")
                .append(escapeCsv(timeStr)).append(",")
                .append(escapeCsv(typeStr)).append(",")
                .append(amountStr).append(",")
                .append(escapeCsv(catName)).append(",")
                .append(escapeCsv(counterpartyStr)).append(",")
                .append(escapeCsv(sourceStr)).append(",")
                .append(escapeCsv(bankStr)).append(",")
                .append(escapeCsv(refNoStr)).append(",")
                .append(escapeCsv(notesStr)).append(",")
                .append(escapeCsv(walletStr)).append(",")
                .append(escapeCsv(transferToStr)).append("\n")
        }

        // Summary Rows
        val net = totalCredit - totalDebit
        sb.append("#\n")
        sb.append("# SUMMARY\n")
        sb.append("# Total Spent (Debit): INR ").append(amountFormat.format(totalDebit)).append("\n")
        sb.append("# Total Received (Credit): INR ").append(amountFormat.format(totalCredit)).append("\n")
        sb.append("# Net Cash Flow: INR ").append(amountFormat.format(net)).append("\n")
        sb.append("# Total Transactions: ").append(transactions.size).append("\n")

        return sb.toString()
    }

    fun calculateSummary(transactions: List<TransactionWithCategory>): ExportSummary {
        var debit = 0.0
        var credit = 0.0
        for (item in transactions) {
            if (item.transaction.isTransfer) continue
            if (item.transaction.type.equals("DEBIT", ignoreCase = true)) {
                debit += item.transaction.amount
            } else if (item.transaction.type.equals("CREDIT", ignoreCase = true)) {
                credit += item.transaction.amount
            }
        }
        return ExportSummary(
            transactionCount = transactions.size,
            totalDebit = debit,
            totalCredit = credit,
            netAmount = credit - debit
        )
    }

    /**
     * RFC 4180 CSV Escaping rule:
     * If the string contains comma, double quote, or newline, enclose in quotes
     * and escape internal quotes by doubling them.
     */
    fun escapeCsv(value: String): String {
        val str = value.trim()
        val containsSpecial = str.contains(',') || str.contains('"') || str.contains('\n') || str.contains('\r')
        return if (containsSpecial) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }
}
