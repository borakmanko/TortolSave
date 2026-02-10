//HomeScreen.kt
package com.example.financetracker


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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

    val transactions by dao.getTransactionsPaginated(
        limit = pageSize,
        offset = currentPage * pageSize
    ).collectAsState(initial = emptyList())

    val totalCount by dao.getTransactionCount().collectAsState(initial = 0)
    val totalIncome by dao.getTotalIncome().collectAsState(initial = 0.0)
    val totalExpenses by dao.getTotalExpenses().collectAsState(initial = 0.0)

    val balance = (totalIncome ?: 0.0) - (totalExpenses ?: 0.0)
    val totalPages = (totalCount + pageSize - 1) / pageSize

    var showEditDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    val scope = rememberCoroutineScope()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Balance",
                    style = when (screenSize) {
                        PhoneScreenSize.Small -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.titleMedium
                    }
                )
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
        }

        Spacer(modifier = Modifier.height(spacing))

        // Recent Transactions Header
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
            Text(
                text = "Page ${currentPage + 1} of ${maxOf(totalPages, 1)}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(spacing))

        // Transaction List
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