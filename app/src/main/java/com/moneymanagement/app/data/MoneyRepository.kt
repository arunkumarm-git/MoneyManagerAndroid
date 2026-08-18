package com.moneymanagement.app.data

import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

fun nowIso(): String = LocalDateTime.now().withNano(0).toString()

class MoneyRepository(private val db: AppDatabase) {
    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val recurringDao = db.recurringTransactionDao()
    private val savingsGoalDao = db.savingsGoalDao()
    private val smsInboxDao = db.smsInboxDao()
    private val creditCardDao = db.creditCardDao()

    val accounts: Flow<List<Account>> = accountDao.observeAll()
    val categories: Flow<List<Category>> = categoryDao.observeAll()
    val transactions: Flow<List<TransactionRow>> = transactionDao.observeAll()
    val recurringTransactions: Flow<List<RecurringTransaction>> = recurringDao.observeAll()
    val savingsGoals: Flow<List<SavingsGoal>> = savingsGoalDao.observeAll()
    val pendingSms: Flow<List<SmsInbox>> = smsInboxDao.observePending()
    val pendingSmsCount: Flow<Int> = smsInboxDao.countPending()
    val creditCards: Flow<List<CreditCard>> = creditCardDao.observeAll()
    val upcomingCreditCardBills: Flow<List<CreditCard>> = creditCardDao.observeUpcomingBills()


    suspend fun seedDefaultsIfNeeded() {
        if (accountDao.count() == 0) {
            addAccount("Cash", "cash", 0.0)
            addAccount("Bank Account", "bank", 0.0)
        }
        if (categoryDao.count() == 0) {
            addCategory("Salary", "income", null)
            addCategory("Other Income", "income", null)
            val defaults = listOf(
                "Food" to 6000.0,
                "Transport" to 2000.0,
                "Shopping" to 3000.0,
                "Bills" to 4000.0,
                "Entertainment" to 1500.0,
                "Health" to 2000.0,
                "Other" to null,
            )
            for ((name, budget) in defaults) {
                addCategory(name, "expense", budget)
            }
        }
    }

    // --- Accounts ---
    suspend fun addAccount(name: String, type: String, initialBalance: Double): Long =
        accountDao.insert(Account(name = name, type = type, initialBalance = initialBalance, createdAt = nowIso()))

    suspend fun updateAccount(id: Long, name: String, type: String, initialBalance: Double) =
        accountDao.update(id, name, type, initialBalance)

    suspend fun deleteAccount(id: Long) = accountDao.delete(id)

    // --- Categories ---
    suspend fun addCategory(name: String, type: String, monthlyBudget: Double?): Long =
        categoryDao.insert(Category(name = name, type = type, monthlyBudget = monthlyBudget, createdAt = nowIso()))

    suspend fun updateCategory(id: Long, name: String, type: String, monthlyBudget: Double?) =
        categoryDao.update(id, name, type, monthlyBudget)

    suspend fun deleteCategory(id: Long) = categoryDao.delete(id)

    // --- Transactions ---
    suspend fun addTransaction(
        accountId: Long,
        type: String,
        amount: Double,
        txnDate: String,
        categoryId: Long?,
        toAccountId: Long?,
        note: String,
    ): Long = transactionDao.insert(
        Transaction(
            accountId = accountId,
            toAccountId = toAccountId,
            categoryId = categoryId,
            type = type,
            amount = amount,
            note = note,
            txnDate = txnDate,
            createdAt = nowIso(),
        )
    )

    suspend fun updateTransaction(
        id: Long,
        accountId: Long,
        type: String,
        amount: Double,
        txnDate: String,
        categoryId: Long?,
        toAccountId: Long?,
        note: String,
    ) = transactionDao.update(id, accountId, toAccountId, categoryId, type, amount, note, txnDate)

    suspend fun deleteTransaction(id: Long) = transactionDao.delete(id)

    // --- Backup & Restore ---
    suspend fun getAllAccounts(): List<Account> = accountDao.getAll()
    suspend fun getAllCategories(): List<Category> = categoryDao.getAll()
    suspend fun getAllTransactions(): List<Transaction> = transactionDao.getAll()

    suspend fun exportBackupJson(): String {
        return com.moneymanagement.app.data.backup.JsonBackupHelper.exportToJson(
            accounts = accountDao.getAll(),
            categories = categoryDao.getAll(),
            transactions = transactionDao.getAll(),
        )
    }

    suspend fun restoreBackupJson(jsonString: String) {
        val backup = com.moneymanagement.app.data.backup.JsonBackupHelper.parseFromJson(jsonString)
        // Clear all existing data
        transactionDao.deleteAll()
        categoryDao.deleteAll()
        accountDao.deleteAll()

        // Re-insert backed up data
        accountDao.insertAll(backup.accounts)
        categoryDao.insertAll(backup.categories)
        transactionDao.insertAll(backup.transactions)
    }

    suspend fun importTransactionsCsv(csvContent: String): Int {
        val rows = com.moneymanagement.app.data.backup.CsvHelper.parseCsv(csvContent)
        return importParsedTransactions(rows)
    }

