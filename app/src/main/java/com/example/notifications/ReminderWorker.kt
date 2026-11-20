package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.ReminderFrequency
import com.example.data.ReminderType
import java.util.Calendar

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Recordatorio PesoClaro"
        val typeRaw = inputData.getString(KEY_TYPE)
        val targetName = inputData.getString(KEY_TARGET_NAME) ?: "Finanzas"
        val frequencyRaw = inputData.getString(KEY_FREQUENCY)
        val scheduledAtMillis = inputData.getLong(KEY_SCHEDULED_AT, System.currentTimeMillis())
        val note = inputData.getString(KEY_NOTE)

        val reminderType = ReminderType.fromRaw(typeRaw)
        val frequency = ReminderFrequency.fromRaw(frequencyRaw)

        ReminderNotificationHelper.showReminderNotification(
            context = applicationContext,
            reminderId = reminderId,
            title = title,
            reminderType = reminderType,
            targetName = targetName,
            scheduledAtMillis = scheduledAtMillis,
            note = note
        )

        if (frequency == ReminderFrequency.WEEKLY || frequency == ReminderFrequency.MONTHLY) {
            val nextTimeMillis = computeNextOccurrence(scheduledAtMillis, frequency)
            ReminderScheduler(applicationContext).scheduleRaw(
                reminderId = reminderId,
                title = title,
                reminderType = reminderType,
                targetName = targetName,
                frequency = frequency,
                scheduledAtMillis = nextTimeMillis,
                note = note
            )
        }

        return Result.success()
    }

    private fun computeNextOccurrence(currentMillis: Long, frequency: ReminderFrequency): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }
        val now = System.currentTimeMillis()
        do {
            when (frequency) {
                ReminderFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                ReminderFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
                ReminderFrequency.ONCE -> break
            }
        } while (cal.timeInMillis <= now)
        return cal.timeInMillis
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
        const val KEY_TITLE = "title"
        const val KEY_TYPE = "type"
        const val KEY_TARGET_NAME = "target_name"
        const val KEY_FREQUENCY = "frequency"
        const val KEY_SCHEDULED_AT = "scheduled_at"
        const val KEY_NOTE = "note"
    }
}
