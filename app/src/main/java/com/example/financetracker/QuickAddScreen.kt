//QuickAddScreen.kt
package com.example.financetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.flow.first
@Composable
fun QuickAddButton(
    template: TransactionTemplate,
    bankName: String,
    onClick: () -> Unit
) {
    val screenSize = rememberPhoneScreenSize()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    val buttonHeight = when (screenSize) {
        PhoneScreenSize.Small -> 120.dp
        PhoneScreenSize.Regular -> 140.dp
        PhoneScreenSize.Large -> 160.dp
    }

    val titleStyle = when (screenSize) {
        PhoneScreenSize.Small -> MaterialTheme.typography.titleSmall
        else -> MaterialTheme.typography.titleMedium
    }

    val amountStyle = when (screenSize) {
        PhoneScreenSize.Small -> MaterialTheme.typography.titleMedium
        PhoneScreenSize.Regular -> MaterialTheme.typography.titleLarge
        PhoneScreenSize.Large -> MaterialTheme.typography.headlineSmall
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight),
        colors = CardDefaults.cardColors(
            containerColor = if (template.type == "Income")
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    when (screenSize) {
                        PhoneScreenSize.Small -> 8.dp
                        else -> 12.dp
                    }
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = template.name,
                style = titleStyle,
                maxLines = 2
            )
            Column {
                Text(
                    text = currencyFormat.format(template.amount),
                    style = amountStyle
                )
                Text(
                    text = template.category,
                    style = MaterialTheme.typography.bodySmall
                )
                if (bankName.isNotEmpty()) {
                    Text(
                        text = "→ $bankName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAddScreen(dao: TransactionDao) {
    val screenSize = rememberPhoneScreenSize()
    val padding = getResponsivePadding()
    val spacing = getResponsiveSpacing()

    val templates by dao.getAllTemplates().collectAsState(initial = emptyList())
    val banks by dao.getAllBanks().collectAsState(initial = emptyList())

    var showAddTemplateDialog by remember { mutableStateOf(false) }
    var showInsufficientBalanceDialog by remember { mutableStateOf(false) }
    var insufficientBalanceMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // Create a map of bankId to bank name
    val bankMap = banks.associateBy { it.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Add",
                style = when (screenSize) {
                    PhoneScreenSize.Small -> MaterialTheme.typography.headlineSmall
                    else -> MaterialTheme.typography.headlineMedium
                }
            )
            FloatingActionButton(
                onClick = { showAddTemplateDialog = true },
                modifier = Modifier.size(
                    when (screenSize) {
                        PhoneScreenSize.Small -> 48.dp
                        else -> 56.dp
                    }
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Template")
            }
        }

        Spacer(modifier = Modifier.height(spacing))

        if (templates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No templates yet!\nTap + to create your first quick add button",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
                contentPadding = PaddingValues(bottom = spacing)
            ) {
                items(templates, key = { it.id }) { template ->
                    val bankName = bankMap[template.bankId]?.let { "${it.icon} ${it.name}" } ?: ""

                    QuickAddButton(
                        template = template,
                        bankName = bankName,
                        onClick = {
                            scope.launch {
                                // Check if bank exists
                                if (template.bankId == 0 || bankMap[template.bankId] == null) {
                                    insufficientBalanceMessage = "Bank not found. Please edit this template."
                                    showInsufficientBalanceDialog = true
                                    return@launch
                                }

                                // Check balance for expenses
                                if (template.type == "Expense") {
                                    val balance = dao.getBankBalance(template.bankId).first() ?: 0.0
                                    if (template.amount > balance) {
                                        insufficientBalanceMessage = "Insufficient balance in ${bankMap[template.bankId]?.name}. Available: ₱$balance"
                                        showInsufficientBalanceDialog = true
                                        return@launch
                                    }
                                }

                                // Add transaction
                                dao.insert(
                                    Transaction(
                                        amount = template.amount,
                                        type = template.type,
                                        category = template.category,
                                        description = template.description,
                                        date = System.currentTimeMillis(),
                                        bankId = template.bankId
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddTemplateDialog) {
        AddTemplateDialog(
            banks = banks,
            onDismiss = { showAddTemplateDialog = false },
            onSave = { template ->
                scope.launch {
                    dao.insertTemplate(template)
                    showAddTemplateDialog = false
                }
            }
        )
    }

    // Insufficient balance dialog
    if (showInsufficientBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showInsufficientBalanceDialog = false },
            title = { Text("Cannot Add Transaction") },
            text = { Text(insufficientBalanceMessage) },
            confirmButton = {
                TextButton(onClick = { showInsufficientBalanceDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTemplateDialog(
    banks: List<Bank>,
    onDismiss: () -> Unit,
    onSave: (TransactionTemplate) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Expense") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedBankId by remember { mutableStateOf(0) }
    var showBankError by remember { mutableStateOf(false) }

    var showBankDropdown by remember { mutableStateOf(false) }
    val selectedBank = banks.find { it.id == selectedBankId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Quick Add Button") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Button Name") },
                    placeholder = { Text("e.g., Jeepney Ride") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    placeholder = { Text("e.g., 13") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("e.g., Transport") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Daily jeepney") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bank selector
                Text(
                    "Bank/Wallet",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = showBankDropdown,
                    onExpandedChange = { showBankDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedBank?.let { "${it.icon} ${it.name}" } ?: "Select bank",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBankDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        isError = showBankError
                    )
                    ExposedDropdownMenu(
                        expanded = showBankDropdown,
                        onDismissRequest = { showBankDropdown = false }
                    ) {
                        if (banks.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No banks available") },
                                onClick = { showBankDropdown = false },
                                enabled = false
                            )
                        } else {
                            banks.forEach { bank ->
                                DropdownMenuItem(
                                    text = { Text("${bank.icon} ${bank.name}") },
                                    onClick = {
                                        selectedBankId = bank.id
                                        showBankDropdown = false
                                        showBankError = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (showBankError) {
                    Text(
                        text = "Please select a bank",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    when {
                        name.isBlank() || amountValue == null || category.isBlank() -> {
                            // Validation will be handled by enabled state
                        }
                        selectedBankId == 0 -> {
                            showBankError = true
                        }
                        else -> {
                            onSave(
                                TransactionTemplate(
                                    name = name,
                                    amount = amountValue,
                                    type = type,
                                    category = category,
                                    description = description,
                                    bankId = selectedBankId
                                )
                            )
                        }
                    }
                },
                enabled = name.isNotBlank() &&
                        amount.toDoubleOrNull() != null &&
                        category.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}