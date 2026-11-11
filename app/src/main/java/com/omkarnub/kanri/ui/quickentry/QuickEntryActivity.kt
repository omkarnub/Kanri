package com.omkarnub.kanri.ui.quickentry

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.wallet.WalletPreferences
import com.omkarnub.kanri.data.wallet.WalletResolver
import com.omkarnub.kanri.ui.common.applyWindowBlur
import com.omkarnub.kanri.ui.home.AddTransactionDialog
import com.omkarnub.kanri.ui.lending.SplitExpenseEntryDialog
import com.omkarnub.kanri.ui.theme.KanriTheme
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.KanriWidgetsUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Translucent dialog activity launched directly from home screen widgets.
 * Keeps the user on their home screen with a transparent window background,
 * presenting the tactile numpad and full manual entry workflow without
 * launching the main app.
 */
class QuickEntryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        applyWindowBlur(window, 120)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.35f)

        val action = intent?.action ?: KanriWidgetActions.ACTION_ADD_EXPENSE
        val isIncome = action == KanriWidgetActions.ACTION_ADD_INCOME
        val isSplit = action == KanriWidgetActions.ACTION_SPLIT_BILL
        val initialType = if (isIncome) "CREDIT" else "DEBIT"

        val db = KanriDatabase.getDatabase(applicationContext)

        setContent {
            KanriTheme {
                val categories by db.categoryDao().getAllCategories().collectAsState(initial = emptyList())
                val peopleListFlow = remember {
                    db.lendingDao().getAllRecords().map { list -> list.map { it.personName }.distinct() }
                }
                val peopleList by peopleListFlow.collectAsState(initial = emptyList())

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    if (isSplit) {
                        SplitExpenseEntryDialog(
                            onDismiss = { finish() },
                            onSaveLenderSplit = { friendNames, perPersonShare, eventDesc ->
                                saveSplitAsLender(friendNames, perPersonShare, eventDesc)
                            },
                            onSaveBorrowerSplit = { payerName, userShare, eventDesc ->
                                saveSplitAsBorrower(payerName, userShare, eventDesc)
                            },
                            existingPeople = peopleList
                        )
                    } else {
                        AddTransactionDialog(
                            initialType = initialType,
                            categories = categories,
                            onDismiss = { finish() },
                            onConfirm = { amount, type, counterparty, sourceType, categoryId, wallet, timestamp ->
                                saveTransaction(amount, type, counterparty, sourceType, categoryId, wallet, timestamp)
                            },
                            onOpenLendBorrow = { _, _, _, _ ->
                                val openIntent = com.omkarnub.kanri.ui.theme.AppIconManager.createLauncherIntent(this@QuickEntryActivity).apply {
                                    this.action = KanriWidgetActions.ACTION_OPEN_LENDING
                                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                try {
                                    startActivity(openIntent)
                                } catch (_: Exception) {}
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun saveTransaction(
        amount: Double,
        type: String,
        counterparty: String,
        sourceType: String,
        categoryId: Long?,
        walletChoice: String?,
        timestamp: Long
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = KanriDatabase.getDatabase(applicationContext)
            val categoryDao = db.categoryDao()
            val transactionDao = db.transactionDao()

            val resolvedCategoryId = categoryId ?: if (counterparty.isNotBlank()) {
                (categoryDao.findSmartRuleForCounterparty(counterparty.trim())
                    ?: categoryDao.getMappingForCounterparty(counterparty.trim()))?.categoryId
            } else null

            val atmMode = WalletPreferences.getInstance(applicationContext).atmWithdrawalMode
            val resolvedWallet = WalletResolver.resolve(
                sourceType = sourceType,
                type = type,
                userChoice = walletChoice,
                atmMode = atmMode
            )

            val entity = TransactionEntity(
                type = type.uppercase(java.util.Locale.ROOT),
                amount = amount,
                sourceType = sourceType.uppercase(java.util.Locale.ROOT),
                counterparty = counterparty.trim(),
                displayName = counterparty.trim(),
                bank = if (sourceType.equals("CASH", ignoreCase = true)) "Cash" else null,
                refNo = null,
                timestamp = timestamp,
                categoryId = resolvedCategoryId,
                rawSms = "Manual entry: $counterparty",
                isManualEntry = true,
                wallet = resolvedWallet.wallet,
                transferToWallet = resolvedWallet.transferToWallet
            )
            transactionDao.insert(entity)

            KanriWidgetsUpdater.updateAllWidgets(applicationContext, playBurst = true)

            withContext(Dispatchers.Main) {
                val label = if (type.equals("DEBIT", ignoreCase = true)) "Expense" else "Income"
                Toast.makeText(applicationContext, "$label recorded", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveSplitAsLender(
        friendNames: List<String>,
        perPersonShare: Double,
        eventDescription: String
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = KanriDatabase.getDatabase(applicationContext)
            val now = System.currentTimeMillis()
            val notePrefix = if (eventDescription.isNotBlank()) "Split: ${eventDescription.trim()}" else "Split Bill"
            friendNames.forEachIndexed { index, name ->
                val trimmedName = name.trim().ifBlank { "Friend ${index + 1}" }
                val record = LendingEntity(
                    personName = trimmedName,
                    amount = perPersonShare,
                    type = "LENT",
                    date = now,
                    dueDate = null,
                    isSettled = false,
                    notes = notePrefix
                )
                db.lendingDao().insert(record)
            }
            KanriWidgetsUpdater.updateAllWidgets(applicationContext, playBurst = true)
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Split recorded", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveSplitAsBorrower(
        payerName: String,
        userShare: Double,
        eventDescription: String
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = KanriDatabase.getDatabase(applicationContext)
            val now = System.currentTimeMillis()
            val trimmedPayer = payerName.trim().ifBlank { "Friend" }
            val notePrefix = if (eventDescription.isNotBlank()) "Split: ${eventDescription.trim()}" else "Split Bill"
            val record = LendingEntity(
                personName = trimmedPayer,
                amount = userShare,
                type = "BORROWED",
                date = now,
                dueDate = null,
                isSettled = false,
                notes = notePrefix
            )
            db.lendingDao().insert(record)
            KanriWidgetsUpdater.updateAllWidgets(applicationContext, playBurst = true)
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Split recorded", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

