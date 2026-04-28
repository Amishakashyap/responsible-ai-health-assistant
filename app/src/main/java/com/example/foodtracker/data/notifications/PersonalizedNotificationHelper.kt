package com.example.foodtracker.data.notifications

import android.content.Context
import com.example.foodtracker.data.user.UserPreferences

/**
 * PersonalizedNotificationHelper - Creates personalized notifications with user's nickname
 */
class PersonalizedNotificationHelper(private val context: Context) {
    
    private val notificationHelper = NotificationHelper(context)
    private val userPrefs = UserPreferences(context)
    
    /**
     * Get user's nickname
     */
    private fun getNickname(): String {
        return userPrefs.nickname
    }
    
    /**
     * Show personalized meal reminder
     */
    fun showPersonalizedMealReminder(mealType: String, notificationId: Int) {
        val nickname = getNickname()
        val title = "$nickname, time for $mealType! 🍽️"
        val message = "Don't forget to log your $mealType to maintain your streak and reach your goals!"
        
        notificationHelper.showMealReminder(mealType, notificationId)
    }
    
    /**
     * Show personalized water reminder
     */
    fun showPersonalizedWaterReminder() {
        val nickname = getNickname()
        val waterGoal = userPrefs.waterGoal
        val message = "$nickname, stay hydrated! Target: ${waterGoal}ml per day 💧"
        
        notificationHelper.showWaterReminder()
    }
    
    /**
     * Show personalized calorie progress notification
     */
    fun showCalorieProgressNotification(consumed: Int) {
        val nickname = getNickname()
        val goal = userPrefs.calorieGoal
        val remaining = goal - consumed
        
        val title = when {
            remaining > 500 -> "$nickname's Calorie Update 📊"
            remaining > 0 -> "$nickname, Almost There! 🎯"
            else -> "$nickname, Goal Reached! ✅"
        }
        
        val message = when {
            remaining > 0 -> "You have $remaining calories left for today!"
            remaining == 0 -> "Perfect! You've reached your calorie goal!"
            else -> "You've exceeded your goal by ${-remaining} calories"
        }
        
        // You can extend NotificationHelper to add this method
        // notificationHelper.showCustomNotification(title, message, NOTIFICATION_ID_CALORIE_PROGRESS)
    }
    
    /**
     * Show personalized protein progress notification
     */
    fun showProteinProgressNotification(consumed: Int) {
        val nickname = getNickname()
        val goal = userPrefs.proteinGoal
        val remaining = goal - consumed
        
        val title = "$nickname's Protein Tracker 💪"
        val message = when {
            remaining > 20 -> "${remaining}g protein remaining! Add some lean meat or legumes."
            remaining > 0 -> "Almost there! Just ${remaining}g protein left for today."
            else -> "Great job! You've met your protein goal of ${goal}g!"
        }
        
        // notificationHelper.showCustomNotification(title, message, NOTIFICATION_ID_PROTEIN_PROGRESS)
    }
    
    /**
     * Show personalized water progress notification
     */
    fun showWaterProgressNotification(glasses: Int) {
        val nickname = getNickname()
        val goalGlasses = userPrefs.waterGoal / 250 // Assuming 250ml per glass
        val remaining = goalGlasses - glasses
        
        val title = "$nickname's Hydration Tracker 💧"
        val message = when {
            remaining > 4 -> "${remaining} more glasses to go! Stay hydrated."
            remaining > 0 -> "You're doing great! ${remaining} glasses left."
            else -> "Perfect! You're well hydrated today!"
        }
        
        // notificationHelper.showCustomNotification(title, message, NOTIFICATION_ID_WATER_PROGRESS)
    }
    
    /**
     * Show evening reminder
     */
    fun showEveningReminder() {
        val nickname = getNickname()
        val title = "$nickname, How was your day? 🌙"
        val message = "Don't forget to log your last meal and complete your daily tracking!"
        
        // notificationHelper.showCustomNotification(title, message, NOTIFICATION_ID_EVENING)
    }
    
    /**
     * Show morning motivation
     */
    fun showMorningMotivation() {
        val nickname = getNickname()
        val bmi = userPrefs.bmi
        val calorieGoal = userPrefs.calorieGoal
        
        val title = "Good morning, $nickname! ☀️"
        val message = "Your goal today: ${calorieGoal} calories. Let's make it a healthy day!"
        
        // notificationHelper.showCustomNotification(title, message, NOTIFICATION_ID_MORNING)
    }
    
    /**
     * Show personalized streak notification
     */
    fun showPersonalizedStreakNotification(streakDays: Int) {
        val nickname = getNickname()
        val title = "$nickname's Streak Achievement! 🔥"
        val message = when {
            streakDays == 1 -> "Great start! Keep it going, $nickname!"
            streakDays < 7 -> "$nickname, you're on a $streakDays day streak!"
            streakDays < 30 -> "Amazing, $nickname! $streakDays days in a row!"
            else -> "$nickname, you're unstoppable! $streakDays day streak!"
        }
        
        notificationHelper.showStreakNotification(streakDays)
    }
    
    /**
     * Show personalized streak warning
     */
    fun showPersonalizedStreakWarning(streakDays: Int) {
        val nickname = getNickname()
        val title = "$nickname, Don't Break Your Streak! ⚠️"
        val message = "You're on a $streakDays day streak! Log your activity to keep it going."
        
        notificationHelper.showStreakWarning(streakDays)
    }
    
    /**
     * Show goal achievement notification
     */
    fun showGoalAchievementNotification(goalType: String) {
        val nickname = getNickname()
        val title = "Congratulations, $nickname! 🎉"
        val message = when (goalType.lowercase()) {
            "calorie" -> "You've reached your calorie goal for today!"
            "protein" -> "Protein goal achieved! Your muscles will thank you!"
            "water" -> "Fully hydrated! Great job, $nickname!"
            "all" -> "All goals achieved today! You're a nutrition champion!"
            else -> "You've achieved your $goalType goal!"
        }
        
        // notificationHelper.showCustomNotification(title, message, NOTIFICATION_ID_ACHIEVEMENT)
    }
    
    companion object {
        const val NOTIFICATION_ID_CALORIE_PROGRESS = 2001
        const val NOTIFICATION_ID_PROTEIN_PROGRESS = 2002
        const val NOTIFICATION_ID_WATER_PROGRESS = 2003
        const val NOTIFICATION_ID_EVENING = 2004
        const val NOTIFICATION_ID_MORNING = 2005
        const val NOTIFICATION_ID_ACHIEVEMENT = 2006
    }
}
