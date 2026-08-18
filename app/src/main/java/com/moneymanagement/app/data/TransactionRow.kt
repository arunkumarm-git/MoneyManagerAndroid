package com.moneymanagement.app.data

import androidx.room.Embedded

data class TransactionRow(
    @Embedded val txn: Transaction,
    val categoryName: String?,
    val accountName: String?,
    val toAccountName: String?,
)
