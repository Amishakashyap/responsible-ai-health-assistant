package com.example.foodtracker.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.foodtracker.MainActivity
import com.example.foodtracker.R

/**
 * NotificationHelper manages notification creation and display
 */
class NotificationHelper(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID_REMINDERS = "reminders_channel"
        const val CHANNEL_ID_STREAK = "streak_channel"
        const val CHANNEL_ID_GOALS = "goals_channel"
        
        const val NOTIFICATION_ID_BREAKFAST = 1001
        const val NOTIFICATION_ID_LUNCH = 1002
        const val NOTIFICATION_ID_DINNER = 1003
        const val NOTIFICATION_ID_WATER = 1004
        const val NOTIFICATION_ID_STEPS = 1005
        const val NOTIFICATION_ID_STREAK = 1006
    }
    
    init {
        createNotificationChannels()
    }

    /** Check POST_NOTIFICATIONS permission (required on Android 13+) */
    private fun canNotify(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * Create notification channels (required for Android 8.0+)
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Reminders channel
            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Meal Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for meals and water intake"
                enableVibration(true)
            }
            
            // Streak channel
            val streakChannel = NotificationChannel(
                CHANNEL_ID_STREAK,
                "Streak Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about your activity streak"
                enableVibration(true)
            }
            
            // Goals channel
            val goalsChannel = NotificationChannel(
                CHANNEL_ID_GOALS,
                "Daily Goals",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about your daily step and nutrition goals"
            }
            
            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(streakChannel)
            notificationManager.createNotificationChannel(goalsChannel)
        }
    }
    
    /**
     * Show a meal reminder notification
     */
    fun showMealReminder(mealType: String, notificationId: Int, nickname: String = "User") {
        if (!canNotify()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$nickname, Time for $mealType!")
            .setContentText("Don't forget to log your $mealType to maintain your streak 🔥")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Show water reminder notification
     */
    fun showWaterReminder(nickname: String = "User") {
        if (!canNotify()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_WATER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$nickname, Stay Hydrated! 💧")
            .setContentText("Time to drink some water")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_WATER, notification)
    }
    
    /**
     * Show step goal reminder
     */
    fun showStepGoalReminder(currentSteps: Int, goalSteps: Int, nickname: String = "User") {
        if (!canNotify()) return
        val remainingSteps = goalSteps - currentSteps
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_STEPS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GOALS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$nickname, Step Goal Reminder 👟")
            .setContentText("You need $remainingSteps more steps to reach your goal!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_STEPS, notification)
    }
    
    /**
     * Show streak achievement notification
     */
    fun showStreakNotification(streakDays: Int) {
        if (!canNotify()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_STREAK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val message = when {
            streakDays == 1 -> "Great start! Keep it going 🔥"
            streakDays < 7 -> "You're on a $streakDays day streak! 🔥"
            streakDays < 30 -> "Amazing! $streakDays days in a row! 🔥🔥"
            else -> "Incredible! $streakDays day streak! You're unstoppable! 🔥🔥🔥"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_STREAK)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Streak Achievement!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_STREAK, notification)
    }
    
    /**
     * Show streak warning (about to lose streak)
     */
    fun showStreakWarning(streakDays: Int, nickname: String = "User") {
        if (!canNotify()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_STREAK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_STREAK)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("$nickname, Don't Break Your Streak! ⚠️")
            .setContentText("You're on a $streakDays day streak! Log your activity to keep it going.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_STREAK, notification)
    }
    
    /**
     * Cancel a specific notification
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
