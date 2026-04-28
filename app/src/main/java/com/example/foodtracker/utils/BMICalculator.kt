package com.example.foodtracker.utils

import kotlin.math.pow

/**
 * BMI Calculator - Calculate Body Mass Index and related metrics
 */
object BMICalculator {
    
    /**
     * Calculate BMI from weight (kg) and height (cm)
     */
    fun calculateBMI(weightKg: Float, heightCm: Float): Float {
        if (heightCm <= 0 || weightKg <= 0) return 0f
        val heightM = heightCm / 100f
        return weightKg / (heightM.pow(2))
    }
    
    /**
     * Get BMI category
     */
    fun getBMICategory(bmi: Float): BMICategory {
        return when {
            bmi < 18.5f -> BMICategory.UNDERWEIGHT
            bmi < 25f -> BMICategory.NORMAL
            bmi < 30f -> BMICategory.OVERWEIGHT
            else -> BMICategory.OBESE
        }
    }
    
    /**
     * Get BMI category description
     */
    fun getBMICategoryDescription(bmi: Float): String {
        return when (getBMICategory(bmi)) {
            BMICategory.UNDERWEIGHT -> "Underweight"
            BMICategory.NORMAL -> "Normal Weight"
            BMICategory.OVERWEIGHT -> "Overweight"
            BMICategory.OBESE -> "Obese"
        }
    }
    
    /**
     * Get ideal weight range for given height
     */
    fun getIdealWeightRange(heightCm: Float): Pair<Float, Float> {
        val heightM = heightCm / 100f
        val minWeight = 18.5f * (heightM.pow(2))
        val maxWeight = 24.9f * (heightM.pow(2))
        return Pair(minWeight, maxWeight)
    }
    
    /**
     * Get health advice based on BMI
     */
    fun getHealthAdvice(bmi: Float): String {
        return when (getBMICategory(bmi)) {
            BMICategory.UNDERWEIGHT -> "Consider increasing calorie intake with nutrient-rich foods."
            BMICategory.NORMAL -> "Great! Maintain your healthy lifestyle."
            BMICategory.OVERWEIGHT -> "Consider a balanced diet and regular exercise."
            BMICategory.OBESE -> "Consult a healthcare provider for a personalized plan."
        }
    }
    
    /**
     * Get emoji for BMI category
     */
    fun getBMIEmoji(bmi: Float): String {
        return when (getBMICategory(bmi)) {
            BMICategory.UNDERWEIGHT -> "⚠️"
            BMICategory.NORMAL -> "✅"
            BMICategory.OVERWEIGHT -> "⚠️"
            BMICategory.OBESE -> "🚨"
        }
    }
}

enum class BMICategory {
    UNDERWEIGHT,
    NORMAL,
    OVERWEIGHT,
    OBESE
}
