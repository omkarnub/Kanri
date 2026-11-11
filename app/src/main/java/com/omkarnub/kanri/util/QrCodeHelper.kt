package com.omkarnub.kanri.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.Immutable
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLEncoder
import java.util.Locale

@Immutable
data class QrMatrix(
    val size: Int,
    val modules: BooleanArray
) {
    fun get(x: Int, y: Int): Boolean = modules[y * size + x]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QrMatrix) return false
        return size == other.size && modules.contentEquals(other.modules)
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + modules.contentHashCode()
        return result
    }
}

object SupportDonationConfig {
    const val UPI_ID = "omkaranandsingh80@okaxis"
    const val UPI_PAYEE_NAME = "Omkar"
    const val UPI_NOTE = "Buy Kanri a coffee"
    const val USDT_EVM_ADDRESS = "0x55bBEf6752A8d83F3cBb55B02ad574c573708BaD"
    val UPI_PRESET_AMOUNTS: List<Int?> = listOf(null, 50, 100, 250)
}

object QrCodeHelper {

    fun buildUpiPaymentUri(
        upiId: String = SupportDonationConfig.UPI_ID,
        payeeName: String = SupportDonationConfig.UPI_PAYEE_NAME,
        amountRupees: Int? = null,
        note: String = SupportDonationConfig.UPI_NOTE
    ): String {
        val params = buildList {
            add("pa=${encodeParam(upiId.trim())}")
            add("pn=${encodeParam(payeeName.trim())}")
            if (amountRupees != null && amountRupees > 0) {
                add("am=${String.format(Locale.US, "%d.00", amountRupees)}")
            }
            add("cu=INR")
            val trimmedNote = note.trim()
            if (trimmedNote.isNotEmpty()) {
                add("tn=${encodeParam(trimmedNote)}")
            }
        }
        return "upi://pay?${params.joinToString("&")}"
    }

    fun encodeQrMatrix(
        content: String,
        quietZoneModules: Int = 2
    ): QrMatrix {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to quietZoneModules.coerceAtLeast(0),
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        val size = matrix.width
        val modules = BooleanArray(size * size)
        for (y in 0 until size) {
            val rowOffset = y * size
            for (x in 0 until size) {
                modules[rowOffset + x] = matrix.get(x, y)
            }
        }
        return QrMatrix(size = size, modules = modules)
    }

    fun QrMatrix.toBitmap(pixelsPerModule: Int = 12): Bitmap {
        val pxSize = size * pixelsPerModule
        val pixels = IntArray(pxSize * pxSize)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val color = if (get(x, y)) Color.BLACK else Color.WHITE
                val startY = y * pixelsPerModule
                val startX = x * pixelsPerModule
                for (dy in 0 until pixelsPerModule) {
                    val rowStart = (startY + dy) * pxSize + startX
                    for (dx in 0 until pixelsPerModule) {
                        pixels[rowStart + dx] = color
                    }
                }
            }
        }
        return Bitmap.createBitmap(pixels, pxSize, pxSize, Bitmap.Config.ARGB_8888)
    }

    private fun encodeParam(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

