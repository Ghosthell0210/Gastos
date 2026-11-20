package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ReminderType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderNotificationHelper {
    const val CHANNEL_ID = "pesoclaro_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showReminderNotification(
        context: Context,
        reminderId: String,
        title: String,
        reminderType: ReminderType,
        targetName: String,
        scheduledAtMillis: Long,
        note: String?
    ) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return

        val dateFormat = SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.Builder().setLanguage("es").setRegion("CU").build()
        )
        val formattedDate = dateFormat.format(Date(scheduledAtMillis))

        val typeLabel = when (reminderType) {
            ReminderType.DEBT_DUE -> "Deuda: $targetName"
            ReminderType.EXPENSE_REVIEW -> "Categoría: $targetName"
        }
        val bodyText = buildString {
            append(typeLabel)
            append(" · Fecha programada: ")
            append(formattedDate)
            if (!note.isNullOrBlank()) {
                append("\n")
                append(note)
            }
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("$typeLabel · $formattedDate")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
    }
}
