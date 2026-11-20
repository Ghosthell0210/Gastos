package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Budget
import com.example.data.BudgetAlertLevel
import com.example.data.CategoryOption
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.CategoryDefaults
import com.example.ui.components.Formatters
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.state.FinanceDashboardData
import com.example.ui.theme.PesoClaroThemeExtras

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresupuestosScreen(
    data: FinanceDashboardData,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddBudget: () -> Unit,
    onAddBudgetForCategory: (CategoryOption) -> Unit,
    onEditBudget: (Budget) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = PesoClaroThemeExtras.semanticColors
    val budgetedIds = remember(data.monthBudgets) {
        data.monthBudgets.map { it.budget.categoryId }.toSet()
    }
    val unbudgetedCategories = remember(data.categories, budgetedIds) {
        data.categories.filter { it.id !in budgetedIds }
    }

    val totalLimit = remember(data.monthBudgets) { data.monthBudgets.sumOf { it.budget.limitAmount } }
    val totalSpentInBudgeted = remember(data.monthBudgets) { data.monthBudgets.sumOf { it.spentAmount } }
    val warning75Count = data.monthBudgets.count { it.alertLevel == BudgetAlertLevel.WARNING_75 }
    val exceeded100Count = data.monthBudgets.count { it.alertLevel == BudgetAlertLevel.EXCEEDED_100 }

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

            if (data.monthBudgets.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "PRESUPUESTO TOTAL DEL MES",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.formatCup(totalLimit),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "GASTADO EN METAS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.formatCup(totalSpentInBudgeted),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (warning75Count > 0 || exceeded100Count > 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (exceeded100Count > 0) {
                                        Surface(
                                            color = semantic.budgetExceededContainer,
                                            shape = RoundedCornerShape(999.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = semantic.budgetExceeded,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "$exceeded100Count superado(s) (>=100%)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = semantic.budgetExceeded,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    if (warning75Count > 0) {
                                        Surface(
                                            color = semantic.budgetWarningContainer,
                                            shape = RoundedCornerShape(999.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.WarningAmber,
                                                    contentDescription = null,
                                                    tint = semantic.budgetWarning,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = "$warning75Count en alerta (>=75%)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = semantic.budgetWarning,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(data.monthBudgets, key = { it.budget.id }) { progress ->
                    BudgetProgressCard(
                        progress = progress,
                        categories = data.categories,
                        onClick = { onEditBudget(progress.budget) }
                    )
                }
            } else {
                item {
                    EmptyFinanceCard(
                        icon = Icons.Rounded.Savings,
                        title = "Sin presupuestos en ${Formatters.formatYearMonthLabel(data.selectedYearMonth)}",
                        subtitle = "Establece límites mensuales en CUP por categoría. Te avisaremos visualmente al alcanzar el 75% y al superar el 100%.",
                        actionLabel = "Crear presupuesto",
                        onAction = onAddBudget
                    )
                }
            }

            if (unbudgetedCategories.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Agregar límite rápido por categoría",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            unbudgetedCategories.forEach { cat ->
                                AssistChip(
                                    onClick = { onAddBudgetForCategory(cat) },
                                    label = { Text(cat.name) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = CategoryDefaults.iconForKey(cat.iconKey),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Definir presupuesto",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("quick_budget_${cat.id}")
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}
