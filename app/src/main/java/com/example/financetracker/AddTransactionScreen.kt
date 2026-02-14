//AddTransactionScreen.kt
package com.example.financetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(dao: TransactionDao) {
    val screenSize = rememberPhoneScreenSize()
    val padding = getResponsivePadding()
    val spacing = getResponsiveSpacing()
    val scrollState = rememberScrollState()

    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Expense") }
    var category by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedBankId by remember { mutableStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var showBankError by remember { mutableStateOf(false) }
    var showInsufficientBalanceError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Get banks
    val banks by dao.getAllBanks().collectAsState(initial = emptyList())
    val selectedBank = banks.find { it.id == selectedBankId }

    // Get selected bank balance
    val selectedBankBalance by if (selectedBankId > 0) {
        dao.getBankBalance(selectedBankId).collectAsState(initial = 0.0)
    } else {
        remember { mutableStateOf(0.0) }
    }

    var showBankDropdown by remember { mutableStateOf(false) }

    val expenseCategories = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Education", "Other")
    val incomeCategories = listOf("Salary", "Freelance", "Business", "Investment", "Gift", "Other")
    val categories = if (type == "Expense") expenseCategories else incomeCategories

    val headlineStyle = when (screenSize) {
        PhoneScreenSize.Small -> MaterialTheme.typography.headlineSmall
        PhoneScreenSize.Regular -> MaterialTheme.typography.headlineMedium
        PhoneScreenSize.Large -> MaterialTheme.typography.headlineLarge
    }

    val amountTextStyle = when (screenSize) {
        PhoneScreenSize.Small -> MaterialTheme.typography.titleLarge
        PhoneScreenSize.Regular -> MaterialTheme.typography.headlineSmall
        PhoneScreenSize.Large -> MaterialTheme.typography.headlineMedium
    }

    val buttonHeight = when (screenSize) {
        PhoneScreenSize.Small -> 48.dp
        PhoneScreenSize.Regular -> 56.dp
        PhoneScreenSize.Large -> 64.dp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(padding)
    ) {
        Text(
            text = "Add Transaction",
            style = headlineStyle,
            modifier = Modifier.padding(bottom = spacing)
        )

        // Type selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = type == "Expense",
                    onClick = {
                        type = "Expense"
                        category = ""
                        customCategory = ""
                    },
                    label = { Text("Expense", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
                FilterChip(
                    selected = type == "Income",
                    onClick = {
                        type = "Income"
                        category = ""
                        customCategory = ""
                    },
                    label = { Text("Income", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing))

        // Bank selector - NEW
        Text(
            text = "Bank/Wallet",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
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
                        text = { Text("No banks available. Create one first!") },
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
                                showInsufficientBalanceError = false
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

        if (showInsufficientBalanceError) {
            Text(
                text = "Insufficient balance in ${selectedBank?.name}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(spacing))

        // Amount input
        OutlinedTextField(
            value = amount,
            onValueChange = {
                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                    amount = it
                    showInsufficientBalanceError = false
                }
            },
            label = { Text("Amount") },
            placeholder = { Text("0.00") },
            leadingIcon = {
                Text(
                    "₱",
                    style = when (screenSize) {
                        PhoneScreenSize.Small -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleLarge
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = amountTextStyle
        )

        Spacer(modifier = Modifier.height(spacing))

        // Category selector
        Text(
            text = "Category",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CategoryChipsGrid(
            categories = categories,
            selectedCategory = category,
            onCategorySelected = {
                category = it
                if (it != "Other") {
                    customCategory = ""
                }
            },
            screenSize = screenSize,
            spacing = spacing
        )

        Spacer(modifier = Modifier.height(spacing))

        // Custom category input
        if (category == "Other") {
            OutlinedTextField(
                value = customCategory,
                onValueChange = { customCategory = it },
                label = { Text("Custom Category") },
                placeholder = { Text("Enter category name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(spacing))
        }

        // Description input
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            placeholder = { Text("What was this for?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = when (screenSize) {
                PhoneScreenSize.Small -> 2
                else -> 3
            },
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(spacing))

        // Date picker
        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(Date(selectedDate)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Select date"
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing * 2))

        // Save button
        Button(
            onClick = {
                val amountValue = amount.toDoubleOrNull()
                val finalCategory = if (category == "Other" && customCategory.isNotBlank()) {
                    customCategory
                } else if (category != "Other") {
                    category
                } else {
                    ""
                }

                // Validation
                when {
                    selectedBankId == 0 -> {
                        showBankError = true
                        showInsufficientBalanceError = false
                    }
                    amountValue == null || amountValue <= 0 -> {
                        // Amount validation handled by button enabled state
                    }
                    type == "Expense" && amountValue > (selectedBankBalance ?: 0.0) -> {
                        showInsufficientBalanceError = true
                        showBankError = false
                    }
                    finalCategory.isBlank() -> {
                        // Category validation handled by button enabled state
                    }
                    else -> {
                        scope.launch {
                            dao.insert(
                                Transaction(
                                    amount = amountValue,
                                    type = type,
                                    category = finalCategory,
                                    description = description,
                                    date = selectedDate,
                                    bankId = selectedBankId
                                )
                            )
                            amount = ""
                            description = ""
                            category = ""
                            customCategory = ""
                            selectedBankId = 0
                            selectedDate = System.currentTimeMillis()
                            showSuccessMessage = true
                            showBankError = false
                            showInsufficientBalanceError = false

                            scope.launch {
                                scrollState.animateScrollTo(0)
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight),
            enabled = amount.toDoubleOrNull() != null &&
                    amount.toDoubleOrNull()!! > 0 &&
                    (category.isNotBlank() && (category != "Other" || customCategory.isNotBlank()))
        ) {
            Text(
                text = "Save Transaction",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Success message
        if (showSuccessMessage) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                showSuccessMessage = false
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "✓ Transaction added successfully!",
                    modifier = Modifier.padding(spacing),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing))
    }

    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChipsGrid(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    screenSize: PhoneScreenSize,
    spacing: Dp
) {
    val chipsPerRow = when (screenSize) {
        PhoneScreenSize.Small -> 3
        PhoneScreenSize.Regular -> 4
        PhoneScreenSize.Large -> 4
    }

    val chipTextStyle = when (screenSize) {
        PhoneScreenSize.Small -> MaterialTheme.typography.bodySmall
        else -> MaterialTheme.typography.bodyMedium
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        categories.chunked(chipsPerRow).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                rowCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategorySelected(cat) },
                        label = {
                            Text(
                                cat,
                                style = chipTextStyle,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(chipsPerRow - rowCategories.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
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