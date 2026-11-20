package com.example.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.Reminder
import com.example.data.ReminderFrequency
import com.example.data.ReminderType
import java.util.concurrent.TimeUnit

class ReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    fun syncReminders(reminders: List<Reminder>) {
        reminders.forEach { reminder ->
            if (reminder.id.isNotBlank()) {
                if (reminder.enabled) {
                    schedule(reminder)
                } else {
                    cancel(reminder.id)
                }
            }
        }
    }

    fun schedule(reminder: Reminder) {
        if (reminder.id.isBlank() || !reminder.enabled) return
        val now = System.currentTimeMillis()
        // For ONCE reminders already far in the past (> 1 hour), don't re-fire on sync
        if (reminder.frequencyEnum == ReminderFrequency.ONCE && reminder.scheduledAtMillis < now - 3_600_000L) {
            return
        }
        scheduleRaw(
            reminderId = reminder.id,
            title = reminder.title,
            reminderType = reminder.typeEnum,
            targetName = reminder.targetName,
            frequency = reminder.frequencyEnum,
            scheduledAtMillis = reminder.scheduledAtMillis,
            note = reminder.note
        )
    }

    fun scheduleRaw(
        reminderId: String,
        title: String,
        reminderType: ReminderType,
        targetName: String,
        frequency: ReminderFrequency,
        scheduledAtMillis: Long,
        note: String?
    ) {
        val delayMillis = (scheduledAtMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
        val input = Data.Builder()
            .putString(ReminderWorker.KEY_REMINDER_ID, reminderId)
            .putString(ReminderWorker.KEY_TITLE, title)
            .putString(ReminderWorker.KEY_TYPE, reminderType.name)
            .putString(ReminderWorker.KEY_TARGET_NAME, targetName)
            .putString(ReminderWorker.KEY_FREQUENCY, frequency.name)
            .putLong(ReminderWorker.KEY_SCHEDULED_AT, scheduledAtMillis)
            .putString(ReminderWorker.KEY_NOTE, note)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .addTag(uniqueWorkName(reminderId))
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(reminderId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(reminderId: String) {
        if (reminderId.isBlank()) return
        workManager.cancelUniqueWork(uniqueWorkName(reminderId))
    }

    fun previewNotificationNow(reminder: Reminder) {
        ReminderNotificationHelper.showReminderNotification(
            context = appContext,
            reminderId = reminder.id.ifBlank { "preview" },
            title = reminder.title,
            reminderType = reminder.typeEnum,
            targetName = reminder.targetName,
            scheduledAtMillis = reminder.scheduledAtMillis,
            note = reminder.note
        )
    }

    private fun uniqueWorkName(reminderId: String) = "pesoclaro_reminder_$reminderId"
}
