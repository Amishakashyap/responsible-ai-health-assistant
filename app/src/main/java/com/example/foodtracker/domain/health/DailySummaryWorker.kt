package com.example.foodtracker.domain.health

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.domain.ai.AIConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker that generates daily health summary in the background
 */
class DailySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val healthAdvisorService = HealthAdvisorService(context)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("DailySummaryWorker", "Starting daily summary generation")
            
            val result = healthAdvisorService.generateDailySummary()
            
            if (result.success) {
                Log.d("DailySummaryWorker", "Daily summary generated successfully")
                Result.success()
            } else {
                Log.e("DailySummaryWorker", "Failed to generate summary: ${result.error}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("DailySummaryWorker", "Error in DailySummaryWorker", e)
            Result.failure()
        }
    }
}

/**
 * Scheduler for daily health summary generation
 */
object DailySummaryScheduler {
    
    private const val WORK_NAME = "daily_health_summary"
    
    /**
     * Schedule daily summary generation
     */
    fun scheduleDailySummary(context: Context) {
        val aiConfig = AIConfig(context)
        val userPrefs = UserPreferences(context)
        
        if (!aiConfig.isEnabled || userPrefs.userId == 0L) {
            cancelDailySummary(context)
            return
        }
        
        val currentTime = java.util.Calendar.getInstance()
        val targetTime = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, aiConfig.summaryHour)
            set(java.util.Calendar.MINUTE, aiConfig.summaryMinute)
            set(java.util.Calendar.SECOND, 0)
        }
        
        // If target time has passed today, schedule for tomorrow
        if (targetTime.before(currentTime)) {
            targetTime.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        
        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Need internet for AI
            .setRequiresBatteryNotLow(true)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
        Log.d("DailySummaryScheduler", "Daily summary scheduled for ${aiConfig.summaryHour}:${String.format("%02d", aiConfig.summaryMinute)}")
    }
    
    /**
     * Cancel scheduled daily summary
     */
    fun cancelDailySummary(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d("DailySummaryScheduler", "Daily summary scheduling cancelled")
    }
    
    /**
     * Trigger immediate summary generation (for testing)
     */
    fun generateNow(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d("DailySummaryScheduler", "Immediate summary generation requested")
    }
}
