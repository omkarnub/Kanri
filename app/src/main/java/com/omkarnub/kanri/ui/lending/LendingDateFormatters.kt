package com.omkarnub.kanri.ui.lending

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Calendar
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

    fun formatLong(timestamp: Long): String {
        return SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatShortDateTime(timestamp: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        val isToday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
        val isYesterday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) == 1

        val timeStr = formatTime(timestamp)
        return when {
            isToday -> "Today, $timeStr"
            isYesterday -> "Yesterday, $timeStr"
            else -> SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatFullDateTime(timestamp: Long): String {
        return SimpleDateFormat("EEEE, d MMMM yyyy • h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}

