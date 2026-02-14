//BanksDashboardScreen.kt
package com.example.financetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanksDashboardScreen(dao: TransactionDao) {
    val screenSize = rememberPhoneScreenSize()
    val padding = getResponsivePadding()
    val spacing = getResponsiveSpacing()
    val scope = rememberCoroutineScope()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    val banks by dao.getAllBanks().collectAsState(initial = emptyList())

    // Get balances for each bank
    val bankBalances = banks.map { bank ->
        val balance by dao.getBankBalance(bank.id).collectAsState(initial = 0.0)
        val transactionCount by dao.getTransactionCountByBank(bank.id).collectAsState(initial = 0)
        BankWithBalance(bank, balance ?: 0.0, transactionCount)
    }

    var showAddBankDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var selectedBank by remember { mutableStateOf<BankWithBalance?>(null) }
    var showBankDetailsDialog by remember { mutableStateOf(false) }

    // Balance visibility state (default hidden)
    var isBalanceVisible by remember { mutableStateOf(false) }

    // Calculate total across all banks
    val totalBalance = bankBalances.sumOf { it.balance }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Header with total balance
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
                        text = "Total Balance",
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

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isBalanceVisible) {
                        Text(
                            text = currencyFormat.format(totalBalance),
                            style = when (screenSize) {
                                PhoneScreenSize.Small -> MaterialTheme.typography.headlineMedium
                                PhoneScreenSize.Regular -> MaterialTheme.typography.headlineLarge
                                PhoneScreenSize.Large -> MaterialTheme.typography.displaySmall
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (totalBalance >= 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Across ${banks.size} ${if (banks.size == 1) "bank" else "banks"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    } else {
                        // Show asterisks when hidden
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

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Button(
                onClick = { showAddBankDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Bank")
            }

            Button(
                onClick = { showTransferDialog = true },
                modifier = Modifier.weight(1f),
                enabled = banks.size >= 2,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Transfer")
            }
        }

        Spacer(modifier = Modifier.height(spacing))

        // Banks grid
        if (banks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No banks yet!\nTap 'Add Bank' to get started",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
                contentPadding = PaddingValues(bottom = spacing)
            ) {
                items(bankBalances, key = { it.bank.id }) { bankWithBalance ->
                    BankCard(
                        bankWithBalance = bankWithBalance,
                        currencyFormat = currencyFormat,
                        isBalanceVisible = isBalanceVisible,
                        onClick = {
                            selectedBank = bankWithBalance
                            showBankDetailsDialog = true
                        }
                    )
                }
            }
        }
    }

    // Add Bank Dialog
    if (showAddBankDialog) {
        AddBankDialog(
            onDismiss = { showAddBankDialog = false },
            onSave = { bank ->
                scope.launch {
                    dao.insertBank(bank)
                    showAddBankDialog = false
                }
            }
        )
    }

    // Transfer Dialog
    if (showTransferDialog) {
        TransferMoneyDialog(
            banks = banks,
            dao = dao,
            onDismiss = { showTransferDialog = false }
        )
    }

    // Bank Details Dialog
    if (showBankDetailsDialog && selectedBank != null) {
        BankDetailsDialog(
            bankWithBalance = selectedBank!!,
            dao = dao,
            onDismiss = { showBankDetailsDialog = false }
        )
    }
}

@Composable
fun BankCard(
    bankWithBalance: BankWithBalance,
    currencyFormat: NumberFormat,
    isBalanceVisible: Boolean,
    onClick: () -> Unit
) {
    val screenSize = rememberPhoneScreenSize()

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(
                when (screenSize) {
                    PhoneScreenSize.Small -> 120.dp
                    PhoneScreenSize.Regular -> 140.dp
                    PhoneScreenSize.Large -> 160.dp
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = try {
                Color(android.graphics.Color.parseColor(bankWithBalance.bank.color))
                    .copy(alpha = 0.2f)
            } catch (e: Exception) {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = bankWithBalance.bank.icon,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (isBalanceVisible) {
                    Text(
                        text = "${bankWithBalance.transactionCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column {
                Text(
                    text = bankWithBalance.bank.name,
                    style = when (screenSize) {
                        PhoneScreenSize.Small -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (isBalanceVisible) {
                    Text(
                        text = currencyFormat.format(bankWithBalance.balance),
                        style = when (screenSize) {
                            PhoneScreenSize.Small -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleLarge
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (bankWithBalance.balance >= 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "••••••",
                        style = when (screenSize) {
                            PhoneScreenSize.Small -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleLarge
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankDialog(
    onDismiss: () -> Unit,
    onSave: (Bank) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("💰") }
    var selectedColor by remember { mutableStateOf("#4CAF50") }

    val icons = listOf("💰", "💵", "💳", "🏦", "📱", "👛", "💼", "🏧", "🪙", "💶")
    val colors = listOf(
        "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
        "#F44336", "#00BCD4", "#FFEB3B", "#795548"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bank/Wallet") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bank Name") },
                    placeholder = { Text("e.g., GCash, My Wallet") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Choose Icon:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Horizontally scrollable icon row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(icons) { icon ->
                        FilterChip(
                            selected = selectedIcon == icon,
                            onClick = { selectedIcon = icon },
                            label = { Text(icon, style = MaterialTheme.typography.titleLarge) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Choose Color:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Horizontally scrollable color row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colors) { color ->
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
                                    Color(android.graphics.Color.parseColor(color))
                                } catch (e: Exception) {
                                    Color.Gray
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
                            Bank(
                                name = name,
                                icon = selectedIcon,
                                color = selectedColor
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
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