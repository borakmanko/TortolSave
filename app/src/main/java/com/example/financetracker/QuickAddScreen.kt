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
import androidx.compose.foundation.layout.PaddingValues
@Composable
fun QuickAddButton(
    template: TransactionTemplate,
    onClick: () -> Unit
) {
    val screenSize = rememberPhoneScreenSize()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    val buttonHeight = when (screenSize) {
        PhoneScreenSize.Small -> 100.dp
        PhoneScreenSize.Regular -> 120.dp
        PhoneScreenSize.Large -> 140.dp
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
            }
        }
    }
}

// Update QuickAddScreen to use responsive padding
// QuickAddScreen.kt - LazyVerticalGrid already handles scrolling
// No changes needed, but ensure it has bottom padding for the last items

@Composable
fun QuickAddScreen(dao: TransactionDao) {
    val screenSize = rememberPhoneScreenSize()
    val padding = getResponsivePadding()
    val spacing = getResponsiveSpacing()

    val templates by dao.getAllTemplates().collectAsState(initial = emptyList())
    var showAddTemplateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
            // LazyVerticalGrid handles scrolling automatically
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
                contentPadding = PaddingValues(bottom = spacing) // Add bottom padding
            ) {
                items(templates, key = { it.id }) { template ->
                    QuickAddButton(
                        template = template,
                        onClick = {
                            scope.launch {
                                dao.insert(
                                    Transaction(
                                        amount = template.amount,
                                        type = template.type,
                                        category = template.category,
                                        description = template.description,
                                        date = System.currentTimeMillis()
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
            onDismiss = { showAddTemplateDialog = false },
            onSave = { template ->
                scope.launch {
                    dao.insertTemplate(template)
                    showAddTemplateDialog = false
                }
            }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTemplateDialog(
    onDismiss: () -> Unit,
    onSave: (TransactionTemplate) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Expense") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    placeholder = { Text("e.g., 13") },
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
                    placeholder = { Text("e.g., Transport") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Daily jeepney") },
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
                            TransactionTemplate(
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