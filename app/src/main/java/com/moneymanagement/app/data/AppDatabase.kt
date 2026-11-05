package com.moneymanagement.app.data

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
        SmsInbox::class,
        CreditCard::class,
        EmergencyFundAllocation::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun smsInboxDao(): SmsInboxDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun emergencyFundDao(): EmergencyFundDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sms_inbox` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sender` TEXT NOT NULL,
                        `rawBody` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `type` TEXT NOT NULL,
                        `suggestedCategory` TEXT,
                        `suggestedAccountId` INTEGER,
                        `merchantOrNote` TEXT,
                        `accountReference` TEXT,
                        `txnDate` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `smsHash` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_sms_inbox_smsHash` ON `sms_inbox` (`smsHash`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credit_cards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cardName` TEXT NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `lastFourDigits` TEXT,
                        `creditLimit` REAL NOT NULL,
                        `currentBalance` REAL NOT NULL,
                        `totalDue` REAL NOT NULL,
                        `minDue` REAL NOT NULL,
                        `dueDate` TEXT,
                        `isPaid` INTEGER NOT NULL,
                        `billingCycleDay` INTEGER,
                        `colorHex` TEXT NOT NULL,
                        `linkedAccountId` INTEGER,
                        `createdAt` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_credit_cards_lastFourDigits` ON `credit_cards` (`lastFourDigits`)
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop any temporary table from previously failed attempts
                db.execSQL("DROP TABLE IF EXISTS `transactions_new`")

                // Recreate transactions with nullable accountId + new creditCardId column
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountId` INTEGER,
                        `toAccountId` INTEGER,
                        `categoryId` INTEGER,
                        `creditCardId` INTEGER,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `note` TEXT,
                        `txnDate` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`creditCardId`) REFERENCES `credit_cards`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO `transactions_new` SELECT `id`, `accountId`, `toAccountId`, `categoryId`, NULL, `type`, `amount`, `note`, `txnDate`, `createdAt` FROM `transactions`")
                db.execSQL("DROP TABLE `transactions`")
                db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions`(`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_toAccountId` ON `transactions`(`toAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions`(`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_creditCardId` ON `transactions`(`creditCardId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `emergency_funds` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `institution` TEXT NOT NULL,
                        `storageType` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `returnRate` REAL,
                        `notes` TEXT,
                        `linkedAccountId` INTEGER,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL,
                        FOREIGN KEY(`linkedAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_emergency_funds_linkedAccountId` ON `emergency_funds` (`linkedAccountId`)
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }
                }).build().also { instance = it }
            }
    }
}


