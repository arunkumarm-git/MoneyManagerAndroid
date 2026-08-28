package com.moneymanagement.app.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneymanagement.app.data.CategoryTotal
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.PeriodMetrics
import com.moneymanagement.app.data.SmartInsight
import com.moneymanagement.app.data.TimeRangePreset
import com.moneymanagement.app.data.TransactionRow
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.ui.common.CategoryAvatar
import com.moneymanagement.app.ui.common.EditSheet
import com.moneymanagement.app.ui.common.EmptyState
import com.moneymanagement.app.ui.common.GenericViewModelFactory
import com.moneymanagement.app.ui.common.SectionHeader
import com.moneymanagement.app.ui.common.TransactionTypeAvatar
import com.moneymanagement.app.ui.common.amountColor
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.common.relativeDateLabel
import com.moneymanagement.app.ui.common.signedAmountText
import com.moneymanagement.app.ui.common.soundClick
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(repository: MoneyRepository) {
    val viewModel: ReportsViewModel = viewModel(factory = GenericViewModelFactory { ReportsViewModel(repository) })
    val state by viewModel.uiState.collectAsState()
    val currencySymbol by AppSettings.currencySymbol.collectAsState()

    var drilldownCategory by remember { mutableStateOf<String?>(null) }
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearchRow by remember { mutableStateOf(false) }
    var chartTab by remember { mutableStateOf(0) } // 0 = Expense, 1 = Income

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Reports & Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = soundClick { showSearchRow = !showSearchRow }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search transactions")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // 1. Time Range Presets Bar
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TimeRangePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = state.timeRangePreset == preset,
                                onClick = soundClick {
                                    if (preset == TimeRangePreset.CUSTOM) {
                                        showCustomDateDialog = true
                                    } else {
                                        viewModel.setTimeRangePreset(preset)
                                    }
                                },
                                label = { Text(preset.displayName) },
                            )
                        }
                    }

                    // Month Stepper if THIS_MONTH is selected
                    if (state.timeRangePreset == TimeRangePreset.THIS_MONTH) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = soundClick { viewModel.changeMonth(-1) }) {
                                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                                }
                                Text(
                                    "${monthName(state.viewMonth)} ${state.viewYear}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                IconButton(onClick = soundClick { viewModel.changeMonth(1) }) {
                                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                                }
                            }
                        }
                    } else {
                        // Date Range Subtitle
                        Text(
                            text = "${state.startDate} to ${state.endDate} (${state.metrics.daysCount} days)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                        )
                    }
                }
            }

            // Optional Search Bar
            if (showSearchRow) {
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text("Search transactions, notes, merchants...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = soundClick { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }

            // 2. Secondary Filter & Dimensions Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Type Filter
                    listOf(null to "All Types", "expense" to "Expenses", "income" to "Income").forEach { (type, label) ->
                        FilterChip(
                            selected = state.selectedType == type,
                            onClick = soundClick { viewModel.setTypeFilter(type) },
                            label = { Text(label) },
                        )
                    }

                    // Account Filter Dropdown
                    Box {
                        FilterChip(
                            selected = state.selectedAccountId != null,
                            onClick = soundClick { showAccountMenu = true },
                            label = {
                                val selectedAcc = state.accounts.find { it.id == state.selectedAccountId }
                                Text(selectedAcc?.name ?: "All Accounts")
                            },
                        )
                        DropdownMenu(
                            expanded = showAccountMenu,
                            onDismissRequest = { showAccountMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Accounts") },
                                onClick = soundClick {
                                    viewModel.setAccountFilter(null)
                                    showAccountMenu = false
                                },
                            )
                            state.accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = soundClick {
                                        viewModel.setAccountFilter(acc.id)
                                        showAccountMenu = false
                                    },
                                )
                            }
                        }
                    }

                    // Category Filter Dropdown
                    Box {
                        FilterChip(
                            selected = state.selectedCategoryId != null,
                            onClick = soundClick { showCategoryMenu = true },
                            label = {
                                val selectedCat = state.categories.find { it.id == state.selectedCategoryId }
                                Text(selectedCat?.name ?: "All Categories")
                            },
                        )
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Categories") },
                                onClick = soundClick {
                                    viewModel.setCategoryFilter(null)
                                    showCategoryMenu = false
                                },
                            )
                            state.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = soundClick {
                                        viewModel.setCategoryFilter(cat.id)
                                        showCategoryMenu = false
                                    },
                                )
                            }
                        }
                    }

                    // Sort By Menu
                    Box {
                        FilterChip(
                            selected = state.sortBy != "date_desc",
                            onClick = soundClick { showSortMenu = true },
                            label = {
                                Text(
                                    when (state.sortBy) {
                                        "amount_desc" -> "Highest Amount"
                                        "amount_asc" -> "Lowest Amount"
                                        "date_asc" -> "Oldest First"
                                        else -> "Sort"
                                    }
                                )
                            },
                        )
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest Date") },
                                onClick = soundClick {
                                    viewModel.setSortBy("date_desc")
                                    showSortMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Highest Amount") },
                                onClick = soundClick {
                                    viewModel.setSortBy("amount_desc")
                                    showSortMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Lowest Amount") },
                                onClick = soundClick {
                                    viewModel.setSortBy("amount_asc")
                                    showSortMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest Date") },
                                onClick = soundClick {
                                    viewModel.setSortBy("date_asc")
                                    showSortMenu = false
                                },
                            )
                        }
                    }

                    // Clear Filters Button if any active
                    if (state.selectedType != null || state.selectedAccountId != null || state.selectedCategoryId != null || state.searchQuery.isNotEmpty()) {
                        TextButton(onClick = soundClick { viewModel.resetFilters() }) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 3. Key Metrics Scorecard
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    KeyMetricsScorecard(
                        metrics = state.metrics,
                        prevMetrics = state.previousMetrics,
                        currencySymbol = currencySymbol,
                    )
                }
            }

            // 4. Smart Insights Carousel
            if (state.smartInsights.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SectionHeader(
                            title = "Smart Insights",
                            trailing = {
                                Icon(
                                    Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                        ) {
                            items(state.smartInsights) { insight ->
                                SmartInsightCard(insight)
                            }
                        }
                    }
                }
            }

            // 5. Income vs Expense Trend Chart
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Cash Flow Trend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(IncomeGreen, CircleShape))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Income", style = MaterialTheme.typography.labelSmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(ExpenseRed, CircleShape))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Expense", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (state.trendData.isNotEmpty()) {
                            TrendBarChart(
                                data = state.trendData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )
                        } else {
                            BarChart(state.trend, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        }
                    }
                }
            }

            // 6. Category Breakdown & Donut Chart with Toggle
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Category Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = chartTab == 0,
                                    onClick = soundClick { chartTab = 0 },
                                    label = { Text("Expenses", style = MaterialTheme.typography.labelSmall) },
                                )
                                FilterChip(
                                    selected = chartTab == 1,
                                    onClick = soundClick { chartTab = 1 },
                                    label = { Text("Income", style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }

                        val activeBreakdown = if (chartTab == 0) state.breakdown else state.incomeBreakdown
                        if (activeBreakdown.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.PieChart,
                                title = "No data recorded",
                                subtitle = "No ${if (chartTab == 0) "expenses" else "income"} recorded for this period.",
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        } else {
                            val total = activeBreakdown.sumOf { it.total }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                DonutChart(
                                    data = activeBreakdown,
                                    modifier = Modifier.weight(0.52f).fillMaxSize(),
                                    centerLabel = "$currencySymbol${formatMoney(total, 0)}",
                                    centerSubLabel = if (chartTab == 0) "Expenses" else "Income",
                                )
                                ChartLegend(
                                    entries = activeBreakdown.take(5).map {
                                        it.name to "$currencySymbol${formatMoney(it.total, 0)} (${(it.total / total * 100).let { p -> "%.0f".format(p) }}%)"
                                    },
                                    modifier = Modifier.weight(0.48f),
                                    onItemClick = { categoryName ->
                                        drilldownCategory = categoryName
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // 7. Ranked Category Spend List
            if (state.breakdown.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Top Spending Categories",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "Tap to view entries",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            RankedCategoryList(
                                data = state.breakdown,
                                currencySymbol = currencySymbol,
                                onItemClick = { cat -> drilldownCategory = cat.name },
                            )
                        }
                    }
                }
            }

            // 8. Day-of-Week Spending Heat / Distribution
            if (state.dayOfWeekBreakdown.isNotEmpty() && state.metrics.expense > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Spending by Day of Week",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "See which days of the week you spend the most",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DayOfWeekBarChart(
                                data = state.dayOfWeekBreakdown,
                                currencySymbol = currencySymbol,
                            )
                        }
                    }
                }
            }

            // 9. Top Merchants & Vendors List
            if (state.topMerchants.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Top Merchants & Payees",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.topMerchants.take(5).forEachIndexed { idx, m ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    "${idx + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                )
                                            }
                                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                                Text(m.merchantName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                Text("${m.count} payments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Text(
                                            "$currencySymbol${formatMoney(m.totalAmount, 0)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    if (idx < 4 && idx < state.topMerchants.size - 1) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 10. Account Spending Distribution
            if (state.accountBreakdown.isNotEmpty() && state.metrics.expense > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Account Utilization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            AccountSpendBarList(
                                data = state.accountBreakdown,
                                currencySymbol = currencySymbol,
                            )
                        }
                    }
                }
            }

            // 11. Budget Watch
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Monthly Budget Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val budgeted = state.budgetStatus.filter { it.budget != null }
                        if (budgeted.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.PieChart,
                                title = "No budgets configured",
                                subtitle = "Set category spending limits in the Categories tab to track monthly health.",
                            )
                        } else {
                            Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                budgeted.forEach { s ->
                                    val budget = s.budget ?: 0.0
                                    val over = s.spent > budget
                                    val pct = (s.spent / (if (budget > 0) budget else 1.0)).toFloat().coerceIn(0f, 1f)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(s.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "$currencySymbol${formatMoney(s.spent, 0)} / $currencySymbol${formatMoney(budget, 0)}",
                                                color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { pct },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .padding(top = 4.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (over) ExpenseRed else if (pct > 0.85f) MaterialTheme.colorScheme.tertiary else IncomeGreen,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 12. Filtered Transactions Preview List
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SectionHeader(
                        title = "Transactions (${state.filteredTransactions.size})",
                        trailing = {
                            if (state.filteredTransactions.isNotEmpty()) {
                                Text(
                                    "Total: $currencySymbol${formatMoney(state.filteredTransactions.filter { it.txn.type == "expense" }.sumOf { it.txn.amount }, 0)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }

            if (state.filteredTransactions.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "No transactions found",
                        subtitle = "No transactions match your currently active filters and date range.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                items(state.filteredTransactions.take(15), key = { "rep-${it.txn.id}" }) { row ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TransactionTypeAvatar(row.txn.type)
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(
                                    text = (row.categoryName ?: row.txn.type.replaceFirstChar { it.uppercase() }) + " · " + (row.accountName ?: "Account"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                val note = row.txn.note ?: ""
                                Text(
                                    text = if (note.isNotBlank()) "$note · ${row.txn.txnDate}" else row.txn.txnDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                signedAmountText(row.txn.type, row.txn.amount),
                                color = amountColor(row.txn.type),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            }
        }
    }

    // Category Drilldown Bottom Sheet
    drilldownCategory?.let { categoryName ->
        val filteredTxns = state.filteredTransactions.filter { row ->
            row.categoryName.equals(categoryName, ignoreCase = true)
        }
        val totalSpent = filteredTxns.sumOf { it.txn.amount }

        EditSheet(onDismiss = { drilldownCategory = null }) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${state.startDate} to ${state.endDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "$currencySymbol${formatMoney(totalSpent, 0)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredTxns.isEmpty()) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "No transactions found",
                        subtitle = "No entries found for $categoryName in this timeframe.",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredTxns, key = { "drill-${it.txn.id}" }) { row ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TransactionTypeAvatar(row.txn.type)
                                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                        Text(
                                            relativeDateLabel(row.txn.txnDate) + " · " + (row.accountName ?: ""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        val note = row.txn.note ?: ""
                                        if (note.isNotBlank()) {
                                            Text(
                                                note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Text(
                                        signedAmountText(row.txn.type, row.txn.amount),
                                        color = amountColor(row.txn.type),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Date Range Dialog
    if (showCustomDateDialog) {
        var startInput by remember { mutableStateOf(state.startDate.ifEmpty { LocalDate.now().minusDays(30).toString() }) }
        var endInput by remember { mutableStateOf(state.endDate.ifEmpty { LocalDate.now().toString() }) }

        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            title = { Text("Custom Date Range") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startInput,
                        onValueChange = { startInput = it },
                        label = { Text("Start Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = endInput,
                        onValueChange = { endInput = it },
                        label = { Text("End Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = soundClick {
                        if (startInput.isNotBlank() && endInput.isNotBlank()) {
                            viewModel.setCustomDateRange(startInput.trim(), endInput.trim())
                            showCustomDateDialog = false
                        }
                    }
                ) {
                    Text("Apply Range")
                }
            },
            dismissButton = {
                TextButton(onClick = soundClick { showCustomDateDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun KeyMetricsScorecard(
    metrics: PeriodMetrics,
    prevMetrics: PeriodMetrics?,
    currencySymbol: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Income & Expense Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val expChange = if (prevMetrics != null && prevMetrics.expense > 0) {
                ((metrics.expense - prevMetrics.expense) / prevMetrics.expense) * 100.0
            } else null

            val incChange = if (prevMetrics != null && prevMetrics.income > 0) {
                ((metrics.income - prevMetrics.income) / prevMetrics.income) * 100.0
            } else null

            // Expense Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Total Expense", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        "$currencySymbol${formatMoney(metrics.expense, 0)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (expChange != null) {
                        Text(
                            text = "${if (expChange >= 0) "+" else ""}${"%.0f".format(expChange)}% vs last period",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (expChange > 0) ExpenseRed else IncomeGreen,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            // Income Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Total Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        "$currencySymbol${formatMoney(metrics.income, 0)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (incChange != null) {
                        Text(
                            text = "${if (incChange >= 0) "+" else ""}${"%.0f".format(incChange)}% vs last period",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (incChange >= 0) IncomeGreen else ExpenseRed,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }

        // Net Savings & Stats Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (metrics.netSavings >= 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Net Savings", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "$currencySymbol${formatMoney(metrics.netSavings, 0)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (metrics.netSavings >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Savings Rate", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${"%.1f".format(metrics.savingsRatePct)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (metrics.savingsRatePct >= 20) IncomeGreen else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Quick Indicators Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            QuickMetricItem(
                label = "Daily Avg",
                value = "$currencySymbol${formatMoney(metrics.dailyAverageSpend, 0)}",
                modifier = Modifier.weight(1f),
            )
            QuickMetricItem(
                label = "Transactions",
                value = "${metrics.transactionCount}",
                modifier = Modifier.weight(1f),
            )
            QuickMetricItem(
                label = "Avg Spend",
                value = "$currencySymbol${formatMoney(metrics.averageTransactionAmount, 0)}",
                modifier = Modifier.weight(1f),
            )
            QuickMetricItem(
                label = "Top Single",
                value = "$currencySymbol${formatMoney(metrics.largestExpense, 0)}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmartInsightCard(insight: SmartInsight) {
    Card(
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (insight.type) {
                SmartInsight.InsightType.POSITIVE -> IncomeGreen.copy(alpha = 0.12f)
                SmartInsight.InsightType.WARNING -> ExpenseRed.copy(alpha = 0.12f)
                SmartInsight.InsightType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(insight.icon, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (insight.type) {
                        SmartInsight.InsightType.POSITIVE -> IncomeGreen
                        SmartInsight.InsightType.WARNING -> ExpenseRed
                        SmartInsight.InsightType.NEUTRAL -> MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun monthName(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
