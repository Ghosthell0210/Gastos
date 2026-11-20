package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinanceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google para sincronizar tus datos.")
    }

    private fun userRoot(userId: String) = db.collection("users").document(userId)

    // --- Real-time Streams ---

    fun observeExpenses(userId: String): Flow<List<Expense>> = observeCollection(
        userId = userId,
        collectionName = "expenses",
        clazz = Expense::class.java
    )

    fun observeDebts(userId: String): Flow<List<Debt>> = observeCollection(
        userId = userId,
        collectionName = "debts",
        clazz = Debt::class.java
    )

    fun observeDebtPayments(userId: String, debtId: String): Flow<List<DebtPayment>> = flow {
        val path = "users/$userId/debts/$debtId/payments"
        if (userId.isBlank() || debtId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            userRoot(userId)
                .collection("debts")
                .document(debtId)
                .collection("payments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DebtPayment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.paymentDateMillis }
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeBudgets(userId: String): Flow<List<Budget>> = observeCollection(
        userId = userId,
        collectionName = "budgets",
        clazz = Budget::class.java
    )

    fun observeReminders(userId: String): Flow<List<Reminder>> = observeCollection(
        userId = userId,
        collectionName = "reminders",
        clazz = Reminder::class.java
    )

    fun observeCustomCategories(userId: String): Flow<List<CustomCategory>> = observeCollection(
        userId = userId,
        collectionName = "categories",
        clazz = CustomCategory::class.java
    )

    private fun <T : Any> observeCollection(
        userId: String,
        collectionName: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/$collectionName"
        emitAll(
            userRoot(userId)
                .collection(collectionName)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- Expenses CRUD ---

    suspend fun addExpense(
        title: String,
        amount: Double,
        categoryId: String,
        categoryName: String,
        expenseDateMillis: Long,
        yearMonth: String,
        note: String?
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/expenses"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(80)
            require(cleanTitle.isNotEmpty()) { "Ingresa un título para el gasto" }
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "Monto fuera de rango válido" }
            require(categoryId.isNotBlank() && categoryName.isNotBlank()) { "Selecciona una categoría" }
            require(expenseDateMillis > 0L) { "Fecha inválida" }
            require(YEAR_MONTH_REGEX.matches(yearMonth)) { "Mes inválido" }

            val docRef = userRoot(uid).collection("expenses").document()
            docPath = docRef.path
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(300)
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "amount" to amount,
                "categoryId" to categoryId.trim().take(64),
                "categoryName" to categoryName.trim().take(40),
                "expenseDateMillis" to expenseDateMillis,
                "yearMonth" to yearMonth,
                "note" to cleanNote,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateExpense(
        expenseId: String,
        title: String,
        amount: Double,
        categoryId: String,
        categoryName: String,
        expenseDateMillis: Long,
        yearMonth: String,
        note: String?
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/expenses/$expenseId"
        return try {
            val uid = requireUserId()
            require(expenseId.isNotBlank()) { "ID de gasto inválido" }
            val cleanTitle = title.trim().take(80)
            require(cleanTitle.isNotEmpty()) { "Ingresa un título para el gasto" }
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "Monto fuera de rango válido" }

            val docRef = userRoot(uid).collection("expenses").document(expenseId)
            docPath = docRef.path
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(300)
            val updates = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "amount" to amount,
                "categoryId" to categoryId.trim().take(64),
                "categoryName" to categoryName.trim().take(40),
                "expenseDateMillis" to expenseDateMillis,
                "yearMonth" to yearMonth,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanNote != null) {
                updates["note"] = cleanNote
            } else {
                updates["note"] = FieldValue.delete()
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/expenses/$expenseId"
        return try {
            val uid = requireUserId()
            require(expenseId.isNotBlank()) { "ID de gasto inválido" }
            val docRef = userRoot(uid).collection("expenses").document(expenseId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Debts & Payments CRUD ---

    suspend fun addDebt(
        direction: DebtDirection,
        counterparty: String,
        concept: String,
        totalAmount: Double,
        dueDateMillis: Long?,
        yearMonth: String,
        note: String?
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/debts"
        return try {
            val uid = requireUserId()
            val cleanCounterparty = counterparty.trim().take(60)
            val cleanConcept = concept.trim().take(100)
            require(cleanCounterparty.isNotEmpty()) { "Ingresa el nombre de la persona o entidad" }
            require(cleanConcept.isNotEmpty()) { "Ingresa un concepto para la deuda" }
            require(totalAmount > 0.0 && totalAmount <= 1_000_000_000.0) { "Monto inválido" }
            require(YEAR_MONTH_REGEX.matches(yearMonth)) { "Mes inválido" }

            val docRef = userRoot(uid).collection("debts").document()
            docPath = docRef.path
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(300)
            val validDueDate = dueDateMillis?.takeIf { it > 0L }
            val payload = mapOf(
                "userId" to uid,
                "direction" to direction.name,
                "counterparty" to cleanCounterparty,
                "concept" to cleanConcept,
                "totalAmount" to totalAmount,
                "paidAmount" to 0.0,
                "status" to DebtStatus.PENDING.name,
                "dueDateMillis" to validDueDate,
                "yearMonth" to yearMonth,
                "note" to cleanNote,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateDebt(
        debt: Debt,
        direction: DebtDirection,
        counterparty: String,
        concept: String,
        totalAmount: Double,
        dueDateMillis: Long?,
        yearMonth: String,
        note: String?
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/debts/${debt.id}"
        return try {
            val uid = requireUserId()
            require(debt.id.isNotBlank()) { "ID de deuda inválido" }
            val cleanCounterparty = counterparty.trim().take(60)
            val cleanConcept = concept.trim().take(100)
            require(cleanCounterparty.isNotEmpty() && cleanConcept.isNotEmpty()) { "Campos obligatorios vacíos" }
            require(totalAmount > 0.0 && totalAmount <= 1_000_000_000.0) { "Monto inválido" }

            val clampedPaid = debt.paidAmount.coerceIn(0.0, totalAmount)
            val newStatus = if (clampedPaid >= totalAmount - 0.005) DebtStatus.SETTLED.name else DebtStatus.PENDING.name
            val docRef = userRoot(uid).collection("debts").document(debt.id)
            docPath = docRef.path
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(300)
            val validDueDate = dueDateMillis?.takeIf { it > 0L }

            val updates = mutableMapOf<String, Any>(
                "direction" to direction.name,
                "counterparty" to cleanCounterparty,
                "concept" to cleanConcept,
                "totalAmount" to totalAmount,
                "paidAmount" to clampedPaid,
                "status" to newStatus,
                "yearMonth" to yearMonth,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (validDueDate != null) updates["dueDateMillis"] = validDueDate else updates["dueDateMillis"] = FieldValue.delete()
            if (cleanNote != null) updates["note"] = cleanNote else updates["note"] = FieldValue.delete()

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addDebtPayment(
        debt: Debt,
        paymentAmount: Double,
        paymentDateMillis: Long,
        note: String?
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/debts/${debt.id}/payments"
        return try {
            val uid = requireUserId()
            require(debt.id.isNotBlank()) { "ID de deuda inválido" }
            require(paymentAmount > 0.0) { "El abono debe ser mayor que cero" }
            val remaining = debt.remainingAmount
            require(remaining > 0.0) { "Esta deuda ya está saldada" }

            val appliedAmount = paymentAmount.coerceAtMost(remaining)
            val newPaidTotal = (debt.paidAmount + appliedAmount).coerceAtMost(debt.totalAmount)
            val newStatus = if (newPaidTotal >= debt.totalAmount - 0.005) {
                DebtStatus.SETTLED.name
            } else {
                DebtStatus.PENDING.name
            }

            val debtRef = userRoot(uid).collection("debts").document(debt.id)
            val paymentRef = debtRef.collection("payments").document()
            docPath = paymentRef.path

            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(200)
            val paymentPayload = mapOf(
                "userId" to uid,
                "amount" to appliedAmount,
                "paymentDateMillis" to paymentDateMillis,
                "note" to cleanNote,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val batch = db.batch()
            batch.set(paymentRef, paymentPayload)
            batch.update(
                debtRef,
                mapOf(
                    "paidAmount" to newPaidTotal,
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDebt(debtId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/debts/$debtId"
        return try {
            val uid = requireUserId()
            require(debtId.isNotBlank()) { "ID de deuda inválido" }
            val debtRef = userRoot(uid).collection("debts").document(debtId)
            docPath = debtRef.path
            val paymentsSnapshot = debtRef.collection("payments").get().await()
            val batch = db.batch()
            for (doc in paymentsSnapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.delete(debtRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Budgets CRUD ---

    suspend fun upsertBudget(
        categoryId: String,
        categoryName: String,
        yearMonth: String,
        limitAmount: Double
    ): Result<Unit> {
        val docId = "${yearMonth}_${categoryId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}"
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/budgets/$docId"
        return try {
            val uid = requireUserId()
            require(limitAmount > 0.0 && limitAmount <= 1_000_000_000.0) { "El límite debe ser mayor a 0 CUP" }
            require(YEAR_MONTH_REGEX.matches(yearMonth)) { "Mes inválido" }

            val docRef = userRoot(uid).collection("budgets").document(docId)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "categoryName" to categoryName.trim().take(40),
                        "limitAmount" to limitAmount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "categoryId" to categoryId.trim().take(64),
                        "categoryName" to categoryName.trim().take(40),
                        "yearMonth" to yearMonth,
                        "limitAmount" to limitAmount,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(budgetId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/budgets/$budgetId"
        return try {
            val uid = requireUserId()
            require(budgetId.isNotBlank()) { "ID de presupuesto inválido" }
            val docRef = userRoot(uid).collection("budgets").document(budgetId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Reminders CRUD ---

    suspend fun saveReminder(
        existingId: String?,
        title: String,
        reminderType: ReminderType,
        targetId: String?,
        targetName: String,
        frequency: ReminderFrequency,
        scheduledAtMillis: Long,
        enabled: Boolean,
        note: String?
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reminders"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(80)
            val cleanTarget = targetName.trim().take(60)
            require(cleanTitle.isNotEmpty()) { "Ingresa un título para el recordatorio" }
            require(cleanTarget.isNotEmpty()) { "Selecciona una deuda o categoría" }
            require(scheduledAtMillis > 0L) { "Fecha programada inválida" }

            val cleanTargetId = targetId?.trim()?.takeIf { it.isNotEmpty() }?.take(128)
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(200)

            val collection = userRoot(uid).collection("reminders")
            if (existingId.isNullOrBlank()) {
                val docRef = collection.document()
                docPath = docRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "reminderType" to reminderType.name,
                    "targetId" to cleanTargetId,
                    "targetName" to cleanTarget,
                    "frequency" to frequency.name,
                    "scheduledAtMillis" to scheduledAtMillis,
                    "enabled" to enabled,
                    "note" to cleanNote,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                Result.success(docRef.id)
            } else {
                val docRef = collection.document(existingId)
                docPath = docRef.path
                val updates = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "reminderType" to reminderType.name,
                    "targetName" to cleanTarget,
                    "frequency" to frequency.name,
                    "scheduledAtMillis" to scheduledAtMillis,
                    "enabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanTargetId != null) updates["targetId"] = cleanTargetId else updates["targetId"] = FieldValue.delete()
                if (cleanNote != null) updates["note"] = cleanNote else updates["note"] = FieldValue.delete()
                docRef.update(updates).await()
                Result.success(existingId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleReminderEnabled(reminderId: String, enabled: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reminders/$reminderId"
        return try {
            val uid = requireUserId()
            require(reminderId.isNotBlank()) { "ID de recordatorio inválido" }
            val docRef = userRoot(uid).collection("reminders").document(reminderId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "enabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(reminderId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/reminders/$reminderId"
        return try {
            val uid = requireUserId()
            require(reminderId.isNotBlank()) { "ID de recordatorio inválido" }
            val docRef = userRoot(uid).collection("reminders").document(reminderId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Custom Categories CRUD ---

    suspend fun addCustomCategory(name: String, iconKey: String, colorHex: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/categories"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(40)
            require(cleanName.isNotEmpty()) { "Ingresa un nombre para la categoría" }
            val cleanColor = if (HEX_COLOR_REGEX.matches(colorHex)) colorHex else "#047857"
            val docRef = userRoot(uid).collection("categories").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "iconKey" to iconKey.trim().take(40),
                    "colorHex" to cleanColor,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomCategory(categoryId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/categories/$categoryId"
        return try {
            val uid = requireUserId()
            require(categoryId.isNotBlank()) { "ID inválido" }
            val docRef = userRoot(uid).collection("categories").document(categoryId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        val YEAR_MONTH_REGEX = Regex("^[0-9]{4}-[0-9]{2}$")
        val HEX_COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")
    }
}
