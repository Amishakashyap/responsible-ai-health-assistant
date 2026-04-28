package com.example.foodtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import kotlin.math.pow

@Composable
fun BMICalculatorScreen(padding: PaddingValues = PaddingValues(0.dp)) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var bmi by remember { mutableStateOf<Double?>(null) }
    var category by remember { mutableStateOf("") }

    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BMI Calculator", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Weight (kg)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        
        OutlinedTextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Height (cm)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = {
                val w = weight.toDoubleOrNull()
                val h = height.toDoubleOrNull()
                if (w != null && h != null && h > 0) {
                    val heightM = h / 100.0
                    val calculatedBMI = w / heightM.pow(2)
                    bmi = calculatedBMI
                    category = when {
                        calculatedBMI < 18.5 -> "Underweight"
                        calculatedBMI < 25.0 -> "Normal weight"
                        calculatedBMI < 30.0 -> "Overweight"
                        else -> "Obese"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate BMI")
        }
        
        Spacer(Modifier.height(24.dp))
        
        bmi?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface)
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your BMI", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("%.2f".format(it), style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(category, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("BMI Categories:", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Text("< 18.5: Underweight", style = MaterialTheme.typography.bodyMedium)
                    Text("18.5 - 24.9: Normal", style = MaterialTheme.typography.bodyMedium)
                    Text("25.0 - 29.9: Overweight", style = MaterialTheme.typography.bodyMedium)
                    Text("≥ 30.0: Obese", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
