package com.omkarnub.kanri.data.parser

import org.junit.Assert.assertEquals
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
    fun testPhonePe_paymentOfReceivedFrom_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_PHONEPE,
            title = "Payment of ₹300 received from Suresh",
            text = "Transaction ID: TXN123456789"
        )

        assertNotNull(parsed)
        assertEquals(300.0, parsed!!.amount, 0.001)
        assertEquals("Suresh", parsed.counterparty)
        assertEquals("TXN123456789", parsed.refNo)
    }

    @Test
    fun testPaytm_receivedRsFormat_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_PAYTM,
            title = "Money Received",
            text = "Received Rs. 850 from Ramesh via UPI Ref 4455667788"
        )

        assertNotNull(parsed)
        assertEquals(850.0, parsed!!.amount, 0.001)
        assertEquals("Ramesh", parsed.counterparty)
        assertEquals("Paytm", parsed.bank)
        assertEquals("4455667788", parsed.refNo)
    }

    @Test
    fun testPaytm_addedToBalanceFormat_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_PAYTM,
            title = "Paytm Wallet",
            text = "Rs. 250 added to your Paytm Balance"
        )

        assertNotNull(parsed)
        assertEquals(250.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, parsed.type)
    }

    @Test
    fun testAmazonPay_cashback_parsesCorrectly() {
        val parsed = NotificationParser.parse(
            packageName = NotificationParser.PKG_AMAZON_IN,
            title = "Amazon Pay",
            text = "Amazon Pay cashback: ₹25 credited"
        )

        assertNotNull(parsed)
        assertEquals(25.0, parsed!!.amount, 0.001)
        assertEquals("Cashback & Rewards", parsed.counterparty)
        assertEquals("Amazon Pay", parsed.bank)
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
        assertEquals("Sneha", parsed.counterparty)
        assertEquals("FamPay", parsed.bank)
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
