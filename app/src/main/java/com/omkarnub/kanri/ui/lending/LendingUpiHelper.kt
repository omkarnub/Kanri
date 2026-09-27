package com.omkarnub.kanri.ui.lending

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale

object LendingUpiHelper {

    fun buildUpiUri(
        personName: String,
        amount: Double,
        note: String? = null,
        upiId: String? = null
    ): Uri {
        val uriBuilder = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pn", personName.trim())
            .appendQueryParameter("am", String.format(Locale.US, "%.2f", amount))
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", (note?.trim()?.ifBlank { null } ?: "Repayment to $personName").take(50))

        val cleanUpi = upiId?.trim()
        if (!cleanUpi.isNullOrEmpty()) {
            uriBuilder.appendQueryParameter("pa", cleanUpi)
        }

        return uriBuilder.build()
    }

    fun launchUpiPayment(
        context: Context,
        personName: String,
        amount: Double,
        note: String? = null,
        upiId: String? = null
    ): Boolean {
        return try {
            val uri = buildUpiUri(personName, amount, note, upiId)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(intent, "Pay ₹${String.format(Locale.US, "%.2f", amount)} with UPI")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No UPI app found on this device", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
