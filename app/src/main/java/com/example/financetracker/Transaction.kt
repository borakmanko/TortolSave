//Transaction.kt
package com.example.financetracker
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val type: String,        // "Income" or "Expense"
    val category: String,
    val description: String,
    val date: Long,
    val bankId: Int = 0      // NEW: Reference to Bank
)

// Quick Add Template
@Entity(tableName = "templates")
data class TransactionTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,           // "Jeepney Ride"
    val amount: Double,         // 13.0
    val type: String,           // "Expense"
    val category: String,       // "Transport"
    val description: String,    // "Daily jeepney"
    val bankId: Int = 0         // NEW: Default bank for this template
)

@Entity(tableName = "budget_settings")
data class BudgetSettings(
    @PrimaryKey
    val id: Int = 1, // Only one budget setting
    val monthlyBudget: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

// NEW: Bank Entity
@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,                   // "GCash", "My Wallet", "Metrobank"
    val icon: String = "💰",           // Optional emoji icon
    val color: String = "#4CAF50",      // Optional color
    val createdAt: Long = System.currentTimeMillis()
)

// NEW: Transfer Entity (for bank-to-bank transfers)
@Entity(tableName = "transfers")
data class Transfer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fromBankId: Int,
    val toBankId: Int,
    val amount: Double,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)