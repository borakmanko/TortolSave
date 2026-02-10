//HomeScreen.kt
package com.example.financetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(dao: TransactionDao) {
    val screenSize = rememberPhoneScreenSize()
    val padding = getResponsivePadding()
    val spacing = getResponsiveSpacing()

    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 10

    // Date filtering states
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedStartDate by remember { mutableStateOf<Long?>(null) }
    var selectedEndDate by remember { mutableStateOf<Long?>(null) }
    var dateFilterMode by remember { mutableStateOf<DateFilterMode>(DateFilterMode.ALL) }

    // Balance visibility state (default hidden)
    var isBalanceVisible by remember { mutableStateOf(false) }

    // Get filtered transactions based on date range
    val transactions by if (selectedStartDate != null && selectedEndDate != null) {
        dao.getTransactionsInDateRange(
            startDate = selectedStartDate!!,
            endDate = selectedEndDate!!,
            limit = pageSize,
            offset = currentPage * pageSize
        ).collectAsState(initial = emptyList())
    } else {
        dao.getTransactionsPaginated(
            limit = pageSize,
            offset = currentPage * pageSize
        ).collectAsState(initial = emptyList())
    }

    val totalCount by if (selectedStartDate != null && selectedEndDate != null) {
        dao.getTransactionCountInDateRange(selectedStartDate!!, selectedEndDate!!)
            .collectAsState(initial = 0)
    } else {
        dao.getTransactionCount().collectAsState(initial = 0)
    }

    // Get totals based on filter
    val totalIncome by if (selectedStartDate != null && selectedEndDate != null) {
        dao.getTotalIncomeInDateRange(selectedStartDate!!, selectedEndDate!!)
            .collectAsState(initial = 0.0)
    } else {
        dao.getTotalIncome().collectAsState(initial = 0.0)
    }

    val totalExpenses by if (selectedStartDate != null && selectedEndDate != null) {
        dao.getTotalExpensesInDateRange(selectedStartDate!!, selectedEndDate!!)
            .collectAsState(initial = 0.0)
    } else {
        dao.getTotalExpenses().collectAsState(initial = 0.0)
    }

    val balance = (totalIncome ?: 0.0) - (totalExpenses ?: 0.0)
    val totalPages = (totalCount + pageSize - 1) / pageSize

    var showEditDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    val scope = rememberCoroutineScope()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Balance Card with visibility toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(padding)
            ) {
                // Header row with title and eye icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Balance",
                        style = when (screenSize) {
                            PhoneScreenSize.Small -> MaterialTheme.typography.titleSmall
                            else -> MaterialTheme.typography.titleMedium
                        }
                    )
                    IconButton(onClick = { isBalanceVisible = !isBalanceVisible }) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isBalanceVisible) "Hide balance" else "Show balance",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (isBalanceVisible) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currencyFormat.format(balance),
                            style = when (screenSize) {
                                PhoneScreenSize.Small -> MaterialTheme.typography.headlineMedium
                                PhoneScreenSize.Regular -> MaterialTheme.typography.headlineLarge
                                PhoneScreenSize.Large -> MaterialTheme.typography.displaySmall
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(spacing))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Income",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    currencyFormat.format(totalIncome ?: 0.0),
                                    style = when (screenSize) {
                                        PhoneScreenSize.Small -> MaterialTheme.typography.bodyMedium
                                        else -> MaterialTheme.typography.bodyLarge
                                    },
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Expenses",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    currencyFormat.format(totalExpenses ?: 0.0),
                                    style = when (screenSize) {
                                        PhoneScreenSize.Small -> MaterialTheme.typography.bodyMedium
                                        else -> MaterialTheme.typography.bodyLarge
                                    },
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    // Show asterisks when hidden
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "••••••",
                            style = when (screenSize) {
                                PhoneScreenSize.Small -> MaterialTheme.typography.headlineMedium
                                PhoneScreenSize.Regular -> MaterialTheme.typography.headlineLarge
                                PhoneScreenSize.Large -> MaterialTheme.typography.displaySmall
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing))

        // Date filter info card (if active)
        if (selectedStartDate != null && selectedEndDate != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Filtered by date:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${dateFormat.format(Date(selectedStartDate!!))} - ${dateFormat.format(Date(selectedEndDate!!))}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    IconButton(
                        onClick = {
                            selectedStartDate = null
                            selectedEndDate = null
                            dateFilterMode = DateFilterMode.ALL
                            currentPage = 0
                        }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear filter",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing))
        }

        // Recent Transactions Header with Date Filter Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                style = when (screenSize) {
                    PhoneScreenSize.Small -> MaterialTheme.typography.titleMedium
                    else -> MaterialTheme.typography.titleLarge
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (totalPages > 1) {
                    Text(
                        text = "Page ${currentPage + 1}/${totalPages}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Filter by date",
                        tint = if (selectedStartDate != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing))

        // Transaction List
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedStartDate != null)
                        "No transactions in this date range"
                    else
                        "No transactions yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onEdit = {
                            editingTransaction = transaction
                            showEditDialog = true
                        },
                        onDelete = {
                            scope.launch {
                                dao.delete(transaction)
                            }
                        }
                    )
                }
            }
        }

        // Pagination Controls
        if (totalPages > 1) {
            Spacer(modifier = Modifier.height(spacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { if (currentPage > 0) currentPage-- },
                    enabled = currentPage > 0,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Text("Previous")
                }
                Button(
                    onClick = { if (currentPage < totalPages - 1) currentPage++ },
                    enabled = currentPage < totalPages - 1,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Text("Next")
                }
            }
        }
    }

    // Date Filter Dialog
    if (showDatePicker) {
        DateRangePickerDialog(
            currentMode = dateFilterMode,
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { mode, start, end ->
                dateFilterMode = mode
                selectedStartDate = start
                selectedEndDate = end
                currentPage = 0
                showDatePicker = false
            }
        )
    }

    if (showEditDialog && editingTransaction != null) {
        EditTransactionDialog(
            transaction = editingTransaction!!,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                scope.launch {
                    dao.update(updated)
                    showEditDialog = false
                }
            }
        )
    }
}

