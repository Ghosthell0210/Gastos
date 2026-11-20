package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BudgetAlertLevel
import com.example.ui.components.DailySpendingBarChart
import com.example.ui.components.Formatters
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.state.FinanceDashboardData
import com.example.ui.theme.PesoClaroThemeExtras

@Composable
fun ReportesScreen(
    data: FinanceDashboardData,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = PesoClaroThemeExtras.semanticColors
    var selectedSliceId by remember(data.selectedYearMonth) { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                MonthSelectorHeader(
                    yearMonth = data.selectedYearMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth
                )
            }

            // 4 KPI Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            label = "GASTO DEL MES",
                            value = Formatters.formatCup(data.monthTotalSpent),
                            subtitle = data.monthDeltaPercent?.let { delta ->
                                val sign = if (delta > 0) "+" else ""
                                "$sign${delta.toInt()}% vs mes previo"
                            } ?: "${data.monthExpenses.size} movimientos",
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            label = "PROMEDIO DIARIO",
                            value = Formatters.formatCup(data.dailyAverage),
                            subtitle = "Basado en el período",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            label = "DÍA PICO",
                            value = if (data.highestDayNumber != null) {
                                Formatters.formatCup(data.highestDayAmount)
                            } else {
                                "$0 CUP"
                            },
                            subtitle = data.highestDayNumber?.let { "Día $it del mes" } ?: "Sin registros",
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            label = "BALANCE DE DEUDAS",
                            value = Formatters.formatCup(kotlin.math.abs(data.netDebtBalance)),
                            subtitle = if (data.netDebtBalance >= 0) "A favor (Me deben más)" else "Por pagar neto",
                            valueColor = if (data.netDebtBalance >= 0) semantic.owedToMe else semantic.iOwe,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (data.monthExpenses.isEmpty()) {
                item {
                    EmptyFinanceCard(
                        icon = Icons.Rounded.Analytics,
                        title = "Sin datos de gastos en ${Formatters.formatYearMonthLabel(data.selectedYearMonth)}",
                        subtitle = "Registra gastos en este mes para ver el gráfico por categorías y la tendencia diaria."
                    )
                }
            } else {
                // Donut Chart + Category Breakdown
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Distribución por categoría", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Toca un segmento",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            InteractiveDonutChart(
                                breakdowns = data.categoryBreakdowns,
                                totalSpent = data.monthTotalSpent,
                                selectedCategoryId = selectedSliceId,
                                onSelectCategory = { selectedSliceId = it }
                            )

                            Spacer(Modifier.height(18.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                data.categoryBreakdowns.forEach { item ->
                                    val isSelected = selectedSliceId == null || selectedSliceId == item.categoryId
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedSliceId = if (selectedSliceId == item.categoryId) null else item.categoryId
                                            }
                                            .background(
                                                if (selectedSliceId == item.categoryId) {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(item.color)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = item.categoryName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (item.budgetAlertLevel != null && item.budgetAlertLevel != BudgetAlertLevel.SAFE) {
                                                    Spacer(Modifier.width(6.dp))
                                                    val isExceeded = item.budgetAlertLevel == BudgetAlertLevel.EXCEEDED_100
                                                    Surface(
                                                        color = if (isExceeded) semantic.budgetExceededContainer else semantic.budgetWarningContainer,
                                                        shape = RoundedCornerShape(999.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isExceeded) "100%+" else "75%+",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isExceeded) semantic.budgetExceeded else semantic.budgetWarning,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${Formatters.formatCup(item.amount)} (${item.percentage.toInt()}%)",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                                            color = item.color,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            strokeCap = StrokeCap.Round,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                        )
                                        item.budgetLimit?.let { limit ->
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "Límite mensual: ${Formatters.formatCup(limit)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Spending Bar Chart
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Ritmo diario de gastos", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            DailySpendingBarChart(
                                dailySpending = data.dailySpending,
                                daysInMonth = data.daysInMonth,
                                dailyAverage = data.dailyAverage
                            )
                        }
                    }
                }
            }

            // Monthly Debts Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Resumen de deudas", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Deudas registradas en este mes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${data.monthDebtsCreatedCount} (${Formatters.formatCup(data.monthDebtsTotalAmount)})", style = MaterialTheme.typography.labelLarge)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total pendiente por cobrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCup(data.totalOwedToMePending), style = MaterialTheme.typography.labelLarge, color = semantic.owedToMe)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total pendiente por pagar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCup(data.totalIOwePending), style = MaterialTheme.typography.labelLarge, color = semantic.iOwe)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun KpiMetricCard(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
