package com.moneymanagement.app.ui.emergency

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneymanagement.app.data.Account
import com.moneymanagement.app.data.EmergencyFundAllocation
import com.moneymanagement.app.data.EmergencyHealthStatus
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.StorageTypeBreakdown
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.ui.common.ConfirmDeleteDialog
import com.moneymanagement.app.ui.common.DropdownField
import com.moneymanagement.app.ui.common.EditSheet
import com.moneymanagement.app.ui.common.EmptyState
import com.moneymanagement.app.ui.common.SectionHeader
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.common.soundClick
import com.moneymanagement.app.ui.theme.IncomeGreen
import com.moneymanagement.app.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyFundScreen(
    repository: MoneyRepository,
    onBack: (() -> Unit)? = null,
    viewModel: EmergencyFundViewModel = viewModel(
        factory = com.moneymanagement.app.ui.common.GenericViewModelFactory {
            EmergencyFundViewModel(repository)
        }
    ),
) {
    val state by viewModel.uiState.collectAsState()
    val currencySymbol by AppSettings.currencySymbol.collectAsState()
    val privacyMode by AppSettings.privacyModeEnabled.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingAllocation by remember { mutableStateOf<EmergencyFundAllocation?>(null) }
    var deletingAllocation by remember { mutableStateOf<EmergencyFundAllocation?>(null) }
    var adjustingAllocation by remember { mutableStateOf<EmergencyFundAllocation?>(null) }
    var showTargetConfigDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF06B6D4).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Emergency Safety Net",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Multi-location liquid reserves",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = soundClick { onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = soundClick { showTargetConfigDialog = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = "Configure Target")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = soundClick { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Stored Location") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ─────────────────────────────────────────────────────
            // 1. Hero Emergency Fund Status Card
            // ─────────────────────────────────────────────────────
            item {
                EmergencyHeroCard(
                    analysis = state.analysis,
                    currencySymbol = currencySymbol,
                    privacyMode = privacyMode,
                    onConfigureClick = soundClick { showTargetConfigDialog = true },
                )
            }

            // ─────────────────────────────────────────────────────
            // 2. Storage Distribution Bar (Where It's Stored)
            // ─────────────────────────────────────────────────────
            if (state.analysis.storageBreakdown.isNotEmpty()) {
                item {
                    StorageBreakdownCard(
                        breakdown = state.analysis.storageBreakdown,
                        currencySymbol = currencySymbol,
                        privacyMode = privacyMode,
                    )
                }
            }

            // ─────────────────────────────────────────────────────
            // 3. Stored Allocations Header & List
            // ─────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Where Funds Are Stored (${state.allocations.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (state.allocations.isEmpty()) {
                        TextButton(onClick = soundClick { viewModel.seedStarterData() }) {
                            Text("Sample Setup")
                        }
                    }
                }
            }

            if (state.allocations.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Security,
                        title = "No emergency funds added yet",
                        subtitle = "Track where your safety net is parked: Bank savings, liquid mutual funds, FDs, or physical cash.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(state.allocations, key = { it.id }) { allocation ->
                    val typeMeta = storageTypeToMeta(allocation.storageType)
                    val sharePct = if (state.analysis.totalSaved > 0.0) {
                        (allocation.amount / state.analysis.totalSaved * 100.0)
                    } else 0.0

                    AllocationCard(
                        allocation = allocation,
                        meta = typeMeta,
                        sharePercentage = sharePct,
                        currencySymbol = currencySymbol,
                        privacyMode = privacyMode,
                        onAdjust = { adjustingAllocation = allocation },
                        onEdit = { editingAllocation = allocation },
                        onDelete = { deletingAllocation = allocation },
                    )
                }
            }

            // ─────────────────────────────────────────────────────
            // 4. Financial Wisdom / 3-Bucket Rule Guide
            // ─────────────────────────────────────────────────────
            item {
                LiquidityGuideCard(recommendation = state.analysis.recommendation)
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // Dialogs & Sheets
    // ─────────────────────────────────────────────────────────

    if (showAddDialog) {
        AddEditAllocationSheet(
            initial = null,
            accounts = state.accounts,
            onDismiss = { showAddDialog = false },
            onSave = { name, inst, type, amt, rate, notes, accId ->
                viewModel.addAllocation(name, inst, type, amt, rate, notes, accId)
                showAddDialog = false
            },
        )
    }

    editingAllocation?.let { item ->
        AddEditAllocationSheet(
            initial = item,
            accounts = state.accounts,
            onDismiss = { editingAllocation = null },
            onSave = { name, inst, type, amt, rate, notes, accId ->
                viewModel.updateAllocation(item.id, name, inst, type, amt, rate, notes, accId)
                editingAllocation = null
            },
        )
    }

    deletingAllocation?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete Storage Location?",
            message = "Remove \"${item.name}\" (${currencySymbol} ${formatMoney(item.amount)}) from your emergency fund tracking?",
            onDismiss = { deletingAllocation = null },
            onConfirm = {
                viewModel.deleteAllocation(item.id)
                deletingAllocation = null
            },
        )
    }

    adjustingAllocation?.let { item ->
        QuickAdjustFundDialog(
            allocation = item,
            currencySymbol = currencySymbol,
            onDismiss = { adjustingAllocation = null },
            onAdjust = { delta ->
                viewModel.adjustAmount(item.id, delta)
                adjustingAllocation = null
            },
        )
    }

    if (showTargetConfigDialog) {
        TargetConfigDialog(
            currentMonths = state.targetMonths,
            currentCustomTarget = state.customTarget,
            currentExpenseOverride = state.expenseOverride,
            monthlyExpenseCalculated = state.analysis.monthlyExpense,
            currencySymbol = currencySymbol,
            onDismiss = { showTargetConfigDialog = false },
            onSave = { months, customTarget, expenseOverride ->
                viewModel.saveTargetSettings(months, customTarget, expenseOverride)
                showTargetConfigDialog = false
            },
        )
    }
}