    suspend fun importParsedTransactions(rows: List<com.moneymanagement.app.data.backup.CsvHelper.ParsedCsvRow>): Int {
        if (rows.isEmpty()) return 0

        val currentAccounts = accountDao.getAll().toMutableList()
        val currentCategories = categoryDao.getAll().toMutableList()

        var count = 0
        for (row in rows) {
            // Find or create account
            var acc = currentAccounts.find { it.name.equals(row.accountName, ignoreCase = true) }
            if (acc == null) {
                val newId = addAccount(if (row.accountName.isBlank()) "Default" else row.accountName, "bank", 0.0)
                acc = Account(id = newId, name = if (row.accountName.isBlank()) "Default" else row.accountName, type = "bank", initialBalance = 0.0, createdAt = nowIso())
                currentAccounts.add(acc)
            }

            // Find or create toAccount (for transfer)
            var toAcc: Account? = null
            if (row.type == "transfer" && row.toAccountName.isNotBlank()) {
                toAcc = currentAccounts.find { it.name.equals(row.toAccountName, ignoreCase = true) }
                if (toAcc == null) {
                    val newId = addAccount(row.toAccountName, "bank", 0.0)
                    toAcc = Account(id = newId, name = row.toAccountName, type = "bank", initialBalance = 0.0, createdAt = nowIso())
                    currentAccounts.add(toAcc)
                }
            }

            // Find or create category
            var cat: Category? = null
            if (row.categoryName.isNotBlank()) {
                cat = currentCategories.find { it.name.equals(row.categoryName, ignoreCase = true) && it.type.equals(row.type, ignoreCase = true) }
                if (cat == null) {
                    val newId = addCategory(row.categoryName, row.type, null)
                    cat = Category(id = newId, name = row.categoryName, type = row.type, monthlyBudget = null, createdAt = nowIso())
                    currentCategories.add(cat)
                }
            }

            addTransaction(
                accountId = acc.id,
                type = row.type,
                amount = row.amount,
                txnDate = row.txnDate,
                categoryId = cat?.id,
                toAccountId = toAcc?.id,
                note = row.note,
            )
            count++
        }
        return count
    }


    // --- Recurring Transactions ---
    suspend fun addRecurringTransaction(
        accountId: Long,
        toAccountId: Long?,
        categoryId: Long?,
        type: String,
        amount: Double,
        frequency: String,
        startDate: String,
        nextDueDate: String,
        note: String?,
        autoApply: Boolean = true,
        isActive: Boolean = true,
    ): Long = recurringDao.insert(
        RecurringTransaction(
            accountId = accountId,
            toAccountId = toAccountId,
            categoryId = categoryId,
            type = type,
            amount = amount,
            frequency = frequency,
            startDate = startDate,
            nextDueDate = nextDueDate,
            note = note,
            autoApply = autoApply,
            isActive = isActive,
            createdAt = nowIso(),
        )
    )

    suspend fun updateRecurringTransaction(
        id: Long,
        accountId: Long,
        toAccountId: Long?,
        categoryId: Long?,
        type: String,
        amount: Double,
        frequency: String,
        startDate: String,
        nextDueDate: String,
        note: String?,
        autoApply: Boolean,
        isActive: Boolean,
    ) = recurringDao.update(
        id, accountId, toAccountId, categoryId, type, amount, frequency, startDate, nextDueDate, note, autoApply, isActive
    )

    suspend fun toggleRecurringActive(id: Long, isActive: Boolean) =
        recurringDao.updateActive(id, isActive)

    suspend fun deleteRecurringTransaction(id: Long) =
        recurringDao.delete(id)

    suspend fun processDueRecurringTransactions(): Int {
        val today = java.time.LocalDate.now().toString()
        val dueList = recurringDao.getDueTransactions(today)
        var appliedCount = 0

        for (item in dueList) {
            if (item.autoApply) {
                addTransaction(
                    accountId = item.accountId,
                    type = item.type,
                    amount = item.amount,
                    txnDate = item.nextDueDate,
                    categoryId = item.categoryId,
                    toAccountId = item.toAccountId,
                    note = item.note ?: "Auto recurring payment (${item.frequency})",
                )
                appliedCount++
            }
            val nextDate = computeNextDueDate(item.nextDueDate, item.frequency)
            recurringDao.updateNextDueDate(item.id, nextDate)
        }
        return appliedCount
    }

    // --- Savings Goals ---
    suspend fun addSavingsGoal(
        name: String,
        targetAmount: Double,
        currentAmount: Double = 0.0,
        targetDate: String? = null,
        colorHex: String = "#3B82F6",
    ): Long = savingsGoalDao.insert(
        SavingsGoal(
            name = name,
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            targetDate = targetDate,
            colorHex = colorHex,
            createdAt = nowIso(),
        )
    )

    suspend fun updateSavingsGoal(
        id: Long,
        name: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: String?,
        colorHex: String,
    ) = savingsGoalDao.update(id, name, targetAmount, currentAmount, targetDate, colorHex)

