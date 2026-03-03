// StatisticsScreen.kt
package com.example.financetracker

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// Helper functions for converting trend data

// Helper: Convert all-time daily trend data into data points with month markers
fun List<AllTimeDailyTrend>.toAllTimeTrendDataPoints(): List<TrendDataPoint> {
    if (this.isEmpty()) return emptyList()

    var lastMonth = ""
    return this.map { trend ->
        val monthNames =
                arrayOf(
                        "",
                        "Jan",
                        "Feb",
                        "Mar",
                        "Apr",
                        "May",
                        "Jun",
                        "Jul",
                        "Aug",
                        "Sep",
                        "Oct",
                        "Nov",
                        "Dec"
                )
        val monthNum = trend.month.toIntOrNull() ?: 0
        val currentMonth = "${monthNames.getOrElse(monthNum) { "" }} ${trend.year.takeLast(2)}"
        val isNewMonth = currentMonth != lastMonth
        lastMonth = currentMonth
        TrendDataPoint(
                label = trend.fullDate,
                income = trend.income,
                expense = trend.expense,
                monthLabel = if (isNewMonth) monthNames.getOrElse(monthNum) { "" } else null
        )
    }
}

fun List<MonthlyTrend>.toMonthlyTrendDataPoints(): List<TrendDataPoint> {
    val monthNames =
            arrayOf(
                    "Jan",
                    "Feb",
                    "Mar",
                    "Apr",
                    "May",
                    "Jun",
                    "Jul",
                    "Aug",
                    "Sep",
                    "Oct",
                    "Nov",
                    "Dec"
            )

    // Create a map of existing data
    val dataMap = this.associateBy { it.month.toIntOrNull() ?: 0 }

    // Create data points for all 12 months
    return (1..12).map { monthNum ->
        val trend = dataMap[monthNum]
        TrendDataPoint(
                label = monthNames[monthNum - 1],
                income = trend?.income ?: 0.0,
                expense = trend?.expense ?: 0.0
        )
    }
}

enum class TrendViewMode {
    DAILY,
    MONTHLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(dao: TransactionDao) {
    val screenSize = rememberPhoneScreenSize()
    val padding = getResponsivePadding()
    val spacing = getResponsiveSpacing()
    val windowSize = rememberWindowSize()
    val scope = rememberCoroutineScope()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    // Trend view mode state
    var trendViewMode by remember { mutableStateOf(TrendViewMode.MONTHLY) }

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
    val monthlyExpenses by
            dao.getMonthlyExpenses(startOfMonth, endOfMonth).collectAsState(initial = 0.0)
    val monthlyIncome by
            dao.getMonthlyIncome(startOfMonth, endOfMonth).collectAsState(initial = 0.0)
    val avgDailyExpense by dao.getAverageDailyExpense(startOfMonth).collectAsState(initial = 0.0)

    // All-time data for Income vs Expenses and Category Breakdown
    val allTimeIncome by dao.getTotalIncome().collectAsState(initial = 0.0)
    val allTimeExpenses by dao.getTotalExpenses().collectAsState(initial = 0.0)
    val allTimeCategoryBreakdown by
            dao.getAllCategoryBreakdown().collectAsState(initial = emptyList())

    // Trend data - all-time daily trends
    val allTimeDailyTrendRaw by dao.getAllDailyTrends().collectAsState(initial = emptyList())
    val dailyTrendData = allTimeDailyTrendRaw.toAllTimeTrendDataPoints()

    val monthlyTrendDataRaw by dao.getMonthlyTrends().collectAsState(initial = emptyList())
    val monthlyTrendData = monthlyTrendDataRaw.toMonthlyTrendDataPoints()

    var showBudgetDialog by remember { mutableStateOf(false) }

    // Month filter state for Income vs Expenses and Spending by Category
    // null means "All Time", otherwise it's the month (0-11) and year
    val currentCalendar = Calendar.getInstance()
    var filterMonth by remember { mutableStateOf<Int?>(null) } // null = All Time
    var filterYear by remember { mutableStateOf(currentCalendar.get(Calendar.YEAR)) }
    val monthNames =
            arrayOf(
                    "Jan",
                    "Feb",
                    "Mar",
                    "Apr",
                    "May",
                    "Jun",
                    "Jul",
                    "Aug",
                    "Sep",
                    "Oct",
                    "Nov",
                    "Dec"
            )

    // Compute filter date range when a month is selected
    val filterStartDate =
            remember(filterMonth, filterYear) {
                if (filterMonth == null) 0L
                else {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.YEAR, filterYear)
                    cal.set(Calendar.MONTH, filterMonth!!)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
            }
    val filterEndDate =
            remember(filterMonth, filterYear) {
                if (filterMonth == null) 0L
                else {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.YEAR, filterYear)
                    cal.set(Calendar.MONTH, filterMonth!!)
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    cal.timeInMillis
                }
            }

