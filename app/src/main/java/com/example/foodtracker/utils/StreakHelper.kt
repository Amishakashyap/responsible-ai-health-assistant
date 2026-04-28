package com.example.foodtracker.utils

import android.content.Context
import com.example.foodtracker.data.streak.StreakManager

/**
 * Helper functions to integrate streak tracking with existing features
 * Call these methods when users complete activities to maintain their streak
 */
object StreakHelper {
    
    /**
     * Call this when user logs food
     */
    fun recordFoodLog(context: Context) {
        val streakManager = StreakManager(context)
        streakManager.recordActivity("food_log")
    }
    
    /**
     * Call this when user tracks steps
     */
    fun recordStepsActivity(context: Context) {
        val streakManager = StreakManager(context)
        streakManager.recordActivity("steps")
    }
    
    /**
     * Call this when user updates profile or completes any other activity
     */
    fun recordGeneralActivity(context: Context) {
        val streakManager = StreakManager(context)
        streakManager.recordActivity("general")
    }
    
    /**
     * Call this when user tracks water intake
     */
    fun recordWaterTracking(context: Context) {
        val streakManager = StreakManager(context)
        streakManager.recordActivity("water")
    }
    
    /**
     * Call this when user tracks weight
     */
    fun recordWeightTracking(context: Context) {
        val streakManager = StreakManager(context)
        streakManager.recordActivity("weight")
    }
}
