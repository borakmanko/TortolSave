//BankDialogs.kt
package com.example.financetracker

import androidx.compose.foundation.border
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
fun TransferMoneyDialog(
    banks: List<Bank>,
    dao: TransactionDao,
    onDismiss: () -> Unit
) {
    var fromBankId by remember { mutableStateOf(0) }
    var toBankId by remember { mutableStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    // Get from bank balance
    val fromBankBalance by if (fromBankId > 0) {
        dao.getBankBalance(fromBankId).collectAsState(initial = 0.0)
    } else {
        remember { mutableStateOf(0.0) }
    }

    var showFromBankDropdown by remember { mutableStateOf(false) }
    var showToBankDropdown by remember { mutableStateOf(false) }

    val fromBank = banks.find { it.id == fromBankId }
    val toBank = banks.find { it.id == toBankId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Money") },
        text = {
            Column {
                // From Bank
                Text("From Bank:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = showFromBankDropdown,
                    onExpandedChange = { showFromBankDropdown = it }
                ) {
                    OutlinedTextField(
                        value = fromBank?.let { "${it.icon} ${it.name}" } ?: "Select bank",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFromBankDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = showFromBankDropdown,
                        onDismissRequest = { showFromBankDropdown = false }
                    ) {
                        banks.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text("${bank.icon} ${bank.name}") },
                                onClick = {
                                    fromBankId = bank.id
                                    showFromBankDropdown = false
                                }
                            )
                        }
                    }
                }

                if (fromBankId > 0) {
                    Text(
                        text = "Available: ${currencyFormat.format(fromBankBalance ?: 0.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // To Bank
                Text("To Bank:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = showToBankDropdown,
                    onExpandedChange = { showToBankDropdown = it }
                ) {
                    OutlinedTextField(
                        value = toBank?.let { "${it.icon} ${it.name}" } ?: "Select bank",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showToBankDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = showToBankDropdown,
                        onDismissRequest = { showToBankDropdown = false }
                    ) {
                        banks.filter { it.id != fromBankId }.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text("${bank.icon} ${bank.name}") },
                                onClick = {
                                    toBankId = bank.id
                                    showToBankDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amount = it
                            showError = false
                        }
                    },
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("₱") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError
                )

                if (showError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Transfer reason...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    when {
                        fromBankId == 0 || toBankId == 0 -> {
                            errorMessage = "Please select both banks"
                            showError = true
                        }
                        fromBankId == toBankId -> {
                            errorMessage = "Cannot transfer to the same bank"
                            showError = true
                        }
                        amountValue == null || amountValue <= 0 -> {
                            errorMessage = "Please enter a valid amount"
                            showError = true
                        }
                        amountValue > (fromBankBalance ?: 0.0) -> {
                            errorMessage = "Insufficient balance in source bank"
                            showError = true
                        }
                        else -> {
                            scope.launch {
                                dao.insertTransfer(
                                    Transfer(
                                        fromBankId = fromBankId,
                                        toBankId = toBankId,
                                        amount = amountValue,
                                        note = note
                                    )
                                )
                                onDismiss()
                            }
                        }
                    }
                }
            ) {
                Text("Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDetailsDialog(
    bankWithBalance: BankWithBalance,
    dao: TransactionDao,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

    val transactions by dao.getTransactionsByBank(bankWithBalance.bank.id)
        .collectAsState(initial = emptyList())
    val transfers by dao.getTransfersByBank(bankWithBalance.bank.id)
        .collectAsState(initial = emptyList())

    val allBanks by dao.getAllBanks().collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(bankWithBalance.bank.icon, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(bankWithBalance.bank.name)
                }
                Row {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Balance card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Current Balance", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = currencyFormat.format(bankWithBalance.balance),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (bankWithBalance.balance >= 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Transaction and transfer list
                if (transactions.isEmpty() && transfers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No activity yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions) { transaction ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = transaction.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = transaction.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = dateFormat.format(Date(transaction.date)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${if (transaction.type == "Income") "+" else "-"}${
                                            currencyFormat.format(transaction.amount)
                                        }",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (transaction.type == "Income")
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        items(transfers) { transfer ->
                            val isIncoming = transfer.toBankId == bankWithBalance.bank.id
                            val otherBankId = if (isIncoming) transfer.fromBankId else transfer.toBankId
                            val otherBank = allBanks.find { it.id == otherBankId }

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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (isIncoming) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isIncoming)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isIncoming) "From" else "To",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${otherBank?.icon} ${otherBank?.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (transfer.note.isNotBlank()) {
                                            Text(
                                                text = transfer.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = dateFormat.format(Date(transfer.date)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${if (isIncoming) "+" else "-"}${
                                            currencyFormat.format(transfer.amount)
                                        }",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncoming)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    // Edit Bank Dialog
    if (showEditDialog) {
        EditBankDialog(
            bank = bankWithBalance.bank,
            onDismiss = { showEditDialog = false },
            onSave = { updatedBank ->
                scope.launch {
                    dao.updateBank(updatedBank)
                    showEditDialog = false
                }
            }
        )
    }

    // Delete Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Bank?") },
            text = {
                if (bankWithBalance.balance != 0.0) {
                    Text("Cannot delete bank with non-zero balance (${currencyFormat.format(bankWithBalance.balance)}). Please transfer the funds first.")
                } else if (bankWithBalance.transactionCount > 0) {
                    Text("This bank has ${bankWithBalance.transactionCount} transactions. Are you sure you want to delete it? The transactions will remain but won't be linked to any bank.")
                } else {
                    Text("Are you sure you want to delete ${bankWithBalance.bank.name}?")
                }
            },
            confirmButton = {
                if (bankWithBalance.balance == 0.0) {
                    Button(
                        onClick = {
                            scope.launch {
                                dao.deleteBank(bankWithBalance.bank)
                                showDeleteConfirm = false
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (bankWithBalance.balance == 0.0) "Cancel" else "OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBankDialog(
    bank: Bank,
    onDismiss: () -> Unit,
    onSave: (Bank) -> Unit
) {
    var name by remember { mutableStateOf(bank.name) }
    var selectedIcon by remember { mutableStateOf(bank.icon) }
    var selectedColor by remember { mutableStateOf(bank.color) }

    val icons = listOf("💰", "💵", "💳", "🏦", "📱", "👛", "💼", "🏧", "🪙", "💶")
    val colors = listOf(
        "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
        "#F44336", "#00BCD4", "#FFEB3B", "#795548"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Bank") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bank Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Icon:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icons.take(5).forEach { icon ->
                        FilterChip(
                            selected = selectedIcon == icon,
                            onClick = { selectedIcon = icon },
                            label = { Text(icon, style = MaterialTheme.typography.titleLarge) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icons.drop(5).forEach { icon ->
                        FilterChip(
                            selected = selectedIcon == icon,
                            onClick = { selectedIcon = icon },
                            label = { Text(icon, style = MaterialTheme.typography.titleLarge) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Color:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.shapes.small
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                onClick = { selectedColor = color },
                                modifier = Modifier.size(32.dp),
                                shape = MaterialTheme.shapes.small,
                                color = try {
                                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(color))
                                } catch (e: Exception) {
                                    androidx.compose.ui.graphics.Color.Gray
                                }
                            ) {}
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            bank.copy(
                                name = name,
                                icon = selectedIcon,
                                color = selectedColor
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
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