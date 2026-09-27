package com.omkarnub.kanri.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationParserTest {

    @Test
    fun testGPay_sentYouFormat_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_GPAY,
            title = "Rahul Sharma sent you ₹500",
            text = "Google Pay • 17 Sep"
        )

        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals(SourceType.UPI, parsed.sourceType)
        assertEquals("Rahul Sharma", parsed.counterparty)
        assertEquals("Google Pay", parsed.bank)
    }

    @Test
    fun testGPay_debitFormat_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_GPAY,
            title = "Payment successful",
            text = "Payment of ₹150 to Swiggy successful via UPI Ref 9876543210"
        )

        assertNotNull(parsed)
        assertEquals(150.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals("Swiggy", parsed.counterparty)
        assertEquals("Google Pay", parsed.bank)
        assertEquals("9876543210", parsed.refNo)
    }

    @Test
    fun testGPay_decimalAmountAndSender_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_GPAY,
            title = "Payment received",
            text = "You received ₹1,250.50 from Amit Kumar via UPI Ref 9876543210"
        )

        assertNotNull(parsed)
        assertEquals(1250.50, parsed!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals("Amit Kumar", parsed.counterparty)
        assertEquals("9876543210", parsed.refNo)
    }

    @Test
    fun testGPay_cashbackReward_tagsAsCashbackAndRewards() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_GPAY,
            title = "Reward won!",
            text = "You won ₹15 cashback on your transaction"
        )

        assertNotNull(parsed)
        assertEquals(15.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals("Cashback & Rewards", parsed.counterparty)
    }

    @Test
    fun testPhonePe_creditedFormat_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_PHONEPE,
            title = "Money Received",
            text = "₹1,500 credited to your account from Priya Sharma via UPI"
        )

        assertNotNull(parsed)
        assertEquals(1500.0, parsed!!.amount, 0.001)
        assertEquals("Priya Sharma", parsed.counterparty)
        assertEquals("PhonePe", parsed.bank)
    }

    @Test
    fun testPhonePe_debitFormat_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_PHONEPE,
            title = "Transaction Successful",
            text = "Paid ₹450 to Starbucks via UPI. Ref 11223344"
        )

        assertNotNull(parsed)
        assertEquals(450.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals("Starbucks", parsed.counterparty)
        assertEquals("PhonePe", parsed.bank)
        assertEquals("11223344", parsed.refNo)
    }

    @Test
    fun testPaytm_debitFormat_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_PAYTM,
            title = "Payment to Chai Point",
            text = "Paid ₹75 to Chai Point using Paytm UPI"
        )

        assertNotNull(parsed)
        assertEquals(75.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals("Chai Point", parsed.counterparty)
        assertEquals("Paytm", parsed.bank)
    }

    @Test
    fun testCred_debitFormat_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_CRED,
            title = "Payment Successful",
            text = "₹1,200 paid to Zomato using CRED pay"
        )

        assertNotNull(parsed)
        assertEquals(1200.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals("Zomato", parsed.counterparty)
        assertEquals("CRED", parsed.bank)
    }

    @Test
    fun testFamPay_gotFamPaidFormat_parsesAsCredit() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_FAMPAY,
            title = "YOU GOT #FAMPAID",
            text = "XYZ SENT YOU ₹1"
        )

        assertNotNull(parsed)
        assertEquals(1.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals("XYZ", parsed.counterparty)
        assertEquals("FamPay", parsed.bank)
    }

    @Test
    fun testFamPay_receivedFormat_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_FAMPAY,
            title = "Money In!",
            text = "You received ₹250 from Sneha"
        )

        assertNotNull(parsed)
        assertEquals(250.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals("Sneha", parsed.counterparty)
        assertEquals("FamPay", parsed.bank)
    }

    @Test
    fun testPackageDetection_smsAndPaymentApps() {
        assertTrue(NotificationParser.isPackageSupported(NotificationParser.PKG_GPAY))
        assertTrue(NotificationParser.isPackageSupported(NotificationParser.PKG_PHONEPE))
        assertTrue(NotificationParser.isPackageSupported(NotificationParser.PKG_GOOGLE_MESSAGES))
        assertTrue(NotificationParser.isPackageSupported(NotificationParser.PKG_SAMSUNG_MESSAGING))
        assertTrue(NotificationParser.isPackageSupported(NotificationParser.PKG_HDFC))

        // WhatsApp must NOT be supported as payment interceptors
        assertFalse(NotificationParser.isPackageSupported("com.whatsapp"))
        assertFalse(NotificationParser.isPackageSupported("com.whatsapp.w4b"))
        assertTrue(NotificationParser.isPackageSupported(NotificationParser.PKG_AMAZON_IN))
        assertTrue(NotificationParser.isReceiveAppSupported(NotificationParser.PKG_AMAZON_IN))

        assertTrue(NotificationParser.isSmsApp(NotificationParser.PKG_GOOGLE_MESSAGES))
        assertTrue(NotificationParser.isSmsApp(NotificationParser.PKG_SAMSUNG_MESSAGING))
        assertFalse(NotificationParser.isSmsApp(NotificationParser.PKG_GPAY))
    }

    @Test
    fun testSmsNotification_parsedViaSmsParserWithoutSmsPermission() {
        // Simulates Google Messages notification from bank SMS
        val title = "VK-HDFCBK"
        val body = "Rs 850.00 debited from A/c XX4321 on 26-SEP-26 to SWIGGY. Ref 987654. Avl Bal Rs 15,200.00"

        val parsed = SmsParser.parse(body = body, sender = title)
        assertNotNull(parsed)
        assertEquals(850.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals("HDFC Bank", parsed.bank)
        assertEquals("SWIGGY", parsed.counterparty)
        assertEquals("987654", parsed.refNo)
        assertEquals(15200.0, parsed.balance!!, 0.001)
    }

    @Test
    fun testNonTransactionalNotifications_areIgnored() {
        // OTP notification
        val otp = NotificationParser.parse(
            packageName = NotificationParser.PKG_GPAY,
            title = "Google Pay",
            text = "Your Google Pay OTP is 123456. Do not share it."
        )
        assertNull(otp)

        // Failed / Declined Payment (Must NOT be recorded as expense)
        val failedPayment = NotificationParser.parse(
            packageName = NotificationParser.PKG_GPAY,
            title = "Google Pay",
            text = "Payment of ₹500 to Starbucks failed"
        )
        assertNull(failedPayment)

        val declinedTxn = NotificationParser.parse(
            packageName = NotificationParser.PKG_PHONEPE,
            title = "PhonePe",
            text = "Transaction declined: Insufficient funds for payment of ₹250 to Chai Point"
        )
        assertNull(declinedTxn)

        // Collect request / scam (Must NOT be recorded)
        val collectRequest = NotificationParser.parse(
            packageName = NotificationParser.PKG_PHONEPE,
            title = "Payment Request",
            text = "Rohan requested ₹2,000 from you on PhonePe"
        )
        assertNull(collectRequest)

        // Promotional discount
        val promo = NotificationParser.parse(
            packageName = NotificationParser.PKG_PHONEPE,
            title = "Special Offer",
            text = "Flat 50% discount on Swiggy with PhonePe! Offer valid today."
        )
        assertNull(promo)

        // Bill reminder
        val reminder = NotificationParser.parse(
            packageName = NotificationParser.PKG_PAYTM,
            title = "Electricity Bill Due",
            text = "Your bill payment of ₹1,400 is due in 2 days."
        )
        assertNull(reminder)

        // Spin and win / scratch card
        val rewardPromo = NotificationParser.parse(
            packageName = NotificationParser.PKG_GPAY,
            title = "Win up to ₹1,000!",
            text = "Send money to get scratch card awaits. Chance to win big."
        )
        assertNull(rewardPromo)
    }

    @Test
    fun testDeduplicationWindow_isFiveMinutes() {
        assertEquals(300000L, NotificationDeduplicationHelper.DEDUPE_WINDOW_MS)
    }
}
