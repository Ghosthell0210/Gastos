package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BudgetAlertLevel
import com.example.data.Expense
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.CategoryDefaults
import com.example.ui.components.Formatters
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.state.FinanceDashboardData
import com.example.ui.theme.PesoClaroThemeExtras

@Composable
fun GastosScreen(
    data: FinanceDashboardData,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onEditBudgetForCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val budgetMap = remember(data.monthBudgets) {
        data.monthBudgets.associateBy { it.budget.categoryId }
    }

    val filteredExpenses = remember(data.monthExpenses, searchQuery, selectedCategoryId) {
        data.monthExpenses.filter { exp ->
            val matchesCat = selectedCategoryId == null || exp.categoryId == selectedCategoryId
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                exp.title.contains(q, ignoreCase = true) ||
                exp.categoryName.contains(q, ignoreCase = true) ||
                (exp.note?.contains(q, ignoreCase = true) == true)
            matchesCat && matchesQuery
        }
    }

    val filteredTotal = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }
    val selectedBudgetProgress = selectedCategoryId?.let { budgetMap[it] }
    val semantic = PesoClaroThemeExtras.semanticColors

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MonthSelectorHeader(
                    yearMonth = data.selectedYearMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por descripción, categoría o nota...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expenses_search_input")
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Todas") }
                        )
                    }
                    items(data.categories, key = { it.id }) { cat ->
                        val budgetProg = budgetMap[cat.id]
                        val hasAlert = budgetProg != null && budgetProg.alertLevel != BudgetAlertLevel.SAFE
                        val alertTint = if (budgetProg?.alertLevel == BudgetAlertLevel.EXCEEDED_100) {
                            semantic.budgetExceeded
                        } else {
                            semantic.budgetWarning
                        }
                        val catColor = Color(cat.colorHex)
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                            },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryDefaults.iconForKey(cat.iconKey),
                                    contentDescription = null,
                                    tint = if (selectedCategoryId == cat.id) Color.White else catColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = if (hasAlert) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = "Alerta de presupuesto",
                                        tint = if (selectedCategoryId == cat.id) Color.White else alertTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (selectedBudgetProgress != null) {
                item {
                    BudgetProgressCard(
                        progress = selectedBudgetProgress,
                        categories = data.categories,
                        onClick = { onEditBudgetForCategory(selectedBudgetProgress.budget.categoryId) }
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredExpenses.size} movimientos",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total: ${Formatters.formatCup(filteredTotal)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                item {
                    EmptyFinanceCard(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = "No hay gastos que coincidan",
                        subtitle = if (data.monthExpenses.isEmpty()) {
                            "Registra tus gastos del mes en CUP para categorizarlos y analizarlos."
                        } else {
                            "Prueba cambiando el filtro de categoría o limpiando el buscador."
                        },
                        actionLabel = if (data.monthExpenses.isEmpty()) "Agregar gasto" else null,
                        onAction = if (data.monthExpenses.isEmpty()) onAddExpense else null
                    )
                }
            } else {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseRowCard(
                        expense = expense,
                        categories = data.categories,
                        onClick = { onEditExpense(expense) }
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}
