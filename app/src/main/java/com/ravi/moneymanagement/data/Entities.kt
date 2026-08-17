package com.ravi.moneymanagement.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val initialBalance: Double,
    val createdAt: String,
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val monthlyBudget: Double?,
    val createdAt: String,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["toAccountId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("accountId"), Index("toAccountId"), Index("categoryId")],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long?,
    val type: String,
    val amount: Double,
    val note: String?,
    val txnDate: String,
    val createdAt: String,
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long?,
    val type: String,
    val amount: Double,
    val frequency: String, // "daily", "weekly", "monthly", "yearly"
    val startDate: String,
    val nextDueDate: String,
    val note: String?,
    val autoApply: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: String,
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: String? = null,
    val colorHex: String = "#3B82F6",
    val createdAt: String,
)

