package com.omkarnub.kanri.ui.lending

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("ConstantLocale")
object LendingDateFormatters {
    fun formatShort(timestamp: Long): String {
        return SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatMedium(timestamp: Long): String {
        return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatMonthHeader(timestamp: Long): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
