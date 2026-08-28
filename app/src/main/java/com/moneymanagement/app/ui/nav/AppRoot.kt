package com.moneymanagement.app.ui.nav

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem("dashboard", "Home", Icons.Filled.Home),
    BottomNavItem("transactions", "History", Icons.Filled.SwapHoriz),
    BottomNavItem("goals", "Goals", Icons.Filled.Flag),
    BottomNavItem("recurring", "Recurring", Icons.Filled.Autorenew),
    BottomNavItem("reports", "Reports", Icons.Filled.PieChart),
)

@Composable
fun AppRoot(repository: MoneyRepository) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            AnimatedBottomNav(
                items = bottomNavItems,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
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
            composable("transactions") {
                TransactionsScreen(
                    repository = repository,
                    onNavigateToSms = { navController.navigate("sms_inbox") },
                )
            }
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

/**
 * Animated bottom navigation bar with:
 * - Pill-shaped active indicator that slides between items
 * - Icon scale animation on selection (spring physics)
 * - Label shown only for selected item (Occam's Razor — reduce visual noise)
 */
@Composable
private fun AnimatedBottomNav(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.18f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                    label = "icon_scale_${item.route}",
                )
                val labelAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    label = "label_alpha_${item.route}",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = soundClick { onNavigate(item.route) },
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Pill indicator behind icon
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                            )
                        }
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(iconScale),
                        )
                    }
                    // Label only visible for selected (smooth fade)
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = labelAlpha),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
