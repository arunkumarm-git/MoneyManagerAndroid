package com.moneymanagement.app.data

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
        ForeignKey(entity = CreditCard::class, parentColumns = ["id"], childColumns = ["creditCardId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("accountId"), Index("toAccountId"), Index("categoryId"), Index("creditCardId")],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long?,       // null when paid by credit card
    val toAccountId: Long?,
    val categoryId: Long?,
    val creditCardId: Long? = null,    // set when expense is charged to a credit card
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

@Entity(
    tableName = "sms_inbox",
    indices = [Index(value = ["smsHash"], unique = true)],
)
data class SmsInbox(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val rawBody: String,
    val amount: Double,
    val type: String, // "expense" or "income"
    val suggestedCategory: String?,
    val suggestedAccountId: Long?,
    val merchantOrNote: String?,
    val accountReference: String?,
    val txnDate: String,
    val status: String = "PENDING", // "PENDING", "APPROVED", "DISMISSED"
    val smsHash: String,
    val createdAt: String,
)

@Entity(
    tableName = "credit_cards",
    indices = [Index(value = ["lastFourDigits"])],
)
data class CreditCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardName: String,
    val bankName: String,
    val lastFourDigits: String? = null,
    val creditLimit: Double = 0.0,
    val currentBalance: Double = 0.0,
    val totalDue: Double = 0.0,
    val minDue: Double = 0.0,
    val dueDate: String? = null, // e.g. "2026-08-25"
    val isPaid: Boolean = false,
    val billingCycleDay: Int? = null,
    val colorHex: String = "#1E293B",
    val linkedAccountId: Long? = null,
    val createdAt: String,
)

@Entity(
    tableName = "emergency_funds",
    foreignKeys = [
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["linkedAccountId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("linkedAccountId")],
)
data class EmergencyFundAllocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // e.g. "HDFC High-Interest Savings", "Parag Parikh Liquid Fund", "Cash in locker"
    val institution: String,             // e.g. "HDFC Bank", "Mutual Fund AMC", "Cash", "SBI Bank"
    val storageType: String,             // "bank_savings", "liquid_fund", "fixed_deposit", "cash", "other"
    val amount: Double,                  // Amount stored
    val returnRate: Double? = null,      // e.g. 7.1% p.a.
    val notes: String? = null,           // e.g. "Instant ATM/UPI access", "T+1 redemption limit", "Home safe"
    val linkedAccountId: Long? = null,   // Optional reference to an Account in the app
    val createdAt: String,
    val updatedAt: String,
)



