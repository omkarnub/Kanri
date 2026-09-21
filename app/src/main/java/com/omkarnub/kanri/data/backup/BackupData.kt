package com.omkarnub.kanri.data.backup

import com.omkarnub.kanri.data.db.BudgetEntity
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.CounterpartyCategoryMapEntity
import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.data.db.LendingRepaymentEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetEntity>,
    val lendingRecords: List<LendingEntity>,
    val counterpartyMappings: List<CounterpartyCategoryMapEntity>,
    val lendingRepayments: List<LendingRepaymentEntity> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 2
    }
}

data class BackupStats(
    val transactionCount: Int,
    val categoryCount: Int,
    val budgetCount: Int,
    val lendingCount: Int,
    val mappingCount: Int,
    val repaymentCount: Int = 0
)

object BackupJsonParser {

    fun toJson(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("version", payload.version)
        root.put("createdAt", payload.createdAt)
        root.put("appVersion", payload.appVersion)

        // Transactions
        val txArray = JSONArray()
        payload.transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("type", tx.type)
            obj.put("amount", tx.amount)
            obj.put("sourceType", tx.sourceType)
            obj.put("counterparty", tx.counterparty ?: JSONObject.NULL)
            obj.put("displayName", tx.displayName ?: JSONObject.NULL)
            obj.put("bank", tx.bank ?: JSONObject.NULL)
            obj.put("refNo", tx.refNo ?: JSONObject.NULL)
            obj.put("timestamp", tx.timestamp)
            obj.put("categoryId", tx.categoryId ?: JSONObject.NULL)
            obj.put("rawSms", tx.rawSms)
            obj.put("isDuplicate", tx.isDuplicate)
            obj.put("isManualEntry", tx.isManualEntry)
            obj.put("notes", tx.notes ?: JSONObject.NULL)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // Categories
        val catArray = JSONArray()
        payload.categories.forEach { cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("colorHex", cat.colorHex)
            obj.put("iconName", cat.iconName)
            obj.put("isCustom", cat.isCustom)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // Budgets
        val budgetArray = JSONArray()
        payload.budgets.forEach { b ->
            val obj = JSONObject()
            obj.put("monthKey", b.monthKey)
            obj.put("monthlyLimit", b.monthlyLimit)
            budgetArray.put(obj)
        }
        root.put("budgets", budgetArray)

        // Lending Records
        val lendingArray = JSONArray()
        payload.lendingRecords.forEach { l ->
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("personName", l.personName)
            obj.put("amount", l.amount)
            obj.put("type", l.type)
            obj.put("date", l.date)
            obj.put("dueDate", l.dueDate ?: JSONObject.NULL)
            obj.put("isSettled", l.isSettled)
            obj.put("notes", l.notes ?: JSONObject.NULL)
            obj.put("linkedTransactionId", l.linkedTransactionId ?: JSONObject.NULL)
            obj.put("originalAmount", l.originalAmount ?: JSONObject.NULL)
            lendingArray.put(obj)
        }
        root.put("lendingRecords", lendingArray)

        // Lending Repayments
        val repaymentArray = JSONArray()
        payload.lendingRepayments.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("lendingId", r.lendingId)
            obj.put("amount", r.amount)
            obj.put("paidAt", r.paidAt)
            obj.put("note", r.note ?: JSONObject.NULL)
            repaymentArray.put(obj)
        }
        root.put("lendingRepayments", repaymentArray)

        // Counterparty Mappings
        val mapArray = JSONArray()
        payload.counterpartyMappings.forEach { m ->
            val obj = JSONObject()
            obj.put("counterparty", m.counterparty)
            obj.put("categoryId", m.categoryId)
            mapArray.put(obj)
        }
        root.put("counterpartyMappings", mapArray)

        return root.toString(2)
    }