    // Filtered data (only queried when a month is selected)
    val filteredIncome by
            dao.getMonthlyIncome(filterStartDate, filterEndDate).collectAsState(initial = 0.0)
    val filteredExpenses by
            dao.getMonthlyExpenses(filterStartDate, filterEndDate).collectAsState(initial = 0.0)
    val filteredCategoryBreakdown by
            dao.getMonthCategoryBreakdown(filterStartDate, filterEndDate)
                    .collectAsState(initial = emptyList())

    // Calculations
    val budget = budgetSettings?.monthlyBudget ?: 0.0
    val expenses = monthlyExpenses ?: 0.0
    val income = monthlyIncome ?: 0.0
    val dailyAvg = avgDailyExpense ?: 0.0
    val totalIncome = allTimeIncome ?: 0.0
    val totalExpenses = allTimeExpenses ?: 0.0

    // Display values: use filtered when a month is selected, all-time otherwise
    val displayIncome = if (filterMonth != null) (filteredIncome ?: 0.0) else totalIncome
    val displayExpenses = if (filterMonth != null) (filteredExpenses ?: 0.0) else totalExpenses
    val displayCategoryBreakdown =
            if (filterMonth != null) filteredCategoryBreakdown else allTimeCategoryBreakdown
    val filterLabel =
            if (filterMonth != null) "${monthNames[filterMonth!!]} $filterYear" else "All Time"

    val budgetUsedPercent = if (budget > 0) (expenses / budget * 100).roundToInt() else 0
    val projectedMonthlyExpense = dailyAvg * daysInMonth
    val projectedOverBudget = if (budget > 0) projectedMonthlyExpense - budget else 0.0
    val remainingBudget = budget - expenses
    val dailyBudgetRemaining = if (daysRemaining > 0) remainingBudget / daysRemaining else 0.0

    // Status color
    val statusColor =
            when {
                budgetUsedPercent >= 100 -> MaterialTheme.colorScheme.error
                budgetUsedPercent >= 90 -> Color(0xFFFF6B00) // Orange
                budgetUsedPercent >= 70 -> Color(0xFFFFB800) // Yellow
                else -> MaterialTheme.colorScheme.primary
            }

    val horizontalPadding =
            when (windowSize.width) {
                WindowSizeClass.Compact -> 16.dp
                WindowSizeClass.Medium -> 32.dp
                WindowSizeClass.Expanded -> 48.dp
            }

    val maxContentWidth =
            when (windowSize.width) {
                WindowSizeClass.Compact -> Dp.Infinity
                WindowSizeClass.Medium -> 800.dp
                WindowSizeClass.Expanded -> 1000.dp
            }

    Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
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
                        Text(text = "Statistics", style = MaterialTheme.typography.headlineMedium)
                        Text(
                                text =
                                        SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                                                .format(Date()),
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
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = statusColor.copy(alpha = 0.1f)
                                )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                modifier = Modifier.fillMaxWidth().height(12.dp),
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
                                        text =
                                                currencyFormat.format(
                                                        remainingBudget.coerceAtLeast(0.0)
                                                ),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                                if (remainingBudget > 0)
                                                        MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.error
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
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor =
                                                    MaterialTheme.colorScheme.errorContainer
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
                                        text =
                                                "At your current rate of ${currencyFormat.format(dailyAvg)}/day, you'll spend ${
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

