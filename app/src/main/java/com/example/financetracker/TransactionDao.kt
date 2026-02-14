// TransactionDao.kt
package com.example.financetracker

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // ========== TRANSACTIONS ==========
    @Insert suspend fun insert(transaction: Transaction)

    @Update suspend fun update(transaction: Transaction)

    @Delete suspend fun delete(transaction: Transaction)

    // Get transactions with pagination (limit 10 per page)
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
    fun getTransactionsPaginated(limit: Int, offset: Int): Flow<List<Transaction>>

    // Get total count for pagination
    @Query("SELECT COUNT(*) FROM transactions") fun getTransactionCount(): Flow<Int>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Income'")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Expense'")
    fun getTotalExpenses(): Flow<Double?>

    @Query(
            "SELECT category, SUM(amount) as total FROM transactions WHERE type = 'Expense' GROUP BY category"
    )
    fun getExpensesByCategory(): Flow<List<CategoryTotal>>

    // Date range filtering queries
    @Query(
            """
        SELECT * FROM transactions 
        WHERE date >= :startDate AND date <= :endDate 
        ORDER BY date DESC 
        LIMIT :limit OFFSET :offset
    """
    )
    fun getTransactionsInDateRange(
            startDate: Long,
            endDate: Long,
            limit: Int,
            offset: Int
    ): Flow<List<Transaction>>

    @Query(
            """
        SELECT COUNT(*) FROM transactions 
        WHERE date >= :startDate AND date <= :endDate
    """
    )
    fun getTransactionCountInDateRange(startDate: Long, endDate: Long): Flow<Int>

    @Query(
            """
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'Income' AND date >= :startDate AND date <= :endDate
    """
    )
    fun getTotalIncomeInDateRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query(
            """
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'Expense' AND date >= :startDate AND date <= :endDate
    """
    )
    fun getTotalExpensesInDateRange(startDate: Long, endDate: Long): Flow<Double?>

    // NEW: Get transactions for a specific bank
    @Query("SELECT * FROM transactions WHERE bankId = :bankId ORDER BY date DESC")
    fun getTransactionsByBank(bankId: Int): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions WHERE bankId = :bankId")
    fun getTransactionCountByBank(bankId: Int): Flow<Int>

    // ========== TEMPLATES ==========
    @Insert suspend fun insertTemplate(template: TransactionTemplate)

    @Update suspend fun updateTemplate(template: TransactionTemplate)

    @Delete suspend fun deleteTemplate(template: TransactionTemplate)

    @Query("SELECT * FROM templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<TransactionTemplate>>

    // ========== BUDGET SETTINGS ==========
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetSettings(settings: BudgetSettings)

    @Query("SELECT * FROM budget_settings WHERE id = 1")
    fun getBudgetSettings(): Flow<BudgetSettings?>

    // ========== STATISTICS QUERIES ==========
    @Query(
            """
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'Expense' 
        AND date >= :startOfMonth 
        AND date <= :endOfMonth
    """
    )
    fun getMonthlyExpenses(startOfMonth: Long, endOfMonth: Long): Flow<Double?>

    @Query(
            """
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'Income' 
        AND date >= :startOfMonth 
        AND date <= :endOfMonth
    """
    )
    fun getMonthlyIncome(startOfMonth: Long, endOfMonth: Long): Flow<Double?>

    @Query(
            """
        SELECT AVG(daily_total) FROM (
            SELECT date(date/1000, 'unixepoch') as day, SUM(amount) as daily_total
            FROM transactions 
            WHERE type = 'Expense'
            AND date >= :startOfMonth
            GROUP BY day
        )
    """
    )
    fun getAverageDailyExpense(startOfMonth: Long): Flow<Double?>

    @Query(
            """
        SELECT category, SUM(amount) as total 
        FROM transactions 
        WHERE type = 'Expense' 
        AND date >= :startOfMonth 
        AND date <= :endOfMonth
        GROUP BY category 
        ORDER BY total DESC
    """
    )
    fun getCategoryBreakdown(startOfMonth: Long, endOfMonth: Long): Flow<List<CategoryTotal>>

    // Daily trend data (for current month)
    @Query(
            """
        SELECT 
            strftime('%d', datetime(date/1000, 'unixepoch', 'localtime')) as day,
            COALESCE(SUM(CASE WHEN type = 'Income' THEN amount ELSE 0 END), 0) as income,
            COALESCE(SUM(CASE WHEN type = 'Expense' THEN amount ELSE 0 END), 0) as expense
        FROM transactions
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY day
        ORDER BY day ASC
    """
    )
    fun getDailyTrends(startDate: Long, endDate: Long): Flow<List<DailyTrend>>

    // Monthly trend data (all 12 months of current year)
    @Query(
            """
        SELECT 
            strftime('%m', datetime(date/1000, 'unixepoch', 'localtime')) as month,
            strftime('%Y', datetime(date/1000, 'unixepoch', 'localtime')) as year,
            COALESCE(SUM(CASE WHEN type = 'Income' THEN amount ELSE 0 END), 0) as income,
            COALESCE(SUM(CASE WHEN type = 'Expense' THEN amount ELSE 0 END), 0) as expense
        FROM transactions
        WHERE strftime('%Y', datetime(date/1000, 'unixepoch', 'localtime')) = strftime('%Y', 'now', 'localtime')
        GROUP BY year, month
        ORDER BY month ASC
    """
    )
    fun getMonthlyTrends(): Flow<List<MonthlyTrend>>

    // ========== BANKS ==========
    @Insert suspend fun insertBank(bank: Bank): Long

    @Update suspend fun updateBank(bank: Bank)

    @Delete suspend fun deleteBank(bank: Bank)

    @Query("SELECT * FROM banks ORDER BY name ASC") fun getAllBanks(): Flow<List<Bank>>

    @Query("SELECT * FROM banks WHERE id = :bankId") fun getBankById(bankId: Int): Flow<Bank?>

    @Query("SELECT COUNT(*) FROM banks") fun getBankCount(): Flow<Int>

    // Get bank balance (income - expenses + transfers in - transfers out)
    @Query(
            """
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'Income' THEN amount ELSE 0 END), 0) - 
            COALESCE(SUM(CASE WHEN type = 'Expense' THEN amount ELSE 0 END), 0) +
            COALESCE((SELECT SUM(amount) FROM transfers WHERE toBankId = :bankId), 0) -
            COALESCE((SELECT SUM(amount) FROM transfers WHERE fromBankId = :bankId), 0)
        FROM transactions 
        WHERE bankId = :bankId
    """
    )
    fun getBankBalance(bankId: Int): Flow<Double?>

    // ========== TRANSFERS ==========
    @Insert suspend fun insertTransfer(transfer: Transfer)

    @Query("SELECT * FROM transfers ORDER BY date DESC") fun getAllTransfers(): Flow<List<Transfer>>

    @Query(
            """
        SELECT * FROM transfers 
        WHERE fromBankId = :bankId OR toBankId = :bankId 
        ORDER BY date DESC
    """
    )
    fun getTransfersByBank(bankId: Int): Flow<List<Transfer>>

    // ========== EXPORT QUERIES (suspend, one-shot) ==========
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsList(): List<Transaction>

    @Query("SELECT * FROM templates ORDER BY name ASC")
    suspend fun getAllTemplatesList(): List<TransactionTemplate>

    @Query("SELECT * FROM banks ORDER BY name ASC") suspend fun getAllBanksList(): List<Bank>

    @Query("SELECT * FROM transfers ORDER BY date DESC")
    suspend fun getAllTransfersList(): List<Transfer>

    @Query("SELECT * FROM budget_settings WHERE id = 1")
    suspend fun getBudgetSettingsDirect(): BudgetSettings?

    // ========== IMPORT QUERIES (bulk delete + insert) ==========
    @Query("DELETE FROM transactions") suspend fun deleteAllTransactions()

    @Query("DELETE FROM templates") suspend fun deleteAllTemplates()

    @Query("DELETE FROM banks") suspend fun deleteAllBanks()

    @Query("DELETE FROM transfers") suspend fun deleteAllTransfers()

    @Query("DELETE FROM budget_settings") suspend fun deleteAllBudgetSettings()

    @Insert suspend fun insertAllTransactions(transactions: List<Transaction>)

    @Insert suspend fun insertAllTemplates(templates: List<TransactionTemplate>)

    @Insert suspend fun insertAllBanks(banks: List<Bank>)

    @Insert suspend fun insertAllTransfers(transfers: List<Transfer>)
}

data class CategoryTotal(val category: String, val total: Double)

data class DailyTrend(val day: String, val income: Double, val expense: Double)

data class MonthlyTrend(
        val month: String,
        val year: String,
        val income: Double,
        val expense: Double
)

// NEW: Data class for bank with balance
data class BankWithBalance(val bank: Bank, val balance: Double, val transactionCount: Int)
