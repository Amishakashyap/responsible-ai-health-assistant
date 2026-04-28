package com.example.foodtracker.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BootReceiver restarts all active reminders after device reboot
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            restoreReminders(context)
        }
    }
    
    private fun restoreReminders(context: Context) {
        val scheduler = ReminderScheduler(context)
        val settings = scheduler.getReminderSettings()
        
        // Restore all enabled reminders
        if (settings.breakfastEnabled) {
            scheduler.scheduleMealReminder(
                ReminderScheduler.TYPE_BREAKFAST,
                settings.breakfastHour,
                settings.breakfastMinute,
                true
            )
        }
        
        if (settings.lunchEnabled) {
            scheduler.scheduleMealReminder(
                ReminderScheduler.TYPE_LUNCH,
                settings.lunchHour,
                settings.lunchMinute,
                true
            )
        }
        
        if (settings.dinnerEnabled) {
            scheduler.scheduleMealReminder(
                ReminderScheduler.TYPE_DINNER,
                settings.dinnerHour,
                settings.dinnerMinute,
                true
            )
        }
        
        if (settings.waterEnabled) {
            scheduler.scheduleWaterReminder(
                settings.waterIntervalHours,
                true
            )
        }
        
        if (settings.stepsEnabled) {
            scheduler.scheduleStepReminder(
                settings.stepsHour,
                settings.stepsMinute,
                true
            )
        }
        
        if (settings.streakWarningEnabled) {
            scheduler.scheduleStreakWarning(
                settings.streakWarningHour,
                settings.streakWarningMinute,
                true
            )
        }
    }
}
