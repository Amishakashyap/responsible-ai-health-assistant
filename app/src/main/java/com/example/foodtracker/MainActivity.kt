package com.example.foodtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.foodtracker.data.db.SampleRecipes
import com.example.foodtracker.domain.ai.AIConfig
import com.example.foodtracker.domain.health.DailySummaryScheduler
import com.example.foodtracker.ui.AppNav
import com.example.foodtracker.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AI Health Assistant
        val aiConfig = AIConfig(this)
        if (aiConfig.isEnabled) {
            DailySummaryScheduler.scheduleDailySummary(this)
        }
        
        // Load sample recipes on first launch
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("recipes_loaded", false)) {
            SampleRecipes.insertSampleRecipes(this)
            prefs.edit().putBoolean("recipes_loaded", true).apply()
        }
        
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNav()
                }
            }
        }
    }
}
