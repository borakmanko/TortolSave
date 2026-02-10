//Transaction.kt
package com.example.financetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val type: String,        // "Income" or "Expense"
    val category: String,
    val description: String,
    val date: Long
)

// New: Quick Add Template
@Entity(tableName = "templates")
data class TransactionTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,           // "Jeepney Ride"
    val amount: Double,         // 13.0
    val type: String,           // "Expense"
    val category: String,       // "Transport"
    val description: String     // "Daily jeepney"
)

@Entity(tableName = "budget_settings")
data class BudgetSettings(
    @PrimaryKey
    val id: Int = 1, // Only one budget setting
    val monthlyBudget: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)