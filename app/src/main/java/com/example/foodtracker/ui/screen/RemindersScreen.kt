package com.example.foodtracker.ui.screen

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.foodtracker.data.notifications.ReminderScheduler
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.ui.theme.AppTextSecondary
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val reminderScheduler = remember { ReminderScheduler(context) }
    var settings by remember { mutableStateOf(reminderScheduler.getReminderSettings()) }

    // Runtime POST_NOTIFICATIONS permission (Android 13+)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasNotificationPermission = isGranted }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders & Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Permission warning card
            if (!hasNotificationPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Notification Permission Required",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Reminders won't work without notification permission.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp
                            )
                            TextButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            ) { Text("Grant Permission") }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Meal Reminders Section
            SectionHeader(
                iconText = "🍽️",
                title = "Meal Reminders",
                subtitle = "Get notified for your meals"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Breakfast
            ReminderItem(
                title = "Breakfast",
                iconText = "☕",
                enabled = settings.breakfastEnabled,
                hour = settings.breakfastHour,
                minute = settings.breakfastMinute,
                onEnabledChange = { enabled ->
                    settings = settings.copy(breakfastEnabled = enabled)
                    reminderScheduler.scheduleMealReminder(
                        ReminderScheduler.TYPE_BREAKFAST,
                        settings.breakfastHour,
                        settings.breakfastMinute,
                        enabled
                    )
                },
                onTimeChange = { hour, minute ->
                    settings = settings.copy(breakfastHour = hour, breakfastMinute = minute)
                    if (settings.breakfastEnabled) {
                        reminderScheduler.scheduleMealReminder(
                            ReminderScheduler.TYPE_BREAKFAST,
                            hour,
                            minute,
                            true
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Lunch
            ReminderItem(
                title = "Lunch",
                iconText = "🍱",
                enabled = settings.lunchEnabled,
                hour = settings.lunchHour,
                minute = settings.lunchMinute,
                onEnabledChange = { enabled ->
                    settings = settings.copy(lunchEnabled = enabled)
                    reminderScheduler.scheduleMealReminder(
                        ReminderScheduler.TYPE_LUNCH,
                        settings.lunchHour,
                        settings.lunchMinute,
                        enabled
                    )
                },
                onTimeChange = { hour, minute ->
                    settings = settings.copy(lunchHour = hour, lunchMinute = minute)
                    if (settings.lunchEnabled) {
                        reminderScheduler.scheduleMealReminder(
                            ReminderScheduler.TYPE_LUNCH,
                            hour,
                            minute,
                            true
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Dinner
            ReminderItem(
                title = "Dinner",
                iconText = "🍽️",
                enabled = settings.dinnerEnabled,
                hour = settings.dinnerHour,
                minute = settings.dinnerMinute,
                onEnabledChange = { enabled ->
                    settings = settings.copy(dinnerEnabled = enabled)
                    reminderScheduler.scheduleMealReminder(
                        ReminderScheduler.TYPE_DINNER,
                        settings.dinnerHour,
                        settings.dinnerMinute,
                        enabled
                    )
                },
                onTimeChange = { hour, minute ->
                    settings = settings.copy(dinnerHour = hour, dinnerMinute = minute)
                    if (settings.dinnerEnabled) {
                        reminderScheduler.scheduleMealReminder(
                            ReminderScheduler.TYPE_DINNER,
                            hour,
                            minute,
                            true
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Water Reminders Section
            SectionHeader(
                iconText = "💧",
                title = "Water Reminders",
                subtitle = "Stay hydrated throughout the day"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            WaterReminderItem(
                enabled = settings.waterEnabled,
                intervalHours = settings.waterIntervalHours,
                onEnabledChange = { enabled ->
                    settings = settings.copy(waterEnabled = enabled)
                    reminderScheduler.scheduleWaterReminder(
                        settings.waterIntervalHours,
                        enabled
                    )
                },
                onIntervalChange = { interval ->
                    settings = settings.copy(waterIntervalHours = interval)
                    if (settings.waterEnabled) {
                        reminderScheduler.scheduleWaterReminder(interval, true)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Activity Reminders Section
            SectionHeader(
                iconText = "🏃",
                title = "Activity Reminders",
                subtitle = "Stay active and maintain your streak"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Steps Reminder
            ReminderItem(
                title = "Daily Steps Check",
                iconText = "👟",
                enabled = settings.stepsEnabled,
                hour = settings.stepsHour,
                minute = settings.stepsMinute,
                onEnabledChange = { enabled ->
                    settings = settings.copy(stepsEnabled = enabled)
                    reminderScheduler.scheduleStepReminder(
                        settings.stepsHour,
                        settings.stepsMinute,
                        enabled
                    )
                },
                onTimeChange = { hour, minute ->
                    settings = settings.copy(stepsHour = hour, stepsMinute = minute)
                    if (settings.stepsEnabled) {
                        reminderScheduler.scheduleStepReminder(hour, minute, true)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Streak Warning
            ReminderItem(
                title = "Streak Reminder",
                iconText = "🔥",
                enabled = settings.streakWarningEnabled,
                hour = settings.streakWarningHour,
                minute = settings.streakWarningMinute,
                onEnabledChange = { enabled ->
                    settings = settings.copy(streakWarningEnabled = enabled)
                    reminderScheduler.scheduleStreakWarning(
                        settings.streakWarningHour,
                        settings.streakWarningMinute,
                        enabled
                    )
                },
                onTimeChange = { hour, minute ->
                    settings = settings.copy(streakWarningHour = hour, streakWarningMinute = minute)
                    if (settings.streakWarningEnabled) {
                        reminderScheduler.scheduleStreakWarning(hour, minute, true)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppSurface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Reminders help you maintain your healthy habits and keep your streak alive!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionHeader(iconText: String, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = iconText,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ReminderItem(
    title: String,
    iconText: String,
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = iconText,
                fontSize = 24.sp,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                if (enabled) {
                    Text(
                        String.format("%02d:%02d", hour, minute),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            if (enabled) {
                TextButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, selectedHour, selectedMinute ->
                                onTimeChange(selectedHour, selectedMinute)
                            },
                            hour,
                            minute,
                            true
                        ).show()
                    }
                ) {
                    Text("Change")
                }
            }
            
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

@Composable
fun WaterReminderItem(
    enabled: Boolean,
    intervalHours: Int,
    onEnabledChange: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💧",
                    fontSize = 24.sp,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Water Reminder",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    if (enabled) {
                        Text(
                            "Every $intervalHours hours",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Interval (hours)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Slider(
                    value = intervalHours.toFloat(),
                    onValueChange = { onIntervalChange(it.toInt()) },
                    valueRange = 1f..6f,
                    steps = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
