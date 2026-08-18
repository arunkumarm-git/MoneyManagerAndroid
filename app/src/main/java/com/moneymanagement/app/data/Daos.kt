package com.moneymanagement.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(account: Account): Long

    @Query("UPDATE accounts SET name=:name, type=:type, initialBalance=:initialBalance WHERE id=:id")
    suspend fun update(id: Long, name: String, type: String, initialBalance: Double)

    @Query("DELETE FROM accounts WHERE id=:id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM accounts ORDER BY id")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun getAll(): List<Account>

    @Insert
    suspend fun insertAll(accounts: List<Account>)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: Category): Long

    @Query("UPDATE categories SET name=:name, type=:type, monthlyBudget=:monthlyBudget WHERE id=:id")
    suspend fun update(id: Long, name: String, type: String, monthlyBudget: Double?)

    @Query("DELETE FROM categories WHERE id=:id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM categories ORDER BY type, name")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun getAll(): List<Category>

    @Insert
    suspend fun insertAll(categories: List<Category>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Query(
        """
        UPDATE transactions
        SET accountId=:accountId, toAccountId=:toAccountId, categoryId=:categoryId,
            type=:type, amount=:amount, note=:note, txnDate=:txnDate
        WHERE id=:id
        """
    )
    suspend fun update(
        id: Long,
        accountId: Long,
        toAccountId: Long?,
        categoryId: Long?,
        type: String,
        amount: Double,
        note: String?,
        txnDate: String,
    )

    @Query("DELETE FROM transactions WHERE id=:id")
    suspend fun delete(id: Long)

    @Query(
        """
        SELECT t.*, c.name AS categoryName, a.name AS accountName, ta.name AS toAccountName
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.categoryId
        LEFT JOIN accounts a ON a.id = t.accountId
        LEFT JOIN accounts ta ON ta.id = t.toAccountId
        ORDER BY t.txnDate DESC, t.id DESC
        """
    )
    fun observeAll(): Flow<List<TransactionRow>>

    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun getAll(): List<Transaction>

    @Insert
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface RecurringTransactionDao {
    @Insert
    suspend fun insert(recurring: RecurringTransaction): Long

    @Query("UPDATE recurring_transactions SET accountId=:accountId, toAccountId=:toAccountId, categoryId=:categoryId, type=:type, amount=:amount, frequency=:frequency, startDate=:startDate, nextDueDate=:nextDueDate, note=:note, autoApply=:autoApply, isActive=:isActive WHERE id=:id")
    suspend fun update(
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
    )

    @Query("UPDATE recurring_transactions SET nextDueDate=:nextDueDate WHERE id=:id")
    suspend fun updateNextDueDate(id: Long, nextDueDate: String)

    @Query("UPDATE recurring_transactions SET isActive=:isActive WHERE id=:id")
    suspend fun updateActive(id: Long, isActive: Boolean)

    @Query("DELETE FROM recurring_transactions WHERE id=:id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDate ASC")
    fun observeAll(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive=1 AND nextDueDate <= :date")
    suspend fun getDueTransactions(date: String): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions ORDER BY id")
    suspend fun getAll(): List<RecurringTransaction>

    @Insert
    suspend fun insertAll(list: List<RecurringTransaction>)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()
}

@Dao
interface SavingsGoalDao {
    @Insert
    suspend fun insert(goal: SavingsGoal): Long

    @Query("UPDATE savings_goals SET name=:name, targetAmount=:targetAmount, currentAmount=:currentAmount, targetDate=:targetDate, colorHex=:colorHex WHERE id=:id")
    suspend fun update(id: Long, name: String, targetAmount: Double, currentAmount: Double, targetDate: String?, colorHex: String)

    @Query("UPDATE savings_goals SET currentAmount = currentAmount + :delta WHERE id=:id")
    suspend fun addAmount(id: Long, delta: Double)

    @Query("DELETE FROM savings_goals WHERE id=:id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM savings_goals ORDER BY id ASC")
    fun observeAll(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals ORDER BY id ASC")
    suspend fun getAll(): List<SavingsGoal>

    @Insert
    suspend fun insertAll(list: List<SavingsGoal>)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll()
}

@Dao
interface SmsInboxDao {
    @Insert
    suspend fun insert(sms: SmsInbox): Long

    @Insert
    suspend fun insertAll(list: List<SmsInbox>)

    @Query("UPDATE sms_inbox SET status=:status WHERE id=:id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM sms_inbox WHERE id=:id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sms_inbox WHERE status != 'PENDING'")
    suspend fun deleteResolved()

    @Query("SELECT * FROM sms_inbox WHERE status = 'PENDING' ORDER BY txnDate DESC, id DESC")
    fun observePending(): Flow<List<SmsInbox>>

    @Query("SELECT COUNT(*) FROM sms_inbox WHERE status = 'PENDING'")
    fun countPending(): Flow<Int>

    @Query("SELECT * FROM sms_inbox ORDER BY id")
    suspend fun getAll(): List<SmsInbox>

    @Query("SELECT * FROM sms_inbox WHERE smsHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): SmsInbox?
}

@Dao
interface CreditCardDao {
    @Insert
    suspend fun insert(card: CreditCard): Long

    @Insert
    suspend fun insertAll(list: List<CreditCard>)

    @Query("UPDATE credit_cards SET cardName=:cardName, bankName=:bankName, lastFourDigits=:lastFourDigits, creditLimit=:creditLimit, currentBalance=:currentBalance, totalDue=:totalDue, minDue=:minDue, dueDate=:dueDate, isPaid=:isPaid, billingCycleDay=:billingCycleDay, colorHex=:colorHex, linkedAccountId=:linkedAccountId WHERE id=:id")
    suspend fun update(
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
    )

    @Query("UPDATE credit_cards SET totalDue=:totalDue, minDue=:minDue, dueDate=:dueDate, isPaid=0 WHERE id=:id")
    suspend fun updateBillStatement(id: Long, totalDue: Double, minDue: Double, dueDate: String?)

    @Query("UPDATE credit_cards SET isPaid=:isPaid WHERE id=:id")
    suspend fun updatePaidStatus(id: Long, isPaid: Boolean)

    @Query("DELETE FROM credit_cards WHERE id=:id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM credit_cards ORDER BY id ASC")
    fun observeAll(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards WHERE isPaid = 0 AND totalDue > 0 AND dueDate IS NOT NULL ORDER BY dueDate ASC")
    fun observeUpcomingBills(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards ORDER BY id ASC")
    suspend fun getAll(): List<CreditCard>

    @Query("SELECT * FROM credit_cards WHERE lastFourDigits = :last4 LIMIT 1")
    suspend fun findByLastFour(last4: String): CreditCard?

    @Query("SELECT * FROM credit_cards WHERE isPaid = 0 AND totalDue > 0 AND dueDate IS NOT NULL ORDER BY dueDate ASC")
    suspend fun getPendingBills(): List<CreditCard>
}


