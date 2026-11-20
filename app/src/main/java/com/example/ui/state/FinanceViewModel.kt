package com.example.ui.state

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Budget
import com.example.data.BudgetAlertLevel
import com.example.data.BudgetProgress
import com.example.data.CategoryOption
import com.example.data.CustomCategory
import com.example.data.Debt
import com.example.data.DebtDirection
import com.example.data.DebtPayment
import com.example.data.DebtStatus
import com.example.data.Expense
import com.example.data.FinanceRepository
import com.example.data.Reminder
import com.example.data.ReminderFrequency
import com.example.data.ReminderType
import com.example.notifications.ReminderScheduler
import com.example.ui.components.CategoryDefaults
import com.example.ui.components.Formatters
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CategoryBreakdown(
    val categoryId: String,
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val color: Color,
    val budgetLimit: Double? = null,
    val budgetAlertLevel: BudgetAlertLevel? = null
)

data class FinanceDashboardData(
    val selectedYearMonth: String,
    val categories: List<CategoryOption>,
    val allExpenses: List<Expense>,
    val monthExpenses: List<Expense>,
    val monthTotalSpent: Double,
    val previousMonthTotalSpent: Double,
    val monthDeltaPercent: Double?,
    val dailyAverage: Double,
    val highestDayNumber: Int?,
    val highestDayAmount: Double,
    val categoryBreakdowns: List<CategoryBreakdown>,
    val dailySpending: Map<Int, Double>,
    val daysInMonth: Int,
    val allDebts: List<Debt>,
    val totalOwedToMePending: Double,
    val totalIOwePending: Double,
    val netDebtBalance: Double,
    val overdueDebtsCount: Int,
    val monthDebtsCreatedCount: Int,
    val monthDebtsTotalAmount: Double,
    val monthBudgets: List<BudgetProgress>,
    val alertingBudgets: List<BudgetProgress>,
    val reminders: List<Reminder>
)

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val currentUserId: String,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _selectedYearMonth = MutableStateFlow(Formatters.currentYearMonth())
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val expensesFlow: Flow<List<Expense>> = repository.observeExpenses(currentUserId)
    private val debtsFlow: Flow<List<Debt>> = repository.observeDebts(currentUserId)
    private val budgetsFlow: Flow<List<Budget>> = repository.observeBudgets(currentUserId)
    private val remindersFlow: Flow<List<Reminder>> = repository.observeReminders(currentUserId)
        .onEach { list -> reminderScheduler.syncReminders(list) }
    private val categoriesFlow: Flow<List<CustomCategory>> = repository.observeCustomCategories(currentUserId)

    private data class CoreStreams(
        val expenses: List<Expense>,
        val debts: List<Debt>,
        val budgets: List<Budget>,
        val reminders: List<Reminder>,
        val customCategories: List<CustomCategory>
    )

    private val combinedStreams: Flow<CoreStreams> = combine(
        expensesFlow,
        debtsFlow,
        budgetsFlow,
        remindersFlow,
        categoriesFlow
    ) { expenses, debts, budgets, reminders, customCategories ->
        CoreStreams(expenses, debts, budgets, reminders, customCategories)
    }

    val dashboardState: StateFlow<UiState<FinanceDashboardData>> = combine(
        combinedStreams,
        _selectedYearMonth
    ) { streams, activeMonth ->
        buildDashboardData(streams, activeMonth)
    }
        .map<FinanceDashboardData, UiState<FinanceDashboardData>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing finance data", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "No se pudieron cargar tus datos."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun previousMonth() {
        _selectedYearMonth.value = Formatters.shiftYearMonth(_selectedYearMonth.value, -1)
    }

    fun nextMonth() {
        _selectedYearMonth.value = Formatters.shiftYearMonth(_selectedYearMonth.value, 1)
    }

    fun selectYearMonth(yearMonth: String) {
        _selectedYearMonth.value = yearMonth
    }

    fun observeDebtPayments(debtId: String): Flow<UiState<List<DebtPayment>>> =
        repository.observeDebtPayments(currentUserId, debtId)
            .map<List<DebtPayment>, UiState<List<DebtPayment>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(UiState.Error(rootError.localizedMessage ?: "No se pudo cargar el historial de abonos."))
            }

    // --- Actions ---

    fun saveExpense(
        existingId: String?,
        title: String,
        amount: Double,
        category: CategoryOption,
        expenseDateMillis: Long,
        note: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val ym = Formatters.yearMonthFromMillis(expenseDateMillis)
            val result = if (existingId.isNullOrBlank()) {
                repository.addExpense(
                    title = title,
                    amount = amount,
                    categoryId = category.id,
                    categoryName = category.name,
                    expenseDateMillis = expenseDateMillis,
                    yearMonth = ym,
                    note = note
                )
            } else {
                repository.updateExpense(
                    expenseId = existingId,
                    title = title,
                    amount = amount,
                    categoryId = category.id,
                    categoryName = category.name,
                    expenseDateMillis = expenseDateMillis,
                    yearMonth = ym,
                    note = note
                )
            }
            _isSaving.value = false
            result.onSuccess {
                _events.tryEmit(if (existingId == null) "Gasto guardado" else "Gasto actualizado")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Error al guardar el gasto")
            }
        }
    }

    fun deleteExpense(expenseId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.deleteExpense(expenseId)
            _isSaving.value = false
            result.onSuccess {
                _events.tryEmit("Gasto eliminado")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "No se pudo eliminar el gasto")
            }
        }
    }

    fun saveDebt(
        existingDebt: Debt?,
        direction: DebtDirection,
        counterparty: String,
        concept: String,
        totalAmount: Double,
        dueDateMillis: Long?,
        note: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val ym = dueDateMillis?.let { Formatters.yearMonthFromMillis(it) }
                ?: existingDebt?.yearMonth?.takeIf { it.isNotBlank() }
                ?: _selectedYearMonth.value
            val result = if (existingDebt == null) {
                repository.addDebt(
                    direction = direction,
                    counterparty = counterparty,
                    concept = concept,
                    totalAmount = totalAmount,
                    dueDateMillis = dueDateMillis,
                    yearMonth = ym,
                    note = note
                ).map { Unit }
            } else {
                repository.updateDebt(
                    debt = existingDebt,
                    direction = direction,
                    counterparty = counterparty,
                    concept = concept,
                    totalAmount = totalAmount,
                    dueDateMillis = dueDateMillis,
                    yearMonth = ym,
                    note = note
                )
            }
            _isSaving.value = false
            result.onSuccess {
                _events.tryEmit(if (existingDebt == null) "Deuda registrada" else "Deuda actualizada")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Error al guardar la deuda")
            }
        }
    }

    fun recordDebtPayment(
        debt: Debt,
        amount: Double,
        note: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.addDebtPayment(
                debt = debt,
                paymentAmount = amount,
                paymentDateMillis = System.currentTimeMillis(),
                note = note
            )
            _isSaving.value = false
            result.onSuccess {
                _events.tryEmit("Abono registrado correctamente")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Error al registrar el abono")
            }
        }
    }

    fun settleDebt(debt: Debt) {
        val remaining = debt.remainingAmount
        if (remaining <= 0.0) return
        recordDebtPayment(
            debt = debt,
            amount = remaining,
            note = "Saldo completo"
        )
    }

    fun deleteDebt(debtId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.deleteDebt(debtId)
            _isSaving.value = false
            result.onSuccess {
                _events.tryEmit("Deuda eliminada")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "No se pudo eliminar la deuda")
            }
        }
    }

    fun saveBudget(
        category: CategoryOption,
        yearMonth: String,
        limitAmount: Double,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.upsertBudget(
                categoryId = category.id,
                categoryName = category.name,
                yearMonth = yearMonth,
                limitAmount = limitAmount
            )
            _isSaving.value = false
            result.onSuccess {
                _events.tryEmit("Presupuesto mensual guardado")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Error al guardar presupuesto")
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val result = repository.deleteBudget(budgetId)
            result.onSuccess {
                _events.tryEmit("Presupuesto eliminado")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Error al eliminar presupuesto")
            }
        }
    }

    fun saveReminder(
        existingId: String?,
        title: String,
        reminderType: ReminderType,
        targetId: String?,
        targetName: String,
        frequency: ReminderFrequency,
        scheduledAtMillis: Long,
        enabled: Boolean,
        note: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveReminder(
                existingId = existingId,
                title = title,
                reminderType = reminderType,
                targetId = targetId,
                targetName = targetName,
                frequency = frequency,
                scheduledAtMillis = scheduledAtMillis,
                enabled = enabled,
                note = note
            )
            _isSaving.value = false
            result.onSuccess { id ->
                val reminder = Reminder(
                    id = id,
                    userId = currentUserId,
                    title = title,
                    reminderType = reminderType.name,
                    targetId = targetId,
                    targetName = targetName,
                    frequency = frequency.name,
                    scheduledAtMillis = scheduledAtMillis,
                    enabled = enabled,
                    note = note
                )
                if (enabled) {
                    reminderScheduler.schedule(reminder)
                } else {
                    reminderScheduler.cancel(id)
                }
                _events.tryEmit("Recordatorio programado")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Error al guardar el recordatorio")
            }
        }
    }

    fun toggleReminder(reminder: Reminder, enabled: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleReminderEnabled(reminder.id, enabled)
            result.onSuccess {
                if (enabled) {
                    reminderScheduler.schedule(reminder.copy(enabled = true))
                    _events.tryEmit("Recordatorio activado")
                } else {
                    reminderScheduler.cancel(reminder.id)
                    _events.tryEmit("Recordatorio pausado")
                }
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "No se pudo actualizar el recordatorio")
            }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            val result = repository.deleteReminder(reminderId)
            result.onSuccess {
                reminderScheduler.cancel(reminderId)
                _events.tryEmit("Recordatorio eliminado")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "No se pudo eliminar el recordatorio")
            }
        }
    }

    fun previewReminderNotification(reminder: Reminder) {
        reminderScheduler.previewNotificationNow(reminder)
        _events.tryEmit("Notificación de prueba enviada")
    }

    fun addCustomCategory(
        name: String,
        iconKey: String,
        colorHex: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.addCustomCategory(name, iconKey, colorHex)
            _isSaving.value = false
            result.onSuccess {
                _events.tryEmit("Categoría creada")
                onDone()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "No se pudo crear la categoría")
            }
        }
    }

    private fun buildDashboardData(
        streams: CoreStreams,
        activeMonth: String
    ): FinanceDashboardData {
        val categories = CategoryDefaults.mergeCategories(streams.customCategories)
        val sortedAllExpenses = streams.expenses.sortedByDescending { it.expenseDateMillis }
        val monthExpenses = sortedAllExpenses.filter { it.yearMonth == activeMonth }
        val monthTotal = monthExpenses.sumOf { it.amount }

        val prevMonth = Formatters.shiftYearMonth(activeMonth, -1)
        val prevTotal = sortedAllExpenses.filter { it.yearMonth == prevMonth }.sumOf { it.amount }
        val deltaPercent = if (prevTotal > 0.0) {
            ((monthTotal - prevTotal) / prevTotal) * 100.0
        } else {
            null
        }

        val daysInMonth = Formatters.daysInYearMonth(activeMonth)
        val dailyMap = mutableMapOf<Int, Double>()
        monthExpenses.forEach { expense ->
            val day = Formatters.dayOfMonth(expense.expenseDateMillis)
            dailyMap[day] = (dailyMap[day] ?: 0.0) + expense.amount
        }
        val highestDayEntry = dailyMap.maxByOrNull { it.value }
        val activeDaysDenominator = if (activeMonth == Formatters.currentYearMonth()) {
            Formatters.dayOfMonth(System.currentTimeMillis()).coerceAtLeast(1)
        } else {
            daysInMonth
        }
        val dailyAverage = if (monthTotal > 0.0) monthTotal / activeDaysDenominator else 0.0

        val monthBudgetsRaw = streams.budgets.filter { it.yearMonth == activeMonth }
        val spentByCategory = monthExpenses.groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val budgetProgressList = monthBudgetsRaw.map { budget ->
            val spent = spentByCategory[budget.categoryId] ?: 0.0
            val ratio = if (budget.limitAmount > 0.0) (spent / budget.limitAmount).toFloat() else 0f
            val percentage = (ratio * 100f).toInt()
            val alertLevel = when {
                ratio >= 1.0f -> BudgetAlertLevel.EXCEEDED_100
                ratio >= 0.75f -> BudgetAlertLevel.WARNING_75
                else -> BudgetAlertLevel.SAFE
            }
            BudgetProgress(
                budget = budget,
                spentAmount = spent,
                ratio = ratio,
                percentage = percentage,
                alertLevel = alertLevel
            )
        }.sortedByDescending { it.ratio }

        val budgetByCategory = budgetProgressList.associateBy { it.budget.categoryId }

        val breakdowns = monthExpenses
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                val catTotal = list.sumOf { it.amount }
                val catName = categories.firstOrNull { it.id == catId }?.name
                    ?: list.first().categoryName
                val budgetProg = budgetByCategory[catId]
                CategoryBreakdown(
                    categoryId = catId,
                    categoryName = catName,
                    amount = catTotal,
                    percentage = if (monthTotal > 0.0) ((catTotal / monthTotal) * 100f).toFloat() else 0f,
                    color = CategoryDefaults.findCategoryColor(catId, categories),
                    budgetLimit = budgetProg?.budget?.limitAmount,
                    budgetAlertLevel = budgetProg?.alertLevel
                )
            }
            .sortedByDescending { it.amount }

        val sortedDebts = streams.debts.sortedWith(
            compareBy<Debt> { it.statusEnum == DebtStatus.SETTLED }
                .thenBy { it.dueDateMillis ?: Long.MAX_VALUE }
        )
        val pendingDebts = sortedDebts.filter { it.statusEnum == DebtStatus.PENDING }
        val owedToMePending = pendingDebts
            .filter { it.directionEnum == DebtDirection.OWED_TO_ME }
            .sumOf { it.remainingAmount }
        val iOwePending = pendingDebts
            .filter { it.directionEnum == DebtDirection.I_OWE }
            .sumOf { it.remainingAmount }
        val nowMillis = System.currentTimeMillis()
        val overdueCount = pendingDebts.count { debt ->
            val due = debt.dueDateMillis
            due != null && due < nowMillis
        }

        val monthDebts = sortedDebts.filter { it.yearMonth == activeMonth }

        return FinanceDashboardData(
            selectedYearMonth = activeMonth,
            categories = categories,
            allExpenses = sortedAllExpenses,
            monthExpenses = monthExpenses,
            monthTotalSpent = monthTotal,
            previousMonthTotalSpent = prevTotal,
            monthDeltaPercent = deltaPercent,
            dailyAverage = dailyAverage,
            highestDayNumber = highestDayEntry?.key,
            highestDayAmount = highestDayEntry?.value ?: 0.0,
            categoryBreakdowns = breakdowns,
            dailySpending = dailyMap,
            daysInMonth = daysInMonth,
            allDebts = sortedDebts,
            totalOwedToMePending = owedToMePending,
            totalIOwePending = iOwePending,
            netDebtBalance = owedToMePending - iOwePending,
            overdueDebtsCount = overdueCount,
            monthDebtsCreatedCount = monthDebts.size,
            monthDebtsTotalAmount = monthDebts.sumOf { it.totalAmount },
            monthBudgets = budgetProgressList,
            alertingBudgets = budgetProgressList.filter { it.alertLevel != BudgetAlertLevel.SAFE },
            reminders = streams.reminders.sortedBy { it.scheduledAtMillis }
        )
    }

    private companion object {
        const val TAG = "FinanceVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