            // Trend Graph Card - NEW
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    text = "Trend Analysis",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                            )

                            // Toggle buttons for day/month view
                            Row {
                                FilterChip(
                                        selected = trendViewMode == TrendViewMode.DAILY,
                                        onClick = { trendViewMode = TrendViewMode.DAILY },
                                        label = { Text("Daily") },
                                        modifier = Modifier.padding(end = 4.dp)
                                )
                                FilterChip(
                                        selected = trendViewMode == TrendViewMode.MONTHLY,
                                        onClick = { trendViewMode = TrendViewMode.MONTHLY },
                                        label = { Text("Monthly") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Graph
                        when (trendViewMode) {
                            TrendViewMode.DAILY -> {
                                if (dailyTrendData.isNotEmpty()) {
                                    TrendLineGraph(
                                            data = dailyTrendData,
                                            modifier = Modifier.fillMaxWidth().height(220.dp),
                                            currencyFormat = currencyFormat,
                                            isMonthly = false
                                    )
                                } else {
                                    Box(
                                            modifier = Modifier.fillMaxWidth().height(220.dp),
                                            contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                                text = "No daily data available",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            TrendViewMode.MONTHLY -> {
                                if (monthlyTrendData.isNotEmpty()) {
                                    TrendLineGraph(
                                            data = monthlyTrendData,
                                            modifier = Modifier.fillMaxWidth().height(220.dp),
                                            currencyFormat = currencyFormat,
                                            isMonthly = true
                                    )
                                } else {
                                    Box(
                                            modifier = Modifier.fillMaxWidth().height(220.dp),
                                            contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                                text = "No monthly data available",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Legend
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendItem(color = Color(0xFF4CAF50), label = "Income")
                            Spacer(modifier = Modifier.width(16.dp))
                            LegendItem(color = Color(0xFFF44336), label = "Expenses")
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
                                    color =
                                            if (dailyAvg > dailyBudgetRemaining)
                                                    MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                            )

                            InsightRow(
                                    label = "Projected monthly total",
                                    value = currencyFormat.format(projectedMonthlyExpense),
                                    color =
                                            if (projectedMonthlyExpense > budget)
                                                    MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
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
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    text = "Income vs Expenses",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                            )
                            // Month filter chip
                            FilterChip(
                                    selected = filterMonth != null,
                                    onClick = {
                                        if (filterMonth != null) {
                                            filterMonth = null
                                        } else {
                                            filterMonth = currentCalendar.get(Calendar.MONTH)
                                            filterYear = currentCalendar.get(Calendar.YEAR)
                                        }
                                    },
                                    label = {
                                        Text(
                                                filterLabel,
                                                style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                            )
                        }

                        // Month navigation (only shown when a month is selected)
                        if (filterMonth != null) {
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                        onClick = {
                                            if (filterMonth == 0) {
                                                filterMonth = 11
                                                filterYear = filterYear - 1
                                            } else {
                                                filterMonth = filterMonth!! - 1
                                            }
                                        }
                                ) { Text("◀", style = MaterialTheme.typography.titleMedium) }
                                Text(
                                        text = "${monthNames[filterMonth!!]} $filterYear",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                        onClick = {
                                            if (filterMonth == 11) {
                                                filterMonth = 0
                                                filterYear = filterYear + 1
                                            } else {
                                                filterMonth = filterMonth!! + 1
                                            }
                                        }
                                ) { Text("▶", style = MaterialTheme.typography.titleMedium) }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox(
                                    label = "Income",
                                    value = currencyFormat.format(displayIncome),
                                    color = MaterialTheme.colorScheme.primary
                            )
                            StatBox(
                                    label = "Expenses",
                                    value = currencyFormat.format(displayExpenses),
                                    color = MaterialTheme.colorScheme.error
                            )
                            StatBox(
                                    label = "Balance",
                                    value = currencyFormat.format(totalIncome - totalExpenses),
                                    color =
                                            if (totalIncome - totalExpenses >= 0)
                                                    MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Category Breakdown
            if (displayCategoryBreakdown.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                        text = "Spending by Category",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                )
                                Text(
                                        text = filterLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            displayCategoryBreakdown.forEach { category ->
                                val percentage =
                                        if (displayExpenses > 0)
                                                (category.total / displayExpenses * 100)
                                                        .roundToInt()
                                        else 0

                                // Title-case the category name for display
                                val displayName =
                                        category.category.lowercase().replaceFirstChar {
                                            it.titlecase()
                                        }

                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                                text = displayName,
                                                style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                                text =
                                                        "${currencyFormat.format(category.total)} ($percentage%)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LinearProgressIndicator(
                                            progress = percentage / 100f,
                                            modifier = Modifier.fillMaxWidth().height(8.dp)
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
fun TrendLineGraph(
        data: List<TrendDataPoint>,
        modifier: Modifier = Modifier,
        currencyFormat: NumberFormat,
        isMonthly: Boolean
) {
    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)
    val gridColor = Color.Gray.copy(alpha = 0.2f)

    Canvas(modifier = modifier.padding(top = 8.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        // Padding for labels
        val paddingLeft = 60f
        val paddingRight = 20f
        val paddingTop = 20f
        val paddingBottom = 40f

        val graphWidth = width - paddingLeft - paddingRight
        val graphHeight = height - paddingTop - paddingBottom

        // Find max value for scaling
        val maxIncome = data.maxOfOrNull { it.income } ?: 0.0
        val maxExpense = data.maxOfOrNull { it.expense } ?: 0.0
        val maxValue = maxOf(maxIncome, maxExpense).toFloat()

        if (maxValue == 0f) {
            // Draw "No data" message
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                        "No transactions yet",
                        width / 2,
                        height / 2,
                        Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 32f
                            textAlign = Paint.Align.CENTER
                        }
                )
            }
            return@Canvas
        }

        // Draw horizontal grid lines and Y-axis labels
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = paddingTop + (graphHeight * i / gridLines)
            val value = maxValue * (1 - i.toFloat() / gridLines)

            // Grid line
            drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1f
            )

            // Y-axis label - shortened format
            val labelText =
                    when {
                        value >= 10000 -> "₱${(value / 1000).toInt()}k"
                        value >= 1000 -> "₱${(value / 1000).roundToInt()}k"
                        else -> "₱${value.toInt()}"
                    }

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                        labelText,
                        paddingLeft - 10f,
                        y + 5f,
                        Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 20f
                            textAlign = Paint.Align.RIGHT
                        }
                )
            }
        }

        // Calculate points
        val stepX = if (data.size > 1) graphWidth / (data.size - 1) else graphWidth

        val incomePoints =
                data.mapIndexed { index, point ->
                    val x = paddingLeft + (index * stepX)
                    val normalizedValue =
                            if (maxValue > 0) point.income.toFloat() / maxValue else 0f
                    val y = paddingTop + graphHeight * (1 - normalizedValue)
                    Offset(x, y)
                }

        val expensePoints =
                data.mapIndexed { index, point ->
                    val x = paddingLeft + (index * stepX)
                    val normalizedValue =
                            if (maxValue > 0) point.expense.toFloat() / maxValue else 0f
                    val y = paddingTop + graphHeight * (1 - normalizedValue)
                    Offset(x, y)
                }

        // Draw income line
        if (incomePoints.size > 1) {
            val incomePath =
                    Path().apply {
                        moveTo(incomePoints[0].x, incomePoints[0].y)
                        for (i in 1 until incomePoints.size) {
                            lineTo(incomePoints[i].x, incomePoints[i].y)
                        }
                    }
            drawPath(
                    path = incomePath,
                    color = incomeColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }

        // Draw expense line
        if (expensePoints.size > 1) {
            val expensePath =
                    Path().apply {
                        moveTo(expensePoints[0].x, expensePoints[0].y)
                        for (i in 1 until expensePoints.size) {
                            lineTo(expensePoints[i].x, expensePoints[i].y)
                        }
                    }
            drawPath(
                    path = expensePath,
                    color = expenseColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }

        // Draw points on lines (only for monthly view or small daily datasets)
        if (isMonthly || data.size <= 30) {
            incomePoints.forEach { point ->
                drawCircle(color = incomeColor, radius = 4f, center = point)
            }

            expensePoints.forEach { point ->
                drawCircle(color = expenseColor, radius = 4f, center = point)
            }
        }

        // Draw X-axis labels
        if (isMonthly) {
            // Monthly view: show all month labels
            data.forEachIndexed { index, point ->
                val x = paddingLeft + (index * stepX)
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                            point.label,
                            x,
                            height - paddingBottom + 25f,
                            Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 18f
                                textAlign = Paint.Align.CENTER
                            }
                    )
                }
            }
        } else {
            // Daily view: show month indicators only at month boundaries
            data.forEachIndexed { index, point ->
                if (point.monthLabel != null) {
                    val x = paddingLeft + (index * stepX)
                    // Draw a subtle vertical tick at month boundary
                    drawLine(
                            color = gridColor.copy(alpha = 0.5f),
                            start = Offset(x, paddingTop),
                            end = Offset(x, height - paddingBottom + 5f),
                            strokeWidth = 1f
                    )
                    // Draw month name below the axis
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                                point.monthLabel,
                                x,
                                height - paddingBottom + 25f,
                                Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 18f
                                    textAlign = Paint.Align.CENTER
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(16.dp).padding(end = 4.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawCircle(color = color) }
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

data class TrendDataPoint(
        val label: String,
        val income: Double,
        val expense: Double,
        val monthLabel: String? = null // Non-null means this point starts a new month
)

@Composable
fun InsightRow(label: String, value: String, color: Color) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
fun BudgetEditDialog(currentBudget: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var budget by remember {
        mutableStateOf(if (currentBudget > 0) currentBudget.toString() else "")
    }

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
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
