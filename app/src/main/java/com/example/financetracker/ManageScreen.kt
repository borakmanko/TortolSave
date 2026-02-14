// ManageScreen.kt
package com.example.financetracker

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ManageScreen(dao: TransactionDao) {
    val screenSize = rememberPhoneScreenSize()
    val padding = getResponsivePadding()
    val spacing = getResponsiveSpacing()

    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Text(
                text = "Manage",
                style =
                        when (screenSize) {
                            PhoneScreenSize.Small -> MaterialTheme.typography.headlineSmall
                            else -> MaterialTheme.typography.headlineMedium
                        }
        )

        Spacer(modifier = Modifier.height(spacing))

        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Templates") }
            )
            Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Banks") }
            )
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Data") })
        }

        Spacer(modifier = Modifier.height(spacing))

        // Tab Content
        when (selectedTab) {
            0 -> ManageTemplatesTab(dao)
            1 -> ManageBanksTab(dao)
            2 -> ManageDataTab(dao)
        }
    }
}

@Composable
fun ManageTemplatesTab(dao: TransactionDao) {
    val spacing = getResponsiveSpacing()
    val templates by dao.getAllTemplates().collectAsState(initial = emptyList())
    val banks by dao.getAllBanks().collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<TransactionTemplate?>(null) }
    val scope = rememberCoroutineScope()

    val bankMap = banks.associateBy { it.id }

    if (templates.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                    text = "No templates to manage",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing)) {
            items(templates, key = { it.id }) { template ->
                TemplateManageCard(
                        template = template,
                        bankName = bankMap[template.bankId]?.let { "${it.icon} ${it.name}" }
                                        ?: "Unknown",
                        onEdit = {
                            editingTemplate = template
                            showEditDialog = true
                        },
                        onDelete = { scope.launch { dao.deleteTemplate(template) } }
                )
            }

            item { Spacer(modifier = Modifier.height(spacing)) }
        }
    }

    if (showEditDialog && editingTemplate != null) {
        EditTemplateDialog(
                template = editingTemplate!!,
                banks = banks,
                onDismiss = { showEditDialog = false },
                onSave = { updated ->
                    scope.launch {
                        dao.updateTemplate(updated)
                        showEditDialog = false
                    }
                }
        )
    }
}

@Composable
fun ManageBanksTab(dao: TransactionDao) {
    val spacing = getResponsiveSpacing()
    val banks by dao.getAllBanks().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Get balances for each bank
    val bankBalances =
            banks.map { bank ->
                val balance by dao.getBankBalance(bank.id).collectAsState(initial = 0.0)
                val transactionCount by
                        dao.getTransactionCountByBank(bank.id).collectAsState(initial = 0)
                BankWithBalance(bank, balance ?: 0.0, transactionCount)
            }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<BankWithBalance?>(null) }

    if (banks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                    text = "No banks to manage",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing)) {
            items(bankBalances, key = { it.bank.id }) { bankWithBalance ->
                BankManageCard(
                        bankWithBalance = bankWithBalance,
                        onEdit = {
                            editingBank = bankWithBalance
                            showEditDialog = true
                        },
                        onDelete = {
                            editingBank = bankWithBalance
                            showDeleteConfirm = true
                        }
                )
            }

            item { Spacer(modifier = Modifier.height(spacing)) }
        }
    }

    // Edit Bank Dialog
    if (showEditDialog && editingBank != null) {
        EditBankDialog(
                bank = editingBank!!.bank,
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
    if (showDeleteConfirm && editingBank != null) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

        AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Bank?") },
                text = {
                    if (editingBank!!.balance != 0.0) {
                        Text(
                                "Cannot delete bank with non-zero balance (${currencyFormat.format(editingBank!!.balance)}). Please transfer the funds first."
                        )
                    } else if (editingBank!!.transactionCount > 0) {
                        Text(
                                "This bank has ${editingBank!!.transactionCount} transactions. Are you sure you want to delete it? The transactions will remain but won't be linked to any bank."
                        )
                    } else {
                        Text("Are you sure you want to delete ${editingBank!!.bank.name}?")
                    }
                },
                confirmButton = {
                    if (editingBank!!.balance == 0.0) {
                        Button(
                                onClick = {
                                    scope.launch {
                                        dao.deleteBank(editingBank!!.bank)
                                        showDeleteConfirm = false
                                    }
                                },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                        )
                        ) { Text("Delete") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(if (editingBank!!.balance == 0.0) "Cancel" else "OK")
                    }
                }
        )
    }
}

// Data class for JSON export/import
data class FinanceBackup(
        val transactions: List<Transaction>,
        val templates: List<TransactionTemplate>,
        val banks: List<Bank>,
        val transfers: List<Transfer>,
        val budgetSettings: BudgetSettings?
)

