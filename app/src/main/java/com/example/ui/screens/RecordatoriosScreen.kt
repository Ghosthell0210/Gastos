package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Reminder
import com.example.data.ReminderType
import com.example.notifications.ReminderNotificationHelper
import com.example.ui.components.Formatters
import com.example.ui.theme.PesoClaroThemeExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordatoriosScreen(
    reminders: List<Reminder>,
    onBack: () -> Unit,
    onAddReminder: () -> Unit,
    onEditReminder: (Reminder) -> Unit,
    onToggleReminder: (Reminder, Boolean) -> Unit,
    onPreviewNotification: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ReminderNotificationHelper.hasNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Recordatorios y alertas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onAddReminder,
                        modifier = Modifier.testTag("add_reminder_top_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Nuevo recordatorio")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = PesoClaroThemeExtras.semanticColors.budgetWarningContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Activa el permiso de notificaciones para recibir recordatorios programados.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }) {
                                    Text("Activar")
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onAddReminder,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_reminder_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Crear recordatorio de deuda o gasto")
                    }
                }

                if (reminders.isEmpty()) {
                    item {
                        EmptyFinanceCard(
                            icon = Icons.Rounded.NotificationsNone,
                            title = "Aún no tienes recordatorios",
                            subtitle = "Programa notificaciones para fechas de vencimiento de deudas o revisiones semanales/mensuales por categoría de gasto."
                        )
                    }
                } else {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderItemCard(
                            reminder = reminder,
                            onClick = { onEditReminder(reminder) },
                            onToggle = { enabled -> onToggleReminder(reminder, enabled) },
                            onPreview = {
                                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onPreviewNotification(reminder)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderItemCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onPreview: () -> Unit
) {
    val isDebt = reminder.typeEnum == ReminderType.DEBT_DUE
    val semantic = PesoClaroThemeExtras.semanticColors
    val badgeColor = if (isDebt) semantic.iOwe else MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isDebt) "Deuda: ${reminder.targetName}" else "Categoría: ${reminder.targetName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = badgeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = Formatters.formatDateTime(reminder.scheduledAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = reminder.frequencyEnum.label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Probar", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