    fun fromJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())
        val appVersion = root.optString("appVersion", "1.0.0")

        // Parse transactions
        val txList = mutableListOf<TransactionEntity>()
        val txArray = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)
            txList.add(
                TransactionEntity(
                    id = obj.optLong("id", 0),
                    type = obj.getString("type"),
                    amount = obj.getDouble("amount"),
                    sourceType = obj.getString("sourceType"),
                    counterparty = if (obj.isNull("counterparty")) null else obj.getString("counterparty"),
                    displayName = if (obj.isNull("displayName")) null else obj.getString("displayName"),
                    bank = if (obj.isNull("bank")) null else obj.getString("bank"),
                    refNo = if (obj.isNull("refNo")) null else obj.getString("refNo"),
                    timestamp = obj.getLong("timestamp"),
                    categoryId = if (obj.isNull("categoryId")) null else obj.getLong("categoryId"),
                    rawSms = obj.optString("rawSms", ""),
                    isDuplicate = obj.optBoolean("isDuplicate", false),
                    isManualEntry = obj.optBoolean("isManualEntry", false),
                    notes = if (obj.isNull("notes")) null else obj.getString("notes")
                )
            )
        }

        // Parse categories
        val catList = mutableListOf<CategoryEntity>()
        val catArray = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catArray.length()) {
            val obj = catArray.getJSONObject(i)
            val catName = obj.getString("name")
            catList.add(
                CategoryEntity(
                    id = obj.optLong("id", 0),
                    name = catName,
                    colorHex = obj.getString("colorHex"),
                    iconName = obj.optString("iconName", "category"),
                    isCustom = obj.optBoolean("isCustom", false)
                )
            )
        }

        // Parse budgets
        val budgetList = mutableListOf<BudgetEntity>()
        val budgetArray = root.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until budgetArray.length()) {
            val obj = budgetArray.getJSONObject(i)
            budgetList.add(
                BudgetEntity(
                    monthKey = obj.getString("monthKey"),
                    monthlyLimit = obj.getDouble("monthlyLimit")
                )
            )
        }

        // Parse lending records
        val lendingList = mutableListOf<LendingEntity>()
        val lendingArray = root.optJSONArray("lendingRecords") ?: JSONArray()
        for (i in 0 until lendingArray.length()) {
            val obj = lendingArray.getJSONObject(i)
            lendingList.add(
                LendingEntity(
                    id = obj.optLong("id", 0),
                    personName = obj.getString("personName"),
                    amount = obj.getDouble("amount"),
                    type = obj.getString("type"),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    dueDate = if (obj.isNull("dueDate")) null else obj.getLong("dueDate"),
                    isSettled = obj.optBoolean("isSettled", false),
                    notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                    linkedTransactionId = if (obj.isNull("linkedTransactionId")) null else obj.getLong("linkedTransactionId"),
                    originalAmount = if (obj.isNull("originalAmount")) null else obj.optDouble("originalAmount")
                )
            )
        }

        // Parse lending repayments
        val repaymentList = mutableListOf<LendingRepaymentEntity>()
        val repaymentArray = root.optJSONArray("lendingRepayments") ?: JSONArray()
        for (i in 0 until repaymentArray.length()) {
            val obj = repaymentArray.getJSONObject(i)
            repaymentList.add(
                LendingRepaymentEntity(
                    id = obj.optLong("id", 0),
                    lendingId = obj.getLong("lendingId"),
                    amount = obj.getDouble("amount"),
                    paidAt = obj.optLong("paidAt", System.currentTimeMillis()),
                    note = if (obj.isNull("note")) null else obj.getString("note")
                )
            )
        }

        // Parse counterparty mappings
        val mapList = mutableListOf<CounterpartyCategoryMapEntity>()
        val mapArray = root.optJSONArray("counterpartyMappings") ?: JSONArray()
        for (i in 0 until mapArray.length()) {
            val obj = mapArray.getJSONObject(i)
            mapList.add(
                CounterpartyCategoryMapEntity(
                    counterparty = obj.getString("counterparty"),
                    categoryId = obj.getLong("categoryId")
                )
            )
        }

        return BackupPayload(
            version = version,
            createdAt = createdAt,
            appVersion = appVersion,
            transactions = txList,
            categories = catList,
            budgets = budgetList,
            lendingRecords = lendingList,
            counterpartyMappings = mapList,
            lendingRepayments = repaymentList
        )
    }
}