@Composable
fun ManageDataTab(dao: TransactionDao) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // SAF launcher for export - create a new file
    val exportLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                uri?.let {
                    scope.launch {
                        isProcessing = true
                        try {
                            val backup =
                                    withContext(Dispatchers.IO) {
                                        FinanceBackup(
                                                transactions = dao.getAllTransactionsList(),
                                                templates = dao.getAllTemplatesList(),
                                                banks = dao.getAllBanksList(),
                                                transfers = dao.getAllTransfersList(),
                                                budgetSettings = dao.getBudgetSettingsDirect()
                                        )
                                    }
                            val json = Gson().toJson(backup)
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openOutputStream(uri)?.use { output ->
                                    output.write(json.toByteArray())
                                }
                            }
                            Toast.makeText(
                                            context,
                                            "Data exported successfully!",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                            context,
                                            "Export failed: ${e.message}",
                                            Toast.LENGTH_LONG
                                    )
                                    .show()
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            }

    // SAF launcher for import - pick a file
    val importLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                    uri ->
                uri?.let {
                    scope.launch {
                        try {
                            val json =
                                    withContext(Dispatchers.IO) {
                                        context.contentResolver.openInputStream(uri)?.use { input ->
                                            input.bufferedReader().readText()
                                        }
                                    }
                            if (json != null) {
                                pendingImportJson = json
                                showImportConfirm = true
                            } else {
                                Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT)
                                        .show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                            context,
                                            "Failed to read file: ${e.message}",
                                            Toast.LENGTH_LONG
                                    )
                                    .show()
                        }
                    }
                }
            }

    Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "Export Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text =
                                "Save all your transactions, templates, banks, and settings to a JSON file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                        onClick = {
                            val timestamp =
                                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                            .format(Date())
                            exportLauncher.launch("finance_backup_$timestamp.json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                ) { Text(if (isProcessing) "Processing..." else "Export to File") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "Import Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text =
                                "Restore data from a previously exported JSON file. This will replace all current data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                ) { Text(if (isProcessing) "Processing..." else "Import from File") }
            }
        }
    }

    // Import confirmation dialog
    if (showImportConfirm && pendingImportJson != null) {
        AlertDialog(
                onDismissRequest = {
                    showImportConfirm = false
                    pendingImportJson = null
                },
                title = { Text("Replace All Data?") },
                text = {
                    Text(
                            "This will delete all existing transactions, templates, banks, and settings, then replace them with the imported data. This cannot be undone."
                    )
                },
                confirmButton = {
                    Button(
                            onClick = {
                                showImportConfirm = false
                                val json = pendingImportJson ?: return@Button
                                pendingImportJson = null
                                scope.launch {
                                    isProcessing = true
                                    try {
                                        val backup =
                                                withContext(Dispatchers.IO) {
                                                    Gson().fromJson(json, FinanceBackup::class.java)
                                                }
                                        withContext(Dispatchers.IO) {
                                            // Delete all existing data
                                            dao.deleteAllTransfers()
                                            dao.deleteAllTransactions()
                                            dao.deleteAllTemplates()
                                            dao.deleteAllBanks()
                                            dao.deleteAllBudgetSettings()

                                            // Insert imported data
                                            if (backup.banks.isNotEmpty())
                                                    dao.insertAllBanks(backup.banks)
                                            if (backup.transactions.isNotEmpty())
                                                    dao.insertAllTransactions(backup.transactions)
                                            if (backup.templates.isNotEmpty())
                                                    dao.insertAllTemplates(backup.templates)
                                            if (backup.transfers.isNotEmpty())
                                                    dao.insertAllTransfers(backup.transfers)
                                            backup.budgetSettings?.let {
                                                dao.insertBudgetSettings(it)
                                            }
                                        }
                                        Toast.makeText(
                                                        context,
                                                        "Data imported successfully!",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                                        context,
                                                        "Import failed: ${e.message}",
                                                        Toast.LENGTH_LONG
                                                )
                                                .show()
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            },
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                    )
                    ) { Text("Replace All Data") }
                },
                dismissButton = {
                    TextButton(
                            onClick = {
                                showImportConfirm = false
                                pendingImportJson = null
                            }
                    ) { Text("Cancel") }
                }
        )
    }
}

@Composable
fun TemplateManageCard(
        template: TransactionTemplate,
        bankName: String,
        onEdit: () -> Unit,
        onDelete: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        text = "${template.category} • ${template.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = template.description, style = MaterialTheme.typography.bodySmall)
                if (bankName.isNotEmpty()) {
                    Text(
                            text = "→ $bankName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                        text = currencyFormat.format(template.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color =
                                if (template.type == "Income") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BankManageCard(bankWithBalance: BankWithBalance, onEdit: () -> Unit, onDelete: () -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                        text = bankWithBalance.bank.icon,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                            text = bankWithBalance.bank.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                    )
                    Text(
                            text = "${bankWithBalance.transactionCount} transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                        text = currencyFormat.format(bankWithBalance.balance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color =
                                if (bankWithBalance.balance >= 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTemplateDialog(
        template: TransactionTemplate,
        banks: List<Bank>,
        onDismiss: () -> Unit,
        onSave: (TransactionTemplate) -> Unit
) {
    var name by remember { mutableStateOf(template.name) }
    var amount by remember { mutableStateOf(template.amount.toString()) }
    var type by remember { mutableStateOf(template.type) }
    var category by remember { mutableStateOf(template.category) }
    var description by remember { mutableStateOf(template.description) }
    var selectedBankId by remember { mutableStateOf(template.bankId) }
    var showBankDropdown by remember { mutableStateOf(false) }

    val selectedBank = banks.find { it.id == selectedBankId }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Template") },
            text = {
                Column {
                    OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Button Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
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
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bank selector
                    Text("Bank/Wallet", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                            expanded = showBankDropdown,
                            onExpandedChange = { showBankDropdown = it }
                    ) {
                        OutlinedTextField(
                                value = selectedBank?.let { "${it.icon} ${it.name}" }
                                                ?: "Select bank",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = showBankDropdown
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                                expanded = showBankDropdown,
                                onDismissRequest = { showBankDropdown = false }
                        ) {
                            banks.forEach { bank ->
                                DropdownMenuItem(
                                        text = { Text("${bank.icon} ${bank.name}") },
                                        onClick = {
                                            selectedBankId = bank.id
                                            showBankDropdown = false
                                        }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            val amountValue = amount.toDoubleOrNull()
                            if (amountValue != null && name.isNotBlank()) {
                                onSave(
                                        template.copy(
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
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
