package com.example.foodtracker.data.streak

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

/**
 * StreakManager tracks consecutive days of user activity.
 * Activities include: food logging, step tracking, profile updates, etc.
 */
class StreakManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    companion object {
        private const val PREFS_NAME = "streak_prefs"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_LAST_ACTIVITY_DATE = "last_activity_date"
        private const val KEY_TOTAL_ACTIVE_DAYS = "total_active_days"
        private const val KEY_STREAK_START_DATE = "streak_start_date"
    }
    
    /**
     * Record an activity for today. Updates streak accordingly.
     * @param activityType Type of activity (e.g., "food_log", "steps", "profile")
     */
    fun recordActivity(activityType: String = "general") {
        val today = getTodayDate()
        val lastActivityDate = prefs.getString(KEY_LAST_ACTIVITY_DATE, null)
        
        // If already recorded today, don't double count
        if (lastActivityDate == today) {
            return
        }
        
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val bestStreak = prefs.getInt(KEY_BEST_STREAK, 0)
        val totalActiveDays = prefs.getInt(KEY_TOTAL_ACTIVE_DAYS, 0)
        
        val newStreak = when {
            lastActivityDate == null -> {
                // First time activity
                prefs.edit().putString(KEY_STREAK_START_DATE, today).apply()
                1
            }
            isConsecutiveDay(lastActivityDate, today) -> {
                // Consecutive day - increase streak
                currentStreak + 1
            }
            else -> {
                // Streak broken - restart
                prefs.edit().putString(KEY_STREAK_START_DATE, today).apply()
                1
            }
        }
        
        // Update streak data
        prefs.edit().apply {
            putInt(KEY_CURRENT_STREAK, newStreak)
            putInt(KEY_BEST_STREAK, maxOf(newStreak, bestStreak))
            putString(KEY_LAST_ACTIVITY_DATE, today)
            putInt(KEY_TOTAL_ACTIVE_DAYS, totalActiveDays + 1)
            apply()
        }
    }
    
    /**
     * Get current streak count
     */
    fun getCurrentStreak(): Int {
        val today = getTodayDate()
        val lastActivityDate = prefs.getString(KEY_LAST_ACTIVITY_DATE, null)
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        
        // Check if streak is still valid (today or yesterday)
        return if (lastActivityDate != null && 
                   (lastActivityDate == today || isConsecutiveDay(lastActivityDate, today))) {
            currentStreak
        } else {
            // Streak expired
            0
        }
    }
    
    /**
     * Get best (longest) streak ever achieved
     */
    fun getBestStreak(): Int {
        return prefs.getInt(KEY_BEST_STREAK, 0)
    }
    
    /**
     * Get total number of active days
     */
    fun getTotalActiveDays(): Int {
        return prefs.getInt(KEY_TOTAL_ACTIVE_DAYS, 0)
    }
    
    /**
     * Get the date when current streak started
     */
    fun getStreakStartDate(): String? {
        return prefs.getString(KEY_STREAK_START_DATE, null)
    }
    
    /**
     * Get last activity date
     */
    fun getLastActivityDate(): String? {
        return prefs.getString(KEY_LAST_ACTIVITY_DATE, null)
    }
    
    /**
     * Check if user has been active today
     */
    fun isActiveToday(): Boolean {
        val today = getTodayDate()
        val lastActivityDate = prefs.getString(KEY_LAST_ACTIVITY_DATE, null)
        return lastActivityDate == today
    }
    
    /**
     * Reset all streak data (for testing or user request)
     */
    fun resetStreak() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Get streak statistics
     */
    fun getStreakStats(): StreakStats {
        val currentStreak = getCurrentStreak()
        return StreakStats(
            currentStreak = currentStreak,
            bestStreak = getBestStreak(),
            totalActiveDays = getTotalActiveDays(),
            lastActivityDate = getLastActivityDate(),
            streakStartDate = getStreakStartDate(),
            isActiveToday = isActiveToday()
        )
    }
    
    // Helper methods
    
    private fun getTodayDate(): String {
        return dateFormat.format(Date())
    }
    
    private fun isConsecutiveDay(lastDate: String, currentDate: String): Boolean {
        try {
            val lastDateObj = dateFormat.parse(lastDate) ?: return false
            val currentDateObj = dateFormat.parse(currentDate) ?: return false
            
            val calendar = Calendar.getInstance()
            calendar.time = lastDateObj
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            
            val expectedNextDay = dateFormat.format(calendar.time)
            return expectedNextDay == currentDate
        } catch (e: Exception) {
            return false
        }
    }
}

/**
 * Data class for streak statistics
 */
data class StreakStats(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalActiveDays: Int,
    val lastActivityDate: String?,
    val streakStartDate: String?,
    val isActiveToday: Boolean
)
