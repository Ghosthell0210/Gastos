package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.CategoryOption
import com.example.data.Debt
import com.example.data.DebtDirection
import com.example.data.DebtPayment
import com.example.data.DebtStatus
import com.example.data.Expense
import com.example.ui.state.UiState
import com.example.ui.theme.PesoClaroThemeExtras

@Composable
fun MonthSelectorHeader(
    yearMonth: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("previous_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Mes anterior"
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = Formatters.formatYearMonthLabel(yearMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Mes siguiente"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditExpenseSheet(
    existingExpense: Expense?,
    categories: List<CategoryOption>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAddCustomCategoryClick: () -> Unit,
    onSave: (title: String, amount: Double, category: CategoryOption, dateMillis: Long, note: String?) -> Unit,
    onDelete: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(existingExpense) { mutableStateOf(existingExpense?.title ?: "") }
    var amountText by remember(existingExpense) {
        mutableStateOf(
            existingExpense?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var selectedCategoryId by remember(existingExpense, categories) {
        mutableStateOf(existingExpense?.categoryId ?: categories.firstOrNull()?.id ?: "food")
    }
    var selectedDateMillis by remember(existingExpense) {
        mutableStateOf(existingExpense?.expenseDateMillis ?: System.currentTimeMillis())
    }
    var note by remember(existingExpense) { mutableStateOf(existingExpense?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.first()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingExpense == null) "Nuevo gasto (CUP)" else "Editar gasto",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Eliminar gasto",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { raw ->
                    if (raw.isEmpty() || raw.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = raw
                        validationError = null
                    }
                },
                label = { Text("Monto en CUP") },
                prefix = { Text("$ ") },
                suffix = { Text("CUP") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_amount_input")
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 80) {
                        title = it
                        validationError = null
                    }
                },
                label = { Text("Descripción o comercio") },
                placeholder = { Text("Ej. Compra en el agro, transporte...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_title_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categoría",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onAddCustomCategoryClick) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nueva categoría")
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat.id == selectedCategory.id
                        val catColor = Color(cat.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryDefaults.iconForKey(cat.iconKey),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else catColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Fecha: ${Formatters.formatShortDate(selectedDateMillis)}")
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 300) note = it },
                label = { Text("Nota (opcional)") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            validationError?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    when {
                        amount == null || amount <= 0.0 -> validationError = "Ingresa un monto mayor a 0 CUP."
                        title.isBlank() -> validationError = "Escribe una descripción para el gasto."
                        else -> onSave(title.trim(), amount, selectedCategory, selectedDateMillis, note.trim().ifEmpty { null })
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_expense_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (existingExpense == null) "Guardar gasto" else "Actualizar gasto")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDebtSheet(
    existingDebt: Debt?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        direction: DebtDirection,
        counterparty: String,
        concept: String,
        totalAmount: Double,
        dueDateMillis: Long?,
        note: String?
    ) -> Unit,
    onDelete: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var direction by remember(existingDebt) {
        mutableStateOf(existingDebt?.directionEnum ?: DebtDirection.I_OWE)
    }
    var counterparty by remember(existingDebt) { mutableStateOf(existingDebt?.counterparty ?: "") }
    var concept by remember(existingDebt) { mutableStateOf(existingDebt?.concept ?: "") }
    var amountText by remember(existingDebt) {
        mutableStateOf(
            existingDebt?.totalAmount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var dueDateMillis by remember(existingDebt) { mutableStateOf(existingDebt?.dueDateMillis) }
    var note by remember(existingDebt) { mutableStateOf(existingDebt?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateMillis ?: (System.currentTimeMillis() + 7L * 86_400_000L)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingDebt == null) "Registrar deuda" else "Editar deuda",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Eliminar deuda",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DebtDirection.entries.forEachIndexed { index, dir ->
                    SegmentedButton(
                        selected = direction == dir,
                        onClick = { direction = dir },
                        shape = SegmentedButtonDefaults.itemShape(index, DebtDirection.entries.size),
                        modifier = Modifier.testTag("debt_dir_${dir.name.lowercase()}")
                    ) {
                        Text(dir.label)
                    }
                }
            }

            OutlinedTextField(
                value = counterparty,
                onValueChange = { if (it.length <= 60) { counterparty = it; validationError = null } },
                label = {
                    Text(if (direction == DebtDirection.OWED_TO_ME) "¿Quién te debe?" else "¿A quién le debes?")
                },
                placeholder = { Text("Nombre de persona o comercio") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_counterparty_input")
            )

            OutlinedTextField(
                value = concept,
                onValueChange = { if (it.length <= 100) { concept = it; validationError = null } },
                label = { Text("Concepto") },
                placeholder = { Text("Ej. Préstamo efectivo, reparación...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_concept_input")
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { raw ->
                    if (raw.isEmpty() || raw.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = raw
                        validationError = null
                    }
                },
                label = { Text("Monto total en CUP") },
                prefix = { Text("$ ") },
                suffix = { Text("CUP") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_amount_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dueDateMillis?.let { "Vence: ${Formatters.formatShortDate(it)}" }
                            ?: "Añadir fecha límite (opcional)"
                    )
                }
                if (dueDateMillis != null) {
                    IconButton(onClick = { dueDateMillis = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Quitar fecha límite")
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 300) note = it },
                label = { Text("Nota adicional (opcional)") },
                maxLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            validationError?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val total = amountText.toDoubleOrNull()
                    when {
                        counterparty.isBlank() -> validationError = "Ingresa el nombre de la persona o entidad."
                        concept.isBlank() -> validationError = "Ingresa el concepto de la deuda."
                        total == null || total <= 0.0 -> validationError = "Ingresa un monto mayor a 0 CUP."
                        existingDebt != null && total < existingDebt.paidAmount ->
                            validationError = "El monto total no puede ser menor a lo ya abonado (${Formatters.formatCup(existingDebt.paidAmount)})."
                        else -> onSave(
                            direction,
                            counterparty.trim(),
                            concept.trim(),
                            total,
                            dueDateMillis,
                            note.trim().ifEmpty { null }
                        )
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_debt_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (existingDebt == null) "Guardar deuda" else "Actualizar deuda")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailSheet(
    debt: Debt,
    paymentsState: UiState<List<DebtPayment>>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onEditDebt: () -> Unit,
    onScheduleReminder: () -> Unit,
    onAddPayment: (amount: Double, note: String?) -> Unit,
    onSettleDebt: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantic = PesoClaroThemeExtras.semanticColors
    var paymentText by remember(debt.id, debt.remainingAmount) { mutableStateOf("") }
    var paymentNote by remember(debt.id) { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val isOwedToMe = debt.directionEnum == DebtDirection.OWED_TO_ME
    val accentColor = if (isOwedToMe) semantic.owedToMe else semantic.iOwe

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = debt.directionEnum.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = debt.counterparty,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = debt.concept,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onScheduleReminder) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Programar recordatorio de deuda",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onEditDebt) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar deuda")
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Saldo pendiente", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.formatCup(debt.remainingAmount),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total original", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.formatCup(debt.totalAmount),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { debt.progressRatio },
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surface,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Abonado: ${Formatters.formatCup(debt.paidAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        debt.dueDateMillis?.let { due ->
                            Text(
                                text = "Vence: ${Formatters.formatShortDate(due)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (due < System.currentTimeMillis() && debt.statusEnum == DebtStatus.PENDING) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            if (debt.statusEnum == DebtStatus.PENDING && debt.remainingAmount > 0.0) {
                Text("Registrar abono", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = paymentText,
                        onValueChange = { raw ->
                            if (raw.isEmpty() || raw.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                paymentText = raw
                                errorMsg = null
                            }
                        },
                        label = { Text("Monto abono (CUP)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("debt_payment_amount_input")
                    )

                    Button(
                        onClick = {
                            val amt = paymentText.toDoubleOrNull()
                            when {
                                amt == null || amt <= 0.0 -> errorMsg = "Ingresa un abono válido."
                                amt > debt.remainingAmount + 0.01 ->
                                    errorMsg = "El abono supera el saldo (${Formatters.formatCup(debt.remainingAmount)})."
                                else -> {
                                    onAddPayment(amt, paymentNote.trim().ifEmpty { null })
                                    paymentText = ""
                                    paymentNote = ""
                                }
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("add_payment_button")
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Abonar")
                    }
                }

                OutlinedTextField(
                    value = paymentNote,
                    onValueChange = { if (it.length <= 200) paymentNote = it },
                    label = { Text("Nota del abono (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedButton(
                    onClick = onSettleDebt,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settle_debt_button")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Saldar deuda completa (${Formatters.formatCup(debt.remainingAmount)})")
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = semantic.owedToMeContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = semantic.owedToMe)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Esta deuda está completamente saldada.",
                            style = MaterialTheme.typography.titleSmall,
                            color = semantic.owedToMe
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Historial de abonos", style = MaterialTheme.typography.titleMedium)

            when (paymentsState) {
                is UiState.Loading -> CircularProgressIndicator(Modifier.size(24.dp))
                is UiState.Error -> Text(paymentsState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (paymentsState.data.isEmpty()) {
                        Text(
                            text = "Aún no hay abonos registrados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            paymentsState.data.forEach { payment ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = Formatters.formatCup(payment.amount),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (!payment.note.isNullOrBlank()) {
                                            Text(
                                                text = payment.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = Formatters.formatShortDate(payment.paymentDateMillis),
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
    }
}
