package com.moneymanagement.app.ui.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanagement.app.data.MoneyRepository
import com.moneymanagement.app.data.SavingsGoal
import com.moneymanagement.app.settings.AppSettings
import com.moneymanagement.app.ui.common.AppDatePickerField
import com.moneymanagement.app.ui.common.ConfirmDeleteDialog
import com.moneymanagement.app.ui.common.EditSheet
import com.moneymanagement.app.ui.common.EmptyState
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.common.soundClick
import com.moneymanagement.app.ui.theme.AmberWarning
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import com.moneymanagement.app.ui.theme.SavingsGold
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Heuristically pick an emoji for a goal based on its name. */
private fun goalEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        "vacation" in lower || "trip" in lower || "travel" in lower || "holiday" in lower -> "✈️"
        "laptop" in lower || "computer" in lower || "pc" in lower || "mac" in lower -> "💻"
        "phone" in lower || "mobile" in lower || "iphone" in lower -> "📱"
        "car" in lower || "bike" in lower || "vehicle" in lower -> "🚗"
        "home" in lower || "house" in lower || "rent" in lower || "flat" in lower -> "🏠"
        "wedding" in lower || "marriage" in lower -> "💍"
        "baby" in lower || "child" in lower -> "👶"
        "education" in lower || "course" in lower || "degree" in lower || "school" in lower || "college" in lower -> "🎓"
        "emergency" in lower || "fund" in lower || "safety" in lower -> "🛡️"
        "invest" in lower || "stock" in lower || "crypto" in lower -> "📈"
        "health" in lower || "medical" in lower || "gym" in lower || "fitness" in lower -> "💪"
        "gift" in lower || "present" in lower -> "🎁"
        "book" in lower || "read" in lower -> "📚"
        "watch" in lower || "gold" in lower || "jewelry" in lower -> "⌚"
        else -> "🎯"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    repository: MoneyRepository,
    onBack: (() -> Unit)? = null,
) {
    val goals by repository.savingsGoals.collectAsState(initial = emptyList())
    val currencySymbol by AppSettings.currencySymbol.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var deletingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var depositingGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Savings Goals",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = soundClick { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Goal", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { innerPadding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Filled.Savings,
                    title = "No savings goals yet",
                    subtitle = "Set target goals for vacations, emergency funds, or dreams!",
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            ) {
                items(goals, key = { it.id }) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        currencySymbol = currencySymbol,
                        onDepositClick = { depositingGoal = goal },
                        onEdit = { editingGoal = goal },
                        onDelete = { deletingGoal = goal },
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog || editingGoal != null) {
        EditSheet(
            onDismiss = {
                showAddDialog = false
                editingGoal = null
            },
            title = if (editingGoal == null) "New Savings Goal" else "Edit Savings Goal",
        ) { dismiss ->
            GoalSheetContent(
                goal = editingGoal,
                currencySymbol = currencySymbol,
                onDismiss = dismiss,
                onSave = { name, target, current, targetDate, colorHex ->
                    scope.launch {
                        if (editingGoal != null) {
                            repository.updateSavingsGoal(editingGoal!!.id, name, target, current, targetDate, colorHex)
                        } else {
                            repository.addSavingsGoal(name, target, current, targetDate, colorHex)
                        }
                        dismiss()
                    }
                },
            )
        }
    }

    depositingGoal?.let { goal ->
        EditSheet(
            onDismiss = { depositingGoal = null },
            title = "Add Funds — ${goal.name}",
        ) { dismiss ->
            DepositSheetContent(
                goal = goal,
                currencySymbol = currencySymbol,
                onDismiss = dismiss,
                onConfirm = { amount ->
                    scope.launch {
                        repository.depositToSavingsGoal(goal.id, amount)
                        dismiss()
                    }
                },
            )
        }
    }

    deletingGoal?.let { goal ->
        ConfirmDeleteDialog(
            title = "Delete ${goal.name}?",
            message = "Are you sure you want to remove this savings goal?",
            onDismiss = { deletingGoal = null },
            onConfirm = {
                scope.launch {
                    repository.deleteSavingsGoal(goal.id)
                    deletingGoal = null
                }
            },
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Goal Card with Arc Progress + Emoji
// ──────────────────────────────────────────────────────────────

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    currencySymbol: String,
    onDepositClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val rawProgress = (if (goal.targetAmount > 0) goal.currentAmount / goal.targetAmount else 0.0)
        .coerceIn(0.0, 1.0)
    val percentage = (rawProgress * 100).toInt()
    val isComplete = rawProgress >= 1.0

    var showMenu by remember { mutableStateOf(false) }

    val arcColor = when {
        rawProgress >= 1.0 -> IncomeGreen
        rawProgress >= 0.8 -> SavingsGold
        rawProgress >= 0.5 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                // Arc progress ring
                ArcProgressRing(
                    progress = rawProgress.toFloat(),
                    color = arcColor,
                    emoji = goalEmoji(goal.name),
                    isComplete = isComplete,
                    modifier = Modifier.size(72.dp),
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp).weight(1f),
                ) {
                    Text(
                        goal.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!goal.targetDate.isNullOrBlank()) {
                        Text(
                            "🗓 Target: ${goal.targetDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Progress text row
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$percentage%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = arcColor,
                        )
                        Text(
                            " achieved",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                Box {
                    IconButton(onClick = soundClick { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { showMenu = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = { showMenu = false; onDelete() },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "$currencySymbol ${formatMoney(goal.currentAmount, 0)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (isComplete) "Completed! 🎉" else "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (isComplete) "$currencySymbol ${formatMoney(goal.targetAmount, 0)}" else "$currencySymbol ${formatMoney(goal.targetAmount - goal.currentAmount, 0)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete) IncomeGreen else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = soundClick(onDepositClick),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Funds", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Arc Progress Ring (Canvas)
// ──────────────────────────────────────────────────────────────

@Composable
private fun ArcProgressRing(
    progress: Float,
    color: Color,
    emoji: String,
    isComplete: Boolean,
    modifier: Modifier = Modifier,
) {
    var animatedProgress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(900),
        label = "arc_progress",
    )
    LaunchedEffect(progress) { animatedProgress = progress }

    val trackColor = color.copy(alpha = 0.15f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val inset = strokeWidth / 2
            val arcRect = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // Track
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            // Progress
            if (animated > 0f) {
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 270f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcRect,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            if (isComplete) "✅" else emoji,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Goal Sheet Content (inside EditSheet)
// ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalSheetContent(
    goal: SavingsGoal?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, target: Double, current: Double, targetDate: String?, colorHex: String) -> Unit,
) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var targetText by remember { mutableStateOf(goal?.targetAmount?.toString() ?: "") }
    var currentText by remember { mutableStateOf(goal?.currentAmount?.toString() ?: "0") }
    var targetDate by remember {
        mutableStateOf(
            goal?.targetDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now().plusMonths(6)
        )
    }
    var hasTargetDate by remember { mutableStateOf(!goal?.targetDate.isNullOrBlank()) }

    // Emoji preview
    if (name.isNotBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(goalEmoji(name), fontSize = 40.sp)
        }
    }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Goal Name") },
        placeholder = { Text("e.g. Vacation, New Laptop, Emergency Fund") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    OutlinedTextField(
        value = targetText,
        onValueChange = { targetText = it },
        label = { Text("Target Amount ($currencySymbol)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    OutlinedTextField(
        value = currentText,
        onValueChange = { currentText = it },
        label = { Text("Already Saved ($currencySymbol)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Set Target Date", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(
            checked = hasTargetDate,
            onCheckedChange = { hasTargetDate = it },
        )
    }

    if (hasTargetDate) {
        AppDatePickerField(
            label = "Target Date",
            value = targetDate,
            onChange = { targetDate = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Button(
        onClick = soundClick {
            val target = targetText.toDoubleOrNull() ?: 0.0
            val current = currentText.toDoubleOrNull() ?: 0.0
            if (name.isNotBlank() && target > 0) {
                onSave(
                    name.trim(),
                    target,
                    current,
                    if (hasTargetDate) targetDate.toString() else null,
                    "#4A55A2",
                )
            }
        },
        enabled = name.isNotBlank() && (targetText.toDoubleOrNull() ?: 0.0) > 0,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text("Save Goal", fontWeight = FontWeight.Bold)
    }
}

// ──────────────────────────────────────────────────────────────
// Deposit Sheet Content
// ──────────────────────────────────────────────────────────────

@Composable
private fun DepositSheetContent(
    goal: SavingsGoal,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }

    Text(
        "How much are you depositing towards \"${goal.name}\"?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Amount ($currencySymbol)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Button(
        onClick = soundClick {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            if (amt > 0) onConfirm(amt)
        },
        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text("Add Funds", fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = soundClick(onDismiss), modifier = Modifier.fillMaxWidth()) {
        Text("Cancel")
    }
}
