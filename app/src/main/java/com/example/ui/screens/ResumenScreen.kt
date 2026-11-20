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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BudgetAlertLevel
import com.example.data.Expense
import com.example.ui.components.CategoryDefaults
import com.example.ui.components.Formatters
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.state.FinanceDashboardData
import com.example.ui.theme.PesoClaroThemeExtras

@Composable
fun ResumenScreen(
    data: FinanceDashboardData,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddExpense: () -> Unit,
    onAddDebt: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = PesoClaroThemeExtras.semanticColors
    val recentExpenses = data.monthExpenses.take(6)

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

            // Hero Emerald & Slate Balance Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    semantic.heroGradientStart,
                                    semantic.heroGradientMid,
                                    semantic.heroGradientEnd
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GASTOS DEL MES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatCup(data.monthTotalSpent),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = semantic.onHero
                                )
                            }

                            Surface(
                                onClick = onNavigateToReports,
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Reportes",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = "Ir a reportes",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        data.monthDeltaPercent?.let { delta ->
                            val isUp = delta > 0.0
                            val sign = if (isUp) "+" else ""
                            Text(
                                text = "$sign${delta.toInt()}% respecto al mes anterior",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.84f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onAddExpense,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF064E3B)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("quick_add_expense")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Gasto", fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = onAddDebt,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.18f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("quick_add_debt")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Deuda", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Debt Quick Summary Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DebtSummaryTile(
                        title = "Me deben",
                        amount = data.totalOwedToMePending,
                        accent = semantic.owedToMe,
                        container = semantic.owedToMeContainer.copy(alpha = 0.55f),
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        onClick = onNavigateToDebts,
                        modifier = Modifier.weight(1f)
                    )
                    DebtSummaryTile(
                        title = "Debo",
                        amount = data.totalIOwePending,
                        accent = semantic.iOwe,
                        container = semantic.iOweContainer.copy(alpha = 0.55f),
                        icon = Icons.AutoMirrored.Rounded.TrendingDown,
                        onClick = onNavigateToDebts,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Budget Alerts Banner (75% & 100%)
            if (data.alertingBudgets.isNotEmpty()) {
                item {
                    val exceededCount = data.alertingBudgets.count { it.alertLevel == BudgetAlertLevel.EXCEEDED_100 }
                    val isCritical = exceededCount > 0
                    val bg = if (isCritical) semantic.budgetExceededContainer else semantic.budgetWarningContainer
                    val fg = if (isCritical) semantic.budgetExceeded else semantic.budgetWarning

                    Card(
                        onClick = onNavigateToBudgets,
                        colors = CardDefaults.cardColors(containerColor = bg),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCritical) Icons.Rounded.ErrorOutline else Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = fg
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isCritical) {
                                        "Límite de presupuesto superado (100%+)"
                                    } else {
                                        "Presupuesto cerca del límite (75%+)"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = fg,
                                    fontWeight = FontWeight.Bold
                                )
                                val names = data.alertingBudgets.take(2).joinToString(", ") {
                                    "${it.budget.categoryName} (${it.percentage}%)"
                                }
                                Text(
                                    text = names,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Ver presupuestos",
                                tint = fg
                            )
                        }
                    }
                }
            }

            // Reminders Quick Strip
            item {
                val activeReminders = data.reminders.filter { it.enabled }
                Card(
                    onClick = onNavigateToReminders,
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recordatorios (${activeReminders.size} activos)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = activeReminders.firstOrNull()?.let {
                                    "${it.targetName} · ${Formatters.formatDateTime(it.scheduledAtMillis)}"
                                } ?: "Configura alertas de deudas y revisión de gastos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Abrir recordatorios",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Recent Movements Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Últimos gastos del mes",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (data.monthExpenses.isNotEmpty()) {
                        TextButton(onClick = onNavigateToExpenses) {
                            Text("Ver todos (${data.monthExpenses.size})")
                        }
                    }
                }
            }

            if (recentExpenses.isEmpty()) {
                item {
                    EmptyFinanceCard(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = "Sin gastos en ${Formatters.formatYearMonthLabel(data.selectedYearMonth)}",
                        subtitle = "Toca el botón + Gasto para registrar tu primer movimiento en CUP.",
                        actionLabel = "Registrar gasto",
                        onAction = onAddExpense
                    )
                }
            } else {
                items(recentExpenses, key = { it.id }) { expense ->
                    ExpenseRowCard(
                        expense = expense,
                        categories = data.categories,
                        onClick = { onEditExpense(expense) }
                    )
                }
            }

            if (data.monthBudgets.isEmpty()) {
                item {
                    EmptyFinanceCard(
                        icon = Icons.Rounded.Savings,
                        title = "Define límites mensuales por categoría",
                        subtitle = "Crea presupuestos y recibe alertas visuales al superar el 75% y 100%.",
                        actionLabel = "Configurar metas",
                        onAction = onNavigateToBudgets
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun DebtSummaryTile(
    title: String,
    amount: Double,
    accent: Color,
    container: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = Formatters.formatCup(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ExpenseRowCard(
    expense: Expense,
    categories: List<com.example.data.CategoryOption>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catColor = CategoryDefaults.findCategoryColor(expense.categoryId, categories)
    val catIcon = CategoryDefaults.findCategoryIcon(expense.categoryId, categories)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(catColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = catIcon,
                    contentDescription = expense.categoryName,
                    tint = catColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${expense.categoryName} · ${Formatters.formatShortDate(expense.expenseDateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "-${Formatters.formatCup(expense.amount)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EmptyFinanceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(onClick = onAction, shape = RoundedCornerShape(12.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}
