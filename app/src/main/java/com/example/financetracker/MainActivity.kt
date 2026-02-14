// MainActivity.kt
package com.example.financetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.transactionDao()

        setContent { MaterialTheme { FinanceTrackerApp(dao) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceTrackerApp(dao: TransactionDao) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items =
            listOf(
                    Screen.Home,
                    Screen.AddTransaction,
                    Screen.QuickAdd,
                    Screen.Banks, // NEW
                    Screen.Statistics,
                    Screen.Manage
            )

    Scaffold(
            bottomBar = {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title, fontSize = 10.sp) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                        )
                    }
                }
            }
    ) { innerPadding ->
        NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(dao) }
            composable(Screen.AddTransaction.route) { AddTransactionScreen(dao) }
            composable(Screen.QuickAdd.route) { QuickAddScreen(dao) }
            composable(Screen.Banks.route) { // NEW
                BanksDashboardScreen(dao)
            }
            composable(Screen.Manage.route) { ManageScreen(dao) }
            composable(Screen.Statistics.route) { StatisticsScreen(dao) }
        }
    }
}
