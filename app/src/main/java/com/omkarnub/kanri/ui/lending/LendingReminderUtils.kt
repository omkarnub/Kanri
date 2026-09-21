package com.omkarnub.kanri.ui.lending

import com.omkarnub.kanri.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LendingReminderUtils {

    fun buildReminderMessage(
        personName: String,
        outstandingAmount: Double,
        originalDateMillis: Long,
        dueDateMillis: Long? = null,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): String {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        val formattedAmount = CurrencyUtils.formatCurrency(outstandingAmount)
        val formattedOriginalDate = dateFormat.format(Date(originalDateMillis))

        val duePart = if (dueDateMillis != null) {
            val formattedDueDate = dateFormat.format(Date(dueDateMillis))
            if (dueDateMillis < currentTimeMillis) {
                " which was due on $formattedDueDate"
            } else {
                " due on $formattedDueDate"
            }
        } else {
            ""
        }

        return "Hi $personName, this is a gentle reminder regarding the pending amount of $formattedAmount from $formattedOriginalDate$duePart. Please settle it when you get a chance. Thanks!"
    }
}
