//TransactionDao.kt
package com.example.financetracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // Transactions
    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    // Get transactions with pagination (limit 10 per page)
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
    fun getTransactionsPaginated(limit: Int, offset: Int): Flow<List<Transaction>>

    // Get total count for pagination
    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Flow<Int>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Income'")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Expense'")
    fun getTotalExpenses(): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'Expense' GROUP BY category")
    fun getExpensesByCategory(): Flow<List<CategoryTotal>>

    // Templates
    @Insert
    suspend fun insertTemplate(template: TransactionTemplate)

    @Update
    suspend fun updateTemplate(template: TransactionTemplate)

    @Delete
    suspend fun deleteTemplate(template: TransactionTemplate)

    @Query("SELECT * FROM templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<TransactionTemplate>>

    // Budget Settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetSettings(settings: BudgetSettings)

    @Query("SELECT * FROM budget_settings WHERE id = 1")
    fun getBudgetSettings(): Flow<BudgetSettings?>

    // Statistics queries
    @Query("""
    SELECT SUM(amount) FROM transactions 
    WHERE type = 'Expense' 
    AND date >= :startOfMonth 
    AND date <= :endOfMonth
""")
    fun getMonthlyExpenses(startOfMonth: Long, endOfMonth: Long): Flow<Double?>

    @Query("""
    SELECT SUM(amount) FROM transactions 
    WHERE type = 'Income' 
    AND date >= :startOfMonth 
    AND date <= :endOfMonth
""")
    fun getMonthlyIncome(startOfMonth: Long, endOfMonth: Long): Flow<Double?>

    @Query("""
    SELECT AVG(daily_total) FROM (
        SELECT date(date/1000, 'unixepoch') as day, SUM(amount) as daily_total
        FROM transactions 
        WHERE type = 'Expense'
        AND date >= :startOfMonth
        GROUP BY day
    )
""")
    fun getAverageDailyExpense(startOfMonth: Long): Flow<Double?>

    // Category breakdown for pie chart
    @Query("""
    SELECT category, SUM(amount) as total 
    FROM transactions 
    WHERE type = 'Expense' 
    AND date >= :startOfMonth 
    AND date <= :endOfMonth
    GROUP BY category 
    ORDER BY total DESC
""")
    fun getCategoryBreakdown(startOfMonth: Long, endOfMonth: Long): Flow<List<CategoryTotal>>
}

data class CategoryTotal(
    val category: String,
    val total: Double
)