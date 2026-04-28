package com.example.foodtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.utils.BMICalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedProfileSetupScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    
    var nickname by remember { mutableStateOf(userPrefs.nickname) }
    var weight by remember { mutableStateOf(if (userPrefs.weight > 0) userPrefs.weight.toString() else "") }
    var height by remember { mutableStateOf(if (userPrefs.height > 0) userPrefs.height.toString() else "") }
    var age by remember { mutableStateOf(if (userPrefs.age > 0) userPrefs.age.toString() else "") }
    var gender by remember { mutableStateOf(userPrefs.gender) }
    var activityLevel by remember { mutableStateOf(userPrefs.activityLevel) }
    var fitnessGoal by remember { mutableStateOf(userPrefs.fitnessGoal) }
    
    var showBMI by remember { mutableStateOf(false) }
    var calculatedBMI by remember { mutableStateOf(0f) }
    var errorMsg by remember { mutableStateOf("") }
    
    val genderOptions = listOf("Male", "Female", "Other")
    val activityOptions = listOf("Sedentary", "Light", "Moderate", "Active", "Very Active")
    val goalOptions = listOf(
        "lose weight" to "Lose Weight",
        "maintain" to "Maintain",
        "gain muscle" to "Gain Muscle"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Your Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Introduction Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Let's personalize your experience! 🎯",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "We'll calculate your BMI and create personalized nutrition goals",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Nickname
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname") },
                placeholder = { Text("How should we call you?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                "We'll use this name in notifications and greetings! 😊",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Weight
            OutlinedTextField(
                value = weight,
                onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) weight = it },
                label = { Text("Weight (kg)") },
                placeholder = { Text("e.g., 70") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Height
            OutlinedTextField(
                value = height,
                onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) height = it },
                label = { Text("Height (cm)") },
                placeholder = { Text("e.g., 170") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Age
            OutlinedTextField(
                value = age,
                onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) age = it },
                label = { Text("Age (years)") },
                placeholder = { Text("e.g., 25") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Gender Selection
            Text(
                "Gender",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genderOptions.forEach { option ->
                    FilterChip(
                        selected = gender == option,
                        onClick = { gender = option },
                        label = { Text(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Activity Level
            Text(
                "Activity Level",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activityOptions.forEach { option ->
                    FilterChip(
                        selected = activityLevel == option,
                        onClick = { activityLevel = option },
                        label = {
                            Column {
                                Text(option, fontWeight = FontWeight.Medium)
                                Text(
                                    getActivityDescription(option),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Fitness Goal
            Text(
                "Fitness Goal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                goalOptions.forEach { (value, label) ->
                    FilterChip(
                        selected = fitnessGoal == value,
                        onClick = { fitnessGoal = value },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // BMI Display Card (if calculated)
            if (showBMI && calculatedBMI > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Your BMI",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                BMICalculator.getBMIEmoji(calculatedBMI),
                                fontSize = 32.sp
                            )
                            Text(
                                "%.1f".format(calculatedBMI),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            BMICalculator.getBMICategoryDescription(calculatedBMI),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            BMICalculator.getHealthAdvice(calculatedBMI),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // Error Message
            if (errorMsg.isNotEmpty()) {
                Text(
                    errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Calculate BMI Button
            if (!showBMI) {
                OutlinedButton(
                    onClick = {
                        val w = weight.toFloatOrNull()
                        val h = height.toFloatOrNull()
                        if (w != null && h != null && w > 0 && h > 0) {
                            calculatedBMI = BMICalculator.calculateBMI(w, h)
                            showBMI = true
                            errorMsg = ""
                        } else {
                            errorMsg = "Please enter valid weight and height"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calculate My BMI")
                }
            }
            
            // Save Button
            Button(
                onClick = {
                    val w = weight.toFloatOrNull()
                    val h = height.toFloatOrNull()
                    val a = age.toIntOrNull()
                    
                    when {
                        nickname.isBlank() -> errorMsg = "Please enter your nickname"
                        w == null || w <= 0 -> errorMsg = "Please enter valid weight"
                        h == null || h <= 0 -> errorMsg = "Please enter valid height"
                        a == null || a <= 0 -> errorMsg = "Please enter valid age"
                        else -> {
                            // Save all data
                            userPrefs.nickname = nickname
                            userPrefs.weight = w
                            userPrefs.height = h
                            userPrefs.age = a
                            userPrefs.gender = gender
                            userPrefs.activityLevel = activityLevel
                            userPrefs.fitnessGoal = fitnessGoal
                            userPrefs.calculateAndSaveGoals()
                            userPrefs.isProfileComplete = true
                            
                            // Complete setup
                            onComplete()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = nickname.isNotBlank() && weight.isNotBlank() && height.isNotBlank() && age.isNotBlank()
            ) {
                Text("Complete Setup & Calculate Goals")
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun getActivityDescription(level: String): String {
    return when (level) {
        "Sedentary" -> "Little or no exercise"
        "Light" -> "Exercise 1-3 days/week"
        "Moderate" -> "Exercise 3-5 days/week"
        "Active" -> "Exercise 6-7 days/week"
        "Very Active" -> "Hard exercise daily"
        else -> ""
    }
}
