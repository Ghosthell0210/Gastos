package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Budget
import com.example.data.CategoryOption
import com.example.data.Debt
import com.example.data.DebtStatus
import com.example.data.Reminder
import com.example.data.ReminderFrequency
import com.example.data.ReminderType
import com.example.notifications.ReminderNotificationHelper
import com.example.ui.theme.PesoClaroThemeExtras
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetEditorSheet(
    existingBudget: Budget?,
    categories: List<CategoryOption>,
    yearMonth: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (category: CategoryOption, limitAmount: Double) -> Unit,
    onDelete: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantic = PesoClaroThemeExtras.semanticColors
    var selectedCategoryId by remember(existingBudget, categories) {
        mutableStateOf(existingBudget?.categoryId ?: categories.firstOrNull()?.id ?: "food")
    }
    var limitText by remember(existingBudget) {
        mutableStateOf(
            existingBudget?.limitAmount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.first()
    val parsedLimit = limitText.toDoubleOrNull() ?: 0.0

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
                Column {
                    Text(
                        text = if (existingBudget == null) "Nuevo presupuesto" else "Editar presupuesto",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = Formatters.formatYearMonthLabel(yearMonth),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Eliminar presupuesto",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (existingBudget == null) {
                Text("Categoría", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = cat.id == selectedCategory.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryDefaults.iconForKey(cat.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Categoría: ${existingBudget.categoryName}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = limitText,
                onValueChange = { raw ->
                    if (raw.isEmpty() || raw.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        limitText = raw
                        errorMsg = null
                    }
                },
                label = { Text("Límite mensual máximo (CUP)") },
                prefix = { Text("$ ") },
                suffix = { Text("CUP") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_limit_input")
            )

            if (parsedLimit > 0.0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Umbrales de alerta automáticos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Advertencia 75%", style = MaterialTheme.typography.bodySmall, color = semantic.budgetWarning)
                            Text(
                                text = Formatters.formatCup(parsedLimit * 0.75),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = semantic.budgetWarning
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Límite 100%", style = MaterialTheme.typography.bodySmall, color = semantic.budgetExceeded)
                            Text(
                                text = Formatters.formatCup(parsedLimit),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = semantic.budgetExceeded
                            )
                        }
                    }
                }
            }

            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull()
                    if (limit == null || limit <= 0.0) {
                        errorMsg = "Ingresa un monto máximo mayor a 0 CUP."
                    } else {
                        onSave(selectedCategory, limit)
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_budget_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar presupuesto")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorSheet(
    existingReminder: Reminder?,
    preselectedDebt: Debt?,
    debts: List<Debt>,
    categories: List<CategoryOption>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        type: ReminderType,
        targetId: String?,
        targetName: String,
        frequency: ReminderFrequency,
        scheduledAtMillis: Long,
        enabled: Boolean,
        note: String?
    ) -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hasNotifPermission by remember {
        mutableStateOf(ReminderNotificationHelper.hasNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    val initialType = when {
        preselectedDebt != null -> ReminderType.DEBT_DUE
        existingReminder != null -> existingReminder.typeEnum
        else -> ReminderType.EXPENSE_REVIEW
    }
    var reminderType by remember(existingReminder, preselectedDebt) { mutableStateOf(initialType) }

    val pendingDebts = remember(debts) { debts.filter { it.statusEnum == DebtStatus.PENDING } }

    var selectedTargetId by remember(existingReminder, preselectedDebt) {
        mutableStateOf(
            preselectedDebt?.id
                ?: existingReminder?.targetId
                ?: if (initialType == ReminderType.DEBT_DUE) pendingDebts.firstOrNull()?.id else categories.firstOrNull()?.id
        )
    }
    var customTargetName by remember(existingReminder, preselectedDebt) {
        mutableStateOf(
            preselectedDebt?.let { "${it.counterparty} (${it.concept})" }
                ?: existingReminder?.targetName
                ?: if (initialType == ReminderType.EXPENSE_REVIEW) {
                    categories.firstOrNull()?.name ?: "Comida"
                } else {
                    pendingDebts.firstOrNull()?.let { "${it.counterparty} (${it.concept})" } ?: ""
                }
        )
    }
    var title by remember(existingReminder, preselectedDebt) {
        mutableStateOf(
            preselectedDebt?.let { "Vencimiento: ${it.counterparty}" }
                ?: existingReminder?.title
                ?: "Revisar gastos"
        )
    }
    var frequency by remember(existingReminder, preselectedDebt) {
        mutableStateOf(
            preselectedDebt?.let { ReminderFrequency.ONCE }
                ?: existingReminder?.frequencyEnum
                ?: ReminderFrequency.WEEKLY
        )
    }
    var scheduledMillis by remember(existingReminder, preselectedDebt) {
        mutableStateOf(
            preselectedDebt?.dueDateMillis
                ?: existingReminder?.scheduledAtMillis
                ?: (System.currentTimeMillis() + 86_400_000L)
        )
    }
    var enabled by remember(existingReminder) { mutableStateOf(existingReminder?.enabled ?: true) }
    var note by remember(existingReminder) { mutableStateOf(existingReminder?.note ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = scheduledMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { pickedDate ->
                        val oldCal = Calendar.getInstance().apply { timeInMillis = scheduledMillis }
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = pickedDate
                            set(Calendar.HOUR_OF_DAY, oldCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, oldCal.get(Calendar.MINUTE))
                        }
                        scheduledMillis = newCal.timeInMillis
                    }
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

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = scheduledMillis }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Hora del recordatorio") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val updated = Calendar.getInstance().apply {
                        timeInMillis = scheduledMillis
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                    }
                    scheduledMillis = updated.timeInMillis
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            }
        )
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
                    text = if (existingReminder == null) "Nuevo recordatorio" else "Editar recordatorio",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Eliminar recordatorio",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = PesoClaroThemeExtras.semanticColors.budgetWarningContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = PesoClaroThemeExtras.semanticColors.budgetWarning
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Permite las notificaciones para recibir tus alertas a tiempo.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) {
                            Text("Permitir")
                        }
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ReminderType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = reminderType == type,
                        onClick = {
                            reminderType = type
                            if (type == ReminderType.DEBT_DUE) {
                                val firstDebt = pendingDebts.firstOrNull()
                                selectedTargetId = firstDebt?.id
                                customTargetName = firstDebt?.let { "${it.counterparty} (${it.concept})" } ?: ""
                                if (title == "Revisar gastos" || title.isBlank()) {
                                    title = firstDebt?.let { "Vencimiento: ${it.counterparty}" } ?: "Pago de deuda"
                                }
                                frequency = ReminderFrequency.ONCE
                            } else {
                                val firstCat = categories.firstOrNull()
                                selectedTargetId = firstCat?.id
                                customTargetName = firstCat?.name ?: "Comida"
                                title = "Revisar gastos de ${customTargetName}"
                                frequency = ReminderFrequency.WEEKLY
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, ReminderType.entries.size)
                    ) {
                        Text(if (type == ReminderType.DEBT_DUE) "Deuda" else "Gastos")
                    }
                }
            }

            if (reminderType == ReminderType.DEBT_DUE && pendingDebts.isNotEmpty()) {
                Text("Seleccionar deuda pendiente", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pendingDebts.forEach { d ->
                        FilterChip(
                            selected = selectedTargetId == d.id,
                            onClick = {
                                selectedTargetId = d.id
                                customTargetName = "${d.counterparty} (${d.concept})"
                                title = "Vencimiento: ${d.counterparty}"
                                d.dueDateMillis?.let { scheduledMillis = it }
                            },
                            label = { Text("${d.counterparty} · ${Formatters.formatCup(d.remainingAmount, false)}") }
                        )
                    }
                }
            } else if (reminderType == ReminderType.EXPENSE_REVIEW) {
                Text("Categoría de gasto a revisar", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedTargetId == cat.id,
                            onClick = {
                                selectedTargetId = cat.id
                                customTargetName = cat.name
                                title = "Revisar gastos de ${cat.name}"
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = customTargetName,
                onValueChange = { if (it.length <= 60) { customTargetName = it; errorMsg = null } },
                label = {
                    Text(if (reminderType == ReminderType.DEBT_DUE) "Nombre de la deuda (visible en alerta)" else "Categoría (visible en alerta)")
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_target_input")
            )

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) { title = it; errorMsg = null } },
                label = { Text("Título de la notificación") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_title_input")
            )

            Text("Frecuencia", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ReminderFrequency.entries.forEachIndexed { index, freq ->
                    SegmentedButton(
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        shape = SegmentedButtonDefaults.itemShape(index, ReminderFrequency.entries.size)
                    ) {
                        Text(freq.label)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(Formatters.formatShortDate(scheduledMillis))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    val cal = Calendar.getInstance().apply { timeInMillis = scheduledMillis }
                    Text(String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 200) note = it },
                label = { Text("Mensaje adicional (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recordatorio activo", style = MaterialTheme.typography.titleSmall)
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    when {
                        customTargetName.isBlank() -> errorMsg = "Indica el nombre de la deuda o categoría."
                        title.isBlank() -> errorMsg = "Ingresa el título de la notificación."
                        else -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            onSave(
                                title.trim(),
                                reminderType,
                                selectedTargetId,
                                customTargetName.trim(),
                                frequency,
                                scheduledMillis,
                                enabled,
                                note.trim().ifEmpty { null }
                            )
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_reminder_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar recordatorio")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomCategoryDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, iconKey: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf("category") }
    var selectedColorHex by remember { mutableStateOf(CategoryDefaults.selectableColorHexes.first()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) { name = it; errorMsg = null } },
                    label = { Text("Nombre de la categoría") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Color", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryDefaults.selectableColorHexes.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Text("Ícono", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryDefaults.selectableIcons.forEach { (key, icon) ->
                        val isSelected = selectedIconKey == key
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedIconKey = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = key,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) errorMsg = "Ingresa un nombre."
                    else onSave(name.trim(), selectedIconKey, selectedColorHex)
                },
                enabled = !isSaving
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsSheet(
    user: FirebaseUser?,
    onDismiss: () -> Unit,
    onOpenReminders: () -> Unit,
    onSignOut: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Cuenta y respaldo", style = MaterialTheme.typography.headlineSmall)

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.displayName ?: "Usuario de Google",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = user?.email ?: "Sesión activa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Sincronización en la nube activa",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Moneda principal: CUP (Peso Cubano)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenReminders()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Gestionar notificaciones y recordatorios")
            }

            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }
}