// ─────────────────────────────────────────────────────────
// Hero Card
// ─────────────────────────────────────────────────────────

@Composable
private fun EmergencyHeroCard(
    analysis: com.moneymanagement.app.data.EmergencyFundAnalysis,
    currencySymbol: String,
    privacyMode: Boolean,
    onConfigureClick: () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (analysis.progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "hero_progress",
    )

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F172A), // Dark slate
            Color(0xFF1E293B), // Elevated navy
            Color(0xFF093744), // Subtle cyan glow
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp), ambientColor = Color(0xFF06B6D4)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header row: Shield badge + Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = analysis.healthStatus.emoji,
                            fontSize = 20.sp,
                        )
                        Text(
                            text = "SAFETY NET RUNWAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.2.sp,
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(android.graphics.Color.parseColor(analysis.healthStatus.colorHex)).copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = analysis.healthStatus.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(android.graphics.Color.parseColor(analysis.healthStatus.colorHex)),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                // Main Metric: Total saved with tabular typography
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (privacyMode) "$currencySymbol ••••••" else "$currencySymbol ${formatMoney(analysis.totalSaved, 0)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Target: ${if (privacyMode) "••••••" else "$currencySymbol ${formatMoney(analysis.targetAmount, 0)}"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1),
                        )
                        Text(
                            text = "•",
                            color = Color(0xFF64748B),
                        )
                        Text(
                            text = "${analysis.progressPercent.toInt()}% funded",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8),
                        )
                    }
                }

                // Progress Bar with milestone ticks
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF06B6D4),
                        trackColor = Color(0xFF334155),
                        strokeCap = StrokeCap.Round,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "0 mo",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                        )
                        Text(
                            text = "3 mo (Halfway)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                        )
                        Text(
                            text = "${analysis.targetMonths} mo (Target)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                        )
                    }
                }

                // Runway badge & configure CTA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0B132B).copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "${String.format("%.1f", analysis.coverageMonths)} Months of Living Expenses",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC),
                        )
                        Text(
                            text = "Based on ${if (privacyMode) "••••••" else "$currencySymbol ${formatMoney(analysis.monthlyExpense, 0)}"}/mo spend",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                        )
                    }

                    TextButton(
                        onClick = onConfigureClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8)),
                    ) {
                        Text("Edit Target", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Storage Distribution Breakdown Card
// ─────────────────────────────────────────────────────────

@Composable
private fun StorageBreakdownCard(
    breakdown: List<StorageTypeBreakdown>,
    currencySymbol: String,
    privacyMode: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Storage Distribution",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${breakdown.size} categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Segmented colored bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                breakdown.forEach { item ->
                    val weight = (item.percentage / 100.0).toFloat().coerceAtLeast(0.02f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxSize()
                            .background(Color(android.graphics.Color.parseColor(item.colorHex)))
                    )
                }
            }

            // Legend items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdown.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(item.colorHex)))
                            )
                            Text(
                                text = "${item.icon} ${item.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = if (privacyMode) "••••••" else "$currencySymbol ${formatMoney(item.amount, 0)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "(${String.format("%.0f", item.percentage)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Individual Allocation Item Card
// ─────────────────────────────────────────────────────────

@Composable
private fun AllocationCard(
    allocation: EmergencyFundAllocation,
    meta: StorageMeta,
    sharePercentage: Double,
    currencySymbol: String,
    privacyMode: Boolean,
    onAdjust: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    // Avatar Icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(android.graphics.Color.parseColor(meta.colorHex)).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = meta.icon, fontSize = 22.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = allocation.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = allocation.institution.ifBlank { meta.displayName },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (allocation.returnRate != null && allocation.returnRate > 0.0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = IncomeGreen.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        text = "${allocation.returnRate}% p.a.",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // More Menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Deposit / Withdraw") },
                            leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                            onClick = soundClick {
                                menuExpanded = false
                                onAdjust()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Details") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = soundClick {
                                menuExpanded = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Location", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = soundClick {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }

            // Notes / Liquidity Info if provided
            if (!allocation.notes.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "ℹ️ ${allocation.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            // Amount + Quick Adjust Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = if (privacyMode) "••••••" else "$currencySymbol ${formatMoney(allocation.amount, 0)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${String.format("%.1f", sharePercentage)}% of total fund",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                FilledTonalButton(
                    onClick = soundClick(onAdjust),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adjust Amount", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Liquidity Rule Educational Card
// ─────────────────────────────────────────────────────────

@Composable
private fun LiquidityGuideCard(recommendation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("💡", fontSize = 20.sp)
                Text(
                    "The 3-Bucket Emergency Rule",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = "1. Immediate Access (1 mo): Bank savings or cash for same-day emergencies.\n" +
                        "2. Quick Liquidity (2-3 mo): Liquid mutual funds with T+1 redemption.\n" +
                        "3. Safety Yield (2-3 mo): High-yield Fixed Deposits or short-term bonds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(
                    text = "🎯 Recommendation: $recommendation",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Dialog: Add / Edit Allocation
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditAllocationSheet(
    initial: EmergencyFundAllocation?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (name: String, institution: String, type: String, amount: Double, returnRate: Double?, notes: String?, linkedAccountId: Long?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var institution by remember { mutableStateOf(initial?.institution ?: "") }
    var storageType by remember { mutableStateOf(initial?.storageType ?: "bank_savings") }
    var amountText by remember { mutableStateOf(if (initial != null) "%.2f".format(initial.amount) else "") }
    var returnRateText by remember { mutableStateOf(if (initial?.returnRate != null) "${initial.returnRate}" else "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var selectedAccountId by remember { mutableStateOf(initial?.linkedAccountId) }

    val storageTypes = listOf(
        "bank_savings" to ("Bank Savings" to "🏦"),
        "liquid_fund" to ("Liquid Mutual Fund" to "💧"),
        "fixed_deposit" to ("Fixed Deposit / HYSA" to "📜"),
        "cash" to ("Cash on Hand" to "💵"),
        "other" to ("Other Reserve" to "🪙"),
    )

    EditSheet(
        title = if (initial == null) "Add Emergency Location" else "Edit Stored Location",
        onDismiss = onDismiss,
    ) { dismiss ->
        // Storage Type Chips
        Text(
            "Storage Category",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            storageTypes.take(3).forEach { (typeKey, pair) ->
                FilterChip(
                    selected = storageType == typeKey,
                    onClick = { storageType = typeKey },
                    label = { Text("${pair.second} ${pair.first}") },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            storageTypes.drop(3).forEach { (typeKey, pair) ->
                FilterChip(
                    selected = storageType == typeKey,
                    onClick = { storageType = typeKey },
                    label = { Text("${pair.second} ${pair.first}") },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name / Account Nickname") },
            placeholder = { Text("e.g. HDFC High Interest Savings") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = institution,
            onValueChange = { institution = it },
            label = { Text("Institution / Provider") },
            placeholder = { Text("e.g. HDFC Bank, Vanguard, Physical Safe") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Amount Stored") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = returnRateText,
            onValueChange = { returnRateText = it },
            label = { Text("Annual Yield / Interest Rate % (Optional)") },
            placeholder = { Text("e.g. 7.1") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Liquidity Notes / Instructions (Optional)") },
            placeholder = { Text("e.g. Instant ATM access, T+1 redemption up to 50k") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )

        if (accounts.isNotEmpty()) {
            val accountNames = listOf("None (Standalone)") + accounts.map { it.name }
            val currentSelectedName = accounts.find { it.id == selectedAccountId }?.name ?: "None (Standalone)"
            DropdownField(
                label = "Link to Account in App (Optional)",
                options = accountNames,
                selected = currentSelectedName,
                onSelect = { selectedName ->
                    selectedAccountId = accounts.find { it.name == selectedName }?.id
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = soundClick {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                val rate = returnRateText.toDoubleOrNull()
                if (name.isNotBlank() && amt > 0.0) {
                    onSave(
                        name.trim(),
                        institution.trim(),
                        storageType,
                        amt,
                        rate,
                        notes.trim().ifBlank { null },
                        selectedAccountId,
                    )
                    dismiss()
                }
            },
            enabled = name.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(if (initial == null) "Save Location" else "Update Location", fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────
// Dialog: Quick Adjust Amount (+ Deposit / - Withdraw)
// ─────────────────────────────────────────────────────────

@Composable
private fun QuickAdjustFundDialog(
    allocation: EmergencyFundAllocation,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAdjust: (delta: Double) -> Unit,
) {
    var deltaText by remember { mutableStateOf("") }
    var isDeposit by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Balance: ${allocation.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current: $currencySymbol ${formatMoney(allocation.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = isDeposit,
                        onClick = { isDeposit = true },
                        label = { Text("+ Deposit") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IncomeGreen.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = !isDeposit,
                        onClick = { isDeposit = false },
                        label = { Text("- Withdraw") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = deltaText,
                    onValueChange = { deltaText = it },
                    label = { Text(if (isDeposit) "Deposit Amount" else "Withdrawal Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                val deltaVal = deltaText.toDoubleOrNull() ?: 0.0
                val resultingAmount = if (isDeposit) allocation.amount + deltaVal else (allocation.amount - deltaVal).coerceAtLeast(0.0)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "New Balance: $currencySymbol ${formatMoney(resultingAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = soundClick {
                    val amount = deltaText.toDoubleOrNull() ?: 0.0
                    if (amount > 0.0) {
                        val finalDelta = if (isDeposit) amount else -amount
                        onAdjust(finalDelta)
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// ─────────────────────────────────────────────────────────
// Dialog: Configure Target & Months
// ─────────────────────────────────────────────────────────

@Composable
private fun TargetConfigDialog(
    currentMonths: Int,
    currentCustomTarget: Double,
    currentExpenseOverride: Double,
    monthlyExpenseCalculated: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (months: Int, customTarget: Double, expenseOverride: Double) -> Unit,
) {
    var selectedMonths by remember { mutableIntStateOf(currentMonths) }
    var useManualTarget by remember { mutableStateOf(currentCustomTarget > 0.0) }
    var manualTargetText by remember { mutableStateOf(if (currentCustomTarget > 0.0) "%.0f".format(currentCustomTarget) else "") }
    var expenseOverrideText by remember { mutableStateOf(if (currentExpenseOverride > 0.0) "%.0f".format(currentExpenseOverride) else "") }

    val presetMonths = listOf(3, 6, 9, 12)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency Target Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Select how many months of living expenses you want to protect:",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presetMonths.forEach { m ->
                        FilterChip(
                            selected = selectedMonths == m && !useManualTarget,
                            onClick = {
                                selectedMonths = m
                                useManualTarget = false
                            },
                            label = { Text("$m Months") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                OutlinedTextField(
                    value = expenseOverrideText,
                    onValueChange = { expenseOverrideText = it },
                    label = { Text("Monthly Expense Benchmark") },
                    placeholder = { Text("Auto-calculated: $currencySymbol ${formatMoney(monthlyExpenseCalculated, 0)}") },
                    supportingText = { Text("Leave blank to auto-detect from your monthly transactions") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = manualTargetText,
                    onValueChange = {
                        manualTargetText = it
                        useManualTarget = it.isNotBlank()
                    },
                    label = { Text("Or Fixed Target Amount (Optional)") },
                    placeholder = { Text("e.g. 300000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                val effectiveMonthly = expenseOverrideText.toDoubleOrNull() ?: monthlyExpenseCalculated
                val targetPreview = if (useManualTarget && manualTargetText.toDoubleOrNull() != null) {
                    manualTargetText.toDoubleOrNull()!!
                } else {
                    selectedMonths * effectiveMonthly
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Resulting Target Goal:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "$currencySymbol ${formatMoney(targetPreview, 0)} (${selectedMonths} months runway)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = soundClick {
                    val customTarget = if (useManualTarget) manualTargetText.toDoubleOrNull() ?: 0.0 else 0.0
                    val overrideExpense = expenseOverrideText.toDoubleOrNull() ?: 0.0
                    onSave(selectedMonths, customTarget, overrideExpense)
                }
            ) {
                Text("Save Target")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// ─────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────

private data class StorageMeta(val displayName: String, val icon: String, val colorHex: String)

private fun storageTypeToMeta(type: String): StorageMeta = when (type) {
    "bank_savings" -> StorageMeta("High-Yield Bank Savings", "🏦", "#3B82F6")
    "liquid_fund" -> StorageMeta("Liquid Mutual Fund", "💧", "#06B6D4")
    "fixed_deposit" -> StorageMeta("Fixed Deposit / HYSA", "📜", "#8B5CF6")
    "cash" -> StorageMeta("Cash on Hand", "💵", "#10B981")
    else -> StorageMeta("Other Liquid Reserve", "🪙", "#F59E0B")
}
