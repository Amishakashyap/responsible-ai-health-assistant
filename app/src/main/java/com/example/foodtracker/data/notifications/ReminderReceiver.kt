package com.example.foodtracker.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.foodtracker.data.streak.StreakManager
import com.example.foodtracker.data.user.UserPreferences

/**
 * BroadcastReceiver that handles scheduled reminders
 */
class ReminderReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val notificationHelper = NotificationHelper(context)
        val userPrefs = UserPreferences(context)
        val nickname = userPrefs.nickname
        
        when (intent.action) {
            "MEAL_REMINDER" -> {
                val reminderType = intent.getStringExtra("reminder_type") ?: return
                handleMealReminder(context, notificationHelper, reminderType, nickname)
            }
            "WATER_REMINDER" -> {
                notificationHelper.showWaterReminder(nickname)
            }
            "STEPS_REMINDER" -> {
                handleStepsReminder(context, notificationHelper, nickname)
            }
            "STREAK_WARNING" -> {
                handleStreakWarning(context, notificationHelper, nickname)
            }
        }
    }
    
    private fun handleMealReminder(context: Context, notificationHelper: NotificationHelper, type: String, nickname: String) {
        val mealName = when (type) {
            ReminderScheduler.TYPE_BREAKFAST -> "Breakfast"
            ReminderScheduler.TYPE_LUNCH -> "Lunch"
            ReminderScheduler.TYPE_DINNER -> "Dinner"
            else -> return
        }
        
        val notificationId = when (type) {
            ReminderScheduler.TYPE_BREAKFAST -> NotificationHelper.NOTIFICATION_ID_BREAKFAST
            ReminderScheduler.TYPE_LUNCH -> NotificationHelper.NOTIFICATION_ID_LUNCH
            ReminderScheduler.TYPE_DINNER -> NotificationHelper.NOTIFICATION_ID_DINNER
            else -> return
        }
        
        notificationHelper.showMealReminder(mealName, notificationId, nickname)
    }
    
    private fun handleStepsReminder(context: Context, notificationHelper: NotificationHelper, nickname: String) {
        // Get current step count from SharedPreferences
        val prefs = context.getSharedPreferences("steps_prefs", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val currentSteps = prefs.getInt("step_count_$today", 0)
        val goalSteps = prefs.getInt("daily_goal", 10000)
        
        // Only show reminder if goal not yet reached
        if (currentSteps < goalSteps) {
            notificationHelper.showStepGoalReminder(currentSteps, goalSteps, nickname)
        }
    }
    
    private fun handleStreakWarning(context: Context, notificationHelper: NotificationHelper, nickname: String) {
        val streakManager = StreakManager(context)
        
        // Only warn if user hasn't been active today and has an active streak
        if (!streakManager.isActiveToday()) {
            val currentStreak = streakManager.getCurrentStreak()
            if (currentStreak > 0) {
                notificationHelper.showStreakWarning(currentStreak, nickname)
            }
        }
    }
}
