package com.moneymanagement.app.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneymanagement.app.data.AccountSpendShare
import com.moneymanagement.app.data.CategoryTotal
import com.moneymanagement.app.data.DayOfWeekSpend
import com.moneymanagement.app.data.MonthPoint
import com.moneymanagement.app.data.TrendPoint
import com.moneymanagement.app.ui.common.CategoryAvatar
import com.moneymanagement.app.ui.common.formatMoney
import com.moneymanagement.app.ui.theme.CHART_PALETTE
import com.moneymanagement.app.ui.theme.ExpenseRed
import com.moneymanagement.app.ui.theme.IncomeGreen
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BarChart(data: List<MonthPoint>, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(top = 4.dp),
        ) {
            drawLine(
                color = outline,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f,
            )
            if (data.isEmpty()) return@Canvas
            val maxVal = (data.maxOf { maxOf(it.income, it.expense) }).let { if (it <= 0.0) 1.0 else it }
            val n = data.size
            val colWidth = size.width / n
            val radius = CornerRadius(6f, 6f)
            data.forEachIndexed { idx, point ->
                val colX = idx * colWidth
                val barWidth = colWidth * 0.32f
                val gap = colWidth * 0.06f
                val incomeHeight = ((point.income / maxVal) * size.height).toFloat().coerceAtLeast(0f)
                val expenseHeight = ((point.expense / maxVal) * size.height).toFloat().coerceAtLeast(0f)
                drawRoundRect(
                    color = IncomeGreen,
                    topLeft = Offset(colX + gap, size.height - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = ExpenseRed,
                    topLeft = Offset(colX + gap * 2 + barWidth, size.height - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = radius,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEach { point ->
                Text(
                    monthAbbr(point.month),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
fun TrendBarChart(data: List<TrendPoint>, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .padding(top = 8.dp),
        ) {
            drawLine(
                color = outline,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f,
            )
            if (data.isEmpty()) return@Canvas
            val maxVal = (data.maxOf { maxOf(it.income, it.expense) }).let { if (it <= 0.0) 1.0 else it }
            val n = data.size
            val colWidth = size.width / n
            val radius = CornerRadius(6f, 6f)
            data.forEachIndexed { idx, point ->
                val colX = idx * colWidth
                val barWidth = colWidth * 0.34f
                val gap = colWidth * 0.05f
                val incomeHeight = ((point.income / maxVal) * size.height).toFloat().coerceAtLeast(0f)
                val expenseHeight = ((point.expense / maxVal) * size.height).toFloat().coerceAtLeast(0f)
                drawRoundRect(
                    color = IncomeGreen,
                    topLeft = Offset(colX + gap, size.height - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = ExpenseRed,
                    topLeft = Offset(colX + gap * 2 + barWidth, size.height - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = radius,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            data.forEach { point ->
                Text(
                    point.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun monthAbbr(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault())

@Composable
fun DonutChart(
    data: List<CategoryTotal>,
    modifier: Modifier = Modifier,
    centerLabel: String? = null,
    centerSubLabel: String? = null,
) {
    val total = data.sumOf { it.total }.let { if (it <= 0.0) 1.0 else it }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = size.minDimension * 0.28f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            var startAngle = -90f
            data.forEachIndexed { idx, entry ->
                val sweep = (360.0 * (entry.total / total)).toFloat()
                drawArc(
                    color = CHART_PALETTE[idx % CHART_PALETTE.size],
                    startAngle = startAngle,
                    sweepAngle = sweep * 0.94f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
                startAngle += sweep
            }
        }
        if (centerLabel != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centerLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (centerSubLabel != null) {
                    Text(
                        centerSubLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun ChartLegend(
    entries: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onItemClick: ((String) -> Unit)? = null,
) {
    Column(modifier = modifier) {
        entries.forEachIndexed { idx, (label, valueStr) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onItemClick != null) {
                            Modifier
                                .clickable { onItemClick(label) }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        } else {
                            Modifier.padding(vertical = 3.dp)
                        }
                    ),
            ) {
                Box(modifier = Modifier.size(10.dp).background(CHART_PALETTE[idx % CHART_PALETTE.size], CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "$label: $valueStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (onItemClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun DayOfWeekBarChart(
    data: List<DayOfWeekSpend>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹",
) {
    if (data.isEmpty()) return
    val maxVal = data.maxOf { it.totalAmount }.let { if (it <= 0.0) 1.0 else it }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            data.forEach { item ->
                val heightPct = (item.totalAmount / maxVal).toFloat().coerceIn(0.04f, 1f)
                val isPeak = item.totalAmount == maxVal && maxVal > 0

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (isPeak) {
                        Text(
                            "Peak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height((100 * heightPct).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (isPeak) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                            ),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            data.forEach { item ->
                Text(
                    text = item.dayName,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    color = if (item.totalAmount == maxVal && maxVal > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (item.totalAmount == maxVal && maxVal > 0) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
fun RankedCategoryList(
    data: List<CategoryTotal>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹",
    onItemClick: ((CategoryTotal) -> Unit)? = null,
) {
    val total = data.sumOf { it.total }.let { if (it <= 0.0) 1.0 else it }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        data.forEachIndexed { idx, cat ->
            val pct = (cat.total / total).toFloat().coerceIn(0f, 1f)
            val color = CHART_PALETTE[idx % CHART_PALETTE.size]

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onItemClick != null) { onItemClick?.invoke(cat) },
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            CategoryAvatar(cat.categoryId, cat.name)
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(
                                    cat.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${cat.count} transactions • ${(pct * 100).let { "%.0f".format(it) }}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            "$currencySymbol${formatMoney(cat.total, 0)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = color.copy(alpha = 0.18f),
                    )
                }
            }
        }
    }
}

@Composable
fun AccountSpendBarList(
    data: List<AccountSpendShare>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹",
) {
    val totalExp = data.sumOf { it.expenseAmount }.let { if (it <= 0.0) 1.0 else it }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.forEachIndexed { idx, acc ->
            val pct = (acc.expenseAmount / totalExp).toFloat().coerceIn(0f, 1f)
            val color = CHART_PALETTE[idx % CHART_PALETTE.size]

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(acc.accountName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text(
                            "$currencySymbol${formatMoney(acc.expenseAmount, 0)} (${(pct * 100).let { "%.0f".format(it) }}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = color.copy(alpha = 0.15f),
                    )
                }
            }
        }
    }
}

