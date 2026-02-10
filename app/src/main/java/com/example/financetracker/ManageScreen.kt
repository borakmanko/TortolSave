//ManageScreen.kt
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
import java.util.*

@Composable
// ManageScreen.kt - Add scrolling
fun ManageScreen(dao: TransactionDao) {
    val screenSize = rememberPhoneScreenSize()
    val padding = getResponsivePadding()
    val spacing = getResponsiveSpacing()

    val templates by dao.getAllTemplates().collectAsState(initial = emptyList())
    var showEditDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<TransactionTemplate?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Text(
            text = "Manage Templates",
            style = when (screenSize) {
                PhoneScreenSize.Small -> MaterialTheme.typography.headlineSmall
                else -> MaterialTheme.typography.headlineMedium
            }
        )

        Spacer(modifier = Modifier.height(spacing))

        if (templates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No templates to manage",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // LazyColumn already handles scrolling automatically
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateManageCard(
                        template = template,
                        onEdit = {
                            editingTemplate = template
                            showEditDialog = true
                        },
                        onDelete = {
                            scope.launch {
                                dao.deleteTemplate(template)
                            }
                        }
                    )
                }

                // Add bottom padding
                item {
                    Spacer(modifier = Modifier.height(spacing))
                }
            }
        }
    }

    if (showEditDialog && editingTemplate != null) {
        EditTemplateDialog(
            template = editingTemplate!!,
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
fun TemplateManageCard(
    template: TransactionTemplate,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${template.category} • ${template.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormat.format(template.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (template.type == "Income")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
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
    onDismiss: () -> Unit,
    onSave: (TransactionTemplate) -> Unit
) {
    var name by remember { mutableStateOf(template.name) }
    var amount by remember { mutableStateOf(template.amount.toString()) }
    var type by remember { mutableStateOf(template.type) }
    var category by remember { mutableStateOf(template.category) }
    var description by remember { mutableStateOf(template.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Template") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Button Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                    if (amountValue != null && name.isNotBlank()) {
                        onSave(
                            template.copy(
                                name = name,
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