enum class DateFilterMode {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    currentMode: DateFilterMode,
    onDismiss: () -> Unit,
    onDateRangeSelected: (DateFilterMode, Long, Long) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Date") },
        text = {
            Column {
                // Quick filter options
                DateFilterOption(
                    label = "All Transactions",
                    isSelected = selectedMode == DateFilterMode.ALL,
                    onClick = { selectedMode = DateFilterMode.ALL }
                )

                DateFilterOption(
                    label = "Today",
                    isSelected = selectedMode == DateFilterMode.TODAY,
                    onClick = { selectedMode = DateFilterMode.TODAY }
                )

                DateFilterOption(
                    label = "This Week",
                    isSelected = selectedMode == DateFilterMode.THIS_WEEK,
                    onClick = { selectedMode = DateFilterMode.THIS_WEEK }
                )

                DateFilterOption(
                    label = "This Month",
                    isSelected = selectedMode == DateFilterMode.THIS_MONTH,
                    onClick = { selectedMode = DateFilterMode.THIS_MONTH }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Custom date range
                DateFilterOption(
                    label = "Custom Range",
                    isSelected = selectedMode == DateFilterMode.CUSTOM,
                    onClick = { selectedMode = DateFilterMode.CUSTOM }
                )

                if (selectedMode == DateFilterMode.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = customStartDate?.let {
                                "From: ${dateFormat.format(Date(it))}"
                            } ?: "Select Start Date"
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = customEndDate?.let {
                                "To: ${dateFormat.format(Date(it))}"
                            } ?: "Select End Date"
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedMode) {
                        DateFilterMode.ALL -> onDismiss()
                        DateFilterMode.TODAY -> {
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            val start = cal.timeInMillis

                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            val end = cal.timeInMillis

                            onDateRangeSelected(DateFilterMode.TODAY, start, end)
                        }
                        DateFilterMode.THIS_WEEK -> {
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            val start = cal.timeInMillis

                            cal.add(Calendar.DAY_OF_WEEK, 6)
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            val end = cal.timeInMillis

                            onDateRangeSelected(DateFilterMode.THIS_WEEK, start, end)
                        }
                        DateFilterMode.THIS_MONTH -> {
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.DAY_OF_MONTH, 1)
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            val start = cal.timeInMillis

                            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                            cal.set(Calendar.HOUR_OF_DAY, 23)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            val end = cal.timeInMillis

                            onDateRangeSelected(DateFilterMode.THIS_MONTH, start, end)
                        }
                        DateFilterMode.CUSTOM -> {
                            if (customStartDate != null && customEndDate != null) {
                                onDateRangeSelected(DateFilterMode.CUSTOM, customStartDate!!, customEndDate!!)
                            }
                        }
                    }
                },
                enabled = when (selectedMode) {
                    DateFilterMode.CUSTOM -> customStartDate != null && customEndDate != null
                    else -> true
                }
            ) {
                Text(if (selectedMode == DateFilterMode.ALL) "Clear Filter" else "Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Start Date Picker
    if (showStartPicker) {
        SingleDatePickerDialog(
            selectedDate = customStartDate ?: System.currentTimeMillis(),
            onDateSelected = {
                customStartDate = it
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    // End Date Picker
    if (showEndPicker) {
        SingleDatePickerDialog(
            selectedDate = customEndDate ?: System.currentTimeMillis(),
            onDateSelected = {
                customEndDate = it
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@Composable
fun DateFilterOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleDatePickerDialog(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCard(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val screenSize = rememberPhoneScreenSize()
    val padding = getResponsivePadding()

    val dateFormat = SimpleDateFormat(
        when (screenSize) {
            PhoneScreenSize.Small -> "MMM dd, yy"
            else -> "MMM dd, yyyy hh:mm a"
        },
        Locale.getDefault()
    )
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    val titleStyle = when (screenSize) {
        PhoneScreenSize.Small -> MaterialTheme.typography.bodyMedium
        else -> MaterialTheme.typography.bodyLarge
    }

    val amountStyle = when (screenSize) {
        PhoneScreenSize.Small -> MaterialTheme.typography.bodyLarge
        else -> MaterialTheme.typography.titleMedium
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = titleStyle,
                    fontWeight = FontWeight.Medium,
                    maxLines = when (screenSize) {
                        PhoneScreenSize.Small -> 1
                        else -> 2
                    }
                )
                Text(
                    text = "${transaction.category} • ${transaction.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(Date(transaction.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormat.format(transaction.amount),
                    style = amountStyle,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == "Income")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(
                            when (screenSize) {
                                PhoneScreenSize.Small -> 40.dp
                                else -> 48.dp
                            }
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(
                                when (screenSize) {
                                    PhoneScreenSize.Small -> 20.dp
                                    else -> 24.dp
                                }
                            )
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(
                            when (screenSize) {
                                PhoneScreenSize.Small -> 40.dp
                                else -> 48.dp
                            }
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(
                                when (screenSize) {
                                    PhoneScreenSize.Small -> 20.dp
                                    else -> 24.dp
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var type by remember { mutableStateOf(transaction.type) }
    var category by remember { mutableStateOf(transaction.category) }
    var description by remember { mutableStateOf(transaction.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == "Expense",
                        onClick = { type = "Expense" },
                        label = { Text("Expense") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = type == "Income",
                        onClick = { type = "Income" },
                        label = { Text("Income") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null) {
                        onSave(
                            transaction.copy(
                                amount = amountValue,
                                type = type,
                                category = category,
                                description = description
                            )
                        )
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