package com.omkarnub.kanri.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    @Test
    fun testBobAtmWithdrawal() {
        val sms = "Rs. 4000.00 withdrawn from A/c ... 8477 at ATM TID ZMN8020 Ref. 624419519952 Avlbl Amt:Rs. 1055.77(01-09-2026 19:07:46). In case your a/c is debited but cash is not dispensed from the ATM, the transaction will be automatically reversed within 48 hours. TC apply. If not used by you, call 18005700-BOB."
        val result = SmsParser.parse(sms, sender = "BOB-TXN")

        assertNotNull("Should parse BOB ATM withdrawal", result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(4000.0, result.amount, 0.001)
        assertEquals(SourceType.ATM, result.sourceType)
        assertEquals("ATM TID ZMN8020", result.counterparty)
        assertEquals("624419519952", result.refNo)
        assertEquals("Bank of Baroda", result.bank)
    }

    @Test
    fun testUpiDebitStandard() {
        val sms = "A/C X8477 debited by 500.00 on date 15Sep26 trf to Rohit Kumar Refno 425612345678. If not u? call 1800..."
        val result = SmsParser.parse(sms, sender = "SBI-UPI")

        assertNotNull("Should parse UPI debit", result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals("Rohit Kumar", result.counterparty)
        assertEquals("425612345678", result.refNo)
        assertEquals("State Bank of India", result.bank)
    }

    @Test
    fun testSentUpi() {
        val sms = "Sent Rs.250.00 from Kotak Bank AC X1234 to swiggy@icici on 12-09-26. UPI Ref 425678912345."
        val result = SmsParser.parse(sms, sender = "KOTAK")

        assertNotNull("Should parse sent UPI", result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(250.0, result.amount, 0.001)
        assertEquals(SourceType.UPI, result.sourceType)
        assertEquals("swiggy@icici", result.counterparty)
        assertEquals("425678912345", result.refNo)
        assertEquals("Kotak Mahindra Bank", result.bank)
    }

    @Test
    fun testUpiCredit() {
        val sms = "Your A/C ...8477 is credited with Rs. 5,000.00 on 11-09-26 by transfer from John Doe (UPI Ref 425678912345)."
        val result = SmsParser.parse(sms, sender = "HDFC")

        assertNotNull("Should parse UPI credit", result)
        assertEquals(TransactionType.CREDIT, result!!.type)
        assertEquals(5000.0, result.amount, 0.001)
        assertEquals("John Doe", result.counterparty)
        assertEquals("425678912345", result.refNo)
        assertEquals("HDFC Bank", result.bank)
    }

    @Test
    fun testCardSpend() {
        val sms = "Alert: You've spent Rs. 850.00 on your Credit Card ending 1234 at STARBUCKS on 14-09-26."
        val result = SmsParser.parse(sms, sender = "ICICI")

        assertNotNull("Should parse Card spend", result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(850.0, result.amount, 0.001)
        assertEquals(SourceType.CARD, result.sourceType)
        assertEquals("STARBUCKS", result.counterparty)
        assertEquals("ICICI Bank", result.bank)
    }

    @Test
    fun testOtpMessageIsIgnored() {
        val sms = "Your OTP for netbanking login is 482910. Valid for 5 minutes. Do not share with anyone."
        val result = SmsParser.parse(sms, sender = "HDFC")

        assertNull("OTP message should return null", result)
    }

    @Test
    fun testRefundMessageIsParsedAsCredit() {
        val sms = "Rs. 350.00 refunded to your A/C ...8477 for Swiggy order. UPI Ref 778899112233."
        val result = SmsParser.parse(sms, sender = "BOB-TXN")

        assertNotNull("Refund SMS should be parsed", result)
        assertEquals(TransactionType.CREDIT, result!!.type)
        assertEquals(350.0, result.amount, 0.001)
    }

    @Test
    fun testDeclinedTransactionIsIgnored() {
        val sms = "Transaction of Rs. 1500.00 on your card was declined due to incorrect PIN. Contact bank if not done by you."
        val result = SmsParser.parse(sms, sender = "HDFC")

        assertNull("Declined transaction without debit should return null", result)
    }

    @Test
    fun testAxisBankDebit() {
        val sms = "Your A/c no. XX3948 is debited for Rs 850.00 on 18-09-26 and credited to VPA swiggy@icici (UPI Ref no 425678912345)."
        val result = SmsParser.parse(sms, sender = "VM-AXISBK")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(850.0, result.amount, 0.001)
        assertEquals("Axis Bank", result.bank)
        assertEquals("swiggy@icici", result.counterparty)
        assertEquals("425678912345", result.refNo)
    }

    @Test
    fun testAxisBankCredit() {
        val sms = "Your A/c no. XX3948 is credited for Rs 2,500.00 on 18-09-26 by transfer from Rahul (UPI Ref no 998877665544)."
        val result = SmsParser.parse(sms, sender = "VM-AXISBK")

        assertNotNull(result)
        assertEquals(TransactionType.CREDIT, result!!.type)
        assertEquals(2500.0, result.amount, 0.001)
        assertEquals("Axis Bank", result.bank)
        assertEquals("Rahul", result.counterparty)
    }

    @Test
    fun testAxisBankDeclined() {
        val sms = "Transaction of Rs. 1200.00 on Axis Bank Credit Card XX4433 declined due to daily limit."
        val result = SmsParser.parse(sms, sender = "VM-AXISBK")

        assertNull("Declined should return null", result)
    }

    @Test
    fun testIdfcFirstDebit() {
        val sms = "Rs. 1,450.00 debited from your IDFC FIRST Bank A/c ending 7821 to Zomato via UPI Ref 382910384729 on 18-09-2026."
        val result = SmsParser.parse(sms, sender = "BZ-IDFCFB")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(1450.0, result.amount, 0.001)
        assertEquals("IDFC First Bank", result.bank)
        assertEquals("Zomato", result.counterparty)
        assertEquals("382910384729", result.refNo)
    }

    @Test
    fun testIdfcFirstReversal() {
        val sms = "Reversal of Rs. 450.00 has been credited to your IDFC FIRST Bank A/c ending 7821."
        val result = SmsParser.parse(sms, sender = "BZ-IDFCFB")

        assertNotNull(result)
        assertEquals(TransactionType.CREDIT, result!!.type)
        assertEquals(450.0, result.amount, 0.001)
        assertEquals("IDFC First Bank", result.bank)
    }

    @Test
    fun testYesBankDebit() {
        val sms = "YES BANK: INR 650.00 debited from A/c XX4123 on 18-Sep-26 towards Amazon Pay UPI Ref: 483920192837."
        val result = SmsParser.parse(sms, sender = "CP-YESBNK")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(650.0, result.amount, 0.001)
        assertEquals("Yes Bank", result.bank)
        assertEquals("Amazon Pay", result.counterparty)
    }

    @Test
    fun testYesBankCredit() {
        val sms = "YES BANK: INR 12,000.00 credited to A/c XX4123 on 18-Sep-26 by transfer from Employer Ref: 102938475610."
        val result = SmsParser.parse(sms, sender = "CP-YESBNK")

        assertNotNull(result)
        assertEquals(TransactionType.CREDIT, result!!.type)
        assertEquals(12000.0, result.amount, 0.001)
        assertEquals("Yes Bank", result.bank)
    }

    @Test
    fun testIndusIndBankDebit() {
        val sms = "Your IndusInd Bank A/c XX8932 has been debited by INR 1,100.00 on 18-09-2026. Transferred to Uber. UPI Ref 382910482910."
        val result = SmsParser.parse(sms, sender = "JD-INDBNK")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(1100.0, result.amount, 0.001)
        assertEquals("IndusInd Bank", result.bank)
        assertEquals("Uber", result.counterparty)
    }

    @Test
    fun testPnbDebit() {
        val sms = "Dear Customer, A/c *6789 debited by Rs 320.00 on 18/09/2026 through UPI to chai_corner@upi. Txn ID: 482910384910. PNB"
        val result = SmsParser.parse(sms, sender = "BP-PNBSMS")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(320.0, result.amount, 0.001)
        assertEquals("Punjab National Bank", result.bank)
        assertEquals("chai_corner@upi", result.counterparty)
    }

    @Test
    fun testUnionBankDebit() {
        val sms = "Union Bank: Rs. 980.00 debited from A/c ...4512 on 18-09-2026 towards BigBasket. UPI Ref: 482910384712."
        val result = SmsParser.parse(sms, sender = "VM-UNIONB")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(980.0, result.amount, 0.001)
        assertEquals("Union Bank of India", result.bank)
        assertEquals("BigBasket", result.counterparty)
    }

    @Test
    fun testCanaraBankDebit() {
        val sms = "Canara Bank: Rs 550.00 debited from A/C XX2345 on 18-09-2026 14:30:25 by UPI/423145678901 to Dominos."
        val result = SmsParser.parse(sms, sender = "VK-CANBNK")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(550.0, result.amount, 0.001)
        assertEquals("Canara Bank", result.bank)
        assertEquals("Dominos", result.counterparty)
    }

    @Test
    fun testBankOfIndiaDebit() {
        val sms = "BOI: A/C *5678 debited with INR 420.00 on 18-09-2026. Paid to BookMyShow. UPI Ref: 382910293847."
        val result = SmsParser.parse(sms, sender = "BW-BOISMS")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(420.0, result.amount, 0.001)
        assertEquals("Bank of India", result.bank)
        assertEquals("BookMyShow", result.counterparty)
    }

    @Test
    fun testSaraswatBankDebit() {
        val sms = "Saraswat Bank: Your A/c ...3344 is debited by Rs. 750.00 on 18-09-2026. Info: UPI/Apollo Pharmacy. UTR: 482910394829."
        val result = SmsParser.parse(sms, sender = "VM-SRSWAT")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(750.0, result.amount, 0.001)
        assertEquals("Saraswat Bank", result.bank)
        assertEquals("Apollo Pharmacy", result.counterparty)
    }

    @Test
    fun testCosmosBankDebit() {
        val sms = "Cosmos Bank: A/c no. XX9988 debited with Rs. 1,250.00 on 18-09-2026 for D-Mart purchase. UPI Ref: 482910384719."
        val result = SmsParser.parse(sms, sender = "VK-COSMOS")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(1250.0, result.amount, 0.001)
        assertEquals("Cosmos Bank", result.bank)
        assertEquals("D-Mart", result.counterparty)
    }

    @Test
    fun testPhonePeFallbackDebit() {
        val sms = "Paid Rs.450.00 to Swiggy on PhonePe using Bank of Baroda A/c XX8477. Txn ID: T26091812345."
        val result = SmsParser.parse(sms, sender = "BP-PHONPE")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(450.0, result.amount, 0.001)
        assertEquals("PhonePe", result.bank)
        assertEquals("Swiggy", result.counterparty)
    }

    @Test
    fun testGPayFallbackDebit() {
        val sms = "You sent Rs 1,200.00 to Rohit Kumar using Google Pay. UPI Ref 426819284712."
        val result = SmsParser.parse(sms, sender = "GOOGLE")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(1200.0, result.amount, 0.001)
        assertEquals("Google Pay", result.bank)
        assertEquals("Rohit Kumar", result.counterparty)
    }

    @Test
    fun testPaytmFallbackDebit() {
        val sms = "Money Sent: Rs 350.00 sent to Zomato from Paytm UPI. Ref: 987654321012."
        val result = SmsParser.parse(sms, sender = "AX-PAYTM")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(350.0, result.amount, 0.001)
        assertEquals("Paytm", result.bank)
        assertEquals("Zomato", result.counterparty)
    }

    @Test
    fun testAllowlistSenderValidation() {
        org.junit.Assert.assertTrue(BankSmsPatterns.isAllowlistedSender("VM-AXISBK"))
        org.junit.Assert.assertTrue(BankSmsPatterns.isAllowlistedSender("AD-HDFCBK"))
        org.junit.Assert.assertTrue(BankSmsPatterns.isAllowlistedSender("VK-CANBNK"))
        org.junit.Assert.assertTrue(BankSmsPatterns.isAllowlistedSender("BP-PHONPE"))
        org.junit.Assert.assertTrue(BankSmsPatterns.isAllowlistedSender("AM-SBICRD-S"))
        org.junit.Assert.assertTrue(BankSmsPatterns.isAllowlistedSender("BZ-JUPITR"))
        org.junit.Assert.assertTrue(BankSmsPatterns.isAllowlistedSender("CP-ONECRD"))
        org.junit.Assert.assertFalse(BankSmsPatterns.isAllowlistedSender("PROMO-DISCOUNT"))
    }

    @Test
    fun testSbiUpiDebitWithBalanceDisambiguation() {
        val sms = "Your a/c XXXX1234 is debited by Rs.500.00 on 20/06/26 transfer to VPA merchant@upi Ref No 123456789012. Avl Bal Rs.9,500.00 -SBI"
        val result = SmsParser.parse(sms, sender = "SBIPAY")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals(9500.0, result.balance!!, 0.001)
        assertEquals("State Bank of India", result.bank)
        assertEquals("123456789012", result.refNo)
    }

    @Test
    fun testHdfcSalaryCredit() {
        val sms = "Rs.15,000.00 deposited to a/c **1234 on 20-06-26. Info: SALARY-ACME Corp. Avl Bal: Rs.25,000.00"
        val result = SmsParser.parse(sms, sender = "HDFCBK")

        assertNotNull(result)
        assertEquals(TransactionType.CREDIT, result!!.type)
        assertEquals(15000.0, result.amount, 0.001)
        assertEquals(25000.0, result.balance!!, 0.001)
        assertEquals("HDFC Bank", result.bank)
        assertEquals("SALARY-ACME Corp", result.counterparty)
    }

    @Test
    fun testIciciCardSpendWithCreditLimit() {
        val sms = "Spent Rs.2,500.00 From ICICI Bank Card 1234 At Amazon On 20-Jun-2026. Avl bal:INR 8,500.00"
        val result = SmsParser.parse(sms, sender = "ICICIB")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(2500.0, result.amount, 0.001)
        assertEquals(8500.0, result.balance!!, 0.001)
        assertEquals("ICICI Bank", result.bank)
        assertEquals(SourceType.CARD, result.sourceType)
    }

    @Test
    fun testOneCardSpend() {
        val sms = "Spent Rs. 3,450.00 on your OneCard at Apple Store on 20-Jun. Available Limit: Rs. 96,550.00."
        val result = SmsParser.parse(sms, sender = "CP-ONECRD")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(3450.0, result.amount, 0.001)
        assertEquals(96550.0, result.creditLimit!!, 0.001)
        assertEquals("OneCard", result.bank)
        assertEquals(SourceType.CARD, result.sourceType)
    }

    @Test
    fun testCredPaySpend() {
        val sms = "Rs. 1,999.00 paid towards Credit Card Bill using CRED Pay. Ref: CRED987654."
        val result = SmsParser.parse(sms, sender = "CREDTX")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(1999.0, result.amount, 0.001)
        assertEquals("CRED", result.bank)
        assertEquals("CRED987654", result.refNo)
    }

    @Test
    fun testJupiterFederalDebit() {
        val sms = "Rs. 250.00 debited from your Jupiter account ending 4321 to Swiggy. UPI Ref: 483920192837."
        val result = SmsParser.parse(sms, sender = "BZ-JUPITR")

        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(250.0, result.amount, 0.001)
        assertEquals("Jupiter", result.bank)
        assertEquals("Swiggy", result.counterparty)
        assertEquals("483920192837", result.refNo)
    }

    @Test
    fun testBillDueReminderIsIgnored() {
        val sms = "Your credit card bill of Rs. 15,000.00 is due by 25-Oct-2026. Pay now to avoid late charges."
        val result = SmsParser.parse(sms, sender = "HDFCBK")

        assertNull("Bill due reminder should return null", result)
    }

    @Test
    fun testMinAmountDueReminderIsIgnored() {
        val sms = "Total due: Rs. 24,000.00. Min amount due Rs. 1,200.00 by 10-Nov. Pls pay min of Rs 1200."
        val result = SmsParser.parse(sms, sender = "ICICIB")

        assertNull("Min amount due reminder should return null", result)
    }

    @Test
    fun testPaymentRequestIsIgnored() {
        val sms = "Rahul has requested Rs. 500.00 from your account via Google Pay. Approve payment request to pay."
        val result = SmsParser.parse(sms, sender = "GOOGLE")

        assertNull("Payment request should return null", result)
    }

    @Test
    fun testUpiMandateCreatedIsIgnored() {
        val sms = "UPI-Mandate created for Netflix UMN ABC123XYZ for Rs.199.00 on 20-Jun-26. -SBI"
        val result = SmsParser.parse(sms, sender = "SBIPAY")

        assertNull("Mandate notification should return null", result)
    }

    @Test
    fun testFutureDebitNotificationIsIgnored() {
        val sms = "Your ICICI Bank account will be debited with Rs.199.00 for Netflix subscription on 25-Jun-2026."
        val result = SmsParser.parse(sms, sender = "ICICIB")

        assertNull("Future debit notification should return null", result)
    }

    @Test
    fun testPreApprovedLoanIsIgnored() {
        val sms = "Congratulations! You are eligible for a pre-approved personal loan of Rs. 5,00,000. Apply now!"
        val result = SmsParser.parse(sms, sender = "AXISBK")

        assertNull("Pre-approved loan should return null", result)
    }
}
