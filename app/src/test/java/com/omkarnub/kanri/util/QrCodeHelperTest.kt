package com.omkarnub.kanri.util

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeHelperTest {

    @Test
    fun buildUpiPaymentUri_withoutAmount_omitsAmountAndEncodesFields() {
        val uri = QrCodeHelper.buildUpiPaymentUri(
            upiId = SupportDonationConfig.UPI_ID,
            payeeName = SupportDonationConfig.UPI_PAYEE_NAME,
            amountRupees = null,
            note = SupportDonationConfig.UPI_NOTE
        )
        assertTrue(uri.startsWith("upi://pay?"))
        assertTrue(uri.contains("pa=omkaranandsingh80%40okaxis"))
        assertTrue(uri.contains("pn=Omkar"))
        assertTrue(uri.contains("cu=INR"))
        assertTrue(uri.contains("tn=Buy%20Kanri%20a%20coffee"))
        assertTrue(!uri.contains("am="))
    }

    @Test
    fun buildUpiPaymentUri_withAmount_formatsFixedTwoDecimals() {
        val uri = QrCodeHelper.buildUpiPaymentUri(amountRupees = 100)
        assertTrue(uri.contains("am=100.00"))
    }

    @Test
    fun encodeQrMatrix_roundTripsPayloadsCleanly() {
        val payloads = listOf(
            QrCodeHelper.buildUpiPaymentUri(amountRupees = 50),
            SupportDonationConfig.USDT_EVM_ADDRESS
        )
        val scale = 6
        for (payload in payloads) {
            val qr = QrCodeHelper.encodeQrMatrix(payload)
            val pxSize = qr.size * scale
            val pixels = IntArray(pxSize * pxSize)
            for (y in 0 until qr.size) {
                for (x in 0 until qr.size) {
                    val argb = if (qr.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    for (dy in 0 until scale) {
                        val rowStart = (y * scale + dy) * pxSize + x * scale
                        for (dx in 0 until scale) {
                            pixels[rowStart + dx] = argb
                        }
                    }
                }
            }
            val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(pxSize, pxSize, pixels)))
            val decoded = QRCodeReader().decode(bitmap)
            assertEquals(payload, decoded.text)
        }
    }
}

