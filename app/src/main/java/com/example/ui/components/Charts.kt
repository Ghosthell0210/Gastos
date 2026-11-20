package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BudgetAlertLevel
import com.example.data.BudgetProgress
import com.example.data.CategoryOption
import com.example.ui.state.CategoryBreakdown
import com.example.ui.theme.PesoClaroThemeExtras
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun InteractiveDonutChart(
    breakdowns: List<CategoryBreakdown>,
    totalSpent: Double,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "donut_anim"
    )

    val selected = breakdowns.firstOrNull { it.categoryId == selectedCategoryId }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(210.dp)
                .pointerInput(breakdowns) {
                    detectTapGestures { tapOffset ->
                        if (breakdowns.isEmpty() || totalSpent <= 0.0) return@detectTapGestures
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = tapOffset.x - cx
                        val dy = tapOffset.y - cy
                        val dist = hypot(dx, dy)
                        val outerR = minOf(size.width, size.height) / 2f
                        val innerR = outerR * 0.45f
                        if (dist in innerR..outerR) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                            if (angle < 0f) angle += 360f
                            var start = 0f
                            var tappedId: String? = null
                            for (slice in breakdowns) {
                                val sweep = (slice.amount / totalSpent).toFloat() * 360f
                                if (angle >= start && angle <= start + sweep) {
                                    tappedId = slice.categoryId
                                    break
                                }
                                start += sweep
                            }
                            onSelectCategory(if (tappedId == selectedCategoryId) null else tappedId)
                        } else {
                            onSelectCategory(null)
                        }
                    }
                }
        ) {
            val strokeWidth = 34.dp.toPx()
            val diameter = minOf(size.width, size.height) - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            if (breakdowns.isEmpty() || totalSpent <= 0.0) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                var startAngle = -90f
                val gapDegrees = if (breakdowns.size > 1) 2.5f else 0f
                breakdowns.forEach { item ->
                    val rawSweep = ((item.amount / totalSpent).toFloat() * 360f) * animationProgress
                    val sweep = (rawSweep - gapDegrees).coerceAtLeast(1.5f)
                    val isSelected = selectedCategoryId == null || selectedCategoryId == item.categoryId
                    val sliceWidth = if (selectedCategoryId == item.categoryId) strokeWidth * 1.16f else strokeWidth
                    drawArc(
                        color = if (isSelected) item.color else item.color.copy(alpha = 0.28f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = sliceWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += rawSweep
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            Text(
                text = selected?.categoryName ?: "Total del mes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = Formatters.formatCup(selected?.amount ?: totalSpent, includeCode = false),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (selected != null) "${selected.percentage.toInt()}% · CUP" else "CUP",
                style = MaterialTheme.typography.labelSmall,
                color = selected?.color ?: MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DailySpendingBarChart(
    dailySpending: Map<Int, Double>,
    daysInMonth: Int,
    dailyAverage: Double,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember(dailySpending, daysInMonth) { mutableStateOf<Int?>(null) }
    val maxAmount = (dailySpending.values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val avgColor = PesoClaroThemeExtras.semanticColors.iOwe

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDay?.let { day ->
                    val amount = dailySpending[day] ?: 0.0
                    "Día $day: ${Formatters.formatCup(amount)}"
                } ?: "Toca una barra para ver el gasto del día",
                style = MaterialTheme.typography.bodySmall,
                color = if (selectedDay != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selectedDay != null) FontWeight.SemiBold else FontWeight.Normal
            )
            if (dailyAverage > 0.0) {
                Text(
                    text = "Prom: ${Formatters.formatCup(dailyAverage, includeCode = false)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = avgColor
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .pointerInput(daysInMonth, dailySpending) {
                    detectTapGestures { offset ->
                        if (daysInMonth <= 0) return@detectTapGestures
                        val slotWidth = size.width / daysInMonth.toFloat()
                        val tapped = ((offset.x / slotWidth).toInt() + 1).coerceIn(1, daysInMonth)
                        selectedDay = if (selectedDay == tapped) null else tapped
                    }
                }
        ) {
            val count = daysInMonth.coerceAtLeast(1)
            val slotWidth = size.width / count.toFloat()
            val barWidth = (slotWidth * 0.58f).coerceAtLeast(3f)
            val chartHeight = size.height - 8.dp.toPx()

            for (day in 1..count) {
                val amount = dailySpending[day] ?: 0.0
                val left = (day - 1) * slotWidth + (slotWidth - barWidth) / 2f
                // Subtle baseline track
                drawRoundRect(
                    color = trackColor.copy(alpha = 0.5f),
                    topLeft = Offset(left, 4.dp.toPx()),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                if (amount > 0.0) {
                    val ratio = (amount / maxAmount).toFloat().coerceIn(0.06f, 1f)
                    val barHeight = chartHeight * ratio
                    val top = 4.dp.toPx() + (chartHeight - barHeight)
                    val isSelected = selectedDay == null || selectedDay == day
                    drawRoundRect(
                        color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.3f),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            if (dailyAverage > 0.0 && maxAmount > 0.0) {
                val avgRatio = (dailyAverage / maxAmount).toFloat().coerceIn(0f, 1f)
                val avgY = 4.dp.toPx() + chartHeight * (1f - avgRatio)
                drawLine(
                    color = avgColor.copy(alpha = 0.85f),
                    start = Offset(0f, avgY),
                    end = Offset(size.width, avgY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("1", "10", "20", daysInMonth.toString()).forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BudgetProgressCard(
    progress: BudgetProgress,
    categories: List<CategoryOption>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = PesoClaroThemeExtras.semanticColors
    val catColor = CategoryDefaults.findCategoryColor(progress.budget.categoryId, categories)
    val catIcon = CategoryDefaults.findCategoryIcon(progress.budget.categoryId, categories)

    val (barColor, badgeBg, badgeTextColor, badgeLabel) = when (progress.alertLevel) {
        BudgetAlertLevel.EXCEEDED_100 -> QuadColorLabel(
            semantic.budgetExceeded,
            semantic.budgetExceededContainer,
            semantic.budgetExceeded,
            "Superado (${progress.percentage}%)"
        )
        BudgetAlertLevel.WARNING_75 -> QuadColorLabel(
            semantic.budgetWarning,
            semantic.budgetWarningContainer,
            semantic.budgetWarning,
            "Alerta >=75% (${progress.percentage}%)"
        )
        BudgetAlertLevel.SAFE -> QuadColorLabel(
            semantic.budgetSafe,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            "${progress.percentage}% usado"
        )
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = catIcon,
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = progress.budget.categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${Formatters.formatCup(progress.spentAmount)} / ${Formatters.formatCup(progress.budget.limitAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        if (progress.alertLevel != BudgetAlertLevel.SAFE) {
                            Icon(
                                imageVector = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED_100) {
                                    Icons.Rounded.ErrorOutline
                                } else {
                                    Icons.Rounded.WarningAmber
                                },
                                contentDescription = null,
                                tint = badgeTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress.ratio.coerceIn(0f, 1f) },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
        }
    }
}

private data class QuadColorLabel(
    val bar: Color,
    val bg: Color,
    val text: Color,
    val label: String
)