    suspend fun depositToSavingsGoal(id: Long, amount: Double) =
        savingsGoalDao.addAmount(id, amount)

    suspend fun deleteSavingsGoal(id: Long) =
        savingsGoalDao.delete(id)

    // --- SMS Inbox ---
    suspend fun addSmsInbox(sms: SmsInbox): Long =
        smsInboxDao.insert(sms)

    suspend fun addAllSmsInbox(list: List<SmsInbox>) =
        smsInboxDao.insertAll(list)

    suspend fun isSmsDuplicate(hash: String): Boolean =
        smsInboxDao.findByHash(hash) != null

    suspend fun approveSms(
        smsId: Long,
        accountId: Long,
        categoryId: Long?,
        toAccountId: Long?,
        type: String,
        amount: Double,
        txnDate: String,
        note: String,
    ): Long {
        val txnId = addTransaction(
            accountId = accountId,
            type = type,
            amount = amount,
            txnDate = txnDate,
            categoryId = categoryId,
            toAccountId = toAccountId,
            note = note,
        )
        smsInboxDao.updateStatus(smsId, "APPROVED")
        return txnId
    }

    suspend fun dismissSms(smsId: Long) {
        smsInboxDao.updateStatus(smsId, "DISMISSED")
    }

    suspend fun deleteSmsDraft(smsId: Long) {
        smsInboxDao.delete(smsId)
    }

    suspend fun clearResolvedSms() {
        smsInboxDao.deleteResolved()
    }

    // --- Credit Cards ---
    suspend fun getAllCreditCards(): List<CreditCard> = creditCardDao.getAll()

    suspend fun getPendingCreditCardBills(): List<CreditCard> = creditCardDao.getPendingBills()

    suspend fun addCreditCard(
        cardName: String,
        bankName: String,
        lastFourDigits: String?,
        creditLimit: Double,
        currentBalance: Double = 0.0,
        totalDue: Double = 0.0,
        minDue: Double = 0.0,
        dueDate: String? = null,
        billingCycleDay: Int? = null,
        colorHex: String = "#1E293B",
        linkedAccountId: Long? = null,
    ): Long = creditCardDao.insert(
        CreditCard(
            cardName = cardName,
            bankName = bankName,
            lastFourDigits = lastFourDigits,
            creditLimit = creditLimit,
            currentBalance = currentBalance,
            totalDue = totalDue,
            minDue = minDue,
            dueDate = dueDate,
            isPaid = false,
            billingCycleDay = billingCycleDay,
            colorHex = colorHex,
            linkedAccountId = linkedAccountId,
            createdAt = nowIso(),
        )
    )

    suspend fun updateCreditCard(
        id: Long,
        cardName: String,
        bankName: String,
        lastFourDigits: String?,
        creditLimit: Double,
        currentBalance: Double,
        totalDue: Double,
        minDue: Double,
        dueDate: String?,
        isPaid: Boolean,
        billingCycleDay: Int?,
        colorHex: String,
        linkedAccountId: Long?,
    ) = creditCardDao.update(
        id, cardName, bankName, lastFourDigits, creditLimit, currentBalance, totalDue, minDue, dueDate, isPaid, billingCycleDay, colorHex, linkedAccountId
    )

    suspend fun updateCreditCardBillStatement(
        id: Long,
        totalDue: Double,
        minDue: Double,
        dueDate: String?,
    ) = creditCardDao.updateBillStatement(id, totalDue, minDue, dueDate)

    suspend fun markCreditCardBillPaid(
        cardId: Long,
        isPaid: Boolean,
        payFromAccountId: Long? = null,
        amount: Double? = null,
    ) {
        creditCardDao.updatePaidStatus(cardId, isPaid)
        if (isPaid && payFromAccountId != null && amount != null && amount > 0) {
            val cards = creditCardDao.getAll()
            val card = cards.find { it.id == cardId }
            val cardTitle = card?.cardName ?: "Credit Card"
            val billCat = categoryDao.getAll().find { it.name.contains("Bill", ignoreCase = true) }
            addTransaction(
                accountId = payFromAccountId,
                type = "expense",
                amount = amount,
                txnDate = java.time.LocalDate.now().toString(),
                categoryId = billCat?.id,
                toAccountId = null,
                note = "Bill Payment: $cardTitle",
            )
        }
    }

    suspend fun deleteCreditCard(id: Long) = creditCardDao.delete(id)

    suspend fun findCreditCardByLastFour(last4: String): CreditCard? =
        creditCardDao.findByLastFour(last4)
}


fun computeNextDueDate(currentDue: String, frequency: String): String {
    val date = runCatching { java.time.LocalDate.parse(currentDue) }.getOrNull() ?: java.time.LocalDate.now()
    val next = when (frequency.lowercase()) {
        "daily" -> date.plusDays(1)
        "weekly" -> date.plusWeeks(1)
        "yearly" -> date.plusYears(1)
        else -> date.plusMonths(1)
    }
    return next.toString()
}
