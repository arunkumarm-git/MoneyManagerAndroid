package com.moneymanagement.app.data.backup

import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.Category
import com.moneymanagement.app.data.Transaction
import org.json.JSONArray
import org.json.JSONObject

data class DatabaseBackup(
    val version: Int,
    val exportedAt: String,
    val accounts: List<Account>,
    val categories: List<Category>,
    val transactions: List<Transaction>,
)

object JsonBackupHelper {

    fun exportToJson(
        accounts: List<Account>,
        categories: List<Category>,
        transactions: List<Transaction>,
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", java.time.LocalDateTime.now().toString())

        // Accounts
        val accArr = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("type", acc.type)
            obj.put("initialBalance", acc.initialBalance)
            obj.put("createdAt", acc.createdAt)
            accArr.put(obj)
        }
        root.put("accounts", accArr)

        // Categories
        val catArr = JSONArray()
        for (cat in categories) {
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("type", cat.type)
            if (cat.monthlyBudget != null) {
                obj.put("monthlyBudget", cat.monthlyBudget)
            } else {
                obj.put("monthlyBudget", JSONObject.NULL)
            }
            obj.put("createdAt", cat.createdAt)
            catArr.put(obj)
        }
        root.put("categories", catArr)

        // Transactions
        val txnArr = JSONArray()
        for (txn in transactions) {
            val obj = JSONObject()
            obj.put("id", txn.id)
            obj.put("accountId", txn.accountId)
            if (txn.toAccountId != null) obj.put("toAccountId", txn.toAccountId) else obj.put("toAccountId", JSONObject.NULL)
            if (txn.categoryId != null) obj.put("categoryId", txn.categoryId) else obj.put("categoryId", JSONObject.NULL)
            obj.put("type", txn.type)
            obj.put("amount", txn.amount)
            if (txn.note != null) obj.put("note", txn.note) else obj.put("note", JSONObject.NULL)
            obj.put("txnDate", txn.txnDate)
            obj.put("createdAt", txn.createdAt)
            txnArr.put(obj)
        }
        root.put("transactions", txnArr)

        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): DatabaseBackup {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val exportedAt = root.optString("exportedAt", "")

        val accList = mutableListOf<Account>()
        val accArr = root.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accArr.length()) {
            val obj = accArr.getJSONObject(i)
            accList.add(
                Account(
                    id = obj.optLong("id", 0),
                    name = obj.getString("name"),
                    type = obj.getString("type"),
                    initialBalance = obj.optDouble("initialBalance", 0.0),
                    createdAt = obj.optString("createdAt", ""),
                )
            )
        }

        val catList = mutableListOf<Category>()
        val catArr = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catArr.length()) {
            val obj = catArr.getJSONObject(i)
            val budget = if (obj.isNull("monthlyBudget")) null else obj.optDouble("monthlyBudget")
            catList.add(
                Category(
                    id = obj.optLong("id", 0),
                    name = obj.getString("name"),
                    type = obj.getString("type"),
                    monthlyBudget = budget,
                    createdAt = obj.optString("createdAt", ""),
                )
            )
        }

        val txnList = mutableListOf<Transaction>()
        val txnArr = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txnArr.length()) {
            val obj = txnArr.getJSONObject(i)
            val toAccId = if (obj.isNull("toAccountId")) null else obj.optLong("toAccountId")
            val catId = if (obj.isNull("categoryId")) null else obj.optLong("categoryId")
            val note = if (obj.isNull("note")) null else obj.optString("note")
            txnList.add(
                Transaction(
                    id = obj.optLong("id", 0),
                    accountId = obj.getLong("accountId"),
                    toAccountId = toAccId,
                    categoryId = catId,
                    type = obj.getString("type"),
                    amount = obj.getDouble("amount"),
                    note = note,
                    txnDate = obj.getString("txnDate"),
                    createdAt = obj.optString("createdAt", ""),
                )
            )
        }

        return DatabaseBackup(
            version = version,
            exportedAt = exportedAt,
            accounts = accList,
            categories = catList,
            transactions = txnList,
        )
    }
}
