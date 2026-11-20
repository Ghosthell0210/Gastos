package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val categoryId: String = "",
    val categoryName: String = "",
    val expenseDateMillis: Long = 0L,
    val yearMonth: String = "",
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class DebtDirection(val label: String) {
    OWED_TO_ME("Me deben"),
    I_OWE("Debo");

    companion object {
        fun fromRaw(raw: String?): DebtDirection =
            entries.firstOrNull { it.name == raw } ?: I_OWE
    }
}

enum class DebtStatus(val label: String) {
    PENDING("Pendiente"),
    SETTLED("Saldada");

    companion object {
        fun fromRaw(raw: String?): DebtStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class Debt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val direction: String = DebtDirection.I_OWE.name,
    val counterparty: String = "",
    val concept: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val status: String = DebtStatus.PENDING.name,
    val dueDateMillis: Long? = null,
    val yearMonth: String = "",
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val directionEnum: DebtDirection get() = DebtDirection.fromRaw(direction)
    val statusEnum: DebtStatus get() = DebtStatus.fromRaw(status)
    val remainingAmount: Double get() = (totalAmount - paidAmount).coerceAtLeast(0.0)
    val progressRatio: Float
        get() = if (totalAmount <= 0.0) 0f else (paidAmount / totalAmount).toFloat().coerceIn(0f, 1f)
}

@Keep
@IgnoreExtraProperties
data class DebtPayment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val paymentDateMillis: Long = 0L,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Budget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val yearMonth: String = "",
    val limitAmount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class BudgetAlertLevel {
    SAFE,
    WARNING_75,
    EXCEEDED_100
}

data class BudgetProgress(
    val budget: Budget,
    val spentAmount: Double,
    val ratio: Float,
    val percentage: Int,
    val alertLevel: BudgetAlertLevel
)

enum class ReminderType(val label: String) {
    DEBT_DUE("Vencimiento de deuda"),
    EXPENSE_REVIEW("Revisión de gastos");

    companion object {
        fun fromRaw(raw: String?): ReminderType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE_REVIEW
    }
}

enum class ReminderFrequency(val label: String) {
    ONCE("Una vez"),
    WEEKLY("Semanal"),
    MONTHLY("Mensual");

    companion object {
        fun fromRaw(raw: String?): ReminderFrequency =
            entries.firstOrNull { it.name == raw } ?: MONTHLY
    }
}

@Keep
@IgnoreExtraProperties
data class Reminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val reminderType: String = ReminderType.EXPENSE_REVIEW.name,
    val targetId: String? = null,
    val targetName: String = "",
    val frequency: String = ReminderFrequency.MONTHLY.name,
    val scheduledAtMillis: Long = 0L,
    val enabled: Boolean = true,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val typeEnum: ReminderType get() = ReminderType.fromRaw(reminderType)
    val frequencyEnum: ReminderFrequency get() = ReminderFrequency.fromRaw(frequency)
}

@Keep
@IgnoreExtraProperties
data class CustomCategory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconKey: String = "category",
    val colorHex: String = "#047857",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CategoryOption(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val isCustom: Boolean = false
)
