package com.example.foodtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.db.UserProfile
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import kotlinx.coroutines.launch

@Composable
fun ProfileSetupScreen(
    userId: Long,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    
    var medicalHistory by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("weightloss") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var targetWeight by remember { mutableStateOf("") }
    var exerciseFreq by remember { mutableStateOf("none") }
    var errorMsg by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val goalOptions = listOf("weightloss", "gain", "muscle")
    val exerciseOptions = listOf("none", "twice_week", "regular", "proper_workout")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Complete Your Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            medicalHistory, { medicalHistory = it },
            label = { Text("Medical History (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            enabled = !isLoading
        )
        Spacer(Modifier.height(12.dp))
        
        Text("Goal", style = MaterialTheme.typography.labelLarge)
        goalOptions.forEach { opt ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = goal == opt, onClick = { goal = opt }, enabled = !isLoading)
                Text(opt)
            }
        }
        Spacer(Modifier.height(12.dp))
        
        OutlinedTextField(weight, { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
        Spacer(Modifier.height(8.dp))
        
        OutlinedTextField(height, { height = it }, label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
        Spacer(Modifier.height(8.dp))
        
        OutlinedTextField(targetWeight, { targetWeight = it }, label = { Text("Target Weight (kg)") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
        Spacer(Modifier.height(12.dp))
        
        Text("Exercise Frequency", style = MaterialTheme.typography.labelLarge)
        exerciseOptions.forEach { opt ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = exerciseFreq == opt, onClick = { exerciseFreq = opt }, enabled = !isLoading)
                Text(opt.replace("_", " "))
            }
        }
        Spacer(Modifier.height(16.dp))
        
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                val weightKg = weight.toDoubleOrNull()
                val heightCm = height.toDoubleOrNull()
                val targetKg = targetWeight.toDoubleOrNull()
                if (weightKg != null && heightCm != null && targetKg != null) {
                    isLoading = true
                    errorMsg = ""
                    scope.launch {
                        try {
                            val profile = UserProfile(
                                userId = userId,
                                medicalHistory = medicalHistory.trim(),
                                goal = goal,
                                weightKg = weightKg,
                                heightCm = heightCm,
                                targetWeightKg = targetKg,
                                exerciseFreq = exerciseFreq
                            )
                            db.profileDao().upsert(profile)
                            onComplete()
                        } catch (e: Exception) {
                            errorMsg = "Failed to save profile: ${e.message}"
                            isLoading = false
                        }
                    }
                } else {
                    errorMsg = "Please enter valid weight, height, and target weight"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp) // Make button taller
                .padding(horizontal = 16.dp), // Add horizontal padding
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "Submit & Continue",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp)) // Add extra space at bottom
    }
}
