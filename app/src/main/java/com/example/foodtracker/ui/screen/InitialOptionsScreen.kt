package com.example.foodtracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun InitialOptionsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCAF0F8))  // Updated to even lighter blue background color
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Welcome to SwasthVision",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)  // Increased spacing after app name
        )

        Spacer(modifier = Modifier.height(16.dp))  // Additional space before subtitle

        Text(
            text = "Select your goal to get started",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 40.dp)  // Increased spacing before cards
        )
        
        Spacer(modifier = Modifier.height(16.dp))  // Additional space before cards
        
        // Container for cards with less spacing
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)  // Reduced spacing between rows
        ) {
            // First row with two cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)  // Reduced spacing between cards
            ) {
                OptionCard("Weight Gain", navController, Modifier.weight(1f))
                OptionCard("Weight Loss", navController, Modifier.weight(1f))
            }

            // Second row with two cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)  // Reduced spacing between cards
            ) {
                OptionCard("BMI Calculator", navController, Modifier.weight(1f))
                OptionCard("Calories Tracking", navController, Modifier.weight(1f))
            }

            // Third row with two cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)  // Reduced spacing between cards
            ) {
                OptionCard("News", navController, Modifier.weight(1f))
                OptionCard("Water Tracking", navController, Modifier.weight(1f))
            }
        } // Close the cards container Column
    }
}

@Composable
fun OptionCard(title: String, navController: NavController, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(160.dp)  // Slightly reduced height to fit better
            .clickable { navController.navigate("login") },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}