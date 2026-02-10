//NavigationItems.kt
package com.example.financetracker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object AddTransaction : Screen("add_transaction", "Add", Icons.Default.AccountBalance)
    object QuickAdd : Screen("quick_add", "Quick", Icons.Default.Add)
    object Statistics : Screen("statistics", "Stats", Icons.Default.BarChart)
    object Manage : Screen("manage", "Manage", Icons.Default.Settings)
}