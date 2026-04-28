package com.example.foodtracker.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import java.util.*

/**
 * ReminderScheduler manages scheduling of recurring reminders using AlarmManager
 */
class ReminderScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "reminder_prefs"
        
        // Reminder types
        const val TYPE_BREAKFAST = "breakfast"
        const val TYPE_LUNCH = "lunch"
        const val TYPE_DINNER = "dinner"
        const val TYPE_WATER = "water"
        const val TYPE_STEPS = "steps"
        const val TYPE_STREAK_WARNING = "streak_warning"
        
        // Request codes
        private const val REQUEST_CODE_BREAKFAST = 2001
        private const val REQUEST_CODE_LUNCH = 2002
        private const val REQUEST_CODE_DINNER = 2003
        private const val REQUEST_CODE_WATER = 2004
        private const val REQUEST_CODE_STEPS = 2005
        private const val REQUEST_CODE_STREAK_WARNING = 2006
        
        // Preference keys
        private const val KEY_BREAKFAST_ENABLED = "breakfast_enabled"
        private const val KEY_BREAKFAST_HOUR = "breakfast_hour"
        private const val KEY_BREAKFAST_MINUTE = "breakfast_minute"
        
        private const val KEY_LUNCH_ENABLED = "lunch_enabled"
        private const val KEY_LUNCH_HOUR = "lunch_hour"
        private const val KEY_LUNCH_MINUTE = "lunch_minute"
        
        private const val KEY_DINNER_ENABLED = "dinner_enabled"
        private const val KEY_DINNER_HOUR = "dinner_hour"
        private const val KEY_DINNER_MINUTE = "dinner_minute"
        
        private const val KEY_WATER_ENABLED = "water_enabled"
        private const val KEY_WATER_INTERVAL = "water_interval_hours"
        
        private const val KEY_STEPS_ENABLED = "steps_enabled"
        private const val KEY_STEPS_HOUR = "steps_hour"
        private const val KEY_STEPS_MINUTE = "steps_minute"
        
        private const val KEY_STREAK_WARNING_ENABLED = "streak_warning_enabled"
        private const val KEY_STREAK_WARNING_HOUR = "streak_warning_hour"
        private const val KEY_STREAK_WARNING_MINUTE = "streak_warning_minute"
    }
    
    /**
     * Schedule a meal reminder
     */
    fun scheduleMealReminder(type: String, hour: Int, minute: Int, enabled: Boolean = true) {
        val requestCode = when (type) {
            TYPE_BREAKFAST -> REQUEST_CODE_BREAKFAST
            TYPE_LUNCH -> REQUEST_CODE_LUNCH
            TYPE_DINNER -> REQUEST_CODE_DINNER
            else -> return
        }
        
        // Save preferences
        prefs.edit().apply {
            putBoolean("${type}_enabled", enabled)
            putInt("${type}_hour", hour)
            putInt("${type}_minute", minute)
            apply()
        }
        
        if (!enabled) {
            cancelReminder(requestCode)
            return
        }
        
        // Schedule alarm
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // If time has passed today, schedule for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "MEAL_REMINDER"
            putExtra("reminder_type", type)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    
    /**
     * Schedule water reminders (repeating every few hours)
     */
    fun scheduleWaterReminder(intervalHours: Int = 2, enabled: Boolean = true) {
        prefs.edit().apply {
            putBoolean(KEY_WATER_ENABLED, enabled)
            putInt(KEY_WATER_INTERVAL, intervalHours)
            apply()
        }
        
        if (!enabled) {
            cancelReminder(REQUEST_CODE_WATER)
            return
        }
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "WATER_REMINDER"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WATER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val intervalMillis = intervalHours * 60 * 60 * 1000L
        
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMillis,
            intervalMillis,
            pendingIntent
        )
    }
    
    /**
     * Schedule step goal reminder
     */
    fun scheduleStepReminder(hour: Int, minute: Int, enabled: Boolean = true) {
        prefs.edit().apply {
            putBoolean(KEY_STEPS_ENABLED, enabled)
            putInt(KEY_STEPS_HOUR, hour)
            putInt(KEY_STEPS_MINUTE, minute)
            apply()
        }
        
        if (!enabled) {
            cancelReminder(REQUEST_CODE_STEPS)
            return
        }
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "STEPS_REMINDER"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_STEPS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    
    /**
     * Schedule streak warning (reminds user to maintain streak)
     */
    fun scheduleStreakWarning(hour: Int, minute: Int, enabled: Boolean = true) {
        prefs.edit().apply {
            putBoolean(KEY_STREAK_WARNING_ENABLED, enabled)
            putInt(KEY_STREAK_WARNING_HOUR, hour)
            putInt(KEY_STREAK_WARNING_MINUTE, minute)
            apply()
        }
        
        if (!enabled) {
            cancelReminder(REQUEST_CODE_STREAK_WARNING)
            return
        }
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "STREAK_WARNING"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_STREAK_WARNING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    
    /**
     * Cancel a specific reminder
     */
    private fun cancelReminder(requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * Cancel all reminders
     */
    fun cancelAllReminders() {
        cancelReminder(REQUEST_CODE_BREAKFAST)
        cancelReminder(REQUEST_CODE_LUNCH)
        cancelReminder(REQUEST_CODE_DINNER)
        cancelReminder(REQUEST_CODE_WATER)
        cancelReminder(REQUEST_CODE_STEPS)
        cancelReminder(REQUEST_CODE_STREAK_WARNING)
    }
    
    /**
     * Get reminder settings
     */
    fun getReminderSettings(): ReminderSettings {
        return ReminderSettings(
            breakfastEnabled = prefs.getBoolean(KEY_BREAKFAST_ENABLED, false),
            breakfastHour = prefs.getInt(KEY_BREAKFAST_HOUR, 8),
            breakfastMinute = prefs.getInt(KEY_BREAKFAST_MINUTE, 0),
            
            lunchEnabled = prefs.getBoolean(KEY_LUNCH_ENABLED, false),
            lunchHour = prefs.getInt(KEY_LUNCH_HOUR, 12),
            lunchMinute = prefs.getInt(KEY_LUNCH_MINUTE, 0),
            
            dinnerEnabled = prefs.getBoolean(KEY_DINNER_ENABLED, false),
            dinnerHour = prefs.getInt(KEY_DINNER_HOUR, 19),
            dinnerMinute = prefs.getInt(KEY_DINNER_MINUTE, 0),
            
            waterEnabled = prefs.getBoolean(KEY_WATER_ENABLED, false),
            waterIntervalHours = prefs.getInt(KEY_WATER_INTERVAL, 2),
            
            stepsEnabled = prefs.getBoolean(KEY_STEPS_ENABLED, false),
            stepsHour = prefs.getInt(KEY_STEPS_HOUR, 20),
            stepsMinute = prefs.getInt(KEY_STEPS_MINUTE, 0),
            
            streakWarningEnabled = prefs.getBoolean(KEY_STREAK_WARNING_ENABLED, false),
            streakWarningHour = prefs.getInt(KEY_STREAK_WARNING_HOUR, 21),
            streakWarningMinute = prefs.getInt(KEY_STREAK_WARNING_MINUTE, 0)
        )
    }
}

/**
 * Data class for reminder settings
 */
data class ReminderSettings(
    val breakfastEnabled: Boolean,
    val breakfastHour: Int,
    val breakfastMinute: Int,
    
    val lunchEnabled: Boolean,
    val lunchHour: Int,
    val lunchMinute: Int,
    
    val dinnerEnabled: Boolean,
    val dinnerHour: Int,
    val dinnerMinute: Int,
    
    val waterEnabled: Boolean,
    val waterIntervalHours: Int,
    
    val stepsEnabled: Boolean,
    val stepsHour: Int,
    val stepsMinute: Int,
    
    val streakWarningEnabled: Boolean,
    val streakWarningHour: Int,
    val streakWarningMinute: Int
)
