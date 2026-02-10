//StatisticsScreen.kt
package com.example.financetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(dao: TransactionDao) {
    val screenSize = rememberPhoneScreenSize()
    val padding = getResponsivePadding()
    val spacing = getResponsiveSpacing()
    val windowSize = rememberWindowSize()
    val scope = rememberCoroutineScope()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    // Get current month range
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val startOfMonth = calendar.timeInMillis

    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    val endOfMonth = calendar.timeInMillis

    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysRemaining = daysInMonth - currentDay

    // Data from database
    val budgetSettings by dao.getBudgetSettings().collectAsState(initial = null)
    val monthlyExpenses by dao.getMonthlyExpenses(startOfMonth, endOfMonth)
        .collectAsState(initial = 0.0)
    val monthlyIncome by dao.getMonthlyIncome(startOfMonth, endOfMonth)
        .collectAsState(initial = 0.0)
    val avgDailyExpense by dao.getAverageDailyExpense(startOfMonth).collectAsState(initial = 0.0)
    val categoryBreakdown by dao.getCategoryBreakdown(startOfMonth, endOfMonth)
        .collectAsState(initial = emptyList())

    var showBudgetDialog by remember { mutableStateOf(false) }

    // Calculations
    val budget = budgetSettings?.monthlyBudget ?: 0.0
    val expenses = monthlyExpenses ?: 0.0
    val income = monthlyIncome ?: 0.0
    val dailyAvg = avgDailyExpense ?: 0.0

    val budgetUsedPercent = if (budget > 0) (expenses / budget * 100).roundToInt() else 0
    val projectedMonthlyExpense = dailyAvg * daysInMonth
    val projectedOverBudget = if (budget > 0) projectedMonthlyExpense - budget else 0.0
    val remainingBudget = budget - expenses
    val dailyBudgetRemaining = if (daysRemaining > 0) remainingBudget / daysRemaining else 0.0

    // Status color
    val statusColor = when {
        budgetUsedPercent >= 100 -> MaterialTheme.colorScheme.error
        budgetUsedPercent >= 90 -> Color(0xFFFF6B00) // Orange
        budgetUsedPercent >= 70 -> Color(0xFFFFB800) // Yellow
        else -> MaterialTheme.colorScheme.primary
    }
    val horizontalPadding = when (windowSize.width) {
        WindowSizeClass.Compact -> 16.dp
        WindowSizeClass.Medium -> 32.dp
        WindowSizeClass.Expanded -> 48.dp
    }

    val maxContentWidth = when (windowSize.width) {
        WindowSizeClass.Compact -> Dp.Infinity
        WindowSizeClass.Medium -> 800.dp
        WindowSizeClass.Expanded -> 1000.dp
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(spacing),
                    contentPadding = PaddingValues(bottom = spacing)
        ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showBudgetDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Budget")
                }
            }
        }

        // Budget Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = statusColor.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Monthly Budget",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = currencyFormat.format(budget),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = (budgetUsedPercent / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Spent: ${currencyFormat.format(expenses)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$budgetUsedPercent% used",
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (budget > 0) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Remaining",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = currencyFormat.format(remainingBudget.coerceAtLeast(0.0)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBudget > 0)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Warning Card
        if (budget > 0 && projectedOverBudget > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Budget Warning!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "At your current rate of ${currencyFormat.format(dailyAvg)}/day, you'll spend ${
                                    currencyFormat.format(
                                        projectedMonthlyExpense
                                    )
                                } this month (${currencyFormat.format(projectedOverBudget)} over budget).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Daily Insights
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InsightRow(
                        label = "Average per day",
                        value = currencyFormat.format(dailyAvg),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (budget > 0) {
                        InsightRow(
                            label = "Budget per day (remaining)",
                            value = currencyFormat.format(dailyBudgetRemaining),
                            color = if (dailyAvg > dailyBudgetRemaining)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )

                        InsightRow(
                            label = "Projected monthly total",
                            value = currencyFormat.format(projectedMonthlyExpense),
                            color = if (projectedMonthlyExpense > budget)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }

                    InsightRow(
                        label = "Days remaining in month",
                        value = "$daysRemaining days",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Income vs Expense
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Income vs Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBox(
                            label = "Income",
                            value = currencyFormat.format(income),
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatBox(
                            label = "Expenses",
                            value = currencyFormat.format(expenses),
                            color = MaterialTheme.colorScheme.error
                        )
                        StatBox(
                            label = "Balance",
                            value = currencyFormat.format(income - expenses),
                            color = if (income - expenses >= 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Category Breakdown
        if (categoryBreakdown.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Spending by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        categoryBreakdown.forEach { category ->
                            val percentage =
                                if (expenses > 0) (category.total / expenses * 100).roundToInt() else 0

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = category.category,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${currencyFormat.format(category.total)} ($percentage%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = percentage / 100f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

    // Budget Edit Dialog
    if (showBudgetDialog) {
        BudgetEditDialog(
            currentBudget = budget,
            onDismiss = { showBudgetDialog = false },
            onSave = { newBudget ->
                scope.launch {
                    dao.insertBudgetSettings(BudgetSettings(monthlyBudget = newBudget))
                    showBudgetDialog = false
                }
            }
        )
    }
}

@Composable
fun InsightRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budget by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Monthly Budget") },
        text = {
            Column {
                Text(
                    text = "Set your spending limit for the month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = budget,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            budget = it
                        }
                    },
                    label = { Text("Monthly Budget") },
                    placeholder = { Text("e.g., 10000") },
                    leadingIcon = { Text("₱") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val budgetValue = budget.toDoubleOrNull()
                    if (budgetValue != null && budgetValue > 0) {
                        onSave(budgetValue)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}