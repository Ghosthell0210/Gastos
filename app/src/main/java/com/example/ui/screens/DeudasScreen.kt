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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Debt
import com.example.data.DebtDirection
import com.example.data.DebtStatus
import com.example.ui.components.Formatters
import com.example.ui.state.FinanceDashboardData
import com.example.ui.theme.PesoClaroThemeExtras

private enum class DebtFilterTab(val label: String) {
    ALL("Todas"),
    OWED_TO_ME("Me deben"),
    I_OWE("Debo")
}

@Composable
fun DeudasScreen(
    data: FinanceDashboardData,
    onAddDebt: () -> Unit,
    onOpenDebtDetail: (Debt) -> Unit,
    onSettleDebt: (Debt) -> Unit,
    onScheduleDebtReminder: (Debt) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = PesoClaroThemeExtras.semanticColors
    var activeDirectionTab by remember { mutableStateOf(DebtFilterTab.ALL) }
    var showSettled by remember { mutableStateOf(false) }

    val filteredDebts = remember(data.allDebts, activeDirectionTab, showSettled) {
        data.allDebts.filter { debt ->
            val dirMatch = when (activeDirectionTab) {
                DebtFilterTab.ALL -> true
                DebtFilterTab.OWED_TO_ME -> debt.directionEnum == DebtDirection.OWED_TO_ME
                DebtFilterTab.I_OWE -> debt.directionEnum == DebtDirection.I_OWE
            }
            val statusMatch = if (showSettled) {
                debt.statusEnum == DebtStatus.SETTLED
            } else {
                debt.statusEnum == DebtStatus.PENDING
            }
            dirMatch && statusMatch
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bi-directional debt summary cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = semantic.owedToMeContainer.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "POR COBRAR (ME DEBEN)",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.owedToMe
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatCup(data.totalOwedToMePending),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = semantic.owedToMe
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = semantic.iOweContainer.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "POR PAGAR (DEBO)",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.iOwe
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatCup(data.totalIOwePending),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = semantic.iOwe
                            )
                        }
                    }
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DebtFilterTab.entries.forEachIndexed { index, tab ->
                        SegmentedButton(
                            selected = activeDirectionTab == tab,
                            onClick = { activeDirectionTab = tab },
                            shape = SegmentedButtonDefaults.itemShape(index, DebtFilterTab.entries.size),
                            modifier = Modifier.testTag("debt_tab_${tab.name.lowercase()}")
                        ) {
                            Text(tab.label)
                        }
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showSettled,
                        onClick = { showSettled = false },
                        label = { Text("Pendientes") }
                    )
                    FilterChip(
                        selected = showSettled,
                        onClick = { showSettled = true },
                        label = { Text("Saldadas") }
                    )
                    if (data.overdueDebtsCount > 0 && !showSettled) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "${data.overdueDebtsCount} vencida(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            if (filteredDebts.isEmpty()) {
                item {
                    EmptyFinanceCard(
                        icon = Icons.Rounded.AccountBalance,
                        title = if (showSettled) "Sin deudas saldadas" else "No hay deudas pendientes",
                        subtitle = if (showSettled) {
                            "Cuando saldes una deuda aparecerá aquí tu historial."
                        } else {
                            "Registra tanto lo que debes como lo que te deben en CUP y lleva el control de abonos."
                        },
                        actionLabel = if (!showSettled) "Nueva deuda" else null,
                        onAction = if (!showSettled) onAddDebt else null
                    )
                }
            } else {
                items(filteredDebts, key = { it.id }) { debt ->
                    DebtCard(
                        debt = debt,
                        onClick = { onOpenDebtDetail(debt) },
                        onSettle = { onSettleDebt(debt) },
                        onScheduleReminder = { onScheduleDebtReminder(debt) }
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun DebtCard(
    debt: Debt,
    onClick: () -> Unit,
    onSettle: () -> Unit,
    onScheduleReminder: () -> Unit
) {
    val semantic = PesoClaroThemeExtras.semanticColors
    val isOwedToMe = debt.directionEnum == DebtDirection.OWED_TO_ME
    val accent = if (isOwedToMe) semantic.owedToMe else semantic.iOwe
    val isPending = debt.statusEnum == DebtStatus.PENDING
    val isOverdue = isPending && debt.dueDateMillis != null && debt.dueDateMillis < System.currentTimeMillis()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = accent.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = debt.directionEnum.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (isOverdue) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "Vencida",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = debt.counterparty,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = debt.concept,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isPending) Formatters.formatCup(debt.remainingAmount) else Formatters.formatCup(debt.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Text(
                        text = if (isPending) "de ${Formatters.formatCup(debt.totalAmount)}" else "Saldada",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { debt.progressRatio },
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    debt.dueDateMillis?.let { due ->
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = Formatters.formatShortDate(due),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } ?: Text(
                        text = "Sin fecha límite",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isPending) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onScheduleReminder,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "Programar alerta",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = onClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Abonar", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onSettle,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Saldar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
