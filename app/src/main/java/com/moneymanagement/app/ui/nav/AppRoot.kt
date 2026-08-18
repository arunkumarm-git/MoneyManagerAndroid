package com.moneymanagement.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.ui.common.soundClick
import com.moneymanagement.app.ui.accounts.AccountsScreen
import com.moneymanagement.app.ui.categories.CategoriesScreen
import com.moneymanagement.app.ui.dashboard.DashboardScreen
import com.moneymanagement.app.ui.reports.ReportsScreen
import com.moneymanagement.app.ui.transactions.TransactionsScreen

import com.moneymanagement.app.ui.cards.CreditCardsScreen
import com.moneymanagement.app.ui.goals.SavingsGoalsScreen
import com.moneymanagement.app.ui.recurring.RecurringScreen
import com.moneymanagement.app.ui.sms.SmsInboxScreen

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem("dashboard", "Home", Icons.Filled.Home),
    BottomNavItem("transactions", "History", Icons.Filled.SwapHoriz),
    BottomNavItem("goals", "Goals", Icons.Filled.AccountBalanceWallet),
    BottomNavItem("recurring", "Recurring", Icons.Filled.Category),
    BottomNavItem("reports", "Reports", Icons.Filled.PieChart),
)

@Composable
fun AppRoot(repository: MoneyRepository) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = soundClick {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = {
                            Text(
                                text = item.label,
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
        ) {
            composable("dashboard") {
                DashboardScreen(
                    repository = repository,
                    onSeeAllTransactions = {
                        navController.navigate("transactions") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSms = {
                        navController.navigate("sms_inbox")
                    },
                    onNavigateToCards = {
                        navController.navigate("cards")
                    },
                )
            }
            composable("transactions") { TransactionsScreen(repository) }
            composable("accounts") { AccountsScreen(repository) }
            composable("categories") { CategoriesScreen(repository) }
            composable("goals") { SavingsGoalsScreen(repository) }
            composable("recurring") { RecurringScreen(repository) }
            composable("reports") { ReportsScreen(repository) }
            composable("sms_inbox") {
                SmsInboxScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("cards") {
                CreditCardsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}


