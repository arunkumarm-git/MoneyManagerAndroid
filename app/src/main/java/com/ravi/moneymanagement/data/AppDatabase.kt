package com.ravi.moneymanagement.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

import androidx.room.migration.Migration

@Database(
    entities = [
        Account::class,
        Category::class,
        Transaction::class,
        RecurringTransaction::class,
        SavingsGoal::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `toAccountId` INTEGER,
                        `categoryId` INTEGER,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `nextDueDate` TEXT NOT NULL,
                        `note` TEXT,
                        `autoApply` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `currentAmount` REAL NOT NULL,
                        `targetDate` TEXT,
                        `colorHex` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_manager.db",
                )
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }
                }).build().also { instance = it }
            }
    }
}
