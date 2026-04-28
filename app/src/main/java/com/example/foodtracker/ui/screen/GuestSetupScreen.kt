package com.example.foodtracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.utils.BMICalculator
import com.example.foodtracker.utils.NutritionCalculator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestSetupScreen(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()
    
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("25") }
    var gender by remember { mutableStateOf("Male") }
    var activityLevel by remember { mutableStateOf("Moderate") }
    var fitnessGoal by remember { mutableStateOf("maintain") }
    
    var showBMI by remember { mutableStateOf(false) }
    var calculatedBMI by remember { mutableStateOf(0f) }
    var bmiCategory by remember { mutableStateOf("") }
    var calculatedCalories by remember { mutableStateOf(0) }
    
    var errorMsg by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showRegisterPrompt by remember { mutableStateOf(false) }

    val activityLevels = listOf(
        "Sedentary" to "Little or no exercise",
        "Lightly Active" to "Light exercise 1-3 days/week",
        "Moderate" to "Moderate exercise 3-5 days/week",
        "Very Active" to "Hard exercise 6-7 days/week",
        "Extremely Active" to "Very hard exercise & physical job"
    )
    
    val fitnessGoals = listOf(
        "lose" to "Lose Weight (Calorie deficit)",
        "maintain" to "Maintain Weight (Balanced)",
        "gain" to "Gain Weight/Muscle (Calorie surplus)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guest Setup") },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, "Info")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome, Guest!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            
            Text(
                "Let's personalize your nutrition goals",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Basic Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppSurface
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Basic Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    // Weight
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { 
                            weight = it
                            showBMI = false
                            errorMsg = ""
                        },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Height
                    OutlinedTextField(
                        value = height,
                        onValueChange = { 
                            height = it
                            showBMI = false
                            errorMsg = ""
                        },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Age
                    OutlinedTextField(
                        value = age,
                        onValueChange = { 
                            age = it
                            errorMsg = ""
                        },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Gender Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppSurface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Gender",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            FilterChip(
                                selected = gender == g,
                                onClick = { gender = g },
                                label = { Text(g) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Activity Level
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppSurface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Activity Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    activityLevels.forEach { (level, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activityLevel == level,
                                onClick = { activityLevel = level }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(level, fontWeight = FontWeight.Medium)
                                Text(
                                    desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Fitness Goal
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppSurface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Fitness Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    fitnessGoals.forEach { (goal, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = fitnessGoal == goal,
                                onClick = { fitnessGoal = goal }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(desc, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Calculate Button
            Button(
                onClick = {
                    val w = weight.toFloatOrNull()
                    val h = height.toFloatOrNull()
                    val a = age.toIntOrNull()
                    
                    if (w == null || w <= 0) {
                        errorMsg = "Please enter valid weight"
                        return@Button
                    }
                    if (h == null || h <= 0) {
                        errorMsg = "Please enter valid height"
                        return@Button
                    }
                    if (a == null || a <= 0 || a > 120) {
                        errorMsg = "Please enter valid age (1-120)"
                        return@Button
                    }
                    
                    // Calculate BMI
                    calculatedBMI = BMICalculator.calculateBMI(w, h)
                    bmiCategory = BMICalculator.getBMICategoryDescription(calculatedBMI)
                    
                    // Calculate nutrition goals
                    val nutritionGoals = NutritionCalculator.calculateAllNutritionGoals(
                        weightKg = w,
                        heightCm = h,
                        age = a,
                        gender = gender,
                        activityLevel = activityLevel,
                        goal = fitnessGoal
                    )
                    
                    calculatedCalories = nutritionGoals.calories
                    showBMI = true
                    errorMsg = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculate BMI & Calories")
            }
            
            if (errorMsg.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Results Card
            if (showBMI) {
                Spacer(Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppSurface
                    )
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Your Results",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // BMI
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("BMI:", fontWeight = FontWeight.Medium)
                            Text(
                                "%.1f (%s)".format(calculatedBMI, bmiCategory),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        
                        // Daily Calories
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Daily Calories:", fontWeight = FontWeight.Medium)
                            Text(
                                "$calculatedCalories kcal",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    // Save guest user data
                                    userPrefs.userId = 0L // Mark as guest
                                    userPrefs.nickname = "Guest"
                                    userPrefs.weight = weight.toFloat()
                                    userPrefs.height = height.toFloat()
                                    userPrefs.age = age.toInt()
                                    userPrefs.gender = gender
                                    userPrefs.activityLevel = activityLevel
                                    userPrefs.fitnessGoal = fitnessGoal
                                    userPrefs.isProfileComplete = true
                                    
                                    // Calculate and save all nutrition goals
                                    userPrefs.calculateAndSaveGoals()
                                    
                                    // Show registration prompt
                                    showRegisterPrompt = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue to App")
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }
    
    // Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("About Guest Setup") },
            text = {
                Text(
                    """
                    As a guest user, we'll calculate your personalized nutrition goals based on:
                    
                    • BMI (Body Mass Index) - Based on weight & height
                    • TDEE (Total Daily Energy Expenditure) - Based on activity level
                    • Macro Goals - Protein, Carbs, Fats optimized for your fitness goal
                    
                    You can always create an account later to sync your data across devices!
                    """.trimIndent()
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
    
    // Registration Prompt Dialog
    if (showRegisterPrompt) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("⚠️ Guest Mode - Data Not Saved") },
            text = {
                Text(
                    """
                    You are using the app as a guest. Your data is TEMPORARY and will be deleted when you close the app.
                    
                    To save your progress and data permanently, please create an account.
                    """.trimIndent()
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRegisterPrompt = false
                    onSetupComplete()
                }) {
                    Text("Continue as Guest")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRegisterPrompt = false
                    // Clear guest data and go back
                    userPrefs.clearAll()
                }) {
                    Text("Go Back")
                }
            }
        )
    }
}
